package com.jugurdzija.homeshelf.data

import kotlinx.serialization.Serializable

@Serializable
data class StorageItem(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long
)
