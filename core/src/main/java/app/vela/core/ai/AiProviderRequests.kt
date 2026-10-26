package app.vela.core.ai

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

/** One HTTP call, transport-independent so the request shapes can be unit tested. */
data class AiProviderRequest(
    val url: HttpUrl,
    val headers: Map<String, String>,
    val body: String?,
)

/**
 * The four native API dialects. The prompt and schema decide the JSON; only the envelope and the
 * response path differ here.
 */
object AiProviderRequests {
    private const val JSON = "application/json; charset=utf-8"
    private const val ANTHROPIC_VERSION = "2023-06-01"

    /** One model turn. The provider only changes the envelope and the response path. */
    fun generate(
        provider: AiProvider,
        model: String,
        apiKey: String,
        system: String,
        user: String,
    ): AiProviderRequest = when (provider) {
        AiProvider.OPENAI -> openAi(model, apiKey, system, user)
        AiProvider.ANTHROPIC -> anthropic(model, apiKey, system, user)
        AiProvider.GOOGLE -> google(model, apiKey, system, user)
        AiProvider.OPENROUTER -> openRouter(model, apiKey, system, user)
    }

    fun verify(provider: AiProvider, apiKey: String): AiProviderRequest = when (provider) {
        AiProvider.OPENAI -> AiProviderRequest(
            url = "https://api.openai.com/v1/models".toHttpUrl(),
            headers = mapOf("Authorization" to "Bearer $apiKey"),
            body = null,
        )
        AiProvider.ANTHROPIC -> AiProviderRequest(
            url = "https://api.anthropic.com/v1/models".toHttpUrl(),
            headers = mapOf("x-api-key" to apiKey, "anthropic-version" to ANTHROPIC_VERSION),
            body = null,
        )
        AiProvider.GOOGLE -> AiProviderRequest(
            url = googleModels(apiKey),
            headers = emptyMap(),
            body = null,
        )
        AiProvider.OPENROUTER -> AiProviderRequest(
            // The models list is public, so it cannot prove a key is valid. The key endpoint can.
            url = "https://openrouter.ai/api/v1/key".toHttpUrl(),
            headers = mapOf("Authorization" to "Bearer $apiKey"),
            body = null,
        )
    }

