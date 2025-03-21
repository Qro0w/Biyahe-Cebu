package com.example.biyahecebu

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: OnboardingPagerAdapter
    private lateinit var prefManager: PreferenceManager
    private lateinit var indicators: Array<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        prefManager = PreferenceManager(this)

        // Check Firestore first for user-specific onboarding status
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            FirebaseFirestore.getInstance().collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val onboardingCompleted = document.getBoolean("onboardingCompleted") ?: false
                        Log.d("Onboarding", "User ${user.uid} onboarding completed: $onboardingCompleted")

                        if (onboardingCompleted) {
                            // User has completed onboarding according to Firestore
                            launchMainActivity()
                            finish()
                            return@addOnSuccessListener
                        } else {
                            // Continue with onboarding
                            setupOnboarding()
                        }
                    } else {
                        // Document doesn't exist, use local preference as fallback
                        if (!prefManager.isFirstTimeLaunch()) {
                            launchMainActivity()
                            finish()
                            return@addOnSuccessListener
                        } else {
                            setupOnboarding()
                        }
                    }
                }
                .addOnFailureListener {
                    // On Firestore error, fall back to local preference
                    Log.e("Onboarding", "Error checking onboarding status", it)
                    if (!prefManager.isFirstTimeLaunch()) {
                        launchMainActivity()
                        finish()
                    } else {
                        setupOnboarding()
                    }
                }
        } else {
            // No user logged in, should not happen normally
            // Fall back to local preference
            if (!prefManager.isFirstTimeLaunch()) {
                launchMainActivity()
                finish()
                return
            } else {
                setupOnboarding()
            }
        }
    }


    // Move the ViewPager setup to a separate method
    private fun setupOnboarding() {
        viewPager = findViewById(R.id.view_pager)
        adapter = OnboardingPagerAdapter(this)
        viewPager.adapter = adapter

        // Initialize dot indicators
        setupIndicators()

        // Update indicator on page change
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateIndicators(position)
            }
        })
    }

    private fun setupIndicators() {
        val indicatorContainer = findViewById<ViewGroup>(R.id.indicators_container)
        val indicatorCount = adapter.itemCount
        indicators = Array(indicatorCount) { View(this) }

        // Create indicator views
        for (i in 0 until indicatorCount) {
            indicators[i] = View(this).apply {
                layoutParams = ViewGroup.LayoutParams(16, 16)
                background = ContextCompat.getDrawable(context, R.drawable.indicator_inactive)
            }
            indicatorContainer.addView(indicators[i])
        }

        // Set initial indicator
        updateIndicators(0)
    }

    private fun updateIndicators(position: Int) {
        for (i in indicators.indices) {
            indicators[i].background = ContextCompat.getDrawable(
                this,
                if (i == position) R.drawable.indicator_active else R.drawable.indicator_inactive
            )
        }
    }

    fun navigateToPage(position: Int) {
        viewPager.currentItem = position
    }

    fun completeOnboarding() {
        // Update Firestore to mark onboarding as completed for this user
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            FirebaseFirestore.getInstance().collection("users").document(user.uid)
                .update("onboardingCompleted", true)
                .addOnSuccessListener {
                    Log.d("Onboarding", "Onboarding completion status updated")
                }
                .addOnFailureListener {
                    Log.e("Onboarding", "Failed to update onboarding status")
                }
        }

        // You can keep the local preference too as a backup
        prefManager.setFirstTimeLaunch(false)

        launchMainActivity()
        finish()
    }

    private fun launchMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}