package com.example.cloudDrive.share;

import java.time.Instant;

public record ShareResponse(Long id, Long fileId, Instant expiresAt, Instant revokedAt,
                            Instant createdAt, String token, String url) {

    public static ShareResponse from(ShareLink s) {
        return new ShareResponse(s.getId(), s.getFileId(), s.getExpiresAt(),
                s.getRevokedAt(), s.getCreatedAt(), null, null);
    }

    public static ShareResponse withToken(ShareLink s, String token) {
        return new ShareResponse(s.getId(), s.getFileId(), s.getExpiresAt(),
                s.getRevokedAt(), s.getCreatedAt(), token, "/api/public/shares/" + token);
    }
}