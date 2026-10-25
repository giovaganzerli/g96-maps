package app.vela.ui.vehicle

import app.vela.core.ai.VehicleAiCriteria
import app.vela.core.ai.VehicleAiField
import app.vela.core.ai.VehicleAiPayload
import app.vela.core.model.ConsumptionStandard
import app.vela.core.model.Drivetrain
import app.vela.core.model.FuelType
import app.vela.core.model.Transmission
import app.vela.core.model.VehicleKind
import app.vela.core.model.VehicleProfile
import app.vela.core.model.VehicleRoutingMode
import java.util.Locale
import java.util.UUID

/**
 * The editor's string form of a vehicle. It is deliberately free of Compose and Android types so
 * the required-field and number rules can be unit tested. [kind] and [fuelType] are nullable
 * because a new vehicle starts with neither chosen.
 */
internal data class VehicleDraft(
    val id: String,
    val legacyNickname: String = "",
    val make: String = "",
    val model: String = "",
    val variant: String = "",
    val year: String = "",
    val fuelType: FuelType? = null,
    val kind: VehicleKind? = null,
    val plate: String = "",
    val emissionStandard: String = "",
    val engineDisplacementCc: String = "",
    val powerKw: String = "",
    val transmission: Transmission = Transmission.UNKNOWN,
    val drivetrain: Drivetrain = Drivetrain.UNKNOWN,
    val maxSpeedKph: String = "",
    val lengthM: String = "",
    val widthM: String = "",
    val widthWithMirrorsM: String = "",
    val heightM: String = "",
    val weightKg: String = "",
    val grossWeightKg: String = "",
    val axleLoadKg: String = "",
    val axleCount: String = "",
    val consumptionL100km: String = "",
    val energyKwh100km: String = "",
    val consumptionStandard: ConsumptionStandard = ConsumptionStandard.UNKNOWN,
    val batteryKwh: String = "",
    val tankL: String = "",
    val maxChargingPowerKw: String = "",
    val chargingConnector: String = "",
    val routingMode: VehicleRoutingMode = VehicleRoutingMode.AUTO,
    val notes: String = "",
    val createdAt: Long = 0L,
) {
    val yearValue: Int? get() = parseInt(year)?.takeIf { it in YEAR_RANGE }

    private val integerFields = listOf(
        year to YEAR_RANGE,
        engineDisplacementCc to 1..20_000,
        powerKw to 1..2_000,
        maxSpeedKph to 1..1_000,
        weightKg to 1..100_000,
        grossWeightKg to 1..100_000,
        axleLoadKg to 1..100_000,
        axleCount to 1..10,
    )

    private val decimalFields = listOf(
        lengthM, widthM, widthWithMirrorsM, heightM, consumptionL100km,
        energyKwh100km, batteryKwh, tankL, maxChargingPowerKw,
    )

    private fun numbersAreValid(): Boolean =
        integerFields.all { (raw, range) ->
            raw.isBlank() || parseInt(raw)?.let { it in range } == true
        } &&
            decimalFields.all { it.isBlank() || parseDouble(it) != null }

    /** Make, model, year, fuel and kind are the identity; nothing can be saved without them. */
    fun isValid(): Boolean =
        make.isNotBlank() &&
            model.isNotBlank() &&
            yearValue != null &&
            fuelType != null &&
            kind != null &&
            numbersAreValid()

    /** The AI search only needs the identity the user typed; kind may still be filled by AI. */
    fun searchCriteria(): VehicleAiCriteria? {
        val year = yearValue ?: return null
        val fuel = fuelType ?: return null
        if (make.isBlank() || model.isBlank()) return null
        return VehicleAiCriteria(make = make.trim(), model = model.trim(), year = year, fuelType = fuel)
    }

    fun toProfile(): VehicleProfile = VehicleProfile(
        id = id,
        nickname = legacyNickname,
        kind = requireNotNull(kind) { "kind is required" },
        make = make.trim(),
        model = model.trim(),
        variant = variant.trim(),
        year = yearValue,
        fuelType = requireNotNull(fuelType) { "fuel is required" },
        routingMode = routingMode,
        plate = plate.trim(),
        emissionStandard = emissionStandard.trim(),
        engineDisplacementCc = parseInt(engineDisplacementCc),
        powerKw = parseInt(powerKw),
        transmission = transmission,
        drivetrain = drivetrain,
        maxSpeedKph = parseInt(maxSpeedKph),
        heightM = parseDouble(heightM),
        widthM = parseDouble(widthM),
        widthWithMirrorsM = parseDouble(widthWithMirrorsM),
        lengthM = parseDouble(lengthM),
        weightKg = parseInt(weightKg),
        grossWeightKg = parseInt(grossWeightKg),
        axleLoadKg = parseInt(axleLoadKg),
        axleCount = parseInt(axleCount),
        consumptionL100km = parseDouble(consumptionL100km),
        energyKwh100km = parseDouble(energyKwh100km),
        consumptionStandard = consumptionStandard,
        batteryKwh = parseDouble(batteryKwh),
        tankL = parseDouble(tankL),
        maxChargingPowerKw = parseDouble(maxChargingPowerKw),
        chargingConnector = chargingConnector.trim(),
        notes = notes.trim(),
        createdAt = createdAt,
    )

    companion object {
        fun newManual(): VehicleDraft = VehicleDraft(id = UUID.randomUUID().toString())

        fun from(profile: VehicleProfile): VehicleDraft = VehicleDraft(
            id = profile.id,
            legacyNickname = profile.nickname,
            make = profile.make,
            model = profile.model,
            variant = profile.variant,
            year = profile.year?.toString().orEmpty(),
            fuelType = profile.fuelType,
            kind = profile.kind,
            plate = profile.plate,
            emissionStandard = profile.emissionStandard,
            engineDisplacementCc = profile.engineDisplacementCc?.toString().orEmpty(),
            powerKw = profile.powerKw?.toString().orEmpty(),
            transmission = profile.transmission,
            drivetrain = profile.drivetrain,
            maxSpeedKph = profile.maxSpeedKph?.toString().orEmpty(),
            lengthM = formatNumber(profile.lengthM),
            widthM = formatNumber(profile.widthM),
            widthWithMirrorsM = formatNumber(profile.widthWithMirrorsM),
            heightM = formatNumber(profile.heightM),
            weightKg = profile.weightKg?.toString().orEmpty(),
            grossWeightKg = profile.grossWeightKg?.toString().orEmpty(),
            axleLoadKg = profile.axleLoadKg?.toString().orEmpty(),
            axleCount = profile.axleCount?.toString().orEmpty(),
            consumptionL100km = formatNumber(profile.consumptionL100km),
            energyKwh100km = formatNumber(profile.energyKwh100km),
            consumptionStandard = profile.consumptionStandard,
            batteryKwh = formatNumber(profile.batteryKwh),
            tankL = formatNumber(profile.tankL),
            maxChargingPowerKw = formatNumber(profile.maxChargingPowerKw),
            chargingConnector = profile.chargingConnector,
            routingMode = profile.routingMode,
            notes = profile.notes,
            createdAt = profile.createdAt,
        )

        /** The typed identity wins; the AI data fills the technical fields and the type. */
        fun fromAi(
            criteria: VehicleAiCriteria,
            payload: VehicleAiPayload,
            plate: String = "",
        ): VehicleDraft = VehicleDraft(
            id = UUID.randomUUID().toString(),
            make = criteria.make,
            model = criteria.model,
            variant = payload.variant.orEmpty(),
            year = criteria.year.toString(),
            fuelType = criteria.fuelType,
            kind = payload.kind,
            plate = plate,
            emissionStandard = payload.emissionStandard.orEmpty(),
            engineDisplacementCc = payload.engineDisplacementCc?.toString().orEmpty(),
            powerKw = payload.powerKw?.toString().orEmpty(),
            transmission = payload.transmission ?: Transmission.UNKNOWN,
            drivetrain = payload.drivetrain ?: Drivetrain.UNKNOWN,
            maxSpeedKph = payload.maxSpeedKph?.toString().orEmpty(),
            lengthM = formatNumber(payload.lengthM),
            widthM = formatNumber(payload.widthM),
            widthWithMirrorsM = formatNumber(payload.widthWithMirrorsM),
            heightM = formatNumber(payload.heightM),
            weightKg = payload.weightKg?.toString().orEmpty(),
            grossWeightKg = payload.grossWeightKg?.toString().orEmpty(),
            axleLoadKg = payload.axleLoadKg?.toString().orEmpty(),
            axleCount = payload.axleCount?.toString().orEmpty(),
            consumptionL100km = formatNumber(payload.consumptionL100km),
            energyKwh100km = formatNumber(payload.energyKwh100km),
            consumptionStandard = payload.consumptionStandard ?: ConsumptionStandard.UNKNOWN,
            batteryKwh = formatNumber(payload.batteryKwh),
            tankL = formatNumber(payload.tankL),
            maxChargingPowerKw = formatNumber(payload.maxChargingPowerKw),
            chargingConnector = payload.chargingConnector.orEmpty(),
        )

        private val YEAR_RANGE = 1886..2100
    }
}

internal fun parseDouble(raw: String): Double? =
    raw.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }

internal fun parseInt(raw: String): Int? =
    raw.trim().toIntOrNull()?.takeIf { it >= 0 }

/** Editing a field removes only that field's AI badge; a null field keeps the set. */
internal fun badgesAfterEdit(badges: Set<VehicleAiField>, field: VehicleAiField?): Set<VehicleAiField> =
    if (field == null) badges else badges - field

internal fun formatNumber(value: Double?): String {
    if (value == null) return ""
    return if (value % 1.0 == 0.0) value.toLong().toString()
    else String.format(Locale.getDefault(), "%.2f", value).trimEnd('0').trimEnd('.')
}
