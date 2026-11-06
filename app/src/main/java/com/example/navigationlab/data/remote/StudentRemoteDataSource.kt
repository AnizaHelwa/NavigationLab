package com.example.navigationlab.data.remote

import com.example.navigationlab.data.remote.dto.UserDto

class StudentRemoteDataSource(
    private val api: StudentApiService
) {

    suspend fun getStudents(): List<UserDto> =
        api.getUsers(limit = 10).users

    suspend fun getStudent(id: Int): UserDto =
        api.getUser(id)

    suspend fun searchStudents(query: String): List<UserDto> =
        api.searchUsers(query).users
}