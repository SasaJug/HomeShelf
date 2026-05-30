package com.jugurdzija.homeshelf.ui.settings

import androidx.lifecycle.ViewModel
import com.jugurdzija.homeshelf.data.CaptureSettingsStoreImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class CaptureSettingsViewModel @Inject constructor(
    private val settingsStore: CaptureSettingsStoreImpl
) : ViewModel() {

    val captureThreshold: StateFlow<Float> = settingsStore.captureThreshold

    fun setCaptureThreshold(value: Float) = settingsStore.setCaptureThreshold(value)
}
