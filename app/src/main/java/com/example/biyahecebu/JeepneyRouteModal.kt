package com.example.biyahecebu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.biyahecebu.models.JeepneyRoute

class JeepneyRouteModal : DialogFragment() {

    companion object {
        fun newInstance(route: JeepneyRoute): JeepneyRouteModal {
            val fragment = JeepneyRouteModal()
            val args = Bundle()
            args.putString("jeepneyCode", route.jeepneyCode)
            args.putString("route", route.route)
            args.putStringArrayList("landmarks", ArrayList(route.landmarks))
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog_MinWidth)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.modal_jeepney_route, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Extract route details from arguments
        val jeepneyCode = arguments?.getString("jeepneyCode") ?: ""
        val routeDescription = arguments?.getString("route") ?: ""
        val landmarks = arguments?.getStringArrayList("landmarks") ?: arrayListOf()

        // Set up UI elements
        val jeepneyCodeText = view.findViewById<TextView>(R.id.jeepneyCodeText)
        val routeDescriptionText = view.findViewById<TextView>(R.id.routeDescriptionText)
        val landmarksContainer = view.findViewById<LinearLayout>(R.id.landmarksContainer)
        val backButton = view.findViewById<ImageButton>(R.id.backButton)

        // Set back button click listener
        backButton.setOnClickListener {
            dismiss()
        }

        jeepneyCodeText.text = jeepneyCode
        routeDescriptionText.text = routeDescription

        // Clear any existing landmarks
        landmarksContainer.removeAllViews()

        // Simply display each landmark as a text item
        for (landmark in landmarks) {
            val textView = TextView(context)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(16, 8, 16, 8)
            textView.layoutParams = params
            textView.text = landmark
            textView.textSize = 16f
            textView.setPadding(16, 12, 16, 12)

            landmarksContainer.addView(textView)
        }
    }
}