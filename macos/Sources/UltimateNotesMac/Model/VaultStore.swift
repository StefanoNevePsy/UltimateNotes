// VaultStore.swift
// UltimateNotesMac
//
// Reads and writes the shared "vault" folder format described in
// docs/VAULT_FORMAT.md:
//
//   <VaultRoot>/vault.json                metadata + folders
//   <VaultRoot>/notes/<noteId>.json       one note per file
//   <VaultRoot>/assets/<noteId>/<file>    that note's attachments
//
// so the same folder can be synced (Drive/Dropbox/iCloud/Syncthing) between
// this app and the Android app with no server involved.

import Combine
import Foundation
#if canImport(Darwin)
import Darwin
#endif

// MARK: - Folder / manifest

struct VaultFolder: Codable, Identifiable, Hashable {
    var id: String
    var name: String
    var color: Int64 = 0xFF4F46E5
    var icon: String = "folder"
    var position: Int = 0

    init(id: String, name: String, color: Int64 = 0xFF4F46E5, icon: String = "folder", position: Int = 0) {
        self.id = id
        self.name = name
        self.color = color
        self.icon = icon
        self.position = position
    }

    private enum CodingKeys: String, CodingKey { case id, name, color, icon, position }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(String.self, forKey: .id)
        name = try c.decodeIfPresent(String.self, forKey: .name) ?? ""
        color = try c.decodeIfPresent(Int64.self, forKey: .color) ?? 0xFF4F46E5
        icon = try c.decodeIfPresent(String.self, forKey: .icon) ?? "folder"
        position = try c.decodeIfPresent(Int.self, forKey: .position) ?? 0
    }

    func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(id, forKey: .id)
        try c.encode(name, forKey: .name)
        try c.encode(color, forKey: .color)
        try c.encode(icon, forKey: .icon)
        try c.encode(position, forKey: .position)
    }
}

struct VaultManifest: Codable {
    var formatVersion: Int = 1
    var folders: [VaultFolder] = []

    init(formatVersion: Int = 1, folders: [VaultFolder] = []) {
        self.formatVersion = formatVersion
        self.folders = folders
    }

    private enum CodingKeys: String, CodingKey { case formatVersion, folders }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        formatVersion = try c.decodeIfPresent(Int.self, forKey: .formatVersion) ?? 1
        folders = try c.decodeIfPresent([VaultFolder].self, forKey: .folders) ?? []
    }

    func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(formatVersion, forKey: .formatVersion)
        try c.encode(folders, forKey: .folders)
    }
}

// MARK: - Note

struct VaultNote: Codable, Identifiable, Hashable {
    var id: String
    var title: String = ""
    var folderId: String? = nil
    var pinned: Bool = false
    /// Milliseconds since epoch, like Android's `System.currentTimeMillis()`.
    var createdAt: Int64
    var updatedAt: Int64
    /// Non-nil = tombstone: the note is deleted but the file stays so the
    /// deletion propagates to other devices on next sync.
    var deletedAt: Int64? = nil
    var content: NoteContent = NoteContent()

    init(
        id: String, title: String = "", folderId: String? = nil, pinned: Bool = false, createdAt: Int64,
        updatedAt: Int64, deletedAt: Int64? = nil, content: NoteContent = NoteContent()
    ) {
        self.id = id
        self.title = title
        self.folderId = folderId
        self.pinned = pinned
        self.createdAt = createdAt
        self.updatedAt = updatedAt
        self.deletedAt = deletedAt
        self.content = content
    }

