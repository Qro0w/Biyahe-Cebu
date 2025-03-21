package com.example.biyahecebu

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.biyahecebu.databinding.ActivityLoginBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseUser

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityLoginBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Check if user is already logged in
        if (auth.currentUser != null) {
            navigateToMainActivity(false)
        }

        // Set up click listeners
        binding.loginBtn.setOnClickListener { loginUser() }
        binding.googleSignInBtn.setOnClickListener { googleSignIn() }
        binding.createAccountBtn.setOnClickListener { showRegistrationBottomSheet() }

        setupGoogleSignIn()
    }

    private fun loginUser() {
        val email = binding.emailInput.text.toString()
        val password = binding.passwordInput.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                // Check if user is new in Firestore
                val user = auth.currentUser
                if (user != null) {
                    firestore.collection("users").document(user.uid)
                        .get()
                        .addOnSuccessListener { document ->
                            Log.d("LoginActivity", "User ${user.uid} - Document exists: ${document.exists()}")
                            if (document.exists()) {
                                val onboardingCompleted = document.getBoolean("onboardingCompleted") ?: false
                                Log.d("LoginActivity", "onboardingCompleted value: $onboardingCompleted")
                                navigateToMainActivity(!onboardingCompleted)
                            } else {
                                navigateToMainActivity(true)
                            }
                        }
                } else {
                    navigateToMainActivity(false)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showRegistrationBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_register, null)
        bottomSheetDialog.setContentView(bottomSheetView)

        // Make bottom sheet expanded by default
        val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        if (bottomSheet != null) {
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            // Don't set the peekHeight to full screen as it can cause issues
            // Instead, let it expand naturally
        }

        // Set up registration button
        val registerBtn = bottomSheetView.findViewById<Button>(R.id.registerBtn)
        registerBtn.setOnClickListener {
            registerUser(bottomSheetView, bottomSheetDialog)
        }

        // Set up Google Sign In button in the bottom sheet
        val googleSignInBtn = bottomSheetView.findViewById<Button>(R.id.googleSignUpBtn)
        googleSignInBtn.setOnClickListener {
            googleSignIn()
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun registerUser(view: View, dialog: BottomSheetDialog) {
        val email = view.findViewById<EditText>(R.id.registerEmail).text.toString()
        val password = view.findViewById<EditText>(R.id.registerPassword).text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val user = auth.currentUser
                if (user != null) {
                    saveUserDataToFirestore(user)
                }
                Toast.makeText(this, "Registered Successfully", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                navigateToMainActivity(true) // true = new user, always show onboarding
            }
            .addOnFailureListener {
                Toast.makeText(this, "Registration Failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveUserDataToFirestore(user: FirebaseUser, isNewUser: Boolean = true) {
        val userName = user.displayName ?: "New User"
        val photoUrl = user.photoUrl?.toString() ?: ""

        val userMap = hashMapOf(
            "name" to userName,
            "email" to user.email,
            "uid" to user.uid,
            "photoUrl" to photoUrl,
            "onboardingCompleted" to !isNewUser  // false for new users, true for existing
        )

        firestore.collection("users").document(user.uid)
            .set(userMap)
            .addOnSuccessListener {
                Log.d("Firestore", "User data saved successfully.")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error saving user data: ${e.message}")
            }
    }

    private fun setupGoogleSignIn() {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, options)
    }

    private fun googleSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign-In Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        firestore.collection("users").document(user.uid)
                            .get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    // Check if onboarding is completed
                                    val onboardingCompleted = document.getBoolean("onboardingCompleted") ?: false

                                    if (onboardingCompleted) {
                                        // User has completed onboarding
                                        navigateToMainActivity(false)
                                    } else {
                                        // User exists but hasn't completed onboarding
                                        navigateToMainActivity(true)
                                    }
                                } else {
                                    // New user - create data
                                    saveUserDataToFirestore(user, true)
                                    navigateToMainActivity(true)
                                }
                            }
                    }
                } else {
                    Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateToMainActivity(isNewUser: Boolean) {
        if (isNewUser) {
            // New user - show onboarding
            val intent = Intent(this, OnboardingActivity::class.java)
            startActivity(intent)
        } else {
            // Existing user - skip onboarding
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
        finish()
    }
}