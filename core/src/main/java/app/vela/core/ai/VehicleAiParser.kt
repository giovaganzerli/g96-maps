package app.vela.core.ai

import app.vela.core.model.ConsumptionStandard
import app.vela.core.model.Drivetrain
import app.vela.core.model.FuelType
import app.vela.core.model.Transmission
import app.vela.core.model.VehicleKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Reads the one JSON shape every provider returns. The parser is deliberately forgiving about
 * wrapping, code fences and quoted numbers, and strict about what the value means: a key it does
 * not know is ignored, an unknown enum or an implausible number becomes null.
 */
object VehicleAiParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): VehicleAiPayload? {
        val objectText = extractObject(text) ?: return null
        val root = runCatching { json.parseToJsonElement(objectText).jsonObject }.getOrNull() ?: return null
        return parse(root)
    }

    fun parse(root: JsonObject): VehicleAiPayload {
        val source = unwrap(root)
        return VehicleAiPayload(
            make = source.string("make", 80),
            model = source.string("model", 80),
            variant = source.string("variant", 120),
            year = source.int("year", 1886, 2100),
            fuelType = source.enum("fuelType", ::fuelType),
            kind = source.enum("kind", ::vehicleKind),
            emissionStandard = source.string("emissionStandard", 40),
            engineDisplacementCc = source.int("engineDisplacementCc", 1, 20_000),
            powerKw = source.int("powerKw", 1, 2_000),
            transmission = source.enum("transmission", ::transmission),
            drivetrain = source.enum("drivetrain", ::drivetrain),
            maxSpeedKph = source.int("maxSpeedKph", 1, 1_000),
            lengthM = source.double("lengthM", 0.5, 30.0),
            widthM = source.double("widthM", 0.5, 30.0),
            widthWithMirrorsM = source.double("widthWithMirrorsM", 0.5, 30.0),
            heightM = source.double("heightM", 0.5, 30.0),
            weightKg = source.int("weightKg", 1, 100_000),
            grossWeightKg = source.int("grossWeightKg", 1, 100_000),
            axleLoadKg = source.int("axleLoadKg", 1, 100_000),
            axleCount = source.int("axleCount", 1, 10),
            tankL = source.double("tankL", 1.0, 2_000.0),
            batteryKwh = source.double("batteryKwh", 0.1, 2_000.0),
            consumptionL100km = source.double("consumptionL100km", 0.1, 100.0),
            energyKwh100km = source.double("energyKwh100km", 0.1, 200.0),
            consumptionStandard = source.enum("consumptionStandard", ::consumptionStandard),
            maxChargingPowerKw = source.double("maxChargingPowerKw", 0.1, 2_000.0),
            chargingConnector = source.string("chargingConnector", 40),
        )
    }

    /** The model sometimes answers with {"vehicle": {...}} or {"result": {...}}. */
    private fun unwrap(root: JsonObject): JsonObject {
        if (VehicleAiSchema.keys.any { root.containsKey(it) }) return root
        val nested = root.values.filterIsInstance<JsonObject>().singleOrNull()
        return nested ?: root
    }

    private fun extractObject(text: String): String? {
        var body = text.trim()
        if (body.startsWith("```")) {
            body = body.substringAfter('\n', "").trim()
            if (body.endsWith("```")) body = body.dropLast(3).trim()
        }
        val start = body.indexOf('{')
        val end = body.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return body.substring(start, end + 1)
    }

    private fun JsonObject.primitive(key: String): JsonPrimitive? =
        (this[key] as? JsonPrimitive)
            ?.takeIf { it !is JsonNull && !(it.isString && it.content.isBlank()) }

    private fun JsonObject.string(key: String, maxLength: Int): String? =
        primitive(key)?.content?.trim()?.takeIf { it.isNotEmpty() }?.take(maxLength)

    private fun JsonObject.int(key: String, min: Int, max: Int): Int? {
        val raw = primitive(key)?.content?.trim()?.replace(',', '.') ?: return null
        val value = raw.toIntOrNull() ?: raw.toDoubleOrNull()?.takeIf { it % 1.0 == 0.0 }?.toInt()
        return value?.takeIf { it in min..max }
    }

    private fun JsonObject.double(key: String, min: Double, max: Double): Double? {
        val raw = primitive(key)?.content?.trim()?.replace(',', '.') ?: return null
        return raw.toDoubleOrNull()?.takeIf { it in min..max }
    }

    private fun <T> JsonObject.enum(key: String, parse: (String?) -> T?): T? =
        parse(primitive(key)?.content)

    private fun fuelType(raw: String?): FuelType? = when (normalize(raw)) {
        "PETROL", "GASOLINE", "BENZINA" -> FuelType.PETROL
        "DIESEL", "GASOLIO" -> FuelType.DIESEL
        "LPG", "GPL", "AUTOGAS" -> FuelType.LPG
        "CNG", "METANO", "NATURAL_GAS", "METHANE" -> FuelType.CNG
        "HYBRID", "HEV", "MILD_HYBRID", "FULL_HYBRID", "IBRIDO", "IBRIDA" -> FuelType.HYBRID
        "PLUG_IN_HYBRID", "PLUGIN_HYBRID", "PHEV", "PLUG_IN", "IBRIDO_PLUG_IN", "IBRIDA_PLUG_IN" -> FuelType.PLUG_IN_HYBRID
        "ELECTRIC", "EV", "BEV", "ELETTRICO", "ELETTRICA" -> FuelType.ELECTRIC
        "HYDROGEN", "H2", "IDROGENO" -> FuelType.HYDROGEN
        "OTHER", "ALTRO", "ALTRA" -> FuelType.OTHER
        else -> null
    }

    private fun vehicleKind(raw: String?): VehicleKind? = when (normalize(raw)) {
        "CAR", "AUTO", "AUTOMOBILE", "AUTOVETTURA" -> VehicleKind.CAR
        "VAN", "FURGONE", "FURGONCINO" -> VehicleKind.VAN
        "CAMPER", "MOTORHOME", "MOTOR_HOME" -> VehicleKind.CAMPER
        "TRUCK", "CAMION", "LORRY", "AUTOCARRO" -> VehicleKind.TRUCK
        "OTHER", "ALTRO" -> VehicleKind.OTHER
        else -> null
    }

    private fun transmission(raw: String?): Transmission? = when (normalize(raw)) {
        "MANUAL", "MANUALE", "MT", "MECHANICAL" -> Transmission.MANUAL
        "AUTOMATIC", "AUTOMATICO", "AUTOMATICA", "AT", "AUTOMATED_MANUAL" -> Transmission.AUTOMATIC
        "DCT", "DSG", "DUAL_CLUTCH", "DOPPIA_FRIZIONE" -> Transmission.DCT
        "CVT", "VARIABLE" -> Transmission.CVT
        "OTHER", "ALTRO" -> Transmission.OTHER
        "UNKNOWN", "SCONOSCIUTO" -> Transmission.UNKNOWN
        else -> null
    }

    private fun drivetrain(raw: String?): Drivetrain? = when (normalize(raw)) {
        "FWD", "FRONT", "FRONT_WHEEL_DRIVE", "2WD", "TRAZIONE_ANTERIORE" -> Drivetrain.FWD
        "RWD", "REAR", "REAR_WHEEL_DRIVE", "TRAZIONE_POSTERIORE" -> Drivetrain.RWD
        "AWD", "4WD", "ALL_WHEEL_DRIVE", "4X4", "INTEGRALE", "TRAZIONE_INTEGRALE" -> Drivetrain.AWD
        "OTHER", "ALTRO" -> Drivetrain.OTHER
        "UNKNOWN", "SCONOSCIUTO" -> Drivetrain.UNKNOWN
        else -> null
    }

    private fun consumptionStandard(raw: String?): ConsumptionStandard? = when (normalize(raw)) {
        "WLTP" -> ConsumptionStandard.WLTP
        "NEDC" -> ConsumptionStandard.NEDC
        "EPA" -> ConsumptionStandard.EPA
        "UNKNOWN", "SCONOSCIUTO", "ND" -> ConsumptionStandard.UNKNOWN
        else -> null
    }

    private fun normalize(raw: String?): String =
        raw.orEmpty().trim().uppercase().replace('-', '_').replace(' ', '_').replace("/", "_")
}
