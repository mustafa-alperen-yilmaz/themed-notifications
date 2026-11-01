import { registerWebModule, NativeModule } from 'expo';
import type { ThemedNotificationsModuleType } from './ThemedNotifications.types';

class ThemedNotificationsModule extends NativeModule<{}> implements ThemedNotificationsModuleType {
  async scheduleDailyNotificationAsync(): Promise<void> {}

  async cancelDailyNotificationAsync(): Promise<void> {}

  async cancelAllDailyNotificationsAsync(): Promise<void> {}
}

export default registerWebModule(ThemedNotificationsModule, 'ThemedNotificationsModule');
