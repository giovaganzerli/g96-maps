package app.vela.ui.vehicle

import app.vela.ui.icons.Sym
import app.vela.ui.icons.SymOutlined

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vela.R
import app.vela.core.data.VehiclePrefs
import app.vela.ui.VehicleActions
import app.vela.ui.VelaMenu
import app.vela.ui.dpadHighlight
import app.vela.ui.item

/**
 * The active vehicle in a route picker. Tapping it offers every garage entry; choosing one writes
 * through [VehicleActions], so the picker updates without taking a new `MapScreen` parameter.
 * Nothing is drawn while the garage is empty.
 */
@Composable
fun VehicleChip(modifier: Modifier = Modifier) {
    val vehicles by VehiclePrefs.vehicles.collectAsStateWithLifecycle()
    val active by VehiclePrefs.active.collectAsStateWithLifecycle()
    if (vehicles.isEmpty()) return

    var menu by remember { mutableStateOf(false) }
    Box(modifier) {
        AssistChip(
            onClick = { menu = true },
            label = { Text(active?.displayName ?: stringResource(R.string.settings_vehicle_choose)) },
            leadingIcon = {
                Icon(
                    SymOutlined.DirectionsCar,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
            shape = CircleShape,
            modifier = Modifier.dpadHighlight(CircleShape),
        )
        VelaMenu(expanded = menu, onDismissRequest = { menu = false }) {
            vehicles.forEach { vehicle ->
                item(
                    text = vehicle.displayName,
                    icon = if (vehicle.id == active?.id) Sym.Check else null,
                ) {
                    menu = false
                    VehicleActions.setActive?.invoke(vehicle.id)
                }
            }
        }
    }
}
