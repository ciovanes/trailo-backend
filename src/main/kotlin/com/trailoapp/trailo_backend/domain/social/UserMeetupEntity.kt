package com.trailoapp.trailo_backend.domain.social

import com.trailoapp.trailo_backend.domain.core.UserEntity
import com.trailoapp.trailo_backend.domain.geo.MeetupEntity
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "user_meetup", schema = "social")
data class UserMeetupEntity(
    @Id
    @Column(nullable = false)
    val uuid: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meetup_id", nullable = false)
    val meetup: MeetupEntity,

    @CreationTimestamp
    @Column(name = "join_date", nullable = false)
    val joinDate: OffsetDateTime = OffsetDateTime.now()
)
