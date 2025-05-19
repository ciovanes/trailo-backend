package com.trailoapp.trailo_backend.domain.geo

import com.trailoapp.trailo_backend.domain.enum.TerrainType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.ColumnTransformer
import java.util.UUID

@Entity
@Table(name = "meetup_terrain_type", schema = "geo")
data class MeetupTerrainTypeEntity(
    @Id
    @Column
    val uuid: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @ColumnTransformer(
        read = "terrain_type::text",
        write = "?::geo.terrain_type"
    )
    var terrainType: TerrainType,
)