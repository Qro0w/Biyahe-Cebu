package com.example.biyahecebu

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.Manifest
import android.graphics.Color
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.Firebase
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.firestore
import com.google.maps.android.PolyUtil

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize the map
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Set up search functionality
        val searchBar = view.findViewById<EditText>(R.id.searchBar)
        val searchButton = view.findViewById<Button>(R.id.searchButton)
        searchButton.setOnClickListener {
            val query = searchBar.text.toString()
            searchRoutes(query)
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Enable user location on the map
        enableUserLocation()

        // Add a marker for demonstration
        val cebuCity = LatLng(10.3157, 123.8854)
        googleMap.addMarker(MarkerOptions().position(cebuCity).title("Cebu City"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cebuCity, 12f))
    }

    private fun enableUserLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true
            getLastKnownLocation()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    googleMap.addMarker(MarkerOptions().position(userLatLng).title("Your Location"))
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                }
            }
    }



    private fun searchRoutes(query: String) {
        val db = Firebase.firestore
        val lowercaseQuery = query.lowercase()

        db.collection("jeepney_routes")
            .get() // Get all routes
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val routeCode = document.getString("code")?.lowercase()
                    val coordinatesMap = document.get("coordinates") as? Map<*, *>
                    val landmarks = document.get("landmarks") as? List<String>
                    val route = document.getString("route")

                    Log.d("MapFragment", "Firestore Data: $document")

                    if (coordinatesMap != null && landmarks != null && route != null) {
                        val coordinates = mutableListOf<LatLng>()

                        for (landmark in landmarks) {
                            val geoPoint = coordinatesMap[landmark] as? GeoPoint
                            if (geoPoint != null) {
                                val latLng = LatLng(geoPoint.latitude, geoPoint.longitude)
                                coordinates.add(latLng)
                                addMarker(latLng, landmark)  // Add marker dynamically
                            } else {
                                Log.e("MapFragment", "Coordinates for $landmark are missing!")
                            }
                        }

                        if (coordinates.size >= 2) {
                            drawRoute(coordinates)  // Draw route with dynamic landmarks
                        } else {
                            Log.e("MapFragment", "Not enough coordinates to draw a route")
                        }

                        Log.d("MapFragment", "Route: $route")
                        Log.d("MapFragment", "Landmarks: $landmarks")
                    } else {
                        Log.e("MapFragment", "Firestore data missing required fields")
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("MapFragment", "Error getting documents: ", exception)
            }
    }




    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
    private fun drawRoute(coordinates: List<LatLng>) {
        if (!::googleMap.isInitialized || coordinates.size < 2) return

        val apiKey = "AIzaSyAmXGB4IZZPkDGmepCsrfrU6N2I_zFqreU" // Replace with your API key
        val origin = "${coordinates.first().latitude},${coordinates.first().longitude}"
        val destination = "${coordinates.last().latitude},${coordinates.last().longitude}"

        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=$origin&destination=$destination&mode=driving&key=$apiKey"

        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                val routes = response.getJSONArray("routes")
                if (routes.length() > 0) {
                    val points = mutableListOf<LatLng>()
                    val steps = routes.getJSONObject(0)
                        .getJSONArray("legs")
                        .getJSONObject(0)
                        .getJSONArray("steps")

                    for (i in 0 until steps.length()) {
                        val polyline = steps.getJSONObject(i)
                            .getJSONObject("polyline")
                            .getString("points")

                        points.addAll(PolyUtil.decode(polyline))
                    }

                    googleMap.addPolyline(
                        PolylineOptions().addAll(points).color(Color.RED).width(10f)
                    )
                }
            },
            { error -> Log.e("MapFragment", "Error fetching route: ${error.message}") }
        )

        Volley.newRequestQueue(requireContext()).add(request)
    }


    private fun addMarker(location: LatLng, title: String) {
        if (!::googleMap.isInitialized) return
        googleMap.addMarker(
            MarkerOptions()
                .position(location)
                .title(title)
        )
    }

}
