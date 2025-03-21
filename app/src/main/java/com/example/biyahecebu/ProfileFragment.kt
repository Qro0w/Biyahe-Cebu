package com.example.biyahecebu

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var nameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var profileImageView: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnEditAccount: Button
    private lateinit var fragmentContainer: View
    private lateinit var subscribeButton: Button
    private lateinit var manageSubscriptionButton: Button
    private lateinit var subscriptionText: TextView
    private val BILLING_REQUEST_CODE = 1001
    private var isGoogleUser = false

    // Mock subscription states
    private var isSubscribed = false
    private var subscriptionEndDate: Date? = null

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        nameTextView.alpha = 0.5f
        emailTextView.alpha = 0.5f
        btnEditAccount.isEnabled = false
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
        nameTextView.alpha = 1.0f
        emailTextView.alpha = 1.0f
        btnEditAccount.isEnabled = true
    }

    override fun onResume() {
        super.onResume()
        // Refresh user data when returning to this fragment
        auth.currentUser?.let {
            fetchUserData(it)
            checkSubscriptionStatus(it.uid)
        }
    }

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
        val logoutBtn = view.findViewById<LinearLayout>(R.id.signoutbtn)
        subscribeButton = view.findViewById(R.id.subscribeButton)
        manageSubscriptionButton = view.findViewById(R.id.manageSubscriptionButton)
        subscriptionText = view.findViewById(R.id.subscriptionText)
        progressBar = view.findViewById(R.id.progressBar) // Make sure to add this to your layout

        btnEditAccount.text = "Edit Profile"

        // Check if user is signed in with Google
        val user = auth.currentUser
        if (user != null) {
            // Check authentication providers
            for (profile in user.providerData) {
                if (profile.providerId == "google.com") {
                    isGoogleUser = true
                    Log.d("ProfileFragment", "User is authenticated with Google")
                    break
                }
            }

            fetchUserData(user)  // Fetch and display user data
            checkSubscriptionStatus(user.uid)  // Check subscription status
        } else {
            Toast.makeText(activity, "User not logged in", Toast.LENGTH_SHORT).show()
        }

        logoutBtn.setOnClickListener {
            logoutUser()
        }

        btnEditAccount.setOnClickListener {
            val editAccountFragment = EditAccountFragment()
            editAccountFragment.show(parentFragmentManager, "editAccountFragment")
        }

        // Set up subscription button click listeners
        subscribeButton.setOnClickListener {
            showSubscriptionDialog()
        }

        manageSubscriptionButton.setOnClickListener {
            showManageSubscriptionDialog()
        }

        return view
    }

    private fun fetchUserData(user: FirebaseUser) {
        showLoading()

        val docRef = firestore.collection("users").document(user.uid)

        docRef.get()
            .addOnSuccessListener { document ->

                hideLoading()

                if (document.exists()) {
                    // Document found, display the user data
                    val name = document.getString("name")
                    val email = document.getString("email")
                    val photoUrl = document.getString("photoUrl")

                    nameTextView.text = name ?: "No Name"
                    emailTextView.text = email

                    // For Google users, always use their Google profile image
                    if (isGoogleUser && user.photoUrl != null) {
                        activity?.let {
                            Glide.with(it)
                                .load(user.photoUrl)
                                .placeholder(R.drawable.profile_placeholder)
                                .into(profileImageView)

                            // Log for debugging
                            Log.d("ProfileImage", "Loading Google profile image: ${user.photoUrl}")
                        }
                    }
                    // For regular users, use their custom image if available
                    else if (!isGoogleUser && !photoUrl.isNullOrEmpty()) {
                        activity?.let {
                            Glide.with(it)
                                .load(photoUrl)
                                .placeholder(R.drawable.profile_placeholder)
                                .into(profileImageView)

                            // Log for debugging
                            Log.d("ProfileImage", "Loading custom profile image: $photoUrl")
                        }
                    }
                    // Use placeholder if no image is available
                    else {
                        profileImageView.setImageResource(R.drawable.profile_placeholder)
                        Log.d("ProfileImage", "Using default placeholder")
                    }

                } else {
                    // No document found, create a new user document
                    createUserDocument(user)
                }
            }
            .addOnFailureListener {
                hideLoading()

                Toast.makeText(activity, "Failed to fetch user data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createUserDocument(user: FirebaseUser) {
        // Check if this is a Google user by provider ID
        var isGoogle = false
        for (profile in user.providerData) {
            if (profile.providerId == "google.com") {
                isGoogle = true
                break
            }
        }

        val userMap = hashMapOf(
            "email" to user.email,
            "name" to user.displayName,
            "uid" to user.uid,
            "photoUrl" to (user.photoUrl?.toString() ?: ""),
            "isGoogleUser" to isGoogle,  // Add flag to identify Google users
            "isSubscribed" to false,     // Initialize subscription status
            "subscriptionEndDate" to null
        )

        firestore.collection("users").document(user.uid)
            .set(userMap)
            .addOnSuccessListener {
                Toast.makeText(activity, "New user document created", Toast.LENGTH_SHORT).show()
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

    // Subscription-related methods

    private fun checkSubscriptionStatus(userId: String) {
        showLoading()

        val userRef = firestore.collection("users").document(userId)
        userRef.get()
            .addOnSuccessListener { document ->
                hideLoading()

                if (document.exists()) {
                    isSubscribed = document.getBoolean("isSubscribed") ?: false

                    // Get subscription end date if it exists
                    val dateTimestamp = document.getTimestamp("subscriptionEndDate")
                    subscriptionEndDate = dateTimestamp?.toDate()

                    // Update UI based on subscription status
                    updateSubscriptionUI()
                }
            }
            .addOnFailureListener {
                hideLoading()
                Log.e("Subscription", "Failed to check subscription status", it)
                Toast.makeText(activity, "Failed to check subscription status", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateSubscriptionUI() {
        if (isSubscribed && subscriptionEndDate != null) {
            // User is subscribed
            val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
            val formattedDate = dateFormat.format(subscriptionEndDate!!)

            subscriptionText.text = "Premium Member\nExpires: $formattedDate"
            subscribeButton.visibility = View.GONE
            manageSubscriptionButton.visibility = View.VISIBLE
        } else {
            // User is not subscribed
            subscriptionText.text = "Unlock premium features!"
            subscribeButton.visibility = View.VISIBLE
            manageSubscriptionButton.visibility = View.GONE
        }
    }

    private fun showSubscriptionDialog() {
        // Create a dialog for subscription options
        val subscriptionDialog = SubscriptionDialogFragment { planId ->
            // This is the callback that will be called when user selects a plan
            processSubscription(planId)
        }
        subscriptionDialog.show(parentFragmentManager, "subscriptionDialog")
    }

    private fun showManageSubscriptionDialog() {
        // Create a dialog for managing subscription
        val manageDialog = ManageSubscriptionDialogFragment(
            subscriptionEndDate,
            { cancelSubscription() },  // Cancel callback
            { renewSubscription() }    // Renew callback
        )
        manageDialog.show(parentFragmentManager, "manageSubscriptionDialog")
    }

    private fun processSubscription(planId: String) {
        // Show loading
        showLoading()

        // In a real app, you would handle the billing process here
        // For this mock, we'll simulate a successful subscription purchase

        // Calculate subscription end date based on plan
        val calendar = Calendar.getInstance()
        when (planId) {
            "monthly" -> calendar.add(Calendar.MONTH, 1)
            "yearly" -> calendar.add(Calendar.YEAR, 1)
            else -> calendar.add(Calendar.MONTH, 1) // Default to monthly
        }

        val newEndDate = calendar.time

        // Update user document with subscription info
        val userId = auth.currentUser?.uid ?: return

        val updateData = hashMapOf(
            "isSubscribed" to true,
            "subscriptionEndDate" to newEndDate,
            "subscriptionPlan" to planId,
            "subscriptionStartDate" to Date()
        )

        firestore.collection("users").document(userId)
            .update(updateData as Map<String, Any>)
            .addOnSuccessListener {
                hideLoading()
                isSubscribed = true
                subscriptionEndDate = newEndDate
                updateSubscriptionUI()

                // Show success message
                Toast.makeText(activity, "Subscription successful!", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                hideLoading()
                Log.e("Subscription", "Failed to update subscription", e)
                Toast.makeText(activity, "Subscription failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun cancelSubscription() {
        showLoading()

        val userId = auth.currentUser?.uid ?: return

        val updateData = hashMapOf(
            "isSubscribed" to false,
            "subscriptionEndDate" to subscriptionEndDate  // Keep the end date for reference
        )

        firestore.collection("users").document(userId)
            .update(updateData as Map<String, Any>)
            .addOnSuccessListener {
                hideLoading()

                // Show confirmation but don't change UI until actual end date
                Toast.makeText(
                    activity,
                    "Your subscription has been canceled but will remain active until the expiration date.",
                    Toast.LENGTH_LONG
                ).show()

                // Close the dialog
                parentFragmentManager.findFragmentByTag("manageSubscriptionDialog")?.let {
                    if (it is androidx.fragment.app.DialogFragment) {
                        it.dismiss()
                    }
                }
            }
            .addOnFailureListener { e ->
                hideLoading()
                Log.e("Subscription", "Failed to cancel subscription", e)
                Toast.makeText(activity, "Failed to cancel subscription: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun renewSubscription() {
        // Simply show the subscription options dialog again
        showSubscriptionDialog()

        // Close the manage dialog
        parentFragmentManager.findFragmentByTag("manageSubscriptionDialog")?.let {
            if (it is androidx.fragment.app.DialogFragment) {
                it.dismiss()
            }
        }
    }
}