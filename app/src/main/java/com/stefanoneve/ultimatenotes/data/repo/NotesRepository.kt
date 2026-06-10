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
    fun search(query: String): Flow<List<NoteEntity>> = db.noteDao().search(query)

    suspend fun upsertFolder(folder: FolderEntity) = db.folderDao().upsert(folder)

    suspend fun deleteFolder(id: String) {
        db.noteDao().detachFolder(id)
        db.folderDao().delete(id)
    }

    suspend fun getNote(id: String): NoteEntity? = db.noteDao().getById(id)

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

    fun decodeContent(note: NoteEntity): NoteContent =
        if (note.contentJson.isBlank()) NoteContent()
        else runCatching {
            json.decodeFromString(NoteContent.serializer(), note.contentJson)
        }.getOrDefault(NoteContent())

    /** Directory holding images / rendered PDF pages of a note. */
    fun assetsDir(noteId: String): File =
        File(File(context.filesDir, "notes"), noteId).apply { mkdirs() }

    fun assetFile(noteId: String, fileName: String): File =
        File(assetsDir(noteId), fileName)
}
