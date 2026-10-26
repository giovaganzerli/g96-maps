package app.vela.core.ai

import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

sealed interface AiSearchOutcome {
    data class Success(val provider: AiProvider, val answer: VehicleAiAnswer) : AiSearchOutcome
    data object NoProvider : AiSearchOutcome
    data class MissingKey(val provider: AiProvider) : AiSearchOutcome
    data class HttpError(val provider: AiProvider, val status: Int, val message: String?) : AiSearchOutcome
    data class NetworkError(val provider: AiProvider, val message: String?) : AiSearchOutcome
    data class InvalidResponse(val provider: AiProvider) : AiSearchOutcome
}

sealed interface AiConsumptionOutcome {
    data class Success(
        val provider: AiProvider,
        val consumption: VehicleConsumptionPayload,
        val redactedPlateCount: Int,
    ) : AiConsumptionOutcome
    data object NoProvider : AiConsumptionOutcome
    data class MissingKey(val provider: AiProvider) : AiConsumptionOutcome
    data class HttpError(val provider: AiProvider, val status: Int, val message: String?) : AiConsumptionOutcome
    data class NetworkError(val provider: AiProvider, val message: String?) : AiConsumptionOutcome
    data class InvalidResponse(val provider: AiProvider) : AiConsumptionOutcome
    data class NoValues(val provider: AiProvider) : AiConsumptionOutcome
}

sealed interface AiVerifyOutcome {
    data class Verified(val provider: AiProvider) : AiVerifyOutcome
    data class MissingKey(val provider: AiProvider) : AiVerifyOutcome
    data class Failed(val provider: AiProvider, val status: Int?, val message: String?) : AiVerifyOutcome
}

/**
 * Runs a vehicle search against the one active provider. The prompt, schema and parser are shared;
 * the provider only changes the HTTP envelope. A failure is reported, never silently retried on
 * another provider.
 */
