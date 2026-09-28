package com.rubberdingyrapids.baking.core

import java.util.UUID

/** Generates stable, globally unique ids for recipes, ingredients and steps. */
object Ids {
    fun next(): String = UUID.randomUUID().toString()
}
