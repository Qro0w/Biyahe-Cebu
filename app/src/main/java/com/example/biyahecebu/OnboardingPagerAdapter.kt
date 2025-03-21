package com.example.biyahecebu


import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class OnboardingPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    private val fragments = ArrayList<OnboardingFragment>()

    init {
        // Add pages
        fragments.add(
            OnboardingFragment.newInstance(
                "Navigate Cebu with ease",
                "Browse a complete list of jeepney routes and stops. Find the right ride for your destination in just a few taps.",
                R.drawable.img_onboarding_1,
                0
            )
        )

        fragments.add(
            OnboardingFragment.newInstance(
                "Visualize your commute",
                "Explore an interactive map of Cebu's jeepney routes. Plan your trip efficiently and avoid unnecessary detours.",
                R.drawable.img_onboarding_2,
                1
            )
        )

        fragments.add(
            OnboardingFragment.newInstance(
                "Your Guide to Public Transport",
                "A hassle-free way to commute. Access route details anytime, anywhere—no need to memorize numbers or ask around. Travel smarter with Biyahe! Cebu",
                R.drawable.img_onboarding_3,
                2
            )
        )
    }

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }
}