package com.stefanoneve.ultimatenotes

import android.app.Application
import com.stefanoneve.ultimatenotes.data.backup.BackupManager
import com.stefanoneve.ultimatenotes.data.fonts.FontManager
import com.stefanoneve.ultimatenotes.data.repo.NotesRepository
import com.stefanoneve.ultimatenotes.data.repo.SettingsStore

class UltimateNotesApp : Application() {

    lateinit var repository: NotesRepository
        private set
    lateinit var fontManager: FontManager
        private set
    lateinit var settingsStore: SettingsStore
        private set
    lateinit var backupManager: BackupManager
        private set
    lateinit var vaultSync: com.stefanoneve.ultimatenotes.data.vault.VaultSync
        private set

    override fun onCreate() {
        super.onCreate()
        repository = NotesRepository(this)
        fontManager = FontManager(this)
        settingsStore = SettingsStore(this)
        backupManager = BackupManager(this)
        vaultSync = com.stefanoneve.ultimatenotes.data.vault.VaultSync(this, repository)
    }
}
