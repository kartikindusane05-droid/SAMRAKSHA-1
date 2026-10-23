package com.samraksha.app.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.samraksha.app.data.model.SafetyZoneDto

/**
 * Persistent local cache for Safety Zones.
 * Stores safety zones locally to ensure data remains accessible when offline.
 */
class SafetyZoneLocalCache(context: Context? = null) {

    private val prefs: SharedPreferences? = context?.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val gson = Gson()
    private val zoneListType = object : TypeToken<List<SafetyZoneDto>>() {}.type

    // In-memory cache for fast UI access
    private var inMemoryZones: List<SafetyZoneDto>? = null
    private var inMemoryTimestamp: Long = 0L

    init {
        // Hydrate from SharedPreferences if available
        prefs?.let {
            val json = it.getString(KEY_ZONES_JSON, null)
            inMemoryTimestamp = it.getLong(KEY_CACHE_TIMESTAMP, 0L)
            if (!json.isNullOrEmpty()) {
                try {
                    inMemoryZones = gson.fromJson(json, zoneListType)
                } catch (_: Exception) {
                    inMemoryZones = null
                }
            }
        }
    }

    /**
     * Persist zones to local cache.
     */
    fun saveZones(zones: List<SafetyZoneDto>) {
        val now = System.currentTimeMillis()
        inMemoryZones = zones
        inMemoryTimestamp = now

        prefs?.edit()?.apply {
            putString(KEY_ZONES_JSON, gson.toJson(zones, zoneListType))
            putLong(KEY_CACHE_TIMESTAMP, now)
            apply()
        }
    }

    /**
     * Retrieve cached zones, returning empty list if no local data exists.
     */
    fun getCachedZones(): List<SafetyZoneDto> {
        return inMemoryZones ?: emptyList()
    }

    /**
     * Check if valid cached zones are available.
     */
    fun hasCachedZones(): Boolean {
        return !inMemoryZones.isNullOrEmpty()
    }

    /**
     * Timestamp (ms) when zones were last saved locally.
     */
    fun getLastCacheTimestamp(): Long {
        return inMemoryTimestamp
    }

    /**
     * Clear all cached zones.
     */
    fun clearCache() {
        inMemoryZones = null
        inMemoryTimestamp = 0L
        prefs?.edit()?.apply {
            remove(KEY_ZONES_JSON)
            remove(KEY_CACHE_TIMESTAMP)
            apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "samraksha_safety_zones_cache"
        private const val KEY_ZONES_JSON = "cached_safety_zones"
        private const val KEY_CACHE_TIMESTAMP = "cached_zones_timestamp"
    }
}
