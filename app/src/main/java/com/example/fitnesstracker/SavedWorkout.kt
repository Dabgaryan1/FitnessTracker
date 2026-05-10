package com.example.fitnesstracker

data class SavedWorkout(
    val id: String = "",
    val name: String = "",
    val time: String = "",
    val dateText: String = "",
    val exercises: List<Exercise> = emptyList(),
    var isExpanded: Boolean = false
)