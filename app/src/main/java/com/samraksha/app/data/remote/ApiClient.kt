package com.samraksha.app.data.remote

import com.samraksha.app.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton networking client managing Retrofit and OkHttpClient instances.
 * Implements security hardening:
 * - Dynamic token injection via AuthInterceptor
 * - Strict header redaction (Authorization, Cookie) to prevent credential leakage
 * - Body logging disabled in release builds to eliminate PII/password exposure in logs
 */
object ApiClient {

    /**
     * Base URL for the SAMRAKSHA backend API.
     * Injected per build type via BuildConfig.BASE_URL (HTTPS in release, local IP in debug).
     * Can be overridden dynamically via setBaseUrl() if needed.
     */

    private var baseUrl: String = if (BuildConfig.BASE_URL.isNotBlank()) {
        if (BuildConfig.BASE_URL.endsWith("/")) BuildConfig.BASE_URL
        else "${BuildConfig.BASE_URL}/"
    } else {
        "https://samraksha-backend.onrender.com/"
    }


    /**
     * Token provider lambda supplied by SessionManager to inject authentication bearer tokens.
     */
    var tokenProvider: (() -> String?)? = null

    fun setBaseUrl(url: String) {
        val formatted = if (url.endsWith("/")) url else "$url/"
        baseUrl = formatted
        retrofit = buildRetrofit()
        authApiService = retrofit.create(AuthApiService::class.java)
        safetyZoneApiService = retrofit.create(SafetyZoneApiService::class.java)
        emergencyContactApiService = retrofit.create(EmergencyContactApiService::class.java)
        emergencyEventApiService = retrofit.create(EmergencyEventApiService::class.java)
        journeyApiService = retrofit.create(JourneyApiService::class.java)
        productApiService = retrofit.create(ProductApiService::class.java)
        incidentReportApiService = retrofit.create(IncidentReportApiService::class.java)
    }

    fun getBaseUrl(): String = baseUrl

    /**
     * Logging interceptor configured to prevent sensitive data (passwords, tokens, coordinates)
     * from leaking into system logs.
     *
     * NOTE: Level.HEADERS is intentionally disabled even in debug builds because Authorization
     * bearer tokens must not appear in device logcat (readable by any app with READ_LOGS).
     * To inspect network traffic during development, use a proxy tool such as mitmproxy or
     * Android Studio Network Inspector instead.
     */
    private val loggingInterceptor: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
            redactHeader("Cookie")
            redactHeader("Set-Cookie")
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            // 1. Automatic Authorization Header Injection
            .addInterceptor { chain ->
                val original = chain.request()
                val token = tokenProvider?.invoke()
                val request = if (!token.isNullOrBlank() && original.header("Authorization") == null) {
                    original.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } else {
                    original
                }
                chain.proceed(request)
            }
            // 2. Redacted Security Logging
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private fun buildRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private var retrofit: Retrofit = buildRetrofit()

    var authApiService: AuthApiService = retrofit.create(AuthApiService::class.java)
        private set

    var safetyZoneApiService: SafetyZoneApiService = retrofit.create(SafetyZoneApiService::class.java)
        private set

    var emergencyContactApiService: EmergencyContactApiService = retrofit.create(EmergencyContactApiService::class.java)
        private set

    var emergencyEventApiService: EmergencyEventApiService = retrofit.create(EmergencyEventApiService::class.java)
        private set

    var journeyApiService: JourneyApiService = retrofit.create(JourneyApiService::class.java)
        private set

    var productApiService: ProductApiService = retrofit.create(ProductApiService::class.java)
        private set

    var incidentReportApiService: IncidentReportApiService = retrofit.create(IncidentReportApiService::class.java)
        private set
}
