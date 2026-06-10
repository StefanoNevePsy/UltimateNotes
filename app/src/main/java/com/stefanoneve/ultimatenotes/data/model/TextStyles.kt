package com.stefanoneve.ultimatenotes.data.model

import kotlinx.serialization.Serializable

/**
 * A customizable paragraph style ("Titolo 1", "Corpo", ...). The user can edit
 * size/weight/font of each style from the settings sheet; text blocks reference
 * styles by [id] so edits restyle every note.
 */
@Serializable
data class TextStyleDef(
    val id: String,
    val name: String,
    val fontSize: Float,
    /** 100..900, multiples of 100 (Compose FontWeight). */
    val fontWeight: Int = 400,
    val fontId: String? = null,
)

@Serializable
data class StyleSet(
    val styles: List<TextStyleDef> = DEFAULTS,
) {
    fun byId(id: String): TextStyleDef =
        styles.firstOrNull { it.id == id } ?: styles.first()

    companion object {
        val DEFAULTS = listOf(
            TextStyleDef("title1", "Titolo 1", fontSize = 32f, fontWeight = 700),
            TextStyleDef("title2", "Titolo 2", fontSize = 26f, fontWeight = 700),
            TextStyleDef("title3", "Titolo 3", fontSize = 21f, fontWeight = 600),
            TextStyleDef("body", "Corpo", fontSize = 16f, fontWeight = 400),
            TextStyleDef("caption", "Didascalia", fontSize = 13f, fontWeight = 400),
        )
    }
}
