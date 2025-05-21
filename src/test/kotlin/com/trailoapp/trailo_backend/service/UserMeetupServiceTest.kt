package com.trailoapp.trailo_backend.service

import com.trailoapp.trailo_backend.domain.core.GroupEntity
import com.trailoapp.trailo_backend.domain.core.UserEntity
import com.trailoapp.trailo_backend.domain.enum.geo.MeetupStatus
import com.trailoapp.trailo_backend.domain.enum.geo.TrailDifficulty
import com.trailoapp.trailo_backend.domain.geo.MeetupEntity
import com.trailoapp.trailo_backend.domain.social.UserMeetupEntity
import com.trailoapp.trailo_backend.exception.definitions.BusinessRuleException
import com.trailoapp.trailo_backend.exception.definitions.ResourceNotFoundException
import com.trailoapp.trailo_backend.repository.UserMeetupRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.kotlin.whenever
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class UserMeetupServiceTest {

    @Mock
    private lateinit var userService: UserService

    @Mock
    private lateinit var meetupService: MeetupService

    @Mock
    private lateinit var groupService: GroupService

    @Mock
    private lateinit var userMeetupRepository: UserMeetupRepository

    @Mock
    private lateinit var mockPoint: Point

    private lateinit var userMeetupService: UserMeetupService

    private val userId = UUID.randomUUID()
    private val hostId = UUID.randomUUID()
    private val meetupId = UUID.randomUUID()
    private val groupId = UUID.randomUUID()
    private lateinit var testUser: UserEntity
    private lateinit var testMeetup: MeetupEntity
    private lateinit var testGroup: GroupEntity
    private lateinit var testUserMeetup: UserMeetupEntity

    @BeforeEach
    fun setup() {
        userMeetupService = UserMeetupService(
            userService,
            meetupService,
            groupService,
            userMeetupRepository
        )

        testUser = UserEntity(
            uuid = userId,
            email = "user@example.com",
            username = "user",
            cognitoId = "cognito-user"
        )

        testMeetup = MeetupEntity(
            uuid = meetupId,
            host = hostId,
            group = groupId,
            title = "Test Meetup",
            description = "Test Description",
            difficulty = TrailDifficulty.INTERMEDIATE,
            distanceKm = BigDecimal("5.5"),
            estimatedDurationInMin = 60,
            meetingTime = OffsetDateTime.now(),
            meetingPoint = mockPoint,
            status = MeetupStatus.WAITING
        )

        testGroup = GroupEntity(
            uuid = groupId,
            name = "Test Group",
            isPrivate = false
        )

        testUserMeetup = UserMeetupEntity(
            uuid = UUID.randomUUID(),
            user = testUser,
            meetup = testMeetup,
            joinDate = OffsetDateTime.now()
        )
    }

    @Test
    fun `joinMeetup successfully adds user to meetup`() {
        whenever(userService.findUserById(userId)).thenReturn(testUser)
        whenever(meetupService.getMeetupOrThrow(meetupId)).thenReturn(testMeetup)
        whenever(groupService.getGroupByUuid(groupId)).thenReturn(testGroup)
        whenever(userMeetupRepository.save(any(UserMeetupEntity::class.java))).thenReturn(testUserMeetup)

        val result = userMeetupService.joinMeetup(userId, meetupId)

        assertNotNull(result)
        assertEquals(testUser, result.user)
        assertEquals(testMeetup, result.meetup)

        verify(userService).findUserById(userId)
        verify(meetupService).getMeetupOrThrow(meetupId)
        verify(groupService).getGroupByUuid(groupId)
        verify(userMeetupRepository).save(any(UserMeetupEntity::class.java))
    }

    @Test
    fun `joinMeetup throws ResourceNotFoundException when user does not exist`() {
        whenever(userService.findUserById(userId)).thenReturn(null)

        val exception = assertThrows(ResourceNotFoundException::class.java) {
            userMeetupService.joinMeetup(userId, meetupId)
        }

        assertTrue(exception.message?.contains("user") == true)

        verify(userService).findUserById(userId)
    }

    @Test
    fun `joinMeetup throws BusinessRuleException when user is host`() {
        val hostMeetup = testMeetup.copy(host = userId)

        whenever(userService.findUserById(userId)).thenReturn(testUser)
        whenever(meetupService.getMeetupOrThrow(meetupId)).thenReturn(hostMeetup)
        whenever(groupService.getGroupByUuid(groupId)).thenReturn(testGroup)

        val exception = assertThrows(BusinessRuleException::class.java) {
            userMeetupService.joinMeetup(userId, meetupId)
        }

        assertTrue(exception.message?.contains("host can't join") == true)

        verify(userService).findUserById(userId)
        verify(meetupService).getMeetupOrThrow(meetupId)
        verify(groupService).getGroupByUuid(groupId)
    }

    @Test
    fun `joinMeetup verifies group membership for private group`() {
        val privateGroup = testGroup.copy(isPrivate = true)

        whenever(userService.findUserById(userId)).thenReturn(testUser)
        whenever(meetupService.getMeetupOrThrow(meetupId)).thenReturn(testMeetup)
        whenever(groupService.getGroupByUuid(groupId)).thenReturn(privateGroup)
        whenever(userMeetupRepository.save(any(UserMeetupEntity::class.java))).thenReturn(testUserMeetup)

        val result = userMeetupService.joinMeetup(userId, meetupId)

        assertNotNull(result)

        verify(userService).findUserById(userId)
        verify(meetupService).getMeetupOrThrow(meetupId)
        verify(groupService).getGroupByUuid(groupId)
        verify(groupService).getUserMembershipOrThrow(userId, groupId)
        verify(userMeetupRepository).save(any(UserMeetupEntity::class.java))
    }

    @Test
    fun `leaveMeetup successfully removes user from meetup`() {
        whenever(userService.findUserById(userId)).thenReturn(testUser)
        whenever(meetupService.getMeetupOrThrow(meetupId)).thenReturn(testMeetup)
        whenever(meetupService.getUserMeetupOrThrow(userId, meetupId)).thenReturn(testUserMeetup)

        userMeetupService.leaveMeetup(userId, meetupId)

        verify(userService).findUserById(userId)
        verify(meetupService).getMeetupOrThrow(meetupId)
        verify(meetupService).getUserMeetupOrThrow(userId, meetupId)
        verify(userMeetupRepository).delete(testUserMeetup)
    }

    @Test
    fun `leaveMeetup throws ResourceNotFoundException when user does not exist`() {
        whenever(userService.findUserById(userId)).thenReturn(null)

        val exception = assertThrows(ResourceNotFoundException::class.java) {
            userMeetupService.leaveMeetup(userId, meetupId)
        }

        assertTrue(exception.message?.contains("user") == true)

        verify(userService).findUserById(userId)
    }
}