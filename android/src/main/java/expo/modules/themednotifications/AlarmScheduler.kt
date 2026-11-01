package expo.modules.themednotifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

internal const val ACTION_FIRE = "expo.modules.themednotifications.ACTION_FIRE"
internal const val EXTRA_IDENTIFIER = "identifier"

/**
 * Deliberately inexact (`setAndAllowWhileIdle`, not `setExact*`): a ~9pm study reminder has no
 * need for to-the-minute precision, and exact alarms require `SCHEDULE_EXACT_ALARM`/
 * `USE_EXACT_ALARM` plus Play Console justification on Android 12+ — unnecessary friction for
 * this use case. This is the same class of trigger expo-notifications' own DAILY trigger uses.
 *
 * Alarms are one-shot by design (AlarmManager has no reliable "repeat daily at this time"
 * primitive once Doze is involved), so every fire re-arms itself for +24h — see
 * [NotificationFireReceiver]. [BootRearmReceiver] is a second, independent backstop since
 * AlarmManager clears all alarms on reboot; a third backstop lives in the JS layer, which
 * re-calls `scheduleDailyNotificationAsync` on every app foreground.
 */
internal object AlarmScheduler {
  fun arm(context: Context, definition: ScheduleDefinition) {
    arm(context, definition.identifier, nextTriggerMillis(definition.hour, definition.minute))
  }

  fun armNextDay(context: Context, identifier: String) {
    val definition = ScheduleStore.get(context, identifier) ?: return
    val calendar = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, definition.hour)
      set(Calendar.MINUTE, definition.minute)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
      add(Calendar.DATE, 1)
    }
    arm(context, identifier, calendar.timeInMillis)
  }

  fun cancel(context: Context, identifier: String) {
    alarmManager(context).cancel(pendingIntentFor(context, identifier))
  }

  private fun arm(context: Context, identifier: String, triggerAtMillis: Long) {
    alarmManager(context).setAndAllowWhileIdle(
      AlarmManager.RTC_WAKEUP,
      triggerAtMillis,
      pendingIntentFor(context, identifier),
    )
  }

  private fun pendingIntentFor(context: Context, identifier: String): PendingIntent {
    val intent = Intent(context, NotificationFireReceiver::class.java).apply {
      action = ACTION_FIRE
      putExtra(EXTRA_IDENTIFIER, identifier)
    }
    return PendingIntent.getBroadcast(
      context,
      identifier.hashCode(),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  private fun nextTriggerMillis(hour: Int, minute: Int): Long {
    val target = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, hour)
      set(Calendar.MINUTE, minute)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    if (!target.after(Calendar.getInstance())) target.add(Calendar.DATE, 1)
    return target.timeInMillis
  }

  private fun alarmManager(context: Context) =
    context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
}
