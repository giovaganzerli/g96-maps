package app.vela.core.ai

import app.vela.core.model.Drivetrain
import app.vela.core.model.FuelType
import app.vela.core.model.Transmission
import app.vela.core.model.VehicleKind
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Every canonical field an AI answer may contain. The list is the contract: every provider is
 * asked for every key, an unknown value is null, and keys outside this list are ignored.
 */
enum class VehicleAiField(val key: String) {
    MAKE("make"),
    MODEL("model"),
    VARIANT("variant"),
    YEAR("year"),
    FUEL_TYPE("fuelType"),
    KIND("kind"),
    EMISSION_STANDARD("emissionStandard"),
    ENGINE_DISPLACEMENT_CC("engineDisplacementCc"),
    POWER_KW("powerKw"),
    TRANSMISSION("transmission"),
    DRIVETRAIN("drivetrain"),
    MAX_SPEED_KPH("maxSpeedKph"),
    LENGTH_M("lengthM"),
    WIDTH_M("widthM"),
    HEIGHT_M("heightM"),
    WEIGHT_KG("weightKg"),
    TANK_L("tankL"),
    BATTERY_KWH("batteryKwh"),
    CONSUMPTION_L100KM("consumptionL100km"),
    CONSUMPTION_URBAN_L100KM("consumptionUrbanL100km"),
    CONSUMPTION_EXTRA_URBAN_L100KM("consumptionExtraUrbanL100km"),
    CONSUMPTION_MOTORWAY_L100KM("consumptionMotorwayL100km"),
    ENERGY_KWH100KM("energyKwh100km"),
    ENERGY_URBAN_KWH100KM("energyUrbanKwh100km"),
    ENERGY_EXTRA_URBAN_KWH100KM("energyExtraUrbanKwh100km"),
    ENERGY_MOTORWAY_KWH100KM("energyMotorwayKwh100km"),
}

/** The identity fields the user typed before the search. AI may not replace them. */
enum class VehicleAiIdentityField { MAKE, MODEL, YEAR, FUEL_TYPE }

/**
 * One normalized AI answer. Every property is nullable and every key is written by
 * [toJsonObject], so two providers that know the same vehicle produce the same shape.
 */
