import ThemedNotificationsModule from './ThemedNotificationsModule';
import type { ScheduleDailyNotificationOptions } from './ThemedNotifications.types';

export type { ScheduleDailyNotificationOptions } from './ThemedNotifications.types';

/**
 * Schedules a daily-repeating local notification. Calling this again with the same
 * `identifier` replaces the previous schedule (hour/minute/title/body/icon can all change).
 *
 * `data` is JSON-stringified on the JS side before crossing into native — avoids relying on
 * JS-to-native Map/Dictionary type conversion for a value neither native side needs to read.
 */
export function scheduleDailyNotification(options: ScheduleDailyNotificationOptions): Promise<void> {
  const { data, ...rest } = options;
  return ThemedNotificationsModule.scheduleDailyNotificationAsync({
    ...rest,
    dataJson: data ? JSON.stringify(data) : undefined,
  });
}

/** Cancels a single scheduled notification by its identifier. No-op if it isn't scheduled. */
export function cancelDailyNotification(identifier: string): Promise<void> {
  return ThemedNotificationsModule.cancelDailyNotificationAsync(identifier);
}

/** Cancels every notification scheduled through this module. */
export function cancelAllDailyNotifications(): Promise<void> {
  return ThemedNotificationsModule.cancelAllDailyNotificationsAsync();
}
