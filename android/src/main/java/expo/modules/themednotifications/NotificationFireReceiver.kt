package expo.modules.themednotifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

private const val CHANNEL_ID = "default"
private const val DEFAULT_ICON_NAME = "notification_icon"

/**
 * Fired by the alarm armed in [AlarmScheduler]. Everything here runs defensively and inside
 * try/catch — a crash in a BroadcastReceiver can take down the app's process even when the app
 * itself isn't in the foreground, and a missed reminder is always better than a crash loop.
 */
class NotificationFireReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val identifier = intent.getStringExtra(EXTRA_IDENTIFIER) ?: return
    try {
      val definition = ScheduleStore.get(context, identifier) ?: return
      ensureChannel(context)
      postNotification(context, definition)
    } catch (_: Exception) {
      // Süs özelliği — bir bildirimin kaçması, uygulamanın çökmesinden daha iyidir.
    } finally {
      // Alarmlar tek seferlik: bir sonraki güne, bildirim gösterilemese bile yeniden kurulur.
      AlarmScheduler.armNextDay(context, identifier)
    }
  }

  private fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (manager.getNotificationChannel(CHANNEL_ID) != null) return
    // Defensive fallback only — the app's JS side already creates this channel on launch
    // (`setNotificationChannelAsync`). This just guards against that path ever being removed.
    manager.createNotificationChannel(
      NotificationChannel(CHANNEL_ID, "Varsayılan", NotificationManager.IMPORTANCE_DEFAULT)
    )
  }

  private fun postNotification(context: Context, definition: ScheduleDefinition) {
    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(resolveIconResId(context, definition.androidSmallIcon))
      .setContentTitle(definition.title)
      .setContentText(definition.body)
      .setAutoCancel(true)
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setContentIntent(launchPendingIntent(context, definition.identifier))
      .build()

    NotificationManagerCompat.from(context).notify(definition.identifier.hashCode(), notification)
  }

  /** Notifications don't auto-open the app on tap unless given an explicit content intent. */
  private fun launchPendingIntent(context: Context, identifier: String): PendingIntent? {
    val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
      ?: return null
    return PendingIntent.getActivity(
      context,
      identifier.hashCode(),
      launchIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  /**
   * Resolved by NAME, not by compile-time R reference, since the icon varies per special day and
   * this module doesn't know those names at compile time. R8's `shrinkResources` (on in this
   * app's release builds) can strip resources that are only ever referenced by dynamic string
   * lookup like this — the consuming app must keep a `tools:keep` entry for these drawables.
   */
  private fun resolveIconResId(context: Context, iconName: String?): Int {
    val fallback = context.resources.getIdentifier(DEFAULT_ICON_NAME, "drawable", context.packageName)
    if (iconName.isNullOrBlank()) return fallback
    val resolved = context.resources.getIdentifier(iconName, "drawable", context.packageName)
    return if (resolved != 0) resolved else fallback
  }
}
