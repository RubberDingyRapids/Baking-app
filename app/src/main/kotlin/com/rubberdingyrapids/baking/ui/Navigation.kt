@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.rubberdingyrapids.baking.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.rubberdingyrapids.baking.BakingApp
import com.rubberdingyrapids.baking.ui.cook.CookScreen
import com.rubberdingyrapids.baking.ui.cook.CookViewModel
import com.rubberdingyrapids.baking.ui.editor.EditorViewModel
import com.rubberdingyrapids.baking.ui.editor.IngredientEditScreen
import com.rubberdingyrapids.baking.ui.editor.IngredientsScreen
import com.rubberdingyrapids.baking.ui.editor.MethodScreen
import com.rubberdingyrapids.baking.ui.editor.RecipeFormScreen
import com.rubberdingyrapids.baking.ui.list.RecipeListScreen
import com.rubberdingyrapids.baking.ui.overview.OverviewScreen
import com.rubberdingyrapids.baking.ui.settings.SettingsScreen
import com.rubberdingyrapids.baking.ui.share.ImportDialogHost
import kotlinx.serialization.Serializable

@Serializable object RecipeListRoute

/** Nested graph for creating or editing one recipe. All its screens share an [EditorViewModel]. */
@Serializable data class EditorRoute(val recipeId: String? = null)
@Serializable object EditorFormRoute
@Serializable object EditorIngredientsRoute
@Serializable data class EditorIngredientRoute(val ingredientId: String? = null)
@Serializable object EditorMethodRoute

@Serializable data class OverviewRoute(val recipeId: String)
@Serializable data class CookRoute(val recipeId: String, val scale: Float = 1f)
@Serializable object SettingsRoute

@Composable
fun app(): BakingApp = LocalContext.current.applicationContext as BakingApp

@Composable
fun BakingNavHost() {
    val navController = rememberNavController()
    ImportDialogHost()
    NavHost(navController = navController, startDestination = RecipeListRoute) {
        composable<RecipeListRoute> {
            RecipeListScreen(
                onAddRecipe = { navController.navigate(EditorRoute()) },
                onEditRecipe = { id -> navController.navigate(EditorRoute(id)) },
                onOpenRecipe = { id -> navController.navigate(OverviewRoute(id)) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
            )
        }

        composable<SettingsRoute> {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        navigation<EditorRoute>(startDestination = EditorFormRoute) {
            composable<EditorFormRoute> { entry ->
                val vm = editorViewModel(navController, entry)
                RecipeFormScreen(
                    viewModel = vm,
                    onOpenIngredients = { navController.navigate(EditorIngredientsRoute) },
                    onOpenMethod = { navController.navigate(EditorMethodRoute) },
                    onClose = { navController.popBackStack<EditorRoute>(inclusive = true) },
                )
            }
            composable<EditorIngredientsRoute> { entry ->
                val vm = editorViewModel(navController, entry)
                IngredientsScreen(
                    viewModel = vm,
                    onAddIngredient = { navController.navigate(EditorIngredientRoute()) },
                    onEditIngredient = { id -> navController.navigate(EditorIngredientRoute(id)) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<EditorIngredientRoute> { entry ->
                val vm = editorViewModel(navController, entry)
                val route = entry.toRoute<EditorIngredientRoute>()
                IngredientEditScreen(
                    viewModel = vm,
                    ingredientId = route.ingredientId,
                    onDone = { navController.popBackStack() },
                )
            }
            composable<EditorMethodRoute> { entry ->
                val vm = editorViewModel(navController, entry)
                MethodScreen(viewModel = vm, onBack = { navController.popBackStack() })
            }
        }

        composable<OverviewRoute> { entry ->
            val route = entry.toRoute<OverviewRoute>()
            OverviewScreen(
                recipeId = route.recipeId,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(EditorRoute(route.recipeId)) },
                onStart = { scale -> navController.navigate(CookRoute(route.recipeId, scale)) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
            )
        }

        composable<CookRoute> { entry ->
            val route = entry.toRoute<CookRoute>()
            CookScreen(
                viewModel = viewModel(factory = CookViewModel.Factory),
                onBack = { navController.popBackStack() },
                onFinished = { navController.popBackStack<RecipeListRoute>(inclusive = false) },
            )
        }
    }
}

/** The editor's ViewModel lives on the nested graph entry so every editor screen sees the same draft. */
@Composable
private fun editorViewModel(navController: NavController, entry: NavBackStackEntry): EditorViewModel {
    val parentEntry = remember(entry) { navController.getBackStackEntry<EditorRoute>() }
    return viewModel(viewModelStoreOwner = parentEntry, factory = EditorViewModel.Factory)
}
