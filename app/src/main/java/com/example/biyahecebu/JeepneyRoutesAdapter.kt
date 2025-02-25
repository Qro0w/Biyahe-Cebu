package com.example.biyahecebu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.biyahecebu.models.JeepneyRoute
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class JeepneyRoutesAdapter(
    private var jeepneyList: List<JeepneyRoute>,
    private val onItemClick: (JeepneyRoute) -> Unit
) : RecyclerView.Adapter<JeepneyRoutesAdapter.ViewHolder>() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val userId = auth.currentUser?.uid

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val jeepIcon: ImageView = view.findViewById(R.id.jeepIcon)
        val jeepCode: TextView = view.findViewById(R.id.jeepCode)
        val routeDescription: TextView = view.findViewById(R.id.routeDescription)
        val favoriteIcon: ImageView = view.findViewById(R.id.favoriteIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_jeepney_route, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val route = jeepneyList[position]
        holder.jeepIcon.setImageResource(R.drawable.ic_jeepney)
        holder.jeepCode.text = route.jeepneyCode
        holder.routeDescription.text = route.route

        // Check if this route is favorited by the user
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    val favorites = document.get("favorites") as? List<String> ?: emptyList()
                    route.isFavorite = favorites.contains(route.jeepneyCode)
                    holder.favoriteIcon.setImageResource(
                        if (route.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline
                    )
                }
        }

        // Handle favorite toggle
        holder.favoriteIcon.setOnClickListener {
            if (userId != null) {
                toggleFavorite(route, holder)
            }
        }

        // Handle route click
        holder.itemView.setOnClickListener { onItemClick(route) }
    }

    private fun toggleFavorite(route: JeepneyRoute, holder: ViewHolder) {
        val userDoc = db.collection("users").document(userId!!)

        userDoc.get().addOnSuccessListener { document ->
            val favorites = document.get("favorites") as? MutableList<String> ?: mutableListOf()

            if (route.isFavorite) {
                favorites.remove(route.jeepneyCode)
            } else {
                favorites.add(route.jeepneyCode)
            }

            userDoc.update("favorites", favorites)
                .addOnSuccessListener {
                    route.isFavorite = !route.isFavorite
                    holder.favoriteIcon.setImageResource(
                        if (route.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline
                    )
                }
        }
    }

    override fun getItemCount(): Int = jeepneyList.size
}
