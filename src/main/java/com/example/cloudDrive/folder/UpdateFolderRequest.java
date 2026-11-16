package com.example.cloudDrive.folder;

import jakarta.validation.constraints.Size;

// name: null = keep the current name.
// moveToRoot: true = move to the top level; otherwise parentId (if given) is the new parent.
public record UpdateFolderRequest(
        @Size(max = 255) String name,
        Long parentId,
        Boolean moveToRoot
) {}