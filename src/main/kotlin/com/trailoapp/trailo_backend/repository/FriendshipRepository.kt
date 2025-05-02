package com.trailoapp.trailo_backend.repository

import com.trailoapp.trailo_backend.domain.enum.FriendshipStatus
import com.trailoapp.trailo_backend.domain.social.FriendshipEntity
import org.springframework.data.domain.Page
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.cdi.JpaRepositoryExtension
import org.springframework.stereotype.Repository
import java.awt.print.Pageable
import java.util.Optional
import java.util.UUID

@Repository
interface FriendshipRepository : JpaRepository<FriendshipEntity, UUID> {
    fun findByUser_UuidAndStatus(userId: UUID, status: FriendshipStatus, pageable: Pageable): Page<FriendshipEntity>
    fun findByFriend_UuidAndStatus(friendId: UUID, status: FriendshipStatus, pageable: Pageable): Page<FriendshipEntity>
    fun existsByUser_UuidAndFriend_Uuid(userId: UUID, friendId: UUID, status: FriendshipStatus): Boolean
    fun findByUser_UuidAndFriend_Uuid(userId: UUID, friendId: UUID): Optional<FriendshipEntity>
}