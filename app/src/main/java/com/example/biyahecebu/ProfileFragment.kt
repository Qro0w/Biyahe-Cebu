package com.example.biyahecebu

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var nameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var profileImageView: ImageView
    private lateinit var progressBar: ProgressBar // Optional: If you want to show loading state
    private lateinit var btnEditAccount: Button
    private lateinit var fragmentContainer: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Reference UI elements
        nameTextView = view.findViewById(R.id.tvName)
        emailTextView = view.findViewById(R.id.tvEmail)
        profileImageView = view.findViewById(R.id.profileImageView)
        btnEditAccount = view.findViewById(R.id.btnEditAccount)
        fragmentContainer = view.findViewById(R.id.fragment_container)
        progressBar = view.findViewById(R.id.progressBar)
        val logoutBtn = view.findViewById<LinearLayout>(R.id.signoutbtn)

        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(activity, "User is not authenticated", Toast.LENGTH_SHORT).show()
            // Optionally, redirect to login screen or handle the error
        }
        if (user != null) {
            Log.d("User UID", user.uid)
            fetchUserData(user)  // Fetch and display user data
        } else {
            Toast.makeText(activity, "User not logged in", Toast.LENGTH_SHORT).show()
        }

        logoutBtn.setOnClickListener {
            logoutUser()
        }

        /*   ----------BOTTOM SHEET TEST-----------

                // Set onClickListener for Edit Account button
        btnEditAccount.setOnClickListener {
            val bottomSheetFragment = BottomSheetEditAccountFragment()
            bottomSheetFragment.show(requireActivity().supportFragmentManager, bottomSheetFragment.tag)
        }

        ------------------WITHIN PAGE SHEET TEST--------------


        btnEditAccount.setOnClickListener {
            // Navigate to the EditAccountFragment
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, EditAccountFragment())
            transaction.addToBackStack(null)  // Optional: To allow back navigation
            transaction.commit()
        }*/



        btnEditAccount.setOnClickListener {
            val editAccountFragment = EditAccountFragment()
            editAccountFragment.show(parentFragmentManager, "editAccountFragment")
        }


        return view
    }

    private fun showEditAccountBottomSheet() {
        val view = layoutInflater.inflate(R.layout.bottom_sheet_edit_account, null)
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        dialog.setContentView(view)

        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        if (bottomSheet != null) {
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED // Make it fully expanded
            behavior.peekHeight = resources.displayMetrics.heightPixels // Set height to full screen
        }

        // Add your button click listener for saving changes (e.g., updating profile info)
        val saveBtn = view.findViewById<Button>(R.id.btnSaveAccountChanges)
        saveBtn.setOnClickListener {
            // Handle saving the new user data here (e.g., updating Firebase)
            val newName = view.findViewById<EditText>(R.id.etAccountName).text.toString()
            val newEmail = view.findViewById<EditText>(R.id.etAccountEmail).text.toString()

            // Save the new name and email (e.g., to Firebase)
            updateUserData(newName, newEmail)

            dialog.dismiss() // Close the bottom sheet
        }

        dialog.show()
    }

    private fun updateUserData(name: String, email: String) {
        val user = auth.currentUser
        if (user != null) {
            val userRef = firestore.collection("users").document(user.uid)
            val updatedData: MutableMap<String, Any> = mutableMapOf(
                "name" to name,
                "email" to email
            )

            userRef.update(updatedData)
                .addOnSuccessListener {
                    // Successfully updated user data
                    Toast.makeText(activity, "Account updated successfully", Toast.LENGTH_SHORT).show()
                    // Optionally, refresh the UI to reflect updated data
                    nameTextView.text = name
                    emailTextView.text = email
                }
                .addOnFailureListener {
                    // Failed to update user data
                    Toast.makeText(activity, "Failed to update account", Toast.LENGTH_SHORT).show()
                }
        }
    }


    private fun fetchUserData(user: FirebaseUser) {
        val docRef = firestore.collection("users").document(user.uid)

        docRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Document found, display the user data
                    val name = document.getString("name")
                    val email = document.getString("email")

                    nameTextView.text = name ?: "No Name"
                    emailTextView.text = email

                    if (user.photoUrl != null) {
                        Glide.with(this).load(user.photoUrl).into(profileImageView)
                    } else {
                        profileImageView.setImageResource(R.drawable.profile_placeholder)
                    }
                } else {
                    // No document found, create a new user document
                    createUserDocument(user)
                }
            }
            .addOnFailureListener {
                Toast.makeText(activity, "Failed to fetch user data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createUserDocument(user: FirebaseUser) {
        val userMap = hashMapOf(
            "email" to user.email,
            "name" to user.displayName,
            "uid" to user.uid
        )

        firestore.collection("users").document(user.uid)
            .set(userMap)
            .addOnSuccessListener {
                // Successfully created the document
                Toast.makeText(activity, "New user document created", Toast.LENGTH_SHORT).show()
                // Optionally, refresh the UI here after creating the document
                fetchUserData(user)  // Now fetch the user data again
            }
            .addOnFailureListener {
                Toast.makeText(activity, "Failed to create user document", Toast.LENGTH_SHORT).show()
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
