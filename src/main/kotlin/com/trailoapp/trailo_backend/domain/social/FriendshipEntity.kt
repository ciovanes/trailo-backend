package com.trailoapp.trailo_backend.domain.social

import com.trailoapp.trailo_backend.domain.core.UserEntity
import com.trailoapp.trailo_backend.domain.enum.FriendshipStatus
import jakarta.persistence.*
import org.hibernate.annotations.ColumnTransformer
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "friendship", schema = "social")
data class FriendshipEntity (
    @Id
    @Column(name = "uuid")
    val uuid: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "friend_id", nullable = false)
    val friend: UserEntity,

    @Column(name = "status", nullable = false, columnDefinition = "social.friendship_status")
    @Enumerated(EnumType.STRING)
    @ColumnTransformer(
        read = "status::text",
        write = "?::social.friendship_status"
    )
    var status: FriendshipStatus,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @UpdateTimestamp
    @Column(name = "last_modified_at", nullable = true)
    val lastModifiedAt: OffsetDateTime? = null
)