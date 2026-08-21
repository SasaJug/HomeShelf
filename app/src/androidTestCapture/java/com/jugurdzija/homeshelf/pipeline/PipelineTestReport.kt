package com.jugurdzija.homeshelf.pipeline

import com.jugurdzija.homeshelf.data.BoundingBox
import org.json.JSONArray
import org.json.JSONObject

data class PipelineAiItemReport(
    val id: String,
    val change: String,
    val description: String,
    val name: String?,
    val box: BoundingBox?
)

data class PipelineCellReport(
    val cellId: String,
    val aiItems: List<PipelineAiItemReport>
)

data class PipelineGroundTruthReport(
    val itemId: String?,
    val name: String,
    val changeType: String,
    val cellName: String?,
    val box: BoundingBox?
)

data class PipelineGoldenReport(
    val goldenName: String,
    val referenceLabel: String,
    val capturedAt: String,
    val alignmentSuccess: Boolean,
    val cells: List<PipelineCellReport>,
    val groundTruth: List<PipelineGroundTruthReport>,
    val error: String? = null
)

data class PipelineTestReport(
    val timestamp: String,
    val goldens: List<PipelineGoldenReport>
)

private fun BoundingBox.toJson(): JSONObject = JSONObject().apply {
    put("x", x)
    put("y", y)
    put("width", width)
    put("height", height)
}

fun PipelineTestReport.toJson(): JSONObject = JSONObject().apply {
    put("timestamp", timestamp)
    put("goldens", JSONArray().apply {
        goldens.forEach { golden ->
            put(JSONObject().apply {
                put("golden_name", golden.goldenName)
                put("reference_label", golden.referenceLabel)
                put("captured_at", golden.capturedAt)
                put("alignment_success", golden.alignmentSuccess)
                put("error", golden.error)
                put("cells", JSONArray().apply {
                    golden.cells.forEach { cell ->
                        put(JSONObject().apply {
                            put("cell_id", cell.cellId)
                            put("ai_items", JSONArray().apply {
                                cell.aiItems.forEach { aiItem ->
                                    put(JSONObject().apply {
                                        put("id", aiItem.id)
                                        put("change", aiItem.change)
                                        put("description", aiItem.description)
                                        put("name", aiItem.name)
                                        aiItem.box?.let { put("box", it.toJson()) }
                                    })
                                }
                            })
                        })
                    }
                })
                put("ground_truth", JSONArray().apply {
                    golden.groundTruth.forEach { gt ->
                        put(JSONObject().apply {
                            put("item_id", gt.itemId)
                            put("name", gt.name)
                            put("change_type", gt.changeType)
                            put("cell_name", gt.cellName)
                            gt.box?.let { put("box", it.toJson()) }
                        })
                    }
                })
            })
        }
    })
}
