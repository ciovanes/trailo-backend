package com.trailoapp.trailo_backend.dto.meetup.response

import com.trailoapp.trailo_backend.domain.core.GroupEntity
import com.trailoapp.trailo_backend.domain.core.UserEntity
import com.trailoapp.trailo_backend.domain.enum.geo.MeetupStatus
import com.trailoapp.trailo_backend.domain.enum.geo.TerrainType
import com.trailoapp.trailo_backend.domain.enum.geo.TrailDifficulty
import com.trailoapp.trailo_backend.domain.geo.MeetupEntity
import com.trailoapp.trailo_backend.dto.meetup.request.PointDto
import org.locationtech.jts.geom.Point
import java.math.BigDecimal
import java.time.Duration
import java.time.OffsetDateTime
import java.util.UUID

data class MeetupResponse(
    val uuid: UUID,
    val host: UUID,
    val group: UUID,
    val title: String,
    val description: String? = null,
    val meetupPicture: String? = null,
    val maxParticipants: Short? = Short.MAX_VALUE,
    val difficulty: TrailDifficulty,
    val terrainTypes: List<TerrainType>,
    val distanceKm: BigDecimal,
    val estimatedDurationInMin: Long,
    val meetingTime: OffsetDateTime,
    val meetingPoint: PointDto,
    val status: MeetupStatus,
    val creationDate: OffsetDateTime
) {
    companion object {
        fun fromEntity(meetup: MeetupEntity): MeetupResponse {
            val latitude = meetup.meetingPoint.y
            val longitude = meetup.meetingPoint.x

            val terrainTypesList = meetup.terrainTypes.map { it.terrainType }

            return MeetupResponse(
                uuid = meetup.uuid,
                host = meetup.host,
                group = meetup.group,
                title = meetup.title,
                description = meetup.description,
                meetupPicture = meetup.meetupPicture,
                maxParticipants = meetup.maxParticipants,
                difficulty = meetup.difficulty,
                terrainTypes = terrainTypesList,
                distanceKm = meetup.distanceKm,
                estimatedDurationInMin = meetup.estimatedDurationInMin,
                meetingTime = meetup.meetingTime,
                meetingPoint = PointDto(
                    type = "Point",
                    coordinates = listOf(latitude, longitude)
                ),
                status = meetup.status,
                creationDate = meetup.creationDate
            )
        }
    }
}
