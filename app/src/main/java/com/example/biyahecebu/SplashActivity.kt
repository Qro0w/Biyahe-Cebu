package com.example.biyahecebu

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set the splash screen layout
        setContentView(R.layout.activity_splash)

        // Delay transition to MainActivity for a short period (e.g., 2 seconds)
        Handler().postDelayed({
            // Start MainActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // Close SplashActivity so it doesn't appear when pressing back
        }, 2000) // Adjust the delay as needed
    }
}
