package com.trailoapp.trailo_backend.controller

import com.trailoapp.trailo_backend.domain.enum.MembershipStatus
import com.trailoapp.trailo_backend.domain.social.GroupEntity
import com.trailoapp.trailo_backend.dto.common.response.PageResponse
import com.trailoapp.trailo_backend.dto.group.request.CreateGroupRequest
import com.trailoapp.trailo_backend.dto.group.request.UpdateStatusRequest
import com.trailoapp.trailo_backend.dto.group.response.GroupMemberResponse
import com.trailoapp.trailo_backend.dto.group.response.GroupResponse
import com.trailoapp.trailo_backend.dto.group.response.UserGroupResponse
import com.trailoapp.trailo_backend.dto.user.response.UserResponse
import com.trailoapp.trailo_backend.repository.GroupRepository
import com.trailoapp.trailo_backend.repository.UserGroupRepository
import com.trailoapp.trailo_backend.service.CognitoService
import com.trailoapp.trailo_backend.service.GroupService
import com.trailoapp.trailo_backend.service.UserService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/groups")
class GroupController(
    private val groupService: GroupService,
    private val userService: UserService
) {

    @PostMapping("/create")
    fun createGroup(
        @Valid @RequestBody request: CreateGroupRequest,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<GroupResponse> {
        val cognitoId = jwt.claims["sub"] as String
        val user = userService.findUserByCognitoId(cognitoId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        val group = groupService.createGroup(request, user)

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(GroupResponse.fromEntity(group))
    }

    @GetMapping
    fun getAllGroups(
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<PageResponse<GroupResponse>> {

        val pageable = PageRequest.of(page, size)
        val groupPage = groupService.getAllGroups(pageable)

        val response = PageResponse(
            content = groupPage.content.map { GroupResponse.fromEntity(it) },
            pageNumber = page,
            pageSize = size,
            totalElements = groupPage.totalElements,
            totalPages = groupPage.totalPages,
            isLast = groupPage.isLast,
        )

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(response)
    }

    @GetMapping("/id/{uuid}")
    fun getGroup(@PathVariable uuid: UUID): ResponseEntity<GroupResponse> {
        val group = groupService.findGroupByUuid(uuid)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(GroupResponse.fromEntity(group))
    }

    @GetMapping("/search")
    fun searchGroups(
        @RequestParam(required = false, defaultValue = "") query: String,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int,
        @RequestParam(required = false, defaultValue = "name") searchBy: String,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<PageResponse<GroupResponse>> {

        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"))

        val groupPage = when (searchBy.lowercase()) {
            "name" -> groupService.searchByName(query, pageable)
            "is_private" -> groupService.searchByIsPrivate(query.toBooleanStrict(), pageable)
            else -> groupService.searchByName(query, pageable)
        }

        val response = PageResponse(
            content = groupPage.content.map { GroupResponse.fromEntity(it) },
            pageNumber =  page,
            pageSize = size,
            totalElements = groupPage.totalElements,
            totalPages = groupPage.totalPages,
            isLast = groupPage.isLast
        )

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(response)
    }

    @DeleteMapping("/{groupId}")
    fun deleteGroup(
        @PathVariable groupId: UUID,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<Unit> {
        val cognitoId = jwt.claims["sub"] as String
        val user = userService.findUserByCognitoId(cognitoId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        groupService.deleteGroup(groupId, user.uuid)

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    @PostMapping("/join/{groupId}")
    fun joinGroup(
        @PathVariable groupId: UUID,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<UserGroupResponse> {
        val cognitoId = jwt.claims["sub"] as String
        val user = userService.findUserByCognitoId(cognitoId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        val userGroup = groupService.joinGroup(user, groupId)

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(UserGroupResponse.fromEntity(userGroup))
    }

    @PostMapping("/{groupId}/favorite")
    fun favoriteGroup(
        @PathVariable groupId: UUID,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<Map<String, Boolean>> {
        val cognitoId = jwt.claims["sub"] as String
        val user = userService.findUserByCognitoId(cognitoId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        groupService.toggleFavorite(user.uuid, groupId)

        val isFavorite = groupService.checkIsFavorite(user.uuid, groupId)

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(mapOf("isFavorite" to isFavorite))
    }

    /*
    Get all pending requests from a private group
     */
    @GetMapping("/{groupId}/requests")
    fun getPendingRequests(
        @PathVariable groupId: UUID,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<PageResponse<UserResponse>> {
        val cognitoId = jwt.claims["sub"] as String
        val user = userService.findUserByCognitoId(cognitoId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        val pageable = PageRequest.of(page, size)

        val pendingRequests = groupService.getPendingRequests(user.uuid, groupId, pageable)

        val response = PageResponse(
            content = pendingRequests.content.map { UserResponse.fromUser(it) },
            pageNumber = page,
            pageSize = size,
            totalElements = pendingRequests.totalElements,
            totalPages = pendingRequests.totalPages,
            isLast = pendingRequests.isLast
        )

        return ResponseEntity.status(HttpStatus.OK).body(response)
    }

    @PostMapping("/{groupId}/requests/{userId}/accept")
    fun acceptMemberRequest(
        @PathVariable groupId: UUID,
        @PathVariable userId: UUID,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<Unit> {
        val cognitoId = jwt.claims["sub"] as String
        val user = userService.findUserByCognitoId(cognitoId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        groupService.updateMembershipRequest(user.uuid, groupId, userId, MembershipStatus.ACCEPTED)

        return ResponseEntity.status(HttpStatus.OK).build()
    }

    @PostMapping("/{groupId}/requests/{userId}/reject")
    fun rejectMemberRequest(
        @PathVariable groupId: UUID,
        @PathVariable userId: UUID,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<Unit> {
        val cognitoId = jwt.claims["sub"] as String
        val user = userService.findUserByCognitoId(cognitoId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        groupService.updateMembershipRequest(user.uuid, groupId, userId, MembershipStatus.REJECTED)

        return ResponseEntity.status(HttpStatus.OK).build()
    }

    /*
    Get my groups
     */
    @GetMapping("/my")
    fun getMyGroups(
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<PageResponse<GroupResponse>> {
        val cognitoId = jwt.claims["sub"] as String
        val user = userService.findUserByCognitoId(cognitoId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        val pageable = PageRequest.of(page, size)

        val myGroups = groupService.getMyGroups(user.uuid, pageable)

        val response = PageResponse(
            content = myGroups.content.map { GroupResponse.fromEntity(it) },
            pageNumber = page,
            pageSize = size,
            totalElements = myGroups.totalElements,
            totalPages = myGroups.totalPages,
            isLast = myGroups.isLast
        )

        return ResponseEntity.status(HttpStatus.OK).body(response)
    }

    @GetMapping("/{groupId}/members")
    fun getMembers(
        @PathVariable groupId: UUID,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<GroupMemberResponse> {
        val cognitoId = jwt.claims["sub"] as String
        val user = userService.findUserByCognitoId(cognitoId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        val pageable = PageRequest.of(page, size)

        val response = groupService.getGroupMembers(user.uuid, groupId, pageable)

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(response)
    }

}