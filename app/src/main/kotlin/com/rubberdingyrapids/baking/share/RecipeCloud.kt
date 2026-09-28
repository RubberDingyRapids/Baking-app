package com.rubberdingyrapids.baking.share

import com.rubberdingyrapids.baking.core.data.RecipeJson
import com.rubberdingyrapids.baking.core.model.Recipe
import com.rubberdingyrapids.baking.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Publishes a recipe to a tiny HTTP store and fetches it back by link.
 *
 * The protocol is deliberately minimal so it can be re-implemented on any
 * server:
 *  - `POST <baseUrl>` with the recipe JSON as the body. Respond with JSON
 *    containing an id, either `{"id": "..."}` or `{"metadata": {"id": "..."}}`.
 *  - `GET <baseUrl>/<id>` returns the recipe JSON, optionally wrapped as
 *    `{"record": ...}`.
 *  - If an API key is set it is sent as the `X-Master-Key` header.
 * JsonBin (https://jsonbin.io) speaks exactly this out of the box.
 */
class RecipeCloud(private val settings: SettingsStore) {

    class CloudException(message: String) : IOException(message)

    /** Uploads [recipe] and returns the link that fetches it. */
    suspend fun publish(recipe: Recipe): String = withContext(Dispatchers.IO) {
        val cfg = settings.cloud.value
        if (!cfg.isConfigured) throw CloudException("Set a sharing server in Settings first.")
        if (cfg.baseUrl.contains("jsonbin.io") && cfg.apiKey.isBlank()) {
            throw CloudException("JsonBin needs an API key. Add yours in Settings.")
        }
        val conn = (URL(cfg.baseUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("X-Bin-Name", recipe.name.trim().take(120).ifBlank { "Recipe" })
            setRequestProperty("X-Bin-Private", "false")
            if (cfg.apiKey.isNotBlank()) setRequestProperty("X-Master-Key", cfg.apiKey)
        }
        try {
            conn.outputStream.use { it.write(RecipeJson.encodeRecipe(recipe).toByteArray()) }
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.readText().orEmpty()
            if (code !in 200..299) throw CloudException(serverMessage(code, body))
            val id = extractId(body) ?: throw CloudException("The server didn't return an id.")
            "${cfg.baseUrl}/$id"
        } finally {
            conn.disconnect()
        }
    }

    /** Downloads whatever [link] points at and decodes the recipes in it. */
    suspend fun fetch(link: String): List<Recipe> = withContext(Dispatchers.IO) {
        val url = resolve(link)
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/json")
            val key = settings.cloud.value.apiKey
            if (key.isNotBlank() && url.startsWith(settings.cloud.value.baseUrl)) setRequestProperty("X-Master-Key", key)
        }
        try {
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.readText().orEmpty()
            if (code !in 200..299) throw CloudException(serverMessage(code, body))
            runCatching { RecipeJson.decodeAny(body) }.getOrElse { throw CloudException("That link doesn't contain a recipe.") }
        } finally {
            conn.disconnect()
        }
    }

    /** Accepts a full link, or just an id which is appended to the configured server. */
    private fun resolve(link: String): String {
        val trimmed = link.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        val base = settings.cloud.value.baseUrl
        if (base.isBlank()) throw CloudException("Set a sharing server in Settings first.")
        return "$base/${trimmed.trimStart('/')}"
    }

    private val json = Json { ignoreUnknownKeys = true }

    private fun extractId(body: String): String? = runCatching {
        val obj = json.parseToJsonElement(body).jsonObject
        obj["metadata"]?.jsonObject?.get("id")?.jsonPrimitive?.content
            ?: obj["id"]?.jsonPrimitive?.content
    }.getOrNull()

    private fun serverMessage(code: Int, body: String): String {
        val detail = runCatching { json.parseToJsonElement(body).jsonObject["message"]?.jsonPrimitive?.content }.getOrNull()
        return when {
            detail != null -> "Server said: $detail"
            code == 401 || code == 403 -> "The server rejected the API key."
            code == 404 -> "Nothing found at that link."
            else -> "Server error $code."
        }
    }
}
