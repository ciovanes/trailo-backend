package com.trailoapp.trailo_backend.controller

import com.trailoapp.trailo_backend.domain.core.UserEntity
import com.trailoapp.trailo_backend.dto.common.response.PageResponse
import com.trailoapp.trailo_backend.dto.meetup.request.CreateMeetupRequest
import com.trailoapp.trailo_backend.dto.meetup.request.UpdateMeetupRequest
import com.trailoapp.trailo_backend.dto.meetup.response.MeetupResponse
import com.trailoapp.trailo_backend.dto.meetup.response.UserMeetupResponse
import com.trailoapp.trailo_backend.dto.user.response.UserResponse
import com.trailoapp.trailo_backend.service.MeetupService
import com.trailoapp.trailo_backend.service.UserMeetupService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/meetups")
class MeetupController(
    private val meetupService: MeetupService,
    private val userMeetupService: UserMeetupService
) {


    @PostMapping("/create")
   fun createMeetup(
       @RequestBody request: CreateMeetupRequest,
       @AuthenticationPrincipal user: UserEntity
   ): ResponseEntity<MeetupResponse> {
       val meetup = meetupService.createMeetup(user.uuid, request)

       return ResponseEntity
           .status(HttpStatus.CREATED)
           .body(MeetupResponse.fromEntity(meetup))
   }

    @DeleteMapping("/{meetupId}")
    fun deleteMeetup(
        @PathVariable meetupId: UUID,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<Unit> {
        meetupService.deleteMeetup(meetupId, user.uuid)

        return ResponseEntity
            .status(HttpStatus.NO_CONTENT)
            .build()
    }

    @PatchMapping("/{meetupId}")
    fun updateMeetup(
        @PathVariable meetupId: UUID,
        @RequestBody request: UpdateMeetupRequest,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<MeetupResponse> {
        val group = meetupService.updateMeetup( meetupId, user.uuid, request)

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(MeetupResponse.fromEntity(group))
    }

    @GetMapping("/{meetupId}")
    fun getMeetup(
        @PathVariable meetupId: UUID
    ): ResponseEntity<MeetupResponse> {
        val meetup = meetupService.findMeetupByUuid(meetupId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(MeetupResponse.fromEntity(meetup))
    }

    @GetMapping("/discover")
    fun getDiscoverMeetups(
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int,
    ): ResponseEntity<PageResponse<MeetupResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "meetingTime"))
        val meetups = meetupService.getDiscoverMeetups(pageable)

        val response = PageResponse(
            content = meetups.content.map { MeetupResponse.fromEntity(it) },
            pageNumber = page,
            pageSize = size,
            totalElements = meetups.totalElements,
            totalPages = meetups.totalPages,
            isLast = meetups.isLast
        )

        return ResponseEntity.status(HttpStatus.OK).body(response)
    }

    @PostMapping("{meetupId}/join")
    fun joinMeetup(
        @PathVariable meetupId: UUID,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<UserMeetupResponse> {
        val userMeetup = userMeetupService.joinMeetup(user.uuid, meetupId)

        return ResponseEntity.status(HttpStatus.OK).body(UserMeetupResponse.fromEntity(userMeetup))
    }

    @DeleteMapping("{meetupId}/leave")
    fun leaveMeetup(
        @PathVariable meetupId: UUID,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<Unit> {
        userMeetupService.leaveMeetup(user.uuid, meetupId)

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    @GetMapping("{meetupId}/participants")
    fun getMeetupParticipants(
        @PathVariable meetupId: UUID,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<PageResponse<UserResponse>> {
        val pageable = PageRequest.of(page, size)

        val participantsPage = meetupService.getParticipants(user.uuid, meetupId, pageable)

        val response = PageResponse(
            content = participantsPage.content.map { UserResponse.fromUser(it) },
            pageNumber = page,
            pageSize = size,
            totalElements = participantsPage.totalElements,
            totalPages = participantsPage.totalPages,
            isLast = participantsPage.isLast
        )

        return ResponseEntity.status(HttpStatus.OK).body(response)
    }

    @GetMapping("/search")
    fun searchMeetups(
        @RequestParam(required = false, defaultValue = "") query: String,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int,
        @RequestParam(required = false, defaultValue = "title") searchBy: String
    ): ResponseEntity<PageResponse<MeetupResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "meetingTime"))

        val meetupPage = when (searchBy.lowercase()) {
            "title" -> meetupService.searchByTitle(query, pageable)
            "difficulty" -> meetupService.searchByDifficulty(query, pageable)
            "distance" -> meetupService.searchByDistance(query.toBigDecimalOrNull(), pageable)
            "terrain" -> meetupService.searchByTerrainType(query, pageable)
            else -> meetupService.searchByTitle(query, pageable)
        }

        val response = PageResponse(
            content = meetupPage.content.map { MeetupResponse.fromEntity(it) },
            pageNumber = page,
            pageSize = size,
            totalElements = meetupPage.totalElements,
            totalPages = meetupPage.totalPages,
            isLast = meetupPage.isLast
        )

        return ResponseEntity.status(HttpStatus.OK).body(response)
    }

    @GetMapping("/nearby")
    fun getNearbyMeetups(
        @RequestParam latitude: Double,
        @RequestParam longitude: Double,
        @RequestParam(defaultValue = "10.0") radiusInKm: Double,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PageResponse<MeetupResponse>> {
        val pageable = PageRequest.of(page, size)

        val meetups = meetupService.findNearbyMeetups(
            latitude = latitude,
            longitude = longitude,
            radiusInKm = radiusInKm,
            pageable = pageable
        )

        val response = PageResponse(
            content = meetups.content.map { MeetupResponse.fromEntity(it) },
            pageNumber = page,
            pageSize = size,
            totalElements = meetups.totalElements,
            totalPages = meetups.totalPages,
            isLast = meetups.isLast
        )

        return ResponseEntity.status(HttpStatus.OK).body(response)
    }

}