@Singleton
class VehicleAiService @Inject constructor(
    base: OkHttpClient,
    private val credentials: AiCredentialStore,
) {
    // The shared client bounds a map scrape at 12 s. A model may need longer, so AI calls get
    // their own timeouts and never inherit that ceiling.
    private val client: OkHttpClient = base.newBuilder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(100, TimeUnit.SECONDS)
        .build()

    val settings = credentials.settings

    fun saveKey(provider: AiProvider, apiKey: String): Boolean = credentials.setKey(provider, apiKey)

    fun clearKey(provider: AiProvider) = credentials.clearKey(provider)

    fun setModel(provider: AiProvider, model: String) = credentials.setModel(provider, model)

    fun setActive(provider: AiProvider) = credentials.setActive(provider)

    suspend fun search(criteria: VehicleAiCriteria): AiSearchOutcome {
        val snapshot = credentials.snapshot()
        val provider = snapshot.active ?: return AiSearchOutcome.NoProvider
        val apiKey = credentials.key(provider) ?: return AiSearchOutcome.MissingKey(provider)
        val config = snapshot.config(provider)
        val redaction = PlateRedactor.redact(criteria.variant)
        val safeCriteria = criteria.copy(variant = redaction.text.trim())
        val request = AiProviderRequests.generate(
            provider = provider,
            model = config.model,
            apiKey = apiKey,
            system = VehicleAiPrompt.system,
            user = VehicleAiPrompt.user(safeCriteria),
        )
        return when (val result = call(request)) {
            is HttpCall.Reply -> {
                if (result.status !in 200..299) {
                    AiSearchOutcome.HttpError(
                        provider = provider,
                        status = result.status,
                        message = AiProviderRequests.errorMessage(result.body),
                    )
                } else {
                    val text = AiProviderRequests.extractText(provider, result.body)
                        ?: return AiSearchOutcome.InvalidResponse(provider)
                    val payload = VehicleAiParser.parse(text)
                        ?: return AiSearchOutcome.InvalidResponse(provider)
                    AiSearchOutcome.Success(
                        provider = provider,
                        answer = VehicleAiMerge.merge(safeCriteria, payload)
                            .copy(redactedPlateCount = redaction.removed),
                    )
                }
            }
            is HttpCall.Failure -> AiSearchOutcome.NetworkError(provider, result.message)
        }
    }

    /**
     * Recalculates the consumption figures for one weight against the one active provider. The
     * request carries the vehicle's main data and the current figures as the reference; it never
     * carries the plate. A failure is reported, never silently retried on another provider.
     */
    suspend fun recalculateConsumption(input: VehicleConsumptionInput): AiConsumptionOutcome {
        val snapshot = credentials.snapshot()
        val provider = snapshot.active ?: return AiConsumptionOutcome.NoProvider
        val apiKey = credentials.key(provider) ?: return AiConsumptionOutcome.MissingKey(provider)
        val config = snapshot.config(provider)
        val redaction = PlateRedactor.redact(input.variant)
        val safeInput = input.copy(variant = redaction.text.trim())
        val request = AiProviderRequests.generate(
            provider = provider,
            model = config.model,
            apiKey = apiKey,
            system = VehicleConsumptionPrompt.system,
            user = VehicleConsumptionPrompt.user(safeInput),
        )
        return when (val result = call(request)) {
            is HttpCall.Reply -> {
                if (result.status !in 200..299) {
                    AiConsumptionOutcome.HttpError(
                        provider = provider,
                        status = result.status,
                        message = AiProviderRequests.errorMessage(result.body),
                    )
                } else {
                    val text = AiProviderRequests.extractText(provider, result.body)
                        ?: return AiConsumptionOutcome.InvalidResponse(provider)
                    val payload = VehicleConsumptionParser.parse(text)
                        ?: return AiConsumptionOutcome.InvalidResponse(provider)
                    if (payload.isEmpty()) {
                        AiConsumptionOutcome.NoValues(provider)
                    } else {
                        AiConsumptionOutcome.Success(
                            provider = provider,
                            consumption = payload,
                            redactedPlateCount = redaction.removed,
                        )
                    }
                }
            }
            is HttpCall.Failure -> AiConsumptionOutcome.NetworkError(provider, result.message)
        }
    }

    suspend fun verify(provider: AiProvider): AiVerifyOutcome {
        val apiKey = credentials.key(provider) ?: return AiVerifyOutcome.MissingKey(provider)
        val request = AiProviderRequests.verify(provider, apiKey)
        return when (val result = call(request)) {
            is HttpCall.Reply ->
                if (result.status in 200..299) AiVerifyOutcome.Verified(provider)
                else AiVerifyOutcome.Failed(
                    provider = provider,
                    status = result.status,
                    message = AiProviderRequests.errorMessage(result.body),
                )
            is HttpCall.Failure -> AiVerifyOutcome.Failed(provider, status = null, message = result.message)
        }
    }

    private suspend fun call(request: AiProviderRequest): HttpCall {
        val builder = Request.Builder().url(request.url)
        request.headers.forEach { (name, value) -> builder.header(name, value) }
        val body = request.body?.toRequestBody(JSON)
        if (body != null) builder.post(body)
        return execute(builder.build())
    }

    private suspend fun execute(request: Request): HttpCall = suspendCancellableCoroutine { continuation ->
        val call = client.newCall(request)
        continuation.invokeOnCancellation { runCatching { call.cancel() } }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) {
                    continuation.resume(HttpCall.Failure(e.message?.take(160)))
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = runCatching { it.body?.string().orEmpty() }.getOrDefault("")
                    if (continuation.isActive) continuation.resume(HttpCall.Reply(it.code, body))
                }
            }
        })
    }

    private sealed interface HttpCall {
        data class Reply(val status: Int, val body: String) : HttpCall
        data class Failure(val message: String?) : HttpCall
    }

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
