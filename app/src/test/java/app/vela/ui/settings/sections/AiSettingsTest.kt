package app.vela.ui.settings.sections

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiSettingsTest {
    @Test
    fun `typing the first key marks it for saving`() {
        val edit = editAiKey(hasKey = false, replacingKey = false, newValue = "AIza-test")

        assertEquals("AIza-test", edit.key)
        assertTrue(edit.replacing)
    }

    @Test
    fun `typing over a saved mask drops the mask`() {
        val edit = editAiKey(hasKey = true, replacingKey = false, newValue = "••••x")

        assertEquals("x", edit.key)
        assertTrue(edit.replacing)
    }

    @Test
    fun `later edits keep the typed key`() {
        val edit = editAiKey(hasKey = true, replacingKey = true, newValue = "AIza-replacement")

        assertEquals("AIza-replacement", edit.key)
        assertTrue(edit.replacing)
    }
}
