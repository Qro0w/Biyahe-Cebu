import android.content.Context
import android.location.Location
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class PlacesAutoCompleteAdapter(
    context: Context,
    private val placesClient: PlacesClient
) : ArrayAdapter<AutocompletePrediction>(context, android.R.layout.simple_dropdown_item_1line) {

    private val resultList = mutableListOf<AutocompletePrediction>()
    private val layoutInflater = LayoutInflater.from(context)
    private var userLocation: Location? = null

    // Default search radius (in meters) if user location is available
    private val DEFAULT_SEARCH_RADIUS = 50000.0 // 10 km

    fun setUserLocation(location: Location) {
        userLocation = location
    }

    override fun getCount(): Int = resultList.size

    override fun getItem(position: Int): AutocompletePrediction = resultList[position]

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: layoutInflater.inflate(android.R.layout.simple_dropdown_item_1line, parent, false)
        val prediction = getItem(position)

        // Set the primary text as the main line in the dropdown
        val textView = view.findViewById<TextView>(android.R.id.text1)
        textView.text = "${prediction.getPrimaryText(null)}, ${prediction.getSecondaryText(null)}"

        return view
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()

                if (constraint.isNullOrEmpty()) {
                    return results
                }

                try {
                    // Create synchronization object
                    val latch = CountDownLatch(1)
                    val tempList = mutableListOf<AutocompletePrediction>()

                    // Build the request with location bias if available
                    val requestBuilder = FindAutocompletePredictionsRequest.builder()
                        .setQuery(constraint.toString())

                    // Add location bias if user location is available
                    userLocation?.let { location ->
                        // Create a bounding box around the user's location
                        val latLng = LatLng(location.latitude, location.longitude)
                        val bounds = createBoundsFromLocation(latLng, DEFAULT_SEARCH_RADIUS)

                        // Add the location bias to the request
                        requestBuilder.setLocationBias(bounds)

                        Log.d("PlacesAutoCompleteAdapter", "Adding location bias: ${latLng.latitude}, ${latLng.longitude}")
                    }

                    val request = requestBuilder.build()

                    // Fetch autocomplete predictions
                    placesClient.findAutocompletePredictions(request)
                        .addOnSuccessListener { response ->
                            Log.d("PlacesAutoCompleteAdapter", "Autocomplete predictions fetched: ${response.autocompletePredictions.size}")
                            tempList.addAll(response.autocompletePredictions)
                            latch.countDown() // Signal that the API call is complete
                        }
                        .addOnFailureListener { exception ->
                            Log.e("PlacesAutoCompleteAdapter", "Error fetching autocomplete predictions", exception)
                            latch.countDown() // Signal that the API call is complete even if it failed
                        }

                    // Wait for the API call to complete (with a timeout)
                    latch.await(5, TimeUnit.SECONDS)

                    results.values = tempList
                    results.count = tempList.size

                } catch (e: Exception) {
                    Log.e("PlacesAutoCompleteAdapter", "Error during filtering", e)
                }

                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                resultList.clear()

                if (results != null && results.count > 0 && results.values is List<*>) {
                    val typedList = results.values as List<*>
                    for (item in typedList) {
                        if (item is AutocompletePrediction) {
                            resultList.add(item)
                        }
                    }
                }

                notifyDataSetChanged()
            }

            override fun convertResultToString(resultValue: Any?): CharSequence {
                return if (resultValue is AutocompletePrediction) {
                    resultValue.getPrimaryText(null)
                } else {
                    super.convertResultToString(resultValue)
                }
            }
        }
    }


    // Helper method to create a bounding box around a location
    private fun createBoundsFromLocation(center: LatLng, radiusInMeters: Double): RectangularBounds {
        // Approximate 1 degree of latitude in meters
        val latDegreeInMeters = 111000.0

        // Calculate the offset in degrees for the given radius
        val latOffset = radiusInMeters / latDegreeInMeters

        // Calculate longitude offset based on latitude (longitude degrees get smaller as you move away from the equator)
        val lngOffset = radiusInMeters / (latDegreeInMeters * Math.cos(Math.toRadians(center.latitude)))

        // Create southwest and northeast corners of the bounding box
        val southwest = LatLng(
            center.latitude - latOffset,
            center.longitude - lngOffset
        )

        val northeast = LatLng(
            center.latitude + latOffset,
            center.longitude + lngOffset
        )

        return RectangularBounds.newInstance(southwest, northeast)
    }
}