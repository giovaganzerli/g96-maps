package app.vela.ui.vehicle

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.vela.R
import app.vela.core.model.ConsumptionStandard
import app.vela.core.model.Drivetrain
import app.vela.core.model.FuelType
import app.vela.core.model.Transmission
import app.vela.core.model.VehicleKind

@Composable
internal fun fuelLabel(fuel: FuelType): String = stringResource(
    when (fuel) {
        FuelType.PETROL -> R.string.settings_vehicle_fuel_petrol
        FuelType.DIESEL -> R.string.settings_vehicle_fuel_diesel
        FuelType.LPG -> R.string.settings_vehicle_fuel_lpg
        FuelType.CNG -> R.string.settings_vehicle_fuel_cng
        FuelType.HYBRID -> R.string.settings_vehicle_fuel_hybrid
        FuelType.PLUG_IN_HYBRID -> R.string.settings_vehicle_fuel_plugin
        FuelType.ELECTRIC -> R.string.settings_vehicle_fuel_electric
        FuelType.HYDROGEN -> R.string.settings_vehicle_fuel_hydrogen
        FuelType.OTHER -> R.string.settings_vehicle_fuel_other
    },
)

@Composable
internal fun kindLabel(kind: VehicleKind): String = stringResource(
    when (kind) {
        VehicleKind.CAR -> R.string.settings_vehicle_kind_car
        VehicleKind.VAN -> R.string.settings_vehicle_kind_van
        VehicleKind.CAMPER -> R.string.settings_vehicle_kind_camper
        VehicleKind.TRUCK -> R.string.settings_vehicle_kind_truck
        VehicleKind.OTHER -> R.string.settings_vehicle_kind_other
    },
)

@Composable
internal fun transmissionLabel(transmission: Transmission): String = stringResource(
    when (transmission) {
        Transmission.MANUAL -> R.string.settings_vehicle_transmission_manual
        Transmission.AUTOMATIC -> R.string.settings_vehicle_transmission_automatic
        Transmission.DCT -> R.string.settings_vehicle_transmission_dct
        Transmission.CVT -> R.string.settings_vehicle_transmission_cvt
        Transmission.OTHER -> R.string.settings_vehicle_transmission_other
        Transmission.UNKNOWN -> R.string.settings_vehicle_transmission_unknown
    },
)

@Composable
internal fun drivetrainLabel(drivetrain: Drivetrain): String = stringResource(
    when (drivetrain) {
        Drivetrain.FWD -> R.string.settings_vehicle_drivetrain_fwd
        Drivetrain.RWD -> R.string.settings_vehicle_drivetrain_rwd
        Drivetrain.AWD -> R.string.settings_vehicle_drivetrain_awd
        Drivetrain.OTHER -> R.string.settings_vehicle_drivetrain_other
        Drivetrain.UNKNOWN -> R.string.settings_vehicle_drivetrain_unknown
    },
)

@Composable
internal fun consumptionStandardLabel(standard: ConsumptionStandard): String = stringResource(
    when (standard) {
        ConsumptionStandard.WLTP -> R.string.settings_vehicle_consumption_standard_wltp
        ConsumptionStandard.NEDC -> R.string.settings_vehicle_consumption_standard_nedc
        ConsumptionStandard.EPA -> R.string.settings_vehicle_consumption_standard_epa
        ConsumptionStandard.UNKNOWN -> R.string.settings_vehicle_consumption_standard_unknown
    },
)
