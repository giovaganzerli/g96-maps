package app.vela.ui.settings.sections

import app.vela.ui.icons.Sym

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vela.R
import app.vela.core.data.VehiclePrefs
import app.vela.core.model.FuelType
import app.vela.core.model.VehicleKind
import app.vela.core.model.VehicleProfile
import app.vela.core.model.VehicleRoutingMode
import app.vela.ui.VelaDialog
import app.vela.ui.VelaMenu
import app.vela.ui.dpadClickable
import app.vela.ui.dpadFieldEscape
import app.vela.ui.dpadHighlight
import app.vela.ui.item
import app.vela.ui.map.MapViewModel
import app.vela.ui.settings.GroupDivider
import app.vela.ui.settings.Hint
import app.vela.ui.settings.PageIntro
import app.vela.ui.settings.SettingsGroup
import app.vela.ui.settings.SettingsScaffold
import app.vela.ui.settings.settingsFieldColors
import java.util.Locale
import java.util.UUID

/** The local garage, its active selection, and the manual editor. */
@Composable
internal fun VehiclesSettingsScreen(vm: MapViewModel, onBack: () -> Unit) {
    val vehicles by VehiclePrefs.vehicles.collectAsStateWithLifecycle()
    val active by VehiclePrefs.active.collectAsStateWithLifecycle()
    var editor by remember { mutableStateOf<VehicleEditorTarget?>(null) }
    var deleting by remember { mutableStateOf<VehicleProfile?>(null) }

    editor?.let { target ->
        VehicleEditorScreen(
            initial = target.profile,
            isNew = target.isNew,
            onBack = { editor = null },
            onSave = {
                vm.saveVehicle(it)
                editor = null
            },
        )
        return
    }

    SettingsScaffold(stringResource(R.string.settings_vehicles), onBack) { topRow ->
        Spacer(Modifier.height(4.dp))
        PageIntro(stringResource(R.string.settings_vehicles_hint))
        SettingsGroup(title = stringResource(R.string.settings_vehicles)) {
            if (vehicles.isEmpty()) {
                Hint(stringResource(R.string.settings_vehicles_empty))
            } else {
                vehicles.forEachIndexed { index, vehicle ->
                    if (index > 0) GroupDivider()
                    VehicleRow(
                        vehicle = vehicle,
                        activeId = active?.id,
                        modifier = if (index == 0) topRow else Modifier,
                        onActivate = { vm.setActiveVehicle(vehicle.id) },
                        onEdit = { editor = VehicleEditorTarget(vehicle, isNew = false) },
                        onDelete = { deleting = vehicle },
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        FilledTonalButton(
            onClick = { editor = VehicleEditorTarget(VehicleDraft.newProfile(), isNew = true) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .then(if (vehicles.isEmpty()) topRow else Modifier),
        ) {
            Icon(Sym.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.settings_vehicle_add))
        }
        Hint(stringResource(R.string.settings_vehicle_plate_privacy))
        Spacer(Modifier.height(24.dp))
    }

    deleting?.let { vehicle ->
        VelaDialog(
            onDismissRequest = { deleting = null },
            title = stringResource(R.string.settings_vehicle_delete_title),
            confirmText = stringResource(R.string.settings_vehicle_delete_confirm),
            onConfirm = {
                vm.deleteVehicle(vehicle.id)
                deleting = null
            },
            dismissText = stringResource(R.string.list_cancel),
            onDismiss = { deleting = null },
        ) {
            Text(stringResource(R.string.settings_vehicle_delete_body, vehicle.displayName))
        }
    }
}

private data class VehicleEditorTarget(val profile: VehicleProfile, val isNew: Boolean)

@Composable
private fun VehicleRow(
    vehicle: VehicleProfile,
    activeId: String?,
    modifier: Modifier,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember(vehicle.id) { mutableStateOf(false) }
    Row(
        modifier
            .fillMaxWidth()
            .dpadHighlight(RoundedCornerShape(16.dp))
            .dpadClickable(onClick = onActivate)
            .padding(start = 16.dp, end = 4.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = vehicle.id == activeId, onClick = null)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(vehicle.displayName, style = MaterialTheme.typography.bodyLarge)
                if (vehicle.id == activeId) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.settings_vehicle_active),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(
                vehicleSummary(vehicle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box {
            IconButton(
                onClick = { menu = true },
                modifier = Modifier.dpadHighlight(CircleShape),
            ) {
                Icon(Sym.MoreVert, contentDescription = stringResource(R.string.exp_chooser_more))
            }
            VelaMenu(expanded = menu, onDismissRequest = { menu = false }) {
                item(stringResource(R.string.settings_vehicle_edit)) { menu = false; onEdit() }
                item(stringResource(R.string.settings_vehicle_delete)) { menu = false; onDelete() }
            }
        }
    }
}

@Composable
private fun vehicleSummary(vehicle: VehicleProfile): String =
    listOfNotNull(
        vehicle.year?.toString(),
        fuelLabel(vehicle.fuelType),
        kindLabel(vehicle.kind),
    ).joinToString(" · ")

@Composable
private fun fuelLabel(fuel: FuelType): String = stringResource(
    when (fuel) {
        FuelType.PETROL -> R.string.settings_vehicle_fuel_petrol
        FuelType.DIESEL -> R.string.settings_vehicle_fuel_diesel
        FuelType.LPG -> R.string.settings_vehicle_fuel_lpg
        FuelType.CNG -> R.string.settings_vehicle_fuel_cng
        FuelType.HYBRID -> R.string.settings_vehicle_fuel_hybrid
        FuelType.PLUG_IN_HYBRID -> R.string.settings_vehicle_fuel_plugin
        FuelType.ELECTRIC -> R.string.settings_vehicle_fuel_electric
        FuelType.HYDROGEN -> R.string.settings_vehicle_fuel_hydrogen
        FuelType.OTHER -> R.string.settings_vehicle_fuel_other
    },
)

@Composable
private fun kindLabel(kind: VehicleKind): String = stringResource(
    when (kind) {
        VehicleKind.CAR -> R.string.settings_vehicle_kind_car
        VehicleKind.VAN -> R.string.settings_vehicle_kind_van
        VehicleKind.CAMPER -> R.string.settings_vehicle_kind_camper
        VehicleKind.TRUCK -> R.string.settings_vehicle_kind_truck
        VehicleKind.OTHER -> R.string.settings_vehicle_kind_other
    },
)

@Composable
private fun VehicleEditorScreen(
    initial: VehicleProfile,
    isNew: Boolean,
    onBack: () -> Unit,
    onSave: (VehicleProfile) -> Unit,
) {
    var draft by remember(initial.id) { mutableStateOf(VehicleDraft.from(initial)) }
    val valid = draft.isValid()
    SettingsScaffold(
        stringResource(if (isNew) R.string.settings_vehicle_new_title else R.string.settings_vehicle_edit_title),
        onBack,
    ) { topRow ->
        Spacer(Modifier.height(4.dp))
        PageIntro(stringResource(R.string.settings_vehicle_editor_hint))
        SettingsGroup(title = stringResource(R.string.settings_vehicle_identity)) {
            VehicleField(
                value = draft.nickname,
                onValueChange = { draft = draft.copy(nickname = it) },
                label = stringResource(R.string.settings_vehicle_name),
                placeholder = stringResource(R.string.settings_vehicle_name_hint),
                modifier = topRow,
            )
            VehicleField(
                value = draft.make,
                onValueChange = { draft = draft.copy(make = it) },
                label = stringResource(R.string.settings_vehicle_make),
            )
            VehicleField(
                value = draft.model,
                onValueChange = { draft = draft.copy(model = it) },
                label = stringResource(R.string.settings_vehicle_model),
            )
            VehicleField(
                value = draft.year,
                onValueChange = { draft = draft.copy(year = it) },
                label = stringResource(R.string.settings_vehicle_year),
                numeric = true,
                integer = true,
            )
            VehicleField(
                value = draft.plate,
                onValueChange = { draft = draft.copy(plate = it) },
                label = stringResource(R.string.settings_vehicle_plate),
                placeholder = stringResource(R.string.settings_vehicle_plate_hint),
            )
        }

        Spacer(Modifier.height(8.dp))
        SettingsGroup(title = stringResource(R.string.settings_vehicle_kind)) {
            ChoiceChips(
                options = VehicleKind.entries.map { it to kindLabel(it) },
                selected = draft.kind,
                onSelected = { draft = draft.copy(kind = it) },
            )
        }

        Spacer(Modifier.height(8.dp))
        SettingsGroup(title = stringResource(R.string.settings_vehicle_fuel)) {
            ChoiceChips(
                options = FuelType.entries.map { it to fuelLabel(it) },
                selected = draft.fuelType,
                onSelected = { draft = draft.copy(fuelType = it) },
            )
        }

        Spacer(Modifier.height(8.dp))
        SettingsGroup(title = stringResource(R.string.settings_vehicle_routing)) {
            ChoiceChips(
                options = listOf(
                    VehicleRoutingMode.AUTO to stringResource(R.string.settings_vehicle_routing_auto),
                    VehicleRoutingMode.TRUCK to stringResource(R.string.settings_vehicle_routing_truck),
                ),
                selected = draft.routingMode,
                onSelected = { draft = draft.copy(routingMode = it) },
            )
            Hint(stringResource(R.string.settings_vehicle_routing_hint))
        }

        Spacer(Modifier.height(8.dp))
        SettingsGroup(title = stringResource(R.string.settings_vehicle_dimensions)) {
            VehicleField(
                value = draft.height,
                onValueChange = { draft = draft.copy(height = it) },
                label = stringResource(R.string.settings_vehicle_height),
                numeric = true,
            )
            VehicleField(
                value = draft.width,
                onValueChange = { draft = draft.copy(width = it) },
                label = stringResource(R.string.settings_vehicle_width),
                numeric = true,
            )
            VehicleField(
                value = draft.widthMirrors,
                onValueChange = { draft = draft.copy(widthMirrors = it) },
                label = stringResource(R.string.settings_vehicle_width_mirrors),
                numeric = true,
            )
            VehicleField(
                value = draft.length,
                onValueChange = { draft = draft.copy(length = it) },
                label = stringResource(R.string.settings_vehicle_length),
                numeric = true,
            )
            VehicleField(
                value = draft.weight,
                onValueChange = { draft = draft.copy(weight = it) },
                label = stringResource(R.string.settings_vehicle_weight),
                numeric = true,
                integer = true,
            )
            VehicleField(
                value = draft.grossWeight,
                onValueChange = { draft = draft.copy(grossWeight = it) },
                label = stringResource(R.string.settings_vehicle_gross_weight),
                numeric = true,
                integer = true,
            )
            VehicleField(
                value = draft.axleLoad,
                onValueChange = { draft = draft.copy(axleLoad = it) },
                label = stringResource(R.string.settings_vehicle_axle_load),
                numeric = true,
                integer = true,
            )
            VehicleField(
                value = draft.axles,
                onValueChange = { draft = draft.copy(axles = it) },
                label = stringResource(R.string.settings_vehicle_axles),
                numeric = true,
                integer = true,
            )
        }

        Spacer(Modifier.height(8.dp))
        SettingsGroup(title = stringResource(R.string.settings_vehicle_consumption)) {
            VehicleField(
                value = draft.consumption,
                onValueChange = { draft = draft.copy(consumption = it) },
                label = stringResource(R.string.settings_vehicle_consumption_l100),
                numeric = true,
            )
            VehicleField(
                value = draft.energy,
                onValueChange = { draft = draft.copy(energy = it) },
                label = stringResource(R.string.settings_vehicle_consumption_kwh),
                numeric = true,
            )
            VehicleField(
                value = draft.battery,
                onValueChange = { draft = draft.copy(battery = it) },
                label = stringResource(R.string.settings_vehicle_battery),
                numeric = true,
            )
            VehicleField(
                value = draft.tank,
                onValueChange = { draft = draft.copy(tank = it) },
                label = stringResource(R.string.settings_vehicle_tank),
                numeric = true,
            )
        }

        Spacer(Modifier.height(8.dp))
        SettingsGroup(title = stringResource(R.string.settings_vehicle_notes)) {
            VehicleField(
                value = draft.notes,
                onValueChange = { draft = draft.copy(notes = it) },
                label = stringResource(R.string.settings_vehicle_notes),
                singleLine = false,
            )
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onSave(draft.toProfile()) },
            enabled = valid,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            Text(stringResource(R.string.settings_vehicle_save))
        }
        if (!valid) Hint(stringResource(R.string.settings_vehicle_required))
        Hint(stringResource(R.string.settings_vehicle_plate_privacy))
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun VehicleField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    numeric: Boolean = false,
    integer: Boolean = false,
    singleLine: Boolean = true,
) {
    val error = numeric && value.isNotBlank() && if (integer) parseInt(value) == null else parseDouble(value) == null
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).dpadFieldEscape(),
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        isError = error,
        supportingText = if (error) {
            { Text(stringResource(R.string.settings_vehicle_invalid_number)) }
        } else {
            null
        },
        shape = RoundedCornerShape(20.dp),
        colors = settingsFieldColors(),
        keyboardOptions = KeyboardOptions(
            keyboardType = when {
                integer -> KeyboardType.Number
                numeric -> KeyboardType.Decimal
                else -> KeyboardType.Text
            },
        ),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChoiceChips(
    options: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelected(value) },
                label = { Text(label) },
                shape = CircleShape,
                modifier = Modifier.dpadHighlight(CircleShape),
            )
        }
    }
}

private data class VehicleDraft(
    val id: String,
    val nickname: String,
    val make: String,
    val model: String,
    val year: String,
    val plate: String,
    val kind: VehicleKind,
    val fuelType: FuelType,
    val routingMode: VehicleRoutingMode,
    val height: String,
    val width: String,
    val widthMirrors: String,
    val length: String,
    val weight: String,
    val grossWeight: String,
    val axleLoad: String,
    val axles: String,
    val consumption: String,
    val energy: String,
    val battery: String,
    val tank: String,
    val notes: String,
    val createdAt: Long,
) {
    fun isValid(): Boolean =
        make.isNotBlank() && model.isNotBlank() &&
            listOf(year, weight, grossWeight, axleLoad, axles).all { it.isBlank() || parseInt(it) != null } &&
            listOf(height, width, widthMirrors, length, consumption, energy, battery, tank)
                .all { it.isBlank() || parseDouble(it) != null }

    fun toProfile(): VehicleProfile = VehicleProfile(
        id = id,
        nickname = nickname.trim(),
        kind = kind,
        make = make.trim(),
        model = model.trim(),
        year = parseInt(year),
        fuelType = fuelType,
        routingMode = routingMode,
        plate = plate.trim(),
        heightM = parseDouble(height),
        widthM = parseDouble(width),
        widthWithMirrorsM = parseDouble(widthMirrors),
        lengthM = parseDouble(length),
        weightKg = parseInt(weight),
        grossWeightKg = parseInt(grossWeight),
        axleLoadKg = parseInt(axleLoad),
        axleCount = parseInt(axles),
        consumptionL100km = parseDouble(consumption),
        energyKwh100km = parseDouble(energy),
        batteryKwh = parseDouble(battery),
        tankL = parseDouble(tank),
        notes = notes.trim(),
        createdAt = createdAt,
    )

    companion object {
        fun newProfile(): VehicleProfile = VehicleProfile(id = UUID.randomUUID().toString())

        fun from(profile: VehicleProfile): VehicleDraft = VehicleDraft(
            id = profile.id,
            nickname = profile.nickname,
            make = profile.make,
            model = profile.model,
            year = profile.year?.toString().orEmpty(),
            plate = profile.plate,
            kind = profile.kind,
            fuelType = profile.fuelType,
            routingMode = profile.routingMode,
            height = formatNumber(profile.heightM),
            width = formatNumber(profile.widthM),
            widthMirrors = formatNumber(profile.widthWithMirrorsM),
            length = formatNumber(profile.lengthM),
            weight = profile.weightKg?.toString().orEmpty(),
            grossWeight = profile.grossWeightKg?.toString().orEmpty(),
            axleLoad = profile.axleLoadKg?.toString().orEmpty(),
            axles = profile.axleCount?.toString().orEmpty(),
            consumption = formatNumber(profile.consumptionL100km),
            energy = formatNumber(profile.energyKwh100km),
            battery = formatNumber(profile.batteryKwh),
            tank = formatNumber(profile.tankL),
            notes = profile.notes,
            createdAt = profile.createdAt,
        )
    }
}

private fun parseDouble(raw: String): Double? =
    raw.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0.0 }

private fun parseInt(raw: String): Int? =
    raw.trim().toIntOrNull()?.takeIf { it >= 0 }

private fun formatNumber(value: Double?): String {
    if (value == null) return ""
    return if (value % 1.0 == 0.0) value.toLong().toString()
    else String.format(Locale.getDefault(), "%.2f", value).trimEnd('0').trimEnd('.')
}
