package com.example.navigationlab.data.repository

import com.example.navigationlab.data.model.Student

interface StudentRepository {
    fun getStudents(callback: (Result<List<Student>>) -> Unit)
    fun getStudent(id: Int, callback: (Result<Student>) -> Unit)

    fun searchStudents(query: String, callback: (Result<List<Student>>) -> Unit)
}