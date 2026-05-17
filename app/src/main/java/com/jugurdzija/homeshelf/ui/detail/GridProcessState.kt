package com.jugurdzija.homeshelf.ui.detail

sealed class GridProcessState {
    object Idle : GridProcessState()
    object Processing : GridProcessState()
    data class Done(val cellCount: Int) : GridProcessState()
    object NoCells : GridProcessState()
    data class Error(val message: String) : GridProcessState()
}
