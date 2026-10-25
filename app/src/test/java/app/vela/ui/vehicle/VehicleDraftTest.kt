package app.vela.ui.vehicle

import app.vela.core.ai.VehicleAiCriteria
import app.vela.core.ai.VehicleAiField
import app.vela.core.ai.VehicleAiPayload
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
        assertTrue(complete.copy(lengthM = "3,65", powerKw = "70").isValid())
    }

    @Test
    fun `the AI search needs only make model year and fuel`() {
        val criteria = complete.copy(kind = null).searchCriteria()

        assertEquals(VehicleAiCriteria("Fiat", "Panda", 2020, FuelType.PETROL), criteria)
        assertNull(complete.copy(fuelType = null).searchCriteria())
        assertNull(complete.copy(year = "nope").searchCriteria())
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
            lengthM = "3,653",
            weightKg = "1055",
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
        assertEquals(50.0, profile.maxChargingPowerKw!!, 0.0001)
        assertEquals("CCS2", profile.chargingConnector)
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
        val criteria = VehicleAiCriteria("Fiat", "Panda", 2020, FuelType.PETROL)
        val payload = VehicleAiPayload(
            make = "Volkswagen",
            model = "Golf",
            variant = "1.3 Multijet",
            year = 2019,
            fuelType = FuelType.DIESEL,
            kind = VehicleKind.CAR,
            powerKw = 70,
            transmission = Transmission.MANUAL,
            consumptionStandard = ConsumptionStandard.WLTP,
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
        assertEquals(ConsumptionStandard.WLTP, draft.consumptionStandard)
        assertEquals(
            setOf(
                VehicleAiField.VARIANT,
                VehicleAiField.KIND,
                VehicleAiField.POWER_KW,
                VehicleAiField.TRANSMISSION,
                VehicleAiField.CONSUMPTION_STANDARD,
            ),
            payload.aiBadgeFields(),
        )
    }
}
