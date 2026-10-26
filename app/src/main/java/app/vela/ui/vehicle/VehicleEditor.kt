package app.vela.ui.vehicle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.vela.R
import app.vela.core.ai.AiConsumptionOutcome
import app.vela.core.ai.AiProvider
import app.vela.core.ai.VehicleAiField
import app.vela.core.ai.VehicleAiIdentityField
import app.vela.core.ai.VehicleConsumptionInput
import app.vela.core.model.Drivetrain
import app.vela.core.model.FuelType
import app.vela.core.model.Transmission
import app.vela.core.model.VehicleKind
import app.vela.core.model.VehicleProfile
import app.vela.ui.dpadFieldEscape
import app.vela.ui.dpadHighlight
import app.vela.ui.settings.Hint
import app.vela.ui.settings.PageIntro
import app.vela.ui.settings.SettingsGroup
import app.vela.ui.settings.SettingsScaffold
import app.vela.ui.settings.settingsFieldColors
import kotlinx.coroutines.launch

/** Everything the editor needs; [badges] and conflicts live only in this screen. */
internal data class VehicleEditorTarget(
    val draft: VehicleDraft,
    val isNew: Boolean,
    val badges: Set<VehicleAiField> = emptySet(),
    val conflicts: Set<VehicleAiIdentityField> = emptySet(),
    val redactedPlateCount: Int = 0,
)

private sealed interface ConsumptionRecalcState {
    data object Idle : ConsumptionRecalcState
    data object Running : ConsumptionRecalcState
    data class Done(val provider: AiProvider, val redactedPlateCount: Int) : ConsumptionRecalcState
    data object Outdated : ConsumptionRecalcState
    data class Failed(val outcome: AiConsumptionOutcome) : ConsumptionRecalcState
}

