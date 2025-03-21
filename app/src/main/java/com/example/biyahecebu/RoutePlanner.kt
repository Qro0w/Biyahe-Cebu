package com.example.biyahecebu

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import kotlin.math.min

data class JeepneyRoute(
    val code: String,
    val coordinates: List<Pair<String, LatLng>>
)

data class RouteSegment(
    val jeepCode: String,
    val startPoint: Pair<String, LatLng>,
    val endPoint: Pair<String, LatLng>,
    val distance: Double
)

data class CompleteRoute(
    val segments: List<RouteSegment>,
    val totalDistance: Double,
    val transfers: Int
)

class RoutePlanner(private val jeepneyRoutes: Map<String, List<Pair<String, LatLng>>>) {

    // Maximum walking distance to consider for transfers (in meters)
    private val MAX_WALKING_DISTANCE = 1000.0

    // Maximum number of transfers to consider
    private val MAX_TRANSFERS = 2

    /**
     * Find possible routes from start to destination with up to MAX_TRANSFERS transfers
     */
    fun findRoutes(start: LatLng, destination: LatLng): List<CompleteRoute> {

        // Debug: Find any routes near the destination, ignoring start position
        for ((code, coordinates) in jeepneyRoutes) {
            val (_, _, endDistance) = findClosestPointOnRoute(coordinates, destination)
            Log.d("RoutePlanner", "Distance from destination to route $code: $endDistance meters")
        }

        val possibleRoutes = mutableListOf<CompleteRoute>()

        // Try direct routes first (no transfers)
        val directRoutes = findDirectRoutes(start, destination)
        possibleRoutes.addAll(directRoutes)

        // If no direct routes or we want more options, try routes with transfers
        if (possibleRoutes.isEmpty() || possibleRoutes.size < 3) {
            val routesWithTransfers = findRoutesWithTransfers(start, destination)

            // Add routes with transfers if they're not substantially longer than the best direct route
            val bestDirectDistance = directRoutes.minByOrNull { it.totalDistance }?.totalDistance ?: Double.MAX_VALUE
            val reasonableTransferRoutes = routesWithTransfers.filter {
                it.totalDistance < bestDirectDistance * 1.5 // Allow up to 50% longer for routes with transfers
            }

            possibleRoutes.addAll(reasonableTransferRoutes)
        }

        // Sort by a combination of distance and number of transfers
        return possibleRoutes.sortedWith(compareBy(
            { it.transfers }, // First sort by number of transfers (fewer is better)
            { it.totalDistance } // Then by total distance
        )).take(5) // Limit to top 5 routes
    }

    /**
     * Find direct routes (no transfers) from start to destination
     */
    private fun findDirectRoutes(start: LatLng, destination: LatLng): List<CompleteRoute> {
        val routes = mutableListOf<CompleteRoute>()

        for ((code, coordinates) in jeepneyRoutes) {
            // Find the closest point on the route to the start location
            val (startPointIndex, startPoint, startDistance) = findClosestPointOnRoute(coordinates, start)

            // Find the closest point on the route to the destination
            val (endPointIndex, endPoint, endDistance) = findClosestPointOnRoute(coordinates, destination)

            // Skip if either point is too far from the route
            if (startDistance > MAX_WALKING_DISTANCE || endDistance > MAX_WALKING_DISTANCE) {
                Log.d("RoutePlanner", "Route $code rejected: walking distance too far (start: $startDistance, end: $endDistance)")
                continue
            }

            // Skip if the destination is before the start on the route
            val actualDistance: Double
            if (endPointIndex >= startPointIndex) {
                actualDistance = calculateRouteDistance(coordinates, startPointIndex, endPointIndex)
            } else {
                // Handle reverse direction
                actualDistance = calculateRouteDistance(coordinates, endPointIndex, startPointIndex)
            }

            // Calculate distance along the route
            val routeDistance = calculateRouteDistance(coordinates, startPointIndex, endPointIndex)
            val totalDistance = startDistance + routeDistance + endDistance

            val segment = RouteSegment(
                jeepCode = code,
                startPoint = startPoint,
                endPoint = endPoint,
                distance = routeDistance
            )

            routes.add(CompleteRoute(
                segments = listOf(segment),
                totalDistance = totalDistance,
                transfers = 0
            ))

            Log.d("RoutePlanner", "Found ${routes.size} direct routes")
        }

        return routes
    }

