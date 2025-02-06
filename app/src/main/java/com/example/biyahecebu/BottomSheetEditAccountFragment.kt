package com.example.biyahecebu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BottomSheetEditAccountFragment : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for the fragment
        val view = inflater.inflate(R.layout.fragment_edit_account, container, false)

        // Find the bottom sheet view
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

        // Check if the bottom sheet is found
        if (bottomSheet != null) {
            val behavior = BottomSheetBehavior.from(bottomSheet)

            // Set the state to expanded so it opens fully
            behavior.state = BottomSheetBehavior.STATE_EXPANDED

            // Set peekHeight to 0 to make it full-screen
            behavior.peekHeight = 0

            // Make it dismissable by swipe
            behavior.isHideable = true

            // Force the bottom sheet to take full screen height
            val height = resources.displayMetrics.heightPixels
            bottomSheet.layoutParams.height = height
            bottomSheet.requestLayout()

            // Optionally add padding/margins if needed
        }

        return view
    }
}




