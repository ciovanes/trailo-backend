package com.trailoapp.trailo_backend.repository

import com.trailoapp.trailo_backend.domain.enum.GroupRoles
import com.trailoapp.trailo_backend.domain.social.UserGroupEntity
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
}