    private enum CodingKeys: String, CodingKey {
        case id, title, folderId, pinned, createdAt, updatedAt, deletedAt, content
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(String.self, forKey: .id)
        title = try c.decodeIfPresent(String.self, forKey: .title) ?? ""
        folderId = try c.decodeIfPresent(String.self, forKey: .folderId) ?? nil
        pinned = try c.decodeIfPresent(Bool.self, forKey: .pinned) ?? false
        // createdAt/updatedAt have no Kotlin default: a note missing either
        // is malformed, so decoding fails for it (the file is skipped, see
        // VaultStore.loadNotes) rather than fabricating a timestamp.
        createdAt = try c.decode(Int64.self, forKey: .createdAt)
        updatedAt = try c.decode(Int64.self, forKey: .updatedAt)
        deletedAt = try c.decodeIfPresent(Int64.self, forKey: .deletedAt) ?? nil
        content = try c.decodeIfPresent(NoteContent.self, forKey: .content) ?? NoteContent()
    }

    func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(id, forKey: .id)
        try c.encode(title, forKey: .title)
        try c.encode(folderId, forKey: .folderId)
        try c.encode(pinned, forKey: .pinned)
        try c.encode(createdAt, forKey: .createdAt)
        try c.encode(updatedAt, forKey: .updatedAt)
        try c.encode(deletedAt, forKey: .deletedAt)
        try c.encode(content, forKey: .content)
    }
}

// MARK: - VaultStore

/// Owns the currently-open vault folder: loads/saves `vault.json` and
/// `notes/*.json`, keeps `notes`/`folders` published for SwiftUI, and
/// watches the notes directory so external edits (sync from another device)
/// show up without a manual reload.
///
/// No public method throws: failures are recorded in `lastError` instead, so
/// a single unreadable note or a revoked folder permission never crashes
/// the app — it just gets skipped or surfaced as a message.
@MainActor
final class VaultStore: ObservableObject {
    /// Live notes only (tombstones filtered out), pinned first, then most
    /// recently updated first.
    @Published private(set) var notes: [VaultNote] = []
    @Published private(set) var folders: [VaultFolder] = []
    @Published private(set) var rootURL: URL? = nil
    @Published private(set) var lastError: String? = nil

    private let fileManager = FileManager.default

    private let encoder: JSONEncoder = {
        let e = JSONEncoder()
        e.outputFormatting = [.sortedKeys]
        return e
    }()
    private let decoder = JSONDecoder()

    private var dirSource: DispatchSourceFileSystemObject?
    private var debounceWorkItem: DispatchWorkItem?
    private static let debounceInterval: TimeInterval = 0.4

    private static let bookmarkDefaultsKey = "com.stefanoneve.ultimatenotes.mac.vaultBookmark"

    init() {}

    // MARK: Opening

    /// Opens `root` as the active vault: remembers it (as a security-scoped
    /// bookmark) for `reopenRemembered()` on next launch, ensures the
    /// `notes/`/`assets/` subdirectories exist, then loads its content.
    func open(root: URL) {
        lastError = nil
        stopWatching()
        rootURL = root
        saveBookmark(for: root)
        ensureVaultDirectories(at: root)
        reload()
        startWatching()
    }

    /// Restores the vault the user picked last time, from its saved
    /// security-scoped bookmark. No-op (silently) if nothing was saved yet.
    func reopenRemembered() {
        guard let data = UserDefaults.standard.data(forKey: Self.bookmarkDefaultsKey) else { return }
        var isStale = false
        do {
            let url = try URL(
                resolvingBookmarkData: data, options: [.withSecurityScope],
                relativeTo: nil, bookmarkDataIsStale: &isStale
            )
            // Best-effort: a non-sandboxed build doesn't need scoped access,
            // and a sandboxed one that fails here will simply hit permission
            // errors on individual file operations, surfaced via lastError.
            _ = url.startAccessingSecurityScopedResource()
            rootURL = url
            if isStale { saveBookmark(for: url) }
            ensureVaultDirectories(at: url)
            reload()
            startWatching()
        } catch {
            lastError = "Impossibile riaprire l'ultimo vault: \(error.localizedDescription)"
        }
    }

