package com.jugurdzija.homeshelf.data

import kotlinx.serialization.Serializable

@Serializable
data class ShoppingListItem(
    val id: String,
    val name: String,
    val storageId: String? = null,
    val createdAt: Long
)
