package com.example.biyahecebu

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val searchBar = view.findViewById<View>(R.id.searchBar)
        val howToBtn = view.findViewById<View>(R.id.howtobtn)
        val termsBtn = view.findViewById<View>(R.id.termsandconbtn) // Button to show Terms and Conditions

        searchBar.setOnClickListener {
            requireActivity().supportFragmentManager.commit {
                replace(R.id.nav_host_fragment, MapFragment())
                addToBackStack(null)
            }
            val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNav)
            bottomNav.selectedItemId = R.id.map
        }

        howToBtn.setOnClickListener {
            showHowToPopup()
        }

        termsBtn.setOnClickListener {
            showTermsConditionsDialog()
        }
    }

    private fun showHowToPopup() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.popup_howto)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
    }

    private fun showTermsConditionsDialog() {
        val dialog = TermsConditionsDialog()
        dialog.show(childFragmentManager, "TermsConditionsDialog")
    }
}
