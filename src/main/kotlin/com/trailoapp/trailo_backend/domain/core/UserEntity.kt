package com.trailoapp.trailo_backend.domain.core

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "\"user\"", schema = "core")
data class UserEntity (
    @Id
    @Column(name = "uuid")
    val uuid: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false, unique = true)
    val username: String,

    @Column(nullable = true)
    var name: String? = null,

    @Column(nullable = true)
    var surname: String? = null,

    @Column(name = "profile_picture", nullable = true)
    var profileImageUrl: String? = null,

    @Column(nullable = true)
    var country: String? = null,

    @Column(name = "cognito_id", nullable = false, unique = true)
    val cognitoId: String,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now()
    )
