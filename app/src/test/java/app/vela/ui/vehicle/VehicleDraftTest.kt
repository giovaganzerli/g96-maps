package app.vela.ui.vehicle

import app.vela.core.ai.VehicleAiCriteria
import app.vela.core.ai.VehicleAiField
import app.vela.core.ai.VehicleAiPayload
import app.vela.core.ai.VehicleConsumptionPayload
import app.vela.core.model.ConsumptionStandard
import app.vela.core.model.Drivetrain
import app.vela.core.model.FuelType
import app.vela.core.model.Transmission
import app.vela.core.model.VehicleKind
import app.vela.core.model.VehicleProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleDraftTest {
    private val complete = VehicleDraft.newManual().copy(
        make = "Fiat",
        model = "Panda",
        year = "2020",
        fuelType = FuelType.PETROL,
        kind = VehicleKind.CAR,
    )

    @Test
    fun `a new manual vehicle starts without identity`() {
        val draft = VehicleDraft.newManual()

        assertNull(draft.fuelType)
        assertNull(draft.kind)
        assertFalse(draft.isValid())
    }

    @Test
    fun `make model year fuel and kind are all required`() {
        assertTrue(complete.isValid())
        assertFalse(complete.copy(make = " ").isValid())
        assertFalse(complete.copy(model = "").isValid())
        assertFalse(complete.copy(year = "").isValid())
        assertFalse(complete.copy(year = "1200").isValid())
        assertFalse(complete.copy(fuelType = null).isValid())
        assertFalse(complete.copy(kind = null).isValid())
    }

    @Test
    fun `numbers must parse and stay plausible`() {
        assertFalse(complete.copy(powerKw = "abc").isValid())
        assertFalse(complete.copy(weightKg = "999999").isValid())
        assertFalse(complete.copy(lengthM = "-1").isValid())
        assertFalse(complete.copy(lengthM = "Infinity").isValid())
        assertFalse(complete.copy(averageSpeedKph = "1001").isValid())
        assertTrue(complete.copy(lengthM = "3,65", powerKw = "70").isValid())
    }

    @Test
    fun `the AI search needs only make model year fuel and uses the version`() {
        val criteria = complete.copy(kind = null, variant = "1.3 Multijet").searchCriteria()

        assertEquals(
            VehicleAiCriteria("Fiat", "Panda", 2020, FuelType.PETROL, "1.3 Multijet"),
            criteria,
        )
        assertNull(complete.copy(fuelType = null).searchCriteria())
        assertNull(complete.copy(year = "nope").searchCriteria())
    }

    @Test
    fun `the consumption request carries the main data and both weights`() {
        val input = complete.copy(
            variant = "1.3 Multijet",
            plate = "AB123CD",
            emissionStandard = "Euro 6d",
            engineDisplacementCc = "1248",
            powerKw = "70",
            transmission = Transmission.MANUAL,
            drivetrain = Drivetrain.FWD,
            maxSpeedKph = "168",
            lengthM = "3,653",
            widthM = "1,643",
            heightM = "1,551",
            weightKg = "1200",
            tankL = "38",
            consumptionStandard = ConsumptionStandard.WLTP,
            consumptionL100km = "4,2",
            consumptionUrbanL100km = "5,1",
        ).consumptionInput(referenceWeightKg = 1055)!!

        assertEquals("Fiat", input.make)
        assertEquals("Panda", input.model)
        assertEquals("1.3 Multijet", input.variant)
        assertEquals(2020, input.year)
        assertEquals(VehicleKind.CAR, input.kind)
        assertEquals(FuelType.PETROL, input.fuelType)
        assertEquals(1200, input.weightKg)
        assertEquals(1055, input.referenceWeightKg)
        assertEquals("Euro 6d", input.emissionStandard)
        assertEquals(1248, input.engineDisplacementCc)
        assertEquals(70, input.powerKw)
        assertEquals(Transmission.MANUAL, input.transmission)
        assertEquals(Drivetrain.FWD, input.drivetrain)
        assertEquals(168, input.maxSpeedKph)
        assertEquals(3.653, input.lengthM!!, 0.0001)
        assertEquals(38.0, input.tankL!!, 0.0001)
        assertEquals(ConsumptionStandard.WLTP, input.consumptionStandard)
        assertEquals(4.2, input.currentConsumptionL100km!!, 0.0001)
        assertEquals(5.1, input.currentConsumptionUrbanL100km!!, 0.0001)
        assertNull(input.batteryKwh)
        assertNull(input.currentEnergyKwh100km)
    }

    @Test
    fun `a consumption request needs identity and a valid weight`() {
        assertNull(complete.copy(weightKg = "").consumptionInput())
        assertNull(complete.copy(weightKg = "120000").consumptionInput())
        assertNull(complete.copy(year = "nope").consumptionInput())
        assertNull(complete.copy(fuelType = null).consumptionInput())
        assertNull(complete.copy(kind = null).consumptionInput())
    }

    @Test
    fun `recalculated consumption fills only the figures for the selected fuel`() {
        val payload = VehicleConsumptionPayload(
            consumptionL100km = 5.1,
            consumptionUrbanL100km = 6.4,
            consumptionExtraUrbanL100km = 4.3,
            consumptionMotorwayL100km = 5.5,
            energyKwh100km = 17.2,
            energyUrbanKwh100km = 15.1,
            energyExtraUrbanKwh100km = 16.0,
            energyMotorwayKwh100km = 21.4,
        )

        val applied = complete.copy(consumptionL100km = "4.2").withRecalculatedConsumption(payload)

        assertEquals(formatNumber(5.1), applied.draft.consumptionL100km)
        assertEquals(formatNumber(6.4), applied.draft.consumptionUrbanL100km)
        assertEquals(formatNumber(4.3), applied.draft.consumptionExtraUrbanL100km)
        assertEquals(formatNumber(5.5), applied.draft.consumptionMotorwayL100km)
        assertEquals("", applied.draft.energyKwh100km)
        assertEquals(
            setOf(
                VehicleAiField.CONSUMPTION_L100KM,
                VehicleAiField.CONSUMPTION_URBAN_L100KM,
                VehicleAiField.CONSUMPTION_EXTRA_URBAN_L100KM,
                VehicleAiField.CONSUMPTION_MOTORWAY_L100KM,
            ),
            applied.badges,
        )
    }

    @Test
    fun `a plug-in hybrid receives fuel and electric figures`() {
        val applied = complete.copy(fuelType = FuelType.PLUG_IN_HYBRID).withRecalculatedConsumption(
            VehicleConsumptionPayload(
                consumptionL100km = 5.1,
                energyKwh100km = 17.2,
            ),
        )

        assertEquals(formatNumber(5.1), applied.draft.consumptionL100km)
        assertEquals(formatNumber(17.2), applied.draft.energyKwh100km)
        assertEquals(
            setOf(VehicleAiField.CONSUMPTION_L100KM, VehicleAiField.ENERGY_KWH100KM),
            applied.badges,
        )
    }

    @Test
    fun `a null figure leaves the existing value and badge alone`() {
        val applied = complete.copy(
            consumptionL100km = "4.2",
            consumptionUrbanL100km = "5.1",
        ).withRecalculatedConsumption(
            VehicleConsumptionPayload(consumptionL100km = 5.0),
        )

        assertEquals(formatNumber(5.0), applied.draft.consumptionL100km)
        assertEquals("5.1", applied.draft.consumptionUrbanL100km)
        assertEquals(setOf(VehicleAiField.CONSUMPTION_L100KM), applied.badges)
    }

    @Test
    fun `a complete draft maps to the profile`() {
        val profile = complete.copy(
            variant = "1.3 Multijet",
            plate = "AB123CD",
            transmission = Transmission.MANUAL,
            drivetrain = Drivetrain.FWD,
            consumptionStandard = ConsumptionStandard.WLTP,
            emissionStandard = "Euro 6d",
            engineDisplacementCc = "1248",
            powerKw = "70",
            maxSpeedKph = "168",
            averageSpeedKph = "62",
            averageUrbanSpeedKph = "38",
            averageExtraUrbanSpeedKph = "69",
            averageMotorwaySpeedKph = "105",
            lengthM = "3,653",
            weightKg = "1055",
            tankL = "38",
            consumptionL100km = "4,2",
            consumptionUrbanL100km = "5,1",
            consumptionExtraUrbanL100km = "3,7",
            consumptionMotorwayL100km = "4,6",
            batteryKwh = "40",
            maxChargingPowerKw = "50",
            chargingConnector = "CCS2",
        ).toProfile()

        assertEquals("Fiat", profile.make)
        assertEquals("Panda", profile.model)
        assertEquals("1.3 Multijet", profile.variant)
        assertEquals(2020, profile.year)
        assertEquals(FuelType.PETROL, profile.fuelType)
        assertEquals(VehicleKind.CAR, profile.kind)
        assertEquals("AB123CD", profile.plate)
        assertEquals(Transmission.MANUAL, profile.transmission)
        assertEquals(Drivetrain.FWD, profile.drivetrain)
        assertEquals(ConsumptionStandard.WLTP, profile.consumptionStandard)
        assertEquals(1248, profile.engineDisplacementCc)
        assertEquals(3.653, profile.lengthM!!, 0.0001)
        assertEquals(62, profile.averageSpeedKph)
        assertEquals(105, profile.averageMotorwaySpeedKph)
        assertEquals(4.2, profile.consumptionL100km!!, 0.0001)
        assertEquals(5.1, profile.consumptionUrbanL100km!!, 0.0001)
        assertEquals(3.7, profile.consumptionExtraUrbanL100km!!, 0.0001)
        assertEquals(4.6, profile.consumptionMotorwayL100km!!, 0.0001)
        assertEquals(50.0, profile.maxChargingPowerKw!!, 0.0001)
        assertEquals("CCS2", profile.chargingConnector)
    }

    @Test
    fun `fuel decides which energy fields apply`() {
        assertTrue(FuelType.DIESEL.usesTank())
        assertFalse(FuelType.DIESEL.usesBattery())
        assertTrue(FuelType.DIESEL.usesFuelConsumption())
        assertFalse(FuelType.DIESEL.usesElectricConsumption())

        assertFalse(FuelType.ELECTRIC.usesTank())
        assertTrue(FuelType.ELECTRIC.usesBattery())
        assertFalse(FuelType.ELECTRIC.usesFuelConsumption())
        assertTrue(FuelType.ELECTRIC.usesElectricConsumption())

        assertTrue(FuelType.HYBRID.usesTank())
        assertTrue(FuelType.HYBRID.usesBattery())
        assertTrue(FuelType.HYBRID.usesFuelConsumption())
        assertFalse(FuelType.HYBRID.usesElectricConsumption())

        assertTrue(FuelType.PLUG_IN_HYBRID.usesTank())
        assertTrue(FuelType.PLUG_IN_HYBRID.usesBattery())
        assertTrue(FuelType.PLUG_IN_HYBRID.usesFuelConsumption())
        assertTrue(FuelType.PLUG_IN_HYBRID.usesElectricConsumption())
    }

    @Test
    fun `changing fuel clears the fields that no longer apply`() {
        val diesel = complete.copy(
            fuelType = FuelType.DIESEL,
            engineDisplacementCc = "1248",
            emissionStandard = "Euro 6d",
            tankL = "45",
            consumptionL100km = "4.5",
            consumptionUrbanL100km = "5.4",
            batteryKwh = "20",
            energyKwh100km = "15",
            energyUrbanKwh100km = "14",
        )

        val electric = diesel.withFuel(FuelType.ELECTRIC)

        assertEquals("", electric.engineDisplacementCc)
        assertEquals("", electric.emissionStandard)
        assertEquals("", electric.tankL)
        assertEquals("", electric.consumptionL100km)
        assertEquals("", electric.consumptionUrbanL100km)
        assertEquals("20", electric.batteryKwh)
        assertEquals("15", electric.energyKwh100km)
        assertEquals("14", electric.energyUrbanKwh100km)
        assertTrue(diesel.clearedByFuel(FuelType.ELECTRIC).contains(VehicleAiField.TANK_L))
        assertTrue(diesel.clearedByFuel(FuelType.ELECTRIC).contains(VehicleAiField.EMISSION_STANDARD))

        val hybrid = diesel.copy(batteryKwh = "12", energyKwh100km = "18")
            .withFuel(FuelType.PLUG_IN_HYBRID)
        assertEquals("45", hybrid.tankL)
        assertEquals("12", hybrid.batteryKwh)
        assertEquals("18", hybrid.energyKwh100km)
    }

    @Test
    fun `editing an existing vehicle keeps its hidden legacy nickname`() {
        val profile = VehicleProfile(id = "1", nickname = "Family car", make = "Fiat", model = "Panda")

        assertEquals("Family car", VehicleDraft.from(profile).toProfile().nickname)
    }

    @Test
    fun `editing a field removes only its AI badge`() {
        val badges = setOf(VehicleAiField.KIND, VehicleAiField.POWER_KW)

        assertEquals(setOf(VehicleAiField.POWER_KW), badgesAfterEdit(badges, VehicleAiField.KIND))
        assertEquals(badges, badgesAfterEdit(badges, null))
        assertEquals(badges, badgesAfterEdit(badges, VehicleAiField.WIDTH_M))
    }

    @Test
    fun `an AI draft uses the typed identity and local plate`() {
        val criteria = VehicleAiCriteria("Fiat", "Panda", 2020, FuelType.PETROL, "1.3 Multijet")
        val payload = VehicleAiPayload(
            make = "Volkswagen",
            model = "Golf",
            variant = "1.3 Multijet",
            year = 2019,
            fuelType = FuelType.DIESEL,
            kind = VehicleKind.CAR,
            powerKw = 70,
            transmission = Transmission.MANUAL,
            tankL = 38.0,
            consumptionL100km = 4.2,
            consumptionUrbanL100km = 5.1,
            consumptionExtraUrbanL100km = 3.7,
            consumptionMotorwayL100km = 4.6,
        )

        val draft = VehicleDraft.fromAi(criteria, payload, plate = "AB123CD")

        assertEquals("Fiat", draft.make)
        assertEquals("Panda", draft.model)
        assertEquals("2020", draft.year)
        assertEquals(FuelType.PETROL, draft.fuelType)
        assertEquals(VehicleKind.CAR, draft.kind)
        assertEquals("AB123CD", draft.plate)
        assertEquals("1.3 Multijet", draft.variant)
        assertEquals("70", draft.powerKw)
        assertEquals(Transmission.MANUAL, draft.transmission)
        assertEquals("38", draft.tankL)
        assertEquals(formatNumber(5.1), draft.consumptionUrbanL100km)
        assertEquals(
            setOf(
                VehicleAiField.VARIANT,
                VehicleAiField.KIND,
                VehicleAiField.POWER_KW,
                VehicleAiField.TRANSMISSION,
                VehicleAiField.TANK_L,
                VehicleAiField.CONSUMPTION_L100KM,
                VehicleAiField.CONSUMPTION_URBAN_L100KM,
                VehicleAiField.CONSUMPTION_EXTRA_URBAN_L100KM,
                VehicleAiField.CONSUMPTION_MOTORWAY_L100KM,
            ),
            payload.aiBadgeFields(),
        )
    }

    @Test
    fun `AI data for a fuel-incompatible field is dropped`() {
        val criteria = VehicleAiCriteria("Tesla", "Model 3", 2024, FuelType.ELECTRIC)
        val payload = VehicleAiPayload(
            kind = VehicleKind.CAR,
            engineDisplacementCc = 1998,
            emissionStandard = "Euro 6d",
            tankL = 50.0,
            consumptionL100km = 6.0,
            batteryKwh = 60.0,
            energyKwh100km = 15.0,
            energyUrbanKwh100km = 14.0,
        )

        val draft = VehicleDraft.fromAi(criteria, payload)

        assertEquals("", draft.engineDisplacementCc)
        assertEquals("", draft.emissionStandard)
        assertEquals("", draft.tankL)
        assertEquals("", draft.consumptionL100km)
        assertEquals("60", draft.batteryKwh)
        assertEquals("15", draft.energyKwh100km)
        assertEquals("14", draft.energyUrbanKwh100km)
        assertEquals(
            setOf(
                VehicleAiField.KIND,
                VehicleAiField.BATTERY_KWH,
                VehicleAiField.ENERGY_KWH100KM,
                VehicleAiField.ENERGY_URBAN_KWH100KM,
            ),
            draft.aiBadges(payload.aiBadgeFields()),
        )
    }

    @Test
    fun `a known AI emission value is normalized to the chip list`() {
        val criteria = VehicleAiCriteria("Fiat", "Panda", 2020, FuelType.DIESEL)
        val draft = VehicleDraft.fromAi(
            criteria,
            VehicleAiPayload(emissionStandard = "euro 6d-temp"),
        )

        assertEquals("Euro 6d-TEMP", draft.emissionStandard)
    }
}
