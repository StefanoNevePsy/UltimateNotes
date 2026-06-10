package com.stefanoneve.ultimatenotes.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    /** ARGB packed color for the folder chip/card. */
    val color: Long = 0xFF4F46E5,
    /** Key of a Material icon (see FolderIcons). */
    val icon: String = "folder",
    val position: Int = 0,
)

@Serializable
@Entity(
    tableName = "notes",
    indices = [Index("folderId"), Index("updatedAt")],
)
data class NoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val folderId: String? = null,
    val title: String = "",
    /** Serialized [com.stefanoneve.ultimatenotes.data.model.NoteContent]. */
    @ColumnInfo(defaultValue = "") val contentJson: String = "",
    /** Plain text mirror of the content, for search. */
    @ColumnInfo(defaultValue = "") val plainText: String = "",
    val pinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
