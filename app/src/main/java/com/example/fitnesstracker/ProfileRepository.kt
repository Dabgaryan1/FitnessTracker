package com.example.fitnesstracker

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

object ProfileRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private fun userDoc() = db.collection("users").document(auth.currentUser!!.uid)

    fun saveProfile(height: String, weight: String, age: String, onComplete: (Boolean) -> Unit) {
        val data = mapOf(
            "height" to height,
            "weight" to weight,
            "age" to age
        )
        userDoc().set(data, SetOptions.merge())
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun loadProfile(onResult: (height: String?, weight: String?, age: String?) -> Unit) {
        userDoc().get()
            .addOnSuccessListener { doc ->
                onResult(
                    doc.getString("height"),
                    doc.getString("weight"),
                    doc.getString("age")
                )
            }
            .addOnFailureListener { onResult(null, null, null) }
    }

    fun getCurrentEmail(): String? = auth.currentUser?.email

    fun reauthenticate(currentPassword: String, onResult: (Boolean) -> Unit) {
        val user = auth.currentUser ?: return onResult(false)
        val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
        user.reauthenticate(credential)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun changePassword(newPassword: String, onResult: (Boolean) -> Unit) {
        auth.currentUser?.updatePassword(newPassword)
            ?.addOnSuccessListener { onResult(true) }
            ?.addOnFailureListener { onResult(false) }
    }
}