    /**
     * Find routes with one or two transfers
     */
    private fun findRoutesWithTransfers(start: LatLng, destination: LatLng): List<CompleteRoute> {
        val routes = mutableListOf<CompleteRoute>()

        // For each route that's near the start point
        for ((firstCode, firstCoordinates) in jeepneyRoutes) {
            val (firstStartIndex, firstStartPoint, firstStartDistance) = findClosestPointOnRoute(firstCoordinates, start)

            // Skip if start point is too far from this route
            if (firstStartDistance > MAX_WALKING_DISTANCE) {
                continue
            }

            // Try to find transfer points to other routes
            for ((secondCode, secondCoordinates) in jeepneyRoutes) {
                // Skip same route
                if (firstCode == secondCode) continue

                // Find potential transfer points between first and second route
                val transferPoints = findTransferPoints(firstCoordinates, secondCoordinates)

                for ((firstEndIndex, firstEndPoint, secondStartIndex, secondStartPoint, transferDistance) in transferPoints) {
                    // Skip invalid route segments
                    if (firstEndIndex < firstStartIndex) continue

                    // Find the closest point on the second route to the destination
                    val (secondEndIndex, secondEndPoint, secondEndDistance) = findClosestPointOnRoute(secondCoordinates, destination)

                    // Skip if destination is too far from second route or before the transfer point
                    if (secondEndDistance > MAX_WALKING_DISTANCE || secondEndIndex < secondStartIndex) {
                        continue
                    }

                    // Calculate distances
                    val firstSegmentDistance = calculateRouteDistance(firstCoordinates, firstStartIndex, firstEndIndex)
                    val secondSegmentDistance = calculateRouteDistance(secondCoordinates, secondStartIndex, secondEndIndex)
                    val totalDistance = firstStartDistance + firstSegmentDistance +
                            transferDistance +
                            secondSegmentDistance + secondEndDistance

                    val firstSegment = RouteSegment(
                        jeepCode = firstCode,
                        startPoint = firstStartPoint,
                        endPoint = firstEndPoint,
                        distance = firstSegmentDistance
                    )

                    val secondSegment = RouteSegment(
                        jeepCode = secondCode,
                        startPoint = secondStartPoint,
                        endPoint = secondEndPoint,
                        distance = secondSegmentDistance
                    )

                    routes.add(CompleteRoute(
                        segments = listOf(firstSegment, secondSegment),
                        totalDistance = totalDistance,
                        transfers = 1
                    ))

                    // If we want to try two transfers (three jeepneys total)
                    if (MAX_TRANSFERS > 1) {
                        findThreeSegmentRoutes(
                            start, destination,
                            firstCode, firstCoordinates, firstStartIndex, firstStartPoint, firstStartDistance,
                            secondCode, secondCoordinates, secondEndIndex, secondEndPoint,
                            firstSegmentDistance, secondSegmentDistance, transferDistance,
                            routes
                        )
                    }
                }
            }
        }

        return routes
    }

