package com.trailoapp.trailo_backend.service

import com.trailoapp.trailo_backend.domain.enum.FriendshipStatus
import com.trailoapp.trailo_backend.domain.social.FriendshipEntity
import com.trailoapp.trailo_backend.exception.definitions.BusinessRuleException
import com.trailoapp.trailo_backend.exception.definitions.PermissionDeniedException
import com.trailoapp.trailo_backend.exception.definitions.ResourceNotFoundException
import com.trailoapp.trailo_backend.repository.FriendshipRepository
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FriendshipService (
    private val friendshipRepository: FriendshipRepository,
    private val userService: UserService
){

    /**
     * Get all accepted friendships for a user.
     */
    fun getFriends(userId: UUID, pageable: Pageable): Page<FriendshipEntity> {
        return friendshipRepository.findFriendshipsByUserIdAndStatus(userId, FriendshipStatus.ACCEPTED, pageable)
    }

    /**
     * Sends a friend request to another user.
     */
    @Transactional
    fun sendFriendRequest(senderId: UUID, receiverId: UUID): FriendshipEntity {
        if (senderId == receiverId) {
            throw BusinessRuleException("Cannot send friend request to self")
        }

        val sender = userService.findUserById(senderId)
            ?: throw ResourceNotFoundException("Sender user", senderId)

        val receiver = userService.findUserById(receiverId)
            ?: throw ResourceNotFoundException("Receiver user", receiverId)

        // Check if direct friendship exists
        findExistingFriendship(senderId, receiverId)?.let { friendship ->
            if (friendship.status == FriendshipStatus.REJECTED) {
                friendship.status = FriendshipStatus.PENDING
                return friendshipRepository.save(friendship)
            }

            throw BusinessRuleException("Friendship already exists")
        }

        // Check if inverse friendship exists
        findExistingFriendship(receiverId, senderId)?.let { inverseFriendship ->
            if (inverseFriendship.status == FriendshipStatus.PENDING) {
                inverseFriendship.status = FriendshipStatus.ACCEPTED
                return friendshipRepository.save(inverseFriendship)
            }

            throw BusinessRuleException("Friendship already exists")
        }

        // Create a new friendship
        return friendshipRepository.save(
             FriendshipEntity(
                user = sender,
                friend = receiver,
                status = FriendshipStatus.PENDING
            )
        )
    }

    /**
     * Updated the status of a friendship.
     */
    @Transactional
    fun updateFriendshipStatus(friendshipId: UUID, userId: UUID, newStatus: FriendshipStatus): FriendshipEntity {
        val friendship = friendshipRepository.findById(friendshipId)
            .orElseThrow { ResourceNotFoundException("Friendship", friendshipId) }

        // Verify if the user is part of the friendship
        if (friendship.user.uuid != userId && friendship.friend.uuid != userId) {
            throw PermissionDeniedException("update", "friendship", friendshipId)
        }

        // Only receiver can accept friendship request
        if (newStatus == FriendshipStatus.ACCEPTED && friendship.status == FriendshipStatus.PENDING) {
            if (userId != friendship.friend.uuid) {
                throw BusinessRuleException("Only the receiver can accept this request")
            }
        }

        friendship.status = newStatus
        return friendshipRepository.save(friendship)
    }

    /**
     * Delete a friendship between two users.
     */
    @Transactional
    fun deleteFriendship(userId: UUID, friendId: UUID) {
        val friendship = friendshipRepository.findFriendshipBetweenUsers(userId, friendId)
            .orElseThrow { ResourceNotFoundException("Friendship") }

        friendshipRepository.delete(friendship)
    }

    /**
     * Get all pending friendships for a user.
     */
    fun getPendingRequests(userId: UUID, pageable: Pageable): Page<FriendshipEntity> {
        return friendshipRepository.findFriendshipsByUserIdAndStatus(userId, FriendshipStatus.PENDING, pageable)
    }

    /**
     * Finds a friendship between two users.
     */
    fun findByUsersIds(userId: UUID, friendId: UUID): FriendshipEntity {
        return findExistingFriendship(userId, friendId)
            ?: throw ResourceNotFoundException("Friendship")
    }

    // ===== UTILITY METHODS =====

    private fun findExistingFriendship(senderId: UUID, receiverId: UUID): FriendshipEntity? {
        return friendshipRepository.findFriendshipBetweenUsers(senderId, receiverId)
            .orElse(null)
    }
}