package com.jugurdzija.homeshelf.data

import kotlinx.serialization.Serializable

@Serializable
data class GuideLine(val id: Int, val isHorizontal: Boolean, val position: Float)
