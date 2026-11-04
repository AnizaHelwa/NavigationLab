package com.example.navigationlab.data.remote

import com.example.navigationlab.data.remote.dto.UserDto
import com.example.navigationlab.data.remote.dto.UserResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StudentRemoteDataSource(
    private val api: StudentApiService
) {

    fun searchStudents(query: String, callback: (Result<List<UserDto>>) -> Unit) {
        api.searchUsers(query).enqueue(object : Callback<UserResponse> {
            override fun onResponse(
                call: Call<UserResponse>,
                response: Response<UserResponse>
            ) {
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    callback(Result.success(body.users))
                } else {
                    callback(Result.failure(Exception("HTTP ${response.code()}")))
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                callback(Result.failure(t))
            }
        })
    }

    fun getStudents(callback: (Result<List<UserDto>>) -> Unit) {
        api.getUsers().enqueue(object : Callback<UserResponse> {
            override fun onResponse(
                call: Call<UserResponse>,
                response: Response<UserResponse>
            ) {
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    callback(Result.success(body.users))
                } else {
                    callback(Result.failure(Exception("HTTP ${response.code()}")))
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                callback(Result.failure(t))
            }
        })
    }

    fun getStudent(id: Int, callback: (Result<UserDto>) -> Unit) {
        api.getUser(id).enqueue(object : Callback<UserDto> {
            override fun onResponse(call: Call<UserDto>, response: Response<UserDto>) {
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    callback(Result.success(body))
                } else {
                    callback(Result.failure(Exception("HTTP ${response.code()}")))
                }
            }

            override fun onFailure(call: Call<UserDto>, t: Throwable) {
                callback(Result.failure(t))
            }
        })
    }
}