package com.trailoapp.trailo_backend.repository

import com.trailoapp.trailo_backend.domain.social.GroupEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface GroupRepository: JpaRepository<GroupEntity, UUID> {
    fun existsByName(name: String): Boolean

    fun searchByNameContainingIgnoreCase(name: String, pageable: Pageable): Page<GroupEntity>

    fun searchByIsPrivate(isPrivate: Boolean, pageable: Pageable): Page<GroupEntity>
}