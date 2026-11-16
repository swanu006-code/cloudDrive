package com.example.cloudDrive.folder;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.example.cloudDrive.file.StoredFileRepository;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import com.example.cloudDrive.file.FileResponse;
import com.example.cloudDrive.file.*;
import java.util.List;

@RestController
@RequestMapping("/api/folders")
public class FolderController {

    private final FolderRepository folderRepository;
    private final StoredFileRepository fileRepository;

    public FolderController(FolderRepository folderRepository, StoredFileRepository fileRepository) {
        this.folderRepository = folderRepository;
        this.fileRepository = fileRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FolderResponse create(@Valid @RequestBody CreateFolderRequest request,
                                 Authentication authentication) {
        Long ownerId = (Long) authentication.getPrincipal();
        String name = request.name().trim();
        if (name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is blank");
        }
        Long parentId = request.parentId();

        boolean duplicate;
        if (parentId == null) {
            duplicate = folderRepository.existsByOwnerIdAndParentIdIsNullAndNameIgnoreCase(ownerId, name);
        } else {
            folderRepository.findByIdAndOwnerId(parentId, ownerId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent folder not found"));
            duplicate = folderRepository.existsByOwnerIdAndParentIdAndNameIgnoreCase(ownerId, parentId, name);
        }
        if (duplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A folder with this name already exists here");
        }

        Folder folder = new Folder();
        folder.setOwnerId(ownerId);
        folder.setName(name);
        folder.setParentId(parentId);
        return FolderResponse.from(folderRepository.save(folder));
    }

    @GetMapping
    public List<FolderResponse> list(@RequestParam(required = false) Long parentId,
                                     Authentication authentication) {
        Long ownerId = (Long) authentication.getPrincipal();
        List<Folder> folders;
        if (parentId == null) {
            folders = folderRepository.findByOwnerIdAndParentIdIsNullOrderByNameAsc(ownerId);
        } else {
            folderRepository.findByIdAndOwnerId(parentId, ownerId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));
            folders = folderRepository.findByOwnerIdAndParentIdOrderByNameAsc(ownerId, parentId);
        }
        return folders.stream().map(FolderResponse::from).toList();
    }
    @PatchMapping("/{id}")
    public FolderResponse update(@PathVariable Long id,
                                 @Valid @RequestBody UpdateFolderRequest request,
                                 Authentication authentication) {
        Long ownerId = (Long) authentication.getPrincipal();
        Folder folder = folderRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));

        String newName = folder.getName();
        if (request.name() != null) {
            newName = request.name().trim();
            if (newName.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is blank");
            }
        }

        Long newParentId = folder.getParentId();
        if (Boolean.TRUE.equals(request.moveToRoot())) {
            newParentId = null;
        } else if (request.parentId() != null) {
            newParentId = request.parentId();
        }

        if (newParentId != null) {
            if (newParentId.equals(id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot move a folder into itself");
            }
            Folder cursor = folderRepository.findByIdAndOwnerId(newParentId, ownerId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent folder not found"));
            // walk up from the new parent; if we meet this folder, it would create a loop
            while (cursor.getParentId() != null) {
                if (cursor.getParentId().equals(id)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Cannot move a folder into its own subfolder");
                }
                cursor = folderRepository.findByIdAndOwnerId(cursor.getParentId(), ownerId).orElse(null);
                if (cursor == null) {
                    break;
                }
            }
        }

        boolean changed = !newName.equals(folder.getName())
                || !java.util.Objects.equals(newParentId, folder.getParentId());
        if (changed) {
            boolean duplicate = (newParentId == null)
                    ? folderRepository.existsByOwnerIdAndParentIdIsNullAndNameIgnoreCase(ownerId, newName)
                    : folderRepository.existsByOwnerIdAndParentIdAndNameIgnoreCase(ownerId, newParentId, newName);
            boolean onlyCaseChange = newName.equalsIgnoreCase(folder.getName())
                    && java.util.Objects.equals(newParentId, folder.getParentId());
            if (duplicate && !onlyCaseChange) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A folder with this name already exists there");
            }
        }

        folder.setName(newName);
        folder.setParentId(newParentId);
        return FolderResponse.from(folderRepository.save(folder));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        Long ownerId = (Long) authentication.getPrincipal();
        Folder folder = folderRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));
        if (folderRepository.existsByParentId(id) || fileRepository.existsByFolderId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Folder is not empty");
        }
        folderRepository.delete(folder);
    }

}