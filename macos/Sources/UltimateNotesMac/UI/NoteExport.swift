// NoteExport.swift
// UltimateNotesMac
//
// Saves a note as a PNG image or a one-page PDF. Both render the very same
// views the editor shows, so the export always matches the canvas.

import AppKit
import SwiftUI
import UniformTypeIdentifiers

/// World-space bounds of everything in a note, with a margin. `nil` when
/// the note is empty.
func noteBounds(_ content: NoteContent, margin: CGFloat = 60) -> CGRect? {
    var box: CGRect?
    func include(_ rect: CGRect) {
        box = box.map { $0.union(rect) } ?? rect
    }

    for element in content.elements { include(elementRect(element)) }
    for frame in content.frames { include(effectiveFrameRect(frame, content)) }
    for tape in content.tapes {
        let half = CGFloat(tape.thickness) / 2
        include(
            CGRect(
                x: CGFloat(min(tape.x1, tape.x2)) - half,
                y: CGFloat(min(tape.y1, tape.y2)) - half,
                width: CGFloat(abs(tape.x2 - tape.x1)) + half * 2,
                height: CGFloat(abs(tape.y2 - tape.y1)) + half * 2
            )
        )
    }
    for stroke in content.strokes {
        for point in stroke.points {
            include(CGRect(x: CGFloat(point.x), y: CGFloat(point.y), width: 1, height: 1))
        }
    }
    for connector in content.connectors {
        for point in connectorSamples(connector, content) ?? [] {
            include(CGRect(x: point.x, y: point.y, width: 1, height: 1))
        }
    }
    return box?.insetBy(dx: -margin, dy: -margin)
}

/// The whole note laid out at 1:1, ready to be rasterised. Uses a fixed
/// camera that shifts the content's top-left corner to the origin.
struct NoteExportView: View {
    let note: VaultNote
    let theme: AppStyle
    let bounds: CGRect
    var assetURL: (String) -> URL?
    var linkedNoteTitle: (String) -> String?

    /// Fixed camera that puts the content's top-left corner at the origin.
    private var camera: CanvasState {
        CanvasState(offset: CGSize(width: -bounds.minX, height: -bounds.minY), scale: 1)
    }

    var body: some View {
        ZStack(alignment: .topLeading) {
            NoteCanvas(content: note.content, theme: theme, camera: camera)
            ForEach(note.content.elements, id: \.id) { element in
                NoteElementView(
                    element: element,
                    theme: theme,
                    assetURL: assetURL,
                    linkedNoteTitle: linkedNoteTitle
                )
                .offset(
                    x: CGFloat(element.x) - bounds.minX,
                    y: CGFloat(element.y) - bounds.minY
                )
            }
        }
        .frame(width: bounds.width, height: bounds.height)
        .background(theme.background)
    }
}

enum NoteExportError: LocalizedError {
    case emptyNote
    case renderFailed
    case writeFailed(String)

    var errorDescription: String? {
        switch self {
        case .emptyNote: return "La nota è vuota: non c'è nulla da esportare."
        case .renderFailed: return "Impossibile disegnare la nota."
        case .writeFailed(let detail): return "Salvataggio non riuscito: \(detail)"
        }
    }
}

@MainActor
enum NoteExporter {

    /// Renders the note into a PNG, at up to 2× for a crisp result.
    static func exportPNG(
        note: VaultNote, theme: AppStyle, to url: URL,
        assetURL: @escaping (String) -> URL?,
        linkedNoteTitle: @escaping (String) -> String?
    ) throws {
        guard let bounds = noteBounds(note.content) else { throw NoteExportError.emptyNote }
        let view = NoteExportView(
            note: note, theme: theme, bounds: bounds,
            assetURL: assetURL, linkedNoteTitle: linkedNoteTitle
        )
        let renderer = ImageRenderer(content: view)
        // Cap the pixel size so a sprawling canvas can't blow up memory.
        renderer.scale = min(2, 8000 / max(bounds.width, bounds.height))

        guard let image = renderer.cgImage else { throw NoteExportError.renderFailed }
        let rep = NSBitmapImageRep(cgImage: image)
        rep.size = NSSize(width: bounds.width, height: bounds.height)
        guard let data = rep.representation(using: .png, properties: [:]) else {
            throw NoteExportError.renderFailed
        }
        do {
            try data.write(to: url, options: .atomic)
        } catch {
            throw NoteExportError.writeFailed(error.localizedDescription)
        }
    }

    /// Renders the note into a single-page PDF sized to its content.
    static func exportPDF(
        note: VaultNote, theme: AppStyle, to url: URL,
        assetURL: @escaping (String) -> URL?,
        linkedNoteTitle: @escaping (String) -> String?
    ) throws {
        guard let bounds = noteBounds(note.content) else { throw NoteExportError.emptyNote }
        let view = NoteExportView(
            note: note, theme: theme, bounds: bounds,
            assetURL: assetURL, linkedNoteTitle: linkedNoteTitle
        )
        let renderer = ImageRenderer(content: view)

        var thrown: Error?
        renderer.render { size, renderInContext in
            var mediaBox = CGRect(origin: .zero, size: size)
            guard let consumer = CGDataConsumer(url: url as CFURL),
                  let context = CGContext(consumer: consumer, mediaBox: &mediaBox, nil)
            else {
                thrown = NoteExportError.writeFailed("destinazione non scrivibile")
                return
            }
            context.beginPDFPage(nil)
            renderInContext(context)
            context.endPDFPage()
            context.closePDF()
        }
        if let thrown { throw thrown }
    }

    /// Asks where to save, then writes. Returns the error to surface, if any.
    static func runExportPanel(
        note: VaultNote, theme: AppStyle, asPDF: Bool,
        assetURL: @escaping (String) -> URL?,
        linkedNoteTitle: @escaping (String) -> String?
    ) -> String? {
        let panel = NSSavePanel()
        panel.allowedContentTypes = [asPDF ? UTType.pdf : UTType.png]
        let base = note.title.isEmpty ? "Nota" : note.title
        panel.nameFieldStringValue = "\(base).\(asPDF ? "pdf" : "png")"
        panel.prompt = "Esporta"
        guard panel.runModal() == .OK, let url = panel.url else { return nil }
        do {
            if asPDF {
                try exportPDF(
                    note: note, theme: theme, to: url,
                    assetURL: assetURL, linkedNoteTitle: linkedNoteTitle
                )
            } else {
                try exportPNG(
                    note: note, theme: theme, to: url,
                    assetURL: assetURL, linkedNoteTitle: linkedNoteTitle
                )
            }
            return nil
        } catch {
            return error.localizedDescription
        }
    }
}
