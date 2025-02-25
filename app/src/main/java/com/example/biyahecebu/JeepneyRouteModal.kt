package com.example.biyahecebu

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.biyahecebu.models.JeepneyRoute

class JeepneyRouteModal(private val route: JeepneyRoute) : DialogFragment() {

    companion object {
        fun newInstance(route: JeepneyRoute): JeepneyRouteModal {
            return JeepneyRouteModal(route)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.modal_jeepney_route, null)
        dialog.setContentView(view)

        val jeepneyCodeText = view.findViewById<TextView>(R.id.jeepneyCodeText)
        val routeDescriptionText = view.findViewById<TextView>(R.id.routeDescriptionText)
        val landmarksText = view.findViewById<TextView>(R.id.landmarksText)

        // Centered display for code and route
        jeepneyCodeText.text = route.jeepneyCode
        routeDescriptionText.text = route.route

        // Format landmarks in a list format
        landmarksText.text = route.landmarks.joinToString("\n")

        return dialog
    }
}
