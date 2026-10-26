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

/** The fuels offered for a new vehicle. Legacy hydrogen/other values stay decodable. */
internal val VEHICLE_FUELS = listOf(
    FuelType.PETROL,
    FuelType.DIESEL,
    FuelType.LPG,
    FuelType.CNG,
    FuelType.HYBRID,
    FuelType.PLUG_IN_HYBRID,
    FuelType.ELECTRIC,
)

/** Only these three vehicle types are offered; legacy truck/other values stay decodable. */
internal val VEHICLE_KINDS = listOf(VehicleKind.CAR, VehicleKind.VAN, VehicleKind.CAMPER)

/** A comprehensive, fixed list so the emission class is a choice rather than free text. */
internal val VEHICLE_EMISSION_STANDARDS = listOf(
    "Euro 0",
    "Euro 1",
    "Euro 2",
    "Euro 3",
    "Euro 4",
    "Euro 5",
    "Euro 5a",
    "Euro 5b",
    "Euro 5+",
    "Euro 6",
    "Euro 6a",
    "Euro 6b",
    "Euro 6c",
    "Euro 6d-TEMP",
    "Euro 6d",
    "Euro 6d-ISC-FCM",
    "Euro 6e",
    "Euro 6e-bis",
    "Euro 7",
)

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
    val averageSpeedKph: String = "",
    val averageUrbanSpeedKph: String = "",
    val averageExtraUrbanSpeedKph: String = "",
    val averageMotorwaySpeedKph: String = "",
    val lengthM: String = "",
    val widthM: String = "",
    val widthWithMirrorsM: String = "",
    val heightM: String = "",
    val weightKg: String = "",
    val grossWeightKg: String = "",
    val axleLoadKg: String = "",
    val axleCount: String = "",
    val consumptionL100km: String = "",
    val consumptionUrbanL100km: String = "",
    val consumptionExtraUrbanL100km: String = "",
    val consumptionMotorwayL100km: String = "",
    val energyKwh100km: String = "",
    val energyUrbanKwh100km: String = "",
    val energyExtraUrbanKwh100km: String = "",
    val energyMotorwayKwh100km: String = "",
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
        averageSpeedKph to 1..1_000,
        averageUrbanSpeedKph to 1..1_000,
        averageExtraUrbanSpeedKph to 1..1_000,
        averageMotorwaySpeedKph to 1..1_000,
        weightKg to 1..100_000,
        grossWeightKg to 1..100_000,
        axleLoadKg to 1..100_000,
        axleCount to 1..10,
    )

    private val decimalFields = listOf(
        lengthM, widthM, widthWithMirrorsM, heightM,
        consumptionL100km, consumptionUrbanL100km, consumptionExtraUrbanL100km,
        consumptionMotorwayL100km, energyKwh100km, energyUrbanKwh100km,
        energyExtraUrbanKwh100km, energyMotorwayKwh100km,
        batteryKwh, tankL, maxChargingPowerKw,
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
        return VehicleAiCriteria(
            make = make.trim(),
            model = model.trim(),
            year = year,
            fuelType = fuel,
            variant = variant.trim(),
        )
    }

    /**
     * Changes the fuel and drops every value that no longer applies. This keeps a diesel's tank
     * data from following the vehicle when the user switches to electric, and vice versa.
     */
    fun withFuel(fuel: FuelType): VehicleDraft = copy(
        fuelType = fuel,
        engineDisplacementCc = if (fuel.usesEngineDisplacement()) engineDisplacementCc else "",
        emissionStandard = if (fuel.usesEmissionStandard()) emissionStandard else "",
        tankL = if (fuel.usesTank()) tankL else "",
        batteryKwh = if (fuel.usesBattery()) batteryKwh else "",
        consumptionL100km = if (fuel.usesFuelConsumption()) consumptionL100km else "",
        consumptionUrbanL100km = if (fuel.usesFuelConsumption()) consumptionUrbanL100km else "",
        consumptionExtraUrbanL100km = if (fuel.usesFuelConsumption()) consumptionExtraUrbanL100km else "",
        consumptionMotorwayL100km = if (fuel.usesFuelConsumption()) consumptionMotorwayL100km else "",
        energyKwh100km = if (fuel.usesElectricConsumption()) energyKwh100km else "",
        energyUrbanKwh100km = if (fuel.usesElectricConsumption()) energyUrbanKwh100km else "",
        energyExtraUrbanKwh100km = if (fuel.usesElectricConsumption()) energyExtraUrbanKwh100km else "",
        energyMotorwayKwh100km = if (fuel.usesElectricConsumption()) energyMotorwayKwh100km else "",
        consumptionStandard = if (fuel.usesFuelConsumption() || fuel.usesElectricConsumption()) {
            consumptionStandard
        } else {
            ConsumptionStandard.UNKNOWN
        },
        maxChargingPowerKw = if (fuel.usesBattery()) maxChargingPowerKw else "",
        chargingConnector = if (fuel.usesBattery()) chargingConnector else "",
    )

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
        averageSpeedKph = parseInt(averageSpeedKph),
        averageUrbanSpeedKph = parseInt(averageUrbanSpeedKph),
        averageExtraUrbanSpeedKph = parseInt(averageExtraUrbanSpeedKph),
        averageMotorwaySpeedKph = parseInt(averageMotorwaySpeedKph),
        heightM = parseDouble(heightM),
        widthM = parseDouble(widthM),
        widthWithMirrorsM = parseDouble(widthWithMirrorsM),
        lengthM = parseDouble(lengthM),
        weightKg = parseInt(weightKg),
        grossWeightKg = parseInt(grossWeightKg),
        axleLoadKg = parseInt(axleLoadKg),
        axleCount = parseInt(axleCount),
        consumptionL100km = parseDouble(consumptionL100km),
        consumptionUrbanL100km = parseDouble(consumptionUrbanL100km),
        consumptionExtraUrbanL100km = parseDouble(consumptionExtraUrbanL100km),
        consumptionMotorwayL100km = parseDouble(consumptionMotorwayL100km),
        energyKwh100km = parseDouble(energyKwh100km),
        energyUrbanKwh100km = parseDouble(energyUrbanKwh100km),
        energyExtraUrbanKwh100km = parseDouble(energyExtraUrbanKwh100km),
        energyMotorwayKwh100km = parseDouble(energyMotorwayKwh100km),
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
            transmission = profile.transmission.uiChoice(),
            drivetrain = profile.drivetrain.uiChoice(),
            maxSpeedKph = profile.maxSpeedKph?.toString().orEmpty(),
            averageSpeedKph = profile.averageSpeedKph?.toString().orEmpty(),
            averageUrbanSpeedKph = profile.averageUrbanSpeedKph?.toString().orEmpty(),
            averageExtraUrbanSpeedKph = profile.averageExtraUrbanSpeedKph?.toString().orEmpty(),
            averageMotorwaySpeedKph = profile.averageMotorwaySpeedKph?.toString().orEmpty(),
            lengthM = formatNumber(profile.lengthM),
            widthM = formatNumber(profile.widthM),
            widthWithMirrorsM = formatNumber(profile.widthWithMirrorsM),
            heightM = formatNumber(profile.heightM),
            weightKg = profile.weightKg?.toString().orEmpty(),
            grossWeightKg = profile.grossWeightKg?.toString().orEmpty(),
            axleLoadKg = profile.axleLoadKg?.toString().orEmpty(),
            axleCount = profile.axleCount?.toString().orEmpty(),
            consumptionL100km = formatNumber(profile.consumptionL100km),
            consumptionUrbanL100km = formatNumber(profile.consumptionUrbanL100km),
            consumptionExtraUrbanL100km = formatNumber(profile.consumptionExtraUrbanL100km),
            consumptionMotorwayL100km = formatNumber(profile.consumptionMotorwayL100km),
            energyKwh100km = formatNumber(profile.energyKwh100km),
            energyUrbanKwh100km = formatNumber(profile.energyUrbanKwh100km),
            energyExtraUrbanKwh100km = formatNumber(profile.energyExtraUrbanKwh100km),
            energyMotorwayKwh100km = formatNumber(profile.energyMotorwayKwh100km),
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
            kind = payload.kind?.takeIf { it in VEHICLE_KINDS },
            plate = plate,
            emissionStandard = canonicalEmission(payload.emissionStandard),
            engineDisplacementCc = payload.engineDisplacementCc?.toString().orEmpty(),
            powerKw = payload.powerKw?.toString().orEmpty(),
            transmission = payload.transmission?.uiChoice() ?: Transmission.UNKNOWN,
            drivetrain = payload.drivetrain?.uiChoice() ?: Drivetrain.UNKNOWN,
            maxSpeedKph = payload.maxSpeedKph?.toString().orEmpty(),
            lengthM = formatNumber(payload.lengthM),
            widthM = formatNumber(payload.widthM),
            heightM = formatNumber(payload.heightM),
            weightKg = payload.weightKg?.toString().orEmpty(),
            consumptionL100km = formatNumber(payload.consumptionL100km),
            consumptionUrbanL100km = formatNumber(payload.consumptionUrbanL100km),
            consumptionExtraUrbanL100km = formatNumber(payload.consumptionExtraUrbanL100km),
            consumptionMotorwayL100km = formatNumber(payload.consumptionMotorwayL100km),
            energyKwh100km = formatNumber(payload.energyKwh100km),
            energyUrbanKwh100km = formatNumber(payload.energyUrbanKwh100km),
            energyExtraUrbanKwh100km = formatNumber(payload.energyExtraUrbanKwh100km),
            energyMotorwayKwh100km = formatNumber(payload.energyMotorwayKwh100km),
            batteryKwh = formatNumber(payload.batteryKwh),
            tankL = formatNumber(payload.tankL),
        ).withFuel(criteria.fuelType)

        private val YEAR_RANGE = 1886..2100
    }
}

