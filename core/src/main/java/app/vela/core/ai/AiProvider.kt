package app.vela.core.ai

/**
 * The four API-key providers the vehicle AI search can talk to. A provider is only used when the
 * user made it the active one and stored a key for it: there is no built-in key and no fallback.
 */
enum class AiProvider(val defaultModel: String) {
    OPENAI("gpt-4.1-mini"),
    ANTHROPIC("claude-sonnet-4-5"),
    GOOGLE("gemini-2.5-flash"),
    OPENROUTER("openai/gpt-4.1-mini"),
}
