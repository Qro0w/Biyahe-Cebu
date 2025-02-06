package com.example.biyahecebu

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment(R.layout.fragment_profile) {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance() // Initialize Firestore

        val logoutBtn = view.findViewById<Button>(R.id.logoutBtn)
        //val nameTextView = view.findViewById<TextView>(R.id.nameTextView)

        // Fetch user data from Firestore and display
        val user = auth.currentUser
        if (user != null) {
            fetchUserData(user)  // Fetch and display user data
        }

        logoutBtn.setOnClickListener {
            logoutUser()
        }

        return view
    }

    private fun fetchUserData(user: FirebaseUser) {
        val docRef = firestore.collection("users").document(user.uid)
        docRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name")
                    val email = document.getString("email")
                    // Display user data in the UI
                    //view?.findViewById<TextView>(R.id.nameTextView)?.text = name ?: "No Name"
                    //view?.findViewById<TextView>(R.id.emailTextView)?.text = email
                }
            }
            .addOnFailureListener {
                Toast.makeText(activity, "Failed to fetch user data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun logoutUser() {
        auth.signOut()

        // If you're using Google Sign-In, sign out from Google as well
        val googleSignInClient = GoogleSignIn.getClient(requireActivity(), GoogleSignInOptions.DEFAULT_SIGN_IN)
        googleSignInClient.signOut()

        // Navigate back to LoginActivity after logging out
        val intent = Intent(activity, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish() // Close ProfileFragment and prevent going back
    }
}