internal fun FuelType?.usesTank(): Boolean = this != null && this != FuelType.ELECTRIC

internal fun FuelType?.usesBattery(): Boolean =
    this == FuelType.HYBRID || this == FuelType.PLUG_IN_HYBRID || this == FuelType.ELECTRIC

internal fun FuelType?.usesEngineDisplacement(): Boolean = this != FuelType.ELECTRIC

internal fun FuelType?.usesEmissionStandard(): Boolean = this != FuelType.ELECTRIC

internal fun FuelType?.usesFuelConsumption(): Boolean = this != null && this != FuelType.ELECTRIC

internal fun FuelType?.usesElectricConsumption(): Boolean =
    this == FuelType.PLUG_IN_HYBRID || this == FuelType.ELECTRIC

/** The AI badges that stop applying when the fuel changes and the values are dropped. */
internal fun VehicleDraft.clearedByFuel(fuel: FuelType): Set<VehicleAiField> = buildSet {
    if (!fuel.usesEngineDisplacement()) {
        add(VehicleAiField.ENGINE_DISPLACEMENT_CC)
        add(VehicleAiField.EMISSION_STANDARD)
    }
    if (!fuel.usesTank()) add(VehicleAiField.TANK_L)
    if (!fuel.usesBattery()) add(VehicleAiField.BATTERY_KWH)
    if (!fuel.usesFuelConsumption()) {
        add(VehicleAiField.CONSUMPTION_L100KM)
        add(VehicleAiField.CONSUMPTION_URBAN_L100KM)
        add(VehicleAiField.CONSUMPTION_EXTRA_URBAN_L100KM)
        add(VehicleAiField.CONSUMPTION_MOTORWAY_L100KM)
    }
    if (!fuel.usesElectricConsumption()) {
        add(VehicleAiField.ENERGY_KWH100KM)
        add(VehicleAiField.ENERGY_URBAN_KWH100KM)
        add(VehicleAiField.ENERGY_EXTRA_URBAN_KWH100KM)
        add(VehicleAiField.ENERGY_MOTORWAY_KWH100KM)
    }
}

