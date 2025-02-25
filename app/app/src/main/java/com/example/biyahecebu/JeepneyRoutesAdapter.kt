package com.example.biyahecebu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.biyahecebu.models.JeepneyRoute

class JeepneyRoutesAdapter(private var jeepneyList: List<JeepneyRoute>) :
    RecyclerView.Adapter<JeepneyRoutesAdapter.ViewHolder>() {

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

        holder.favoriteIcon.setImageResource(
            if (route.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline
        )

        holder.favoriteIcon.setOnClickListener {
            route.isFavorite = !route.isFavorite
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = jeepneyList.size
}
