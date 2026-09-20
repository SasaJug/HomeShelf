package com.jugurdzija.homeshelf.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class StorageItem(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long
)
