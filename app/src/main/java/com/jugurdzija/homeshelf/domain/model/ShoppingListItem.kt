package com.jugurdzija.homeshelf.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ShoppingListItem(
    val id: String,
    val name: String,
    val storageId: String? = null,
    val createdAt: Long
)
