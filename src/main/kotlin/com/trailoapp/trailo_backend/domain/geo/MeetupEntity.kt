package com.trailoapp.trailo_backend.domain.geo

import com.trailoapp.trailo_backend.domain.enum.geo.MeetupStatus
import com.trailoapp.trailo_backend.domain.enum.geo.TrailDifficulty
import jakarta.persistence.*
import org.hibernate.annotations.ColumnTransformer
import org.hibernate.annotations.CreationTimestamp
import org.locationtech.jts.geom.Point
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID


@Entity
@Table(name = "meetup", schema = "geo")
data class MeetupEntity(
    @Id
    @Column
    val uuid: UUID = UUID.randomUUID(),

    @Column(name = "host_id", nullable = true)
    val host: UUID,

    @Column(name = "group_id", nullable = false)
    val group: UUID,

    @Column(nullable = false)
    var title: String,

    @Column(nullable = true)
    var description: String? = null,

    @Column(name = "meetup_picture", nullable = true)
    var meetupPicture: String? = null,

    @Column(name = "max_participants")
    var maxParticipants: Short? = Short.MAX_VALUE,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @ColumnTransformer(
        read = "difficulty::text",
        write = "?::geo.difficulty"
    )
    var difficulty: TrailDifficulty,

    @OneToMany(fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name ="meetup_id", nullable = false)
    var terrainTypes: MutableSet<MeetupTerrainTypeEntity> = mutableSetOf(),

    @Column(name = "distance_km", nullable = false, precision = 8, scale = 2)
    var distanceKm: BigDecimal,

    @Column(name = "estimated_duration_in_min", nullable = false)
    var estimatedDurationInMin: Long,

    @Column(name = "meeting_time", nullable = false)
    var meetingTime: OffsetDateTime,

    @Column(name = "meeting_point", nullable = false, columnDefinition = "geography(Point, 4326)")
    var meetingPoint: Point,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @ColumnTransformer(
        read = "status::text",
        write = "?::geo.meetup_status"
    )
    var status: MeetupStatus = MeetupStatus.WAITING,

    @CreationTimestamp
    @Column(name = "creation_date", nullable = false)
    val creationDate: OffsetDateTime = OffsetDateTime.now()
    )
