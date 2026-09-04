// NoteListView.swift
// UltimateNotesMac
//
// Sidebar: folder filter, search and the note list.

import SwiftUI

struct NoteListView: View {
    @ObservedObject var store: VaultStore
    let theme: AppStyle
    @Binding var selection: String?

    @State private var query: String = ""
    @State private var folderId: String?

    private var visibleNotes: [VaultNote] {
        store.notes.filter { note in
            if let folderId, note.folderId != folderId { return false }
            guard !query.isEmpty else { return true }
            let needle = query.lowercased()
            return note.title.lowercased().contains(needle)
                || note.content.plainText().lowercased().contains(needle)
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            if !store.folders.isEmpty {
                folderPicker
                Divider()
            }
            List(selection: $selection) {
                ForEach(visibleNotes) { note in
                    row(note).tag(note.id)
                }
            }
            .listStyle(.sidebar)
        }
        .searchable(text: $query, placement: .sidebar, prompt: "Cerca nelle note")
        .safeAreaInset(edge: .bottom) { footer }
    }

    private var folderPicker: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                chip(title: "Tutte", active: folderId == nil, color: theme.primary) {
                    folderId = nil
                }
                ForEach(store.folders) { folder in
                    chip(
                        title: folder.name,
                        active: folderId == folder.id,
                        color: Color(argb: folder.color)
                    ) {
                        folderId = folderId == folder.id ? nil : folder.id
                    }
                }
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 8)
        }
    }

    private func chip(
        title: String, active: Bool, color: Color, action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Text(title)
                .font(.caption)
                .padding(.horizontal, 10)
                .padding(.vertical, 5)
                .background(
                    Capsule().fill(active ? color.opacity(0.28) : Color.secondary.opacity(0.12))
                )
        }
        .buttonStyle(.plain)
    }

    private func row(_ note: VaultNote) -> some View {
        VStack(alignment: .leading, spacing: 3) {
            HStack(spacing: 5) {
                if note.pinned {
                    Image(systemName: "pin.fill").font(.caption2).foregroundColor(theme.primary)
                }
                Text(note.title.isEmpty ? "Senza titolo" : note.title)
                    .font(.body)
                    .lineLimit(1)
            }
            let preview = note.content.plainText()
                .replacingOccurrences(of: "\n", with: " ")
            if !preview.isEmpty {
                Text(preview).font(.caption).foregroundColor(.secondary).lineLimit(2)
            }
            Text(Self.dateFormatter.string(from: note.updatedDate))
                .font(.caption2)
                .foregroundColor(.secondary)
        }
        .padding(.vertical, 3)
        .contextMenu {
            Button(note.pinned ? "Sblocca" : "Fissa in alto") { togglePin(note) }
            Button("Duplica") { duplicate(note) }
            Divider()
            Button("Elimina", role: .destructive) { store.delete(id: note.id) }
        }
    }

    private var footer: some View {
        HStack {
            Button {
                let created = store.newNote(folderId: folderId)
                selection = created.id
            } label: {
                Label("Nuova nota", systemImage: "square.and.pencil")
            }
            Spacer()
            Button {
                store.reload()
            } label: {
                Label("Ricarica", systemImage: "arrow.clockwise")
            }
            .help("Rilegge la cartella condivisa")
        }
        .buttonStyle(.borderless)
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(.bar)
    }

    private func togglePin(_ note: VaultNote) {
        var updated = note
        updated.pinned.toggle()
        store.save(updated)
    }

    private func duplicate(_ note: VaultNote) {
        var copy = note
        copy.id = UUID().uuidString.lowercased()
        copy.title = note.title.isEmpty ? "" : note.title + " (copia)"
        copy.pinned = false
        store.save(copy)
        selection = copy.id
    }

    private static let dateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateStyle = .short
        f.timeStyle = .short
        return f
    }()
}

extension VaultNote {
    var updatedDate: Date {
        Date(timeIntervalSince1970: TimeInterval(updatedAt) / 1000)
    }
}
