package app.vela.ui.settings.sections

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vela.R
import app.vela.core.ai.PlateRedactor
import app.vela.core.ai.VehicleAiCriteria
import app.vela.core.ai.VehicleAiField
import app.vela.core.model.VehicleProfile
import app.vela.ui.VelaDialog
import app.vela.ui.VelaMenu
import app.vela.ui.dpadClickable
import app.vela.ui.dpadHighlight
import app.vela.ui.icons.Sym
import app.vela.ui.item
import app.vela.ui.settings.Hint
import app.vela.ui.settings.PageIntro
import app.vela.ui.settings.SettingsGroup
import app.vela.ui.settings.SettingsScaffold
import app.vela.ui.vehicle.ChoiceChips
import app.vela.ui.vehicle.VehicleAiSearchState
import app.vela.ui.vehicle.VehicleDraft
import app.vela.ui.vehicle.VehicleEditorScreen
import app.vela.ui.vehicle.VehicleEditorTarget
import app.vela.ui.vehicle.VehicleField
import app.vela.ui.vehicle.VehicleSettingsViewModel
import app.vela.ui.vehicle.aiProviderLabel
import app.vela.ui.vehicle.conflictWarning
import app.vela.ui.vehicle.fuelLabel
import app.vela.ui.vehicle.kindLabel
import app.vela.ui.vehicle.plateRedactionWarning

/** The garage list, the AI/manual creation entry points, and the AI result preview. */
@Composable
internal fun VehiclesSettingsScreen(
    onBack: () -> Unit,
    vm: VehicleSettingsViewModel = hiltViewModel(),
) {
    val vehicles by vm.vehicles.collectAsStateWithLifecycle()
    val active by vm.activeVehicle.collectAsStateWithLifecycle()
    var page by remember { mutableStateOf<VehiclePage>(VehiclePage.Garage) }
    var editor by remember { mutableStateOf<VehicleEditorTarget?>(null) }
    var deleting by remember { mutableStateOf<VehicleProfile?>(null) }

    BackHandler(enabled = editor != null || page != VehiclePage.Garage) {
        when {
            editor != null -> editor = null
            page == VehiclePage.CreateAi -> {
                vm.resetSearch()
                page = VehiclePage.CreateChoice
            }
            page == VehiclePage.CreateChoice -> page = VehiclePage.Garage
        }
    }

    editor?.let { target ->
        VehicleEditorScreen(
            target = target,
            onBack = { editor = null },
            onSave = {
                vm.save(it)
                editor = null
                page = VehiclePage.Garage
                if (target.isNew) vm.resetSearch()
            },
        )
        return
    }

    when (page) {
        VehiclePage.Garage -> {
            GarageList(
                vehicles = vehicles,
                activeId = active?.id,
                onBack = onBack,
                onAdd = { page = VehiclePage.CreateChoice },
                onActivate = { vm.setActive(it) },
                onEdit = { editor = VehicleEditorTarget(VehicleDraft.from(it), isNew = false) },
                onDelete = { deleting = it },
            )
            deleting?.let { vehicle ->
                VelaDialog(
                    onDismissRequest = { deleting = null },
                    title = stringResource(R.string.settings_vehicle_delete_title),
                    confirmText = stringResource(R.string.settings_vehicle_delete_confirm),
                    onConfirm = {
                        vm.delete(vehicle.id)
                        deleting = null
                    },
                    dismissText = stringResource(R.string.list_cancel),
                    onDismiss = { deleting = null },
                ) {
                    Text(stringResource(R.string.settings_vehicle_delete_body, vehicle.displayName))
                }
            }
        }

        VehiclePage.CreateChoice -> CreateChoice(
            onBack = { page = VehiclePage.Garage },
            onAi = { page = VehiclePage.CreateAi },
            onManual = {
                editor = VehicleEditorTarget(VehicleDraft.newManual(), isNew = true)
                page = VehiclePage.Garage
            },
        )

        VehiclePage.CreateAi -> VehicleAiCreate(
            vm = vm,
            onBack = {
                vm.resetSearch()
                page = VehiclePage.CreateChoice
            },
            onOpenEditor = { target -> editor = target },
        )
    }
}

