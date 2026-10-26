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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/**
 * The main vehicle data and the weight a recalculated consumption answer is based on. It has no
 * plate, notes or average-speed fields: only the data that changes consumption crosses to a
 * provider. [variant] is redacted before the prompt is built.
 */
data class VehicleConsumptionInput(
    val make: String,
    val model: String,
    val year: Int,
    val kind: VehicleKind,
    val fuelType: FuelType,
    val weightKg: Int,
    val variant: String = "",
    val emissionStandard: String? = null,
    val engineDisplacementCc: Int? = null,
    val powerKw: Int? = null,
    val transmission: Transmission? = null,
    val drivetrain: Drivetrain? = null,
    val maxSpeedKph: Int? = null,
    val lengthM: Double? = null,
    val widthM: Double? = null,
    val heightM: Double? = null,
    /** The weight the current consumption figures refer to, when the editor knows it. */
    val referenceWeightKg: Int? = null,
    val tankL: Double? = null,
    val batteryKwh: Double? = null,
    val consumptionStandard: ConsumptionStandard = ConsumptionStandard.UNKNOWN,
    val currentConsumptionL100km: Double? = null,
    val currentConsumptionUrbanL100km: Double? = null,
    val currentConsumptionExtraUrbanL100km: Double? = null,
    val currentConsumptionMotorwayL100km: Double? = null,
    val currentEnergyKwh100km: Double? = null,
    val currentEnergyUrbanKwh100km: Double? = null,
    val currentEnergyExtraUrbanKwh100km: Double? = null,
    val currentEnergyMotorwayKwh100km: Double? = null,
) {
    fun isComplete(): Boolean =
        make.isNotBlank() &&
            model.isNotBlank() &&
            year in 1886..2100 &&
            weightKg in 1..100_000
}

/** One recalculated answer. Only the consumption keys are present, never the rest of the vehicle. */
data class VehicleConsumptionPayload(
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
        put("consumptionL100km", consumptionL100km)
        put("consumptionUrbanL100km", consumptionUrbanL100km)
        put("consumptionExtraUrbanL100km", consumptionExtraUrbanL100km)
        put("consumptionMotorwayL100km", consumptionMotorwayL100km)
        put("energyKwh100km", energyKwh100km)
        put("energyUrbanKwh100km", energyUrbanKwh100km)
        put("energyExtraUrbanKwh100km", energyExtraUrbanKwh100km)
        put("energyMotorwayKwh100km", energyMotorwayKwh100km)
    }

    fun filledFields(): Set<VehicleAiField> = buildSet {
        if (consumptionL100km != null) add(VehicleAiField.CONSUMPTION_L100KM)
        if (consumptionUrbanL100km != null) add(VehicleAiField.CONSUMPTION_URBAN_L100KM)
        if (consumptionExtraUrbanL100km != null) add(VehicleAiField.CONSUMPTION_EXTRA_URBAN_L100KM)
        if (consumptionMotorwayL100km != null) add(VehicleAiField.CONSUMPTION_MOTORWAY_L100KM)
        if (energyKwh100km != null) add(VehicleAiField.ENERGY_KWH100KM)
        if (energyUrbanKwh100km != null) add(VehicleAiField.ENERGY_URBAN_KWH100KM)
        if (energyExtraUrbanKwh100km != null) add(VehicleAiField.ENERGY_EXTRA_URBAN_KWH100KM)
        if (energyMotorwayKwh100km != null) add(VehicleAiField.ENERGY_MOTORWAY_KWH100KM)
    }

    fun isEmpty(): Boolean = filledFields().isEmpty()

    private fun kotlinx.serialization.json.JsonObjectBuilder.put(key: String, value: Double?) {
        put(key, value?.let(::JsonPrimitive) ?: JsonNull)
    }
}

/** The consumption-only schema. It deliberately has no other vehicle fields. */
object VehicleConsumptionSchema {
    val keys: List<String> = listOf(
        "consumptionL100km",
        "consumptionUrbanL100km",
        "consumptionExtraUrbanL100km",
        "consumptionMotorwayL100km",
        "energyKwh100km",
        "energyUrbanKwh100km",
        "energyExtraUrbanKwh100km",
        "energyMotorwayKwh100km",
    )

    const val VERSION = 1

    /** A readable field list with units for the prompt. */
    val description: String = """
        consumptionL100km: number (liters per 100 km, overall)
        consumptionUrbanL100km: number (liters per 100 km, urban)
        consumptionExtraUrbanL100km: number (liters per 100 km, extra-urban)
        consumptionMotorwayL100km: number (liters per 100 km, motorway)
        energyKwh100km: number (kilowatt hours per 100 km, overall)
        energyUrbanKwh100km: number (kilowatt hours per 100 km, urban)
        energyExtraUrbanKwh100km: number (kilowatt hours per 100 km, extra-urban)
        energyMotorwayKwh100km: number (kilowatt hours per 100 km, motorway)
    """.trimIndent()

    val emptyJson: String = VehicleConsumptionPayload().toJsonObject().toString()
}

