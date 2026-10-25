package app.vela.core.model

import kotlinx.serialization.Serializable

/** What a garage entry is, for the editor and for later routing rules. */
@Serializable
enum class VehicleKind { CAR, VAN, CAMPER, TRUCK, OTHER }

/** The fuel or energy the vehicle uses. A hybrid may carry both fuel and electric figures. */
@Serializable
enum class FuelType { PETROL, DIESEL, LPG, CNG, HYBRID, PLUG_IN_HYBRID, ELECTRIC, HYDROGEN, OTHER }

/** How routes should treat the vehicle. Truck-aware routing is not used yet. */
@Serializable
enum class VehicleRoutingMode { AUTO, TRUCK }

/**
 * One vehicle in the local garage. Every field except [id] is defaulted so a payload written by an
 * older build still decodes. The route picker only reads the display fields today; the dimensions,
 * weights and consumption are kept for vehicle-aware routing and trip costs.
 */
@Serializable
data class VehicleProfile(
    val id: String,
    val nickname: String = "",
    val kind: VehicleKind = VehicleKind.CAR,
    val make: String = "",
    val model: String = "",
    val year: Int? = null,
    val fuelType: FuelType = FuelType.PETROL,
    val routingMode: VehicleRoutingMode = VehicleRoutingMode.AUTO,
    /** Optional plate. Kept on this phone and outside Android backup. */
    val plate: String = "",
    val heightM: Double? = null,
    val widthM: Double? = null,
    val widthWithMirrorsM: Double? = null,
    val lengthM: Double? = null,
    val weightKg: Int? = null,
    val grossWeightKg: Int? = null,
    val axleLoadKg: Int? = null,
    val axleCount: Int? = null,
    val consumptionL100km: Double? = null,
    val energyKwh100km: Double? = null,
    val batteryKwh: Double? = null,
    val tankL: Double? = null,
    val notes: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    /** The name the garage and the route picker show. Falls back to make and model, then Vehicle. */
    val displayName: String
        get() = nickname.trim().ifBlank {
            listOf(make.trim(), model.trim()).filter { it.isNotBlank() }.joinToString(" ")
                .ifBlank { "Vehicle" }
        }
}
