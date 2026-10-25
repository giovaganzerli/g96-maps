package app.vela.core.data

import app.vela.core.model.FuelType
import app.vela.core.model.VehicleKind
import app.vela.core.model.VehicleProfile
import app.vela.core.model.VehicleRoutingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleDataTest {
    private val car = VehicleProfile(
        id = "car",
        nickname = "Family car",
        kind = VehicleKind.CAR,
        make = "Fiat",
        model = "Panda",
        year = 2020,
        fuelType = FuelType.PETROL,
        routingMode = VehicleRoutingMode.AUTO,
        plate = "AB123CD",
        heightM = 1.55,
        widthM = 1.65,
        widthWithMirrorsM = 1.85,
        lengthM = 3.65,
        weightKg = 950,
        grossWeightKg = 1300,
        axleLoadKg = 650,
        axleCount = 2,
        consumptionL100km = 5.4,
        energyKwh100km = 14.0,
        batteryKwh = 40.0,
        tankL = 38.0,
        notes = "Small car",
        createdAt = 100L,
        updatedAt = 200L,
    )

    private val van = VehicleProfile(id = "van", make = "Ford", model = "Transit")

    @Test
    fun `a vehicle survives a JSON round trip`() {
        val decoded = VehicleData.decode(VehicleData.encode(listOf(car)))

        assertEquals(listOf(car), decoded)
    }

    @Test
    fun `unknown JSON keys are ignored`() {
        val decoded = VehicleData.decode(
            """[{"id":"1","make":"Fiat","model":"Panda","unknown":true}]""",
        )

        assertEquals(1, decoded.size)
        assertEquals("Fiat Panda", decoded.single().displayName)
    }

    @Test
    fun `invalid JSON reads as an empty garage`() {
        assertTrue(VehicleData.decode("{not json").isEmpty())
        assertTrue(VehicleData.decode(null).isEmpty())
    }

    @Test
    fun `a new vehicle is trimmed and placed first`() {
        val saved = VehicleData.upsert(
            current = listOf(car),
            incoming = van.copy(make = "  Ford ", model = " Transit "),
            now = 300L,
        )

        assertEquals(listOf("van", "car"), saved.map { it.id })
        assertEquals("Ford", saved.first().make)
        assertEquals("Transit", saved.first().model)
        assertEquals(300L, saved.first().createdAt)
        assertEquals(300L, saved.first().updatedAt)
    }

    @Test
    fun `updating a vehicle keeps its creation time`() {
        val saved = VehicleData.upsert(
            current = listOf(car),
            incoming = car.copy(model = "Panda Cross", updatedAt = 999L),
            now = 400L,
        )

        assertEquals(listOf("car"), saved.map { it.id })
        assertEquals("Panda Cross", saved.single().model)
        assertEquals(100L, saved.single().createdAt)
        assertEquals(400L, saved.single().updatedAt)
    }

    @Test
    fun `a blank id gets a generated one`() {
        val saved = VehicleData.upsert(emptyList(), car.copy(id = ""), now = 500L)

        assertTrue(saved.single().id.isNotBlank())
    }

    @Test
    fun `delete removes only the matching vehicle`() {
        assertEquals(listOf("car"), VehicleData.delete(listOf(car, van), "van").map { it.id })
    }

    @Test
    fun `the first saved vehicle becomes active`() {
        val saved = VehicleData.upsert(emptyList(), car, now = 100L)

        assertEquals("car", VehicleData.activeAfter(saved, null))
    }

    @Test
    fun `deleting the active vehicle promotes the first remaining one`() {
        val saved = listOf(car, van)
        val afterDelete = VehicleData.delete(saved, "car")

        assertEquals("van", VehicleData.activeAfter(afterDelete, "car"))
    }

    @Test
    fun `an unknown active id falls back to the first vehicle`() {
        assertEquals("car", VehicleData.activeAfter(listOf(car, van), "missing"))
    }

    @Test
    fun `an empty garage has no active vehicle`() {
        assertNull(VehicleData.activeAfter(emptyList(), "car"))
    }
}
