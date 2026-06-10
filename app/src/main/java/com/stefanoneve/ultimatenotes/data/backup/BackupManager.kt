package com.stefanoneve.ultimatenotes.data.backup

import android.content.Context
import android.net.Uri
import com.stefanoneve.ultimatenotes.data.db.AppDatabase
import com.stefanoneve.ultimatenotes.data.db.FolderEntity
import com.stefanoneve.ultimatenotes.data.db.NoteEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Serializable
private data class BackupPayload(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val folders: List<FolderEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
)

/**
 * Exports/imports the whole app state as a single .zip:
 *  - backup.json    → folders + notes (content included as JSON)
 *  - notes/<id>/... → images and rendered PDF pages
 *  - fonts/...      → imported fonts
 *
 * The zip is written through SAF, so the user can save it straight into
 * Google Drive (or any cloud provider visible in the system file picker).
 */
class BackupManager(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun export(uri: Uri): Result<Int> = runCatching {
        val db = AppDatabase.get(context)
        val payload = BackupPayload(
            folders = db.folderDao().getAll(),
            notes = db.noteDao().getAll(),
        )
        val out = context.contentResolver.openOutputStream(uri, "wt")
            ?: error("Impossibile aprire la destinazione")
        ZipOutputStream(out.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("backup.json"))
            zip.write(json.encodeToString(BackupPayload.serializer(), payload).toByteArray())
            zip.closeEntry()
            zipDir(zip, File(context.filesDir, "notes"), "notes")
            zipDir(zip, File(context.filesDir, "fonts"), "fonts")
        }
        payload.notes.size
    }

    suspend fun import(uri: Uri): Result<Int> = runCatching {
        val input = context.contentResolver.openInputStream(uri)
            ?: error("Impossibile leggere il file")
        var payload: BackupPayload? = null
        ZipInputStream(input.buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                when {
                    entry.name == "backup.json" ->
                        payload = json.decodeFromString(
                            BackupPayload.serializer(),
                            zip.readBytes().decodeToString(),
                        )
                    !entry.isDirectory &&
                        (entry.name.startsWith("notes/") || entry.name.startsWith("fonts/")) -> {
                        val dest = File(context.filesDir, entry.name)
                        // Guard against zip-slip paths escaping filesDir.
                        if (dest.canonicalPath.startsWith(context.filesDir.canonicalPath)) {
                            dest.parentFile?.mkdirs()
                            dest.outputStream().use { zip.copyTo(it) }
                        }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val data = payload ?: error("Backup non valido: manca backup.json")
        val db = AppDatabase.get(context)
        data.folders.forEach { db.folderDao().upsert(it) }
        data.notes.forEach { db.noteDao().upsert(it) }
        data.notes.size
    }

    private fun zipDir(zip: ZipOutputStream, dir: File, prefix: String) {
        if (!dir.exists()) return
        dir.walkTopDown().filter { it.isFile }.forEach { file ->
            val rel = file.relativeTo(dir).invariantSeparatorsPath
            zip.putNextEntry(ZipEntry("$prefix/$rel"))
            file.inputStream().use { it.copyTo(zip) }
            zip.closeEntry()
        }
    }
}
