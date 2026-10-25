package app.vela.core.ai

import app.vela.core.model.FuelType

/**
 * The identity the user typed before asking AI. It has no plate field: the plate is private and
 * never crosses into this package.
 */
data class VehicleAiCriteria(
    val make: String,
    val model: String,
    val year: Int,
    val fuelType: FuelType,
) {
    fun isComplete(): Boolean =
        make.isNotBlank() && model.isNotBlank() && year in 1886..2100
}

/** The answer after the typed identity was restored over the provider's values. */
data class VehicleAiAnswer(
    val payload: VehicleAiPayload,
    val conflicts: Set<VehicleAiIdentityField>,
    val redactedPlateCount: Int,
) {
    val badgeFields: Set<VehicleAiField> get() = payload.aiBadgeFields()
}

object VehicleAiMerge {
    /**
     * Keeps the user's make, model, year and fuel whatever the provider said. A different
     * non-null value is reported as a conflict so the editor can warn, but never applied.
     */
    fun merge(criteria: VehicleAiCriteria, payload: VehicleAiPayload): VehicleAiAnswer {
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
            ),
            conflicts = conflicts,
            redactedPlateCount = 0,
        )
    }
}
