package com.example.navigationlab.data.repository

import com.example.navigationlab.data.model.Student
import com.example.navigationlab.data.remote.StudentRemoteDataSource
import com.example.navigationlab.data.remote.dto.toStudent

class DefaultStudentRepository(
    private val remoteDataSource: StudentRemoteDataSource
) : StudentRepository {

    override fun getStudents(callback: (Result<List<Student>>) -> Unit) {
        remoteDataSource.getStudents { result ->
            callback(result.map { users -> users.map { it.toStudent() } })
        }
    }

    override fun getStudent(id: Int, callback: (Result<Student>) -> Unit) {
        remoteDataSource.getStudent(id) { result ->
            callback(result.map { it.toStudent() })
        }
    }

    override fun searchStudents(
        query: String,
        callback: (Result<List<Student>>) -> Unit
    ) {
        remoteDataSource.searchStudents(query) { result ->
            callback(result.map { users -> users.map { it.toStudent() } })
        }
    }
}