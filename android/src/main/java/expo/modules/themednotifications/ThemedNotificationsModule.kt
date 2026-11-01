package expo.modules.themednotifications

import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record

// `data` arrives pre-serialized (JS calls JSON.stringify before invoking this) — avoids relying
// on Map<String, Any?> JS-to-Kotlin conversion for a value this module never reads anyway.
class ScheduleOptions : Record {
  @Field var identifier: String = ""
  @Field var hour: Int = 0
  @Field var minute: Int = 0
  @Field var title: String = ""
  @Field var body: String = ""
  @Field var androidSmallIcon: String? = null
  @Field var dataJson: String? = null
}

class ThemedNotificationsModule : Module() {
  override fun definition() = ModuleDefinition {
    Name("ThemedNotifications")

    AsyncFunction("scheduleDailyNotificationAsync") { options: ScheduleOptions ->
      scheduleDailyNotification(options)
    }

    AsyncFunction("cancelDailyNotificationAsync") { identifier: String ->
      cancelDailyNotification(identifier)
    }

    AsyncFunction("cancelAllDailyNotificationsAsync") {
      cancelAllDailyNotifications()
    }
  }

  private fun scheduleDailyNotification(options: ScheduleOptions) {
    val context = appContext.reactContext ?: return
    val definition = ScheduleDefinition(
      identifier = options.identifier,
      hour = options.hour,
      minute = options.minute,
      title = options.title,
      body = options.body,
      dataJson = options.dataJson,
      androidSmallIcon = options.androidSmallIcon,
    )
    ScheduleStore.put(context, definition)
    AlarmScheduler.arm(context, definition)
  }

  private fun cancelDailyNotification(identifier: String) {
    val context = appContext.reactContext ?: return
    AlarmScheduler.cancel(context, identifier)
    ScheduleStore.remove(context, identifier)
  }

  private fun cancelAllDailyNotifications() {
    val context = appContext.reactContext ?: return
    ScheduleStore.all(context).forEach { AlarmScheduler.cancel(context, it.identifier) }
    ScheduleStore.clear(context)
  }
}
