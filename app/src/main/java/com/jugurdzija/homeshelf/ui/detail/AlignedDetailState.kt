package com.jugurdzija.homeshelf.ui.detail

sealed class AlignedDetailState {
    object Idle : AlignedDetailState()
    object Processing : AlignedDetailState()
    object NoReference : AlignedDetailState()
    object NoCells : AlignedDetailState()
    data class Done(val similarities: Map<String, Float>) : AlignedDetailState()
    data class Error(val message: String) : AlignedDetailState()
}
