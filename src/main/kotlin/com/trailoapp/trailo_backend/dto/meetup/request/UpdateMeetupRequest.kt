package com.trailoapp.trailo_backend.dto.meetup.request

import com.trailoapp.trailo_backend.domain.enum.geo.MeetupStatus
import com.trailoapp.trailo_backend.domain.enum.geo.TerrainType
import com.trailoapp.trailo_backend.domain.enum.geo.TrailDifficulty
import java.math.BigDecimal
import java.time.OffsetDateTime

data class UpdateMeetupRequest (
    val title: String?,
    val description: String?,
    val meetupPicture: String?,
    val maxParticipants: Short?,
    val difficulty: TrailDifficulty?,
    val terrainTypes: List<TerrainType>?,
    val distanceKm: BigDecimal?,
    val estimatedDurationInMin: Long?,
    val meetingTime: OffsetDateTime?,
    val meetingPoint: PointDto?,
    val status: MeetupStatus?
)