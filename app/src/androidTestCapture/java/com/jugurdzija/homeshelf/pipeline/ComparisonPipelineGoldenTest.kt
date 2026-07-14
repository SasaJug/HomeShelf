package com.jugurdzija.homeshelf.pipeline

import android.graphics.BitmapFactory
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import com.jugurdzija.homeshelf.data.ChangeType
import com.jugurdzija.homeshelf.data.GoldenStore
import com.jugurdzija.homeshelf.di.DiConstants
import com.jugurdzija.homeshelf.llm.CellPair
import com.jugurdzija.homeshelf.llm.ShelfDiffAnalyzer
import com.jugurdzija.homeshelf.usecase.ComparisonPipeline
import com.jugurdzija.homeshelf.usecase.ComparisonResult
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
        val goldens = goldenStore.loadAll()
        assumeTrue("No golden items captured yet — capture some via the app first", goldens.isNotEmpty())

        val goldenReports = goldens.mapIndexed { index, golden ->
            if (index > 0) delay(LLM_REQUEST_INTERVAL_MS.milliseconds)
            scoreGolden(golden)
        }

        val allCells = goldenReports.flatMap { it.cells }
        val analyzedCells = allCells.filter { it.changed != null }
        val metrics = if (analyzedCells.isEmpty()) null else {
            val tp = analyzedCells.count { it.actualChanged && it.changed == true }
            val tn = analyzedCells.count { !it.actualChanged && it.changed == false }
            val fp = analyzedCells.count { !it.actualChanged && it.changed == true }
            val fn = analyzedCells.count { it.actualChanged && it.changed == false }
            val precision = if (tp + fp == 0) null else tp.toDouble() / (tp + fp)
            val recall = if (tp + fn == 0) null else tp.toDouble() / (tp + fn)
            val f1 = if (precision == null || recall == null || precision + recall == 0.0) null
                     else 2.0 * precision * recall / (precision + recall)
            Metrics(
                totalCells = analyzedCells.size,
                tp = tp, tn = tn, fp = fp, fn = fn,
                accuracy = (tp + tn).toDouble() / analyzedCells.size,
                precision = precision,
                recall = recall,
                f1 = f1
            )
        }
        val report = PipelineTestReport(
            timestamp = Instant.now().toString(),
            totalCells = allCells.size,
            metrics = metrics,
            goldens = goldenReports
        )

        val dir = File(storageRoot, "test_results").apply { mkdirs() }
        val json = report.toJson().toString(2)
        File(dir, "pipeline_test_${System.currentTimeMillis()}.json").writeText(json)
        File(dir, "pipeline_test_latest.json").writeText(json)
    }

    private suspend fun scoreGolden(golden: com.jugurdzija.homeshelf.data.GoldenItem): PipelineGoldenReport {
        val storageId = golden.storageId
            ?: return PipelineGoldenReport(golden.name, golden.referenceLabel, golden.timestamp, false, emptyList())

        val capturedBitmap = BitmapFactory.decodeFile(File(golden.dir, "photo.jpg").absolutePath)
            ?: return PipelineGoldenReport(golden.name, golden.referenceLabel, golden.timestamp, false, emptyList())

        val result = pipeline.run(capturedBitmap, storageId)
        if (result !is ComparisonResult.Success) {
            return PipelineGoldenReport(golden.name, golden.referenceLabel, golden.timestamp, false, emptyList())
        }

        val newCellsByName = result.newCells.associateBy { it.name }
        val pairs = result.referenceCells.mapNotNull { refCell ->
            val newCell = newCellsByName[refCell.name] ?: return@mapNotNull null
            CellPair(cellId = refCell.name, referenceBitmap = refCell.bitmap, newBitmap = newCell.bitmap)
        }
        val analyzeResult = shelfDiffAnalyzer.analyze(pairs)
        val aiByCellId = analyzeResult.getOrNull()?.associateBy { it.cellId }
        val analyzeError = analyzeResult.exceptionOrNull()?.message

        val groundTruthMap = golden.groundTruth.associate { it.cellIndex to it.changeType }
        val cellNames = result.similarities.keys
        val numCols = (cellNames.maxOfOrNull { it[0] - 'A' } ?: 0) + 1
        val cells = cellNames.map { name ->
            val colIdx = name[0] - 'A'
            val rowIdx = name.substring(1).toInt() - 1
            val cellIndex = rowIdx * numCols + colIdx
            val groundTruth = groundTruthMap[cellIndex] ?: ChangeType.NO_CHANGE
            val aiCell = aiByCellId?.get(name)
            PipelineCellReport(
                cellIndex = cellIndex,
                groundTruth = groundTruth.name,
                actualChanged = groundTruth != ChangeType.NO_CHANGE,
                changed = aiCell?.changed,
                description = aiCell?.description
            )
        }
        return PipelineGoldenReport(golden.name, golden.referenceLabel, golden.timestamp, true, cells, error = analyzeError)
    }
}
