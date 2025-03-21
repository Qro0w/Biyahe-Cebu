package com.example.biyahecebu

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.biyahecebu.R

class TermsConditionsDialog : DialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_terms_conditions, container, false)

        val closeButton = view.findViewById<Button>(R.id.btnClose)
        val termsTextView = view.findViewById<TextView>(R.id.tvTerms)
        val scrollView = view.findViewById<ScrollView>(R.id.TermsCons)

        termsTextView.text = getStyledTermsAndConditionsText()
        termsTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        termsTextView.textSize = 14f
        termsTextView.setLineSpacing(1.5f, 1.5f)

        closeButton.setBackgroundColor(Color.parseColor("#1AAB7F"))
        closeButton.setTextColor(Color.WHITE)
        closeButton.setPadding(20, 10, 20, 10)
        closeButton.textSize = 16f

        closeButton.setOnClickListener {
            dismiss()
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(
                (resources.displayMetrics.widthPixels * 0.85).toInt(),
                (resources.displayMetrics.heightPixels * 0.85).toInt()
            )
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    private fun getStyledTermsAndConditionsText(): SpannableString {
        val text = """
            TERMS AND CONDITIONS
            Last Updated: Mar 9, 2025
            Welcome to Biyahe! Cebu! By using our mobile application, you agree to the following terms and conditions. Please read them carefully.
            
            1. Acceptance of Terms
            By accessing or using Biyahe! Cebu, you acknowledge that you have read, understood, and agree to be bound by these Terms and Conditions. If you do not agree, please do not use the app.
            
            2. Purpose of the App
            Biyahe! Cebu is designed to help users navigate Cebu’s modern jeepney routes efficiently. The app provides route recommendations but does not guarantee real-time tracking, seat availability, or exact arrival times.
            
            3. User Accounts & Security
            Registration is optional but may be required for certain features.
            You are responsible for maintaining the security of your account and personal information.
            Any unauthorized use of your account should be reported to us immediately.
            
            4. Location Access & Permissions
            The app uses GPS data to provide route recommendations.
            You can disable location tracking in your device settings, but this may affect app functionality.
            We do not store your real-time location beyond what is necessary for the app’s operation.
            
            5. Limitations & Liability
            Biyahe! Cebu does not own or operate any jeepney services.
            We are not responsible for delays, route changes, lost items, or any inconvenience caused during your commute.
            The app provides suggestions based on available data, which may not always be up-to-date.
            
            6. Termination of Use
            We may suspend or terminate your access to Biyahe! Cebu if:
            - You violate these terms.
            - You misuse the app or attempt unauthorized access.
            - The app undergoes major updates or discontinuation.
            
            7. Changes to These Terms
            We may update these terms periodically. Continued use of the app after changes implies acceptance of the updated terms.
            
            For questions, contact us at BiyaheCebu@gmail.com.
        """.trimIndent()

        val spannable = SpannableString(text)
        val boldSections = listOf(
            "TERMS AND CONDITIONS",
            "1. Acceptance of Terms",
            "2. Purpose of the App",
            "3. User Accounts & Security",
            "4. Location Access & Permissions",
            "5. Limitations & Liability",
            "6. Termination of Use",
            "7. Changes to These Terms"
        )

        for (section in boldSections) {
            val start = text.indexOf(section)
            if (start >= 0) {
                spannable.setSpan(
                    StyleSpan(android.graphics.Typeface.BOLD),
                    start,
                    start + section.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        return spannable
    }
}