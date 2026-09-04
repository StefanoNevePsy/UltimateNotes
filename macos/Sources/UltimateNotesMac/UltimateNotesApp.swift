// UltimateNotesApp.swift
// UltimateNotesMac
//
// App entry point. Notes live in a folder the user keeps in sync with
// Drive / Dropbox / iCloud / Syncthing; the Android app reads and writes
// the same files (see docs/VAULT_FORMAT.md).

import AppKit
import SwiftUI

@main
struct UltimateNotesApp: App {
    @StateObject private var store = VaultStore()
    @AppStorage("themeId") private var themeId: String = "latte"
    @State private var selection: String?

    private var theme: AppStyle { themeById(themeId) }

    var body: some Scene {
        WindowGroup {
            RootView(store: store, theme: theme, selection: $selection)
                .frame(minWidth: 900, minHeight: 600)
                .onAppear {
                    store.reopenRemembered()
                }
                .onDisappear {
                    store.stopWatching()
                }
        }
        .commands {
            CommandGroup(replacing: .newItem) {
                Button("Nuova nota") {
                    guard store.rootURL != nil else { return }
                    selection = store.newNote(folderId: nil).id
                }
                .keyboardShortcut("n")
                .disabled(store.rootURL == nil)

                Button("Apri cartella vault…") { pickVaultFolder(store: store) }
                    .keyboardShortcut("o")

                Button("Ricarica dalla cartella") { store.reload() }
                    .keyboardShortcut("r")
                    .disabled(store.rootURL == nil)
            }
            CommandMenu("Tema") {
                ForEach(appThemes) { style in
                    Button {
                        themeId = style.id
                    } label: {
                        if style.id == themeId {
                            Label(style.name, systemImage: "checkmark")
                        } else {
                            Text(style.name)
                        }
                    }
                }
            }
        }
    }
}

/// Opens the folder picker and hands the choice to the store.
@MainActor
func pickVaultFolder(store: VaultStore) {
    let panel = NSOpenPanel()
    panel.canChooseDirectories = true
    panel.canChooseFiles = false
    panel.allowsMultipleSelection = false
    panel.prompt = "Usa questa cartella"
    panel.message = "Scegli la cartella condivisa con il tablet"
    if panel.runModal() == .OK, let url = panel.url {
        store.open(root: url)
    }
}

struct RootView: View {
    @ObservedObject var store: VaultStore
    let theme: AppStyle
    @Binding var selection: String?

    var body: some View {
        Group {
            if store.rootURL == nil {
                OnboardingView(store: store, theme: theme)
            } else {
                NavigationSplitView {
                    NoteListView(store: store, theme: theme, selection: $selection)
                        .navigationSplitViewColumnWidth(min: 240, ideal: 290)
                } detail: {
                    if let selection, store.note(id: selection) != nil {
                        EditorView(noteId: selection, store: store, theme: theme)
                    } else {
                        ContentUnavailableFallback(
                            title: "Nessuna nota selezionata",
                            message: "Scegline una dall'elenco, o creane una nuova con ⌘N."
                        )
                    }
                }
            }
        }
        .overlay(alignment: .bottom) {
            if let error = store.lastError {
                Text(error)
                    .font(.callout)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 9)
                    .background(Capsule().fill(.regularMaterial))
                    .padding(.bottom, 16)
            }
        }
    }
}

struct OnboardingView: View {
    @ObservedObject var store: VaultStore
    let theme: AppStyle

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "folder.badge.gearshape")
                .font(.system(size: 46))
                .foregroundColor(theme.primary)
            Text("Scegli la cartella delle note")
                .font(.title2)
            Text(
                "Indica una cartella dentro iCloud Drive, Dropbox, Google Drive o "
                + "Syncthing. Il tablet punterà alla stessa cartella e le note "
                + "viaggeranno da sole, senza server né account."
            )
            .font(.callout)
            .foregroundColor(.secondary)
            .multilineTextAlignment(.center)
            .frame(maxWidth: 440)

            Button("Scegli cartella…") { pickVaultFolder(store: store) }
                .keyboardShortcut(.defaultAction)

            if let error = store.lastError {
                Text(error).font(.caption).foregroundColor(.red)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(40)
    }
}
