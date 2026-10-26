package app.vela.ui.settings.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vela.R
import app.vela.core.ai.AiProvider
import app.vela.ui.dpadClickable
import app.vela.ui.dpadFieldEscape
import app.vela.ui.dpadHighlight
import app.vela.ui.settings.GroupDivider
import app.vela.ui.settings.Hint
import app.vela.ui.settings.PageIntro
import app.vela.ui.settings.SettingsGroup
import app.vela.ui.settings.SettingsScaffold
import app.vela.ui.settings.settingsFieldColors
import app.vela.ui.vehicle.AiSaveState
import app.vela.ui.vehicle.AiVerifyState
import app.vela.ui.vehicle.VehicleField
import app.vela.ui.vehicle.VehicleSettingsViewModel
import app.vela.ui.vehicle.aiProviderLabel

/** A fixed mask, so the saved key never reappears in the field or leaks its length. */
private const val SAVED_KEY_MASK = "••••••••••••"
private const val MASK_CHAR = '•'

@Composable
internal fun AiSettingsScreen(
    onBack: () -> Unit,
    vm: VehicleSettingsViewModel = hiltViewModel(),
) {
    val settings by vm.aiSettings.collectAsStateWithLifecycle()
    val verify by vm.verify.collectAsStateWithLifecycle()
    val save by vm.saveState.collectAsStateWithLifecycle()

    val active = settings.active
    var selected by remember(active) { mutableStateOf(active) }
    var model by remember(selected, settings) { mutableStateOf(selected?.let { settings.config(it).model }.orEmpty()) }
    var key by remember(selected) { mutableStateOf("") }
    var replacingKey by remember(selected) { mutableStateOf(false) }
    val hasKey = selected?.let { settings.config(it).hasKey } == true

    LaunchedEffect(save) {
        if (save is AiSaveState.Saved) {
            key = ""
            replacingKey = false
        }
    }
    LaunchedEffect(hasKey) {
        if (!hasKey) {
            key = ""
            replacingKey = false
        }
    }

    SettingsScaffold(stringResource(R.string.settings_ai), onBack) { topRow ->
        Spacer(Modifier.height(4.dp))
        PageIntro(stringResource(R.string.settings_ai_hint))
        when {
            selected == null -> Hint(stringResource(R.string.settings_ai_no_provider))
            selected != active -> Hint(stringResource(R.string.settings_ai_no_active))
        }

        SettingsGroup(title = stringResource(R.string.settings_ai_provider)) {
            AiProvider.entries.forEachIndexed { index, provider ->
                if (index > 0) GroupDivider()
                ProviderRow(
                    provider = provider,
                    selected = selected == provider,
                    active = active == provider,
                    hasKey = settings.config(provider).hasKey,
                    modifier = if (index == 0) topRow else Modifier,
                    onClick = {
                        selected = provider
                        vm.consumeSaveState()
                        vm.consumeVerifyState()
                    },
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        val provider = selected
        if (provider != null) {
            SettingsGroup(title = stringResource(R.string.settings_ai_credentials)) {
                VehicleField(
                    value = model,
                    onValueChange = {
                        model = it
                        vm.consumeSaveState()
                        vm.consumeVerifyState()
                    },
                    label = stringResource(R.string.settings_ai_model),
                    placeholder = stringResource(R.string.settings_ai_model_hint),
                )
                OutlinedTextField(
                    value = if (hasKey && !replacingKey) SAVED_KEY_MASK else key,
                    onValueChange = { newValue ->
                        if (hasKey && !replacingKey) {
                            replacingKey = true
                            key = newValue.filterNot { it == MASK_CHAR }
                        } else {
                            key = newValue
                        }
                        vm.consumeSaveState()
                        vm.consumeVerifyState()
                    },
                    label = { Text(stringResource(R.string.settings_ai_key)) },
                    placeholder = { Text(stringResource(R.string.settings_ai_key_hint)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(20.dp),
                    colors = settingsFieldColors(),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).dpadFieldEscape(),
                )
                if (replacingKey) {
                    Hint(stringResource(R.string.settings_ai_key_unsaved))
                } else if (hasKey) {
                    Hint(stringResource(R.string.settings_ai_key_keep))
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { vm.saveProvider(provider, model, if (replacingKey) key else "") },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.settings_ai_save))
                }
                OutlinedButton(
                    onClick = {
                        key = ""
                        replacingKey = false
                        vm.clearKey(provider)
                    },
                    enabled = hasKey,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.settings_ai_clear_key))
                }
            }
            SaveMessage(save)

            Spacer(Modifier.height(12.dp))
            SettingsGroup(title = stringResource(R.string.settings_ai_test)) {
                Hint(stringResource(R.string.settings_ai_test_hint))
                FilledTonalButton(
                    onClick = { vm.verify(provider) },
                    enabled = hasKey && !replacingKey && verify !is AiVerifyState.Running,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        stringResource(
                            if (verify is AiVerifyState.Running) R.string.settings_ai_verify_running
                            else R.string.settings_ai_verify,
                        ),
                    )
                }
                VerifyMessage(verify)
            }
        }

        Spacer(Modifier.height(8.dp))
        Hint(stringResource(R.string.settings_ai_privacy))
        Hint(stringResource(R.string.settings_ai_no_builtin))
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ProviderRow(
    provider: AiProvider,
    selected: Boolean,
    active: Boolean,
    hasKey: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .dpadHighlight(RoundedCornerShape(16.dp))
            .dpadClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(aiProviderLabel(provider)), style = MaterialTheme.typography.bodyLarge)
                if (active) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.settings_ai_active),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(
                stringResource(if (hasKey) R.string.settings_ai_key_saved else R.string.settings_ai_key_missing),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SaveMessage(state: AiSaveState) {
    val text = when (state) {
        AiSaveState.Saved -> stringResource(R.string.settings_ai_saved)
        AiSaveState.KeystoreFailed -> stringResource(R.string.settings_ai_keystore_failed)
        AiSaveState.Idle -> return
    }
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (state is AiSaveState.KeystoreFailed) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
private fun VerifyMessage(state: AiVerifyState) {
    val text = when (state) {
        AiVerifyState.Idle, is AiVerifyState.Running -> return
        is AiVerifyState.Verified -> stringResource(R.string.settings_ai_verified)
        is AiVerifyState.MissingKey -> stringResource(R.string.settings_ai_verify_missing)
        is AiVerifyState.Failed -> {
            val detail = when {
                state.status != null -> buildString {
                    append(stringResource(R.string.settings_ai_error_http, state.status))
                    state.message?.let { append(" · ").append(it) }
                }
                else -> state.message ?: stringResource(R.string.settings_ai_error_network)
            }
            stringResource(R.string.settings_ai_verify_failed, detail)
        }
    }
    val failed = state is AiVerifyState.MissingKey || state is AiVerifyState.Failed
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}
