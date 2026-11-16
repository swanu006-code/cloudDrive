package com.example.cloudDrive.share;

import com.example.cloudDrive.file.StoredFileRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@RestController
public class ShareController {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ShareLinkRepository shareRepository;
    private final StoredFileRepository fileRepository;

    public ShareController(ShareLinkRepository shareRepository, StoredFileRepository fileRepository) {
        this.shareRepository = shareRepository;
        this.fileRepository = fileRepository;
    }

    @PostMapping("/api/files/{fileId}/shares")
    @ResponseStatus(HttpStatus.CREATED)
    public ShareResponse create(@PathVariable Long fileId,
                                @Valid @RequestBody(required = false) CreateShareRequest request,
                                Authentication authentication) {
        Long ownerId = (Long) authentication.getPrincipal();
        fileRepository.findByIdAndOwnerId(fileId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));

        int hours = (request == null || request.expiresInHours() == null) ? 24 : request.expiresInHours();

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        ShareLink link = new ShareLink();
        link.setFileId(fileId);
        link.setOwnerId(ownerId);
        link.setTokenHash(hash(token));
        link.setExpiresAt(Instant.now().plus(hours, ChronoUnit.HOURS));
        return ShareResponse.withToken(shareRepository.save(link), token);
    }

    @GetMapping("/api/files/{fileId}/shares")
    public List<ShareResponse> list(@PathVariable Long fileId, Authentication authentication) {
        Long ownerId = (Long) authentication.getPrincipal();
        fileRepository.findByIdAndOwnerId(fileId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
        return shareRepository.findByFileIdAndOwnerIdOrderByCreatedAtDesc(fileId, ownerId)
                .stream().map(ShareResponse::from).toList();
    }

    @DeleteMapping("/api/shares/{shareId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable Long shareId, Authentication authentication) {
        Long ownerId = (Long) authentication.getPrincipal();
        ShareLink link = shareRepository.findByIdAndOwnerId(shareId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Share not found"));
        if (link.getRevokedAt() == null) {
            link.setRevokedAt(Instant.now());
            shareRepository.save(link);
        }
    }

    static String hash(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}