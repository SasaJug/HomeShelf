package com.jugurdzija.homeshelf.pipeline

import org.json.JSONArray
import org.json.JSONObject

data class PipelineCellReport(
    val cellIndex: Int,
    val groundTruth: String,
    val actualChanged: Boolean,
    val changed: Boolean? = null,
    val description: String? = null
)

data class PipelineGoldenReport(
    val goldenName: String,
    val referenceLabel: String,
    val capturedAt: String,
    val alignmentSuccess: Boolean,
    val cells: List<PipelineCellReport>,
    val error: String? = null
)

data class Metrics(
    val totalCells: Int,
    val tp: Int,
    val tn: Int,
    val fp: Int,
    val fn: Int,
    val accuracy: Double?,
    val precision: Double?,
    val recall: Double?,
    val f1: Double?
)

data class PipelineTestReport(
    val timestamp: String,
    val totalCells: Int,
    val metrics: Metrics?,
    val goldens: List<PipelineGoldenReport>
)

fun PipelineTestReport.toJson(): JSONObject = JSONObject().apply {
    put("timestamp", timestamp)
    put("total_cells", totalCells)
    put("metrics", metrics?.let { m ->
        JSONObject().apply {
            put("total_cells", m.totalCells)
            put("accuracy", m.accuracy)
            put("precision", m.precision)
            put("recall", m.recall)
            put("f1", m.f1)
            put("confusion_matrix", JSONObject().apply {
                put("predicted_changed_true", JSONObject().apply {
                    put("actual_true", m.tp)
                    put("actual_false", m.fp)
                })
                put("predicted_changed_false", JSONObject().apply {
                    put("actual_true", m.fn)
                    put("actual_false", m.tn)
                })
            })
        }
    })
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
                            put("cell_index", cell.cellIndex)
                            put("ground_truth", cell.groundTruth)
                            put("actual_changed", cell.actualChanged)
                            put("changed", cell.changed)
                            put("description", cell.description)
                        })
                    }
                })
            })
        }
    })
}
