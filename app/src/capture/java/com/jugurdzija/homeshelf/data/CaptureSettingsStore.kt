package com.jugurdzija.homeshelf.data

import kotlinx.coroutines.flow.StateFlow

interface CaptureSettingsStore {
    val captureThreshold: StateFlow<Float>
    fun setCaptureThreshold(value: Float)
}
