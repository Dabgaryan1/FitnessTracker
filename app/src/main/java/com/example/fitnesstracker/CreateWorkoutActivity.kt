package com.example.fitnesstracker

import android.os.Bundle
import android.widget.*
import android.content.Intent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class CreateWorkoutActivity : AppCompatActivity() {
    private lateinit var checkMarker: TextView
    private lateinit var workoutName: EditText
    private lateinit var workoutTime: EditText

    private fun isValidTimeOfDay(time: String): Boolean {
        val timeRegex = Regex("^(1[0-2]|0?[1-9]):[0-5][0-9]\\s?(AM|PM|am|pm)$")
        return timeRegex.matches(time.trim())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_createworkout)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.CreateWorkout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        BottomNavHelper.bottomNav(this)

        checkMarker = findViewById(R.id.CheckMark)
        workoutName = findViewById(R.id.WorkoutName)
        workoutTime = findViewById(R.id.WorkoutTime)

        checkMarker.setOnClickListener {
            val name = workoutName.text.toString().trim()
            val time = workoutTime.text.toString().trim()

            if (name.isEmpty() || time.isEmpty()) {
                Toast.makeText(this, "Please enter a name and time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isValidTimeOfDay(time)) {
                Toast.makeText(this, "Please enter a valid time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            WorkoutStorage.saveCurrentWorkout(
                context = this,
                workoutName = name,
                workoutTime = time,
                exercises = emptyList()
            )

            val intent = Intent(this, WorkoutActivity::class.java)
            intent.putExtra("workoutName", name)
            intent.putExtra("workoutTime", time)
            startActivity(intent)
        }
    }
}