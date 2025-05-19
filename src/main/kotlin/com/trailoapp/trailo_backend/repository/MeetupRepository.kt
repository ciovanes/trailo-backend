package com.trailoapp.trailo_backend.repository

import com.trailoapp.trailo_backend.domain.enum.MeetupStatus
import com.trailoapp.trailo_backend.domain.enum.TerrainType
import com.trailoapp.trailo_backend.domain.enum.TrailDifficulty
import com.trailoapp.trailo_backend.domain.geo.MeetupEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.util.UUID

@Repository
interface MeetupRepository: JpaRepository<MeetupEntity, UUID> {

    @Query(value =
        """
            SELECT m FROM MeetupEntity m
            JOIN GroupEntity g ON m.group = g.uuid
            WHERE g.isPrivate = false
            AND m.status = 'WAITING'
        """
    )
    fun findNextMeetups(pageable: Pageable): Page<MeetupEntity>


    @Query(value =
        """
            SELECT m FROM MeetupEntity m
            JOIN GroupEntity g ON m.group = g.uuid
            WHERE g.uuid = :groupId
            AND m.status = :status
        """
    )
    fun findGroupMeetupsByStatus(groupId: UUID, status: MeetupStatus, pageable: Pageable): Page<MeetupEntity>

    @Query(value =
        """
            SELECT m FROM MeetupEntity m
            WHERE m.host = :userId
        """
    )
    fun findUserHostMeetups(userId: UUID, pageable: Pageable): Page<MeetupEntity>

    fun findByTitleContainingIgnoreCase(query: String, pageable: Pageable): Page<MeetupEntity>

    fun findByDifficulty(difficulty: TrailDifficulty, pageable: Pageable): Page<MeetupEntity>

    fun findByDistanceKm(distance: BigDecimal, pageable: Pageable): Page<MeetupEntity>

    @Query(value =
        """
            SELECT DISTINCT m FROM MeetupEntity m
            JOIN m.terrainTypes tt
            WHERE tt.terrainType = :terrainType
        """
    )
    fun findByTerrainTypesIn(terrainType: TerrainType, pageable: Pageable): Page<MeetupEntity>

    @Query(value =
        """
            SELECT * FROM geo.meetup m
            WHERE ST_DWithin(
                m.meeting_point,
                ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                :radiusInKm * 1000
            )
        """, nativeQuery = true
    )
    fun findNearbyMeetups(latitude: Double, longitude: Double, radiusInKm: Double, pageable: Pageable): Page<MeetupEntity>
}