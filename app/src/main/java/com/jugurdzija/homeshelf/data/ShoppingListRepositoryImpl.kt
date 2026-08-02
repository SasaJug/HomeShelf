package com.jugurdzija.homeshelf.data

import com.jugurdzija.homeshelf.di.DiConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ShoppingListRepositoryImpl @Inject constructor(
    @Named(DiConstants.NAMED_STORAGE_ROOT) private val storageRoot: File
) : ShoppingListRepository {

    private companion object {
        const val FILE_SHOPPING_LIST = "shopping_list.json"
    }

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private fun file(): File = File(storageRoot, FILE_SHOPPING_LIST)

    private fun readAll(): List<ShoppingListItem> {
        val f = file()
        if (!f.exists()) return emptyList()
        return json.decodeFromString(ListSerializer(ShoppingListItem.serializer()), f.readText())
    }

    private fun writeAll(items: List<ShoppingListItem>) {
        file().writeText(json.encodeToString(ListSerializer(ShoppingListItem.serializer()), items))
    }

    override suspend fun loadAll(): List<ShoppingListItem> = withContext(Dispatchers.IO) {
        readAll().sortedByDescending { it.createdAt }
    }

    override suspend fun add(name: String, storageId: String?): ShoppingListItem = withContext(Dispatchers.IO) {
        val item = ShoppingListItem(
            id = UUID.randomUUID().toString(),
            name = name,
            storageId = storageId,
            createdAt = System.currentTimeMillis()
        )
        writeAll(readAll() + item)
        item
    }

    override suspend fun addAutoDetected(candidates: List<Pair<String, String>>): List<ShoppingListItem> =
        withContext(Dispatchers.IO) {
            val current = readAll()
            val existingKeys = current.map { it.name.trim().lowercase() to it.storageId }.toSet()
            val added = mutableListOf<ShoppingListItem>()
            val seenKeys = existingKeys.toMutableSet()
            candidates.forEach { (name, storageId) ->
                val key = name.trim().lowercase() to storageId
                if (key !in seenKeys) {
                    seenKeys += key
                    added += ShoppingListItem(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        storageId = storageId,
                        createdAt = System.currentTimeMillis()
                    )
                }
            }
            if (added.isNotEmpty()) writeAll(current + added)
            added
        }

    override suspend fun remove(id: String) = withContext(Dispatchers.IO) {
        writeAll(readAll().filterNot { it.id == id })
    }
}
