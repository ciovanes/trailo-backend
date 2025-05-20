package com.trailoapp.trailo_backend.service

import com.trailoapp.trailo_backend.domain.core.UserEntity
import com.trailoapp.trailo_backend.dto.user.request.CreateUserDto
import com.trailoapp.trailo_backend.dto.user.request.UpdateUserRequest
import com.trailoapp.trailo_backend.exception.definitions.DuplicateResourceException
import com.trailoapp.trailo_backend.exception.definitions.ResourceNotFoundException
import com.trailoapp.trailo_backend.repository.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.kotlin.whenever
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.OffsetDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class UserServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var cognitoService: CognitoService

    private lateinit var userService: UserService

    private lateinit var testUser: UserEntity
    private val testUserId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        userService = UserService(userRepository, cognitoService)

        testUser = UserEntity(
            uuid = testUserId,
            email = "test@trailo-app.com",
            username = "testuser",
            name = "Test",
            surname = "User",
            profileImageUrl = null,
            country = "Romania",
            cognitoId = "test-cognito-id",
            lastLoginAt = OffsetDateTime.now(),
            lastModifiedAt = OffsetDateTime.now(),
            createdAt = OffsetDateTime.now()
        )
    }

    @Test
    fun `updateUser updates user data in database and returns updated user`() {
        val updateUserRequest = UpdateUserRequest(
            name = "Updated Name",
            surname = "Updated Surname",
            profileImageUrl = "updated-profile-image-url",
            country = "Updated Country"
        )

        whenever(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser))

        whenever(userRepository.save(any(UserEntity::class.java))).thenAnswer {
            it.getArgument<UserEntity>(0)
        }

        val result = userService.updateUser(testUserId, updateUserRequest)

        assertNotNull(result)
        assertEquals(testUserId, result.uuid)
        assertEquals(testUser.email, result.email)
        assertEquals(updateUserRequest.name, result.name)
        assertEquals(updateUserRequest.surname, result.surname)

        verify(userRepository).findById(testUserId)
        verify(userRepository).save(any(UserEntity::class.java))
    }

    @Test
    fun `updateUser throws ResourceNotFoundException when user does not exist`() {
        val updateUserRequest = UpdateUserRequest(
            name = "Updated Name",
            surname = "Updated Surname",
            profileImageUrl = "updated-profile-image-url",
            country = "Updated Country"
        )

        whenever(userRepository.findById(testUserId)).thenReturn(Optional.empty())

        val exception = assertThrows(ResourceNotFoundException::class.java) {
            userService.updateUser(testUserId, updateUserRequest)
        }

        assertTrue(exception.message?.contains("user") == true)

        verify(userRepository).findById(testUserId)
        verify(userRepository, never()).save(any(UserEntity::class.java))
    }

    @Test
    fun `deleteUser deletes user from database and cognito`() {
        userService.deleteUser(testUserId, testUser.username)

        verify(cognitoService).deleteUser(testUser.username)
        verify(userRepository).deleteById(testUserId)
    }

    @Test
    fun `searchQuery delegates to correct search method based on searchBy`() {
        val pageable = PageRequest.of(0, 10)
        val query = "test"
        val users = PageImpl(listOf(testUser), pageable, 1)

        whenever(userRepository.searchByUsernameContainingIgnoreCase(query, pageable)).thenReturn(users)
        whenever(userRepository.searchByNameContainingIgnoreCase(query, pageable)).thenReturn(users)
        whenever(userRepository.searchByCountryContainingIgnoreCase(query, pageable)).thenReturn(users)
        whenever(userRepository.searchBySurnameContainingIgnoreCase(query, pageable)).thenReturn(users)

        val usernameResult = userService.searchQuery("username", query, pageable)
        assertEquals(1, usernameResult.totalElements)
        verify(userRepository).searchByUsernameContainingIgnoreCase(query, pageable)

        clearInvocations(userRepository)

        val nameResult = userService.searchQuery("name", query, pageable)
        assertEquals(1, nameResult.totalElements)
        verify(userRepository).searchByNameContainingIgnoreCase(query, pageable)

        clearInvocations(userRepository)

        val countryResult = userService.searchQuery("country", query, pageable)
        assertEquals(1, countryResult.totalElements)
        verify(userRepository).searchByCountryContainingIgnoreCase(query, pageable)

        clearInvocations(userRepository)

        val surnameResult = userService.searchQuery("surname", query, pageable)
        assertEquals(1, surnameResult.totalElements)
        verify(userRepository).searchBySurnameContainingIgnoreCase(query, pageable)

        clearInvocations(userRepository)

        val elseResult = userService.searchQuery("otro", query, pageable)
        assertEquals(1, elseResult.totalElements)
        verify(userRepository).searchByUsernameContainingIgnoreCase(query, pageable)
    }

    @Test
    fun `getAllUsers returns all users in database`() {
        val pageable = PageRequest.of(0, 10)
        val users = PageImpl(listOf(testUser), pageable, 1)

        whenever(userRepository.findAll(pageable)).thenReturn(users)

        val result = userService.getAllUsers(pageable)

        assertNotNull(result)
        assertEquals(1, result.totalElements)
        assertEquals(testUser, result.content[0])

        verify(userRepository).findAll(pageable)
    }


    @Test
    fun `findUserById returns user when user exists`() {
        whenever(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser))

        val result = userService.findUserById(testUserId)

        assertNotNull(result)
        assertEquals(testUserId, result?.uuid)
        assertEquals(testUser.email, result?.email)
        assertEquals(testUser.username, result?.username)

        verify(userRepository).findById(testUserId)
    }

    @Test
    fun `findUserById returns null when user does not exists` () {
        whenever(userRepository.findById(testUserId)).thenReturn(Optional.empty())

        val result = userService.findUserById(testUserId)

        assertNull(result)

        verify(userRepository).findById(testUserId)
    }

    @Test
    fun `findUserByEmail returns user when user exists`() {
        whenever(userRepository.findByEmail(testUser.email)).thenReturn(testUser)

        val result = userService.findUserByEmail(testUser.email)

        assertNotNull(result)
        assertEquals(testUserId, result?.uuid)
        assertEquals(testUser.email, result?.email)
        assertEquals(testUser.username, result?.username)

        verify(userRepository).findByEmail(testUser.email)
    }

    @Test
    fun `findUserByEmail returns null when user does not exists`() {
        whenever(userRepository.findByEmail(testUser.email)).thenReturn(null)

        val result = userService.findUserByEmail(testUser.email)

        assertNull(result)

        verify(userRepository).findByEmail(testUser.email)
    }

    @Test
    fun `findUserByUsername returns user when user exists`() {
        whenever(userRepository.findByUsername(testUser.username)).thenReturn(testUser)

        val result = userService.findUserByUsername(testUser.username)

        assertNotNull(result)
        assertEquals(testUserId, result?.uuid)
        assertEquals(testUser.email, result?.email)
        assertEquals(testUser.username, result?.username)

        verify(userRepository).findByUsername(testUser.username)
    }

    @Test
    fun `findUserByUsername returns null when user does not exists`() {
        whenever(userRepository.findByUsername(testUser.username)).thenReturn(null)

        val result = userService.findUserByUsername(testUser.username)

        assertNull(result)

        verify(userRepository).findByUsername(testUser.username)
    }

    @Test
    fun `findUserByCognitoId returns user when user exists`() {
        whenever(userRepository.findByCognitoId(testUser.cognitoId)).thenReturn(testUser)

        val result = userService.findUserByCognitoId(testUser.cognitoId)

        assertNotNull(result)
        assertEquals(testUserId, result?.uuid)
        assertEquals(testUser.email, result?.email)
        assertEquals(testUser.username, result?.username)

        verify(userRepository).findByCognitoId(testUser.cognitoId)
    }

    @Test
    fun `findUserByCognitoId returns null when user does not exists`() {
        whenever(userRepository.findByCognitoId(testUser.cognitoId)).thenReturn(null)

        val result = userService.findUserByCognitoId(testUser.cognitoId)

        assertNull(result)

        verify(userRepository).findByCognitoId(testUser.cognitoId)
    }

    @Test
    fun `createUser creates and returns a new user when username and email are unique`() {
        val createUserDto = CreateUserDto(
            email = "unique@email",
            username = "uniqueUsername",
            cognitoId = "test-cognito-id"
        )

        whenever(userRepository.existsByEmail(createUserDto.email)).thenReturn(false)
        whenever(userRepository.existsByUsername(createUserDto.username)).thenReturn(false)

        whenever(userRepository.save(any(UserEntity::class.java))).thenAnswer {
            val savedUser = it.getArgument<UserEntity>(0)
            savedUser.copy(uuid = UUID.randomUUID())
        }

        val result = userService.createUser(createUserDto)

        assertNotNull(result)
        assertEquals(createUserDto.email, result.email)
        assertEquals(createUserDto.username, result.username)
        assertEquals(createUserDto.cognitoId, result.cognitoId)

        verify(userRepository).existsByEmail(createUserDto.email)
        verify(userRepository).existsByUsername(createUserDto.username)
        verify(userRepository).save(any(UserEntity::class.java))
    }

    @Test
    fun `createUser throws DuplicateResourceException when email is already in use`() {
        val createUserDto = CreateUserDto(
            email = testUser.email,
            username = "uniqueUsername",
            cognitoId = "test-cognito-id"
        )

        whenever(userRepository.existsByEmail(createUserDto.email)).thenReturn(true)

        val exception = assertThrows(DuplicateResourceException::class.java) {
            userService.createUser(createUserDto)
        }

        assertTrue(exception.message?.contains("email") == true)

        verify(userRepository).existsByEmail(createUserDto.email)
        verify(userRepository, never()).save(any(UserEntity::class.java))
    }

    @Test
    fun `createUser throws DuplicateResourceException when username is already in use`() {
        val createUserDto = CreateUserDto(
            email = "unique@email.com",
            username = testUser.username,
            cognitoId = "test-cognito-id"
        )

        whenever(userRepository.existsByUsername(createUserDto.username)).thenReturn(true)

        val exception = assertThrows(DuplicateResourceException::class.java) {
            userService.createUser(createUserDto)
        }

        assertTrue(exception.message?.contains("username") == true)

        verify(userRepository).existsByUsername(createUserDto.username)
        verify(userRepository, never()).save(any(UserEntity::class.java))
    }

    @Test
    fun `saveUser saves user to database`() {
        whenever(userRepository.save(testUser)).thenReturn(testUser)

        val result = userService.saveUser(testUser)

        assertNotNull(result)
        assertEquals(testUser, result)

        verify(userRepository).save(testUser)
    }

    @Test
    fun `searchByUsername returns matching users`() {
        val pageable = PageRequest.of(0, 10)
        val query = testUser.username
        val users = PageImpl(listOf(testUser), pageable, 1)

        whenever(userRepository.searchByUsernameContainingIgnoreCase(query, pageable)).thenReturn(users)

        val result = userService.searchByUsername(query,  pageable)

        assertEquals(1, result.totalElements)
        assertEquals(testUser, result.content[0])

        verify(userRepository).searchByUsernameContainingIgnoreCase(query, pageable)
    }

    @Test
    fun `searchByName returns matching users`() {
        val pageable = PageRequest.of(0, 10)
        val query = testUser.name.toString()
        val users = PageImpl(listOf(testUser), pageable, 1)

        whenever(userRepository.searchByNameContainingIgnoreCase(query, pageable)).thenReturn(users)

        val result = userService.searchByName(query,  pageable)

        assertEquals(1, result.totalElements)
        assertEquals(testUser, result.content[0])

        verify(userRepository).searchByNameContainingIgnoreCase(query, pageable)
    }

    @Test
    fun `searchByCountry returns matching users`() {
        val pageable = PageRequest.of(0, 10)
        val query = testUser.country.toString()
        val users = PageImpl(listOf(testUser), pageable, 1)

        whenever(userRepository.searchByCountryContainingIgnoreCase(query, pageable)).thenReturn(users)

        val result = userService.searchByCountry(query,  pageable)

        assertEquals(1, result.totalElements)
        assertEquals(testUser, result.content[0])

        verify(userRepository).searchByCountryContainingIgnoreCase(query, pageable)
    }

    @Test
    fun `searchBySurname returns matching users`() {
        val pageable = PageRequest.of(0, 10)
        val query = testUser.surname.toString()
        val users = PageImpl(listOf(testUser), pageable, 1)

        whenever(userRepository.searchBySurnameContainingIgnoreCase(query, pageable)).thenReturn(users)

        val result = userService.searchBySurname(query,  pageable)

        assertEquals(1, result.totalElements)
        assertEquals(testUser, result.content[0])

        verify(userRepository).searchBySurnameContainingIgnoreCase(query, pageable)
    }
}