package com.example.fitnesstracker

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.app.AlertDialog
class WorkoutActivity : AppCompatActivity() {
    private lateinit var tvTitle: TextView

    private lateinit var etExerciseName: EditText
    private lateinit var etExerciseType: EditText
    private lateinit var etMuscleGroup: EditText

    private lateinit var btnAddExercise: Button
    private lateinit var recyclerExercises: RecyclerView
    private lateinit var btnSaveWorkout: Button
    private lateinit var backButton: ImageButton

    private lateinit var adapter: ExerciseAdapter
    private lateinit var exerciseList: MutableList<Exercise>

    private var workoutName: String = "New Workout"
    private var workoutTime: String = ""
    private var shouldSaveOnPause = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout)

        BottomNavHelper.bottomNav(this)

        tvTitle = findViewById(R.id.tvTitle)
        etExerciseName = findViewById(R.id.etExerciseName)
        etExerciseType = findViewById(R.id.etExerciseType)
        etMuscleGroup = findViewById(R.id.etMuscleGroup)
        btnAddExercise = findViewById(R.id.btnAddExercise)
        recyclerExercises = findViewById(R.id.recyclerExercises)
        btnSaveWorkout = findViewById(R.id.btnSaveWorkout)
        backButton = findViewById(R.id.backButton)

        val savedWorkout = WorkoutStorage.loadCurrentWorkout(this)

        if (savedWorkout != null) {
            workoutName = savedWorkout.name
            workoutTime = savedWorkout.time
            exerciseList = savedWorkout.exercises
        } else {
            workoutName = intent.getStringExtra("workoutName") ?: "New Workout"
            workoutTime = intent.getStringExtra("workoutTime") ?: ""
            exerciseList = mutableListOf()

            WorkoutStorage.saveCurrentWorkout(
                context = this,
                workoutName = workoutName,
                workoutTime = workoutTime,
                exercises = exerciseList
            )
        }

        tvTitle.text = workoutName

        adapter = ExerciseAdapter(
            exercises = exerciseList,
            onDeleteClick = { position ->
                exerciseList.removeAt(position)
                adapter.notifyItemRemoved(position)
                saveWorkoutState()
            },
            onWorkoutChanged = {
                saveWorkoutState()
            }
        )

        recyclerExercises.layoutManager = LinearLayoutManager(this)
        recyclerExercises.adapter = adapter

        btnAddExercise.setOnClickListener {
            val exerciseName = etExerciseName.text.toString().trim()
            val exerciseType = etExerciseType.text.toString().trim()
            val muscleGroup = etMuscleGroup.text.toString().trim()

            if (exerciseName.isEmpty() || exerciseType.isEmpty() || muscleGroup.isEmpty()) {
                Toast.makeText(this, "Please fill out all exercise fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val exercise = Exercise(
                name = exerciseName,
                type = exerciseType,
                muscleGroup = muscleGroup
            )
            //add exercise to list
            exerciseList.add(exercise)
            adapter.notifyItemInserted(exerciseList.size - 1)

            //get most recent version of exercise
            WorkoutRepository.getLastStatsForExercise(exerciseName) { previousStats ->
                runOnUiThread {
                    val index = exerciseList.indexOf(exercise)

                    if (index != -1 && previousStats != null) {
                        exercise.previousStats = previousStats
                        adapter.notifyItemChanged(index)
                        saveWorkoutState()
                    }
                }
            }

            etExerciseName.text.clear()
            etExerciseType.text.clear()
            etMuscleGroup.text.clear()

            saveWorkoutState()
        }

        backButton.setOnClickListener {
            shouldSaveOnPause = false
            WorkoutStorage.clearCurrentWorkout(this)

            val intent = Intent(this, CreateWorkoutActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        btnSaveWorkout.setOnClickListener {
            if (exerciseList.isEmpty()) {
                Toast.makeText(this, "Add at least one exercise", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setTitle("Save Workout")
                .setMessage("Are you sure you want to save and end this workout?")
                .setPositiveButton("Save") { _, _ ->

                    shouldSaveOnPause = false
                    btnSaveWorkout.isEnabled = false

                    val savedWorkoutName = workoutName
                    val savedWorkoutTime = workoutTime
                    val savedExerciseList = exerciseList.toList()

                    WorkoutStorage.clearCurrentWorkout(this)

                    WorkoutRepository.saveCompletedWorkout(
                        workoutName = savedWorkoutName,
                        workoutTime = savedWorkoutTime,
                        exercises = savedExerciseList
                    ) { success ->
                        runOnUiThread {
                            if (!success) {
                                Toast.makeText(
                                    this,
                                    "Failed to save workout",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                    val intent = Intent(this, ViewWorkoutActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onPause() {
        super.onPause()
        if (shouldSaveOnPause) {
            saveWorkoutState()
        }
    }

    private fun saveWorkoutState() {
        WorkoutStorage.saveCurrentWorkout(
            context = this,
            workoutName = workoutName,
            workoutTime = workoutTime,
            exercises = exerciseList
        )
    }
}