package com.example.navigationlab

object Routes {
    const val HOME = "com/example/navigationlab/ui/home"
    const val PROFILE = "profile"
    const val ABOUT = "about"
    const val DETAIL = "detail/{studentId}"

    fun detail(studentId: Int): String = "detail/$studentId"
}