    /**
     * Search for routes with two transfers (three jeepney segments)
     */
    private fun findThreeSegmentRoutes(
        start: LatLng, destination: LatLng,
        firstCode: String, firstCoordinates: List<Pair<String, LatLng>>,
        firstStartIndex: Int, firstStartPoint: Pair<String, LatLng>, firstStartDistance: Double,
        secondCode: String, secondCoordinates: List<Pair<String, LatLng>>,
        secondLastIndex: Int, secondLastPoint: Pair<String, LatLng>,
        firstSegmentDistance: Double, secondSegmentDistance: Double, firstTransferDistance: Double,
        routes: MutableList<CompleteRoute>
    ) {
        // For each potential second transfer to a third route
        for ((thirdCode, thirdCoordinates) in jeepneyRoutes) {
            // Skip if same as previous route
            if (thirdCode == secondCode) continue

            // Find potential transfer points between second and third route
            val transferPoints = findTransferPoints(secondCoordinates, thirdCoordinates)

            for ((secondEndIndex, secondEndPoint, thirdStartIndex, thirdStartPoint, secondTransferDistance) in transferPoints) {
                // Skip invalid segments
                if (secondEndIndex < 0 || secondEndIndex > secondLastIndex) continue

                // Find the closest point on the third route to the destination
                val (thirdEndIndex, thirdEndPoint, thirdEndDistance) = findClosestPointOnRoute(thirdCoordinates, destination)

                // Skip if destination is too far or before transfer point
                if (thirdEndDistance > MAX_WALKING_DISTANCE || thirdEndIndex < thirdStartIndex) {
                    continue
                }

                // Calculate the remaining distances
                val actualSecondSegmentDistance = calculateRouteDistance(secondCoordinates, 0, secondEndIndex)
                val thirdSegmentDistance = calculateRouteDistance(thirdCoordinates, thirdStartIndex, thirdEndIndex)

                val totalDistance = firstStartDistance + firstSegmentDistance +
                        firstTransferDistance +
                        actualSecondSegmentDistance +
                        secondTransferDistance +
                        thirdSegmentDistance + thirdEndDistance

                // Create segments
                val firstSegment = RouteSegment(
                    jeepCode = firstCode,
                    startPoint = firstStartPoint,
                    endPoint = secondCoordinates[0],
                    distance = firstSegmentDistance
                )

                val secondSegment = RouteSegment(
                    jeepCode = secondCode,
                    startPoint = secondCoordinates[0],
                    endPoint = secondEndPoint,
                    distance = actualSecondSegmentDistance
                )

                val thirdSegment = RouteSegment(
                    jeepCode = thirdCode,
                    startPoint = thirdStartPoint,
                    endPoint = thirdEndPoint,
                    distance = thirdSegmentDistance
                )

                routes.add(CompleteRoute(
                    segments = listOf(firstSegment, secondSegment, thirdSegment),
                    totalDistance = totalDistance,
                    transfers = 2
                ))
            }
        }
    }

    /**
     * Find potential transfer points between two routes where walking is feasible
     */
    private fun findTransferPoints(
        route1: List<Pair<String, LatLng>>,
        route2: List<Pair<String, LatLng>>
    ): List<TransferPoint> {
        val transfers = mutableListOf<TransferPoint>()

        for ((index1, point1) in route1.withIndex()) {
            for ((index2, point2) in route2.withIndex()) {
                val distance = SphericalUtil.computeDistanceBetween(point1.second, point2.second)

                if (distance <= MAX_WALKING_DISTANCE) {
                    transfers.add(TransferPoint(index1, point1, index2, point2, distance))
                }
            }
        }

        // Return only the best transfer points (shortest walking distance)
        return transfers.sortedBy { it.distance }.take(5)
    }

    /**
     * Find the closest point on a route to a given location
     */
    private fun findClosestPointOnRoute(
        routePoints: List<Pair<String, LatLng>>,
        location: LatLng
    ): ClosestPoint {
        var closestIndex = 0
        var closestPoint = routePoints[0]
        var minDistance = SphericalUtil.computeDistanceBetween(location, routePoints[0].second)

        for (i in 1 until routePoints.size) {
            val distance = SphericalUtil.computeDistanceBetween(location, routePoints[i].second)
            if (distance < minDistance) {
                minDistance = distance
                closestIndex = i
                closestPoint = routePoints[i]
            }
        }

        return ClosestPoint(closestIndex, closestPoint, minDistance)
    }

    /**
     * Calculate the distance along a route between two points
     */
    private fun calculateRouteDistance(
        routePoints: List<Pair<String, LatLng>>,
        startIndex: Int,
        endIndex: Int
    ): Double {
        var distance = 0.0
        for (i in startIndex until min(endIndex, routePoints.size - 1)) {
            distance += SphericalUtil.computeDistanceBetween(
                routePoints[i].second,
                routePoints[i + 1].second
            )
        }
        return distance
    }

    /**
     * Data class representing a point on a route closest to a given location
     */
    data class ClosestPoint(
        val index: Int,
        val point: Pair<String, LatLng>,
        val distance: Double
    )

    /**
     * Data class representing a potential transfer point between two routes
     */
    data class TransferPoint(
        val firstRouteIndex: Int,
        val firstRoutePoint: Pair<String, LatLng>,
        val secondRouteIndex: Int,
        val secondRoutePoint: Pair<String, LatLng>,
        val distance: Double
    )
}