package com.stefanoneve.ultimatenotes.ui.components

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.Book
import com.composables.icons.lucide.Brain
import com.composables.icons.lucide.Briefcase
import com.composables.icons.lucide.Dumbbell
import com.composables.icons.lucide.Folder
import com.composables.icons.lucide.GraduationCap
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.House
import com.composables.icons.lucide.Lightbulb
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Paintbrush
import com.composables.icons.lucide.PartyPopper
import com.composables.icons.lucide.PawPrint
import com.composables.icons.lucide.Plane
import com.composables.icons.lucide.ShoppingCart
import com.composables.icons.lucide.Star
import com.composables.icons.lucide.Utensils

/** Icons selectable for folders, keyed by a stable string stored in the DB. */
val FolderIcons: Map<String, ImageVector> = linkedMapOf(
    "folder" to Lucide.Folder,
    "work" to Lucide.Briefcase,
    "school" to Lucide.GraduationCap,
    "home" to Lucide.House,
    "idea" to Lucide.Lightbulb,
    "star" to Lucide.Star,
    "heart" to Lucide.Heart,
    "book" to Lucide.Book,
    "brush" to Lucide.Paintbrush,
    "music" to Lucide.Music,
    "food" to Lucide.Utensils,
    "travel" to Lucide.Plane,
    "fitness" to Lucide.Dumbbell,
    "shopping" to Lucide.ShoppingCart,
    "pets" to Lucide.PawPrint,
    "mind" to Lucide.Brain,
    "party" to Lucide.PartyPopper,
)

fun folderIcon(key: String): ImageVector = FolderIcons[key] ?: Lucide.Folder

/** Palette offered when customizing folders. */
val FolderPalette: List<Long> = listOf(
    0xFF4F46E5, 0xFF7C3AED, 0xFFDB2777, 0xFFE11D48,
    0xFFEA580C, 0xFFD97706, 0xFF65A30D, 0xFF059669,
    0xFF0D9488, 0xFF0284C7, 0xFF475569, 0xFF1E293B,
)
