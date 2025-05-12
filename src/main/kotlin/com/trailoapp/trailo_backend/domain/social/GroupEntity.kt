package com.trailoapp.trailo_backend.domain.social

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "group", schema = "social")
data class GroupEntity (
    @Id
    @Column(name = "uuid")
    val uuid: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val name: String,

    @Column(nullable = true)
    var description: String? = null,

    @Column(name = "private", nullable = false)
    var isPrivate: Boolean,

    @Column(name = "image_url", nullable = true)
    var imageUrl: String? = null,

    @UpdateTimestamp
    @Column(name = "last_modified_at", nullable = true)
    val lastModifiedAt: OffsetDateTime? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)