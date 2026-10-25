package app.vela.core.data

import android.content.Context
import app.vela.core.model.VehicleProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * The local vehicle garage. It is a JSON list in [PREFS], the same shape as the other small
 * stores, because the app keeps no database for user settings. The active id lives in the same
 * file so a deleted vehicle cannot leave an orphan behind.
 *
 * The file is deliberately not in the Android backup include lists: a plate and the vehicle's
 * dimensions stay on the phone.
 */
@Singleton
class VehicleStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    init {
        publish()
    }

    @Synchronized
    fun all(): List<VehicleProfile> = VehicleData.decode(prefs.getString(KEY_VEHICLES, null))

    @Synchronized
    fun activeId(): String? = VehicleData.activeAfter(all(), prefs.getString(KEY_ACTIVE, null))

    @Synchronized
    fun active(): VehicleProfile? {
        val all = all()
        val id = VehicleData.activeAfter(all, prefs.getString(KEY_ACTIVE, null)) ?: return null
        return all.firstOrNull { it.id == id }
    }

    /** Insert or update [vehicle]. The first saved vehicle becomes active. */
    @Synchronized
    fun save(vehicle: VehicleProfile): List<VehicleProfile> {
        val all = all()
        val saved = VehicleData.upsert(all, vehicle, System.currentTimeMillis())
        val active = activeIdIn(saved, prefs.getString(KEY_ACTIVE, null)) ?: saved.firstOrNull()?.id
        write(saved, active)
        publish()
        return saved
    }

    /** Remove [id]. Deleting the active vehicle promotes the first remaining one. */
    @Synchronized
    fun delete(id: String): List<VehicleProfile> {
        val all = VehicleData.delete(all(), id)
        val active = activeIdIn(all, prefs.getString(KEY_ACTIVE, null))
        write(all, active)
        publish()
        return all
    }

    /** Set the active vehicle. An unknown id is ignored. */
    @Synchronized
    fun setActive(id: String): List<VehicleProfile> {
        val all = all()
        val active = all.firstOrNull { it.id == id }?.id
            ?: return all
        write(all, active)
        publish()
        return all
    }

    private fun activeIdIn(all: List<VehicleProfile>, wanted: String?): String? =
        wanted?.takeIf { id -> all.any { it.id == id } } ?: all.firstOrNull()?.id

    private fun write(all: List<VehicleProfile>, activeId: String?) {
        prefs.edit()
            .putString(KEY_VEHICLES, VehicleData.encode(all))
            .putString(KEY_ACTIVE, activeId)
            .apply()
    }

    private fun publish() {
        val all = all()
        val id = activeIdIn(all, prefs.getString(KEY_ACTIVE, null))
        VehiclePrefs.publish(all, all.firstOrNull { it.id == id })
    }

    private companion object {
        const val PREFS = "vela_vehicles"
        const val KEY_VEHICLES = "vehicles"
        const val KEY_ACTIVE = "active_vehicle_id"
    }
}

/**
 * JSON and list rules, separated from [VehicleStore] so they run without an Android context and
 * the unit tests cover the parts that can lose a garage entry.
 */
internal object VehicleData {
    private val json = Json { ignoreUnknownKeys = true }

    fun decode(raw: String?): List<VehicleProfile> =
        runCatching { json.decodeFromString<List<VehicleProfile>>(raw ?: "[]") }
            .getOrDefault(emptyList())

    fun encode(all: List<VehicleProfile>): String = json.encodeToString(all)

    /** [incoming] placed first, trimmed, with its original creation time kept on an update. */
    fun upsert(
        current: List<VehicleProfile>,
        incoming: VehicleProfile,
        now: Long,
    ): List<VehicleProfile> {
        val existing = current.firstOrNull { it.id == incoming.id }
        val saved = incoming.copy(
            id = incoming.id.ifBlank { UUID.randomUUID().toString() },
            nickname = incoming.nickname.trim(),
            make = incoming.make.trim(),
            model = incoming.model.trim(),
            plate = incoming.plate.trim(),
            notes = incoming.notes.trim(),
            createdAt = existing?.createdAt?.takeIf { it > 0L } ?: now,
            updatedAt = now,
        )
        return listOf(saved) + current.filterNot { it.id == saved.id }
    }

    fun delete(current: List<VehicleProfile>, id: String): List<VehicleProfile> =
        current.filterNot { it.id == id }

    /** [wanted] when it still exists, else the first vehicle, else null. */
    fun activeAfter(current: List<VehicleProfile>, wanted: String?): String? =
        wanted?.takeIf { id -> current.any { it.id == id } } ?: current.firstOrNull()?.id
}
