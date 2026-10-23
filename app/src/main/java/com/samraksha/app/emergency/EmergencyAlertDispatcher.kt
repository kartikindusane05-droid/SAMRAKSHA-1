package com.samraksha.app.emergency

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.samraksha.app.domain.model.EmergencyContact
import com.samraksha.app.location.model.UserLocation
import java.util.Locale

/**
 * Delivery status for an individual emergency contact alert.
 */
enum class AlertDeliveryState {
    PENDING,
    SENT,
    DELIVERED,
    FAILED_PERMISSION_DENIED,
    FAILED_NO_SERVICE,
    FAILED_INVALID_NUMBER,
    FAILED_UNKNOWN
}

/**
 * Record of an alert attempt to a specific emergency contact.
 */
data class ContactAlertDelivery(
    val contactId: String,
    val contactName: String,
    val phoneNumber: String,
    val status: AlertDeliveryState,
    val errorMessage: String? = null
)

/**
 * Interface responsible for preparing, formatting, and dispatching manual SOS alerts.
 */
interface EmergencyAlertDispatcher {
    fun hasSmsPermission(): Boolean
    fun canSendSms(): Boolean
    fun formatEmergencyMessage(
        userName: String?,
        location: UserLocation?,
        address: String? = null
    ): String
    suspend fun dispatchAlerts(
        contacts: List<EmergencyContact>,
        message: String
    ): List<ContactAlertDelivery>
    fun createMessagingIntent(phoneNumbers: List<String>, message: String): Intent
}

/**
 * Default implementation using Android telephony and SmsManager with intent fallback.
 */
class DefaultEmergencyAlertDispatcher(
    private val context: Context
) : EmergencyAlertDispatcher {

    override fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun canSendSms(): Boolean {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        return telephonyManager != null && telephonyManager.phoneType != TelephonyManager.PHONE_TYPE_NONE
    }

    override fun formatEmergencyMessage(
        userName: String?,
        location: UserLocation?,
        address: String?
    ): String {
        val sender = if (!userName.isNullOrBlank()) userName else "A SAMRAKSHA user"
        val locationSection = if (location != null && (location.latitude != 0.0 || location.longitude != 0.0)) {
            val mapLink = String.format(
                Locale.US,
                "https://www.openstreetmap.org/?mlat=%.6f&mlon=%.6f#map=16/%.6f/%.6f",
                location.latitude,
                location.longitude,
                location.latitude,
                location.longitude
            )
            val addrLine = if (!address.isNullOrBlank()) "\nNear: $address" else ""
            "Location: $mapLink$addrLine\nGPS: ${String.format(Locale.US, "%.5f, %.5f", location.latitude, location.longitude)}"
        } else {
            "Location: Unavailable (Coordinates could not be acquired at trigger time)"
        }

        return "EMERGENCY SOS ALERT!\n" +
                "$sender has triggered an emergency alert in SAMRAKSHA.\n" +
                "$locationSection\n" +
                "ACTION REQUIRED: Call immediately or contact emergency services (112)."
    }

    override suspend fun dispatchAlerts(
        contacts: List<EmergencyContact>,
        message: String
    ): List<ContactAlertDelivery> {
        if (contacts.isEmpty()) {
            return emptyList()
        }

        if (!hasSmsPermission()) {
            return contacts.map { contact ->
                ContactAlertDelivery(
                    contactId = contact.id,
                    contactName = contact.name,
                    phoneNumber = contact.phoneNumber,
                    status = AlertDeliveryState.FAILED_PERMISSION_DENIED,
                    errorMessage = "SMS permission required for automated delivery"
                )
            }
        }

        if (!canSendSms()) {
            return contacts.map { contact ->
                ContactAlertDelivery(
                    contactId = contact.id,
                    contactName = contact.name,
                    phoneNumber = contact.phoneNumber,
                    status = AlertDeliveryState.FAILED_NO_SERVICE,
                    errorMessage = "No active cellular service or SIM card detected"
                )
            }
        }

        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }

        val results = mutableListOf<ContactAlertDelivery>()

        for (contact in contacts) {
            val cleanedNumber = contact.phoneNumber.replace(Regex("[^0-9+]"), "")
            if (cleanedNumber.length < 10) {
                results.add(
                    ContactAlertDelivery(
                        contactId = contact.id,
                        contactName = contact.name,
                        phoneNumber = contact.phoneNumber,
                        status = AlertDeliveryState.FAILED_INVALID_NUMBER,
                        errorMessage = "Invalid phone number"
                    )
                )
                continue
            }

            try {
                val parts = smsManager.divideMessage(message)
                if (parts.size > 1) {
                    smsManager.sendMultipartTextMessage(cleanedNumber, null, parts, null, null)
                } else {
                    smsManager.sendTextMessage(cleanedNumber, null, message, null, null)
                }
                results.add(
                    ContactAlertDelivery(
                        contactId = contact.id,
                        contactName = contact.name,
                        phoneNumber = contact.phoneNumber,
                        status = AlertDeliveryState.SENT
                    )
                )
            } catch (e: Exception) {
                results.add(
                    ContactAlertDelivery(
                        contactId = contact.id,
                        contactName = contact.name,
                        phoneNumber = contact.phoneNumber,
                        status = AlertDeliveryState.FAILED_UNKNOWN,
                        errorMessage = e.localizedMessage ?: "Failed to transmit SMS"
                    )
                )
            }
        }

        return results
    }

    override fun createMessagingIntent(phoneNumbers: List<String>, message: String): Intent {
        val joinedNumbers = phoneNumbers.joinToString(";")
        return Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$joinedNumbers")
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
