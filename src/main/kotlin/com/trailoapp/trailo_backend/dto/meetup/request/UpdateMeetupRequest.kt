package com.trailoapp.trailo_backend.dto.meetup.request

import com.trailoapp.trailo_backend.domain.enum.geo.MeetupStatus
import com.trailoapp.trailo_backend.domain.enum.geo.TerrainType
import com.trailoapp.trailo_backend.domain.enum.geo.TrailDifficulty
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.OffsetDateTime

data class UpdateMeetupRequest (
    val title: String?,

    val description: String?,

    val meetupPicture: String?,

    val maxParticipants: Short?,

    @Schema(allowableValues = ["BEGINNER", "INTERMEDIATE", "ADVANCED"])
    val difficulty: TrailDifficulty?,

    @Schema(allowableValues = ["UNSPECIFIED", "ROCKY", "MUDDY", "SANDY", "FOREST", "MOUNTAIN", "DESERT", "RIVER", "SNOW",
        "GRAVEL", "CLAY", "VOLCANIC"])
    val terrainTypes: List<TerrainType>?,

    val distanceKm: BigDecimal?,

    val estimatedDurationInMin: Long?,

    val meetingTime: OffsetDateTime?,

    val meetingPoint: PointDto?,

    val status: MeetupStatus?
)