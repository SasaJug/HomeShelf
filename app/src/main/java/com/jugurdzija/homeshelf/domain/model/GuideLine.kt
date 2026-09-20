package com.jugurdzija.homeshelf.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class GuideLine(val id: Int, val isHorizontal: Boolean, val position: Float)
