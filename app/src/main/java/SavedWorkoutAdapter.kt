package com.example.fitnesstracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SavedWorkoutAdapter(
    private val workouts: MutableList<SavedWorkout>,
    private val onDeleteClick: (SavedWorkout) -> Unit
) : RecyclerView.Adapter<SavedWorkoutAdapter.SavedWorkoutViewHolder>() {

    class SavedWorkoutViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSavedWorkoutName: TextView = itemView.findViewById(R.id.tvSavedWorkoutName)
        val tvSavedWorkoutDate: TextView = itemView.findViewById(R.id.tvSavedWorkoutDate)
        val tvSavedWorkoutExercises: TextView = itemView.findViewById(R.id.tvSavedWorkoutExercises)
        val btnDeleteSavedWorkout: ImageView = itemView.findViewById(R.id.btnDeleteSavedWorkout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedWorkoutViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_itemsavedworkout, parent, false)

        return SavedWorkoutViewHolder(view)
    }

    override fun onBindViewHolder(holder: SavedWorkoutViewHolder, position: Int) {
        val workout = workouts[position]

        holder.tvSavedWorkoutName.text = workout.name
        holder.tvSavedWorkoutDate.text = workout.dateText

        val exerciseText = workout.exercises.joinToString("\n\n") { exercise ->
            val setsText = if (exercise.sets.isEmpty()) {
                "No sets"
            } else {
                exercise.sets.joinToString("\n") { set ->
                    val weightText = if (set.weight % 1.0 == 0.0) {
                        set.weight.toInt().toString()
                    } else {
                        set.weight.toString()
                    }

                    "Set ${set.setNumber}: ${set.reps} reps x $weightText lbs"
                }
            }

            "${exercise.name} (${exercise.type} • ${exercise.muscleGroup})\n$setsText"
        }

        holder.tvSavedWorkoutExercises.text = exerciseText

        holder.tvSavedWorkoutExercises.visibility = if (workout.isExpanded) {
            View.VISIBLE
        } else {
            View.GONE
        }

        holder.itemView.setOnClickListener {
            workout.isExpanded = !workout.isExpanded
            notifyItemChanged(holder.adapterPosition)
        }

        holder.btnDeleteSavedWorkout.setOnClickListener {
            onDeleteClick(workout)
        }
    }

    override fun getItemCount(): Int {
        return workouts.size
    }
}