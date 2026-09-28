package com.rubberdingyrapids.baking.core.flow

import com.rubberdingyrapids.baking.core.model.Recipe

/** Where one step sits in the branched chart. */
data class FlowNode(
    val stepId: String,
    /** Row, counted from the top. A step is one row below the latest step it consumes from. */
    val level: Int,
    /** Column. Independent chains get their own lane; a step that joins chains takes the leftmost. */
    val lane: Int,
    val dependsOn: Set<String>,
    /** Level of the first step that consumes this step's output, or null if nothing does. */
    val consumedAtLevel: Int?,
)

data class FlowLayout(
    val nodes: List<FlowNode>,
    val laneCount: Int,
    /** Step ids per row, ordered by lane. */
    val rows: List<List<FlowNode>>,
) {
    val byId: Map<String, FlowNode> = nodes.associateBy { it.stepId }

    /** True when something produced on [lane] above [level] is still waiting to be used further down. */
    fun laneInFlight(lane: Int, level: Int): Boolean = nodes.any { node ->
        node.lane == lane && node.level < level && (node.consumedAtLevel ?: Int.MAX_VALUE) > level
    }

    /** True when the step at [lane]/[level] continues into the row below. */
    fun laneContinues(lane: Int, level: Int): Boolean = nodes.any { node ->
        node.lane == lane && node.level == level && node.consumedAtLevel != null
    }
}

/**
 * Lays a recipe's steps out as parallel lanes. Chains that share nothing run
 * side by side; a step that uses the output of several chains joins them.
 * Steps must already be in dependency order (see [FlowEngine.normalise]).
 */
object FlowLayoutEngine {

    fun layout(recipe: Recipe): FlowLayout {
        val deps = FlowEngine.dependencies(recipe)
        val level = mutableMapOf<String, Int>()
        val lane = mutableMapOf<String, Int>()
        // lane -> step whose output is still unconsumed on that lane
        val openLanes = mutableMapOf<Int, String>()

        recipe.steps.forEach { step ->
            val d = deps.getValue(step.id).filter { it in level }
            level[step.id] = if (d.isEmpty()) 0 else d.maxOf { level.getValue(it) } + 1

            val continuable = d.mapNotNull { dep -> lane[dep]?.takeIf { openLanes[it] == dep } }
            continuable.forEach { openLanes.remove(it) }
            val chosen = continuable.minOrNull() ?: generateSequence(0) { it + 1 }.first { it !in openLanes }
            lane[step.id] = chosen
            openLanes[chosen] = step.id
        }

        val consumers = mutableMapOf<String, Int>()
        recipe.steps.forEach { step ->
            deps.getValue(step.id).forEach { dep ->
                val at = level.getValue(step.id)
                consumers[dep] = minOf(consumers[dep] ?: at, at)
            }
        }

        val nodes = recipe.steps.map { step ->
            FlowNode(step.id, level.getValue(step.id), lane.getValue(step.id), deps.getValue(step.id), consumers[step.id])
        }
        val rows = nodes.groupBy { it.level }.toSortedMap().values.map { row -> row.sortedBy { it.lane } }
        return FlowLayout(nodes, (nodes.maxOfOrNull { it.lane } ?: -1) + 1, rows)
    }
}
