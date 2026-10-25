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

/** The gearbox. UNKNOWN is the default until the user or an AI search fills it. */
@Serializable
enum class Transmission { MANUAL, AUTOMATIC, DCT, CVT, OTHER, UNKNOWN }

/** Which wheels drive the vehicle. UNKNOWN is the default. */
@Serializable
enum class Drivetrain { FWD, RWD, AWD, OTHER, UNKNOWN }

/** The cycle a consumption figure was measured on. UNKNOWN is the default. */
@Serializable
enum class ConsumptionStandard { WLTP, NEDC, EPA, UNKNOWN }

/**
 * One vehicle in the local garage. Every field except [id] is defaulted so a payload written by an
 * older build still decodes. The route picker only reads the display fields today; the dimensions,
 * weights and consumption are kept for vehicle-aware routing and trip costs.
 *
 * [nickname] is retired from the UI: it stays only so a garage saved by an older build still
 * decodes. New vehicles leave it blank.
 */
@Serializable
data class VehicleProfile(
    val id: String,
    val nickname: String = "",
    val kind: VehicleKind = VehicleKind.CAR,
    val make: String = "",
    val model: String = "",
    val variant: String = "",
    val year: Int? = null,
    val fuelType: FuelType = FuelType.PETROL,
    val routingMode: VehicleRoutingMode = VehicleRoutingMode.AUTO,
    /** Optional plate. Kept on this phone and outside Android backup. */
    val plate: String = "",
    val emissionStandard: String = "",
    val engineDisplacementCc: Int? = null,
    val powerKw: Int? = null,
    val transmission: Transmission = Transmission.UNKNOWN,
    val drivetrain: Drivetrain = Drivetrain.UNKNOWN,
    val maxSpeedKph: Int? = null,
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
    val consumptionStandard: ConsumptionStandard = ConsumptionStandard.UNKNOWN,
    val batteryKwh: Double? = null,
    val tankL: Double? = null,
    val maxChargingPowerKw: Double? = null,
    val chargingConnector: String = "",
    val notes: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    /**
     * The name the garage and the route picker show: make and model, then the plate when one was
     * entered. A plate is private and is never sent to an AI provider.
     */
    val displayName: String
        get() {
            val base = listOf(make.trim(), model.trim()).filter { it.isNotBlank() }.joinToString(" ")
                .ifBlank { "Vehicle" }
            val trimmedPlate = plate.trim()
            return if (trimmedPlate.isBlank()) base else "$base · $trimmedPlate"
        }
}
