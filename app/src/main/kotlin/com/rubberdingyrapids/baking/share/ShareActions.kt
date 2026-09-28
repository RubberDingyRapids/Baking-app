package com.rubberdingyrapids.baking.share

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.rubberdingyrapids.baking.core.data.RecipeJson
import com.rubberdingyrapids.baking.core.model.Recipe
import com.rubberdingyrapids.baking.core.model.RecipeLibrary
import com.rubberdingyrapids.baking.core.share.RecipeText
import java.io.File

/** Hands recipes to the Android share sheet as files or text. */
object ShareActions {
    const val FILE_EXTENSION = ".cookbook.json"

    fun shareRecipeFile(context: Context, recipe: Recipe) {
        shareFile(context, "${slug(recipe.name)}$FILE_EXTENSION", RecipeJson.encodeRecipe(recipe), recipe.name)
    }

    fun shareLibraryFile(context: Context, recipes: List<Recipe>) {
        shareFile(context, "all-recipes$FILE_EXTENSION", RecipeJson.encodeLibrary(RecipeLibrary(recipes = recipes)), "All recipes")
    }

    fun shareRecipeText(context: Context, recipe: Recipe) {
        shareText(context, recipe.name, RecipeText.render(recipe))
    }

    fun shareLink(context: Context, recipe: Recipe, link: String) {
        shareText(
            context, recipe.name,
            "${recipe.name.trim()}\n\nOpen Cookbook, choose \"Import from link\" and paste:\n$link",
        )
    }

    fun shareText(context: Context, subject: String, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, subject).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun shareFile(context: Context, fileName: String, content: String, title: String) {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, fileName).apply { writeText(content) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun slug(name: String): String =
        name.trim().lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "recipe" }
}
