# themed-notifications

Schedule daily-repeating local notifications on iOS and Android whose **Android status-bar
small icon** can be swapped per notification — something `expo-notifications` can't do, since it
bakes a single icon drawable into the APK at build time.

On iOS this module is a thin wrapper around `UNUserNotificationCenter` — there's no small-icon
concept to swap there; the OS already shows the app's current (possibly alternate) icon in
notification banners automatically. The Android side owns its own scheduling end-to-end
(`AlarmManager` + a `BroadcastReceiver`) instead of going through `expo-notifications`, so it can
pick a different icon resource by name every time a notification fires.

## Installation

```sh
npx expo install themed-notifications
```

## API

```ts
import {
  scheduleDailyNotification,
  cancelDailyNotification,
  cancelAllDailyNotifications,
} from 'themed-notifications';

await scheduleDailyNotification({
  identifier: 'streak_warning',
  hour: 21,
  minute: 0,
  title: 'TestApp 🎃',
  body: "Test Notification!",
  androidSmallIcon: 'notification_icon_halloween', // optional, Android only
});

await cancelDailyNotification('streak_warning');
await cancelAllDailyNotifications();
```

Calling `scheduleDailyNotification` again with the same `identifier` replaces the previous
schedule — safe to call on every app launch/foreground to refresh content that depends on
today's date (which is exactly what the consuming app does, since the OS otherwise bakes in
whatever title/icon was set at schedule time and never refreshes it on its own).

## Android setup

1. **Icon drawables**: provide a `notification_icon` drawable (the default/fallback) and, for
   each themed variant, a differently-named monochrome drawable (e.g.
   `notification_icon_halloween`) at the usual `drawable-{m,h,x,xx,xxx}hdpi/` density buckets.
   Pass the name (without extension) as `androidSmallIcon`.
2. **Resource shrinking**: if the consuming app has `shrinkResources` enabled on release builds,
   add a `res/raw/keep.xml` with a `tools:keep` entry covering these drawables — they're only
   ever referenced by runtime string lookup (`resources.getIdentifier`), which R8 can't see
   statically and will otherwise strip.
3. **Reboot persistence**: add `<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>`
   to the app's own `AndroidManifest.xml` (this module's manifest declares the receiver itself;
   the permission has to land in the final merged manifest).
4. A force-stopped app won't receive `BOOT_COMPLETED` (or anything else) until the user manually
   reopens it — Android platform behavior, not something this module works around. Calling
   `scheduleDailyNotification` again on app foreground is the practical backstop: it re-arms
   the alarm every time the app is opened, so a missed reboot re-arm self-heals quickly.

## Why not just use `expo-notifications`?

It already does 95% of what's needed (permissions, channels, daily triggers) — this module only
exists for the remaining 5%: a per-notification Android small icon. It deliberately reuses the
same notification channel `expo-notifications` creates (`"default"`) rather than introducing a
second one, so there's exactly one channel visible in system notification settings.
