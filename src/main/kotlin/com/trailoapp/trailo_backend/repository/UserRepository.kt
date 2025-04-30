package com.trailoapp.trailo_backend.repository

import com.trailoapp.trailo_backend.domain.core.UserEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<UserEntity, UUID> {
    fun findByEmail(email: String): Optional<UserEntity>
    fun findByUsername(username: String): Optional<UserEntity>
    fun existsByEmail(email: String): Boolean
    fun existsByUsername(username: String): Boolean
    fun findByCognitoId(cognitoId: String): Optional<UserEntity>

    fun searchByUsernameContainingIgnoreCase(username: String, pageable: Pageable): Page<UserEntity>
    fun searchByNameContainingIgnoreCase(name: String, pageable: Pageable): Page<UserEntity>
    fun searchBySurnameContainingIgnoreCase(surname: String, pageable: Pageable): Page<UserEntity>
    fun searchByCountryContainingIgnoreCase(country: String, pageable: Pageable): Page<UserEntity>
}