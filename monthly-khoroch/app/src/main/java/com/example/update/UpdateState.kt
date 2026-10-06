package com.example.update

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class Available(val manifest: UpdateManifest) : UpdateState
    data class Downloading(val manifest: UpdateManifest, val progress: Float) : UpdateState
    data class Failed(val manifest: UpdateManifest, val message: String) : UpdateState
}
