package app.vela.ui.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vela.core.ai.AiProvider
import app.vela.core.ai.AiSearchOutcome
import app.vela.core.ai.AiVerifyOutcome
import app.vela.core.ai.VehicleAiAnswer
import app.vela.core.ai.VehicleAiCriteria
import app.vela.core.ai.VehicleAiService
import app.vela.core.data.VehiclePrefs
import app.vela.core.data.VehicleStore
import app.vela.core.model.VehicleProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface VehicleAiSearchState {
    data object Idle : VehicleAiSearchState
    data object Loading : VehicleAiSearchState
    data class Success(val provider: AiProvider, val answer: VehicleAiAnswer) : VehicleAiSearchState
    data object NoProvider : VehicleAiSearchState
    data class MissingKey(val provider: AiProvider) : VehicleAiSearchState
    data class HttpError(val provider: AiProvider, val status: Int, val message: String?) : VehicleAiSearchState
    data class NetworkError(val provider: AiProvider, val message: String?) : VehicleAiSearchState
    data class InvalidResponse(val provider: AiProvider) : VehicleAiSearchState
}

sealed interface AiVerifyState {
    data object Idle : AiVerifyState
    data class Running(val provider: AiProvider) : AiVerifyState
    data class Verified(val provider: AiProvider) : AiVerifyState
    data class MissingKey(val provider: AiProvider) : AiVerifyState
    data class Failed(val provider: AiProvider, val status: Int?, val message: String?) : AiVerifyState
}

sealed interface AiSaveState {
    data object Idle : AiSaveState
    data object Saved : AiSaveState
    data object KeystoreFailed : AiSaveState
}

/**
 * Garage writes and the AI settings/search plumbing. The map ViewModel no longer owns the garage
 * buttons; the route-picker writes still go through `VehicleActions`, untouched.
 */
@HiltViewModel
class VehicleSettingsViewModel @Inject constructor(
    private val store: VehicleStore,
    private val ai: VehicleAiService,
) : ViewModel() {
    val vehicles = VehiclePrefs.vehicles
    val activeVehicle = VehiclePrefs.active
    val aiSettings = ai.settings

    private val _search = MutableStateFlow<VehicleAiSearchState>(VehicleAiSearchState.Idle)
    val search: StateFlow<VehicleAiSearchState> = _search.asStateFlow()

    private val _verify = MutableStateFlow<AiVerifyState>(AiVerifyState.Idle)
    val verify: StateFlow<AiVerifyState> = _verify.asStateFlow()

    private val _saveState = MutableStateFlow<AiSaveState>(AiSaveState.Idle)
    val saveState: StateFlow<AiSaveState> = _saveState.asStateFlow()

    private var searchJob: Job? = null
    private var verifyJob: Job? = null

    fun save(vehicle: VehicleProfile) {
        viewModelScope.launch(Dispatchers.IO) { store.save(vehicle) }
    }

    fun delete(id: String) {
        viewModelScope.launch(Dispatchers.IO) { store.delete(id) }
    }

    fun setActive(id: String) {
        viewModelScope.launch(Dispatchers.IO) { store.setActive(id) }
    }

    fun search(criteria: VehicleAiCriteria) {
        if (_search.value is VehicleAiSearchState.Loading) return
        searchJob?.cancel()
        _search.value = VehicleAiSearchState.Loading
        searchJob = viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) { ai.search(criteria) }
            _search.value = when (outcome) {
                is AiSearchOutcome.Success -> VehicleAiSearchState.Success(outcome.provider, outcome.answer)
                is AiSearchOutcome.NoProvider -> VehicleAiSearchState.NoProvider
                is AiSearchOutcome.MissingKey -> VehicleAiSearchState.MissingKey(outcome.provider)
                is AiSearchOutcome.HttpError ->
                    VehicleAiSearchState.HttpError(outcome.provider, outcome.status, outcome.message)
                is AiSearchOutcome.NetworkError ->
                    VehicleAiSearchState.NetworkError(outcome.provider, outcome.message)
                is AiSearchOutcome.InvalidResponse ->
                    VehicleAiSearchState.InvalidResponse(outcome.provider)
            }
        }
    }

    fun resetSearch() {
        searchJob?.cancel()
        searchJob = null
        _search.value = VehicleAiSearchState.Idle
    }

    /**
     * A blank [apiKey] keeps the stored key; use [clearKey] to remove it. The model is saved even
     * when the key is left untouched.
     */
    fun saveProvider(provider: AiProvider, model: String, apiKey: String) {
        _saveState.value = AiSaveState.Idle
        consumeVerifyState()
        viewModelScope.launch {
            val stored = withContext(Dispatchers.IO) {
                if (apiKey.isNotBlank() && !ai.saveKey(provider, apiKey)) {
                    false
                } else {
                    ai.setModel(provider, model)
                    ai.setActive(provider)
                    true
                }
            }
            _saveState.value = if (stored) AiSaveState.Saved else AiSaveState.KeystoreFailed
        }
    }

    fun clearKey(provider: AiProvider) {
        consumeVerifyState()
        viewModelScope.launch(Dispatchers.IO) { ai.clearKey(provider) }
    }

    fun verify(provider: AiProvider) {
        if (_verify.value is AiVerifyState.Running) return
        consumeVerifyState()
        _verify.value = AiVerifyState.Running(provider)
        verifyJob = viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) { ai.verify(provider) }
            _verify.value = when (outcome) {
                is AiVerifyOutcome.Verified -> AiVerifyState.Verified(outcome.provider)
                is AiVerifyOutcome.MissingKey -> AiVerifyState.MissingKey(outcome.provider)
                is AiVerifyOutcome.Failed ->
                    AiVerifyState.Failed(outcome.provider, outcome.status, outcome.message)
            }
        }
    }

    fun consumeSaveState() {
        _saveState.value = AiSaveState.Idle
    }

    fun consumeVerifyState() {
        verifyJob?.cancel()
        verifyJob = null
        _verify.value = AiVerifyState.Idle
    }
}
