package com.trailoapp.trailo_backend.repository

import com.trailoapp.trailo_backend.domain.core.UserEntity
import com.trailoapp.trailo_backend.domain.social.UserMeetupEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface UserMeetupRepository: JpaRepository<UserMeetupEntity, UUID> {

    fun findByUserUuidAndMeetupUuid(userId: UUID, meetupId: UUID): UserMeetupEntity?

    @Query(value =
        """
            SELECT um.user FROM UserMeetupEntity um
            WHERE um.meetup.uuid = :meetupId
        """
    )
    fun findAllByMeetupUuid(meetupId: UUID, pageable: Pageable): Page<UserEntity>
}