package app.vela.ui.vehicle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.vela.R
import app.vela.core.ai.VehicleAiField
import app.vela.core.ai.VehicleAiIdentityField
import app.vela.core.model.ConsumptionStandard
import app.vela.core.model.Drivetrain
import app.vela.core.model.FuelType
import app.vela.core.model.Transmission
import app.vela.core.model.VehicleKind
import app.vela.core.model.VehicleProfile
import app.vela.core.model.VehicleRoutingMode
import app.vela.ui.dpadFieldEscape
import app.vela.ui.dpadHighlight
import app.vela.ui.settings.Hint
import app.vela.ui.settings.PageIntro
import app.vela.ui.settings.SettingsGroup
import app.vela.ui.settings.SettingsScaffold
import app.vela.ui.settings.settingsFieldColors

/** Everything the editor needs; [badges] and conflicts live only in this screen. */
internal data class VehicleEditorTarget(
    val draft: VehicleDraft,
    val isNew: Boolean,
    val badges: Set<VehicleAiField> = emptySet(),
    val conflicts: Set<VehicleAiIdentityField> = emptySet(),
    val redactedPlateCount: Int = 0,
)

@Composable
internal fun VehicleEditorScreen(
    target: VehicleEditorTarget,
    onBack: () -> Unit,
    onSave: (VehicleProfile) -> Unit,
) {
    var draft by remember(target.draft.id) { mutableStateOf(target.draft) }
    var badges by remember(target.draft.id) { mutableStateOf(target.badges) }
    val valid = draft.isValid()

    fun change(field: VehicleAiField?, transform: (VehicleDraft) -> VehicleDraft) {
        draft = transform(draft)
        badges = badgesAfterEdit(badges, field)
    }

    SettingsScaffold(
        stringResource(if (target.isNew) R.string.settings_vehicle_new_title else R.string.settings_vehicle_edit_title),
        onBack,
    ) { topRow ->
        Spacer(Modifier.height(4.dp))
        PageIntro(stringResource(R.string.settings_vehicle_editor_hint))
        conflictWarning(target.conflicts)
        plateRedactionWarning(target.redactedPlateCount)

        SettingsGroup(title = stringResource(R.string.settings_vehicle_identity)) {
            VehicleField(
                value = draft.make,
                onValueChange = { draft = draft.copy(make = it) },
                label = stringResource(R.string.settings_vehicle_make),
                modifier = topRow,
            )
            VehicleField(
                value = draft.model,
                onValueChange = { draft = draft.copy(model = it) },
                label = stringResource(R.string.settings_vehicle_model),
            )
            VehicleField(
                value = draft.variant,
                onValueChange = { change(VehicleAiField.VARIANT) { d -> d.copy(variant = it) } },
                label = stringResource(R.string.settings_vehicle_variant),
                badge = VehicleAiField.VARIANT in badges,
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
        SettingsGroup(title = groupTitle(stringResource(R.string.settings_vehicle_kind), VehicleAiField.KIND in badges)) {
            ChoiceChips(
                options = VehicleKind.entries.map { it to kindLabel(it) },
                selected = draft.kind,
                onSelected = { value -> change(VehicleAiField.KIND) { d -> d.copy(kind = value) } },
            )
        }

        Spacer(Modifier.height(8.dp))
        SettingsGroup(title = stringResource(R.string.settings_vehicle_fuel)) {
            ChoiceChips(
                options = FuelType.entries.map { it to fuelLabel(it) },
                selected = draft.fuelType,
                onSelected = { value -> draft = draft.copy(fuelType = value) },
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
        SettingsGroup(title = stringResource(R.string.settings_vehicle_technical)) {
            VehicleField(
                value = draft.engineDisplacementCc,
                onValueChange = { change(VehicleAiField.ENGINE_DISPLACEMENT_CC) { d -> d.copy(engineDisplacementCc = it) } },
                label = stringResource(R.string.settings_vehicle_engine_cc),
                numeric = true,
                integer = true,
                badge = VehicleAiField.ENGINE_DISPLACEMENT_CC in badges,
            )
            VehicleField(
                value = draft.powerKw,
                onValueChange = { change(VehicleAiField.POWER_KW) { d -> d.copy(powerKw = it) } },
                label = stringResource(R.string.settings_vehicle_power_kw),
                numeric = true,
                integer = true,
                badge = VehicleAiField.POWER_KW in badges,
            )
            VehicleField(
                value = draft.emissionStandard,
                onValueChange = { change(VehicleAiField.EMISSION_STANDARD) { d -> d.copy(emissionStandard = it) } },
                label = stringResource(R.string.settings_vehicle_emission_standard),
                badge = VehicleAiField.EMISSION_STANDARD in badges,
            )
            VehicleField(
                value = draft.maxSpeedKph,
                onValueChange = { change(VehicleAiField.MAX_SPEED_KPH) { d -> d.copy(maxSpeedKph = it) } },
                label = stringResource(R.string.settings_vehicle_max_speed),
                numeric = true,
                integer = true,
                badge = VehicleAiField.MAX_SPEED_KPH in badges,
            )
            SettingsGroupHeader(
                title = groupTitle(
                    stringResource(R.string.settings_vehicle_transmission),
                    VehicleAiField.TRANSMISSION in badges,
                ),
            )
            ChoiceChips(
                options = Transmission.entries.map { it to transmissionLabel(it) },
                selected = draft.transmission,
                onSelected = { value -> change(VehicleAiField.TRANSMISSION) { d -> d.copy(transmission = value) } },
            )
            SettingsGroupHeader(
                title = groupTitle(
                    stringResource(R.string.settings_vehicle_drivetrain),
                    VehicleAiField.DRIVETRAIN in badges,
                ),
            )
            ChoiceChips(
                options = Drivetrain.entries.map { it to drivetrainLabel(it) },
                selected = draft.drivetrain,
                onSelected = { value -> change(VehicleAiField.DRIVETRAIN) { d -> d.copy(drivetrain = value) } },
            )
        }

        Spacer(Modifier.height(8.dp))
        SettingsGroup(title = stringResource(R.string.settings_vehicle_dimensions)) {
            VehicleField(
                value = draft.heightM,
                onValueChange = { change(VehicleAiField.HEIGHT_M) { d -> d.copy(heightM = it) } },
                label = stringResource(R.string.settings_vehicle_height),
                numeric = true,
                badge = VehicleAiField.HEIGHT_M in badges,
            )
            VehicleField(
                value = draft.widthM,
                onValueChange = { change(VehicleAiField.WIDTH_M) { d -> d.copy(widthM = it) } },
                label = stringResource(R.string.settings_vehicle_width),
                numeric = true,
                badge = VehicleAiField.WIDTH_M in badges,
            )
            VehicleField(
                value = draft.widthWithMirrorsM,
                onValueChange = { change(VehicleAiField.WIDTH_WITH_MIRRORS_M) { d -> d.copy(widthWithMirrorsM = it) } },
                label = stringResource(R.string.settings_vehicle_width_mirrors),
                numeric = true,
                badge = VehicleAiField.WIDTH_WITH_MIRRORS_M in badges,
            )
            VehicleField(
                value = draft.lengthM,
                onValueChange = { change(VehicleAiField.LENGTH_M) { d -> d.copy(lengthM = it) } },
                label = stringResource(R.string.settings_vehicle_length),
                numeric = true,
                badge = VehicleAiField.LENGTH_M in badges,
            )
            VehicleField(
                value = draft.weightKg,
                onValueChange = { change(VehicleAiField.WEIGHT_KG) { d -> d.copy(weightKg = it) } },
                label = stringResource(R.string.settings_vehicle_weight),
                numeric = true,
                integer = true,
                badge = VehicleAiField.WEIGHT_KG in badges,
            )
            VehicleField(
                value = draft.grossWeightKg,
                onValueChange = { change(VehicleAiField.GROSS_WEIGHT_KG) { d -> d.copy(grossWeightKg = it) } },
                label = stringResource(R.string.settings_vehicle_gross_weight),
                numeric = true,
                integer = true,
                badge = VehicleAiField.GROSS_WEIGHT_KG in badges,
            )
            VehicleField(
                value = draft.axleLoadKg,
                onValueChange = { change(VehicleAiField.AXLE_LOAD_KG) { d -> d.copy(axleLoadKg = it) } },
                label = stringResource(R.string.settings_vehicle_axle_load),
                numeric = true,
                integer = true,
                badge = VehicleAiField.AXLE_LOAD_KG in badges,
            )
            VehicleField(
                value = draft.axleCount,
                onValueChange = { change(VehicleAiField.AXLE_COUNT) { d -> d.copy(axleCount = it) } },
                label = stringResource(R.string.settings_vehicle_axles),
                numeric = true,
                integer = true,
                badge = VehicleAiField.AXLE_COUNT in badges,
            )
        }

        Spacer(Modifier.height(8.dp))
        SettingsGroup(title = stringResource(R.string.settings_vehicle_consumption)) {
            VehicleField(
                value = draft.consumptionL100km,
                onValueChange = { change(VehicleAiField.CONSUMPTION_L100KM) { d -> d.copy(consumptionL100km = it) } },
                label = stringResource(R.string.settings_vehicle_consumption_l100),
                numeric = true,
                badge = VehicleAiField.CONSUMPTION_L100KM in badges,
            )
            VehicleField(
                value = draft.energyKwh100km,
                onValueChange = { change(VehicleAiField.ENERGY_KWH100KM) { d -> d.copy(energyKwh100km = it) } },
                label = stringResource(R.string.settings_vehicle_consumption_kwh),
                numeric = true,
                badge = VehicleAiField.ENERGY_KWH100KM in badges,
            )
            VehicleField(
                value = draft.batteryKwh,
                onValueChange = { change(VehicleAiField.BATTERY_KWH) { d -> d.copy(batteryKwh = it) } },
                label = stringResource(R.string.settings_vehicle_battery),
                numeric = true,
                badge = VehicleAiField.BATTERY_KWH in badges,
            )
            VehicleField(
                value = draft.tankL,
                onValueChange = { change(VehicleAiField.TANK_L) { d -> d.copy(tankL = it) } },
                label = stringResource(R.string.settings_vehicle_tank),
                numeric = true,
                badge = VehicleAiField.TANK_L in badges,
            )
            SettingsGroupHeader(
                title = groupTitle(
                    stringResource(R.string.settings_vehicle_consumption_standard),
                    VehicleAiField.CONSUMPTION_STANDARD in badges,
                ),
            )
            ChoiceChips(
                options = ConsumptionStandard.entries.map { it to consumptionStandardLabel(it) },
                selected = draft.consumptionStandard,
                onSelected = { value ->
                    change(VehicleAiField.CONSUMPTION_STANDARD) { d -> d.copy(consumptionStandard = value) }
                },
            )
        }

        Spacer(Modifier.height(8.dp))
        SettingsGroup(title = stringResource(R.string.settings_vehicle_energy)) {
            VehicleField(
                value = draft.maxChargingPowerKw,
                onValueChange = { change(VehicleAiField.MAX_CHARGING_POWER_KW) { d -> d.copy(maxChargingPowerKw = it) } },
                label = stringResource(R.string.settings_vehicle_max_charging),
                numeric = true,
                badge = VehicleAiField.MAX_CHARGING_POWER_KW in badges,
            )
            VehicleField(
                value = draft.chargingConnector,
                onValueChange = { change(VehicleAiField.CHARGING_CONNECTOR) { d -> d.copy(chargingConnector = it) } },
                label = stringResource(R.string.settings_vehicle_connector),
                placeholder = stringResource(R.string.settings_vehicle_connector_hint),
                badge = VehicleAiField.CHARGING_CONNECTOR in badges,
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

/** A group heading inside an existing [SettingsGroup] (the enum chip fields). */
@Composable
internal fun SettingsGroupHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 2.dp),
    )
}

@Composable
internal fun groupTitle(title: String, badge: Boolean): String =
    if (badge) "$title · ${stringResource(R.string.settings_vehicle_ai_badge)}" else title

@Composable
internal fun conflictWarning(conflicts: Set<VehicleAiIdentityField>) {
    if (conflicts.isEmpty()) return
    val makeLabel = stringResource(R.string.settings_vehicle_make)
    val modelLabel = stringResource(R.string.settings_vehicle_model)
    val yearLabel = stringResource(R.string.settings_vehicle_year)
    val fuelLabelText = stringResource(R.string.settings_vehicle_fuel)
    val labels = conflicts.sortedBy { it.ordinal }.map { field ->
        when (field) {
            VehicleAiIdentityField.MAKE -> makeLabel
            VehicleAiIdentityField.MODEL -> modelLabel
            VehicleAiIdentityField.YEAR -> yearLabel
            VehicleAiIdentityField.FUEL_TYPE -> fuelLabelText
        }
    }.joinToString(", ")
    Text(
        stringResource(R.string.settings_vehicle_ai_conflict, labels),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
internal fun plateRedactionWarning(count: Int) {
    if (count > 0) Hint(stringResource(R.string.settings_vehicle_ai_plate_removed, count))
}

@Composable
internal fun VehicleField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    numeric: Boolean = false,
    integer: Boolean = false,
    singleLine: Boolean = true,
    badge: Boolean = false,
) {
    val error = numeric && value.isNotBlank() && if (integer) parseInt(value) == null else parseDouble(value) == null
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        trailingIcon = if (badge) {
            {
                Text(
                    stringResource(R.string.settings_vehicle_ai_badge),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        } else null,
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
internal fun <T> ChoiceChips(
    options: List<Pair<T, String>>,
    selected: T?,
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
