package com.trailoapp.trailo_backend.domain.social

import com.trailoapp.trailo_backend.domain.core.GroupEntity
import com.trailoapp.trailo_backend.domain.core.UserEntity
import com.trailoapp.trailo_backend.domain.enum.social.GroupRoles
import com.trailoapp.trailo_backend.domain.enum.social.MembershipStatus
import jakarta.persistence.*
import org.hibernate.annotations.ColumnTransformer
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "user_group", schema = "social")
data class UserGroupEntity(
    @Id
    @Column(name = "uuid")
    val uuid: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    val group: GroupEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @ColumnTransformer(
        read = "status::text",
        write = "?::social.membership_status"
    )
    var status: MembershipStatus,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @ColumnTransformer(
        read = "role::text",
        write = "?::social.group_role"
    )
    var role: GroupRoles,

    @Column(name = "invited_by", nullable = false)
    var invitedBy: UUID,

    @Column(name = "is_favorite")
    var isFavorite: Boolean = false,

    @UpdateTimestamp
    @Column(name = "last_modified_at", nullable = true)
    val lastModifiedAt: OffsetDateTime? = null,

    @CreationTimestamp
    @Column(name = "join_date", nullable = false, updatable = false)
    val joinDate: OffsetDateTime = OffsetDateTime.now()
)
