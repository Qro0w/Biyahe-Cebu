package com.example.biyahecebu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.biyahecebu.models.JeepneyRoute
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class JeepneyRoutesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var favoriteFilterIcon: ImageView
    private lateinit var adapter: JeepneyRoutesAdapter
    private val jeepneyRoutes = mutableListOf<JeepneyRoute>()
    private var showFavoritesOnly = false
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val userId = auth.currentUser?.uid

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_jeepney_routes, container, false)

        searchView = view.findViewById(R.id.searchView)
        favoriteFilterIcon = view.findViewById(R.id.favoriteFilterIcon)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = JeepneyRoutesAdapter(jeepneyRoutes, ::showRouteDetails)
        recyclerView.adapter = adapter

        fetchJeepneyRoutes()
        setupSearchView()
        setupFavoriteFilter()

        return view
    }

    private fun fetchJeepneyRoutes() {
        db.collection("jeepney_routes").get()
            .addOnSuccessListener { documents ->
                jeepneyRoutes.clear()
                for (document in documents) {
                    val jeepneyCode = document.id
                    val route = document.getString("routes") ?: "Unknown Route"
                    val landmarks = (document.get("landmarks") as? List<*>)?.mapNotNull { it as? String } ?: listOf()

                    val jeepneyRoute = JeepneyRoute(jeepneyCode, route, landmarks, false)
                    jeepneyRoutes.add(jeepneyRoute)
                }

                if (userId != null) {
                    loadUserFavorites()
                } else {
                    filterRoutes()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadUserFavorites() {
        db.collection("users").document(userId!!).get()
            .addOnSuccessListener { document ->
                val favorites = document.get("favorites") as? List<String> ?: emptyList()
                for (route in jeepneyRoutes) {
                    route.isFavorite = favorites.contains(route.jeepneyCode)
                }
                filterRoutes()
            }
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterRoutes(newText)
                return true
            }
        })
    }

    private fun setupFavoriteFilter() {
        favoriteFilterIcon.setOnClickListener {
            showFavoritesOnly = !showFavoritesOnly
            favoriteFilterIcon.setImageResource(
                if (showFavoritesOnly) R.drawable.ic_star_filled else R.drawable.ic_star_outline
            )
            filterRoutes()
        }
    }

    private fun filterRoutes(query: String? = null) {
        val filteredList = jeepneyRoutes.filter {
            (!showFavoritesOnly || it.isFavorite) &&
                    (query.isNullOrEmpty() ||
                            it.jeepneyCode.contains(query, ignoreCase = true) ||
                            it.route.contains(query, ignoreCase = true))
        }
        adapter = JeepneyRoutesAdapter(filteredList, ::showRouteDetails)
        recyclerView.adapter = adapter
    }

    private fun showRouteDetails(route: JeepneyRoute) {
        val modal = JeepneyRouteModal.newInstance(route)
        modal.show(parentFragmentManager, "JeepneyRouteModal")
    }
}
