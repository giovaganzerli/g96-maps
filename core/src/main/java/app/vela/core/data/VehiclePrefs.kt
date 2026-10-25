package app.vela.core.data

import app.vela.core.model.VehicleProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The active vehicle and the whole garage, published for surfaces that cannot take another
 * callback through `MapScreen` (the route pickers read this directly). [VehicleStore] is the only
 * writer.
 */
object VehiclePrefs {
    private val _vehicles = MutableStateFlow<List<VehicleProfile>>(emptyList())
    val vehicles: StateFlow<List<VehicleProfile>> = _vehicles.asStateFlow()

    private val _active = MutableStateFlow<VehicleProfile?>(null)
    val active: StateFlow<VehicleProfile?> = _active.asStateFlow()

    internal fun publish(vehicles: List<VehicleProfile>, active: VehicleProfile?) {
        _vehicles.value = vehicles
        _active.value = active
    }
}
