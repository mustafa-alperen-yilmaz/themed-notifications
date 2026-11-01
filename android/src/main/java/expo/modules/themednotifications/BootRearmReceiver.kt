package expo.modules.themednotifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * AlarmManager drops every alarm on reboot. This re-arms everything that was persisted in
 * [ScheduleStore]. Note this does NOT fire for an app the user force-stopped — Android withholds
 * BOOT_COMPLETED (and every other broadcast) from a force-stopped app until the user manually
 * reopens it. That's an accepted, documented limitation shared by every AlarmManager-based
 * reminder library; fighting it is out of scope. The JS layer's foreground re-sync is the
 * practical backstop for that case (reminders self-heal the next time the app is opened).
 */
class BootRearmReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
    try {
      ScheduleStore.all(context).forEach { AlarmScheduler.arm(context, it) }
    } catch (_: Exception) {
      // Süs özelliği — başarısızlık uygulamayı etkilemesin.
    }
  }
}
