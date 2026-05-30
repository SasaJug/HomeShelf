package com.jugurdzija.homeshelf.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import com.jugurdzija.homeshelf.di.DiConstants
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class GuideLineStoreImpl @Inject constructor(
    @Named(DiConstants.NAMED_STORAGE_ROOT) private val storageRoot: File
) : GuideLineStore {

    private fun linesFile(imageFilePath: String): File {
        val name = File(imageFilePath).nameWithoutExtension
        return File(storageRoot, "grids/$name").apply { mkdirs() }.let {
            File(it, "lines.csv")
        }
    }

    override suspend fun save(imageFilePath: String, lines: List<GuideLine>) = withContext(Dispatchers.IO) {
        linesFile(imageFilePath).writeText(
            lines.joinToString("\n") { "${if (it.isHorizontal) "H" else "V"},${it.id},${it.position}" }
        )
    }

    override suspend fun load(imageFilePath: String): List<GuideLine> = withContext(Dispatchers.IO) {
        val file = linesFile(imageFilePath)
        if (!file.exists()) return@withContext emptyList()
        file.readLines().mapNotNull { raw ->
            val parts = raw.split(",")
            if (parts.size != 3) return@mapNotNull null
            val isH = parts[0] == "H"
            val id = parts[1].toIntOrNull() ?: return@mapNotNull null
            val pos = parts[2].toFloatOrNull() ?: return@mapNotNull null
            GuideLine(id = id, isHorizontal = isH, position = pos)
        }
    }
}
