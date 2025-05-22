package com.trailoapp.trailo_backend.controller

import com.trailoapp.trailo_backend.domain.core.UserEntity
import com.trailoapp.trailo_backend.dto.common.response.ErrorResponse
import com.trailoapp.trailo_backend.dto.common.response.PageResponse
import com.trailoapp.trailo_backend.dto.meetup.request.CreateMeetupRequest
import com.trailoapp.trailo_backend.dto.meetup.request.UpdateMeetupRequest
import com.trailoapp.trailo_backend.dto.meetup.response.MeetupResponse
import com.trailoapp.trailo_backend.dto.meetup.response.UserMeetupResponse
import com.trailoapp.trailo_backend.dto.user.response.UserResponse
import com.trailoapp.trailo_backend.service.MeetupService
import com.trailoapp.trailo_backend.service.UserMeetupService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
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
@Tag(name = "Meetups")
class MeetupController(
    private val meetupService: MeetupService,
    private val userMeetupService: UserMeetupService
) {

    // ===== BASIC MEETUP OPERATIONS =====

    @PostMapping("/create")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Create a new meetup",
        description = "Create a new meetup with the given information."
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "201",
            description = "Meetup created successfully",
            content = [Content(schema = Schema(implementation = MeetupResponse::class))]
        ),
        ApiResponse(
            responseCode = "400",
            description = "Invalid input data or validation errors",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "403",
            description = "User not member of the group",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Group or user not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun createMeetup(
       @RequestBody request: CreateMeetupRequest,
       @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<MeetupResponse> {
       val meetup = meetupService.createMeetup(user.uuid, request)

       return ResponseEntity
           .status(HttpStatus.CREATED)
           .body(MeetupResponse.fromEntity(meetup))
    }

    @GetMapping("/{meetupId}")
    @Operation(
        summary = "Get meetup details"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "Meetup details retrieved successfully",
            content = [Content(schema = Schema(implementation = MeetupResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Meetup not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun getMeetup(
        @Parameter(description = "Meetup UUID") @PathVariable meetupId: UUID
    ): ResponseEntity<MeetupResponse> {
        val meetup = meetupService.findMeetupByUuid(meetupId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(MeetupResponse.fromEntity(meetup))
    }

    @PatchMapping("/{meetupId}")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Update meetup details"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "Meetup updated successfully",
            content = [Content(schema = Schema(implementation = MeetupResponse::class))]
        ),
        ApiResponse(
            responseCode = "400",
            description = "Invalid input data",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "403",
            description = "User is not the meetup host",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Meetup not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun updateMeetup(
        @Parameter(description = "Meetup UUID") @PathVariable meetupId: UUID,
        @RequestBody request: UpdateMeetupRequest,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<MeetupResponse> {
        val group = meetupService.updateMeetup( meetupId, user.uuid, request)

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(MeetupResponse.fromEntity(group))
    }

    @DeleteMapping("/{meetupId}")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Delete meetup"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "204",
            description = "Meetup deleted successfully"
        ),
        ApiResponse(
            responseCode = "401",
            description = "User not authenticated",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "403",
            description = "User is not the meetup host",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Meetup not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun deleteMeetup(
        @Parameter(description = "Meetup UUID") @PathVariable meetupId: UUID,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<Unit> {
        meetupService.deleteMeetup(meetupId, user.uuid)

        return ResponseEntity
            .status(HttpStatus.NO_CONTENT)
            .build()
    }

    // ===== SEARCH AND LISTING =====

    @GetMapping("/search")
    @Operation(
        summary = "Search meetups"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "Search results retrieved successfully",
            content = [Content(schema = Schema(implementation = PageResponse::class))]
        ),
        ApiResponse(
            responseCode = "400",
            description = "Invalid search parameters",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Invalid difficulty or terrain type",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun searchMeetups(
        @Parameter(description = "Query") @RequestParam(required = false, defaultValue = "") query: String,
        @Parameter(description = "Page number") @RequestParam(required = false, defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20") size: Int,
        @Parameter(description = "Search by title, difficulty, distance or terrain")
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

    @GetMapping("/discover")
    @Operation(
        summary = "Discover meetups",
        description = "Retrieves upcoming public meetups for discovery."
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "Meetups retrieved successfully",
            content = [Content(schema = Schema(implementation = PageResponse::class))]
        )
    ])
    fun getDiscoverMeetups(
        @Parameter(description = "Page number") @RequestParam(required = false, defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20") size: Int,
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

    @GetMapping("/nearby")
    @Operation(
        summary = "Find nearby meetups",
        description = "Finds meetups within given coordinates and radius in km using geospatial search"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "Nearby meetups retrieved successfully",
            content = [Content(schema = Schema(implementation = PageResponse::class))]
        ),
        ApiResponse(
            responseCode = "400",
            description = "Invalid coordinates or radius",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun getNearbyMeetups(
        @Parameter(description = "Latitude") @RequestParam latitude: Double,
        @Parameter(description = "Longitude") @RequestParam longitude: Double,
        @Parameter(description = "Radius in km") @RequestParam(defaultValue = "10.0") radiusInKm: Double,
        @Parameter(description = "Page number") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") size: Int
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

    // ===== MEETUP ACTIONS =====

    @PostMapping("{meetupId}/join")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Join a meetup",
        description = "Join a meetup as a participant. For private group meetups, the user needs to be in the group"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "Joined meetup successfully",
            content = [Content(schema = Schema(implementation = UserMeetupResponse::class))]
        ),
        ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "400",
            description = "Host cannot join own meetup or already joined",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "403",
            description = "User not member of private group",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Meetup not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
    ])
    fun joinMeetup(
        @Parameter(description = "Meetup UUID") @PathVariable meetupId: UUID,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<UserMeetupResponse> {
        val userMeetup = userMeetupService.joinMeetup(user.uuid, meetupId)

        return ResponseEntity.status(HttpStatus.OK).body(UserMeetupResponse.fromEntity(userMeetup))
    }

    @DeleteMapping("{meetupId}/leave")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Leave a meetup",
        description = "Leave a meetup where you are a participant"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "204",
            description = "Left meetup successfully"
        ),
        ApiResponse(
            responseCode = "401",
            description = "User not authenticated",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Meetup not found or user not participant",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun leaveMeetup(
        @Parameter(description = "Meetup UUID") @PathVariable meetupId: UUID,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<Unit> {
        userMeetupService.leaveMeetup(user.uuid, meetupId)

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    @GetMapping("{meetupId}/participants")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Get meetup participants",
        description = "Get a list of participants for a meetup. If the meetup's group is private, the user needs to be" +
                "a member of the group to access to the participants list"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "Participants retrieved successfully",
            content = [Content(schema = Schema(implementation = PageResponse::class))]
        ),
        ApiResponse(
            responseCode = "401",
            description = "User not authenticated",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "403",
            description = "User cannot view participants of private group meetup",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Meetup not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun getMeetupParticipants(
        @Parameter(description = "Meetup UUID") @PathVariable meetupId: UUID,
        @Parameter(description = "Page number") @RequestParam(required = false, defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20") size: Int,
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
}