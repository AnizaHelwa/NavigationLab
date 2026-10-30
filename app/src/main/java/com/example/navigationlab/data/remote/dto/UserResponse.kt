package com.example.navigationlab.data.remote.dto

data class UserResponse(
    val users: List<UserDto>,
    val total: Int,
    val skip: Int,
    val limit: Int
)