package com.example.biyahecebu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.SearchView // Use this import instead of android.widget.SearchView
import com.example.biyahecebu.models.JeepneyRoute

class JeepneyRoutesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var adapter: JeepneyRoutesAdapter
    private var jeepneyRoutes = mutableListOf(
        JeepneyRoute("01A", "Ayala - SM via Colon", false),
        JeepneyRoute("02B", "Talamban - Colon", false),
        JeepneyRoute("10M", "Mandaue - SM - Colon", false)
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_jeepney_routes, container, false)

        searchView = view.findViewById(R.id.searchView) as SearchView // ✅ No more casting issues
        searchView.setOnClickListener {
            searchView.isIconified = false  // Make sure it's expanded
            searchView.requestFocus()      // Focus the search view
        }
        recyclerView = view.findViewById(R.id.recyclerView)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = JeepneyRoutesAdapter(jeepneyRoutes)
        recyclerView.adapter = adapter

        setupSearchView()

        return view
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val filteredList = jeepneyRoutes.filter {
                    it.jeepneyCode.contains(newText ?: "", ignoreCase = true) ||
                            it.route.contains(newText ?: "", ignoreCase = true)
                }
                adapter = JeepneyRoutesAdapter(filteredList)
                recyclerView.adapter = adapter
                return true
            }
        })
    }
}