data class VehicleAiPayload(
    val make: String? = null,
    val model: String? = null,
    val variant: String? = null,
    val year: Int? = null,
    val fuelType: FuelType? = null,
    val kind: VehicleKind? = null,
    val emissionStandard: String? = null,
    val engineDisplacementCc: Int? = null,
    val powerKw: Int? = null,
    val transmission: Transmission? = null,
    val drivetrain: Drivetrain? = null,
    val maxSpeedKph: Int? = null,
    val lengthM: Double? = null,
    val widthM: Double? = null,
    val heightM: Double? = null,
    val weightKg: Int? = null,
    val tankL: Double? = null,
    val batteryKwh: Double? = null,
    val consumptionL100km: Double? = null,
    val consumptionUrbanL100km: Double? = null,
    val consumptionExtraUrbanL100km: Double? = null,
    val consumptionMotorwayL100km: Double? = null,
    val energyKwh100km: Double? = null,
    val energyUrbanKwh100km: Double? = null,
    val energyExtraUrbanKwh100km: Double? = null,
    val energyMotorwayKwh100km: Double? = null,
) {
    /** Every schema key, in order, with JSON null for an unknown value. */
    fun toJsonObject(): JsonObject = buildJsonObject {
        put("make", make)
        put("model", model)
        put("variant", variant)
        put("year", year)
        put("fuelType", fuelType?.name)
        put("kind", kind?.name)
        put("emissionStandard", emissionStandard)
        put("engineDisplacementCc", engineDisplacementCc)
        put("powerKw", powerKw)
        put("transmission", transmission?.name)
        put("drivetrain", drivetrain?.name)
        put("maxSpeedKph", maxSpeedKph)
        put("lengthM", lengthM)
        put("widthM", widthM)
        put("heightM", heightM)
        put("weightKg", weightKg)
        put("tankL", tankL)
        put("batteryKwh", batteryKwh)
        put("consumptionL100km", consumptionL100km)
        put("consumptionUrbanL100km", consumptionUrbanL100km)
        put("consumptionExtraUrbanL100km", consumptionExtraUrbanL100km)
        put("consumptionMotorwayL100km", consumptionMotorwayL100km)
        put("energyKwh100km", energyKwh100km)
        put("energyUrbanKwh100km", energyUrbanKwh100km)
        put("energyExtraUrbanKwh100km", energyExtraUrbanKwh100km)
        put("energyMotorwayKwh100km", energyMotorwayKwh100km)
    }

    /** The AI-filled fields, in schema order. Identity fields are excluded because the user typed them. */
    fun filledFields(): Set<VehicleAiField> {
        val map = mapOf(
            VehicleAiField.MAKE to (make != null),
            VehicleAiField.MODEL to (model != null),
            VehicleAiField.VARIANT to (variant != null),
            VehicleAiField.YEAR to (year != null),
            VehicleAiField.FUEL_TYPE to (fuelType != null),
            VehicleAiField.KIND to (kind != null),
            VehicleAiField.EMISSION_STANDARD to (emissionStandard != null),
            VehicleAiField.ENGINE_DISPLACEMENT_CC to (engineDisplacementCc != null),
            VehicleAiField.POWER_KW to (powerKw != null),
            VehicleAiField.TRANSMISSION to (transmission != null),
            VehicleAiField.DRIVETRAIN to (drivetrain != null),
            VehicleAiField.MAX_SPEED_KPH to (maxSpeedKph != null),
            VehicleAiField.LENGTH_M to (lengthM != null),
            VehicleAiField.WIDTH_M to (widthM != null),
            VehicleAiField.HEIGHT_M to (heightM != null),
            VehicleAiField.WEIGHT_KG to (weightKg != null),
            VehicleAiField.TANK_L to (tankL != null),
            VehicleAiField.BATTERY_KWH to (batteryKwh != null),
            VehicleAiField.CONSUMPTION_L100KM to (consumptionL100km != null),
            VehicleAiField.CONSUMPTION_URBAN_L100KM to (consumptionUrbanL100km != null),
            VehicleAiField.CONSUMPTION_EXTRA_URBAN_L100KM to (consumptionExtraUrbanL100km != null),
            VehicleAiField.CONSUMPTION_MOTORWAY_L100KM to (consumptionMotorwayL100km != null),
            VehicleAiField.ENERGY_KWH100KM to (energyKwh100km != null),
            VehicleAiField.ENERGY_URBAN_KWH100KM to (energyUrbanKwh100km != null),
            VehicleAiField.ENERGY_EXTRA_URBAN_KWH100KM to (energyExtraUrbanKwh100km != null),
            VehicleAiField.ENERGY_MOTORWAY_KWH100KM to (energyMotorwayKwh100km != null),
        )
        return map.filterValues { it }.keys
    }

    /** The badge fields this answer contributes on top of the identity the user typed. */
    fun aiBadgeFields(): Set<VehicleAiField> = filledFields() - setOf(
        VehicleAiField.MAKE,
        VehicleAiField.MODEL,
        VehicleAiField.YEAR,
        VehicleAiField.FUEL_TYPE,
    ) - buildSet {
        if (transmission == null || transmission == Transmission.UNKNOWN) add(VehicleAiField.TRANSMISSION)
        if (drivetrain == null || drivetrain == Drivetrain.UNKNOWN) add(VehicleAiField.DRIVETRAIN)
    }

    private fun kotlinx.serialization.json.JsonObjectBuilder.put(key: String, value: String?) {
        put(key, value?.let(::JsonPrimitive) ?: JsonNull)
    }

    private fun kotlinx.serialization.json.JsonObjectBuilder.put(key: String, value: Int?) {
        put(key, value?.let(::JsonPrimitive) ?: JsonNull)
    }

    private fun kotlinx.serialization.json.JsonObjectBuilder.put(key: String, value: Double?) {
        put(key, value?.let(::JsonPrimitive) ?: JsonNull)
    }

    companion object {
        val empty = VehicleAiPayload()
    }
}

/**
 * The one schema shared by every provider. It asks for primary data only. Range, payload,
 * acceleration, CO2, costs, toll class, charging time and average driver speeds are derived or
 * behaviour, and stay out.
 */
object VehicleAiSchema {
    val keys: List<String> = VehicleAiField.entries.map { it.key }

    const val VERSION = 2

    /** A readable field list with units and enums for the prompt. */
    val description: String = """
        make: string
        model: string
        variant: string (trim, engine or edition name)
        year: integer (model year, 1886 or later)
        fuelType: one of PETROL, DIESEL, LPG, CNG, HYBRID, PLUG_IN_HYBRID, ELECTRIC
        kind: one of CAR, VAN, CAMPER
        emissionStandard: string (for example "Euro 6d")
        engineDisplacementCc: integer (cubic centimeters)
        powerKw: integer (kilowatts)
        transmission: one of MANUAL, AUTOMATIC, UNKNOWN
        drivetrain: one of FWD, RWD, AWD, UNKNOWN
        maxSpeedKph: integer (kilometers per hour)
        lengthM: number (meters, 3 decimals at most)
        widthM: number (meters, without mirrors)
        heightM: number (meters)
        weightKg: integer (kilograms, ready to drive without payload)
        tankL: number (liters; null for a battery-only vehicle)
        batteryKwh: number (usable kilowatt hours when known)
        consumptionL100km: number (liters per 100 km, overall)
        consumptionUrbanL100km: number (liters per 100 km, urban)
        consumptionExtraUrbanL100km: number (liters per 100 km, extra-urban)
        consumptionMotorwayL100km: number (liters per 100 km, motorway)
        energyKwh100km: number (kilowatt hours per 100 km, overall)
        energyUrbanKwh100km: number (kilowatt hours per 100 km, urban)
        energyExtraUrbanKwh100km: number (kilowatt hours per 100 km, extra-urban)
        energyMotorwayKwh100km: number (kilowatt hours per 100 km, motorway)
    """.trimIndent()

    val emptyJson: String = VehicleAiPayload.empty.toJsonObject().toString()
}
