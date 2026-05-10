package com.example.fitnesstracker

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object WorkoutStorage {
    private const val PREFS_NAME = "workout_prefs"
    private const val KEY_ACTIVE_NAME = "active_name"
    private const val KEY_ACTIVE_TIME = "active_time"
    private const val KEY_ACTIVE_EXERCISES = "active_exercises"
    private const val KEY_HAS_ACTIVE = "has_active"

    fun saveCurrentWorkout(
        context: Context,
        workoutName: String,
        workoutTime: String,
        exercises: List<Exercise>
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()

        for (exercise in exercises) {
            val setsArray = JSONArray()

            for (set in exercise.sets) {
                val setObj = JSONObject().apply {
                    put("setNumber", set.setNumber)
                    put("reps", set.reps)
                    put("weight", set.weight)
                }

                setsArray.put(setObj)
            }

            val obj = JSONObject().apply {
                put("name", exercise.name)
                put("type", exercise.type)
                put("muscleGroup", exercise.muscleGroup)
                put("previousStats", exercise.previousStats)
                put("sets", setsArray)
            }
            jsonArray.put(obj)
        }

        prefs.edit()
            .putBoolean(KEY_HAS_ACTIVE, true)
            .putString(KEY_ACTIVE_NAME, workoutName)
            .putString(KEY_ACTIVE_TIME, workoutTime)
            .putString(KEY_ACTIVE_EXERCISES, jsonArray.toString())
            .apply()
    }

    fun loadCurrentWorkout(context: Context): CurrentWorkout? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val hasActive = prefs.getBoolean(KEY_HAS_ACTIVE, false)
        if (!hasActive) return null

        val name = prefs.getString(KEY_ACTIVE_NAME, "") ?: ""
        val time = prefs.getString(KEY_ACTIVE_TIME, "") ?: ""
        val exercisesJson = prefs.getString(KEY_ACTIVE_EXERCISES, "[]") ?: "[]"

        val exercises = mutableListOf<Exercise>()
        val jsonArray = JSONArray(exercisesJson)

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val sets = mutableListOf<ExerciseSet>()
            val setsArray = obj.optJSONArray("sets") ?: JSONArray()

            for (j in 0 until setsArray.length()) {
                val setObj = setsArray.getJSONObject(j)

                sets.add(
                    ExerciseSet(
                        setNumber = setObj.optInt("setNumber", j + 1),
                        reps = setObj.optInt("reps", 0),
                        weight = setObj.optDouble("weight", 0.0)
                    )
                )
            }

            val previousStats = if (obj.isNull("previousStats")) {
                null
            } else {
                obj.optString("previousStats")
            }

            exercises.add(
                Exercise(
                    name = obj.getString("name"),
                    type = obj.getString("type"),
                    muscleGroup = obj.getString("muscleGroup"),
                    sets = sets,
                    previousStats = previousStats
                )
            )
        }

        return CurrentWorkout(
            name = name,
            time = time,
            exercises = exercises
        )
    }

    fun clearCurrentWorkout(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        prefs.edit()
            .putBoolean(KEY_HAS_ACTIVE, false)
            .remove(KEY_ACTIVE_NAME)
            .remove(KEY_ACTIVE_TIME)
            .remove(KEY_ACTIVE_EXERCISES)
            .apply()
    }
}

data class CurrentWorkout(
    val name: String,
    val time: String,
    val exercises: MutableList<Exercise>
)