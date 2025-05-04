package com.trailoapp.trailo_backend.service

import com.trailoapp.trailo_backend.domain.enum.FriendshipStatus
import com.trailoapp.trailo_backend.domain.social.FriendshipEntity
import com.trailoapp.trailo_backend.repository.FriendshipRepository
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FriendshipService (
    private val friendshipRepository: FriendshipRepository,
    private val userService: UserService
){

    private val logger = LoggerFactory.getLogger(this.javaClass)

    /*
    Send a friend request to a user
     */
    @Transactional
    fun sendFriendRequest(senderId: UUID, receiverId: UUID): FriendshipEntity {

        if (senderId == receiverId) {
            throw IllegalArgumentException("Cannot send friend request to self")
        }

        val existingFriendship = friendshipRepository.findByUser_UuidAndFriend_Uuid(senderId, receiverId)
        if (existingFriendship.isPresent) {
            val friendship = existingFriendship.get()

            if (friendship.status == FriendshipStatus.REJECTED) {
                friendship.status = FriendshipStatus.PENDING
                return friendshipRepository.save(friendship)
            }

            throw IllegalArgumentException("Friendship already exists")
        }

        val inverseExistingFriendship = friendshipRepository.findByUser_UuidAndFriend_Uuid(receiverId, senderId)
        if (inverseExistingFriendship.isPresent) {
            val inverseFriendship = inverseExistingFriendship.get()

            if (inverseFriendship.status == FriendshipStatus.PENDING) {
                inverseFriendship.status = FriendshipStatus.ACCEPTED
                return friendshipRepository.save(inverseFriendship)
            }

            throw IllegalArgumentException("Friendship already exists")
        }

        val user = userService.findUserById(senderId)
        val friend = userService.findUserById(receiverId)
            ?: throw IllegalArgumentException("User does not exist: $receiverId")

        // create friendship
        val friendship = FriendshipEntity(
            user = user!!,
            friend = friend,
            status = FriendshipStatus.PENDING
        )

        return friendshipRepository.save(friendship)
    }

    @Transactional
    fun updateFriendshipStatus(friendshipId: UUID, userId: UUID, newStatus: FriendshipStatus): FriendshipEntity {
        val friendship = friendshipRepository.findById(friendshipId)
            .orElseThrow { Exception("Friendship not found") } // TODO: Create ResourceNotFoundException!!

        if (friendship.user.uuid != userId && friendship.friend.uuid != userId) {
            throw AccessDeniedException("You don't hace permission to update this friendship")
        }

        if (newStatus == FriendshipStatus.ACCEPTED && friendship.status == FriendshipStatus.PENDING) {
            if (userId != friendship.friend.uuid) {
                throw IllegalStateException("Only reciver can accept this request")
            }
        }

        friendship.status = newStatus
        return friendshipRepository.save(friendship)
    }

    fun getFriends(userId: UUID, pageable: Pageable): Page<FriendshipEntity> {
        return friendshipRepository.findFriendshipsByUserIdAndStatus(userId, FriendshipStatus.ACCEPTED, pageable)
    }

    fun getPendingRequests(userId: UUID, pageable: Pageable): Page<FriendshipEntity> {
        return friendshipRepository.findFriendshipsByUserIdAndStatus(userId, FriendshipStatus.PENDING, pageable)
    }

    @Transactional
    fun deleteFriendship(userId: UUID, friendId: UUID) {
        val friendship = friendshipRepository.findFriendshipBetweenUsers(userId, friendId)
            .orElseThrow { Exception("Friendship not found") }

        friendshipRepository.delete(friendship)
    }

    fun areFriends(userId: UUID, friendId: UUID): Boolean {
        return friendshipRepository.existsByUser_UuidAndFriend_Uuid(userId, friendId) ||
                friendshipRepository.existsByUser_UuidAndFriend_Uuid(friendId, userId)
    }

    fun findByUsersIds(userId: UUID, friendId: UUID): FriendshipEntity {
        val friendship = friendshipRepository.findFriendshipBetweenUsers(userId, friendId)
            .orElseThrow { Exception("Friendship not found") }

        return friendship
    }
}