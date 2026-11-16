package com.example.cloudDrive.file;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final StorageService storageService;
    private final StoredFileRepository fileRepository;

    public FileController(StorageService storageService, StoredFileRepository fileRepository) {
        this.storageService = storageService;
        this.fileRepository = fileRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FileResponse upload(@RequestParam("file") MultipartFile file,
                               Authentication authentication) throws IOException {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        Long ownerId = (Long) authentication.getPrincipal();

        String key = storageService.store(file);

        StoredFile entity = new StoredFile();
        entity.setOwnerId(ownerId);
        entity.setOriginalName(file.getOriginalFilename() == null ? "unnamed" : file.getOriginalFilename());
        entity.setContentType(file.getContentType());
        entity.setSize(file.getSize());
        entity.setStorageKey(key);
        return FileResponse.from(fileRepository.save(entity));
    }

    @GetMapping
    public List<FileResponse> list(Authentication authentication) {
        Long ownerId = (Long) authentication.getPrincipal();
        return fileRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId)
                .stream().map(FileResponse::from).toList();
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id, Authentication authentication) {
        Long ownerId = (Long) authentication.getPrincipal();
        StoredFile f = fileRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));

        Resource resource = storageService.load(f.getStorageKey());
        if (!resource.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File data missing");
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
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) throws IOException {
        Long ownerId = (Long) authentication.getPrincipal();
        StoredFile f = fileRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
        fileRepository.delete(f);
        storageService.delete(f.getStorageKey());
    }
}