    /** The assistant text from a native success body, or null when the shape is not recognized. */
    fun extractText(provider: AiProvider, body: String): String? {
        val root = runCatching { Json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        return when (provider) {
            AiProvider.OPENAI -> openAiText(root)
            AiProvider.ANTHROPIC -> anthropicText(root)
            AiProvider.GOOGLE -> googleText(root)
            AiProvider.OPENROUTER -> openRouterText(root)
        }?.trim()?.takeIf { it.isNotEmpty() }
    }

    /** The provider's own error sentence, for a status line. Never includes the API key. */
    fun errorMessage(body: String): String? {
        val root = runCatching { Json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        val error = root["error"]
        val message = when (error) {
            is JsonObject -> error["message"]?.jsonPrimitive?.contentOrNull()
            is JsonPrimitive -> error.contentOrNull()
            else -> root["message"]?.jsonPrimitive?.contentOrNull()
        }
        return message?.trim()?.take(240)?.takeIf { it.isNotBlank() }
    }

    private fun openAi(model: String, apiKey: String, system: String, user: String): AiProviderRequest {
        val body = buildJsonObject {
            put("model", model)
            put("input", buildJsonArray {
                add(message("system", system))
                add(message("user", user))
            })
            put("text", buildJsonObject { put("format", buildJsonObject { put("type", "json_object") }) })
        }
        return AiProviderRequest(
            url = "https://api.openai.com/v1/responses".toHttpUrl(),
            headers = mapOf(
                "Authorization" to "Bearer $apiKey",
                "Content-Type" to JSON,
            ),
            body = body.toString(),
        )
    }

    private fun anthropic(model: String, apiKey: String, system: String, user: String): AiProviderRequest {
        val body = buildJsonObject {
            put("model", model)
            put("max_tokens", 4096)
            put("system", system)
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("content", user)
                })
            })
            put("temperature", 0.1)
        }
        return AiProviderRequest(
            url = "https://api.anthropic.com/v1/messages".toHttpUrl(),
            headers = mapOf(
                "x-api-key" to apiKey,
                "anthropic-version" to ANTHROPIC_VERSION,
                "Content-Type" to JSON,
            ),
            body = body.toString(),
        )
    }

    private fun google(model: String, apiKey: String, system: String, user: String): AiProviderRequest {
        val body = buildJsonObject {
            put("systemInstruction", buildJsonObject {
                put("parts", buildJsonArray { add(buildJsonObject { put("text", system) }) })
            })
            put("contents", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("parts", buildJsonArray { add(buildJsonObject { put("text", user) }) })
                })
            })
            put("generationConfig", buildJsonObject {
                put("responseMimeType", "application/json")
                put("temperature", 0.1)
            })
        }
        val url = "https://generativelanguage.googleapis.com".toHttpUrl().newBuilder()
            .addPathSegment("v1beta")
            .addPathSegment("models")
            .addPathSegment("$model:generateContent")
            .addQueryParameter("key", apiKey)
            .build()
        return AiProviderRequest(
            url = url,
            headers = mapOf("Content-Type" to JSON),
            body = body.toString(),
        )
    }

    private fun openRouter(model: String, apiKey: String, system: String, user: String): AiProviderRequest {
        val body = buildJsonObject {
            put("model", model)
            put("messages", buildJsonArray {
                add(buildJsonObject { put("role", "system"); put("content", system) })
                add(buildJsonObject { put("role", "user"); put("content", user) })
            })
            put("response_format", buildJsonObject { put("type", "json_object") })
            put("temperature", 0.1)
        }
        return AiProviderRequest(
            url = "https://openrouter.ai/api/v1/chat/completions".toHttpUrl(),
            headers = mapOf(
                "Authorization" to "Bearer $apiKey",
                "Content-Type" to JSON,
            ),
            body = body.toString(),
        )
    }

    private fun message(role: String, text: String): JsonObject = buildJsonObject {
        put("role", role)
        put("content", buildJsonArray { add(buildJsonObject { put("type", "input_text"); put("text", text) }) })
    }

    private fun googleModels(apiKey: String): HttpUrl =
        "https://generativelanguage.googleapis.com".toHttpUrl().newBuilder()
            .addPathSegment("v1beta")
            .addPathSegment("models")
            .addQueryParameter("key", apiKey)
            .build()

    private fun openAiText(root: JsonObject): String? {
        root["output_text"]?.jsonPrimitive?.contentOrNull()?.let { return it }
        val output = root["output"] as? JsonArray ?: return null
        return output.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val content = obj["content"] as? JsonArray ?: return@mapNotNull null
            content.mapNotNull { part ->
                val partObject = part as? JsonObject ?: return@mapNotNull null
                if (partObject["type"]?.jsonPrimitive?.contentOrNull() == "output_text") {
                    partObject["text"]?.jsonPrimitive?.contentOrNull()
                } else null
            }.joinToString("")
        }.joinToString("")
    }

    private fun anthropicText(root: JsonObject): String? {
        val content = root["content"] as? JsonArray ?: return null
        return content.mapNotNull { part ->
            val obj = part as? JsonObject ?: return@mapNotNull null
            if (obj["type"]?.jsonPrimitive?.contentOrNull() == "text") {
                obj["text"]?.jsonPrimitive?.contentOrNull()
            } else null
        }.joinToString("")
    }

    private fun googleText(root: JsonObject): String? {
        val candidates = root["candidates"] as? JsonArray ?: return null
        return candidates.mapNotNull { candidate ->
            val content = (candidate as? JsonObject)?.get("content") as? JsonObject ?: return@mapNotNull null
            val parts = content["parts"] as? JsonArray ?: return@mapNotNull null
            parts.mapNotNull { part ->
                val partObject = part as? JsonObject ?: return@mapNotNull null
                if (partObject["thought"]?.jsonPrimitive?.contentOrNull()?.toBoolean() == true) {
                    return@mapNotNull null
                }
                partObject["text"]?.jsonPrimitive?.contentOrNull()
            }.joinToString("")
        }.joinToString("")
    }

    private fun openRouterText(root: JsonObject): String? {
        val choices = root["choices"] as? JsonArray ?: return null
        val first = choices.firstOrNull() as? JsonObject ?: return null
        val content = (first["message"] as? JsonObject)?.get("content")
        return when (content) {
            is JsonPrimitive -> content.contentOrNull()
            is JsonArray -> content.mapNotNull { part ->
                (part as? JsonObject)?.get("text")?.jsonPrimitive?.contentOrNull()
            }.joinToString("")
            else -> first["text"]?.jsonPrimitive?.contentOrNull()
        }
    }

    private fun JsonPrimitive.contentOrNull(): String? =
        if (this is kotlinx.serialization.json.JsonNull) null else content
}
