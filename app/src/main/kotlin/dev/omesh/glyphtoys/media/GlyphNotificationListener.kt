package dev.omesh.glyphtoys.media

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService

/**
 * Exists only to be granted notification access.
 *
 * `MediaSessionManager.getActiveSessions` will not tell you what is playing unless the caller is
 * an enabled notification listener, and the only way to become one is to declare a service like
 * this and have the user switch it on. It deliberately does nothing with notifications.
 */
class GlyphNotificationListener : NotificationListenerService() {

    companion object {

        /** Whether the user has granted us notification access yet. */
        fun isEnabled(context: Context): Boolean {
            val listeners = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners",
            ) ?: return false
            val us = ComponentName(context, GlyphNotificationListener::class.java)
            return listeners.split(':').any {
                ComponentName.unflattenFromString(it)?.packageName == us.packageName
            }
        }
    }
}