private sealed interface VehiclePage {
    data object Garage : VehiclePage
    data object CreateChoice : VehiclePage
    data object CreateAi : VehiclePage
}

@Composable
private fun GarageList(
    vehicles: List<VehicleProfile>,
    activeId: String?,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onActivate: (String) -> Unit,
    onEdit: (VehicleProfile) -> Unit,
    onDelete: (VehicleProfile) -> Unit,
) {
    SettingsScaffold(stringResource(R.string.settings_vehicles), onBack) { topRow ->
        Spacer(Modifier.height(4.dp))
        PageIntro(stringResource(R.string.settings_vehicles_hint))
        SettingsGroup(title = stringResource(R.string.settings_vehicles)) {
            if (vehicles.isEmpty()) {
                Hint(stringResource(R.string.settings_vehicles_empty))
            } else {
                vehicles.forEachIndexed { index, vehicle ->
                    if (index > 0) app.vela.ui.settings.GroupDivider()
                    VehicleRow(
                        vehicle = vehicle,
                        activeId = activeId,
                        modifier = if (index == 0) topRow else Modifier,
                        onActivate = { onActivate(vehicle.id) },
                        onEdit = { onEdit(vehicle) },
                        onDelete = { onDelete(vehicle) },
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        FilledTonalButton(
            onClick = onAdd,
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
}

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
private fun CreateChoice(
    onBack: () -> Unit,
    onAi: () -> Unit,
    onManual: () -> Unit,
) {
    SettingsScaffold(stringResource(R.string.settings_vehicle_add), onBack) { topRow ->
        Spacer(Modifier.height(4.dp))
        PageIntro(stringResource(R.string.settings_vehicle_create_hint))
        Spacer(Modifier.height(12.dp))
        FilledTonalButton(
            onClick = onAi,
            modifier = topRow.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            Icon(Sym.Search, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.settings_vehicle_add_ai))
        }
        Hint(stringResource(R.string.settings_vehicle_add_ai_hint))
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = onManual,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            Text(stringResource(R.string.settings_vehicle_add_manual))
        }
        Hint(stringResource(R.string.settings_vehicle_add_manual_hint))
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun VehicleAiCreate(
    vm: VehicleSettingsViewModel,
    onBack: () -> Unit,
    onOpenEditor: (VehicleEditorTarget) -> Unit,
) {
    val state by vm.search.collectAsStateWithLifecycle()
    val settings by vm.aiSettings.collectAsStateWithLifecycle()
    var form by remember { mutableStateOf(VehicleDraft.newManual()) }
    var query by remember { mutableStateOf("") }
    var pendingCriteria by remember { mutableStateOf<VehicleAiCriteria?>(null) }
    val redaction = remember(query) { PlateRedactor.redact(query) }

    val success = state as? VehicleAiSearchState.Success
    BackHandler {
        if (success != null && pendingCriteria != null) {
            vm.resetSearch()
        } else {
            vm.resetSearch()
            onBack()
        }
    }
    if (success != null && pendingCriteria != null) {
        val criteria = pendingCriteria!!
        val draft = remember(success.answer, criteria, form.plate) {
            VehicleDraft.fromAi(criteria, success.answer.payload, form.plate)
        }
        VehicleAiPreview(
            providerLabel = stringResource(aiProviderLabel(success.provider)),
            draft = draft,
            badges = success.answer.badgeFields,
            conflicts = success.answer.conflicts,
            redactedPlateCount = success.answer.redactedPlateCount,
            onBack = { vm.resetSearch() },
            onContinue = {
                onOpenEditor(
                    VehicleEditorTarget(
                        draft = draft,
                        isNew = true,
                        badges = success.answer.badgeFields,
                        conflicts = success.answer.conflicts,
                        redactedPlateCount = success.answer.redactedPlateCount,
                    ),
                )
            },
        )
        return
    }

    val criteria = form.searchCriteria()
    val loading = state is VehicleAiSearchState.Loading
    SettingsScaffold(
        stringResource(R.string.settings_vehicle_ai_title),
        onBack = {
            vm.resetSearch()
            onBack()
        },
    ) { topRow ->
        Spacer(Modifier.height(4.dp))
        PageIntro(stringResource(R.string.settings_vehicle_ai_intro))
        val active = settings.active
        if (active == null) {
            Hint(stringResource(R.string.settings_vehicle_ai_provider_none))
        } else {
            Hint(stringResource(R.string.settings_vehicle_ai_active_provider, stringResource(aiProviderLabel(active))))
            if (!settings.config(active).hasKey) {
                Hint(stringResource(R.string.settings_vehicle_ai_missing_key))
            }
        }
        SettingsGroup(title = stringResource(R.string.settings_vehicle_identity)) {
            VehicleField(
                value = form.make,
                onValueChange = { form = form.copy(make = it) },
                label = stringResource(R.string.settings_vehicle_make),
                modifier = topRow,
            )
            VehicleField(
                value = form.model,
                onValueChange = { form = form.copy(model = it) },
                label = stringResource(R.string.settings_vehicle_model),
            )
            VehicleField(
                value = form.year,
                onValueChange = { form = form.copy(year = it) },
                label = stringResource(R.string.settings_vehicle_year),
                numeric = true,
                integer = true,
            )
            VehicleField(
                value = form.plate,
                onValueChange = { form = form.copy(plate = it) },
                label = stringResource(R.string.settings_vehicle_plate),
                placeholder = stringResource(R.string.settings_vehicle_plate_hint),
            )
        }

        Spacer(Modifier.height(8.dp))
        SettingsGroup(title = stringResource(R.string.settings_vehicle_fuel)) {
            ChoiceChips(
                options = app.vela.core.model.FuelType.entries.map { it to fuelLabel(it) },
                selected = form.fuelType,
                onSelected = { form = form.copy(fuelType = it) },
            )
        }

        Spacer(Modifier.height(8.dp))
        SettingsGroup(title = stringResource(R.string.settings_vehicle_ai_query_group)) {
            VehicleField(
                value = query,
                onValueChange = { query = it },
                label = stringResource(R.string.settings_vehicle_ai_query),
                placeholder = stringResource(R.string.settings_vehicle_ai_query_hint),
                singleLine = false,
            )
            Hint(stringResource(R.string.settings_vehicle_ai_query_privacy))
            if (redaction.removed > 0) {
                plateRedactionWarning(redaction.removed)
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                val ready = form.searchCriteria() ?: return@Button
                pendingCriteria = ready
                vm.search(ready, query)
            },
            enabled = criteria != null && !loading,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_vehicle_ai_searching))
            } else {
                Text(stringResource(R.string.settings_vehicle_ai_search))
            }
        }
        if (criteria == null) Hint(stringResource(R.string.settings_vehicle_ai_criteria_required))
        SearchError(state)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SearchError(state: VehicleAiSearchState) {
    val text = when (state) {
        VehicleAiSearchState.NoProvider -> stringResource(R.string.settings_vehicle_ai_provider_none)
        is VehicleAiSearchState.MissingKey -> stringResource(R.string.settings_vehicle_ai_missing_key)
        is VehicleAiSearchState.HttpError -> buildString {
            append(stringResource(R.string.settings_vehicle_ai_http_error, state.status))
            state.message?.let { append(" · ").append(it) }
        }
        is VehicleAiSearchState.NetworkError -> stringResource(R.string.settings_vehicle_ai_network_error)
        is VehicleAiSearchState.InvalidResponse -> stringResource(R.string.settings_vehicle_ai_invalid)
        else -> return
    }
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

@Composable
private fun VehicleAiPreview(
    providerLabel: String,
    draft: VehicleDraft,
    badges: Set<VehicleAiField>,
    conflicts: Set<app.vela.core.ai.VehicleAiIdentityField>,
    redactedPlateCount: Int,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val rows = mutableListOf<Pair<String, String>>()
    fun add(label: String, value: String?) {
        if (!value.isNullOrBlank()) rows += label to value
    }

    add(stringResource(R.string.settings_vehicle_make), draft.make)
    add(stringResource(R.string.settings_vehicle_model), draft.model)
    add(stringResource(R.string.settings_vehicle_variant), draft.variant)
    add(stringResource(R.string.settings_vehicle_year), draft.year)
    draft.fuelType?.let { rows += stringResource(R.string.settings_vehicle_fuel) to fuelLabel(it) }
    draft.kind?.let { rows += stringResource(R.string.settings_vehicle_kind) to kindLabel(it) }
    add(stringResource(R.string.settings_vehicle_plate), draft.plate)
    add(stringResource(R.string.settings_vehicle_emission_standard), draft.emissionStandard)
    add(stringResource(R.string.settings_vehicle_engine_cc), draft.engineDisplacementCc)
    add(stringResource(R.string.settings_vehicle_power_kw), draft.powerKw)
    if (draft.transmission != app.vela.core.model.Transmission.UNKNOWN) {
        rows += stringResource(R.string.settings_vehicle_transmission) to
            app.vela.ui.vehicle.transmissionLabel(draft.transmission)
    }
    if (draft.drivetrain != app.vela.core.model.Drivetrain.UNKNOWN) {
        rows += stringResource(R.string.settings_vehicle_drivetrain) to
            app.vela.ui.vehicle.drivetrainLabel(draft.drivetrain)
    }
    add(stringResource(R.string.settings_vehicle_max_speed), draft.maxSpeedKph)
    add(stringResource(R.string.settings_vehicle_length), draft.lengthM)
    add(stringResource(R.string.settings_vehicle_width), draft.widthM)
    add(stringResource(R.string.settings_vehicle_width_mirrors), draft.widthWithMirrorsM)
    add(stringResource(R.string.settings_vehicle_height), draft.heightM)
    add(stringResource(R.string.settings_vehicle_weight), draft.weightKg)
    add(stringResource(R.string.settings_vehicle_gross_weight), draft.grossWeightKg)
    add(stringResource(R.string.settings_vehicle_axle_load), draft.axleLoadKg)
    add(stringResource(R.string.settings_vehicle_axles), draft.axleCount)
    add(stringResource(R.string.settings_vehicle_consumption_l100), draft.consumptionL100km)
    add(stringResource(R.string.settings_vehicle_consumption_kwh), draft.energyKwh100km)
    add(stringResource(R.string.settings_vehicle_battery), draft.batteryKwh)
    add(stringResource(R.string.settings_vehicle_tank), draft.tankL)
    if (draft.consumptionStandard != app.vela.core.model.ConsumptionStandard.UNKNOWN) {
        rows += stringResource(R.string.settings_vehicle_consumption_standard) to
            app.vela.ui.vehicle.consumptionStandardLabel(draft.consumptionStandard)
    }
    add(stringResource(R.string.settings_vehicle_max_charging), draft.maxChargingPowerKw)
    add(stringResource(R.string.settings_vehicle_connector), draft.chargingConnector)

    val technical = badges.isNotEmpty()
    SettingsScaffold(stringResource(R.string.settings_vehicle_ai_preview_title), onBack) { topRow ->
        Spacer(Modifier.height(4.dp))
        PageIntro(stringResource(R.string.settings_vehicle_ai_preview_intro))
        conflictWarning(conflicts)
        plateRedactionWarning(redactedPlateCount)
        SettingsGroup(
            title = stringResource(R.string.settings_vehicle_ai_found, providerLabel),
        ) {
            if (rows.isEmpty()) {
                Hint(stringResource(R.string.settings_vehicle_ai_empty))
            } else {
                rows.forEachIndexed { index, (label, value) ->
                    if (index > 0) app.vela.ui.settings.GroupDivider()
                    SummaryRow(label = label, value = value)
                }
            }
        }
        if (!technical) Hint(stringResource(R.string.settings_vehicle_ai_no_values))
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onContinue,
            modifier = topRow.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            Text(stringResource(R.string.settings_vehicle_ai_open_editor))
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SummaryRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
    }
}
