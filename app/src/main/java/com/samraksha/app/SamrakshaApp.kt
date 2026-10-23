package com.samraksha.app

import android.app.Application
import androidx.room.Room
import com.samraksha.app.data.local.SafetyZoneLocalCache
import com.samraksha.app.data.local.SamrakshaDatabase
import com.samraksha.app.data.local.SessionManager
import com.samraksha.app.data.remote.ApiClient
import com.samraksha.app.data.repository.AuthRepositoryImpl
import com.samraksha.app.data.repository.JourneyRepositoryImpl
import com.samraksha.app.data.repository.RoomEmergencyContactRepository
import com.samraksha.app.data.repository.RoomEmergencyEventRepository
import com.samraksha.app.data.repository.RoomUserRepository
import com.samraksha.app.data.repository.SafetyZoneRepositoryImpl
import com.samraksha.app.data.repository.SyncRepositoryImpl
import com.samraksha.app.data.repository.ProductRepositoryImpl
import com.samraksha.app.domain.repository.AuthRepository
import com.samraksha.app.domain.repository.EmergencyContactRepository
import com.samraksha.app.domain.repository.EmergencyEventRepository
import com.samraksha.app.domain.repository.JourneyRepository
import com.samraksha.app.domain.repository.ProductRepository
import com.samraksha.app.domain.repository.RoutingRepository
import com.samraksha.app.domain.repository.SafetyZoneRepository
import com.samraksha.app.domain.repository.SyncRepository
import com.samraksha.app.domain.repository.UserRepository
import com.samraksha.app.domain.repository.IncidentReportRepository
import com.samraksha.app.data.repository.IncidentReportRepositoryImpl
import com.samraksha.app.data.repository.GeoapifyRoutingRepositoryImpl
import com.samraksha.app.location.DefaultLocationClient
import com.samraksha.app.location.LocationClient
import com.samraksha.app.notifications.SamrakshaNotificationManager
import com.samraksha.app.utils.NetworkMonitor
import org.maplibre.android.MapLibre

/**
 * Application class for SAMRAKSHA.
 * Acts as the central service locator for the database, repositories, session state, and sync.
 */
class SamrakshaApp : Application() {

    lateinit var sessionManager: SessionManager
        private set

    lateinit var settingsPreferences: com.samraksha.app.data.local.SettingsPreferences
        private set

    lateinit var networkMonitor: NetworkMonitor
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var userRepository: UserRepository
        private set

    lateinit var emergencyContactRepository: EmergencyContactRepository
        private set

    lateinit var emergencyEventRepository: EmergencyEventRepository
        private set

    lateinit var safetyZoneLocalCache: SafetyZoneLocalCache
        private set

    lateinit var safetyZoneRepository: SafetyZoneRepository
        private set

    lateinit var routingRepository: RoutingRepository
        private set

    lateinit var locationClient: LocationClient
        private set

    lateinit var journeyRepository: JourneyRepository
        private set

    lateinit var productRepository: ProductRepository
        private set

    lateinit var incidentReportRepository: IncidentReportRepository
        private set

    lateinit var syncRepository: SyncRepository
        private set

    lateinit var notificationManager: SamrakshaNotificationManager
        private set

    lateinit var emergencyAlertDispatcher: com.samraksha.app.emergency.EmergencyAlertDispatcher
        private set

    val database: SamrakshaDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            SamrakshaDatabase::class.java,
            "samraksha.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize MapLibre Native SDK for Android
        MapLibre.getInstance(this)

        // Create notification channels before anything else so that no notification
        // fires before its channel exists (would silently be dropped by the OS).
        notificationManager = SamrakshaNotificationManager(this)
        notificationManager.createChannels()

        networkMonitor = NetworkMonitor(this)
        sessionManager = SessionManager(this)
        ApiClient.tokenProvider = { sessionManager.getToken() }
        settingsPreferences = com.samraksha.app.data.local.SettingsPreferences(this)
        authRepository = AuthRepositoryImpl(
            apiService = ApiClient.authApiService,
            sessionManager = sessionManager,
            userDao = database.userDao()
        )
        userRepository = RoomUserRepository(
            userDao = database.userDao(),
            apiService = ApiClient.authApiService,
            sessionManager = sessionManager
        )
        safetyZoneLocalCache = SafetyZoneLocalCache(this)
        safetyZoneRepository = SafetyZoneRepositoryImpl(
            apiService = ApiClient.safetyZoneApiService,
            localCache = safetyZoneLocalCache
        )
        emergencyContactRepository = RoomEmergencyContactRepository(
            dao = database.emergencyContactDao(),
            apiService = ApiClient.emergencyContactApiService,
            sessionManager = sessionManager
        )
        emergencyEventRepository = RoomEmergencyEventRepository(
            dao = database.emergencyEventDao(),
            apiService = ApiClient.emergencyEventApiService,
            sessionManager = sessionManager
        )
        locationClient = DefaultLocationClient(this)
        journeyRepository = JourneyRepositoryImpl(
            context = this,
            dao = database.journeyDao(),
            apiService = ApiClient.journeyApiService,
            sessionManager = sessionManager
        )
        productRepository = ProductRepositoryImpl(
            apiService = ApiClient.productApiService
        )
        incidentReportRepository = IncidentReportRepositoryImpl(
            apiService = ApiClient.incidentReportApiService
        )
        routingRepository = GeoapifyRoutingRepositoryImpl()
        emergencyAlertDispatcher = com.samraksha.app.emergency.DefaultEmergencyAlertDispatcher(this)

        // Central synchronization coordinator managing User, Contacts, Events, Journeys & Safety Zones
        syncRepository = SyncRepositoryImpl(
            userRepository = userRepository,
            emergencyContactRepository = emergencyContactRepository,
            emergencyEventRepository = emergencyEventRepository,
            journeyRepository = journeyRepository,
            safetyZoneRepository = safetyZoneRepository,
            networkMonitor = networkMonitor
        )
    }

    companion object {
        lateinit var instance: SamrakshaApp
            private set
    }
}
