package com.example.cloudDrive.file;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class StorageService {

    private final Path root;

    public StorageService(@Value("${app.storage.location}") String location) throws IOException {
        this.root = Paths.get(location).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    /** Saves the bytes under a random name and returns that name (the storage key). */
    public String store(MultipartFile file) throws IOException {
        String key = UUID.randomUUID().toString();
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, resolve(key), StandardCopyOption.REPLACE_EXISTING);
        }
        return key;
    }

    public Resource load(String key) {
        return new FileSystemResource(resolve(key));
    }

    public void delete(String key) throws IOException {
        Files.deleteIfExists(resolve(key));
    }

    private Path resolve(String key) {
        Path p = root.resolve(key).normalize();
        if (!p.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return p;
    }
}