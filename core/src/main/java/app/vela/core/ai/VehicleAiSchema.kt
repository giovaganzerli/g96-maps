package app.vela.core.ai

import app.vela.core.model.ConsumptionStandard
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
    WIDTH_WITH_MIRRORS_M("widthWithMirrorsM"),
    HEIGHT_M("heightM"),
    WEIGHT_KG("weightKg"),
    GROSS_WEIGHT_KG("grossWeightKg"),
    AXLE_LOAD_KG("axleLoadKg"),
    AXLE_COUNT("axleCount"),
    TANK_L("tankL"),
    BATTERY_KWH("batteryKwh"),
    CONSUMPTION_L100KM("consumptionL100km"),
    ENERGY_KWH100KM("energyKwh100km"),
    CONSUMPTION_STANDARD("consumptionStandard"),
    MAX_CHARGING_POWER_KW("maxChargingPowerKw"),
    CHARGING_CONNECTOR("chargingConnector"),
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
    val widthWithMirrorsM: Double? = null,
    val heightM: Double? = null,
    val weightKg: Int? = null,
    val grossWeightKg: Int? = null,
    val axleLoadKg: Int? = null,
    val axleCount: Int? = null,
    val tankL: Double? = null,
    val batteryKwh: Double? = null,
    val consumptionL100km: Double? = null,
    val energyKwh100km: Double? = null,
    val consumptionStandard: ConsumptionStandard? = null,
    val maxChargingPowerKw: Double? = null,
    val chargingConnector: String? = null,
) {
    /** All 27 keys, in schema order, with JSON null for an unknown value. */
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
        put("widthWithMirrorsM", widthWithMirrorsM)
        put("heightM", heightM)
        put("weightKg", weightKg)
        put("grossWeightKg", grossWeightKg)
        put("axleLoadKg", axleLoadKg)
        put("axleCount", axleCount)
        put("tankL", tankL)
        put("batteryKwh", batteryKwh)
        put("consumptionL100km", consumptionL100km)
        put("energyKwh100km", energyKwh100km)
        put("consumptionStandard", consumptionStandard?.name)
        put("maxChargingPowerKw", maxChargingPowerKw)
        put("chargingConnector", chargingConnector)
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
            VehicleAiField.WIDTH_WITH_MIRRORS_M to (widthWithMirrorsM != null),
            VehicleAiField.HEIGHT_M to (heightM != null),
            VehicleAiField.WEIGHT_KG to (weightKg != null),
            VehicleAiField.GROSS_WEIGHT_KG to (grossWeightKg != null),
            VehicleAiField.AXLE_LOAD_KG to (axleLoadKg != null),
            VehicleAiField.AXLE_COUNT to (axleCount != null),
            VehicleAiField.TANK_L to (tankL != null),
            VehicleAiField.BATTERY_KWH to (batteryKwh != null),
            VehicleAiField.CONSUMPTION_L100KM to (consumptionL100km != null),
            VehicleAiField.ENERGY_KWH100KM to (energyKwh100km != null),
            VehicleAiField.CONSUMPTION_STANDARD to (consumptionStandard != null),
            VehicleAiField.MAX_CHARGING_POWER_KW to (maxChargingPowerKw != null),
            VehicleAiField.CHARGING_CONNECTOR to (chargingConnector != null),
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
        if (consumptionStandard == null || consumptionStandard == ConsumptionStandard.UNKNOWN) {
            add(VehicleAiField.CONSUMPTION_STANDARD)
        }
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
 * acceleration, CO2, costs, toll class and charging time are derived and stay out.
 */
object VehicleAiSchema {
    val keys: List<String> = VehicleAiField.entries.map { it.key }

    const val VERSION = 1

    /** A readable field list with units and enums for the prompt. */
    val description: String = """
        make: string
        model: string
        variant: string (trim, engine or edition name)
        year: integer (model year, 1886 or later)
        fuelType: one of PETROL, DIESEL, LPG, CNG, HYBRID, PLUG_IN_HYBRID, ELECTRIC, HYDROGEN, OTHER
        kind: one of CAR, VAN, CAMPER, TRUCK, OTHER
        emissionStandard: string (for example "Euro 6d")
        engineDisplacementCc: integer (cubic centimeters)
        powerKw: integer (kilowatts)
        transmission: one of MANUAL, AUTOMATIC, DCT, CVT, OTHER, UNKNOWN
        drivetrain: one of FWD, RWD, AWD, OTHER, UNKNOWN
        maxSpeedKph: integer (kilometers per hour)
        lengthM: number (meters, 3 decimals at most)
        widthM: number (meters, without mirrors)
        widthWithMirrorsM: number (meters, with mirrors)
        heightM: number (meters)
        weightKg: integer (kilograms, ready to drive without payload)
        grossWeightKg: integer (kilograms, maximum permitted)
        axleLoadKg: integer (kilograms, maximum axle load)
        axleCount: integer
        tankL: number (liters)
        batteryKwh: number (usable kilowatt hours when known)
        consumptionL100km: number (liters per 100 km)
        energyKwh100km: number (kilowatt hours per 100 km)
        consumptionStandard: one of WLTP, NEDC, EPA, UNKNOWN
        maxChargingPowerKw: number (kilowatts)
        chargingConnector: string (for example "Type 2", "CCS2", "CHAdeMO")
    """.trimIndent()

    val emptyJson: String = VehicleAiPayload.empty.toJsonObject().toString()
}
