package com.example.fitnesstracker

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.Firebase

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var etNewEmail: EditText
    private lateinit var etNewPassword: EditText
    private lateinit var etRepeatPassword: EditText
    private lateinit var btnConfirmAccount: Button
    private lateinit var backButton: ImageButton
    private lateinit var loginError: TextView

    companion object {
        private const val TAG = "SignUpActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_createaccount)

        auth = Firebase.auth

        etNewEmail = findViewById(R.id.etNewEmail)
        etNewPassword = findViewById(R.id.etNewPassword)
        etRepeatPassword = findViewById(R.id.etRepeatPassword)
        btnConfirmAccount = findViewById(R.id.btnConfirmAccount)
        backButton = findViewById(R.id.backButton)
        loginError = findViewById(R.id.loginError)

        backButton.setOnClickListener {
            finish()
        }

        btnConfirmAccount.setOnClickListener {
            val email = etNewEmail.text.toString().trim()
            val password = etNewPassword.text.toString().trim()
            val repeatPassword = etRepeatPassword.text.toString().trim()

            loginError.visibility = View.GONE

            if (email.isEmpty() || password.isEmpty() || repeatPassword.isEmpty()) {
                loginError.text = "Please fill in all fields"
                loginError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (password != repeatPassword) {
                loginError.text = "Passwords do not match"
                loginError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (password.length < 6) {
                loginError.text = "Password must be at least 6 characters"
                loginError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "createUserWithEmail:success")
                        Toast.makeText(this, "Account created successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.exception)
                        loginError.text = task.exception?.message ?: "Authentication failed"
                        loginError.visibility = View.VISIBLE
                    }
                }
        }
    }
}