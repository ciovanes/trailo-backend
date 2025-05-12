package com.trailoapp.trailo_backend.repository

import com.trailoapp.trailo_backend.domain.core.UserEntity
import com.trailoapp.trailo_backend.domain.enum.GroupRoles
import com.trailoapp.trailo_backend.domain.social.GroupEntity
import com.trailoapp.trailo_backend.domain.social.UserGroupEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface UserGroupRepository: JpaRepository<UserGroupEntity, UUID> {

    @Query(value =
        """
            SELECT EXISTS(
                SELECT 1 FROM UserGroupEntity g
                WHERE g.user.uuid = :userId
                AND g.group.uuid = :groupId
                AND (g.role = 'LEADER' OR g.role = 'CO_LEADER')
            )
        """
    )
    fun userHavePermissions(userId: UUID, groupId: UUID): Boolean

    fun findByGroup_UuidAndUser_Uuid(groupId: UUID, userId: UUID): Optional<UserGroupEntity>

    @Query(value =
    """
        SELECT u FROM UserGroupEntity g
        JOIN g.user u
        WHERE g.group.uuid = :groupId
        AND g.status = 'PENDING'
    """)
    fun getPendingRequests(groupId: UUID, pageable: Pageable): Page<UserEntity>

    @Query(value =
    """
        SELECT g FROM GroupEntity g
        JOIN UserGroupEntity ug ON ug.group.uuid = g.uuid
        WHERE ug.user.uuid = :userId
        AND ug.status = 'ACCEPTED'
    """)
    fun findMyGroups(userId: UUID, pageable: Pageable): Page<GroupEntity>

    @Query(value =
    """
        SELECT COUNT(ug) FROM UserGroupEntity ug
        WHERE ug.group.uuid = :groupId
        AND ug.status = 'ACCEPTED'
    """)
    fun countAcceptedMembersByGroupId(groupId: UUID): Long


    @Query(value =
    """
        SELECT ug.user FROM UserGroupEntity ug
        WHERE ug.group.uuid = :groupId
        AND ug.status = 'ACCEPTED'
    """)
    fun findMembersByGroupId(groupId: UUID, pageable: Pageable): Page<UserEntity>

    @Query(value =
    """
        SELECT EXISTS (
            SELECT 1 FROM UserGroupEntity ug
            WHERE ug.group.uuid = :groupId
            AND ug.user.uuid = :userId
            AND ug.status = 'ACCEPTED'
        )
    """)
    fun userIsMemberOfGroup(userId: UUID, groupId: UUID): Boolean

    @Query(value =
    """
        SELECT g FROM GroupEntity g
        JOIN UserGroupEntity ug ON ug.group.uuid = g.uuid
        WHERE ug.user.uuid = :userId
        AND ug.isFavorite = true
        AND ug.status = 'ACCEPTED'
    """)
    fun findFavoriteGroups(userId: UUID, pageable: Pageable): Page<GroupEntity>
}