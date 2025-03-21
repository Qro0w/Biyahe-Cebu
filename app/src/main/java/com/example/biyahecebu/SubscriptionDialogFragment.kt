package com.example.biyahecebu

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SubscriptionDialogFragment(private val onPlanSelected: (String) -> Unit) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_subscription, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get dialog components
        val radioGroup = view.findViewById<RadioGroup>(R.id.planRadioGroup)
        val subscribeButton = view.findViewById<Button>(R.id.confirmSubscribeButton)
        val cancelButton = view.findViewById<Button>(R.id.cancelSubscribeButton)

        // Set up the subscribe button
        subscribeButton.setOnClickListener {
            val selectedId = radioGroup.checkedRadioButtonId
            val planId = when(selectedId) {
                R.id.monthlyPlanRadio -> "monthly"
                R.id.yearlyPlanRadio -> "yearly"
                else -> "monthly" // Default to monthly if somehow nothing is selected
            }

            // Invoke the callback with the selected plan ID
            onPlanSelected(planId)

            // Dismiss the dialog
            dismiss()
        }

        // Set up the cancel button
        cancelButton.setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}

class ManageSubscriptionDialogFragment(
    private val subscriptionEndDate: Date?,
    private val onCancelSubscription: () -> Unit,
    private val onRenewSubscription: () -> Unit
) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_manage_subscription, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get dialog components
        val statusTextView = view.findViewById<TextView>(R.id.subscriptionStatusText)
        val cancelButton = view.findViewById<Button>(R.id.cancelSubscriptionButton)
        val renewButton = view.findViewById<Button>(R.id.renewSubscriptionButton)
        val closeButton = view.findViewById<Button>(R.id.closeManageButton)

        // Set subscription status text
        if (subscriptionEndDate != null) {
            val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
            val formattedDate = dateFormat.format(subscriptionEndDate)
            statusTextView.text = "Your subscription is active until $formattedDate"
        } else {
            statusTextView.text = "Subscription status unknown"
        }

        // Set up button listeners
        cancelButton.setOnClickListener {
            onCancelSubscription()
        }

        renewButton.setOnClickListener {
            onRenewSubscription()
        }

        closeButton.setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}