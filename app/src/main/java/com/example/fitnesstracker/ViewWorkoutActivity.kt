package com.example.fitnesstracker

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ListenerRegistration

class ViewWorkoutActivity : AppCompatActivity() {

    private lateinit var workoutRecyclerView: RecyclerView
    private lateinit var tvEmptyWorkouts: TextView
    private lateinit var adapter: SavedWorkoutAdapter
    private val savedWorkouts = mutableListOf<SavedWorkout>()

    private var workoutsListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewworkout)

        BottomNavHelper.bottomNav(this)

        workoutRecyclerView = findViewById(R.id.workoutRecyclerView)
        tvEmptyWorkouts = findViewById(R.id.tvEmptyWorkouts)

        adapter = SavedWorkoutAdapter(savedWorkouts) { workout ->
            confirmDeleteWorkout(workout)
        }

        workoutRecyclerView.layoutManager = LinearLayoutManager(this)
        workoutRecyclerView.adapter = adapter

        startListeningForWorkouts()
    }

    private fun startListeningForWorkouts() {
        workoutsListener = WorkoutRepository.listenForSavedWorkouts { workouts ->
            runOnUiThread {
                savedWorkouts.clear()
                savedWorkouts.addAll(workouts)
                adapter.notifyDataSetChanged()
                updateEmptyState()
            }
        }
    }

    private fun updateEmptyState() {
        if (savedWorkouts.isEmpty()) {
            tvEmptyWorkouts.visibility = View.VISIBLE
            workoutRecyclerView.visibility = View.GONE
        } else {
            tvEmptyWorkouts.visibility = View.GONE
            workoutRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun confirmDeleteWorkout(workout: SavedWorkout) {
        AlertDialog.Builder(this)
            .setTitle("Delete Workout")
            .setMessage("Are you sure you want to delete ${workout.name}?")
            .setPositiveButton("Delete") { _, _ ->

                val indexToRemove = savedWorkouts.indexOfFirst { it.id == workout.id }

                if (indexToRemove != -1) {
                    savedWorkouts.removeAt(indexToRemove)
                    adapter.notifyItemRemoved(indexToRemove)
                    adapter.notifyItemRangeChanged(indexToRemove, savedWorkouts.size)
                    updateEmptyState()
                }

                WorkoutRepository.deleteSavedWorkout(workout.id) { success ->
                    runOnUiThread {
                        if (success) {
                            Toast.makeText(this, "Workout deleted", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Failed to delete workout", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        workoutsListener?.remove()
    }
}