package com.example.cloudDrive.share;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShareLinkRepository extends JpaRepository<ShareLink, Long> {

    Optional<ShareLink> findByTokenHash(String tokenHash);

    Optional<ShareLink> findByIdAndOwnerId(Long id, Long ownerId);

    List<ShareLink> findByFileIdAndOwnerIdOrderByCreatedAtDesc(Long fileId, Long ownerId);
}