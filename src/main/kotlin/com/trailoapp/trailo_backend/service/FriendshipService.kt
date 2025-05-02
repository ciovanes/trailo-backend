package com.trailoapp.trailo_backend.service

import com.trailoapp.trailo_backend.domain.enum.FriendshipStatus
import com.trailoapp.trailo_backend.domain.social.FriendshipEntity
import com.trailoapp.trailo_backend.repository.FriendshipRepository
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
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
                friendshipRepository.save(friendship)
            }

            throw IllegalArgumentException("Friendship already exists")
        }

        val inverseExistingFriendship = friendshipRepository.findByUser_UuidAndFriend_Uuid(receiverId, senderId)
        if (inverseExistingFriendship.isPresent) {
            val inverseFriendship = inverseExistingFriendship.get()

            if (inverseFriendship.status == FriendshipStatus.PENDING) {
                inverseFriendship.status = FriendshipStatus.ACCEPTED
                friendshipRepository.save(inverseFriendship)
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
            status = FriendshipStatus.ACCEPTED
        )

        return friendshipRepository.save(friendship)
    }
}