/** AI sometimes changes the case of a known standard ("6d-Temp"); keep the chip list canonical. */
private fun canonicalEmission(raw: String?): String {
    val text = raw.orEmpty().trim()
    if (text.isEmpty()) return ""
    return VEHICLE_EMISSION_STANDARDS.firstOrNull { it.equals(text, ignoreCase = true) } ?: text
}

private fun Transmission.uiChoice(): Transmission = when (this) {
    Transmission.MANUAL -> Transmission.MANUAL
    Transmission.AUTOMATIC, Transmission.DCT, Transmission.CVT -> Transmission.AUTOMATIC
    Transmission.OTHER, Transmission.UNKNOWN -> Transmission.UNKNOWN
}

private fun Drivetrain.uiChoice(): Drivetrain = when (this) {
    Drivetrain.FWD -> Drivetrain.FWD
    Drivetrain.RWD -> Drivetrain.RWD
    Drivetrain.AWD -> Drivetrain.AWD
    Drivetrain.OTHER, Drivetrain.UNKNOWN -> Drivetrain.UNKNOWN
}

internal fun parseDouble(raw: String): Double? =
    raw.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }

internal fun parseInt(raw: String): Int? =
    raw.trim().toIntOrNull()?.takeIf { it >= 0 }

/** Editing a field removes only that field's AI badge; a null field keeps the set. */
internal fun badgesAfterEdit(badges: Set<VehicleAiField>, field: VehicleAiField?): Set<VehicleAiField> =
    if (field == null) badges else badges - field

