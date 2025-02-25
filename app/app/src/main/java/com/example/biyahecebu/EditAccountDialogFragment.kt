package com.example.biyahecebu

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.bumptech.glide.Glide

class EditAccountDialogFragment : Fragment() {

    private lateinit var profileImageView: ImageView
    private lateinit var editIcon: ImageView
    private lateinit var etFirstName: EditText
    private lateinit var etEmail: EditText
    private lateinit var tvResetPasswordBtn: TextView
    private lateinit var backButton: ImageView

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
        backButton = view.findViewById(R.id.backButton)

        // Load user data
        loadUserData()

        // Back button logic
        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Camera icon click (for updating profile image)
        editIcon.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, REQUEST_IMAGE_PICK)
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
            val selectedImageUri: Uri = data.data!!
            profileImageView.setImageURI(selectedImageUri)
            uploadProfileImage(selectedImageUri)
        }
    }

    private fun loadUserData() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            etFirstName.setText(user.displayName)
            etEmail.setText(user.email)

            Glide.with(this)
                .load(user.photoUrl)
                .into(profileImageView)
        }
    }

    private fun uploadProfileImage(uri: Uri) {
        val storageReference = FirebaseStorage.getInstance().reference
        val imageRef = storageReference.child("profile_images/${FirebaseAuth.getInstance().currentUser?.uid}")
        val uploadTask = imageRef.putFile(uri)

        uploadTask.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    val user = FirebaseAuth.getInstance().currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setPhotoUri(downloadUrl)
                        .build()

                    user?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener {
                            if (it.isSuccessful) {
                                Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }
        }
    }

    private fun updateUserData(name: String, email: String) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val userUpdates = mapOf(
                "name" to name,
                "email" to email
            )

            FirebaseFirestore.getInstance().collection("users").document(user.uid)
                .update(userUpdates)
                .addOnSuccessListener {
                    Toast.makeText(context, "Account updated!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to update account", Toast.LENGTH_SHORT).show()
                }
        }
    }
}

