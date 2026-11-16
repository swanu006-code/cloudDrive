package com.example.cloudDrive.folder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateFolderRequest(
        @NotBlank @Size(max = 255) String name,
        Long parentId
) {}