package com.onescompany.Inventa.security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EvidenceStorageService {
    public static final int MAX_FILES = 5;
    public static final long MAX_FILE_SIZE = 50L * 1024 * 1024;

    private static final Map<String, String> EXTENSIONS = Map.ofEntries(
            Map.entry("image/jpeg", ".jpg"),
            Map.entry("image/png", ".png"),
            Map.entry("image/webp", ".webp"),
            Map.entry("video/mp4", ".mp4"),
            Map.entry("video/quicktime", ".mov"),
            Map.entry("video/webm", ".webm"));

    private final Path storageDirectory;

    public EvidenceStorageService(@Value("${inventa.evidence.storage-dir:uploads/evidence}") String storageDirectory) {
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
    }

    public StoredFile store(MultipartFile file) throws IOException {
        validate(file);
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        String extension = EXTENSIONS.get(contentType);

        String storageKey = UUID.randomUUID() + extension;
        Files.createDirectories(storageDirectory);
        Path target = storageDirectory.resolve(storageKey).normalize();
        if (!target.startsWith(storageDirectory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid evidence file.");
        }
        try (var input = file.getInputStream()) {
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }
        String originalName = file.getOriginalFilename();
        String fileName = originalName == null || originalName.isBlank()
                ? storageKey
                : Path.of(originalName).getFileName().toString();
        if (fileName.length() > 240) fileName = fileName.substring(fileName.length() - 240);
        return new StoredFile(storageKey, fileName, contentType, target);
    }

    public void validate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Evidence file is empty.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Evidence file exceeds 50 MB.");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        String extension = EXTENSIONS.get(contentType);
        if (extension == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only JPEG, PNG, WebP, MP4, MOV and WebM files are accepted.");
        }
    }

    public Path resolve(String storageKey) {
        Path path = storageDirectory.resolve(storageKey).normalize();
        if (!path.startsWith(storageDirectory)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Evidence file not found.");
        }
        return path;
    }

    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(resolve(storageKey));
    }

    public void storeBackupFile(String storageKey, Path source) throws IOException {
        if (!storageKey.matches("[a-fA-F0-9-]{36}\\.(jpg|png|webp|mp4|mov|webm)")) {
            throw new IOException("Backup contains an invalid evidence file name.");
        }
        Files.createDirectories(storageDirectory);
        Files.copy(source, resolve(storageKey), StandardCopyOption.REPLACE_EXISTING);
    }

    public List<Path> listFiles() throws IOException {
        if (!Files.isDirectory(storageDirectory)) return List.of();
        try (var files = Files.list(storageDirectory)) {
            return files.filter(Files::isRegularFile).toList();
        }
    }

    public record StoredFile(String storageKey, String fileName, String contentType, Path path) {
    }
}
