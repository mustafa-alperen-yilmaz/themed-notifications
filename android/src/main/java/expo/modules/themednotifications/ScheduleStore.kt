package expo.modules.themednotifications

import android.content.Context
import org.json.JSONObject

/** One scheduled daily local notification, as persisted across process death / reboot. */
internal data class ScheduleDefinition(
  val identifier: String,
  val hour: Int,
  val minute: Int,
  val title: String,
  val body: String,
  val dataJson: String?,
  val androidSmallIcon: String?,
)

private const val PREFS_NAME = "themed_notifications_schedules"
private const val KEY_SCHEDULES = "schedules"

/**
 * AlarmManager clears every alarm on reboot and the process can die between scheduling and
 * firing, so the schedule itself has to live outside memory. A single JSON blob in
 * SharedPreferences, keyed by identifier, is enough — there are only ever a handful of these
 * per app (one per reminder type), never a real dataset.
 */
internal object ScheduleStore {
  fun all(context: Context): List<ScheduleDefinition> {
    val raw = prefs(context).getString(KEY_SCHEDULES, null) ?: return emptyList()
    val root = JSONObject(raw)
    return root.keys().asSequence().mapNotNull { identifier ->
      val entry = root.optJSONObject(identifier) ?: return@mapNotNull null
      ScheduleDefinition(
        identifier = identifier,
        hour = entry.optInt("hour"),
        minute = entry.optInt("minute"),
        title = entry.optString("title"),
        body = entry.optString("body"),
        dataJson = if (entry.has("data")) entry.optString("data") else null,
        androidSmallIcon = if (entry.has("androidSmallIcon")) entry.optString("androidSmallIcon") else null,
      )
    }.toList()
  }

  fun get(context: Context, identifier: String): ScheduleDefinition? =
    all(context).firstOrNull { it.identifier == identifier }

  fun put(context: Context, definition: ScheduleDefinition) {
    val root = readRoot(context)
    val entry = JSONObject().apply {
      put("hour", definition.hour)
      put("minute", definition.minute)
      put("title", definition.title)
      put("body", definition.body)
      definition.dataJson?.let { put("data", it) }
      definition.androidSmallIcon?.let { put("androidSmallIcon", it) }
    }
    root.put(definition.identifier, entry)
    writeRoot(context, root)
  }

  fun remove(context: Context, identifier: String) {
    val root = readRoot(context)
    root.remove(identifier)
    writeRoot(context, root)
  }

  fun clear(context: Context) {
    prefs(context).edit().remove(KEY_SCHEDULES).apply()
  }

  private fun readRoot(context: Context): JSONObject {
    val raw = prefs(context).getString(KEY_SCHEDULES, null)
    return if (raw != null) JSONObject(raw) else JSONObject()
  }

  private fun writeRoot(context: Context, root: JSONObject) {
    prefs(context).edit().putString(KEY_SCHEDULES, root.toString()).apply()
  }

  private fun prefs(context: Context) =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
