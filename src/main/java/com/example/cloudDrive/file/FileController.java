package com.example.cloudDrive.file;

import com.example.cloudDrive.folder.FolderRepository;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
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
    private final FolderRepository folderRepository;

    public FileController(StorageService storageService,
                          StoredFileRepository fileRepository,
                          FolderRepository folderRepository) {
        this.storageService = storageService;
        this.fileRepository = fileRepository;
        this.folderRepository = folderRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FileResponse upload(@RequestParam("file") MultipartFile file,
                               @RequestParam(value = "folderId", required = false) Long folderId,
                               Authentication authentication) throws IOException {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        Long ownerId = (Long) authentication.getPrincipal();
        if (folderId != null) {
            folderRepository.findByIdAndOwnerId(folderId, ownerId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));
        }

        String key = storageService.store(file);

        StoredFile entity = new StoredFile();
        entity.setOwnerId(ownerId);
        entity.setFolderId(folderId);
        entity.setOriginalName(file.getOriginalFilename() == null ? "unnamed" : file.getOriginalFilename());
        entity.setContentType(file.getContentType());
        entity.setSize(file.getSize());
        entity.setStorageKey(key);
        return FileResponse.from(fileRepository.save(entity));
    }

    @GetMapping
    public List<FileResponse> list(@RequestParam(value = "folderId", required = false) Long folderId,
                                   Authentication authentication) {
        Long ownerId = (Long) authentication.getPrincipal();
        List<StoredFile> files;
        if (folderId == null) {
            files = fileRepository.findByOwnerIdAndFolderIdIsNullOrderByCreatedAtDesc(ownerId);
        } else {
            folderRepository.findByIdAndOwnerId(folderId, ownerId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));
            files = fileRepository.findByOwnerIdAndFolderIdOrderByCreatedAtDesc(ownerId, folderId);
        }
        return files.stream().map(FileResponse::from).toList();
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
    @PatchMapping("/{id}")
    public FileResponse move(@PathVariable Long id,
                             @RequestBody MoveFileRequest request,
                             Authentication authentication) {
        Long ownerId = (Long) authentication.getPrincipal();
        StoredFile f = fileRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
        if (request.folderId() != null) {
            folderRepository.findByIdAndOwnerId(request.folderId(), ownerId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));
        }
        f.setFolderId(request.folderId());
        return FileResponse.from(fileRepository.save(f));
    }
}