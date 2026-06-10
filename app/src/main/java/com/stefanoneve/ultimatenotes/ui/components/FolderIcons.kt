package com.stefanoneve.ultimatenotes.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Work
import androidx.compose.ui.graphics.vector.ImageVector

/** Icons selectable for folders, keyed by a stable string stored in the DB. */
val FolderIcons: Map<String, ImageVector> = linkedMapOf(
    "folder" to Icons.Outlined.Folder,
    "work" to Icons.Outlined.Work,
    "school" to Icons.Outlined.School,
    "home" to Icons.Outlined.Home,
    "idea" to Icons.Outlined.Lightbulb,
    "star" to Icons.Outlined.Star,
    "heart" to Icons.Outlined.FavoriteBorder,
    "book" to Icons.Outlined.Book,
    "brush" to Icons.Outlined.Brush,
    "music" to Icons.Outlined.MusicNote,
    "food" to Icons.Outlined.Restaurant,
    "travel" to Icons.Outlined.Flight,
    "fitness" to Icons.Outlined.FitnessCenter,
    "shopping" to Icons.Outlined.ShoppingCart,
    "pets" to Icons.Outlined.Pets,
    "mind" to Icons.Outlined.Psychology,
    "party" to Icons.Outlined.Celebration,
)

fun folderIcon(key: String): ImageVector = FolderIcons[key] ?: Icons.Outlined.Folder

/** Palette offered when customizing folders. */
val FolderPalette: List<Long> = listOf(
    0xFF4F46E5, 0xFF7C3AED, 0xFFDB2777, 0xFFE11D48,
    0xFFEA580C, 0xFFD97706, 0xFF65A30D, 0xFF059669,
    0xFF0D9488, 0xFF0284C7, 0xFF475569, 0xFF1E293B,
)
