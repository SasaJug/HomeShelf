package com.jugurdzija.homeshelf.ui.test

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jugurdzija.homeshelf.data.ChangeType
import com.jugurdzija.homeshelf.data.GoldenItem
import com.jugurdzija.homeshelf.data.GoldenStore
import com.jugurdzija.homeshelf.data.StorageRepository
import com.jugurdzija.homeshelf.di.DiConstants
import com.jugurdzija.homeshelf.usecase.ComparisonPipeline
import com.jugurdzija.homeshelf.usecase.ComparisonResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.opencv.android.OpenCVLoader
import java.io.File
import java.time.Instant
import javax.inject.Inject
import javax.inject.Named

data class TestReferenceItem(
    val label: String,
    val thumbnailBitmap: Bitmap?,
    val goldenCount: Int
)

data class TestRunResult(
    val timestamp: String,
    val results: List<TestImageResult>
)

data class TestImageResult(
    val goldenName: String,
    val referenceLabel: String,
    val alignmentSuccess: Boolean,
    val cells: List<TestCellResult>
)

data class TestCellResult(
    val cellIndex: Int,
    val similarityScore: Float,
    val groundTruth: String
)

@HiltViewModel
class TestViewModel @Inject constructor(
    private val goldenStore: GoldenStore,
    private val pipeline: ComparisonPipeline,
    private val storageRepository: StorageRepository,
    @Named(DiConstants.NAMED_STORAGE_ROOT) private val storageRoot: File
) : ViewModel() {

    sealed interface State {
        data object Loading : State
        data class Selecting(
            val references: List<TestReferenceItem>,
            val selected: Set<String>
        ) : State
        data class Running(val current: Int, val total: Int) : State
        data class Done(val run: TestRunResult) : State
    }

    sealed interface Event {
        data object NavigateToRun : Event
        data object NavigateToResults : Event
        data class ShareFile(val file: File) : Event
    }

    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _events = Channel<Event>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        OpenCVLoader.initLocal()
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val all = goldenStore.loadAll()
            val groups = all.groupBy { it.referenceLabel }
            val references = groups.map { (label, items) ->
                val firstItem = items.first()
                val thumbnail = withContext(Dispatchers.IO) {
                    val refPath = firstItem.referenceFilePath
                    if (refPath.isNotEmpty() && File(refPath).exists()) {
                        BitmapFactory.decodeFile(
                            refPath,
                            BitmapFactory.Options().apply { inSampleSize = 4 }
                        )
                    } else {
                        null
                    }
                }
                TestReferenceItem(
                    label = label,
                    thumbnailBitmap = thumbnail,
                    goldenCount = items.size
                )
            }
            _state.value = State.Selecting(references, emptySet())
        }
    }

    fun toggleSelection(label: String) {
        val current = _state.value as? State.Selecting ?: return
        val newSelected = if (label in current.selected) {
            current.selected - label
        } else {
            current.selected + label
        }
        _state.value = current.copy(selected = newSelected)
    }

    fun runTests() {
        val current = _state.value as? State.Selecting ?: return
        if (current.selected.isEmpty()) return
        viewModelScope.launch {
            val all = goldenStore.loadAll()
            val toTest = all.filter { it.referenceLabel in current.selected }
            _state.value = State.Running(0, toTest.size)
            _events.send(Event.NavigateToRun)
            val results = mutableListOf<TestImageResult>()
            toTest.forEachIndexed { index, item ->
                results.add(processGoldenItem(item))
                _state.value = State.Running(index + 1, toTest.size)
            }
            val run = TestRunResult(
                timestamp = Instant.now().toString(),
                results = results
            )
            _state.value = State.Done(run)
            _events.send(Event.NavigateToResults)
        }
    }

    private suspend fun processGoldenItem(item: GoldenItem): TestImageResult {
        val storageId = storageRepository.loadAll().firstOrNull { it.name == item.referenceLabel }?.id
            ?: return TestImageResult(item.name, item.referenceLabel, false, emptyList())

        val capturedBitmap = withContext(Dispatchers.IO) {
            BitmapFactory.decodeFile(File(item.dir, "photo.jpg").absolutePath)
        } ?: return TestImageResult(item.name, item.referenceLabel, false, emptyList())

        return when (val result = pipeline.run(capturedBitmap, storageId)) {
            is ComparisonResult.Success -> {
                val groundTruthMap = item.groundTruth.associate { it.cellIndex to it.changeType }
                val numCols = (result.similarities.keys.maxOfOrNull { it[0] - 'A' } ?: 0) + 1
                val cellResults = result.similarities.map { (name, similarity) ->
                    val colIdx = name[0] - 'A'
                    val rowIdx = name.substring(1).toInt() - 1
                    val cellIndex = rowIdx * numCols + colIdx
                    val groundTruth = groundTruthMap[cellIndex]?.name ?: ChangeType.NO_CHANGE.name
                    TestCellResult(cellIndex, similarity, groundTruth)
                }
                TestImageResult(item.name, item.referenceLabel, true, cellResults)
            }
            ComparisonResult.AlignmentFailed -> TestImageResult(item.name, item.referenceLabel, false, emptyList())
            ComparisonResult.NoCells -> TestImageResult(item.name, item.referenceLabel, true, emptyList())
            ComparisonResult.NoGuideLines, ComparisonResult.NoEmbeddings -> TestImageResult(item.name, item.referenceLabel, false, emptyList())
        }
    }

    fun saveAndShare() {
        val done = _state.value as? State.Done ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val dir = File(storageRoot, "test_results").apply { mkdirs() }
            val file = File(dir, "test_${System.currentTimeMillis()}.json")
            file.writeText(toJson(done.run).toString(2))
            _events.send(Event.ShareFile(file))
        }
    }

    private fun toJson(run: TestRunResult): JSONObject {
        val resultsArr = JSONArray()
        run.results.forEach { result ->
            val cellsArr = JSONArray()
            result.cells.forEach { cell ->
                cellsArr.put(JSONObject().apply {
                    put("cell_index", cell.cellIndex)
                    put("similarity_score", cell.similarityScore.toDouble())
                    put("ground_truth", cell.groundTruth)
                })
            }
            resultsArr.put(JSONObject().apply {
                put("golden_name", result.goldenName)
                put("reference_label", result.referenceLabel)
                put("alignment_success", result.alignmentSuccess)
                put("cells", cellsArr)
            })
        }
        return JSONObject().apply {
            put("run_timestamp", run.timestamp)
            put("results", resultsArr)
        }
    }
}
