package com.example.cloudDrive.file;

import java.time.Instant;

public record FileResponse(Long id, String name, String contentType, long size,
                           Long folderId, Instant createdAt) {

    public static FileResponse from(StoredFile f) {
        return new FileResponse(f.getId(), f.getOriginalName(), f.getContentType(),
                f.getSize(), f.getFolderId(), f.getCreatedAt());
    }
}