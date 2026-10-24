package com.onescompany.Inventa.security;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;

@Service
public class PushNotificationService {
    private static final Logger logger = LoggerFactory.getLogger(PushNotificationService.class);

    private final PushDeviceTokenRepository tokens;
    private final AppUserRepository users;
    private final ApplicationEventPublisher events;
    private final String projectId;
    private volatile FirebaseMessaging messaging;

    public PushNotificationService(
            PushDeviceTokenRepository tokens,
            AppUserRepository users,
            ApplicationEventPublisher events,
            @Value("${inventa.firebase.project-id:}") String projectId) {
        this.tokens = tokens;
        this.users = users;
        this.events = events;
        this.projectId = projectId;
    }

    public void notifyUsername(String username, String title, String body, String type) {
        publish(new PushNotificationEvent(List.of(username), title, body, type));
    }

    public void notifyRole(UserRole role, String title, String body, String type) {
        publish(new PushNotificationEvent(role, null, title, body, type));
    }

    public void notifyRoleInField(UserRole role, Long fieldId, String title, String body, String type) {
        publish(new PushNotificationEvent(role, fieldId, title, body, type));
    }

    private void publish(PushNotificationEvent event) {
        events.publishEvent(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendAfterCommit(PushNotificationEvent event) {
        List<String> usernames = event.usernames() != null
                ? event.usernames()
                : users.findEnabledUsernamesByRoleAndField(event.role(), event.fieldId());
        for (String username : usernames) {
            List<String> deviceTokens = tokens.findByUsername(username).stream()
                    .map(PushDeviceToken::getToken)
                    .toList();
            sendToTokens(deviceTokens, event);
        }
    }

    private void sendToTokens(List<String> deviceTokens, PushNotificationEvent event) {
        if (deviceTokens.isEmpty()) return;
        if (projectId.isBlank()) {
            logger.warn("Push notification skipped because inventa.firebase.project-id is not configured.");
            return;
        }
        try {
            FirebaseMessaging firebaseMessaging = FirebaseMessaging.getInstance(firebaseApp());
            for (int start = 0; start < deviceTokens.size(); start += 500) {
                List<String> batch = deviceTokens.subList(start, Math.min(start + 500, deviceTokens.size()));
                MulticastMessage message = MulticastMessage.builder()
                        .addAllTokens(batch)
                        .setNotification(Notification.builder().setTitle(event.title()).setBody(event.body()).build())
                        .putData("type", event.type())
                        .build();
                BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
                if (response.getFailureCount() > 0) {
                    for (int i = 0; i < response.getResponses().size(); i++) {
                        var result = response.getResponses().get(i);
                        if (!result.isSuccessful() && result.getException() != null) {
                            FirebaseMessagingException error = result.getException();
                            logger.warn("Push delivery failed for a registered device: {}",
                                    error.getMessagingErrorCode());
                            if (error.getMessagingErrorCode() != null
                                    && (error.getMessagingErrorCode().name().equals("UNREGISTERED")
                                            || error.getMessagingErrorCode().name().equals("INVALID_ARGUMENT"))) {
                                tokens.deleteByToken(batch.get(i));
                            }
                        }
                    }
                }
            }
        } catch (FirebaseMessagingException | IOException | FirebaseInitializationException error) {
            logger.error("Unable to send Firebase push notification.", error);
        }
    }

    private FirebaseApp firebaseApp() throws IOException {
        synchronized (FirebaseApp.class) {
            return FirebaseApp.getApps().stream()
                    .filter(app -> app.getName().equals("inventa-push"))
                    .findFirst()
                    .orElseGet(() -> {
                        try {
                            FirebaseOptions options = FirebaseOptions.builder()
                                    .setCredentials(GoogleCredentials.getApplicationDefault())
                                    .setProjectId(projectId)
                                    .build();
                            return FirebaseApp.initializeApp(options, "inventa-push");
                        } catch (IOException error) {
                            throw new FirebaseInitializationException(error);
                        }
                    });
        }
    }

    public record PushNotificationEvent(
            List<String> usernames,
            UserRole role,
            Long fieldId,
            String title,
            String body,
            String type) {
        public PushNotificationEvent(List<String> usernames, String title, String body, String type) {
            this(usernames, null, null, title, body, type);
        }

        public PushNotificationEvent(UserRole role, Long fieldId, String title, String body, String type) {
            this(null, role, fieldId, title, body, type);
        }
    }

    private static final class FirebaseInitializationException extends RuntimeException {
        FirebaseInitializationException(IOException cause) {
            super(cause);
        }
    }
}
