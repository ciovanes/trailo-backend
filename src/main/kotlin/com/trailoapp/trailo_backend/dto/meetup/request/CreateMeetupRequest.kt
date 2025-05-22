package com.trailoapp.trailo_backend.dto.meetup.request

import com.trailoapp.trailo_backend.domain.enum.geo.TerrainType
import com.trailoapp.trailo_backend.domain.enum.geo.TrailDifficulty
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class CreateMeetupRequest (
    val group: UUID,

    val title: String,

    val description: String? = null,

    val meetupPicture: String? = null,

    val maxParticipants: Short? = Short.MAX_VALUE,

    @Schema(allowableValues = ["BEGINNER", "INTERMEDIATE", "ADVANCED"])
    val difficulty: TrailDifficulty,

    @Schema(allowableValues = ["UNSPECIFIED", "ROCKY", "MUDDY", "SANDY", "FOREST", "MOUNTAIN", "DESERT", "RIVER", "SNOW",
        "GRAVEL", "CLAY", "VOLCANIC"])
    val terrainTypes: List<TerrainType> = emptyList(),

    val distanceKm: BigDecimal,

    val estimatedDurationInMin: Long,

    val meetingTime: OffsetDateTime,

    val meetingPoint: PointDto
)