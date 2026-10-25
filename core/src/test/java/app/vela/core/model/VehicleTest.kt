package app.vela.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class VehicleTest {
    @Test
    fun `nickname leads the display name`() {
        val vehicle = VehicleProfile(
            id = "1",
            nickname = "Family car",
            make = "Fiat",
            model = "Panda",
        )

        assertEquals("Family car", vehicle.displayName)
    }

    @Test
    fun `make and model are the fallback name`() {
        val vehicle = VehicleProfile(id = "1", make = "Fiat", model = "Panda")

        assertEquals("Fiat Panda", vehicle.displayName)
    }

    @Test
    fun `a blank profile still has a name`() {
        assertEquals("Vehicle", VehicleProfile(id = "1").displayName)
    }

    @Test
    fun `only the filled part of make and model is shown`() {
        assertEquals("Fiat", VehicleProfile(id = "1", make = "Fiat").displayName)
        assertEquals("Panda", VehicleProfile(id = "1", model = "Panda").displayName)
    }
}