    private func saveBookmark(for url: URL) {
        _ = url.startAccessingSecurityScopedResource()
        do {
            let data = try url.bookmarkData(
                options: [.withSecurityScope], includingResourceValuesForKeys: nil, relativeTo: nil
            )
            UserDefaults.standard.set(data, forKey: Self.bookmarkDefaultsKey)
        } catch {
            lastError = "Impossibile salvare l'accesso alla cartella: \(error.localizedDescription)"
        }
    }

    private func ensureVaultDirectories(at root: URL) {
        for sub in ["notes", "assets"] {
            do {
                try ensureDirectoryExists(root.appendingPathComponent(sub, isDirectory: true))
            } catch {
                lastError = "Impossibile creare \(sub)/: \(error.localizedDescription)"
            }
        }
    }

    private func ensureDirectoryExists(_ url: URL) throws {
        if !fileManager.fileExists(atPath: url.path) {
            try fileManager.createDirectory(at: url, withIntermediateDirectories: true)
        }
    }

    // MARK: Paths

    private var notesDirURL: URL? { rootURL?.appendingPathComponent("notes", isDirectory: true) }
    private var assetsDirURL: URL? { rootURL?.appendingPathComponent("assets", isDirectory: true) }

    /// URL for asset `fileName` belonging to note `noteId`, creating its
    /// per-note asset directory if needed.
    func assetURL(noteId: String, fileName: String) -> URL {
        let assetsRoot = assetsDirURL ?? URL(fileURLWithPath: NSTemporaryDirectory())
        let dir = assetsRoot.appendingPathComponent(noteId, isDirectory: true)
        try? ensureDirectoryExists(dir)
        return dir.appendingPathComponent(fileName)
    }

    // MARK: Reload

    /// Reloads `vault.json` and every `notes/*.json` from disk. Corrupt or
    /// unreadable note files are skipped rather than failing the whole load.
    func reload() {
        guard let root = rootURL else { return }
        loadManifest(at: root)
        loadNotes()
    }

    private func loadManifest(at root: URL) {
        let url = root.appendingPathComponent("vault.json")
        guard fileManager.fileExists(atPath: url.path) else {
            folders = []
            return
        }
        do {
            let data = try Data(contentsOf: url)
            let manifest = try decoder.decode(VaultManifest.self, from: data)
            folders = manifest.folders.sorted { $0.position < $1.position }
        } catch {
            lastError = "vault.json non leggibile: \(error.localizedDescription)"
            folders = []
        }
    }

    private func loadNotes() {
        guard let notesDir = notesDirURL,
              let entries = try? fileManager.contentsOfDirectory(at: notesDir, includingPropertiesForKeys: nil)
        else {
            notes = []
            return
        }
        var loaded: [VaultNote] = []
        for url in entries where url.pathExtension.lowercased() == "json" {
            guard let data = try? Data(contentsOf: url),
                  let note = try? decoder.decode(VaultNote.self, from: data)
            else { continue }
            if note.deletedAt == nil {
                loaded.append(note)
            }
        }
        notes = Self.sortedForDisplay(loaded)
    }

    private static func sortedForDisplay(_ notes: [VaultNote]) -> [VaultNote] {
        notes.sorted { a, b in
            if a.pinned != b.pinned { return a.pinned && !b.pinned }
            return a.updatedAt > b.updatedAt
        }
    }

    // MARK: CRUD

    func note(id: String) -> VaultNote? {
        notes.first { $0.id == id }
    }

    /// Creates, saves, and returns a new blank note in `folderId`.
    @discardableResult
    func newNote(folderId: String?) -> VaultNote {
        let now = Self.nowMillis()
        let note = VaultNote(
            id: UUID().uuidString.lowercased(), title: "", folderId: folderId, pinned: false,
            createdAt: now, updatedAt: now, deletedAt: nil, content: NoteContent()
        )
        save(note)
        return note
    }

