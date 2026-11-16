package com.example.cloudDrive.folder;

import java.time.Instant;

public record FolderResponse(Long id, String name, Long parentId, Instant createdAt) {

    public static FolderResponse from(Folder f) {
        return new FolderResponse(f.getId(), f.getName(), f.getParentId(), f.getCreatedAt());
    }
}