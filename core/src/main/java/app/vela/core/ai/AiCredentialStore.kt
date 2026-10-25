package app.vela.core.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.ByteBuffer
import java.security.KeyStore
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What the settings page and the search need to know about one provider, never the key itself. */
data class AiProviderConfig(
    val provider: AiProvider,
    val hasKey: Boolean,
    val model: String,
)

data class AiSettingsSnapshot(
    val active: AiProvider?,
    val providers: List<AiProviderConfig>,
) {
    fun config(provider: AiProvider): AiProviderConfig =
        providers.first { it.provider == provider }

    val activeConfig: AiProviderConfig? get() = active?.let(::config)
}

/**
 * The API keys live in the `vela_ai` preference file as AES-GCM ciphertext from a key that never
 * leaves the Android Keystore. The file is excluded from Android backup and device transfer, so a
 * restored phone has no key. Models are not secret and are stored as plain text.
 *
 * One provider is active at a time. There is no built-in key and no fallback to another provider.
 */
@Singleton
class AiCredentialStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<AiSettingsSnapshot> = _settings.asStateFlow()

    fun snapshot(): AiSettingsSnapshot = read()

    fun setActive(provider: AiProvider) {
        prefs.edit().putString(KEY_ACTIVE, provider.name).apply()
        publish()
    }

    /** A blank model falls back to the provider's default. */
    fun setModel(provider: AiProvider, model: String) {
        val value = model.trim()
        prefs.edit().apply {
            if (value.isBlank()) remove(modelKey(provider)) else putString(modelKey(provider), value)
        }.apply()
        publish()
    }

    /** Store [apiKey] encrypted. A blank value clears the key. False means the Keystore refused. */
    fun setKey(provider: AiProvider, apiKey: String): Boolean {
        val value = apiKey.trim()
        if (value.isEmpty()) {
            clearKey(provider)
            return true
        }
        val encrypted = AiSecretCipher.encrypt(value) ?: return false
        prefs.edit().putString(keyKey(provider), encrypted).apply()
        publish()
        return true
    }

    fun clearKey(provider: AiProvider) {
        prefs.edit().remove(keyKey(provider)).apply()
        publish()
    }

    /** The decrypted key for one call, or null when none is stored or the Keystore key is gone. */
    fun key(provider: AiProvider): String? =
        prefs.getString(keyKey(provider), null)?.let(AiSecretCipher::decrypt)?.takeIf { it.isNotBlank() }

    fun hasKey(provider: AiProvider): Boolean = key(provider) != null

    private fun read(): AiSettingsSnapshot {
        val active = prefs.getString(KEY_ACTIVE, null)
            ?.let { raw -> AiProvider.entries.firstOrNull { it.name == raw } }
        return AiSettingsSnapshot(
            active = active,
            providers = AiProvider.entries.map { provider ->
                AiProviderConfig(
                    provider = provider,
                    hasKey = hasKey(provider),
                    model = prefs.getString(modelKey(provider), null)?.takeIf { it.isNotBlank() }
                        ?: provider.defaultModel,
                )
            },
        )
    }

    private fun publish() {
        _settings.value = read()
    }

    private fun modelKey(provider: AiProvider) = "model_${provider.name.lowercase(Locale.ROOT)}"

    private fun keyKey(provider: AiProvider) = "key_${provider.name.lowercase(Locale.ROOT)}"

    private companion object {
        const val PREFS = "vela_ai"
        const val KEY_ACTIVE = "active_provider"
    }
}

/** AES-GCM with a non-exportable Android Keystore key, aliased per app. */
private object AiSecretCipher {
    private const val ALIAS = "vela_ai_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val TAG_BITS = 128

    @Synchronized
    fun encrypt(plain: String): String? = runCatching {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val buffer = ByteBuffer.allocate(1 + iv.size + encrypted.size)
        buffer.put(iv.size.toByte())
        buffer.put(iv)
        buffer.put(encrypted)
        Base64.encodeToString(buffer.array(), Base64.NO_WRAP)
    }.getOrNull()

    @Synchronized
    fun decrypt(raw: String): String? = runCatching {
        val bytes = Base64.decode(raw, Base64.NO_WRAP)
        if (bytes.size <= 1) return@runCatching null
        val ivLength = bytes[0].toInt() and 0xFF
        if (ivLength <= 0 || bytes.size <= 1 + ivLength) return@runCatching null
        val iv = bytes.copyOfRange(1, 1 + ivLength)
        val ciphertext = bytes.copyOfRange(1 + ivLength, bytes.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv))
        String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }.getOrNull()

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }
}
