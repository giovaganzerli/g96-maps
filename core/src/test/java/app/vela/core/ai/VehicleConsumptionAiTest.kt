package app.vela.core.ai

import app.vela.core.model.ConsumptionStandard
import app.vela.core.model.Drivetrain
import app.vela.core.model.FuelType
import app.vela.core.model.Transmission
import app.vela.core.model.VehicleKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleConsumptionSchemaTest {
    @Test
    fun `every payload writes all consumption keys`() {
        val objectKeys = VehicleConsumptionPayload().toJsonObject().keys

        assertEquals(VehicleConsumptionSchema.keys.toSet(), objectKeys)
        assertEquals(8, VehicleConsumptionSchema.keys.size)
    }

    @Test
    fun `an unknown consumption payload is all null`() {
        val parsed = Json.parseToJsonElement(VehicleConsumptionSchema.emptyJson).jsonObject

        VehicleConsumptionSchema.keys.forEach { key ->
            assertTrue("$key should be null", parsed[key] is JsonNull)
        }
    }

    @Test
    fun `the schema has no vehicle fields`() {
        listOf(
            "make", "model", "variant", "year", "fuelType", "weightKg", "rangeKm",
            "averageSpeedKph", "plate",
        ).forEach { field -> assertFalse(VehicleConsumptionSchema.keys.contains(field)) }
    }
}

class VehicleConsumptionParserTest {
    @Test
    fun `a complete answer is read`() {
        val payload = VehicleConsumptionParser.parse(
            """
            {
              "consumptionL100km": 5.1,
              "consumptionUrbanL100km": 6.4,
              "consumptionExtraUrbanL100km": 4.3,
              "consumptionMotorwayL100km": 5.5,
              "energyKwh100km": 17.2,
              "energyUrbanKwh100km": 15.1,
              "energyExtraUrbanKwh100km": 16.0,
              "energyMotorwayKwh100km": 21.4,
              "rangeKm": 500
            }
            """.trimIndent(),
        )!!

        assertEquals(5.1, payload.consumptionL100km!!, 0.0001)
        assertEquals(6.4, payload.consumptionUrbanL100km!!, 0.0001)
        assertEquals(4.3, payload.consumptionExtraUrbanL100km!!, 0.0001)
        assertEquals(5.5, payload.consumptionMotorwayL100km!!, 0.0001)
        assertEquals(17.2, payload.energyKwh100km!!, 0.0001)
        assertEquals(21.4, payload.energyMotorwayKwh100km!!, 0.0001)
        assertEquals(8, payload.filledFields().size)
        assertFalse(payload.isEmpty())
    }

    @Test
    fun `a fenced answer with quoted numbers is accepted`() {
        val payload = VehicleConsumptionParser.parse(
            """
            ```json
            {"consumptionL100km":"5,1","energyKwh100km":"17.2"}
            ```
            """.trimIndent(),
        )!!

        assertEquals(5.1, payload.consumptionL100km!!, 0.0001)
        assertEquals(17.2, payload.energyKwh100km!!, 0.0001)
        assertNull(payload.consumptionUrbanL100km)
    }

    @Test
    fun `an implausible figure becomes null`() {
        val payload = VehicleConsumptionParser.parse(
            """{"consumptionL100km":0,"consumptionUrbanL100km":-2,"energyKwh100km":400}""",
        )!!

        assertNull(payload.consumptionL100km)
        assertNull(payload.consumptionUrbanL100km)
        assertNull(payload.energyKwh100km)
        assertTrue(payload.isEmpty())
    }

    @Test
    fun `a nested object is unwrapped`() {
        val payload = VehicleConsumptionParser.parse(
            """{"consumption":{"consumptionL100km":5.1}}""",
        )!!

        assertEquals(5.1, payload.consumptionL100km!!, 0.0001)
    }

    @Test
    fun `a non JSON answer is rejected`() {
        assertNull(VehicleConsumptionParser.parse("I cannot answer that."))
    }
}

class VehicleConsumptionPromptTest {
    private val input = VehicleConsumptionInput(
        make = "Fiat",
        model = "Panda",
        year = 2021,
        kind = VehicleKind.CAR,
        fuelType = FuelType.PETROL,
        weightKg = 1200,
        variant = "1.3 Multijet",
        emissionStandard = "Euro 6d",
        engineDisplacementCc = 1248,
        powerKw = 70,
        transmission = Transmission.MANUAL,
        drivetrain = Drivetrain.FWD,
        maxSpeedKph = 168,
        lengthM = 3.653,
        widthM = 1.643,
        heightM = 1.551,
        referenceWeightKg = 1055,
        tankL = 38.0,
        consumptionStandard = ConsumptionStandard.WLTP,
        currentConsumptionL100km = 4.2,
        currentConsumptionUrbanL100km = 5.1,
        currentConsumptionExtraUrbanL100km = 3.7,
        currentConsumptionMotorwayL100km = 4.6,
    )

    @Test
    fun `the user prompt carries the main data and both weights`() {
        val prompt = VehicleConsumptionPrompt.user(input)

        assertTrue(prompt.contains("Fiat"))
        assertTrue(prompt.contains("Panda"))
        assertTrue(prompt.contains("1.3 Multijet"))
        assertTrue(prompt.contains("referenceWeightKg: 1055"))
        assertTrue(prompt.contains("newWeightKg: 1200"))
        assertTrue(prompt.contains("currentConsumptionL100km: 4.2"))
        assertTrue(prompt.contains("fuelConsumptionApplies: true"))
        assertTrue(prompt.contains("electricConsumptionApplies: false"))
        assertFalse(prompt.contains("plate", ignoreCase = true))
    }

    @Test
    fun `the system prompt asks for every key and a weight adjustment`() {
        val prompt = VehicleConsumptionPrompt.system

        VehicleConsumptionSchema.keys.forEach { key -> assertTrue("missing $key", prompt.contains(key)) }
        assertTrue(prompt.contains("referenceWeightKg"))
        assertTrue(prompt.contains("newWeightKg"))
        assertTrue(prompt.contains("do not copy the old figures"))
    }

    @Test
    fun `electric input marks the electric figures as applicable`() {
        val prompt = VehicleConsumptionPrompt.user(
            input.copy(
                fuelType = FuelType.ELECTRIC,
                engineDisplacementCc = null,
                emissionStandard = null,
                tankL = null,
                batteryKwh = 40.0,
                currentConsumptionL100km = null,
                currentConsumptionUrbanL100km = null,
                currentConsumptionExtraUrbanL100km = null,
                currentConsumptionMotorwayL100km = null,
                currentEnergyKwh100km = 15.0,
                currentEnergyUrbanKwh100km = 14.0,
                currentEnergyExtraUrbanKwh100km = 14.5,
                currentEnergyMotorwayKwh100km = 18.0,
            ),
        )

        assertTrue(prompt.contains("fuelConsumptionApplies: false"))
        assertTrue(prompt.contains("electricConsumptionApplies: true"))
        assertTrue(prompt.contains("currentEnergyKwh100km: 15.0"))
        assertFalse(prompt.contains("currentConsumptionL100km"))
    }
}
