package app.vela.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class VehicleTest {
    @Test
    fun `make and model lead the display name`() {
        val vehicle = VehicleProfile(id = "1", make = "Fiat", model = "Panda")

        assertEquals("Fiat Panda", vehicle.displayName)
    }

    @Test
    fun `the plate follows make and model`() {
        val vehicle = VehicleProfile(id = "1", make = "Fiat", model = "Panda", plate = "AB123CD")

        assertEquals("Fiat Panda · AB123CD", vehicle.displayName)
    }

    @Test
    fun `a retired nickname is ignored`() {
        val vehicle = VehicleProfile(id = "1", nickname = "Family car", make = "Fiat", model = "Panda")

        assertEquals("Fiat Panda", vehicle.displayName)
    }

    @Test
    fun `a blank profile still has a name`() {
        assertEquals("Vehicle", VehicleProfile(id = "1").displayName)
        assertEquals("Vehicle · AB123CD", VehicleProfile(id = "1", plate = "AB123CD").displayName)
    }

    @Test
    fun `only the filled part of make and model is shown`() {
        assertEquals("Fiat", VehicleProfile(id = "1", make = "Fiat").displayName)
        assertEquals("Panda", VehicleProfile(id = "1", model = "Panda").displayName)
    }
}
