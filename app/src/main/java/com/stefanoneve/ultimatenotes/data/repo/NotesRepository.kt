package com.stefanoneve.ultimatenotes.data.repo

import android.content.Context
import com.stefanoneve.ultimatenotes.data.db.AppDatabase
import com.stefanoneve.ultimatenotes.data.db.FolderEntity
import com.stefanoneve.ultimatenotes.data.db.NoteEntity
import com.stefanoneve.ultimatenotes.data.model.NoteContent
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import java.io.File

class NotesRepository(private val context: Context) {

    private val db = AppDatabase.get(context)
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun observeFolders(): Flow<List<FolderEntity>> = db.folderDao().observeAll()
    fun observeNotes(): Flow<List<NoteEntity>> = db.noteDao().observeAll()
    fun observeNotes(folderId: String): Flow<List<NoteEntity>> =
        db.noteDao().observeByFolder(folderId)

    fun search(query: String): Flow<List<NoteEntity>> =
        // LIKE special characters in the user query must not act as wildcards.
        db.noteDao().search(
            query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_"),
        )

    suspend fun upsertFolder(folder: FolderEntity) = db.folderDao().upsert(folder)

    suspend fun deleteFolder(id: String) {
        db.noteDao().detachFolder(id)
        db.folderDao().delete(id)
    }

    suspend fun getNote(id: String): NoteEntity? = db.noteDao().getById(id)

    /** Every note/folder, for backup and vault sync. */
    suspend fun allNotes(): List<NoteEntity> = db.noteDao().getAll()
    suspend fun allFolders(): List<FolderEntity> = db.folderDao().getAll()

    /** Writes a note straight from the vault, keeping its timestamps as-is. */
    suspend fun upsertFromVault(note: NoteEntity) = db.noteDao().upsert(note)

    suspend fun newNote(folderId: String?): NoteEntity {
        val note = NoteEntity(folderId = folderId)
        db.noteDao().upsert(note)
        return note
    }

    suspend fun saveNote(note: NoteEntity, content: NoteContent) {
        db.noteDao().upsert(
            note.copy(
                contentJson = json.encodeToString(NoteContent.serializer(), content),
                plainText = content.plainText(),
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun updateNoteMeta(note: NoteEntity) = db.noteDao().update(note)

    suspend fun deleteNote(id: String) {
        db.noteDao().delete(id)
        assetsDir(id).deleteRecursively()
    }

    /**
     * Null when the stored JSON exists but can't be decoded: callers must NOT
     * save over it in that case, or the original note would be destroyed.
     */
    fun decodeContentOrNull(note: NoteEntity): NoteContent? =
        if (note.contentJson.isBlank()) NoteContent()
        else runCatching {
            json.decodeFromString(NoteContent.serializer(), note.contentJson)
        }.getOrNull()

    fun decodeContent(note: NoteEntity): NoteContent =
        decodeContentOrNull(note) ?: NoteContent()

    /** Deep copy of a note (content + asset files) under a new id. */
    suspend fun duplicateNote(id: String): NoteEntity? {
        val original = db.noteDao().getById(id) ?: return null
        val copy = original.copy(
            id = java.util.UUID.randomUUID().toString(),
            title = if (original.title.isBlank()) "" else original.title + " (copia)",
            pinned = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        val srcAssets = assetsDir(original.id)
        if (srcAssets.exists()) srcAssets.copyRecursively(assetsDir(copy.id), overwrite = true)
        db.noteDao().upsert(copy)
        return copy
    }

    /** Directory holding images / rendered PDF pages of a note. */
    fun assetsDir(noteId: String): File =
        File(File(context.filesDir, "notes"), noteId).apply { mkdirs() }

    fun assetFile(noteId: String, fileName: String): File =
        File(assetsDir(noteId), fileName)
}
