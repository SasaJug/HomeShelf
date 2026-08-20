package com.jugurdzija.homeshelf.pipeline

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import com.jugurdzija.homeshelf.data.GoldenItem
import com.jugurdzija.homeshelf.data.GoldenStore
import com.jugurdzija.homeshelf.data.StorageRepository
import com.jugurdzija.homeshelf.di.DiConstants
import com.jugurdzija.homeshelf.llm.CellPair
import com.jugurdzija.homeshelf.llm.KnownItem
import com.jugurdzija.homeshelf.llm.ShelfDiffAnalyzer
import com.jugurdzija.homeshelf.usecase.ComparisonPipeline
import com.jugurdzija.homeshelf.usecase.ComparisonResult
import com.jugurdzija.homeshelf.util.cellBoundsAsFraction
import com.jugurdzija.homeshelf.util.fromCellLocalFraction
import com.jugurdzija.homeshelf.util.resolveItemsByCell
import com.jugurdzija.homeshelf.util.toCellLocalFraction
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.opencv.android.OpenCVLoader
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import javax.inject.Inject
import javax.inject.Named
import kotlin.time.Duration.Companion.milliseconds

// Delay to LLM calls to prevent reaching limit.
private const val LLM_REQUEST_INTERVAL_MS = 6_500L

@HiltAndroidTest
class ComparisonPipelineGoldenTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var goldenStore: GoldenStore

    @Inject lateinit var pipeline: ComparisonPipeline

    @Inject lateinit var shelfDiffAnalyzer: ShelfDiffAnalyzer

    @Inject lateinit var storageRepository: StorageRepository

    @Inject
    @Named(DiConstants.NAMED_STORAGE_ROOT)
    lateinit var storageRoot: File

    @Before
    fun setup() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pkg = InstrumentationRegistry.getInstrumentation().targetContext.packageName
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .executeShellCommand("appops set $pkg MANAGE_EXTERNAL_STORAGE allow")
                .close()
        }
        hiltRule.inject()
        OpenCVLoader.initLocal()
    }

    @Test
    fun goldenSet_pipelineReport() = runBlocking {
        val candidates = goldenStore.loadAll().filterNot { it.isLegacyFormat() }
        val goldens = buildList {
            for (golden in candidates) {
                if (golden.hasMarkedItems()) add(golden)
            }
        }
        assumeTrue("No golden items with marked items in the current format — capture some via the app first", goldens.isNotEmpty())

        val goldenReports = goldens.mapIndexed { index, golden ->
            if (index > 0) delay(LLM_REQUEST_INTERVAL_MS.milliseconds)
            scoreGolden(golden)
        }

        val report = PipelineTestReport(
            timestamp = Instant.now().toString(),
            goldens = goldenReports
        )

        val dir = File(storageRoot, "test_results").apply { mkdirs() }
        val json = report.toJson().toString(2)
        File(dir, "pipeline_test_${System.currentTimeMillis()}.json").writeText(json)
        File(dir, "pipeline_test_latest.json").writeText(json)
    }

    private fun GoldenItem.isLegacyFormat(): Boolean =
        groundTruth.isNotEmpty() && groundTruth.all { it.name.isBlank() }

    private suspend fun GoldenItem.hasMarkedItems(): Boolean {
        val id = storageId ?: return false
        return storageRepository.loadLatestData(id).markedItems.isNotEmpty()
    }

    private fun Throwable.describeForReport(): String =
        generateSequence(this) { it.cause }
            .toList()
            .joinToString(" <- ") { "${it::class.simpleName}: ${it.message}" }

    private suspend fun scoreGolden(golden: GoldenItem): PipelineGoldenReport {
        val storageId = golden.storageId
            ?: return PipelineGoldenReport(golden.name, golden.referenceLabel, golden.timestamp, false, emptyList(), golden.toGroundTruthReports())

        val capturedBitmap = BitmapFactory.decodeFile(File(golden.dir, "photo.jpg").absolutePath)
            ?: return PipelineGoldenReport(golden.name, golden.referenceLabel, golden.timestamp, false, emptyList(), golden.toGroundTruthReports())

        val result = pipeline.run(capturedBitmap, storageId)
        if (result !is ComparisonResult.Success) {
            return PipelineGoldenReport(golden.name, golden.referenceLabel, golden.timestamp, false, emptyList(), golden.toGroundTruthReports())
        }

        saveAlignedBitmap(golden.name, result.alignedBitmap)

        val bitmapWidth = result.alignedBitmap.width
        val bitmapHeight = result.alignedBitmap.height
        val itemsByCell = resolveItemsByCell(result.markedItems, result.guideLines, bitmapWidth, bitmapHeight)
        val cellNameByItemId = itemsByCell.flatMap { (cellName, items) -> items.map { it.id to cellName } }.toMap()

        val newCellsByName = result.newCells.associateBy { it.name }
        val cellBoundsByName = mutableMapOf<String, RectF>()
        val pairs = result.referenceCells.mapNotNull { refCell ->
            val newCell = newCellsByName[refCell.name] ?: return@mapNotNull null
            val cellItems = itemsByCell[refCell.name].orEmpty()
            val cellBounds = cellBoundsAsFraction(result.guideLines, bitmapWidth, bitmapHeight, bitmapWidth, bitmapHeight, refCell.name)
                ?.also { cellBoundsByName[refCell.name] = it }
            CellPair(
                cellId = refCell.name,
                referenceBitmap = refCell.bitmap,
                newBitmap = newCell.bitmap,
                knownItems = cellItems.map { item ->
                    KnownItem(
                        id = item.id,
                        name = item.name,
                        isTransparentContainer = item.isTransparentContainer,
                        box = cellBounds?.let { toCellLocalFraction(item.boundingBox, it) }
                    )
                }
            )
        }
        val analyzeResult = shelfDiffAnalyzer.analyze(pairs)
        val analyzeError = analyzeResult.exceptionOrNull()?.let { it.describeForReport() }
        val cells = analyzeResult.getOrNull().orEmpty().map { cellDiff ->
            val cellBounds = cellBoundsByName[cellDiff.cellId]
            PipelineCellReport(
                cellId = cellDiff.cellId,
                aiItems = cellDiff.items.map { item ->
                    PipelineAiItemReport(
                        id = item.id,
                        change = item.change.name,
                        description = item.description,
                        name = item.name,
                        box = item.box?.let { box -> cellBounds?.let { fromCellLocalFraction(box, it) } }
                    )
                }
            )
        }
        val groundTruth = golden.toGroundTruthReports { itemId -> cellNameByItemId[itemId] }
        return PipelineGoldenReport(golden.name, golden.referenceLabel, golden.timestamp, true, cells, groundTruth, error = analyzeError)
    }

    private fun saveAlignedBitmap(goldenName: String, bitmap: Bitmap) {
        val dir = File(storageRoot, "test_results/aligned").apply { mkdirs() }
        FileOutputStream(File(dir, "$goldenName.jpg")).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
    }

    private fun GoldenItem.toGroundTruthReports(
        resolveCellName: (String) -> String? = { null }
    ): List<PipelineGroundTruthReport> = groundTruth.map {
        PipelineGroundTruthReport(
            itemId = it.itemId,
            name = it.name,
            changeType = it.changeType.name,
            cellName = it.cellName ?: it.itemId?.let(resolveCellName),
            box = it.box
        )
    }
}
