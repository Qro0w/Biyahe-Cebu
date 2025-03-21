package com.example.biyahecebu

import PlacesAutoCompleteAdapter
import android.Manifest
import android.app.AlertDialog
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
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Firebase
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.firestore
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.button.MaterialButton
import com.google.maps.android.PolyUtil
import com.google.maps.android.SphericalUtil

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var startSearchBar: AutoCompleteTextView
    private lateinit var destinationSearchBar: AutoCompleteTextView
    private lateinit var searchCard: MaterialCardView
    private lateinit var bottomSheetDialog: BottomSheetDialog
    private lateinit var startAdapter: PlacesAutoCompleteAdapter
    private lateinit var destinationAdapter: PlacesAutoCompleteAdapter
    private lateinit var placesClient: PlacesClient
    private lateinit var routePlanner: RoutePlanner
    private lateinit var myLocationButton: FloatingActionButton
    private lateinit var findRouteButton: MaterialButton
    private lateinit var clearRouteButton: ImageButton
    private lateinit var showRouteDetailsButton: FloatingActionButton

    private var jeepneyRoutes = mutableMapOf<String, List<Pair<String, LatLng>>>()
    private var currentPolyline: Polyline? = null
    private var currentRoute: CompleteRoute? = null
    private var startMarker: Marker? = null
    private var endMarker: Marker? = null
    private val landmarkMarkers = mutableListOf<Marker>()
    private var landmarksVisible = true
    private val apiKey = "AIzaSyAmXGB4IZZPkDGmepCsrfrU6N2I_zFqreU"

    private var startLocation: LatLng? = null
    private var destinationLocation: LatLng? = null
    private var startLocationName: String = "My Location"
    private var destinationLocationName: String = ""
    private var useCurrentLocation = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map_improved, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), "AIzaSyAmXGB4IZZPkDGmepCsrfrU6N2I_zFqreU")
        }
        super.onViewCreated(view, savedInstanceState)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Initialize UI components
        startSearchBar = view.findViewById(R.id.startLocationSearch)
        destinationSearchBar = view.findViewById(R.id.destinationLocationSearch)
        searchCard = view.findViewById(R.id.searchCard)
        myLocationButton = view.findViewById(R.id.myLocationButton)
        findRouteButton = view.findViewById(R.id.findRouteButton)
        clearRouteButton = view.findViewById(R.id.clearRouteButton)
        showRouteDetailsButton = view.findViewById(R.id.showRouteDetailsButton)
        showRouteDetailsButton.visibility = View.GONE  // Initially hidden
        showRouteDetailsButton.setOnClickListener {
            currentRoute?.let { route ->
                showMultiSegmentBottomSheet(route)
            }
        }

        setupSearchBars()
        setupButtons()
        setupBottomSheet()
        fetchJeepneyCodes()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        enableUserLocation()

        googleMap.setOnMarkerClickListener { marker ->
            marker.showInfoWindow()
            true
        }

        // Add map click listener to allow manual location selection
        googleMap.setOnMapLongClickListener { latLng ->
            showLocationSelectionDialog(latLng)
        }
    }

    private fun showLocationSelectionDialog(latLng: LatLng) {
        val options = arrayOf("Set as Start Location", "Set as Destination")

        AlertDialog.Builder(requireContext())
            .setTitle("Select Option")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // Set as start location
                        startLocation = latLng
                        useCurrentLocation = false
                        startLocationName = "Selected Location"
                        startSearchBar.setText(startLocationName)

                        // Update or add marker
                        startMarker?.remove()
                        startMarker = googleMap.addMarker(
                            MarkerOptions()
                                .position(latLng)
                                .title("Start Location")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                        )
                    }
                    1 -> {
                        // Set as destination
                        destinationLocation = latLng
                        destinationLocationName = "Selected Location"
                        destinationSearchBar.setText(destinationLocationName)

                        // Update or add marker
                        endMarker?.remove()
                        endMarker = googleMap.addMarker(
                            MarkerOptions()
                                .position(latLng)
                                .title("Destination")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                        )
                    }
                }
            }
            .show()
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
                // Initialize the route planner after loading all routes
                initializeRoutePlanner()
            }
    }

    private fun initializeRoutePlanner() {
        routePlanner = RoutePlanner(jeepneyRoutes)
    }

    private fun setupSearchBars() {
        placesClient = Places.createClient(requireContext())

        // Setup adapters
        startAdapter = PlacesAutoCompleteAdapter(requireContext(), placesClient)
        destinationAdapter = PlacesAutoCompleteAdapter(requireContext(), placesClient)

        // Set the initial user location if available
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    startAdapter.setUserLocation(location)
                    destinationAdapter.setUserLocation(location)
                    startLocation = LatLng(location.latitude, location.longitude)
                }
            }
        }

        // Set up start location search
        startSearchBar.threshold = 2
        startSearchBar.setAdapter(startAdapter)

        startSearchBar.setOnItemClickListener { _, _, position, _ ->
            val selectedPlace = startAdapter.getItem(position)
            val placeId = selectedPlace.placeId

            val placeRequest = FetchPlaceRequest.newInstance(placeId, listOf(Place.Field.LAT_LNG, Place.Field.NAME))

            placesClient.fetchPlace(placeRequest)
                .addOnSuccessListener { response ->
                    val place = response.place
                    val location = place.latLng

                    if (location != null) {
                        startLocation = location
                        startLocationName = place.name ?: selectedPlace.getPrimaryText(null).toString()
                        useCurrentLocation = false

                        // Update or add marker
                        startMarker?.remove()
                        startMarker = googleMap.addMarker(
                            MarkerOptions()
                                .position(location)
                                .title("Start: $startLocationName")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                        )

                        // Move camera to the location
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
                    }
                }
        }

        // Handle user typing "Current Location"
        startSearchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s.toString().equals("Current Location", ignoreCase = true)) {
                    useCurrentLocation = true
                    if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                            if (location != null) {
                                startLocation = LatLng(location.latitude, location.longitude)
                                startLocationName = "Current Location"
                            }
                        }
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s != null && s.length >= 2) {
                    startSearchBar.showDropDown()
                }
            }
        })

        // Set up destination search
        destinationSearchBar.threshold = 2
        destinationSearchBar.setAdapter(destinationAdapter)

        destinationSearchBar.setOnItemClickListener { _, _, position, _ ->
            val selectedPlace = destinationAdapter.getItem(position)
            val placeId = selectedPlace.placeId

            val placeRequest = FetchPlaceRequest.newInstance(placeId, listOf(Place.Field.LAT_LNG, Place.Field.NAME))

            placesClient.fetchPlace(placeRequest)
                .addOnSuccessListener { response ->
                    val place = response.place
                    val location = place.latLng

                    if (location != null) {
                        destinationLocation = location
                        destinationLocationName = place.name ?: selectedPlace.getPrimaryText(null).toString()

                        // Update or add marker
                        endMarker?.remove()
                        endMarker = googleMap.addMarker(
                            MarkerOptions()
                                .position(location)
                                .title("Destination: $destinationLocationName")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                        )

                        // Move camera to the location
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
                    }
                }
        }

        destinationSearchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s != null && s.length >= 2) {
                    destinationSearchBar.showDropDown()
                }
            }
        })

        // Handle keyboard done button for destination search
        destinationSearchBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                findRouteIfReady()
                return@setOnEditorActionListener true
            }
            false
        }
    }

    private fun setupButtons() {
        myLocationButton.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val userLatLng = LatLng(location.latitude, location.longitude)
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                    }
                }
            } else {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        findRouteButton.setOnClickListener {
            findRouteIfReady()
        }

        clearRouteButton.setOnClickListener {
            clearRoute()
            startSearchBar.setText("")
            destinationSearchBar.setText("")
            startLocationName = "My Location"
            destinationLocationName = ""
            useCurrentLocation = true
            startLocation = null
            destinationLocation = null
        }
    }

    private fun findRouteIfReady() {
        // Make sure we have destination
        if (destinationLocation == null) {
            Toast.makeText(requireContext(), "Please select a destination", Toast.LENGTH_SHORT).show()
            return
        }

        // Get current location if using current location
        if (useCurrentLocation) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        startLocation = LatLng(location.latitude, location.longitude)
                        findRouteWithLocations()
                    } else {
                        Toast.makeText(requireContext(), "Unable to get current location", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        } else {
            // Using manually selected start location
            if (startLocation != null) {
                findRouteWithLocations()
            } else {
                Toast.makeText(requireContext(), "Please select a start location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun findRouteWithLocations() {
        // Now we have both start and destination, find routes
        val start = startLocation ?: return
        val destination = destinationLocation ?: return

        // Use the route planner to find possible routes
        val possibleRoutes = routePlanner.findRoutes(start, destination)

        if (possibleRoutes.isNotEmpty()) {
            // Show route selection dialog
            showRouteOptionsDialog(possibleRoutes, destination)
        } else {
            Toast.makeText(requireContext(), "No routes found to your destination", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRouteOptionsDialog(routes: List<CompleteRoute>, destination: LatLng) {
        val dialog = AlertDialog.Builder(requireContext())
        dialog.setTitle("Select a Route")

        val routeDescriptions = routes.mapIndexed { index, route ->
            val transferText = when (route.transfers) {
                0 -> "Direct route"
                1 -> "1 transfer"
                else -> "${route.transfers} transfers"
            }
            val distanceKm = String.format("%.1f", route.totalDistance / 1000)
            "Option ${index + 1}: ${route.segments.map { it.jeepCode }.joinToString(" → ")} ($transferText, $distanceKm km)"
        }.toTypedArray()

        dialog.setItems(routeDescriptions) { _, which ->
            displaySelectedRoute(routes[which], destination)
        }

        dialog.show()
    }

    private fun displaySelectedRoute(route: CompleteRoute, destination: LatLng) {
        clearRoute()

        // Store current route
        currentRoute = route

        // Show the route details button
        showRouteDetailsButton.visibility = View.VISIBLE

        // Add markers for destination
        val destinationMarker = googleMap.addMarker(
            MarkerOptions()
                .position(destination)
                .title("Destination: $destinationLocationName")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        )
        endMarker = destinationMarker

        // Add start marker if not using current location
        if (!useCurrentLocation && startLocation != null) {
            startMarker = googleMap.addMarker(
                MarkerOptions()
                    .position(startLocation!!)
                    .title("Start: $startLocationName")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            )
        }

        // Different colors for different route segments
        val segmentColors = listOf(Color.BLUE, Color.GREEN, Color.MAGENTA)

        // Add polylines and markers for each route segment
        route.segments.forEachIndexed { index, segment ->
            val routeCoordinates = jeepneyRoutes[segment.jeepCode] ?: return@forEachIndexed

            // Find the indexes of the start and end points
            val startIndex = routeCoordinates.indexOfFirst { it.first == segment.startPoint.first }
            val endIndex = routeCoordinates.indexOfFirst { it.first == segment.endPoint.first }

            if (startIndex != -1 && endIndex != -1) {
                val routeSegment = if (startIndex <= endIndex) {
                    routeCoordinates.subList(startIndex, endIndex + 1)
                } else {
                    routeCoordinates.subList(endIndex, startIndex + 1).reversed()
                }

                // Instead of drawing direct lines, get road-following directions
                // between consecutive points
                if (routeSegment.size >= 2) {
                    for (i in 0 until routeSegment.size - 1) {
                        getRoadDirections(
                            routeSegment[i].second,
                            routeSegment[i + 1].second,
                            segmentColors[index % segmentColors.size]
                        )
                    }
                }

                // Add markers for start and end of segment
                val startMarkerOptions = MarkerOptions()
                    .position(segment.startPoint.second)
                    .title("Board ${segment.jeepCode}")
                    .snippet(segment.startPoint.first)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))

                googleMap.addMarker(startMarkerOptions)

                // Only add end marker if it's the last segment or a transfer point
                if (index == route.segments.size - 1) {
                    val endMarkerOptions = MarkerOptions()
                        .position(segment.endPoint.second)
                        .title("Alight from ${segment.jeepCode}")
                        .snippet(segment.endPoint.first)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))

                    googleMap.addMarker(endMarkerOptions)
                } else {
                    // This is a transfer point
                    val transferMarkerOptions = MarkerOptions()
                        .position(segment.endPoint.second)
                        .title("Transfer: ${segment.jeepCode} → ${route.segments[index + 1].jeepCode}")
                        .snippet(segment.endPoint.first)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))

                    googleMap.addMarker(transferMarkerOptions)
                }

                // Add landmark markers for this route segment
                addLandmarkMarkers(routeSegment)
            }
        }

        // Move camera to show the entire route
        val builder = LatLngBounds.Builder()

        // Add all points from all segments
        route.segments.forEach { segment ->
            builder.include(segment.startPoint.second)
            builder.include(segment.endPoint.second)
        }

        // Include destination
        builder.include(destination)

        // Include start location
        startLocation?.let {
            builder.include(it)
        }

        // Move camera to show the entire route
        try {
            val bounds = builder.build()
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
        } catch (e: Exception) {
            Log.e("MapFragment", "Error moving camera: ", e)
        }

        // Show bottom sheet with route information
        showMultiSegmentBottomSheet(route)
    }

    private fun getRoadDirections(origin: LatLng, destination: LatLng, color: Int) {
        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${origin.latitude},${origin.longitude}" +
                "&destination=${destination.latitude},${destination.longitude}" +
                "&mode=driving" +
                "&key=$apiKey"

        val queue = Volley.newRequestQueue(requireContext())
        val request = JsonObjectRequest(Request.Method.GET, url, null, { response ->
            val routes = response.getJSONArray("routes")
            if (routes.length() > 0) {
                val route = routes.getJSONObject(0)
                val polyline = route.getJSONObject("overview_polyline").getString("points")
                val decodedPath = PolyUtil.decode(polyline)

                // Draw this segment with the specified color
                googleMap.addPolyline(
                    PolylineOptions()
                        .addAll(decodedPath)
                        .color(color)
                        .width(10f)
                )
            }
        }, { error ->
            Log.e("MapFragment", "Error fetching road path: ", error)
        })

        queue.add(request)
    }

    // Show bottom sheet with detailed information about the multi-segment route
    private fun showMultiSegmentBottomSheet(route: CompleteRoute) {
        bottomSheetDialog = BottomSheetDialog(requireContext())
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_multi_route_details, null)
        bottomSheetDialog.setContentView(bottomSheetView)

        bottomSheetDialog.setOnDismissListener {
            // The button remains visible after dismissal
        }

        // Set up route summary
        val routeSummaryText = bottomSheetView.findViewById<TextView>(R.id.routeSummaryText)
        val totalDistance = String.format("%.1f", route.totalDistance / 1000)
        val transfers = if (route.transfers == 0) "No transfers" else "${route.transfers} transfer(s)"
        routeSummaryText.text = "Total distance: $totalDistance km\n$transfers"

        // Set up route segments list
        val segmentsContainer = bottomSheetView.findViewById<LinearLayout>(R.id.segmentsContainer)

        route.segments.forEachIndexed { index, segment ->
            // Create a view for each segment
            val segmentView = layoutInflater.inflate(R.layout.item_route_segment, segmentsContainer, false)

            // Set up segment details
            val jeepCodeText = segmentView.findViewById<TextView>(R.id.jeepCodeText)
            val segmentDetailsText = segmentView.findViewById<TextView>(R.id.segmentDetailsText)

            jeepCodeText.text = segment.jeepCode
            val segmentDistance = String.format("%.1f", segment.distance / 1000)
            segmentDetailsText.text = "From: ${segment.startPoint.first}\nTo: ${segment.endPoint.first}\nDistance: $segmentDistance km"

            // Add a transfer instruction if this isn't the last segment
            if (index < route.segments.size - 1) {
                val transferView = layoutInflater.inflate(R.layout.item_transfer_instruction, segmentsContainer, false)
                val transferText = transferView.findViewById<TextView>(R.id.transferInstructionText)
                transferText.text = "Transfer to ${route.segments[index + 1].jeepCode} at ${segment.endPoint.first}"

                segmentsContainer.addView(segmentView)
                segmentsContainer.addView(transferView)
            } else {
                segmentsContainer.addView(segmentView)
            }
        }

        // Set up the toggle landmarks switch
        val toggleLandmarksSwitch = bottomSheetView.findViewById<Switch>(R.id.toggleLandmarksSwitch)
        toggleLandmarksSwitch.isChecked = landmarksVisible
        toggleLandmarksSwitch.setOnCheckedChangeListener { _, isChecked ->
            landmarksVisible = isChecked
            for (marker in landmarkMarkers) {
                marker.isVisible = landmarksVisible
            }
        }

        bottomSheetDialog.show()
    }

    private fun addLandmarkMarkers(coordinates: List<Pair<String, LatLng>>) {
        for ((name, latLng) in coordinates) {
            val marker = googleMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(name)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            )
            if (marker != null) {
                marker.isVisible = landmarksVisible
                landmarkMarkers.add(marker)
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                enableUserLocation()
            } else {
                // Handle permission denied
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
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
            // Request permission using ActivityResultLauncher
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
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
                    startLocation = userLatLng

                    // Update adapters with the user's location
                    startAdapter.setUserLocation(location)
                    destinationAdapter.setUserLocation(location)
                }
            }
            .addOnFailureListener { e ->
                Log.e("MapFragment", "Error fetching last known location: ", e)
            }
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

    private fun clearRoute() {
        currentPolyline?.remove()
        startMarker?.remove()
        endMarker?.remove()
        landmarkMarkers.forEach { it.remove() }
        landmarkMarkers.clear()

        currentRoute = null
        if (::showRouteDetailsButton.isInitialized) {
            showRouteDetailsButton.visibility = View.GONE
        }

        if (::bottomSheetDialog.isInitialized) {
            bottomSheetDialog.dismiss()
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}