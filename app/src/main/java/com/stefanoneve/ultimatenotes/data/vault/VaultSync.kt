package com.stefanoneve.ultimatenotes.data.vault

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.stefanoneve.ultimatenotes.data.db.FolderEntity
import com.stefanoneve.ultimatenotes.data.db.NoteEntity
import com.stefanoneve.ultimatenotes.data.model.NoteContent
import com.stefanoneve.ultimatenotes.data.repo.NotesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * One note as it lives in the shared vault folder. Mirrors
 * `docs/VAULT_FORMAT.md`, which the macOS app reads and writes too.
 */
@Serializable
data class VaultNoteFile(
    val id: String,
    val title: String = "",
    val folderId: String? = null,
    val pinned: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    /** Non-null = tombstone: the note was deleted on some device. */
    val deletedAt: Long? = null,
    val content: NoteContent = NoteContent(),
)

@Serializable
data class VaultManifest(
    val formatVersion: Int = 1,
    val folders: List<FolderEntity> = emptyList(),
)

data class SyncStats(
    val pushed: Int = 0,
    val pulled: Int = 0,
    val deletedLocally: Int = 0,
    val assetsCopied: Int = 0,
) {
    fun summary(): String = buildList {
        if (pushed > 0) add("$pushed inviate")
        if (pulled > 0) add("$pulled ricevute")
        if (deletedLocally > 0) add("$deletedLocally eliminate")
        if (assetsCopied > 0) add("$assetsCopied file")
    }.joinToString(", ").ifEmpty { "già allineate" }
}

/**
 * Two-way sync between the local database and a folder the user keeps in
 * sync with Drive / Dropbox / iCloud / Syncthing. There is no server: the
 * merge is per note, last-write-wins on `updatedAt`, exactly as the macOS
 * app does it.
 */
