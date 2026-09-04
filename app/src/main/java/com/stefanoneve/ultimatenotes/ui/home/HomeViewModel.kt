package com.stefanoneve.ultimatenotes.ui.home

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stefanoneve.ultimatenotes.UltimateNotesApp
import com.stefanoneve.ultimatenotes.data.db.FolderEntity
import com.stefanoneve.ultimatenotes.data.db.NoteEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as UltimateNotesApp
    private val repo = app.repository
    val settingsStore = app.settingsStore
    val fontManager = app.fontManager
    private val backupManager = app.backupManager
    private val vaultSync = app.vaultSync

    val folders: StateFlow<List<FolderEntity>> = repo.observeFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val query = MutableStateFlow("")
    val selectedFolderId = MutableStateFlow<String?>(null)

    val notes: StateFlow<List<NoteEntity>> =
        combine(query, selectedFolderId) { q, f -> q to f }
            .flatMapLatest { (q, folderId) ->
                when {
                    q.isNotBlank() -> repo.search(q.trim())
                    folderId != null -> repo.observeNotes(folderId)
                    else -> repo.observeNotes()
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val message = MutableStateFlow<String?>(null)

    fun createNote(onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val note = repo.newNote(selectedFolderId.value)
            onCreated(note.id)
        }
    }

    fun saveFolder(folder: FolderEntity) {
        viewModelScope.launch { repo.upsertFolder(folder) }
    }

    fun deleteFolder(id: String) {
        viewModelScope.launch {
            repo.deleteFolder(id)
            if (selectedFolderId.value == id) selectedFolderId.value = null
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch {
            // Record the deletion in the vault first, so the other devices
            // learn about it instead of pushing the note back.
            val vault = settingsStore.settings.value.vaultUri
            if (vault != null) {
                repo.getNote(id)?.let { vaultSync.writeTombstone(Uri.parse(vault), it) }
            }
            repo.deleteNote(id)
        }
    }

    // ---- Vault sync ----

    val syncing = MutableStateFlow(false)

    /** Picks the shared folder and runs a first full sync. */
    fun setVaultFolder(uri: Uri) {
        if (!vaultSync.persistAccess(uri)) {
            message.value = "Impossibile ottenere l'accesso permanente alla cartella"
            return
        }
        settingsStore.update { it.copy(vaultUri = uri.toString()) }
        syncNow(announceIdle = true)
    }

    fun clearVaultFolder() {
        settingsStore.update { it.copy(vaultUri = null) }
        message.value = "Sincronizzazione disattivata"
    }

    fun syncNow(announceIdle: Boolean = true) {
        val uri = settingsStore.settings.value.vaultUri ?: return
        if (syncing.value) return
        viewModelScope.launch {
            syncing.value = true
            vaultSync.sync(Uri.parse(uri))
                .onSuccess {
                    if (announceIdle || it.pushed + it.pulled + it.deletedLocally > 0) {
                        message.value = "Sincronizzato: ${it.summary()}"
                    }
                }
                .onFailure { message.value = "Sincronizzazione fallita: ${it.message}" }
            syncing.value = false
        }
    }

    /** Silent sync when the note list appears. */
    fun autoSyncIfEnabled() {
        val settings = settingsStore.settings.value
        if (settings.vaultUri != null && settings.vaultAutoSync) syncNow(announceIdle = false)
    }

    fun togglePin(note: NoteEntity) {
        viewModelScope.launch { repo.updateNoteMeta(note.copy(pinned = !note.pinned)) }
    }

    fun moveNote(note: NoteEntity, folderId: String?) {
        viewModelScope.launch { repo.updateNoteMeta(note.copy(folderId = folderId)) }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            backupManager.export(uri)
                .onSuccess { message.value = "Backup completato: $it note esportate" }
                .onFailure { message.value = "Backup fallito: ${it.message}" }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            backupManager.import(uri)
                .onSuccess {
                    // Restored font files must become visible without a restart.
                    fontManager.reload()
                    message.value = "Import completato: $it note ripristinate"
                }
                .onFailure { message.value = "Import fallito: ${it.message}" }
        }
    }

    fun importFont(uri: Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val font = fontManager.importFont(uri)
            message.value =
                if (font != null) "Font \"${font.name}\" importato"
                else "File non valido: scegli un .ttf o .otf"
        }
    }

    fun duplicateNote(id: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val copy = repo.duplicateNote(id)
            message.value =
                if (copy != null) "Nota duplicata" else "Duplicazione fallita"
        }
    }
}
