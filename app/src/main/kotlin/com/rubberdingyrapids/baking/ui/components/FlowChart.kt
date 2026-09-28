@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.rubberdingyrapids.baking.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SubdirectoryArrowLeft
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import com.rubberdingyrapids.baking.core.flow.FlowAnalysis
import com.rubberdingyrapids.baking.core.flow.FlowLayout
import com.rubberdingyrapids.baking.core.flow.FlowNode
import com.rubberdingyrapids.baking.core.flow.StepAnalysis

/**
 * Draws a recipe's steps as parallel lanes: independent chains sit side by
 * side and a step that uses several chains' results joins them. Each step is
 * drawn by [stepCard], so the editor, overview and cook mode share the shape
 * but decorate tiles differently.
 */
@Composable
fun FlowChart(
    analysis: FlowAnalysis,
    layout: FlowLayout,
    modifier: Modifier = Modifier,
    mutedLane: (FlowNode) -> Boolean = { false },
    stepCard: @Composable (step: StepAnalysis, node: FlowNode, compact: Boolean) -> Unit,
) {
    val byId = analysis.steps.associateBy { it.step.id }
    val compact = layout.laneCount > 1
    Column(modifier.fillMaxWidth()) {
        layout.rows.forEachIndexed { rowIndex, row ->
            val level = row.first().level
            if (rowIndex > 0) ConnectorRow(layout, level)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            ) {
                for (lane in 0 until layout.laneCount) {
                    val node = row.firstOrNull { it.lane == lane }
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        when {
                            node != null -> byId[node.stepId]?.let { stepCard(it, node, compact) }
                            layout.laneInFlight(lane, level) -> InFlightLine(
                                muted = layout.nodes.firstOrNull { it.lane == lane && it.level < level }?.let(mutedLane) ?: false,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Arrows between two rows: straight down where a lane continues, bent where it joins another lane. */
@Composable
private fun ConnectorRow(layout: FlowLayout, level: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        for (lane in 0 until layout.laneCount) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                val above = layout.nodes.firstOrNull { it.lane == lane && it.level == level - 1 }
                val consumer = above?.let { node ->
                    layout.nodes.firstOrNull { it.level == level && node.stepId in it.dependsOn }
                }
                when {
                    consumer != null && consumer.lane == lane -> FlowConnector()
                    consumer != null && consumer.lane < lane -> Icon(
                        Icons.Filled.SubdirectoryArrowLeft,
                        contentDescription = "Joins the lane to the left",
                        tint = MaterialTheme.colorScheme.outline,
                    )
                    consumer != null -> Icon(
                        Icons.Filled.SubdirectoryArrowRight,
                        contentDescription = "Joins the lane to the right",
                        tint = MaterialTheme.colorScheme.outline,
                    )
                    layout.laneInFlight(lane, level) || (above != null && above.consumedAtLevel != null) -> InFlightLine(height = 28.dp)
                    else -> Box(Modifier.height(28.dp))
                }
            }
        }
    }
}

/** Dashed line showing that something made earlier on this lane is waiting to be used. */
@Composable
private fun InFlightLine(modifier: Modifier = Modifier, muted: Boolean = false, height: androidx.compose.ui.unit.Dp? = null) {
    val color = MaterialTheme.colorScheme.outline.copy(alpha = if (muted) 0.25f else 0.5f)
    val sized = if (height != null) modifier.height(height) else modifier.fillMaxHeight()
    Box(sized.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.width(2.dp).fillMaxHeight()) {
            drawLine(
                color = color,
                start = Offset(size.width / 2, 0f),
                end = Offset(size.width / 2, size.height),
                strokeWidth = size.width,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
            )
        }
    }
}
