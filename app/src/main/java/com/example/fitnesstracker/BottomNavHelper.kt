package com.example.fitnesstracker

import android.app.Activity
import android.content.Intent
import android.widget.ImageButton

object BottomNavHelper {
    fun bottomNav(activity: Activity) {
        val navLeft: ImageButton? = activity.findViewById(R.id.navLeft)
        val navCenter: ImageButton? = activity.findViewById(R.id.navCenter)
        val navRight: ImageButton? = activity.findViewById(R.id.navRight)
        val navViewWorkout: ImageButton? = activity.findViewById(R.id.navViewWorkout)
        //RepBot Navigation
        navLeft?.setOnClickListener {
            if (activity !is AiChatActivity) {
                activity.startActivity(Intent(activity, AiChatActivity::class.java))
            }
        }
        //New Or Current Workout Navigation
        navCenter?.setOnClickListener {
            val currentWorkout = WorkoutStorage.loadCurrentWorkout(activity)

            if (currentWorkout != null) {
                if (activity !is WorkoutActivity) {
                    activity.startActivity(Intent(activity, WorkoutActivity::class.java))
                }
            } else {
                if (activity !is CreateWorkoutActivity) {
                    activity.startActivity(Intent(activity, CreateWorkoutActivity::class.java))
                }
            }
        }
        //View Profile Navigation
        navRight?.setOnClickListener {
            if (activity !is ViewProfileActivity) {
                activity.startActivity(Intent(activity, ViewProfileActivity::class.java))
            }
        }
        //View Workouts Navigation
        navViewWorkout?.setOnClickListener {
            if (activity !is ViewWorkoutActivity) {
                activity.startActivity(Intent(activity, ViewWorkoutActivity::class.java))
            }
        }
    }
}