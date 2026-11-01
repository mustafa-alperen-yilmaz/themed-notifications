import { NativeModule, requireNativeModule } from 'expo';
import type { ThemedNotificationsModuleType } from './ThemedNotifications.types';

declare class ThemedNotificationsModule
  extends NativeModule<{}>
  implements ThemedNotificationsModuleType
{
  scheduleDailyNotificationAsync(options: {
    identifier: string;
    hour: number;
    minute: number;
    title: string;
    body: string;
    androidSmallIcon?: string;
    dataJson?: string;
  }): Promise<void>;
  cancelDailyNotificationAsync(identifier: string): Promise<void>;
  cancelAllDailyNotificationsAsync(): Promise<void>;
}

export default requireNativeModule<ThemedNotificationsModule>('ThemedNotifications');
