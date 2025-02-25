package com.example.biyahecebu.models

data class JeepneyRoute(
    val jeepneyCode: String = "",
    val route: String = "",
    val landmarks: List<String> = listOf(),
    var isFavorite: Boolean = false
)
