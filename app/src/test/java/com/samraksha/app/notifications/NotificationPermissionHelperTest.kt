package com.samraksha.app.notifications

import android.Manifest
import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPermissionHelperTest {

    @Test
    fun rationaleStrings_areSafetyFocusedAndRejectMarketing() {
        assertTrue(NotificationPermissionHelper.RATIONALE_TITLE.isNotBlank())
        assertTrue(NotificationPermissionHelper.RATIONALE_MESSAGE.contains("marketing", ignoreCase = true))
        assertTrue(NotificationPermissionHelper.RATIONALE_MESSAGE.contains("safety", ignoreCase = true))
        assertTrue(NotificationPermissionHelper.RATIONALE_PRIVACY_NOTE.contains("private", ignoreCase = true))
        assertTrue(NotificationPermissionHelper.PERMANENTLY_DENIED_TITLE.isNotBlank())
        assertTrue(NotificationPermissionHelper.PERMANENTLY_DENIED_MESSAGE.contains("settings", ignoreCase = true))
    }

    @Test
    fun permissionConstant_matchesAndroidSpecification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            assertEquals(Manifest.permission.POST_NOTIFICATIONS, NotificationPermissionHelper.PERMISSION)
            assertEquals(1, NotificationPermissionHelper.REQUIRED_PERMISSIONS.size)
            assertEquals(Manifest.permission.POST_NOTIFICATIONS, NotificationPermissionHelper.REQUIRED_PERMISSIONS[0])
        } else {
            assertEquals(null, NotificationPermissionHelper.PERMISSION)
            assertEquals(0, NotificationPermissionHelper.REQUIRED_PERMISSIONS.size)
        }
    }
}
