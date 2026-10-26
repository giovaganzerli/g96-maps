package app.vela.core.ai

import app.vela.core.model.FuelType

/**
 * The identity the user typed before asking AI. It has no plate field: the plate is private and
 * never crosses into this package. [variant] is the version/trim field, which may also carry any
 * detail that helps identify the vehicle; it is redacted before it reaches a provider.
 */
data class VehicleAiCriteria(
    val make: String,
    val model: String,
    val year: Int,
    val fuelType: FuelType,
    val variant: String = "",
) {
    fun isComplete(): Boolean =
        make.isNotBlank() && model.isNotBlank() && year in 1886..2100
}

/** The answer after the typed identity was restored over the provider's values. */
data class VehicleAiAnswer(
    val payload: VehicleAiPayload,
    val conflicts: Set<VehicleAiIdentityField>,
    val redactedPlateCount: Int,
    /** True when the version/trim came from the user, so it must not carry an AI badge. */
    val typedVariant: Boolean = false,
) {
    val badgeFields: Set<VehicleAiField>
        get() = payload.aiBadgeFields().let { fields ->
            if (typedVariant) fields - VehicleAiField.VARIANT else fields
        }
}

object VehicleAiMerge {
    /**
     * Keeps the user's make, model, year, fuel and typed variant whatever the provider said. A
     * different identity value is reported as a conflict so the editor can warn, but never applied.
     */
    fun merge(criteria: VehicleAiCriteria, payload: VehicleAiPayload): VehicleAiAnswer {
        val typedVariant = criteria.variant.trim()
        val conflicts = buildSet {
            if (payload.make != null && !payload.make.equals(criteria.make.trim(), ignoreCase = true)) {
                add(VehicleAiIdentityField.MAKE)
            }
            if (payload.model != null && !payload.model.equals(criteria.model.trim(), ignoreCase = true)) {
                add(VehicleAiIdentityField.MODEL)
            }
            if (payload.year != null && payload.year != criteria.year) {
                add(VehicleAiIdentityField.YEAR)
            }
            if (payload.fuelType != null && payload.fuelType != criteria.fuelType) {
                add(VehicleAiIdentityField.FUEL_TYPE)
            }
        }
        return VehicleAiAnswer(
            payload = payload.copy(
                make = criteria.make.trim(),
                model = criteria.model.trim(),
                year = criteria.year,
                fuelType = criteria.fuelType,
                variant = typedVariant.ifBlank { payload.variant },
            ),
            conflicts = conflicts,
            redactedPlateCount = 0,
            typedVariant = typedVariant.isNotBlank(),
        )
    }
}
