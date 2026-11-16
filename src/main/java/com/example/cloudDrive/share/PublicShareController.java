package com.example.cloudDrive.share;

import com.example.cloudDrive.file.StorageService;
import com.example.cloudDrive.file.StoredFile;
import com.example.cloudDrive.file.StoredFileRepository;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@RestController
public class PublicShareController {

    private final ShareLinkRepository shareRepository;
    private final StoredFileRepository fileRepository;
    private final StorageService storageService;

    public PublicShareController(ShareLinkRepository shareRepository,
                                 StoredFileRepository fileRepository,
                                 StorageService storageService) {
        this.shareRepository = shareRepository;
        this.fileRepository = fileRepository;
        this.storageService = storageService;
    }

    @GetMapping("/api/public/shares/{token}")
    public ResponseEntity<Resource> download(@PathVariable String token) {
        ResponseStatusException notFound =
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Link not found or expired");

        ShareLink link = shareRepository.findByTokenHash(ShareController.hash(token))
                .orElseThrow(() -> notFound);
        if (link.getRevokedAt() != null || !link.getExpiresAt().isAfter(Instant.now())) {
            throw notFound;
        }
        StoredFile f = fileRepository.findById(link.getFileId()).orElseThrow(() -> notFound);
        Resource resource = storageService.load(f.getStorageKey());
        if (!resource.exists()) {
            throw notFound;
        }

        MediaType type = MediaType.APPLICATION_OCTET_STREAM;
        if (f.getContentType() != null) {
            try {
                type = MediaType.parseMediaType(f.getContentType());
            } catch (Exception ignored) {
                // keep octet-stream
            }
        }
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(f.getOriginalName(), StandardCharsets.UTF_8)
                                .build().toString())
                .body(resource);
    }
}