package com.trailoapp.trailo_backend.dto.meetup.request

import com.trailoapp.trailo_backend.domain.enum.geo.TerrainType
import com.trailoapp.trailo_backend.domain.enum.geo.TrailDifficulty
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class CreateMeetupRequest (
    val group: UUID,
    val title: String,
    val description: String? = null,
    val meetupPicture: String? = null,
    val maxParticipants: Short? = Short.MAX_VALUE,
    val difficulty: TrailDifficulty,
    val terrainTypes: List<TerrainType> = emptyList(),
    val distanceKm: BigDecimal,
    val estimatedDurationInMin: Long,
    val meetingTime: OffsetDateTime,
    val meetingPoint: PointDto
)