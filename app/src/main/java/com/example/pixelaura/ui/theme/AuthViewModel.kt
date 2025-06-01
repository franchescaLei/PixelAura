package com.example.pixelaura.ui.theme

import android.util.Log
import android.util.Patterns
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore

open class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    // Signout
    fun signOut() {
        auth.signOut()
        _authState.value = AuthState.UnAuthenticated
    }
    init {
        checkAuthStatus()
    }

    // Check auth
    private fun checkAuthStatus() {
        if (auth.currentUser == null) {
            _authState.value = AuthState.UnAuthenticated
        } else {
            _authState.value = AuthState.Authenticated
        }
    }

    val errorMessage = mutableStateOf("")


    // Sign up
    fun signUp(email: String, password: String, confirmPassword: String, callback: (Result) -> Unit) {
        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            callback(Result.Failure("Please enter all fields"))
            return
        }

        if (password != confirmPassword) {
            callback(Result.Failure("Passwords do not match"))
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        // Generate handle from email prefix
                        val handle = "@" + email.substringBefore("@")
                        val defaultImgurProfileUrl = "https://i.imgur.com/Z0IXdLS.png"
                        // Firestore user data
                        val userData = hashMapOf(
                            "username" to email.substringBefore("@"),
                            "email" to email,
                            "handle" to handle, // ✅ ADDED handle here
                            "profile_picture" to "defaultImgurProfileUrl",
                            "followers_count" to 0,
                            "following_count" to 0,
                            "b_day" to "",
                            "header" to "",
                            "bio" to "",
                            "following" to emptyList<String>()
                        )

                        val firestore = FirebaseFirestore.getInstance()
                        firestore.collection("users")
                            .document(user.uid)
                            .set(userData)
                            .addOnSuccessListener {
                                callback(Result.Success("Sign up successful"))
                            }
                            .addOnFailureListener { exception ->
                                callback(Result.Failure("Error saving user data: ${exception.message}"))
                            }
                    }
                } else {
                    val errorMessage = task.exception?.message ?: "Something went wrong"
                    callback(Result.Failure(errorMessage))
                }
            }
    }


    //handle email logins
    open fun login(email: String, password: String, onResult: (Result) -> Unit) {
        if (email.isEmpty() || password.isEmpty()) {
            onResult(Result.Failure("Please enter all fields"))
            return
        }

        if (!email.matches(Regex("^[A-Za-z0-9._%+-]+@(gmail\\.com|yahoo\\.com|outlook\\.com)$"))) {
            onResult(Result.Failure("Invalid email format"))
            return
        }

        // attempt login and check error codes
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(Result.Success("Login successful"))
                } else {
                    val exception = task.exception
                    when (exception) {
                        is FirebaseAuthInvalidUserException -> {
                            //email is NOT registered
                            onResult(Result.Failure("This email is not registered to PixelAura"))
                        }
                        is FirebaseAuthInvalidCredentialsException -> {
                            //password is wrong
                            onResult(Result.Failure("Incorrect password"))
                        }
                        else -> {
                            //other error
                            onResult(Result.Failure("Error logging in, please try again"))
                        }
                    }
                }
            }
    }


    fun resetPassword(email: String, callback: (Result) -> Unit) {
        if (email.isBlank()) {
            callback(Result.Failure("Please enter your email"))
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            callback(Result.Failure("Please enter a valid email"))
            return
        }

        // Check if email exists in Firestore
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    // Email not found in Firestore
                    callback(Result.Failure("This email is not registered"))
                } else {
                    // Email exists, show success message immediately
                    callback(Result.Success("Please check your email and log in with your new password"))

                    // Now send the reset email
                    auth.sendPasswordResetEmail(email)
                }
            }
            .addOnFailureListener { exception ->
                callback(Result.Failure("Error checking email in database: ${exception.message}"))
            }
    }


    sealed class Result {
        data class Success(val successMessage: String) : Result()
        data class Failure(val errorMessage: String) : Result()
    }


    sealed class AuthState {
        object Authenticated : AuthState()
        object UnAuthenticated : AuthState()
        object Loading : AuthState()
        data class Error(val message: String) : AuthState()
    }
}
