package com.trailoapp.trailo_backend.service

import com.trailoapp.trailo_backend.domain.core.UserEntity
import com.trailoapp.trailo_backend.domain.enum.social.FriendshipStatus
import com.trailoapp.trailo_backend.domain.social.FriendshipEntity
import com.trailoapp.trailo_backend.exception.definitions.BusinessRuleException
import com.trailoapp.trailo_backend.exception.definitions.PermissionDeniedException
import com.trailoapp.trailo_backend.exception.definitions.ResourceNotFoundException
import com.trailoapp.trailo_backend.repository.FriendshipRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.kotlin.whenever
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.OffsetDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class FriendshipServiceTest {

    @Mock
    private lateinit var friendshipRepository: FriendshipRepository

    @Mock
    private lateinit var userService: UserService

    private lateinit var friendshipService: FriendshipService

    // Test data
    private lateinit var testUser1: UserEntity
    private lateinit var testUser2: UserEntity
    private lateinit var testFriendship: FriendshipEntity

    private val user1Id = UUID.randomUUID()
    private val user2Id = UUID.randomUUID()
    private val friendshipId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        friendshipService = FriendshipService(friendshipRepository, userService)

        testUser1 = UserEntity(
            uuid = user1Id,
            email = "user1@example.com",
            username = "user1",
            cognitoId = "cognito-user1"
        )

        testUser2 = UserEntity(
            uuid = user2Id,
            email = "user2@example.com",
            username = "user2",
            cognitoId = "cognito-user2"
        )

        testFriendship = FriendshipEntity(
            uuid = friendshipId,
            user = testUser1,
            friend = testUser2,
            status = FriendshipStatus.PENDING,
            createdAt = OffsetDateTime.now()
        )
    }

    @Test
    fun `getFriends returns accepted friendships for a user`() {
        val pageable = PageRequest.of(0, 10)
        val friendships = listOf(testFriendship)
        val friendshipsPage = PageImpl(friendships, pageable, friendships.size.toLong())

        whenever(friendshipRepository.findFriendshipsByUserIdAndStatus(
            user1Id, FriendshipStatus.ACCEPTED, pageable
        )).thenReturn(friendshipsPage)

        val result = friendshipService.getFriends(user1Id, pageable)

        assertNotNull(result)
        assertEquals(1, result.totalElements)
        assertEquals(friendshipId, result.content[0].uuid)

        verify(friendshipRepository).findFriendshipsByUserIdAndStatus(
            user1Id, FriendshipStatus.ACCEPTED, pageable
        )
    }

    @Test
    fun `sendFriendRequest creates new friendship when it does not exist`() {
        whenever(userService.findUserById(user1Id)).thenReturn(testUser1)
        whenever(userService.findUserById(user2Id)).thenReturn(testUser2)
        whenever(friendshipRepository.findFriendshipBetweenUsers(user1Id, user2Id)).thenReturn(null)
        whenever(friendshipRepository.findFriendshipBetweenUsers(user2Id, user1Id)).thenReturn(null)

        whenever(friendshipRepository.save(any(FriendshipEntity::class.java))).thenAnswer { invocation ->
            val savedFriendship = invocation.getArgument<FriendshipEntity>(0)
            savedFriendship.copy(uuid = UUID.randomUUID())
        }

        val result = friendshipService.sendFriendRequest(user1Id, user2Id)

        assertNotNull(result)
        assertEquals(testUser1, result.user)
        assertEquals(testUser2, result.friend)
        assertEquals(FriendshipStatus.PENDING, result.status)

        verify(userService).findUserById(user1Id)
        verify(userService).findUserById(user2Id)
        verify(friendshipRepository).findFriendshipBetweenUsers(user1Id, user2Id)
        verify(friendshipRepository).findFriendshipBetweenUsers(user2Id, user1Id)
        verify(friendshipRepository).save(any(FriendshipEntity::class.java))
    }

    @Test
    fun `sendFriendRequest throws BusinessRuleException when sending to self`() {
        val exception = assertThrows(BusinessRuleException::class.java, {
            friendshipService.sendFriendRequest(user1Id, user1Id)
        })

        assertTrue(exception.message?.contains("self") == true)
    }

    @Test
    fun `sendFriendRequest throws ResourceNotFoundException when sender does not exist`() {
        whenever(userService.findUserById(user1Id)).thenReturn(null)

        val exception = assertThrows(ResourceNotFoundException::class.java, {
            friendshipService.sendFriendRequest(user1Id, user2Id)
        })

        assertTrue(exception.message?.contains("Sender") == true)

        verify(userService).findUserById(user1Id)
    }

    @Test
    fun `sendFriendRequest throws ResourceNotFoundException when receiver does not exist`() {
        whenever(userService.findUserById(user1Id)).thenReturn(testUser1)
        whenever(userService.findUserById(user2Id)).thenReturn(null)

        val exception = assertThrows(ResourceNotFoundException::class.java, {
            friendshipService.sendFriendRequest(user1Id, user2Id)
        })

        assertTrue(exception.message?.contains("Receiver") == true)

        verify(userService).findUserById(user1Id)
        verify(userService).findUserById(user2Id)
    }

    @Test
    fun `sendFriendRequest updates status to PENDING when existing friendship is REJECTED`() {
        whenever(userService.findUserById(user1Id)).thenReturn(testUser1)
        whenever(userService.findUserById(user2Id)).thenReturn(testUser2)

        val rejectedFriendship = testFriendship.copy(status = FriendshipStatus.REJECTED)

        whenever(friendshipRepository.findFriendshipBetweenUsers(user1Id, user2Id)).thenReturn(rejectedFriendship)
        whenever(friendshipRepository.save(rejectedFriendship)).thenReturn(rejectedFriendship.copy(status = FriendshipStatus.PENDING))

        val result = friendshipService.sendFriendRequest(user1Id, user2Id)

        assertNotNull(result)
        assertEquals(FriendshipStatus.PENDING, result.status)

        verify(userService).findUserById(user1Id)
        verify(userService).findUserById(user2Id)
        verify(friendshipRepository).findFriendshipBetweenUsers(user1Id, user2Id)
        verify(friendshipRepository).save(rejectedFriendship)
    }

    @Test
    fun `sendFriendRequest throws BusinessRuleException when friendship already exists`() {
        whenever(userService.findUserById(user1Id)).thenReturn(testUser1)
        whenever(userService.findUserById(user2Id)).thenReturn(testUser2)

        val existingFriendship = testFriendship.copy(status = FriendshipStatus.PENDING)

        whenever(friendshipRepository.findFriendshipBetweenUsers(user1Id, user2Id)).thenReturn(existingFriendship)

        val exception = assertThrows(BusinessRuleException::class.java, {
            friendshipService.sendFriendRequest(user1Id, user2Id)
        })

        assertTrue(exception.message?.contains("already exists") == true)

        verify(userService).findUserById(user1Id)
        verify(userService).findUserById(user2Id)
        verify(friendshipRepository).findFriendshipBetweenUsers(user1Id, user2Id)
    }

    @Test
    fun `sendFriendRequest auto accepts when there is a pending inverse request`() {
        whenever(userService.findUserById(user1Id)).thenReturn(testUser1)
        whenever(userService.findUserById(user2Id)).thenReturn(testUser2)
        whenever(friendshipRepository.findFriendshipBetweenUsers(user1Id, user2Id)).thenReturn(null)

        val inverseFriendship = FriendshipEntity(
            uuid = UUID.randomUUID(),
            user = testUser2,
            friend = testUser1,
            status = FriendshipStatus.PENDING,
            createdAt = OffsetDateTime.now(),
            lastModifiedAt = OffsetDateTime.now()
        )

        whenever(friendshipRepository.findFriendshipBetweenUsers(user2Id, user1Id)).thenReturn(inverseFriendship)
        whenever(friendshipRepository.save(inverseFriendship)).thenReturn(inverseFriendship.copy(status = FriendshipStatus.ACCEPTED))

        val result = friendshipService.sendFriendRequest(user1Id, user2Id)

        assertNotNull(result)
        assertEquals(FriendshipStatus.ACCEPTED, result.status)

        verify(userService).findUserById(user1Id)
        verify(userService).findUserById(user2Id)
        verify(friendshipRepository).findFriendshipBetweenUsers(user1Id, user2Id)
        verify(friendshipRepository).findFriendshipBetweenUsers(user2Id, user1Id)
        verify(friendshipRepository).save(inverseFriendship)
    }

    @Test
    fun `updateFriendshipStatus updates status of friendship when user is the receiver`() {
        val pendingFriendship = FriendshipEntity(
            uuid = friendshipId,
            user = testUser1,
            friend = testUser2,
            status = FriendshipStatus.PENDING,
            createdAt = OffsetDateTime.now(),
            lastModifiedAt = OffsetDateTime.now()
        )

        whenever(friendshipRepository.findById(friendshipId)).thenReturn(Optional.of(pendingFriendship))
        whenever(friendshipRepository.save(pendingFriendship)).thenReturn(
            pendingFriendship.copy(status = FriendshipStatus.ACCEPTED)
        )

        val result = friendshipService.updateFriendshipStatus(friendshipId, user2Id, FriendshipStatus.ACCEPTED)

        assertNotNull(result)
        assertEquals(FriendshipStatus.ACCEPTED, result.status)

        verify(friendshipRepository).findById(friendshipId)
        verify(friendshipRepository).save(pendingFriendship)
    }

    @Test
    fun `updateFriendshipStatus throws PermissionDeniedException when user is not part of the friendship`() {
        val randomUserId = UUID.randomUUID()

        whenever(friendshipRepository.findById(friendshipId)).thenReturn(Optional.of(testFriendship))

        assertThrows(PermissionDeniedException::class.java, {
            friendshipService.updateFriendshipStatus(friendshipId, randomUserId, FriendshipStatus.ACCEPTED)
        })

        verify(friendshipRepository).findById(friendshipId)
    }

    @Test
    fun `updateFriendshipStatus throws BusinessRuleException when sender tries to accept request`() {
        val pendingFriendship = FriendshipEntity(
            uuid = friendshipId,
            user = testUser1,
            friend = testUser2,
            status = FriendshipStatus.PENDING,
            createdAt = OffsetDateTime.now(),
            lastModifiedAt = OffsetDateTime.now()
        )

        whenever(friendshipRepository.findById(friendshipId)).thenReturn(Optional.of(pendingFriendship))

        val exception = assertThrows(BusinessRuleException::class.java, {
            friendshipService.updateFriendshipStatus(friendshipId, user1Id, FriendshipStatus.ACCEPTED)
        })

        assertTrue(exception.message?.contains("Only the receiver") == true)

        verify(friendshipRepository).findById(friendshipId)
    }

    @Test
    fun `deleteFriendship deletes friendship when it exists`() {
        whenever(friendshipRepository.findFriendshipBetweenUsers(user1Id, user2Id)).thenReturn(testFriendship)

        friendshipService.deleteFriendship(user1Id, user2Id)

        verify(friendshipRepository).findFriendshipBetweenUsers(user1Id, user2Id)
        verify(friendshipRepository).delete(testFriendship)
    }

    @Test
    fun `deleteFriendship throws ResourceNotFoundException when friendship does not exist`() {
        whenever(friendshipRepository.findFriendshipBetweenUsers(user1Id, user2Id)).thenReturn(null)

        assertThrows(ResourceNotFoundException::class.java, {
            friendshipService.deleteFriendship(user1Id, user2Id)
        })

        verify(friendshipRepository).findFriendshipBetweenUsers(user1Id, user2Id)
    }

    @Test
    fun `getPendingRequests returns pending friendship requests`() {
        val pageable = PageRequest.of(0, 10)
        val friendships = listOf(testFriendship)
        val friendshipsPage = PageImpl(friendships, pageable, friendships.size.toLong())

        whenever(friendshipRepository.findFriendshipsByUserIdAndStatus(
            user1Id, FriendshipStatus.PENDING, pageable
        )).thenReturn(friendshipsPage)

        val result = friendshipService.getPendingRequests(user1Id, pageable)

        assertNotNull(result)
        assertEquals(1, result.totalElements)
        assertEquals(friendshipId, result.content[0].uuid)

        verify(friendshipRepository).findFriendshipsByUserIdAndStatus(
            user1Id, FriendshipStatus.PENDING, pageable
        )
    }

    @Test
    fun `findByUsersIds returns friendship when it exists`() {
        whenever(friendshipRepository.findFriendshipBetweenUsers(user1Id, user2Id)).thenReturn(testFriendship)

        val result = friendshipService.findByUsersIds(user1Id, user2Id)

        assertNotNull(result)
        assertEquals(friendshipId, result.uuid)

        verify(friendshipRepository).findFriendshipBetweenUsers(user1Id, user2Id)
    }

    @Test
    fun `findByUsersIds throws ResourceNotFoundException when friendship does not exist`() {
        whenever(friendshipRepository.findFriendshipBetweenUsers(user1Id, user2Id)).thenReturn(null)

        assertThrows(ResourceNotFoundException::class.java, {
            friendshipService.findByUsersIds(user1Id, user2Id)
        })

        verify(friendshipRepository).findFriendshipBetweenUsers(user1Id, user2Id)
    }
}