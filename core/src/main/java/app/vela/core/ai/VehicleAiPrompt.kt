package app.vela.core.ai

/**
 * The vehicle-search prompt every provider receives. The structured identity is a hard
 * constraint; the version/trim text only refines variant, engine and equipment. Derived values
 * and driver behavior are explicitly excluded.
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
          keys even if the text mentions another vehicle.
        - If a variant is supplied, return it unchanged. Otherwise infer the version or trim.
        - Return primary manufacturer data only. Do not return derived values: range or electric
          range, payload, power-to-weight ratio, acceleration, CO2, cost per kilometer, trip cost,
          toll class, charging time or ETA.
        - Do not return average driver speeds or real-world trip averages. Those are not
          manufacturer data.
        - Use one of the listed enum values. When no listed value fits, use null (or UNKNOWN where
          the schema allows it).
        - batteryKwh is the usable capacity when known. Use tankL only for a vehicle with a fuel
          tank. Return only the consumption figures the manufacturer publishes.
        - A value you are not confident about is null, not a guess.
    """.trimIndent()

    fun user(criteria: VehicleAiCriteria): String = buildString {
        appendLine("Vehicle details from the driver:")
        appendLine("make: ${criteria.make.trim()}")
        appendLine("model: ${criteria.model.trim()}")
        appendLine("year: ${criteria.year}")
        appendLine("fuelType: ${criteria.fuelType.name}")
        if (criteria.variant.isNotBlank()) {
            appendLine("variant: ${criteria.variant.trim()}")
        }
        appendLine()
        append("Return the JSON object now.")
    }
}