class VaultSync(
    private val context: Context,
    private val repo: NotesRepository,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = true
    }

    /** Grabs long-lived read/write access to a folder the user just picked. */
    fun persistAccess(treeUri: Uri): Boolean = runCatching {
        context.contentResolver.takePersistableUriPermission(
            treeUri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        true
    }.getOrDefault(false)

    /**
     * Records a deletion in the vault so it reaches the other devices.
     * Called right when a note is deleted: if the vault is unreachable the
     * note simply reappears on the next sync (documented trade-off of having
     * no local tombstone table).
     */
    suspend fun writeTombstone(treeUri: Uri, note: NoteEntity) =
        withContext(Dispatchers.IO) {
            runCatching {
                val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@runCatching
                val notesDir = childDir(root, "notes") ?: return@runCatching
                val now = System.currentTimeMillis()
                writeJson(
                    notesDir,
                    "${note.id}.json",
                    json.encodeToString(
                        VaultNoteFile.serializer(),
                        VaultNoteFile(
                            id = note.id,
                            title = note.title,
                            folderId = note.folderId,
                            pinned = false,
                            createdAt = note.createdAt,
                            updatedAt = note.updatedAt,
                            deletedAt = now,
                            content = NoteContent(),
                        ),
                    ),
                )
            }
            Unit
        }

    suspend fun sync(treeUri: Uri): Result<SyncStats> = withContext(Dispatchers.IO) {
        runCatching {
            val root = DocumentFile.fromTreeUri(context, treeUri)
                ?: error("Cartella non accessibile")
            if (!root.canWrite()) error("Nessun permesso di scrittura sulla cartella")
            val notesDir = childDir(root, "notes") ?: error("Impossibile creare notes/")
            val assetsDir = childDir(root, "assets") ?: error("Impossibile creare assets/")

            // ---- Read the vault side ----
            val remote = mutableMapOf<String, VaultNoteFile>()
            val remoteFiles = mutableMapOf<String, DocumentFile>()
            notesDir.listFiles().forEach { file ->
                val name = file.name ?: return@forEach
                if (!name.endsWith(".json")) return@forEach
                val text = readText(file) ?: return@forEach
                // A single unreadable file must not abort the whole sync.
                val parsed = runCatching {
                    json.decodeFromString(VaultNoteFile.serializer(), text)
                }.getOrNull() ?: return@forEach
                remote[parsed.id] = parsed
                remoteFiles[parsed.id] = file
            }

            // ---- Read the local side ----
            val local = repo.allNotes().associateBy { it.id }

            var pushed = 0
            var pulled = 0
            var deletedLocally = 0
            var assets = 0

            // ---- Local -> vault ----
            for ((id, entity) in local) {
                val r = remote[id]
                val remoteWins = r != null &&
                    maxOf(r.updatedAt, r.deletedAt ?: 0L) >= entity.updatedAt
                if (remoteWins) continue
                val content = repo.decodeContentOrNull(entity) ?: continue
                writeJson(
                    notesDir,
                    "$id.json",
                    json.encodeToString(
                        VaultNoteFile.serializer(),
                        VaultNoteFile(
                            id = id,
                            title = entity.title,
                            folderId = entity.folderId,
                            pinned = entity.pinned,
                            createdAt = entity.createdAt,
                            updatedAt = entity.updatedAt,
                            deletedAt = null,
                            content = content,
                        ),
                    ),
                )
                assets += pushAssets(assetsDir, id)
                pushed++
            }

            // ---- Vault -> local ----
            for ((id, r) in remote) {
                val entity = local[id]
                if (r.deletedAt != null) {
                    // Tombstone: only obeyed when nothing newer happened here.
                    if (entity != null && r.deletedAt >= entity.updatedAt) {
                        repo.deleteNote(id)
                        deletedLocally++
                    }
                    continue
                }
                if (entity != null && entity.updatedAt >= r.updatedAt) continue
                repo.upsertFromVault(
                    NoteEntity(
                        id = r.id,
                        folderId = r.folderId,
                        title = r.title,
                        contentJson = json.encodeToString(
                            NoteContent.serializer(), r.content,
                        ),
                        plainText = r.content.plainText(),
                        pinned = r.pinned,
                        createdAt = if (r.createdAt > 0) r.createdAt else r.updatedAt,
                        updatedAt = r.updatedAt,
                    ),
                )
                assets += pullAssets(assetsDir, id)
                pulled++
            }

            // ---- Folders ----
            syncFolders(root)

            SyncStats(pushed, pulled, deletedLocally, assets)
        }
    }

    /** Folders are small: the side with more of them wins, then union by id. */
    private suspend fun syncFolders(root: DocumentFile) {
        val localFolders = repo.allFolders()
        val manifestFile = root.findFile("vault.json")
        val remoteManifest = manifestFile?.let { f ->
            readText(f)?.let {
                runCatching {
                    json.decodeFromString(VaultManifest.serializer(), it)
                }.getOrNull()
            }
        }
        val merged = (localFolders + (remoteManifest?.folders ?: emptyList()))
            .associateBy { it.id }
            .values
            .sortedBy { it.position }
        merged.forEach { if (localFolders.none { l -> l.id == it.id }) repo.upsertFolder(it) }
        writeJson(
            root,
            "vault.json",
            json.encodeToString(VaultManifest.serializer(), VaultManifest(folders = merged)),
        )
    }

    /** Copies this note's local asset files into the vault when missing. */
    private fun pushAssets(assetsDir: DocumentFile, noteId: String): Int {
        val localDir = repo.assetsDir(noteId)
        val files = localDir.listFiles()?.filter { it.isFile } ?: return 0
        if (files.isEmpty()) return 0
        val target = childDir(assetsDir, noteId) ?: return 0
        var copied = 0
        files.forEach { file ->
            if (target.findFile(file.name) != null) return@forEach
            val doc = target.createFile("application/octet-stream", file.name)
                ?: return@forEach
            runCatching {
                context.contentResolver.openOutputStream(doc.uri, "wt")?.use { out ->
                    file.inputStream().use { it.copyTo(out) }
                }
                copied++
            }
        }
        return copied
    }

    /** Copies this note's vault asset files down when missing locally. */
    private fun pullAssets(assetsDir: DocumentFile, noteId: String): Int {
        val source = assetsDir.findFile(noteId)?.takeIf { it.isDirectory } ?: return 0
        val localDir = repo.assetsDir(noteId)
        var copied = 0
        source.listFiles().forEach { doc ->
            val name = doc.name ?: return@forEach
            val dest = java.io.File(localDir, name)
            if (dest.exists()) return@forEach
            runCatching {
                context.contentResolver.openInputStream(doc.uri)?.use { input ->
                    dest.outputStream().use { input.copyTo(it) }
                }
                copied++
            }
        }
        return copied
    }

    // ---- Small DocumentFile helpers ----

    private fun childDir(parent: DocumentFile, name: String): DocumentFile? =
        parent.findFile(name)?.takeIf { it.isDirectory } ?: parent.createDirectory(name)

    private fun readText(file: DocumentFile): String? = runCatching {
        context.contentResolver.openInputStream(file.uri)?.use {
            it.readBytes().decodeToString()
        }
    }.getOrNull()

    private fun writeJson(dir: DocumentFile, name: String, text: String) {
        val existing = dir.findFile(name)
        val doc = existing ?: dir.createFile("application/json", name) ?: return
        runCatching {
            context.contentResolver.openOutputStream(doc.uri, "wt")?.use {
                it.write(text.toByteArray())
            }
        }
    }
}
