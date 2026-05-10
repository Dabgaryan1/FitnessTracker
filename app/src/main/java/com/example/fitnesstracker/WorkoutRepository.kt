package com.example.fitnesstracker

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale
import com.google.firebase.firestore.ListenerRegistration
object WorkoutRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private fun userWorkoutsCollection() =
        db.collection("users")
            .document(auth.currentUser!!.uid)
            .collection("workouts")

    fun saveCompletedWorkout(
        workoutName: String,
        workoutTime: String,
        exercises: List<Exercise>,
        onComplete: (Boolean) -> Unit
    ) {
        val exerciseMaps = exercises.map { exercise ->
            mapOf(
                "name" to exercise.name,
                "type" to exercise.type,
                "muscleGroup" to exercise.muscleGroup,
                "sets" to exercise.sets.map { set ->
                    mapOf(
                        "setNumber" to set.setNumber,
                        "reps" to set.reps,
                        "weight" to set.weight
                    )
                }
            )
        }

        val workoutData = mapOf(
            "name" to workoutName,
            "time" to workoutTime,
            "date" to Timestamp.now(),
            "exercises" to exerciseMaps
        )

        userWorkoutsCollection()
            .add(workoutData)
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

    fun getLastStatsForExercise(
        exerciseName: String,
        onResult: (String?) -> Unit
    ) {
        userWorkoutsCollection()
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    val workoutName = doc.getString("name") ?: "Workout"
                    val exercises = doc.get("exercises") as? List<Map<String, Any>> ?: continue

                    for (exercise in exercises) {
                        val name = exercise["name"] as? String ?: continue

                        if (name.equals(exerciseName, ignoreCase = true)) {
                            val sets = exercise["sets"] as? List<Map<String, Any>> ?: emptyList()

                            if (sets.isEmpty()) {
                                onResult("$workoutName: No sets found")
                                return@addOnSuccessListener
                            }

                            val setText = sets.mapIndexed { index, set ->
                                val reps = when (val repsValue = set["reps"]) {
                                    is Long -> repsValue.toInt()
                                    is Int -> repsValue
                                    is Double -> repsValue.toInt()
                                    else -> 0
                                }

                                val weight = when (val weightValue = set["weight"]) {
                                    is Long -> weightValue.toDouble()
                                    is Int -> weightValue.toDouble()
                                    is Double -> weightValue
                                    else -> 0.0
                                }

                                val weightText = if (weight % 1.0 == 0.0) {
                                    weight.toInt().toString()
                                } else {
                                    weight.toString()
                                }

                                "Set ${index + 1}: $reps reps x $weightText lbs"
                            }.joinToString("\n")

                            onResult("Last time for $exerciseName:\n$setText")
                            return@addOnSuccessListener
                        }
                    }
                }

                onResult(null)
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    fun loadSavedWorkouts(
        onResult: (List<SavedWorkout>) -> Unit
    ) {
        userWorkoutsCollection()
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val workouts = mutableListOf<SavedWorkout>()

                for (doc in snapshot.documents) {
                    val workoutName = doc.getString("name") ?: "Workout"
                    val workoutTime = doc.getString("time") ?: ""

                    val timestamp = doc.getTimestamp("date")
                    val dateText = if (timestamp != null) {
                        val formatter = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
                        formatter.format(timestamp.toDate())
                    } else {
                        "No date"
                    }

                    val exercisesList = mutableListOf<Exercise>()
                    val exercises = doc.get("exercises") as? List<Map<String, Any>> ?: emptyList()

                    for (exerciseMap in exercises) {
                        val setsList = mutableListOf<ExerciseSet>()
                        val sets = exerciseMap["sets"] as? List<Map<String, Any>> ?: emptyList()

                        for ((index, setMap) in sets.withIndex()) {
                            val reps = when (val repsValue = setMap["reps"]) {
                                is Long -> repsValue.toInt()
                                is Int -> repsValue
                                is Double -> repsValue.toInt()
                                else -> 0
                            }

                            val weight = when (val weightValue = setMap["weight"]) {
                                is Long -> weightValue.toDouble()
                                is Int -> weightValue.toDouble()
                                is Double -> weightValue
                                else -> 0.0
                            }

                            setsList.add(
                                ExerciseSet(
                                    setNumber = index + 1,
                                    reps = reps,
                                    weight = weight
                                )
                            )
                        }

                        exercisesList.add(
                            Exercise(
                                name = exerciseMap["name"] as? String ?: "",
                                type = exerciseMap["type"] as? String ?: "",
                                muscleGroup = exerciseMap["muscleGroup"] as? String ?: "",
                                sets = setsList
                            )
                        )
                    }

                    workouts.add(
                        SavedWorkout(
                            id = doc.id,
                            name = workoutName,
                            time = workoutTime,
                            dateText = dateText,
                            exercises = exercisesList
                        )
                    )
                }

                onResult(workouts)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }
    fun listenForSavedWorkouts(
        onResult: (List<SavedWorkout>) -> Unit
    ): ListenerRegistration {
        return userWorkoutsCollection()
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) {
                    onResult(emptyList())
                    return@addSnapshotListener
                }

                val workouts = mutableListOf<SavedWorkout>()

                for (doc in snapshot.documents) {
                    val workoutName = doc.getString("name") ?: "Workout"
                    val workoutTime = doc.getString("time") ?: ""

                    val timestamp = doc.getTimestamp("date")
                    val dateText = if (timestamp != null) {
                        val formatter = java.text.SimpleDateFormat(
                            "MMM d, yyyy • h:mm a",
                            java.util.Locale.getDefault()
                        )
                        formatter.format(timestamp.toDate())
                    } else {
                        "No date"
                    }

                    val exercisesList = mutableListOf<Exercise>()
                    val exercises = doc.get("exercises") as? List<Map<String, Any>> ?: emptyList()

                    for (exerciseMap in exercises) {
                        val setsList = mutableListOf<ExerciseSet>()
                        val sets = exerciseMap["sets"] as? List<Map<String, Any>> ?: emptyList()

                        for ((index, setMap) in sets.withIndex()) {
                            val reps = when (val repsValue = setMap["reps"]) {
                                is Long -> repsValue.toInt()
                                is Int -> repsValue
                                is Double -> repsValue.toInt()
                                else -> 0
                            }

                            val weight = when (val weightValue = setMap["weight"]) {
                                is Long -> weightValue.toDouble()
                                is Int -> weightValue.toDouble()
                                is Double -> weightValue
                                else -> 0.0
                            }

                            setsList.add(
                                ExerciseSet(
                                    setNumber = index + 1,
                                    reps = reps,
                                    weight = weight
                                )
                            )
                        }

                        exercisesList.add(
                            Exercise(
                                name = exerciseMap["name"] as? String ?: "",
                                type = exerciseMap["type"] as? String ?: "",
                                muscleGroup = exerciseMap["muscleGroup"] as? String ?: "",
                                sets = setsList
                            )
                        )
                    }

                    workouts.add(
                        SavedWorkout(
                            id = doc.id,
                            name = workoutName,
                            time = workoutTime,
                            dateText = dateText,
                            exercises = exercisesList
                        )
                    )
                }

                onResult(workouts)
            }
    }
    fun deleteSavedWorkout(
        workoutId: String,
        onComplete: (Boolean) -> Unit
    ) {
        userWorkoutsCollection()
            .document(workoutId)
            .delete()
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }
}