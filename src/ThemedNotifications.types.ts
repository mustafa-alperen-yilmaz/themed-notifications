export interface ScheduleDailyNotificationOptions {
  /** Stable id — scheduling again with the same id replaces the previous one. */
  identifier: string;
  hour: number;
  minute: number;
  title: string;
  body: string;
  data?: Record<string, unknown>;
  /**
   * Android-only: drawable resource name (e.g. `"notification_icon_halloween"`) for the
   * status-bar small icon. Falls back to the app's default `notification_icon` drawable when
   * omitted or not found. Ignored on iOS — the OS already shows the app's current icon in
   * notification banners automatically.
   */
  androidSmallIcon?: string;
}

export interface ThemedNotificationsModuleType {
  scheduleDailyNotificationAsync(options: ScheduleDailyNotificationOptions & { dataJson?: string }): Promise<void>;
  cancelDailyNotificationAsync(identifier: string): Promise<void>;
  cancelAllDailyNotificationsAsync(): Promise<void>;
}
