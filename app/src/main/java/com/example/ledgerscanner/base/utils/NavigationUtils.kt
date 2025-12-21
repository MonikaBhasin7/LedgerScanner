package com.example.ledgerscanner.base.utils

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController

/**
 * Navigate back or finish activity if no back stack exists
 *
 * Usage in Composable:
 * ```
 * navController.navigateBackOrFinish(context)
 * ```
 */
fun NavHostController.navigateBackOrFinish(context: Context) {
    if (!this.popBackStack()) {
        context.findActivity()?.finish()
    }
}

fun NavHostController.navigateFromActivity(context: Context) {
    context.findActivity()?.finish()
}

/**
 * Alternative: Navigate back with custom fallback action
 *
 * Usage:
 * ```
 * navController.navigateBackOr {
 *     activity?.finish()
 * }
 * ```
 */
inline fun NavHostController.navigateBackOr(crossinline onNoBackStack: () -> Unit) {
    if (!this.popBackStack()) {
        onNoBackStack()
    }
}

/**
 * Safe navigation back - handles both navigation and activity finish
 *
 * Usage:
 * ```
 * navController.safeNavigateBack(context)
 * ```
 */
fun NavHostController.safeNavigateBack(context: Context): Boolean {
    return if (this.previousBackStackEntry != null) {
        this.popBackStack()
    } else {
        context.findActivity()?.finish()
        true
    }
}

/**
 * Navigate up or finish activity
 *
 * Usage:
 * ```
 * navController.navigateUpOrFinish(context)
 * ```
 */
fun NavHostController.navigateUpOrFinish(context: Context): Boolean {
    return if (this.navigateUp()) {
        true
    } else {
        context.findActivity()?.finish()
        true
    }
}

/**
 * Find the ComponentActivity from Context
 * Handles ContextWrapper chain
 */
fun Context.findActivity(): ComponentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is ComponentActivity) return context
        context = context.baseContext
    }
    return null
}

/**
 * Composable function to handle back navigation
 *
 * Usage in Composable:
 * ```
 * val handleBack = rememberBackHandler(navController)
 *
 * GenericToolbar(
 *     title = "Screen",
 *     onBackClick = handleBack
 * )
 * ```
 */
@Composable
fun rememberBackHandler(navController: NavHostController): () -> Unit {
    val context = LocalContext.current
    return {
        navController.navigateBackOrFinish(context)
    }
}

/**
 * Composable function with custom fallback
 *
 * Usage:
 * ```
 * val handleBack = rememberBackHandler(navController) {
 *     // Custom action when no back stack
 *     Toast.makeText(context, "Exiting...", Toast.LENGTH_SHORT).show()
 *     activity?.finish()
 * }
 * ```
 */
@Composable
fun rememberBackHandler(
    navController: NavHostController,
    onNoBackStack: () -> Unit
): () -> Unit {
    return {
        navController.navigateBackOr(onNoBackStack)
    }
}