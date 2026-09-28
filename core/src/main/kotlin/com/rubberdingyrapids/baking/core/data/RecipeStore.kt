package com.rubberdingyrapids.baking.core.data

import com.rubberdingyrapids.baking.core.model.Recipe
import com.rubberdingyrapids.baking.core.model.RecipeLibrary
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.io.File

/** JSON codec shared by storage and (later) sharing/export. */
object RecipeJson {
    val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encodeRecipe(recipe: Recipe): String = json.encodeToString(Recipe.serializer(), recipe)
    fun decodeRecipe(text: String): Recipe = json.decodeFromString(Recipe.serializer(), text)
    fun encodeLibrary(library: RecipeLibrary): String = json.encodeToString(RecipeLibrary.serializer(), library)
    fun decodeLibrary(text: String): RecipeLibrary = json.decodeFromString(RecipeLibrary.serializer(), text)

    /**
     * Reads whatever a share might contain: a single recipe, a whole library,
     * or either of those wrapped in a server envelope such as JsonBin's
     * `{"record": ..., "metadata": ...}`. Throws when nothing sensible is found.
     */
    fun decodeAny(text: String): List<Recipe> {
        var element: JsonElement = json.parseToJsonElement(text)
        while (element is JsonObject && "record" in element && "name" !in element && "recipes" !in element) {
            element = element.getValue("record")
        }
        val obj = element as? JsonObject ?: throw IllegalArgumentException("Not a recipe")
        return when {
            "recipes" in obj -> json.decodeFromJsonElement(RecipeLibrary.serializer(), obj).recipes
            "name" in obj -> listOf(json.decodeFromJsonElement(Recipe.serializer(), obj))
            else -> throw IllegalArgumentException("Not a recipe")
        }
    }
}

/**
 * Keeps all recipes in one JSON file and exposes them as a [StateFlow].
 * Writes are atomic (temp file + rename) so a crash mid-write cannot corrupt
 * the library.
 */
class RecipeStore(
    private val directory: File,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val file = File(directory, FILE_NAME)
    private val mutex = Mutex()
    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes: StateFlow<List<Recipe>> = _recipes.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    /** True once the library file has been read (or found absent). */
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    suspend fun load() = mutex.withLock {
        if (_loaded.value) return@withLock
        val library = withContext(io) {
            if (!file.exists()) return@withContext RecipeLibrary()
            runCatching { RecipeJson.decodeLibrary(file.readText()) }.getOrElse {
                // Keep a copy of anything unreadable rather than silently losing it.
                file.copyTo(File(directory, "$FILE_NAME.corrupt-${System.currentTimeMillis()}"), overwrite = true)
                RecipeLibrary()
            }
        }
        _recipes.value = library.recipes.sortedByDescending { it.updatedAt }
        _loaded.value = true
    }

    fun get(id: String): Recipe? = _recipes.value.firstOrNull { it.id == id }

    /** Inserts or replaces [recipe] and persists the library. */
    suspend fun save(recipe: Recipe) {
        val stamped = recipe.copy(updatedAt = System.currentTimeMillis())
        update { list -> list.filterNot { it.id == stamped.id } + stamped }
    }

    suspend fun delete(id: String) = update { list -> list.filterNot { it.id == id } }

    private suspend fun update(transform: (List<Recipe>) -> List<Recipe>) = mutex.withLock {
        val next = transform(_recipes.value).sortedByDescending { it.updatedAt }
        _recipes.value = next
        withContext(io) { writeAtomically(RecipeLibrary(recipes = next)) }
    }

    private fun writeAtomically(library: RecipeLibrary) {
        directory.mkdirs()
        val tmp = File(directory, "$FILE_NAME.tmp")
        tmp.writeText(RecipeJson.encodeLibrary(library))
        if (!tmp.renameTo(file)) {
            file.delete()
            if (!tmp.renameTo(file)) tmp.copyTo(file, overwrite = true)
        }
    }

    companion object {
        const val FILE_NAME = "recipes.json"
    }
}
