package app.vela.core.ai

/**
 * The one prompt every provider receives. The structured identity is a hard constraint; the free
 * text only refines variant, engine and equipment. Derived values are explicitly excluded.
 */
object VehicleAiPrompt {
    val system: String = """
        You identify a single road vehicle from the details a driver gives you.
        Answer with one JSON object only. Do not wrap it in prose or a code fence.

        The object must contain every key below. Use null when you do not know a value; do not
        invent one. Do not add keys. Numbers must use the units stated here.

        ${VehicleAiSchema.description}

        Rules:
        - The driver's make, model, year and fuelType are fixed. Return them unchanged in those
          keys even if the free text mentions another vehicle.
        - Return primary manufacturer data only. Do not return derived values: range or electric
          range, payload, power-to-weight ratio, acceleration, CO2, cost per kilometer, trip cost,
          toll class, charging time or ETA.
        - Use one of the listed enum values. When no listed value fits, use null (or UNKNOWN where
          the schema allows it).
        - batteryKwh is the usable capacity when known. axleLoadKg is the maximum axle load.
        - A value you are not confident about is null, not a guess.
    """.trimIndent()

    fun user(criteria: VehicleAiCriteria, redactedQuery: String): String = buildString {
        appendLine("Vehicle details from the driver:")
        appendLine("make: ${criteria.make.trim()}")
        appendLine("model: ${criteria.model.trim()}")
        appendLine("year: ${criteria.year}")
        appendLine("fuelType: ${criteria.fuelType.name}")
        if (redactedQuery.isNotBlank()) {
            appendLine()
            appendLine("Free-text refinement (may contain no plate; any plate was already removed):")
            appendLine(redactedQuery.trim())
        }
        appendLine()
        append("Return the JSON object now.")
    }
}
