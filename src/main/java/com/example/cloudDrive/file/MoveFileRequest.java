package com.example.cloudDrive.file;

// folderId null = move to the top level
public record MoveFileRequest(Long folderId) {}