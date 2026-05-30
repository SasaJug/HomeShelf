package com.jugurdzija.homeshelf.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit



@Singleton
class CaptureSettingsStoreImpl @Inject constructor(
    @ApplicationContext context: Context
) : CaptureSettingsStore {

    private val prefs = context.getSharedPreferences(CAPTURE_SETTING, Context.MODE_PRIVATE)

    private val _captureThreshold = MutableStateFlow(
        prefs.getFloat(KEY_THRESHOLD, DEFAULT_THRESHOLD)
    )
    override val captureThreshold: StateFlow<Float> = _captureThreshold.asStateFlow()

    override fun setCaptureThreshold(value: Float) {
        _captureThreshold.value = value
        prefs.edit { putFloat(KEY_THRESHOLD, value) }
    }

    companion object {
        private const val KEY_THRESHOLD = "capture_threshold"
        private const val CAPTURE_SETTING = "capture_setting"
        private const val DEFAULT_THRESHOLD = 0.75f
    }
}
