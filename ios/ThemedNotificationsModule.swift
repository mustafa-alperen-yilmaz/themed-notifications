import ExpoModulesCore
import UserNotifications

struct ScheduleOptions: Record {
  @Field var identifier: String = ""
  @Field var hour: Int = 0
  @Field var minute: Int = 0
  @Field var title: String = ""
  @Field var body: String = ""
  // Android-only — the small status-bar icon doesn't exist as a concept on iOS. The OS already
  // shows the app's current (possibly alternate) icon in notification banners automatically,
  // driven by the sibling `icon-swapper` module, so there's nothing to do with this on iOS.
  @Field var androidSmallIcon: String?
  @Field var dataJson: String?
}

/// Schedules a daily-repeating local notification via `UNUserNotificationCenter`.
///
/// Every `UNUserNotificationCenter` access below lives inside a function body, never in a
/// stored property initializer — the same crash class `IconSwapperModule` was built to avoid
/// under the bridgeless New Architecture (see its README).
public class ThemedNotificationsModule: Module {
  public func definition() -> ModuleDefinition {
    Name("ThemedNotifications")

    AsyncFunction("scheduleDailyNotificationAsync") { (options: ScheduleOptions) in
      await self.scheduleDailyNotification(options)
    }

    AsyncFunction("cancelDailyNotificationAsync") { (identifier: String) in
      await self.cancelDailyNotification(identifier)
    }

    AsyncFunction("cancelAllDailyNotificationsAsync") {
      await self.cancelAllDailyNotifications()
    }
  }

  @MainActor
  private func scheduleDailyNotification(_ options: ScheduleOptions) async {
    let center = UNUserNotificationCenter.current()
    center.removePendingNotificationRequests(withIdentifiers: [options.identifier])

    let content = UNMutableNotificationContent()
    content.title = options.title
    content.body = options.body
    if let dataJson = options.dataJson, let data = dataJson.data(using: .utf8) {
      let parsed = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
      content.userInfo = parsed ?? [:]
    }

    var dateComponents = DateComponents()
    dateComponents.hour = options.hour
    dateComponents.minute = options.minute
    let trigger = UNCalendarNotificationTrigger(dateMatching: dateComponents, repeats: true)

    let request = UNNotificationRequest(identifier: options.identifier, content: content, trigger: trigger)
    try? await center.add(request)
  }

  @MainActor
  private func cancelDailyNotification(_ identifier: String) async {
    UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [identifier])
  }

  @MainActor
  private func cancelAllDailyNotifications() async {
    UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
  }
}
