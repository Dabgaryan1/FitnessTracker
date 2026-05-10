package com.example.fitnesstracker

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import android.view.View
class ForgotPasswordActivity : AppCompatActivity() {
    //variables
    private lateinit var auth: FirebaseAuth
    private lateinit var etEmail: EditText
    private lateinit var tvResendEmail: TextView
    private lateinit var btnSendResetEmail: Button
    private lateinit var backButton: ImageButton

    // ADDED: cooldown length in milliseconds
    private val resendCooldown = 30000L

    //onCreate function
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgotpassword)

        //get firebase instance
        auth = FirebaseAuth.getInstance()

        //get variables from forgot password xml page
        etEmail = findViewById(R.id.etEmail)
        tvResendEmail = findViewById(R.id.tvResendEmail)
        btnSendResetEmail = findViewById(R.id.btnSendResetEmail)
        backButton = findViewById(R.id.backButton)

        //returns to previous page when back button is clicked
        backButton.setOnClickListener {
            finish()
        }

        //sends reset email through firebase connection when button is clicked
        btnSendResetEmail.setOnClickListener {
            val email = etEmail.text.toString().trim()

            //empty email error handling
            if (email.isEmpty()) {
                etEmail.error = "Please enter your email"
                etEmail.requestFocus()
                return@setOnClickListener
            }

            //sends reset email and confirms it to user
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(
                            this,
                            "Reset email sent,(check spam folder)",
                            Toast.LENGTH_SHORT
                        ).show()
                        btnSendResetEmail.visibility = View.GONE
                        tvResendEmail.visibility = View.VISIBLE
                    } else {
                        //error handling of failed password reset
                        Toast.makeText(
                            this,
                            task.exception?.message ?: "Password reset failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

        //resends email when resend email text is clicked
        tvResendEmail.setOnClickListener {
            val email = etEmail.text.toString().trim()

            //error handling for empty email field
            if (email.isEmpty()) {
                etEmail.error = "Please enter your email"
                etEmail.requestFocus()
                return@setOnClickListener
            }

            //resends email and confirms it to user
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(
                            this,
                            "Reset email resent to $email",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            //error message of failed resend
                            this,
                            task.exception?.message ?: "Resend failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }
}