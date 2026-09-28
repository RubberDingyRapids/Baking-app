package com.rubberdingyrapids.baking.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.rubberdingyrapids.baking.core.data.RecipeJson
import com.rubberdingyrapids.baking.core.data.RecipeStore
import com.rubberdingyrapids.baking.core.model.Recipe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Receives recipes from outside the app (opened files, shared JSON, pasted
 * links) and holds them until the user confirms the import. The UI observes
 * [state] and shows a dialog.
 */
class ImportCoordinator(
    private val context: Context,
    private val store: RecipeStore,
    private val cloud: RecipeCloud,
    private val scope: CoroutineScope,
) {
    sealed class State {
        object Idle : State()
        class Loading(val source: String) : State()
        class Ready(val recipes: List<Recipe>, val source: String) : State()
        class Failed(val message: String) : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    /** Returns true when the intent carried something to import. */
    fun handleIntent(intent: Intent?): Boolean {
        intent ?: return false
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                val uri = intent.data ?: return false
                if (uri.scheme == "http" || uri.scheme == "https") importFromLink(uri.toString()) else importFromUri(uri)
                return true
            }
            Intent.ACTION_SEND -> {
                @Suppress("DEPRECATION")
                val stream = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                when {
                    stream != null -> importFromUri(stream)
                    text != null -> importFromText(text)
                    else -> return false
                }
                return true
            }
        }
        return false
    }

    fun importFromUri(uri: Uri) = load("file") {
        val text = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        } ?: throw IllegalArgumentException("Couldn't read that file.")
        RecipeJson.decodeAny(text)
    }

    fun importFromLink(link: String) = load("link") { cloud.fetch(link) }

    fun importFromText(text: String) = load("text") {
        val trimmed = text.trim()
        val link = Regex("https?://\\S+").find(trimmed)?.value
        if (trimmed.startsWith("{")) RecipeJson.decodeAny(trimmed)
        else if (link != null) cloud.fetch(link)
        else throw IllegalArgumentException("No recipe or link found in that text.")
    }

    private fun load(source: String, block: suspend () -> List<Recipe>) {
        _state.value = State.Loading(source)
        scope.launch {
            _state.value = try {
                val recipes = block()
                if (recipes.isEmpty()) State.Failed("Nothing to import.") else State.Ready(recipes, source)
            } catch (e: Exception) {
                State.Failed(e.message?.takeIf { it.isNotBlank() } ?: "That doesn't look like a Cookbook recipe.")
            }
        }
    }

    /** Saves the pending recipes, replacing any with the same id. */
    fun confirm() {
        val ready = _state.value as? State.Ready ?: return
        scope.launch {
            ready.recipes.forEach { store.save(it) }
            _state.value = State.Idle
        }
    }

    fun dismiss() {
        _state.value = State.Idle
    }
}
