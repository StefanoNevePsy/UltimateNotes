// NoteElementView.swift
// UltimateNotesMac
//
// How a single canvas element is drawn. Shared by the editor and the
// exporters so what you save is what you saw.

import AppKit
import SwiftUI

struct NoteElementView: View {
    let element: NoteElement
    let theme: AppStyle
    /// Resolves image/file assets; nil while the note has no vault yet.
    var assetURL: (String) -> URL?
    /// Title of a linked note, when it can be resolved.
    var linkedNoteTitle: (String) -> String?

    var body: some View {
        switch element {
        case .text(let e):
            MarkdownBlockView(element: e, theme: theme)
        case .image(let e):
            imageView(e)
        case .noteLink(let e):
            card(
                icon: "link",
                title: linkedNoteTitle(e.targetNoteId) ?? "Nota collegata",
                subtitle: nil,
                width: CGFloat(e.width)
            )
        case .webLink(let e):
            card(
                icon: "globe",
                title: e.title.isEmpty ? e.url : e.title,
                subtitle: e.url,
                width: CGFloat(e.width)
            )
        case .file(let e):
            card(
                icon: "paperclip",
                title: e.displayName.isEmpty ? e.fileName : e.displayName,
                subtitle: e.mimeType,
                width: CGFloat(e.width)
            )
        }
    }

    @ViewBuilder
    private func imageView(_ element: ImageElement) -> some View {
        let image = assetURL(element.fileName).flatMap { NSImage(contentsOf: $0) }
        if let image {
            Image(nsImage: image)
                .resizable()
                .scaledToFit()
                .frame(width: CGFloat(element.width), height: CGFloat(element.height))
        } else {
            // The note arrived before its assets did — say so instead of
            // leaving a blank hole on the canvas.
            RoundedRectangle(cornerRadius: 8)
                .fill(theme.surface)
                .overlay(
                    VStack(spacing: 4) {
                        Image(systemName: "photo")
                        Text("immagine non ancora sincronizzata").font(.caption)
                    }
                    .foregroundColor(theme.onSurfaceVariant)
                )
                .frame(width: CGFloat(element.width), height: CGFloat(element.height))
        }
    }

    private func card(
        icon: String, title: String, subtitle: String?, width: CGFloat
    ) -> some View {
        HStack(spacing: 10) {
            Image(systemName: icon).foregroundColor(theme.primary)
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.body).lineLimit(2)
                if let subtitle, !subtitle.isEmpty {
                    Text(subtitle)
                        .font(.caption)
                        .foregroundColor(theme.onSurfaceVariant)
                        .lineLimit(1)
                }
            }
            Spacer(minLength: 0)
        }
        .padding(12)
        .frame(width: width, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 12).fill(theme.surface))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(theme.outlineVariant, lineWidth: 1))
    }
}
