package com.trailoapp.trailo_backend.service

import com.trailoapp.trailo_backend.domain.core.GroupEntity
import com.trailoapp.trailo_backend.domain.core.UserEntity
import com.trailoapp.trailo_backend.domain.enum.geo.MeetupStatus
import com.trailoapp.trailo_backend.domain.enum.geo.TerrainType
import com.trailoapp.trailo_backend.domain.enum.geo.TrailDifficulty
import com.trailoapp.trailo_backend.domain.geo.MeetupEntity
import com.trailoapp.trailo_backend.domain.social.UserMeetupEntity
import com.trailoapp.trailo_backend.dto.meetup.request.CreateMeetupRequest
import com.trailoapp.trailo_backend.dto.meetup.request.PointDto
import com.trailoapp.trailo_backend.dto.meetup.request.UpdateMeetupRequest
import com.trailoapp.trailo_backend.exception.definitions.PermissionDeniedException
import com.trailoapp.trailo_backend.exception.definitions.ResourceNotFoundException
import com.trailoapp.trailo_backend.repository.MeetupRepository
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
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class MeetupServiceTest {

    @Mock
    private lateinit var meetupRepository: MeetupRepository

    @Mock
    private lateinit var userMeetupRepository: UserMeetupRepository

    @Mock
    private lateinit var userService: UserService

    @Mock
    private lateinit var groupService: GroupService

    @Mock
    private lateinit var geometryFactory: GeometryFactory

    @Mock
    private lateinit var point: Point

    private lateinit var meetupService: MeetupService

    private val testUserId = UUID.randomUUID()
    private val testGroupId = UUID.randomUUID()
    private val testMeetupId = UUID.randomUUID()
    private lateinit var testUser: UserEntity
    private lateinit var testGroup: GroupEntity
    private lateinit var testMeetup: MeetupEntity
    private lateinit var testUserMeetup: UserMeetupEntity

    @BeforeEach
    fun setup() {
        meetupService = MeetupService(
            groupService,
            meetupRepository,
            geometryFactory,
            userService,
            userMeetupRepository
        )

        testUser = UserEntity(
            uuid = testUserId,
            email = "test@example.com",
            username = "testuser",
            cognitoId = "cognito-123"
        )

        testGroup = GroupEntity(
            uuid = testGroupId,
            name = "Test Group",
            isPrivate = false
        )

        testMeetup = MeetupEntity(
            uuid = testMeetupId,
            host = testUserId,
            group = testGroupId,
            title = "Test Meetup",
            description = "Test Description",
            difficulty = TrailDifficulty.INTERMEDIATE,
            distanceKm = BigDecimal("5.5"),
            estimatedDurationInMin = 60,
            meetingTime = OffsetDateTime.now().plusDays(1),
            meetingPoint = point,
            status = MeetupStatus.WAITING
        )

        testUserMeetup = UserMeetupEntity(
            uuid = UUID.randomUUID(),
            user = testUser,
            meetup = testMeetup
        )
    }

    @Test
    fun `createMeetup creates new meetup successfully`() {
        val pointDto = PointDto(
            type = "Point",
            coordinates = listOf(40.0, -3.0)
        )

        val createRequest = CreateMeetupRequest(
            group = testGroupId,
            title = "New Meetup",
            description = "Description",
            difficulty = TrailDifficulty.INTERMEDIATE,
            terrainTypes = listOf(TerrainType.FOREST, TerrainType.MOUNTAIN),
            distanceKm = BigDecimal("5.5"),
            estimatedDurationInMin = 60,
            meetingTime = OffsetDateTime.now().plusDays(1),
            meetingPoint = pointDto
        )

        whenever(groupService.getGroupByUuid(testGroupId)).thenReturn(testGroup)
        whenever(userService.findUserById(testUserId)).thenReturn(testUser)
        whenever(geometryFactory.createPoint(any(Coordinate::class.java))).thenReturn(point)
        whenever(meetupRepository.save(any(MeetupEntity::class.java))).thenAnswer { invocation ->
            val savedMeetup = invocation.getArgument<MeetupEntity>(0)
            savedMeetup.copy(uuid = testMeetupId)
        }
        whenever(userMeetupRepository.save(any(UserMeetupEntity::class.java))).thenReturn(testUserMeetup)

        val result = meetupService.createMeetup(testUserId, createRequest)

        assertNotNull(result)
        assertEquals(testMeetupId, result.uuid)
        assertEquals(testUserId, result.host)
        assertEquals(testGroupId, result.group)
        assertEquals(createRequest.title, result.title)
        assertEquals(createRequest.description, result.description)
        assertEquals(createRequest.difficulty, result.difficulty)
        assertEquals(createRequest.distanceKm, result.distanceKm)
        assertEquals(MeetupStatus.WAITING, result.status)

        verify(groupService).getGroupByUuid(testGroupId)
        verify(userService).findUserById(testUserId)
        verify(groupService).getUserMembershipOrThrow(testUserId, testGroupId)
        verify(geometryFactory).createPoint(any(Coordinate::class.java))
        verify(meetupRepository, times(2)).save(any(MeetupEntity::class.java))
        verify(userMeetupRepository).save(any(UserMeetupEntity::class.java))
    }

    @Test
    fun `createMeetup throws ResourceNotFoundException when user not found`() {
        val pointDto = PointDto(
            type = "Point",
            coordinates = listOf(40.0, -3.0)
        )

        val createRequest = CreateMeetupRequest(
            group = testGroupId,
            title = "New Meetup",
            description = "Description",
            difficulty = TrailDifficulty.INTERMEDIATE,
            distanceKm = BigDecimal("5.5"),
            estimatedDurationInMin = 60,
            meetingTime = OffsetDateTime.now().plusDays(1),
            meetingPoint = pointDto
        )

        whenever(groupService.getGroupByUuid(testGroupId)).thenReturn(testGroup)
        whenever(userService.findUserById(testUserId)).thenReturn(null)

        val exception = assertThrows(ResourceNotFoundException::class.java) {
            meetupService.createMeetup(testUserId, createRequest)
        }

        assertTrue(exception.message?.contains("user") == true)

        verify(groupService).getGroupByUuid(testGroupId)
        verify(userService).findUserById(testUserId)
    }

    @Test
    fun `deleteMeetup deletes meetup when user is host`() {
        whenever(meetupRepository.findById(testMeetupId)).thenReturn(Optional.of(testMeetup))

        meetupService.deleteMeetup(testMeetupId, testUserId)

        verify(meetupRepository).findById(testMeetupId)
        verify(meetupRepository).delete(testMeetup)
    }

    @Test
    fun `deleteMeetup throws PermissionDeniedException when user is not host`() {
        val otherUserId = UUID.randomUUID()
        whenever(meetupRepository.findById(testMeetupId)).thenReturn(Optional.of(testMeetup))

        val exception = assertThrows(PermissionDeniedException::class.java) {
            meetupService.deleteMeetup(testMeetupId, otherUserId)
        }

        assertTrue(exception.message?.contains("delete meetup") == true)

        verify(meetupRepository).findById(testMeetupId)
    }

    @Test
    fun `updateMeetup updates meetup when user is host`() {
        val updateRequest = UpdateMeetupRequest(
            title = "Updated Title",
            description = "Updated Description",
            difficulty = TrailDifficulty.ADVANCED,
            status = MeetupStatus.CURRENT,
            terrainTypes = listOf(TerrainType.MOUNTAIN),
            meetingPoint = null,
            maxParticipants = 20,
            distanceKm = BigDecimal("10.0"),
            estimatedDurationInMin = 120,
            meetingTime = OffsetDateTime.now().plusDays(2),
            meetupPicture = "updated.jpg"
        )

        whenever(meetupRepository.findById(testMeetupId)).thenReturn(Optional.of(testMeetup))
        whenever(meetupRepository.save(any(MeetupEntity::class.java))).thenReturn(testMeetup)

        val result = meetupService.updateMeetup(testMeetupId, testUserId, updateRequest)

        assertNotNull(result)
        assertEquals("Updated Title", result.title)
        assertEquals("Updated Description", result.description)
        assertEquals(TrailDifficulty.ADVANCED, result.difficulty)
        assertEquals(MeetupStatus.CURRENT, result.status)

        verify(meetupRepository).findById(testMeetupId)
        verify(meetupRepository).save(any(MeetupEntity::class.java))
    }

    @Test
    fun `updateMeetup throws PermissionDeniedException when user is not host`() {
        val otherUserId = UUID.randomUUID()
        val updateRequest = UpdateMeetupRequest(
            title = "Updated Title",
            description = null,
            difficulty = null,
            status = null,
            terrainTypes = null,
            meetingPoint = null,
            maxParticipants = null,
            distanceKm = null,
            estimatedDurationInMin = null,
            meetingTime = null,
            meetupPicture = null
        )

        whenever(meetupRepository.findById(testMeetupId)).thenReturn(Optional.of(testMeetup))

        val exception = assertThrows(PermissionDeniedException::class.java) {
            meetupService.updateMeetup(testMeetupId, otherUserId, updateRequest)
        }

        assertTrue(exception.message?.contains("update meetup") == true)

        verify(meetupRepository).findById(testMeetupId)
    }

    @Test
    fun `findMeetupByUuid returns meetup when it exists`() {
        whenever(meetupRepository.findById(testMeetupId)).thenReturn(Optional.of(testMeetup))

        val result = meetupService.findMeetupByUuid(testMeetupId)

        assertNotNull(result)
        assertEquals(testMeetupId, result?.uuid)

        verify(meetupRepository).findById(testMeetupId)
    }

    @Test
    fun `findMeetupByUuid returns null when meetup does not exist`() {
        whenever(meetupRepository.findById(testMeetupId)).thenReturn(Optional.empty())

        val result = meetupService.findMeetupByUuid(testMeetupId)

        assertNull(result)

        verify(meetupRepository).findById(testMeetupId)
    }

    @Test
    fun `getDiscoverMeetups returns public meetups`() {
        val pageable = PageRequest.of(0, 10)
        val meetups = listOf(testMeetup)
        val meetupsPage = PageImpl(meetups, pageable, meetups.size.toLong())

        whenever(meetupRepository.findNextMeetups(pageable)).thenReturn(meetupsPage)

        val result = meetupService.getDiscoverMeetups(pageable)

        assertNotNull(result)
        assertEquals(1, result.totalElements)
        assertEquals(testMeetupId, result.content[0].uuid)

        verify(meetupRepository).findNextMeetups(pageable)
    }

    @Test
    fun `getGroupMeetupsByStatus returns meetups for public group`() {
        val pageable = PageRequest.of(0, 10)
        val meetups = listOf(testMeetup)
        val meetupsPage = PageImpl(meetups, pageable, meetups.size.toLong())

        whenever(groupService.getGroupByUuid(testGroupId)).thenReturn(testGroup)
        whenever(meetupRepository.findGroupMeetupsByStatus(testGroupId, MeetupStatus.WAITING, pageable))
            .thenReturn(meetupsPage)

        val result = meetupService.getGroupMeetupsByStatus(testUserId, testGroupId, MeetupStatus.WAITING, pageable)

        assertNotNull(result)
        assertEquals(1, result.totalElements)
        assertEquals(testMeetupId, result.content[0].uuid)

        verify(groupService).getGroupByUuid(testGroupId)
        verify(meetupRepository).findGroupMeetupsByStatus(testGroupId, MeetupStatus.WAITING, pageable)
    }

    @Test
    fun `getGroupMeetupsByStatus verifies membership for private group`() {
        val pageable = PageRequest.of(0, 10)
        val meetups = listOf(testMeetup)
        val meetupsPage = PageImpl(meetups, pageable, meetups.size.toLong())
        val privateGroup = testGroup.copy(isPrivate = true)

        whenever(groupService.getGroupByUuid(testGroupId)).thenReturn(privateGroup)
        whenever(meetupRepository.findGroupMeetupsByStatus(testGroupId, MeetupStatus.WAITING, pageable))
            .thenReturn(meetupsPage)

        val result = meetupService.getGroupMeetupsByStatus(testUserId, testGroupId, MeetupStatus.WAITING, pageable)

        assertNotNull(result)
        assertEquals(1, result.totalElements)

        verify(groupService).getGroupByUuid(testGroupId)
        verify(groupService).getUserMembershipOrThrow(testUserId, testGroupId)
        verify(meetupRepository).findGroupMeetupsByStatus(testGroupId, MeetupStatus.WAITING, pageable)
    }

    @Test
    fun `getUserMeetups returns meetups hosted by user`() {
        val pageable = PageRequest.of(0, 10)
        val meetups = listOf(testMeetup)
        val meetupsPage = PageImpl(meetups, pageable, meetups.size.toLong())

        whenever(userService.findUserById(testUserId)).thenReturn(testUser)
        whenever(meetupRepository.findUserHostMeetups(testUserId, pageable)).thenReturn(meetupsPage)

        val result = meetupService.getUserMeetups(testUserId, pageable)

        assertNotNull(result)
        assertEquals(1, result.totalElements)
        assertEquals(testMeetupId, result.content[0].uuid)

        verify(userService).findUserById(testUserId)
        verify(meetupRepository).findUserHostMeetups(testUserId, pageable)
    }

    @Test
    fun `getParticipants returns participants for public group meetup`() {
        val pageable = PageRequest.of(0, 10)
        val participants = listOf(testUser)
        val participantsPage = PageImpl(participants, pageable, participants.size.toLong())

        whenever(meetupRepository.findById(testMeetupId)).thenReturn(Optional.of(testMeetup))
        whenever(groupService.getGroupByUuid(testGroupId)).thenReturn(testGroup)
        whenever(userMeetupRepository.findAllByMeetupUuid(testMeetupId, pageable)).thenReturn(participantsPage)

        val result = meetupService.getParticipants(testUserId, testMeetupId, pageable)

        assertNotNull(result)
        assertEquals(1, result.totalElements)
        assertEquals(testUserId, result.content[0].uuid)

        verify(meetupRepository).findById(testMeetupId)
        verify(groupService).getGroupByUuid(testGroupId)
        verify(userMeetupRepository).findAllByMeetupUuid(testMeetupId, pageable)
    }

    @Test
    fun `getParticipants verifies membership for private group meetup`() {
        val pageable = PageRequest.of(0, 10)
        val participants = listOf(testUser)
        val participantsPage = PageImpl(participants, pageable, participants.size.toLong())
        val privateGroup = testGroup.copy(isPrivate = true)

        whenever(meetupRepository.findById(testMeetupId)).thenReturn(Optional.of(testMeetup))
        whenever(groupService.getGroupByUuid(testGroupId)).thenReturn(privateGroup)
        whenever(userMeetupRepository.findByUserUuidAndMeetupUuid(testUserId, testMeetupId)).thenReturn(testUserMeetup)
        whenever(userMeetupRepository.findAllByMeetupUuid(testMeetupId, pageable)).thenReturn(participantsPage)

        val result = meetupService.getParticipants(testUserId, testMeetupId, pageable)

        assertNotNull(result)
        assertEquals(1, result.totalElements)

        verify(meetupRepository).findById(testMeetupId)
        verify(groupService).getGroupByUuid(testGroupId)
        verify(userMeetupRepository).findByUserUuidAndMeetupUuid(testUserId, testMeetupId)
        verify(userMeetupRepository).findAllByMeetupUuid(testMeetupId, pageable)
    }

    @Test
    fun `findNearbyMeetups returns meetups within radius`() {
        val latitude = 40.0
        val longitude = -3.0
        val radiusInKm = 10.0
        val pageable = PageRequest.of(0, 10)
        val meetups = listOf(testMeetup)
        val meetupsPage = PageImpl(meetups, pageable, meetups.size.toLong())

        whenever(meetupRepository.findNearbyMeetups(latitude, longitude, radiusInKm, pageable))
            .thenReturn(meetupsPage)

        val result = meetupService.findNearbyMeetups(latitude, longitude, radiusInKm, pageable)

        assertNotNull(result)
        assertEquals(1, result.totalElements)
        assertEquals(testMeetupId, result.content[0].uuid)

        verify(meetupRepository).findNearbyMeetups(latitude, longitude, radiusInKm, pageable)
    }
}