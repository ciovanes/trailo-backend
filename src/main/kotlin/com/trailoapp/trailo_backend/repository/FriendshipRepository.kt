package com.trailoapp.trailo_backend.repository

import com.trailoapp.trailo_backend.domain.enum.social.FriendshipStatus
import com.trailoapp.trailo_backend.domain.social.FriendshipEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface FriendshipRepository : JpaRepository<FriendshipEntity, UUID> {

    @Query(value =
        """
            SELECT f FROM FriendshipEntity f 
            WHERE (f.user.uuid = :userId OR f.friend.uuid = :userId)
            AND f.status = :status
            """
    )
    fun findFriendshipsByUserIdAndStatus(userId: UUID, status: FriendshipStatus, pageable: Pageable): Page<FriendshipEntity>

    @Query(value =
        """
            SELECT f FROM FriendshipEntity f
            WHERE (f.user.uuid = :userId AND f.friend.uuid = :friendId)
            OR (f.user.uuid = :friendId AND f.friend.uuid = :userId)
        """
    )
    fun findFriendshipBetweenUsers(userId: UUID, friendId: UUID): FriendshipEntity?
}