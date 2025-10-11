import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Util for handling all standard permissions via ActivityCompat.requestPermissions():
 * - writing image
 * - notification
 **/
object StandardPermissionsUtil {
    const val PERM_REQUEST_CODE_COMBINED = 65754
    const val PERM_REQUEST_CODE_NOTIFICATION = 12432

    // Main function to check and request all standard perms (and execute callback if already granted)
    fun requestStandardPermissions(activity: Activity) {
        // Check if need to get perms (if already granted)
        // Notification perms
        val notificationPerms = getRequiredNotificationPerms(activity)
        if (notificationPerms.isEmpty()) {
            permsCallback(activity, true, PERM_REQUEST_CODE_NOTIFICATION)
        }

        // Request perms based on string (only if needed)
        val permissions = notificationPerms
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity, permissions.toTypedArray(), PERM_REQUEST_CODE_COMBINED
            )
        }
    }

    // Main callback from activity result
    fun handlePermissionsResult(
        activity: Activity,
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERM_REQUEST_CODE_COMBINED) {
            permissions.forEachIndexed { index, permission ->
                val granted = grantResults[index] == PackageManager.PERMISSION_GRANTED
                when (permission) {
                    // Notification
                    Manifest.permission.POST_NOTIFICATIONS -> {
                        permsCallback(activity, granted, PERM_REQUEST_CODE_NOTIFICATION)
                    }
                }
            }
        }
    }

    /**
     * Check and get required perms string list
     **/
    // Notification perms for foreground service notification
    private fun getRequiredNotificationPerms(activity: Activity): List<String> {
        val permissionsToRequest = mutableListOf<String>()

        // Only needed for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        return permissionsToRequest
    }

    // Execute callback based on granted or denied
    private fun permsCallback(activity: Activity, granted: Boolean, requestCode: Int) {
        if (activity is StandardPermissionCallback) {
            if (granted) {
                activity.onStandardPermissionsGranted(requestCode)
            } else {
                activity.onStandardPermissionsDenied(requestCode)
            }
        }
    }

    interface StandardPermissionCallback {
        fun onStandardPermissionsGranted(requestCode: Int)
        fun onStandardPermissionsDenied(requestCode: Int)
    }
}
