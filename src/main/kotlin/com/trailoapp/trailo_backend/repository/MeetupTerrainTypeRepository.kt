package com.trailoapp.trailo_backend.repository

import com.trailoapp.trailo_backend.domain.geo.MeetupTerrainTypeEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface MeetupTerrainTypeRepository: JpaRepository<MeetupTerrainTypeEntity, UUID> {

//    @Modifying
//    @Query(value = "DELETE FROM MeetupTerrainTypeEntity m WHERE m.meetupId = :meetupId")
//    fun deleteAllByMeetupId(meetupId: UUID)

//    fun findAllByMeetupId(uuid: UUID)
}