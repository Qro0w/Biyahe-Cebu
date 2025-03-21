package com.example.biyahecebu

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.bumptech.glide.Glide

class EditAccountDialogFragment : DialogFragment() {

    private lateinit var profileImageView: ImageView
    private lateinit var editIcon: ImageView
    private lateinit var etFirstName: EditText
    private lateinit var etEmail: EditText
    private lateinit var tvResetPasswordBtn: TextView
    private lateinit var backButton: ImageView
    private var isGoogleUser = false

    private val REQUEST_IMAGE_PICK = 1001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize the views
        profileImageView = view.findViewById(R.id.editProfileImage)
        editIcon = view.findViewById(R.id.editIcon)
        etFirstName = view.findViewById(R.id.etFirstName)
        etEmail = view.findViewById(R.id.etEmail)
        tvResetPasswordBtn = view.findViewById(R.id.tvResetPasswordBtn)

        // Check if user is signed in with Google - THIS IS THE KEY PART
        val user = FirebaseAuth.getInstance().currentUser

        if (user != null) {
            // Check authentication providers
            for (profile in user.providerData) {
                if (profile.providerId == "google.com") {
                    isGoogleUser = true
                    Log.d("EditAccount", "User is authenticated with Google")
                    break
                }
            }
        }

        // Immediately modify UI for Google users
        if (isGoogleUser) {
            editIcon.visibility = View.GONE // Completely hide it for Google users
            // Or make it obvious it's disabled:
            // editIcon.alpha = 0.3f
            // editIcon.setColorFilter(android.graphics.Color.GRAY)
        }

        // Load user data
        loadUserData()

        // Camera icon click (for updating profile image)
        editIcon.setOnClickListener {
            if (isGoogleUser) {
                Toast.makeText(context, "Google account users cannot change profile picture", Toast.LENGTH_LONG).show()
            } else {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(intent, REQUEST_IMAGE_PICK)
            }
        }

        // Reset password button click (shows bottom sheet)
        tvResetPasswordBtn.setOnClickListener {
            val resetPasswordFragment = ResetPasswordBottomSheetFragment()
            resetPasswordFragment.show(parentFragmentManager, resetPasswordFragment.tag)
        }

        // Save changes button
        val btnSaveChanges: Button = view.findViewById(R.id.btnSaveChanges)
        btnSaveChanges.setOnClickListener {
            val name = etFirstName.text.toString()
            val email = etEmail.text.toString()

            updateUserData(name, email)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK && data != null) {
            if (!isGoogleUser) {
                val selectedImageUri: Uri = data.data!!
                profileImageView.setImageURI(selectedImageUri)
                uploadProfileImage(selectedImageUri)
            }
        }
    }

    private fun loadUserData() {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        etFirstName.setText(user.displayName)
        etEmail.setText(user.email)

        FirebaseFirestore.getInstance().collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val photoUrl = document.getString("photoUrl")

                    if (isGoogleUser && user.photoUrl != null) {
                        // For Google users, always use their Google profile picture
                        Glide.with(requireContext())
                            .load(user.photoUrl)
                            .placeholder(R.drawable.profile_placeholder)
                            .into(profileImageView)

                        // Log for debugging
                        Log.d("ProfileImage", "Loading Google profile image: ${user.photoUrl}")
                    }
                    else if (!isGoogleUser && !photoUrl.isNullOrEmpty()) {
                        // For regular users, use their custom image if available
                        Glide.with(requireContext())
                            .load(photoUrl)
                            .placeholder(R.drawable.profile_placeholder)
                            .into(profileImageView)

                        // Log for debugging
                        Log.d("ProfileImage", "Loading custom profile image: $photoUrl")
                    }
                    else {
                        // Default placeholder
                        profileImageView.setImageResource(R.drawable.profile_placeholder)
                        Log.d("ProfileImage", "Using default placeholder")
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error loading user data: ${e.message}")
                profileImageView.setImageResource(R.drawable.profile_placeholder)
            }
    }

    private fun uploadProfileImage(uri: Uri) {
        // Double-check that this is not a Google user
        if (isGoogleUser) {
            Toast.makeText(context, "Google account users cannot change profile picture", Toast.LENGTH_LONG).show()
            return
        }

        val user = FirebaseAuth.getInstance().currentUser ?: return
        val storageReference = FirebaseStorage.getInstance().reference
        val imageRef = storageReference.child("profile_images/${user.uid}")

        val uploadTask = imageRef.putFile(uri)
        uploadTask.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    // 1. Update Firebase Auth profile
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setPhotoUri(downloadUrl)
                        .build()

                    user.updateProfile(profileUpdates)
                        .addOnCompleteListener { profileTask ->
                            if (profileTask.isSuccessful) {
                                // 2. Update Firestore with the new image URL
                                val updates = mapOf(
                                    "photoUrl" to downloadUrl.toString()
                                )

                                FirebaseFirestore.getInstance().collection("users").document(user.uid)
                                    .update(updates)
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Profile image updated successfully!", Toast.LENGTH_SHORT).show()
                                        dismiss()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context, "Failed to update profile image in database: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                Toast.makeText(context, "Failed to update profile: ${profileTask.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            } else {
                Toast.makeText(context, "Upload failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUserData(name: String, email: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        // Update Firebase Auth display name
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .build()

        user.updateProfile(profileUpdates)
            .addOnCompleteListener { profileTask ->
                if (profileTask.isSuccessful) {
                    // Update Firestore
                    val userUpdates = mapOf(
                        "name" to name,
                        "email" to email
                    )

                    FirebaseFirestore.getInstance().collection("users").document(user.uid)
                        .update(userUpdates)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Account updated successfully!", Toast.LENGTH_SHORT).show()
                            dismiss()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Failed to update account: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(context, "Failed to update profile", Toast.LENGTH_SHORT).show()
                }
            }
    }
}