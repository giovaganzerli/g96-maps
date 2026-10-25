package app.vela.ui.vehicle

import androidx.annotation.StringRes
import app.vela.R
import app.vela.core.ai.AiProvider

@StringRes
internal fun aiProviderLabel(provider: AiProvider): Int = when (provider) {
    AiProvider.OPENAI -> R.string.settings_ai_provider_openai
    AiProvider.ANTHROPIC -> R.string.settings_ai_provider_anthropic
    AiProvider.GOOGLE -> R.string.settings_ai_provider_google
    AiProvider.OPENROUTER -> R.string.settings_ai_provider_openrouter
}
