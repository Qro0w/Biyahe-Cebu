package com.example.biyahecebu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment

class OnboardingFragment : Fragment() {
    private var pageTitle: String? = null
    private var pageDescription: String? = null
    private var imageResource: Int = 0
    private var position: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            pageTitle = it.getString(ARG_TITLE)
            pageDescription = it.getString(ARG_DESCRIPTION)
            imageResource = it.getInt(ARG_IMAGE_RESOURCE)
            position = it.getInt(ARG_POSITION)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_onboarding, container, false)

        view.findViewById<TextView>(R.id.tv_title).text = pageTitle
        view.findViewById<TextView>(R.id.tv_description).text = pageDescription
        view.findViewById<ImageView>(R.id.iv_onboarding).setImageResource(imageResource)

        // Set up buttons based on position
        val btnSkip = view.findViewById<TextView>(R.id.btn_skip)
        val btnNext = view.findViewById<Button>(R.id.btn_next)
        val btnGetStarted = view.findViewById<Button>(R.id.btn_get_started)

        when (position) {
            0, 1 -> {
                btnSkip.visibility = View.VISIBLE
                btnNext.visibility = View.VISIBLE
                btnGetStarted.visibility = View.GONE

                btnNext.setOnClickListener {
                    (activity as? OnboardingActivity)?.navigateToPage(position + 1)
                }

                btnSkip.setOnClickListener {
                    (activity as? OnboardingActivity)?.completeOnboarding()
                }
            }
            2 -> {
                btnSkip.visibility = View.GONE
                btnNext.visibility = View.GONE
                btnGetStarted.visibility = View.VISIBLE

                btnGetStarted.setOnClickListener {
                    (activity as? OnboardingActivity)?.completeOnboarding()
                }
            }
        }

        return view
    }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_DESCRIPTION = "description"
        private const val ARG_IMAGE_RESOURCE = "image_resource"
        private const val ARG_POSITION = "position"

        fun newInstance(title: String, description: String, imageResource: Int, position: Int): OnboardingFragment {
            val fragment = OnboardingFragment()
            val args = Bundle()
            args.putString(ARG_TITLE, title)
            args.putString(ARG_DESCRIPTION, description)
            args.putInt(ARG_IMAGE_RESOURCE, imageResource)
            args.putInt(ARG_POSITION, position)
            fragment.arguments = args
            return fragment
        }
    }
}