/**
 * Keeps only the badges whose field still has a value. Fuel changes can drop an AI value before
 * the editor opens, and an empty field must not claim the provider filled it.
 */
internal fun VehicleDraft.aiBadges(fields: Set<VehicleAiField>): Set<VehicleAiField> =
    fields.filterTo(linkedSetOf()) { field ->
        when (field) {
            VehicleAiField.MAKE, VehicleAiField.MODEL, VehicleAiField.YEAR,
            VehicleAiField.FUEL_TYPE -> false
            VehicleAiField.VARIANT -> variant.isNotBlank()
            VehicleAiField.KIND -> kind != null
            VehicleAiField.EMISSION_STANDARD -> emissionStandard.isNotBlank()
            VehicleAiField.ENGINE_DISPLACEMENT_CC -> engineDisplacementCc.isNotBlank()
            VehicleAiField.POWER_KW -> powerKw.isNotBlank()
            VehicleAiField.TRANSMISSION -> transmission != Transmission.UNKNOWN
            VehicleAiField.DRIVETRAIN -> drivetrain != Drivetrain.UNKNOWN
            VehicleAiField.MAX_SPEED_KPH -> maxSpeedKph.isNotBlank()
            VehicleAiField.LENGTH_M -> lengthM.isNotBlank()
            VehicleAiField.WIDTH_M -> widthM.isNotBlank()
            VehicleAiField.HEIGHT_M -> heightM.isNotBlank()
            VehicleAiField.WEIGHT_KG -> weightKg.isNotBlank()
            VehicleAiField.TANK_L -> tankL.isNotBlank()
            VehicleAiField.BATTERY_KWH -> batteryKwh.isNotBlank()
            VehicleAiField.CONSUMPTION_L100KM -> consumptionL100km.isNotBlank()
            VehicleAiField.CONSUMPTION_URBAN_L100KM -> consumptionUrbanL100km.isNotBlank()
            VehicleAiField.CONSUMPTION_EXTRA_URBAN_L100KM -> consumptionExtraUrbanL100km.isNotBlank()
            VehicleAiField.CONSUMPTION_MOTORWAY_L100KM -> consumptionMotorwayL100km.isNotBlank()
            VehicleAiField.ENERGY_KWH100KM -> energyKwh100km.isNotBlank()
            VehicleAiField.ENERGY_URBAN_KWH100KM -> energyUrbanKwh100km.isNotBlank()
            VehicleAiField.ENERGY_EXTRA_URBAN_KWH100KM -> energyExtraUrbanKwh100km.isNotBlank()
            VehicleAiField.ENERGY_MOTORWAY_KWH100KM -> energyMotorwayKwh100km.isNotBlank()
        }
    }

internal fun formatNumber(value: Double?): String {
    if (value == null) return ""
    return if (value % 1.0 == 0.0) value.toLong().toString()
    else String.format(Locale.getDefault(), "%.2f", value).trimEnd('0').trimEnd('.')
}
