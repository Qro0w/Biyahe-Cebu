package com.example.biyahecebu

import android.content.Intent
import android.graphics.Rect
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
    private lateinit var firestore: FirebaseFirestore  // Firestore instance

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()  // Initialize Firestore

        if (auth.currentUser != null) {
            navigateToMainActivity()
        }

        val loginBtn = findViewById<Button>(R.id.loginBtn)
        val googleSignInBtn = findViewById<LinearLayout>(R.id.googleSignInBtn)
        val createAccountBtn = findViewById<Button>(R.id.createAccountBtn)

        googleSignInBtn.setOnClickListener { googleSignIn() }
        loginBtn.setOnClickListener { loginUser() }
        createAccountBtn.setOnClickListener { showRegisterBottomSheet() }

        setupGoogleSignIn()
    }

    private fun loginUser() {
        val email = findViewById<EditText>(R.id.emailInput).text.toString()
        val password = findViewById<EditText>(R.id.passwordInput).text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                navigateToMainActivity()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showRegisterBottomSheet() {
        val view = layoutInflater.inflate(R.layout.bottom_sheet_register, null)
        val dialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        dialog.setContentView(view)

        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        if (bottomSheet != null) {
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED // Make it fully expanded
            behavior.peekHeight = resources.displayMetrics.heightPixels // Set height to full screen
        }

        val registerBtn = view.findViewById<Button>(R.id.registerBtn)
        val googleSignInBtn = view.findViewById<Button>(R.id.googleSignInBtn)

        registerBtn.setOnClickListener { registerUser(view, dialog) }
        googleSignInBtn.setOnClickListener { googleSignIn() }

        dialog.show()
    }


    private fun registerUser(view: View, dialog: BottomSheetDialog) {
        val email = view.findViewById<EditText>(R.id.registerEmail).text.toString()
        val password = view.findViewById<EditText>(R.id.registerPassword).text.toString()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val user = auth.currentUser
                if (user != null) {
                    saveUserDataToFirestore(user)  // Save additional user data to Firestore
                }
                Toast.makeText(this, "Registered Successfully", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                navigateToMainActivity()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Registration Failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveUserDataToFirestore(user: FirebaseUser) {
        val userName = user.displayName ?: "New User"  // Use Google's display name, fallback to "New User"
        val userMap = hashMapOf(
            "name" to userName,  // Use the display name from Google Sign-In
            "email" to user.email,
            "uid" to user.uid
        )

        firestore.collection("users").document(user.uid) // Using the user's UID as the document ID
            .set(userMap)
            .addOnSuccessListener {
                // Successfully saved user data
                Log.d("Firestore", "User data saved successfully.")
            }
            .addOnFailureListener {
                // Failed to save user data
                Log.e("Firestore", "Error saving user data.")
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
                Toast.makeText(this, "Google Sign-In Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    navigateToMainActivity() // Redirect after successful Google login
                } else {
                    Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Prevent going back to LoginActivity
    }
}
