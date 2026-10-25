package app.vela.core.ai

/** [text] with plate-like tokens replaced; [removed] is how many were found. */
data class PlateRedaction(val text: String, val removed: Int)

/**
 * Removes plate-like tokens from the free-text AI query. The plate field itself is never sent, and
 * a plate typed into the query by mistake is replaced before the prompt leaves the phone.
 */
object PlateRedactor {
    private val patterns = listOf(
        // Current Italian and most EU formats: AB123CD, AB 123 CD.
        Regex("""(?i)\b[A-Z]{2}[\s.\-]?\d{3}[\s.\-]?[A-Z]{2}\b"""),
        // Older Italian format: AB123456.
        Regex("""(?i)\b[A-Z]{2}[\s.\-]?\d{6}\b"""),
        // One-letter formats used by a few countries: A123BCD.
        Regex("""(?i)\b[A-Z][\s.\-]?\d{3}[\s.\-]?[A-Z]{3}\b"""),
    )

    fun redact(text: String): PlateRedaction {
        var current = text
        var removed = 0
        for (pattern in patterns) {
            removed += pattern.findAll(current).count()
            current = pattern.replace(current, PLACEHOLDER)
        }
        return PlateRedaction(current, removed)
    }

    const val PLACEHOLDER = "[plate removed]"
}
