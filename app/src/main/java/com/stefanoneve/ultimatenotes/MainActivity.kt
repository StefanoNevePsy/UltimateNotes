package com.stefanoneve.ultimatenotes

import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stefanoneve.ultimatenotes.ui.editor.EditorScreen
import com.stefanoneve.ultimatenotes.ui.home.HomeScreen
import com.stefanoneve.ultimatenotes.ui.theme.UltimateNotesTheme
import com.stefanoneve.ultimatenotes.util.SPenEvents

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settingsStore = (application as UltimateNotesApp).settingsStore
        setContent {
            val settings by settingsStore.settings.collectAsState()
            UltimateNotesTheme(themeId = settings.themeId) {
                AppNavHost()
            }
        }
    }

    // Route every motion event through SPenEvents so the radial menu can
    // react to the S Pen barrel button anywhere in the app.
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        SPenEvents.onMotion(ev)
        return super.dispatchTouchEvent(ev)
    }

    override fun dispatchGenericMotionEvent(ev: MotionEvent): Boolean {
        SPenEvents.onMotion(ev)
        return super.dispatchGenericMotionEvent(ev)
    }
}

@Composable
private fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "home",
        enterTransition = {
            fadeIn(tween(260)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                tween(320),
            ) { it / 6 }
        },
        exitTransition = { fadeOut(tween(200)) },
        popEnterTransition = { fadeIn(tween(260)) },
        popExitTransition = {
            fadeOut(tween(220)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                tween(320),
            ) { it / 6 }
        },
    ) {
        composable("home") {
            HomeScreen(
                onOpenNote = { noteId -> navController.navigate("editor/$noteId") },
            )
        }
        composable(
            route = "editor/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.StringType }),
        ) { entry ->
            EditorScreen(
                noteId = entry.arguments?.getString("noteId").orEmpty(),
                onBack = { navController.popBackStack() },
            )
        }
    }
}
