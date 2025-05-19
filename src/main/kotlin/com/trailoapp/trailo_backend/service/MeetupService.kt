package com.trailoapp.trailo_backend.service

import com.trailoapp.trailo_backend.domain.core.UserEntity
import com.trailoapp.trailo_backend.domain.enum.MeetupStatus
import com.trailoapp.trailo_backend.domain.enum.TerrainType
import com.trailoapp.trailo_backend.domain.enum.TrailDifficulty
import com.trailoapp.trailo_backend.domain.geo.MeetupEntity
import com.trailoapp.trailo_backend.domain.geo.MeetupTerrainTypeEntity
import com.trailoapp.trailo_backend.domain.social.UserMeetupEntity
import com.trailoapp.trailo_backend.dto.meetup.request.CreateMeetupRequest
import com.trailoapp.trailo_backend.dto.meetup.request.PointDto
import com.trailoapp.trailo_backend.dto.meetup.request.UpdateMeetupRequest
import com.trailoapp.trailo_backend.exception.definitions.PermissionDeniedException
import com.trailoapp.trailo_backend.exception.definitions.ResourceNotFoundException
import com.trailoapp.trailo_backend.repository.MeetupRepository
import com.trailoapp.trailo_backend.repository.UserMeetupRepository
import jakarta.transaction.Transactional
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class MeetupService(
    private val groupService: GroupService,
    private val meetupRepository: MeetupRepository,
    private val geometryFactory: GeometryFactory,
    private val userService: UserService,
    private val userMeetupRepository: UserMeetupRepository
) {

    @Transactional
    fun createMeetup(userId: UUID, request: CreateMeetupRequest): MeetupEntity {
        val group = groupService.getGroupByUuid(request.group)

        val user = userService.findUserById(userId)
            ?: throw ResourceNotFoundException("user", userId)

        groupService.getUserMembershipOrThrow(userId, group.uuid)

        val point = generatePointFromDto(request.meetingPoint)

        val meetup = meetupRepository.save(
            MeetupEntity(
                host = userId,
                group = group.uuid,
                title = request.title,
                description = request.description,
                meetupPicture = request.meetupPicture,
                maxParticipants = request.maxParticipants,
                difficulty = request.difficulty,
                distanceKm = request.distanceKm,
                estimatedDurationInMin = request.estimatedDurationInMin,
                meetingTime = request.meetingTime,
                meetingPoint = point,
                status = MeetupStatus.WAITING
            )
        )

        if (request.terrainTypes.isNotEmpty()) {
            request.terrainTypes.forEach { terrainType ->
                meetup.terrainTypes.add(
                    MeetupTerrainTypeEntity(
                        terrainType = terrainType
                    )
                )
            }

            meetupRepository.save(meetup)
        }

        userMeetupRepository.save(
            UserMeetupEntity(
                user = user,
                meetup = meetup
            )
        )

        return meetup
    }

    @Transactional
    fun deleteMeetup(meetupId: UUID, userId: UUID) {
        val meetup = meetupRepository.findById(meetupId)
            .orElseThrow { throw ResourceNotFoundException("meetup", meetupId) }

        if (meetup.host != userId) {
            throw PermissionDeniedException("delete meetup", "meetup", meetupId )
        }

        meetupRepository.delete(meetup)
    }

    @Transactional
    fun updateMeetup(meetupId: UUID, userId: UUID, request: UpdateMeetupRequest): MeetupEntity {
        val meetup = meetupRepository.findById(meetupId)
            .orElseThrow { throw ResourceNotFoundException("meetup", meetupId) }

        if (meetup.host != userId) {
            throw PermissionDeniedException("update meetup", "meetup", meetupId)
        }

        request.apply {
            title?.let { meetup.title = it }
            description?.let { meetup.description = it }
            meetupPicture?.let { meetup.meetupPicture = it }
            maxParticipants?.let { meetup.maxParticipants = it }
            difficulty?.let { meetup.difficulty = it }
            distanceKm?.let { meetup.distanceKm = it }
            estimatedDurationInMin?.let { meetup.estimatedDurationInMin = it }
            meetingTime?.let { meetup.meetingTime = it }
            meetingPoint?.let { meetup.meetingPoint = generatePointFromDto(it) }
            status?.let { meetup.status = it }
        }

        request.terrainTypes?.let { newTerrainTypes ->
            val newUniqueTerrainTypes = newTerrainTypes.toSet()
            val currentTerrainTypes = meetup.terrainTypes.map { it.terrainType }.toSet()

            val typesToDelete = meetup.terrainTypes.filter {
                it.terrainType !in newUniqueTerrainTypes
            }

            typesToDelete.forEach {
                meetup.terrainTypes.remove(it)
            }

            val typesToAdd = newUniqueTerrainTypes.filter {
                it !in currentTerrainTypes
            }
            typesToAdd.forEach { terrainType ->
                meetup.terrainTypes.add(
                    MeetupTerrainTypeEntity(
                        terrainType = terrainType
                    )
                )
            }
        }

        return meetupRepository.save(meetup)
    }

    fun findMeetupByUuid(meetupId: UUID): MeetupEntity? {
        return meetupRepository.findByIdOrNull(meetupId)
    }

    fun getDiscoverMeetups(pageable: Pageable): Page<MeetupEntity> {
        return meetupRepository.findNextMeetups(pageable)
    }

    fun findGroupMeetups(userId: UUID, groupId: UUID, status: MeetupStatus, pageable: Pageable): Page<MeetupEntity> {
        val group = groupService.getGroupByUuid(groupId)

        if (group.isPrivate) {
            groupService.getUserMembershipOrThrow(userId, groupId)
        }

        return meetupRepository.findGroupMeetupsByStatus(groupId, status, pageable)
    }

    fun getUserMeetups(userId: UUID, pageable: Pageable): Page<MeetupEntity> {
        val user = userService.findUserById(userId)
            ?: throw ResourceNotFoundException("user", userId)

        return meetupRepository.findUserHostMeetups(user.uuid, pageable)
    }

    fun getParticipants(userId: UUID, meetupId: UUID, pageable: Pageable): Page<UserEntity> {
        val meetup = meetupRepository.findById(meetupId)
            .orElseThrow { throw ResourceNotFoundException("meetup", meetupId) }

        val group = groupService.getGroupByUuid(meetup.group)

        if (group.isPrivate) {
            userMeetupRepository.findByUserUuidAndMeetupUuid(userId, meetup.uuid)
                ?: throw ResourceNotFoundException("user meetup", "$userId - ${meetup.uuid}")

        }

        return userMeetupRepository.findAllByMeetupUuid(meetup.uuid, pageable)
    }


    fun searchByTitle(query: String, pageable: Pageable): Page<MeetupEntity> {
        return meetupRepository.findByTitleContainingIgnoreCase(query, pageable)
    }

    fun searchByDifficulty(query: String, pageable: Pageable): Page<MeetupEntity> {
        val difficulty = try {
            TrailDifficulty.valueOf(query.uppercase())
        } catch (_: Exception) {
            throw ResourceNotFoundException("terrain type", query)
        }

        return meetupRepository.findByDifficulty(difficulty, pageable)
    }

    fun searchByDistance(distance: BigDecimal?, pageable: Pageable): Page<MeetupEntity> {
        return if (distance != null) {
            meetupRepository.findByDistanceKm(distance, pageable)
        } else {
            return Page.empty(pageable)
        }
    }

    fun searchByTerrainType(terrainTypeValue: String, pageable: Pageable): Page<MeetupEntity> {
        val terrainType = try {
            TerrainType.valueOf(terrainTypeValue.uppercase())
        } catch (_: Exception) {
            throw ResourceNotFoundException("terrain type", terrainTypeValue)
        }

        return meetupRepository.findByTerrainTypesIn(terrainType, pageable)
    }

    fun findNearbyMeetups(
        latitude: Double,
        longitude: Double,
        radiusInKm: Double,
        pageable: Pageable
    ): Page<MeetupEntity> {
        return meetupRepository.findNearbyMeetups(latitude, longitude, radiusInKm, pageable)
    }

    // ===== UTILITY METHODS =====

    private fun generatePointFromDto(pointDto: PointDto): Point {
        val latitude = pointDto.coordinates[0]
        val longitude= pointDto.coordinates[1]

        return geometryFactory.createPoint(Coordinate(longitude, latitude))
    }
}