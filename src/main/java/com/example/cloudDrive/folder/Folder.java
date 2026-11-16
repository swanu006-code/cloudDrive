package com.example.cloudDrive.folder;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "folders")
@Getter
@Setter
@NoArgsConstructor
public class Folder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private String name;

    // NULL means "top level". Otherwise it is the id of another row in this same table.
    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}