@Composable
internal fun VehicleEditorScreen(
    target: VehicleEditorTarget,
    onBack: () -> Unit,
    onSave: (VehicleProfile) -> Unit,
    onRecalculateConsumption: suspend (VehicleConsumptionInput) -> AiConsumptionOutcome,
) {
    var draft by remember(target.draft.id) { mutableStateOf(target.draft) }
    var badges by remember(target.draft.id) { mutableStateOf(target.badges) }
    var referenceWeightKg by remember(target.draft.id) {
        mutableStateOf(parseInt(target.draft.weightKg)?.takeIf { it in 1..100_000 })
    }
    var recalcState by remember(target.draft.id) {
        mutableStateOf<ConsumptionRecalcState>(ConsumptionRecalcState.Idle)
    }
    val scope = rememberCoroutineScope()
    val recalcInput = draft.consumptionInput(referenceWeightKg)
    val recalcRunning = recalcState is ConsumptionRecalcState.Running
    val valid = draft.isValid()

    fun change(field: VehicleAiField?, transform: (VehicleDraft) -> VehicleDraft) {
        draft = transform(draft)
        badges = badgesAfterEdit(badges, field)
    }

    fun changeFuel(fuel: FuelType) {
        if (recalcState !is ConsumptionRecalcState.Running) {
            recalcState = ConsumptionRecalcState.Idle
        }
        val cleared = draft.clearedByFuel(fuel)
        draft = draft.withFuel(fuel)
        badges = badges - cleared
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
                label = requiredLabel(stringResource(R.string.settings_vehicle_make)),
                modifier = topRow,
            )
            VehicleField(
                value = draft.model,
                onValueChange = { draft = draft.copy(model = it) },
                label = requiredLabel(stringResource(R.string.settings_vehicle_model)),
            )
            VehicleField(
                value = draft.variant,
                onValueChange = { change(VehicleAiField.VARIANT) { d -> d.copy(variant = it) } },
                label = stringResource(R.string.settings_vehicle_variant),
                placeholder = stringResource(R.string.settings_vehicle_variant_hint),
                badge = VehicleAiField.VARIANT in badges,
            )
            VehicleField(
                value = draft.year,
                onValueChange = { draft = draft.copy(year = it) },
                label = requiredLabel(stringResource(R.string.settings_vehicle_year)),
                numeric = true,
                integer = true,
            )
            VehicleField(
                value = draft.plate,
                onValueChange = { draft = draft.copy(plate = it) },
                label = stringResource(R.string.settings_vehicle_plate),
                placeholder = stringResource(R.string.settings_vehicle_plate_hint),
            )
            SettingsGroupHeader(
                title = groupTitle(
                    requiredLabel(stringResource(R.string.settings_vehicle_kind)),
                    VehicleAiField.KIND in badges,
                ),
            )
            ChoiceChips(
                options = kindOptions(draft.kind),
                selected = draft.kind,
                onSelected = { value -> change(VehicleAiField.KIND) { d -> d.copy(kind = value) } },
            )
        }

        Spacer(Modifier.height(8.dp))
        SettingsGroup(title = stringResource(R.string.settings_vehicle_technical)) {
            val fuel = draft.fuelType
            if (fuel == null || fuel.usesEngineDisplacement()) {
                VehicleField(
                    value = draft.engineDisplacementCc,
                    onValueChange = { change(VehicleAiField.ENGINE_DISPLACEMENT_CC) { d -> d.copy(engineDisplacementCc = it) } },
                    label = stringResource(R.string.settings_vehicle_engine_cc),
                    numeric = true,
                    integer = true,
                    badge = VehicleAiField.ENGINE_DISPLACEMENT_CC in badges,
                )
            }
            VehicleField(
                value = draft.powerKw,
                onValueChange = { change(VehicleAiField.POWER_KW) { d -> d.copy(powerKw = it) } },
                label = stringResource(R.string.settings_vehicle_power_kw),
                numeric = true,
                integer = true,
                badge = VehicleAiField.POWER_KW in badges,
            )
            SettingsGroupHeader(
                title = requiredLabel(stringResource(R.string.settings_vehicle_fuel)),
            )
            ChoiceChips(
                options = fuelOptions(draft.fuelType),
                selected = draft.fuelType,
                onSelected = { fuel -> changeFuel(fuel) },
            )
            if (fuel == null || fuel.usesEmissionStandard()) {
                SettingsGroupHeader(
                    title = groupTitle(
                        stringResource(R.string.settings_vehicle_emission_standard),
                        VehicleAiField.EMISSION_STANDARD in badges,
                    ),
                )
                ChoiceChips(
                    options = emissionOptions(draft.emissionStandard),
                    selected = draft.emissionStandard,
                    onSelected = { value ->
                        change(VehicleAiField.EMISSION_STANDARD) { d -> d.copy(emissionStandard = value) }
                    },
                )
            }
            SettingsGroupHeader(
                title = groupTitle(
                    stringResource(R.string.settings_vehicle_transmission),
                    VehicleAiField.TRANSMISSION in badges,
                ),
            )
            ChoiceChips(
                options = listOf(
                    Transmission.MANUAL to transmissionLabel(Transmission.MANUAL),
                    Transmission.AUTOMATIC to transmissionLabel(Transmission.AUTOMATIC),
                    Transmission.UNKNOWN to transmissionLabel(Transmission.UNKNOWN),
                ),
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
                options = listOf(
                    Drivetrain.FWD to drivetrainLabel(Drivetrain.FWD),
                    Drivetrain.RWD to drivetrainLabel(Drivetrain.RWD),
                    Drivetrain.AWD to drivetrainLabel(Drivetrain.AWD),
                    Drivetrain.UNKNOWN to drivetrainLabel(Drivetrain.UNKNOWN),
                ),
                selected = draft.drivetrain,
                onSelected = { value -> change(VehicleAiField.DRIVETRAIN) { d -> d.copy(drivetrain = value) } },
            )
        }

        Spacer(Modifier.height(8.dp))
        SettingsGroup(title = stringResource(R.string.settings_vehicle_dimensions)) {
            VehicleField(
                value = draft.lengthM,
                onValueChange = { change(VehicleAiField.LENGTH_M) { d -> d.copy(lengthM = it) } },
                label = stringResource(R.string.settings_vehicle_length),
                numeric = true,
                badge = VehicleAiField.LENGTH_M in badges,
            )
            VehicleField(
                value = draft.widthM,
                onValueChange = { change(VehicleAiField.WIDTH_M) { d -> d.copy(widthM = it) } },
                label = stringResource(R.string.settings_vehicle_width),
                numeric = true,
                badge = VehicleAiField.WIDTH_M in badges,
            )
            VehicleField(
                value = draft.heightM,
                onValueChange = { change(VehicleAiField.HEIGHT_M) { d -> d.copy(heightM = it) } },
                label = stringResource(R.string.settings_vehicle_height),
                numeric = true,
                badge = VehicleAiField.HEIGHT_M in badges,
            )
            VehicleField(
                value = draft.weightKg,
                onValueChange = {
                    if (recalcState !is ConsumptionRecalcState.Running) {
                        recalcState = ConsumptionRecalcState.Idle
                    }
                    change(VehicleAiField.WEIGHT_KG) { d -> d.copy(weightKg = it) }
                },
                label = stringResource(R.string.settings_vehicle_weight),
                numeric = true,
                integer = true,
                badge = VehicleAiField.WEIGHT_KG in badges,
            )
            if (draft.fuelType != null) {
                FilledTonalButton(
                    onClick = {
                        val input = draft.consumptionInput(referenceWeightKg)
                            ?: return@FilledTonalButton
                        recalcState = ConsumptionRecalcState.Running
                        scope.launch {
                            val outcome = onRecalculateConsumption(input)
                            if (outcome is AiConsumptionOutcome.Success) {
                                if (draft.consumptionInput(referenceWeightKg) != input) {
                                    recalcState = ConsumptionRecalcState.Outdated
                                } else {
                                    val applied = draft.withRecalculatedConsumption(outcome.consumption)
                                    if (applied.badges.isEmpty()) {
                                        recalcState = ConsumptionRecalcState.Failed(
                                            AiConsumptionOutcome.NoValues(outcome.provider),
                                        )
                                    } else {
                                        draft = applied.draft
                                        badges = badges + applied.badges
                                        referenceWeightKg = input.weightKg
                                        recalcState = ConsumptionRecalcState.Done(
                                            provider = outcome.provider,
                                            redactedPlateCount = outcome.redactedPlateCount,
                                        )
                                    }
                                }
                            } else {
                                recalcState = ConsumptionRecalcState.Failed(outcome)
                            }
                        }
                    },
                    enabled = recalcInput != null && !recalcRunning,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    if (recalcRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_vehicle_recalculate_running))
                    } else {
                        Text(stringResource(R.string.settings_vehicle_recalculate))
                    }
                }
                Hint(stringResource(R.string.settings_vehicle_recalculate_hint))
                RecalculationMessage(recalcState)
            }
        }

        Spacer(Modifier.height(8.dp))
        SettingsGroup(title = stringResource(R.string.settings_vehicle_consumption)) {
            val fuel = draft.fuelType
            if (fuel == null) {
                Hint(stringResource(R.string.settings_vehicle_fuel_fields_hint))
            } else {
                if (fuel.usesTank()) {
                    VehicleField(
                        value = draft.tankL,
                        onValueChange = { change(VehicleAiField.TANK_L) { d -> d.copy(tankL = it) } },
                        label = stringResource(R.string.settings_vehicle_tank),
                        numeric = true,
                        badge = VehicleAiField.TANK_L in badges,
                    )
                }
                if (fuel.usesBattery()) {
                    VehicleField(
                        value = draft.batteryKwh,
                        onValueChange = { change(VehicleAiField.BATTERY_KWH) { d -> d.copy(batteryKwh = it) } },
                        label = stringResource(R.string.settings_vehicle_battery),
                        numeric = true,
                        badge = VehicleAiField.BATTERY_KWH in badges,
                    )
                }
                if (fuel.usesFuelConsumption()) {
                    VehicleField(
                        value = draft.consumptionL100km,
                        onValueChange = { change(VehicleAiField.CONSUMPTION_L100KM) { d -> d.copy(consumptionL100km = it) } },
                        label = stringResource(R.string.settings_vehicle_consumption_l100),
                        numeric = true,
                        badge = VehicleAiField.CONSUMPTION_L100KM in badges,
                    )
                    VehicleField(
                        value = draft.consumptionUrbanL100km,
                        onValueChange = {
                            change(VehicleAiField.CONSUMPTION_URBAN_L100KM) { d -> d.copy(consumptionUrbanL100km = it) }
                        },
                        label = stringResource(R.string.settings_vehicle_consumption_urban),
                        numeric = true,
                        badge = VehicleAiField.CONSUMPTION_URBAN_L100KM in badges,
                    )
                    VehicleField(
                        value = draft.consumptionExtraUrbanL100km,
                        onValueChange = {
                            change(VehicleAiField.CONSUMPTION_EXTRA_URBAN_L100KM) { d ->
                                d.copy(consumptionExtraUrbanL100km = it)
                            }
                        },
                        label = stringResource(R.string.settings_vehicle_consumption_extra_urban),
                        numeric = true,
                        badge = VehicleAiField.CONSUMPTION_EXTRA_URBAN_L100KM in badges,
                    )
                    VehicleField(
                        value = draft.consumptionMotorwayL100km,
                        onValueChange = {
                            change(VehicleAiField.CONSUMPTION_MOTORWAY_L100KM) { d ->
                                d.copy(consumptionMotorwayL100km = it)
                            }
                        },
                        label = stringResource(R.string.settings_vehicle_consumption_motorway),
                        numeric = true,
                        badge = VehicleAiField.CONSUMPTION_MOTORWAY_L100KM in badges,
                    )
                }
                if (fuel.usesElectricConsumption()) {
                    SettingsGroupHeader(title = stringResource(R.string.settings_vehicle_electric_consumption))
                    VehicleField(
                        value = draft.energyKwh100km,
                        onValueChange = { change(VehicleAiField.ENERGY_KWH100KM) { d -> d.copy(energyKwh100km = it) } },
                        label = stringResource(R.string.settings_vehicle_consumption_kwh),
                        numeric = true,
                        badge = VehicleAiField.ENERGY_KWH100KM in badges,
                    )
                    VehicleField(
                        value = draft.energyUrbanKwh100km,
                        onValueChange = {
                            change(VehicleAiField.ENERGY_URBAN_KWH100KM) { d -> d.copy(energyUrbanKwh100km = it) }
                        },
                        label = stringResource(R.string.settings_vehicle_energy_urban),
                        numeric = true,
                        badge = VehicleAiField.ENERGY_URBAN_KWH100KM in badges,
                    )
                    VehicleField(
                        value = draft.energyExtraUrbanKwh100km,
                        onValueChange = {
                            change(VehicleAiField.ENERGY_EXTRA_URBAN_KWH100KM) { d ->
                                d.copy(energyExtraUrbanKwh100km = it)
                            }
                        },
                        label = stringResource(R.string.settings_vehicle_energy_extra_urban),
                        numeric = true,
                        badge = VehicleAiField.ENERGY_EXTRA_URBAN_KWH100KM in badges,
                    )
                    VehicleField(
                        value = draft.energyMotorwayKwh100km,
                        onValueChange = {
                            change(VehicleAiField.ENERGY_MOTORWAY_KWH100KM) { d ->
                                d.copy(energyMotorwayKwh100km = it)
                            }
                        },
                        label = stringResource(R.string.settings_vehicle_energy_motorway),
                        numeric = true,
                        badge = VehicleAiField.ENERGY_MOTORWAY_KWH100KM in badges,
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        SettingsGroup(title = stringResource(R.string.settings_vehicle_speed)) {
            VehicleField(
                value = draft.averageSpeedKph,
                onValueChange = { draft = draft.copy(averageSpeedKph = it) },
                label = stringResource(R.string.settings_vehicle_average_speed),
                numeric = true,
                integer = true,
            )
            VehicleField(
                value = draft.averageUrbanSpeedKph,
                onValueChange = { draft = draft.copy(averageUrbanSpeedKph = it) },
                label = stringResource(R.string.settings_vehicle_average_urban_speed),
                numeric = true,
                integer = true,
            )
            VehicleField(
                value = draft.averageExtraUrbanSpeedKph,
                onValueChange = { draft = draft.copy(averageExtraUrbanSpeedKph = it) },
                label = stringResource(R.string.settings_vehicle_average_extra_urban_speed),
                numeric = true,
                integer = true,
            )
            VehicleField(
                value = draft.averageMotorwaySpeedKph,
                onValueChange = { draft = draft.copy(averageMotorwaySpeedKph = it) },
                label = stringResource(R.string.settings_vehicle_average_motorway_speed),
                numeric = true,
                integer = true,
            )
            VehicleField(
                value = draft.maxSpeedKph,
                onValueChange = { change(VehicleAiField.MAX_SPEED_KPH) { d -> d.copy(maxSpeedKph = it) } },
                label = stringResource(R.string.settings_vehicle_max_speed),
                numeric = true,
                integer = true,
                badge = VehicleAiField.MAX_SPEED_KPH in badges,
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
private fun kindOptions(selected: VehicleKind?): List<Pair<VehicleKind, String>> {
    val values = VEHICLE_KINDS.toMutableList()
    if (selected != null && selected !in values) values += selected
    return values.map { it to kindLabel(it) }
}

@Composable
private fun fuelOptions(selected: FuelType?): List<Pair<FuelType, String>> {
    val values = VEHICLE_FUELS.toMutableList()
    if (selected != null && selected !in values) values += selected
    return values.map { it to fuelLabel(it) }
}

@Composable
private fun emissionOptions(selected: String): List<Pair<String, String>> {
    val values = VEHICLE_EMISSION_STANDARDS.toMutableList()
    if (selected.isNotBlank() && selected !in values) values += selected
    return values.map { it to it } + ("" to stringResource(R.string.settings_vehicle_emission_unknown))
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
private fun RecalculationMessage(state: ConsumptionRecalcState) {
    when (state) {
        is ConsumptionRecalcState.Done -> {
            Hint(
                stringResource(
                    R.string.settings_vehicle_recalculate_done,
                    stringResource(aiProviderLabel(state.provider)),
                ),
            )
            plateRedactionWarning(state.redactedPlateCount)
        }
        ConsumptionRecalcState.Outdated ->
            Hint(stringResource(R.string.settings_vehicle_recalculate_outdated))
        is ConsumptionRecalcState.Failed -> {
            val text = when (val outcome = state.outcome) {
                AiConsumptionOutcome.NoProvider ->
                    stringResource(R.string.settings_vehicle_ai_provider_none)
                is AiConsumptionOutcome.MissingKey ->
                    stringResource(R.string.settings_vehicle_ai_missing_key)
                is AiConsumptionOutcome.HttpError -> buildString {
                    append(stringResource(R.string.settings_vehicle_ai_http_error, outcome.status))
                    outcome.message?.let { append(" · ").append(it) }
                }
                is AiConsumptionOutcome.NetworkError ->
                    stringResource(R.string.settings_vehicle_ai_network_error)
                is AiConsumptionOutcome.InvalidResponse ->
                    stringResource(R.string.settings_vehicle_ai_invalid)
                is AiConsumptionOutcome.NoValues ->
                    stringResource(R.string.settings_vehicle_recalculate_no_values)
                is AiConsumptionOutcome.Success -> return
            }
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        ConsumptionRecalcState.Idle, ConsumptionRecalcState.Running -> Unit
    }
}

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
