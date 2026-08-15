package com.jugurdzija.homeshelf.pipeline

import android.graphics.BitmapFactory
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
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

// Just adjusted the tests to compile. It does not work properly at this moment.
// This will be fixed in the next phase.
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

        val report = PipelineTestReport(
            timestamp = Instant.now().toString(),
            goldens = goldenReports
        )

        val dir = File(storageRoot, "test_results").apply { mkdirs() }
        val json = report.toJson().toString(2)
        File(dir, "pipeline_test_${System.currentTimeMillis()}.json").writeText(json)
        File(dir, "pipeline_test_latest.json").writeText(json)
    }

    private suspend fun scoreGolden(golden: com.jugurdzija.homeshelf.data.GoldenItem): PipelineGoldenReport {
        val groundTruth = golden.groundTruth.map {
            PipelineGroundTruthReport(
                itemId = it.itemId,
                name = it.name,
                changeType = it.changeType.name,
                cellName = it.cellName
            )
        }
        val storageId = golden.storageId
            ?: return PipelineGoldenReport(golden.name, golden.referenceLabel, golden.timestamp, false, emptyList(), groundTruth)

        val capturedBitmap = BitmapFactory.decodeFile(File(golden.dir, "photo.jpg").absolutePath)
            ?: return PipelineGoldenReport(golden.name, golden.referenceLabel, golden.timestamp, false, emptyList(), groundTruth)

        val result = pipeline.run(capturedBitmap, storageId)
        if (result !is ComparisonResult.Success) {
            return PipelineGoldenReport(golden.name, golden.referenceLabel, golden.timestamp, false, emptyList(), groundTruth)
        }

        val newCellsByName = result.newCells.associateBy { it.name }
        val pairs = result.referenceCells.mapNotNull { refCell ->
            val newCell = newCellsByName[refCell.name] ?: return@mapNotNull null
            CellPair(cellId = refCell.name, referenceBitmap = refCell.bitmap, newBitmap = newCell.bitmap)
        }
        val analyzeResult = shelfDiffAnalyzer.analyze(pairs)
        val analyzeError = analyzeResult.exceptionOrNull()?.message
        val cells = analyzeResult.getOrNull().orEmpty().map { cellDiff ->
            PipelineCellReport(
                cellId = cellDiff.cellId,
                aiItems = cellDiff.items.map { item ->
                    PipelineAiItemReport(
                        id = item.id,
                        change = item.change.name,
                        description = item.description,
                        name = item.name
                    )
                }
            )
        }
        return PipelineGoldenReport(golden.name, golden.referenceLabel, golden.timestamp, true, cells, groundTruth, error = analyzeError)
    }
}
