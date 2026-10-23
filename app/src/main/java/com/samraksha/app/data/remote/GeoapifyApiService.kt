package com.samraksha.app.data.remote

import com.samraksha.app.data.model.GeoapifyGeocodeResponse
import com.samraksha.app.data.model.GeoapifyPlacesResponse
import com.samraksha.app.data.model.GeoapifyRoutingResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for Geoapify Routing, Geocoding, and Places APIs.
 */
interface GeoapifyApiService {

    @GET("v1/routing")
    suspend fun getRoute(
        @Query("waypoints") waypoints: String, // e.g. "lat1,lon1|lat2,lon2"
        @Query("mode") mode: String = "drive",
        @Query("type") type: String? = null,    // "balanced", "short", "less_maneuvers"
        @Query("details") details: String? = null, // "route_details,instruction_details"
        @Query("apiKey") apiKey: String
    ): Response<GeoapifyRoutingResponse>

    @GET("v1/geocode/search")
    suspend fun geocode(
        @Query("text") text: String,
        @Query("apiKey") apiKey: String,
        @Query("limit") limit: Int = 1
    ): Response<GeoapifyGeocodeResponse>

    @GET("v1/geocode/autocomplete")
    suspend fun autocomplete(
        @Query("text") text: String,
        @Query("apiKey") apiKey: String,
        @Query("limit") limit: Int = 6,
        @Query("bias") bias: String? = null,
        @Query("filter") filter: String? = null
    ): Response<GeoapifyGeocodeResponse>

    @GET("v2/places")
    suspend fun getPlaces(
        @Query("categories") categories: String,
        @Query("filter") filter: String,        // e.g. "circle:lon,lat,radius"
        @Query("limit") limit: Int = 30,
        @Query("apiKey") apiKey: String
    ): Response<GeoapifyPlacesResponse>
}