/** The prompt that recalculates the eight consumption figures after a weight change. */
object VehicleConsumptionPrompt {
    val system: String = """
        You recalculate the average consumption of one road vehicle after its weight changed.
        Answer with one JSON object only. Do not wrap it in prose or a code fence.

        The object must contain every key below. Use null when a figure does not apply or when the
        available data cannot support an estimate. Do not add keys. Numbers use the units stated
        here.

        ${VehicleConsumptionSchema.description}

        Rules:
        - Use the vehicle data and the two weights in the user message.
        - The current figures are the reference at referenceWeightKg. Recalculate them for newWeightKg,
          keeping the same cycle, tires, load distribution, driving style and conditions. Weight
          changes mainly affect rolling resistance and acceleration, so do not copy the old figures
          unchanged when the weights differ.
        - If referenceWeightKg or a current figure is missing, estimate that figure for this vehicle
          at newWeightKg from its make, model, variant, engine, power, transmission, drivetrain,
          dimensions and fuel type.
        - Return fuel figures only when fuelConsumptionApplies is true, and electric figures only
          when electricConsumptionApplies is true. Use null for a figure that does not apply.
        - Keep every figure realistic and round it to one decimal place. Do not return a range,
          text, unit or explanation. Do not return any other vehicle data.
    """.trimIndent()

    fun user(input: VehicleConsumptionInput): String = buildString {
        appendLine("Vehicle details from the editor:")
        appendLine("make: ${input.make.trim()}")
        appendLine("model: ${input.model.trim()}")
        if (input.variant.isNotBlank()) appendLine("variant: ${input.variant.trim()}")
        appendLine("year: ${input.year}")
        appendLine("kind: ${input.kind.name}")
        appendLine("fuelType: ${input.fuelType.name}")
        input.emissionStandard?.takeIf { it.isNotBlank() }?.let { appendLine("emissionStandard: $it") }
        input.engineDisplacementCc?.let { appendLine("engineDisplacementCc: $it") }
        input.powerKw?.let { appendLine("powerKw: $it") }
        input.transmission?.let { appendLine("transmission: ${it.name}") }
        input.drivetrain?.let { appendLine("drivetrain: ${it.name}") }
        input.maxSpeedKph?.let { appendLine("maxSpeedKph: $it") }
        input.lengthM?.let { appendLine("lengthM: $it") }
        input.widthM?.let { appendLine("widthM: $it") }
        input.heightM?.let { appendLine("heightM: $it") }
        input.tankL?.let { appendLine("tankL: $it") }
        input.batteryKwh?.let { appendLine("batteryKwh: $it") }
        if (input.consumptionStandard != ConsumptionStandard.UNKNOWN) {
            appendLine("consumptionStandard: ${input.consumptionStandard.name}")
        }
        appendLine("referenceWeightKg: ${input.referenceWeightKg ?: "unknown"}")
        appendLine("newWeightKg: ${input.weightKg}")
        appendLine("fuelConsumptionApplies: ${input.fuelType != FuelType.ELECTRIC}")
        appendLine(
            "electricConsumptionApplies: " +
                "${input.fuelType == FuelType.PLUG_IN_HYBRID || input.fuelType == FuelType.ELECTRIC}",
        )
        input.currentConsumptionL100km?.let { appendLine("currentConsumptionL100km: $it") }
        input.currentConsumptionUrbanL100km?.let { appendLine("currentConsumptionUrbanL100km: $it") }
        input.currentConsumptionExtraUrbanL100km?.let {
            appendLine("currentConsumptionExtraUrbanL100km: $it")
        }
        input.currentConsumptionMotorwayL100km?.let { appendLine("currentConsumptionMotorwayL100km: $it") }
        input.currentEnergyKwh100km?.let { appendLine("currentEnergyKwh100km: $it") }
        input.currentEnergyUrbanKwh100km?.let { appendLine("currentEnergyUrbanKwh100km: $it") }
        input.currentEnergyExtraUrbanKwh100km?.let {
            appendLine("currentEnergyExtraUrbanKwh100km: $it")
        }
        input.currentEnergyMotorwayKwh100km?.let { appendLine("currentEnergyMotorwayKwh100km: $it") }
        appendLine()
        append("Return the JSON object now.")
    }
}

/**
 * Reads the consumption-only JSON shape. It is as forgiving as [VehicleAiParser] about wrapping,
 * code fences and quoted numbers, and turns an unknown or implausible value into null.
 */
object VehicleConsumptionParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): VehicleConsumptionPayload? {
        val objectText = extractObject(text) ?: return null
        val root = runCatching { json.parseToJsonElement(objectText).jsonObject }.getOrNull() ?: return null
        return parse(root)
    }

    fun parse(root: JsonObject): VehicleConsumptionPayload {
        val source = unwrap(root)
        return VehicleConsumptionPayload(
            consumptionL100km = source.double("consumptionL100km", 0.1, 100.0),
            consumptionUrbanL100km = source.double("consumptionUrbanL100km", 0.1, 100.0),
            consumptionExtraUrbanL100km = source.double("consumptionExtraUrbanL100km", 0.1, 100.0),
            consumptionMotorwayL100km = source.double("consumptionMotorwayL100km", 0.1, 100.0),
            energyKwh100km = source.double("energyKwh100km", 0.1, 200.0),
            energyUrbanKwh100km = source.double("energyUrbanKwh100km", 0.1, 200.0),
            energyExtraUrbanKwh100km = source.double("energyExtraUrbanKwh100km", 0.1, 200.0),
            energyMotorwayKwh100km = source.double("energyMotorwayKwh100km", 0.1, 200.0),
        )
    }

    /** Some models wrap the answer in {"consumption": {...}} or {"result": {...}}. */
    private fun unwrap(root: JsonObject): JsonObject {
        if (VehicleConsumptionSchema.keys.any { root.containsKey(it) }) return root
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

    private fun JsonObject.double(key: String, min: Double, max: Double): Double? {
        val raw = primitive(key)?.content?.trim()?.replace(',', '.') ?: return null
        return raw.toDoubleOrNull()?.takeIf { it in min..max }
    }
}
