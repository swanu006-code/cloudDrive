package com.example.cloudDrive.folder;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder, Long> {

    Optional<Folder> findByIdAndOwnerId(Long id, Long ownerId);

    List<Folder> findByOwnerIdAndParentIdIsNullOrderByNameAsc(Long ownerId);

    List<Folder> findByOwnerIdAndParentIdOrderByNameAsc(Long ownerId, Long parentId);

    boolean existsByOwnerIdAndParentIdAndNameIgnoreCase(Long ownerId, Long parentId, String name);

    boolean existsByOwnerIdAndParentIdIsNullAndNameIgnoreCase(Long ownerId, String name);

    boolean existsByParentId(Long parentId);
}