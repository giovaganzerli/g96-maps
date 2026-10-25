package app.vela

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The encrypted AI keys must never enter Android backup or a phone-to-phone transfer. */
class BackupRulesTest {
    private fun xml(name: String): String {
        val candidates = listOf(
            File("src/main/res/xml/$name"),
            File("app/src/main/res/xml/$name"),
        )
        return candidates.firstOrNull { it.exists() }?.readText()
            ?: error("$name not found from ${File(".").absolutePath}")
    }

    @Test
    fun `the AI preferences are excluded from legacy backup`() {
        val rules = xml("backup_rules.xml")

        assertTrue(rules.contains(AI_EXCLUDE))
        assertFalse(rules.contains(AI_INCLUDE))
    }

    @Test
    fun `the AI preferences are excluded from cloud backup and transfer`() {
        val rules = xml("data_extraction_rules.xml")

        assertEquals(2, Regex(Regex.escape(AI_EXCLUDE)).findAll(rules).count())
        assertFalse(rules.contains(AI_INCLUDE))
    }

    private companion object {
        const val AI_EXCLUDE = "<exclude domain=\"sharedpref\" path=\"vela_ai.xml\""
        const val AI_INCLUDE = "<include domain=\"sharedpref\" path=\"vela_ai.xml\""
    }
}
