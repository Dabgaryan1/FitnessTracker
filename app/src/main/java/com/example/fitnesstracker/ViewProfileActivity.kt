package com.example.fitnesstracker

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.enableEdgeToEdge
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth

class ViewProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_viewprofile)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.ViewProfile)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        BottomNavHelper.bottomNav(activity = this)

        val tvEmail = findViewById<TextView>(R.id.editUsername)
        val tvPassword = findViewById<TextView>(R.id.textPassword)
        val etHeight = findViewById<EditText>(R.id.editHeight)
        val etWeight = findViewById<EditText>(R.id.editWeight)
        val etAge = findViewById<EditText>(R.id.editAge)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        // Show logged-in email
        tvEmail.text = ProfileRepository.getCurrentEmail() ?: "No email"

        // Show dots for password, open dialog on click
        tvPassword.text = "••••••••"
        tvPassword.setOnClickListener { showPasswordDialog() }

        // Load saved profile data from Firestore
        ProfileRepository.loadProfile { height, weight, age ->
            runOnUiThread {
                height?.let { etHeight.setText(it) }
                weight?.let { etWeight.setText(it) }
                age?.let { etAge.setText(it) }
            }
        }

        // Save button
        btnSave.setOnClickListener {
            val height = etHeight.text.toString().trim()
            val weight = etWeight.text.toString().trim()
            val age = etAge.text.toString().trim()

            if (height.isEmpty() || weight.isEmpty() || age.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            ProfileRepository.saveProfile(height, weight, age) { success ->
                runOnUiThread {
                    val msg = if (success) "Profile saved!" else "Failed to save. Try again."
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Logout button
        btnLogout.setOnClickListener {
            WorkoutStorage.clearCurrentWorkout(this)
            Firebase.auth.signOut()
            startActivity(android.content.Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun showPasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_password, null)
        val etCurrent = dialogView.findViewById<EditText>(R.id.etCurrentPassword)
        val etNew = dialogView.findViewById<EditText>(R.id.etNewPassword)

        AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val current = etCurrent.text.toString()
                val newPass = etNew.text.toString()

                if (newPass.length < 6) {
                    Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                ProfileRepository.reauthenticate(current) { success ->
                    if (success) {
                        ProfileRepository.changePassword(newPass) { changed ->
                            runOnUiThread {
                                val msg = if (changed) "Password updated!" else "Update failed."
                                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this, "Current password is incorrect", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}