package com.example.biyahecebu

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.Firebase
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.firestore
import android.text.Editable
import android.text.TextWatcher
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.maps.android.PolyUtil

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var searchBar: AutoCompleteTextView
    private lateinit var bottomSheetDialog: BottomSheetDialog
    private lateinit var toggleLandmarksButton: Button
    private var jeepneyRoutes = mutableMapOf<String, List<Pair<String, LatLng>>>()
    private var currentPolyline: Polyline? = null
    private var startMarker: Marker? = null
    private var endMarker: Marker? = null
    private val landmarkMarkers = mutableListOf<Marker>()
    private var landmarksVisible = true
    private val apiKey = "AIzaSyAmXGB4IZZPkDGmepCsrfrU6N2I_zFqreU"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        searchBar = view.findViewById(R.id.searchBar)
        setupBottomSheet()
        fetchJeepneyCodes()

        searchBar.setOnItemClickListener { _, _, position, _ ->
            val selectedJeepCode = searchBar.adapter.getItem(position) as String
            showRoute(selectedJeepCode)
        }

        searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty()) {
                    clearRoute()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        enableUserLocation()

        googleMap.setOnMarkerClickListener { marker ->
            marker.showInfoWindow()
            true
        }
    }

    private fun fetchJeepneyCodes() {
        val db = Firebase.firestore
        db.collection("jeepney_routes")
            .get()
            .addOnSuccessListener { documents ->
                val jeepneyCodes = mutableListOf<String>()
                for (document in documents) {
                    val code = document.getString("code")
                    val coordinatesMap = document.get("coordinates") as? Map<*, *>

                    if (code != null && coordinatesMap != null) {
                        jeepneyCodes.add(code)
                        val coordinates = mutableListOf<Pair<String, LatLng>>()

                        for ((key, value) in coordinatesMap) {
                            val geoPoint = value as? GeoPoint
                            if (geoPoint != null) {
                                coordinates.add(Pair(key.toString(), LatLng(geoPoint.latitude, geoPoint.longitude)))
                            }
                        }

                        jeepneyRoutes[code] = coordinates
                    }
                }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, jeepneyCodes)
                searchBar.setAdapter(adapter)
            }
    }


    private fun enableUserLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
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
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                }
            }
            .addOnFailureListener { e ->
                Log.e("MapFragment", "Error fetching last known location: ", e)
            }
    }

    private fun showRoute(jeepCode: String) {
        clearRoute()

        val coordinates = jeepneyRoutes[jeepCode] ?: return
        if (coordinates.size < 2) return

        val start = coordinates.first().second
        val end = coordinates.last().second

        // Add start and end markers
        startMarker = googleMap.addMarker(MarkerOptions().position(start).title("Start"))
        endMarker = googleMap.addMarker(MarkerOptions().position(end).title("End"))

        moveCameraToRoute(coordinates)
        getDirections(start, end, coordinates)
        addLandmarkMarkers(coordinates)
        showBottomSheet(jeepCode)
    }

    private fun addLandmarkMarkers(coordinates: List<Pair<String, LatLng>>) {
        for ((name, latLng) in coordinates) {
            val marker = googleMap.addMarker(
                MarkerOptions().position(latLng).title(name)
            )
            if (marker != null) {
                landmarkMarkers.add(marker)
            }
        }
    }

    private fun toggleLandmarksVisibility() {
        landmarksVisible = !landmarksVisible
        for (marker in landmarkMarkers) {
            marker.isVisible = landmarksVisible
        }
    }

    private fun moveCameraToRoute(coordinates: List<Pair<String, LatLng>>) {
        val builder = LatLngBounds.Builder()
        for ((_, latLng) in coordinates) {
            builder.include(latLng)
        }
        val bounds = builder.build()
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
    }

    private fun getDirections(start: LatLng, end: LatLng, waypoints: List<Pair<String, LatLng>>) {
        val waypointString = waypoints.joinToString("|") { "${it.second.latitude},${it.second.longitude}" }
        val url = "https://maps.googleapis.com/maps/api/directions/json?origin=${start.latitude},${start.longitude}" +
                "&destination=${end.latitude},${end.longitude}" +
                "&waypoints=optimize:true|$waypointString" +
                "&mode=driving&key=$apiKey"

        val queue = Volley.newRequestQueue(requireContext())
        val request = JsonObjectRequest(Request.Method.GET, url, null, { response ->
            val routes = response.getJSONArray("routes")
            if (routes.length() > 0) {
                val route = routes.getJSONObject(0)
                val polyline = route.getJSONObject("overview_polyline").getString("points")
                drawPolyline(polyline)
            }
        }, { error ->
            Log.e("MapFragment", "Error fetching route: ", error)
        })

        queue.add(request)
    }

    private fun drawPolyline(encodedPolyline: String) {
        val decodedPath = PolyUtil.decode(encodedPolyline)
        currentPolyline = googleMap.addPolyline(
            PolylineOptions()
                .addAll(decodedPath)
                .color(Color.BLUE)
                .width(10f)
        )
    }

    private fun setupBottomSheet() {
        bottomSheetDialog = BottomSheetDialog(requireContext())
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_route_details, null)
        bottomSheetDialog.setContentView(bottomSheetView)

        val toggleLandmarksSwitch = bottomSheetView.findViewById<Switch>(R.id.toggleLandmarksSwitch)
        toggleLandmarksSwitch.setOnCheckedChangeListener { _, isChecked ->
            landmarksVisible = isChecked
            for (marker in landmarkMarkers) {
                marker.isVisible = landmarksVisible
            }
        }
    }


    private fun showBottomSheet(jeepCode: String) {
        val db = Firebase.firestore
        val landmarksListView = bottomSheetDialog.findViewById<ListView>(R.id.landmarksListView)

        if (landmarksListView == null) return

        db.collection("jeepney_routes").whereEqualTo("code", jeepCode)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val landmarks = document.get("landmarks") as? List<String> ?: emptyList()
                    val landmarkNames = ArrayList(landmarks)

                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, landmarkNames)
                    landmarksListView.adapter = adapter
                }
            }
            .addOnFailureListener { e ->
                Log.e("MapFragment", "Error fetching landmarks: ", e)
            }

        bottomSheetDialog.show()
    }


    private fun clearRoute() {
        currentPolyline?.remove()
        startMarker?.remove()
        endMarker?.remove()
        landmarkMarkers.forEach { it.remove() }
        landmarkMarkers.clear()
        bottomSheetDialog.dismiss()
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}