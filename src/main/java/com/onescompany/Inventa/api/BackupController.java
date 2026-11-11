package com.onescompany.Inventa.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.onescompany.Inventa.security.BackupService;

@RestController
@RequestMapping("/api/backups")
public class BackupController {
    private final BackupService backups;

    public BackupController(BackupService backups) {
        this.backups = backups;
    }

    @GetMapping
    @PreAuthorize("hasRole('DIRECTOR')")
    public ResponseEntity<StreamingResponseBody> downloadBackup() {
        Path file;
        try {
            file = backups.createBackup();
        } catch (IOException | SQLException error) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to create backup.", error);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/zip"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("inventa-backup-" + LocalDate.now() + ".zip")
                .build());
        headers.setContentLength(file.toFile().length());
        headers.setCacheControl("no-store");
        StreamingResponseBody body = output -> {
            try {
                Files.copy(file, output);
                output.flush();
            } finally {
                Files.deleteIfExists(file);
            }
        };
        return new ResponseEntity<>(body, headers, HttpStatus.OK);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('DIRECTOR')")
    public ResponseEntity<Void> restoreBackup(@RequestParam("backup") MultipartFile backup) {
        try {
            backups.restore(backup);
        } catch (ResponseStatusException error) {
            throw error;
        } catch (IOException | SQLException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to restore this backup.", error);
        }
        return ResponseEntity.noContent().build();
    }
}
