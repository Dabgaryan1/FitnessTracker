package com.example.fitnesstracker

data class Exercise(
    val name: String,
    val type: String,
    val muscleGroup: String,
    val sets: MutableList<ExerciseSet> = mutableListOf(),
    var previousStats: String? = null
)