    /// Stamps `updatedAt` and writes `notes/<id>.json` atomically.
    func save(_ note: VaultNote) {
        guard let notesDir = notesDirURL else {
            lastError = "Nessun vault aperto"
            return
        }
        var toSave = note
        toSave.updatedAt = Self.nowMillis()
        do {
            try ensureDirectoryExists(notesDir)
            let data = try encoder.encode(toSave)
            try data.write(to: notesDir.appendingPathComponent("\(toSave.id).json"), options: .atomic)
            upsertLocal(toSave)
        } catch {
            lastError = "Impossibile salvare la nota: \(error.localizedDescription)"
        }
    }

    /// Writes a tombstone for `id` (kept on disk with `deletedAt` set and
    /// content emptied) so the deletion propagates on next sync.
    func delete(id: String) {
        guard let notesDir = notesDirURL else {
            lastError = "Nessun vault aperto"
            return
        }
        let now = Self.nowMillis()
        let url = notesDir.appendingPathComponent("\(id).json")
        var tombstone: VaultNote
        if let data = try? Data(contentsOf: url), let existing = try? decoder.decode(VaultNote.self, from: data) {
            tombstone = existing
        } else if let existing = notes.first(where: { $0.id == id }) {
            tombstone = existing
        } else {
            tombstone = VaultNote(id: id, createdAt: now, updatedAt: now)
        }
        tombstone.deletedAt = now
        tombstone.updatedAt = now
        tombstone.content = NoteContent()
        do {
            let data = try encoder.encode(tombstone)
            try data.write(to: url, options: .atomic)
        } catch {
            lastError = "Impossibile eliminare la nota: \(error.localizedDescription)"
        }
        notes.removeAll { $0.id == id }
    }

    private func upsertLocal(_ note: VaultNote) {
        var updated = notes.filter { $0.id != note.id }
        if note.deletedAt == nil {
            updated.append(note)
        }
        notes = Self.sortedForDisplay(updated)
    }

    /// Replaces `vault.json`'s folder list.
    func saveFolders(_ folders: [VaultFolder]) {
        guard let root = rootURL else {
            lastError = "Nessun vault aperto"
            return
        }
        let manifest = VaultManifest(formatVersion: 1, folders: folders)
        do {
            let data = try encoder.encode(manifest)
            try data.write(to: root.appendingPathComponent("vault.json"), options: .atomic)
            self.folders = folders.sorted { $0.position < $1.position }
        } catch {
            lastError = "Impossibile salvare le cartelle: \(error.localizedDescription)"
        }
    }

    // MARK: Watching

    /// Watches `notes/` for external changes (e.g. a sync client writing new
    /// files) and debounces a `reload()` ~400ms after the last event.
    func startWatching() {
        stopWatching()
        guard let notesDir = notesDirURL else { return }
        let fd = Darwin.open(notesDir.path, O_EVTONLY)
        guard fd >= 0 else { return }
        let source = DispatchSource.makeFileSystemObjectSource(
            fileDescriptor: fd, eventMask: [.write, .extend, .rename, .delete, .link], queue: .main
        )
        source.setEventHandler { [weak self] in
            Task { @MainActor in self?.scheduleDebouncedReload() }
        }
        source.setCancelHandler {
            Darwin.close(fd)
        }
        source.resume()
        dirSource = source
    }

    func stopWatching() {
        dirSource?.cancel()
        dirSource = nil
        debounceWorkItem?.cancel()
        debounceWorkItem = nil
    }

    private func scheduleDebouncedReload() {
        debounceWorkItem?.cancel()
        let work = DispatchWorkItem { [weak self] in
            Task { @MainActor in self?.reload() }
        }
        debounceWorkItem = work
        DispatchQueue.main.asyncAfter(deadline: .now() + Self.debounceInterval, execute: work)
    }

    // MARK: Time

    private static func nowMillis() -> Int64 {
        Int64(Date().timeIntervalSince1970 * 1000)
    }
}
