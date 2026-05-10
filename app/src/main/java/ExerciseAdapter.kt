package com.example.fitnesstracker

import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView

class ExerciseAdapter(
    private val exercises: MutableList<Exercise>,
    private val onDeleteClick: (Int) -> Unit,
    private val onWorkoutChanged: () -> Unit
) : RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder>() {

    class ExerciseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvExerciseName: TextView = itemView.findViewById(R.id.tvExerciseName)
        val tvExerciseDetails: TextView = itemView.findViewById(R.id.tvExerciseDetails)
        val btnDeleteExercise: ImageView = itemView.findViewById(R.id.btnDeleteExercise)
        val tvPreviousStats: TextView = itemView.findViewById(R.id.tvPreviousStats)
        val tvSetsReps: TextView = itemView.findViewById(R.id.tvSetsReps)
        val btnAddSet: Button = itemView.findViewById(R.id.btnAddSet)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_itemexercise, parent, false)

        return ExerciseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        val exercise = exercises[position]

        holder.tvExerciseName.text = exercise.name
        holder.tvExerciseDetails.text = "${exercise.type} • ${exercise.muscleGroup}"

        if (exercise.previousStats.isNullOrEmpty()) {
            holder.tvPreviousStats.visibility = View.GONE
        } else {
            holder.tvPreviousStats.visibility = View.VISIBLE
            holder.tvPreviousStats.text = exercise.previousStats
        }

        holder.tvSetsReps.text = if (exercise.sets.isEmpty()) {
            "No sets added yet"
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

        holder.btnAddSet.setOnClickListener {
            val context = holder.itemView.context

            val layout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(50, 20, 50, 10)
            }

            val repsInput = EditText(context).apply {
                hint = "Reps"
                inputType = InputType.TYPE_CLASS_NUMBER
            }

            val weightInput = EditText(context).apply {
                hint = "Weight"
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            }

            layout.addView(repsInput)
            layout.addView(weightInput)

            AlertDialog.Builder(context)
                .setTitle("Add Set")
                .setView(layout)
                .setPositiveButton("Add") { _, _ ->
                    val repsText = repsInput.text.toString().trim()
                    val weightText = weightInput.text.toString().trim()

                    if (repsText.isEmpty() || weightText.isEmpty()) {
                        return@setPositiveButton
                    }

                    val newSet = ExerciseSet(
                        setNumber = exercise.sets.size + 1,
                        reps = repsText.toInt(),
                        weight = weightText.toDouble()
                    )

                    exercise.sets.add(newSet)
                    notifyItemChanged(holder.adapterPosition)
                    onWorkoutChanged()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        holder.btnDeleteExercise.setOnClickListener {
            onDeleteClick(holder.adapterPosition)
        }
    }

    override fun getItemCount(): Int {
        return exercises.size
    }
}