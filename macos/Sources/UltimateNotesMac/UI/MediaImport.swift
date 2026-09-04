// MediaImport.swift
// UltimateNotesMac
//
// Brings images and PDFs into a note. Files are copied into the vault's
// per-note asset folder with the same naming the Android app uses, so the
// other device picks them up on the next sync.

import AppKit
import PDFKit
import SwiftUI
import UniformTypeIdentifiers

@MainActor
enum MediaImporter {

    /// Longest side of an imported image, matching the Android downsample.
    private static let maxImageSide: CGFloat = 2048
    /// Width a PDF page occupies on the canvas, as on Android.
    private static let pdfPageWidth: CGFloat = 800

    static let imageTypes: [UTType] = [.png, .jpeg, .gif, .bmp, .tiff, .heic, .webP]

    // MARK: Images

    /// Copies an image into the note and returns the element to place.
    /// Large images are downsampled and re-encoded as PNG.
    static func importImage(
        from source: URL, noteId: String, store: VaultStore, at point: CGPoint
    ) -> ImageElement? {
        guard let image = NSImage(contentsOf: source) else { return nil }
        let pixelSize = imagePixelSize(image)
        guard pixelSize.width > 0, pixelSize.height > 0 else { return nil }

        let fileName = "img_\(UUID().uuidString.lowercased()).png"
        let destination = store.assetURL(noteId: noteId, fileName: fileName)
        guard writePNG(image, size: pixelSize, to: destination) else { return nil }

        let displayWidth = min(500, pixelSize.width)
        return ImageElement(
            x: Float(point.x), y: Float(point.y),
            width: Float(displayWidth),
            height: Float(displayWidth * pixelSize.height / pixelSize.width),
            fileName: fileName
        )
    }

    /// True pixel dimensions, capped to `maxImageSide` on the longest side.
    private static func imagePixelSize(_ image: NSImage) -> CGSize {
        let rep = image.representations.first
        var width = CGFloat(rep?.pixelsWide ?? 0)
        var height = CGFloat(rep?.pixelsHigh ?? 0)
        if width <= 0 || height <= 0 {
            width = image.size.width
            height = image.size.height
        }
        let longest = max(width, height)
        if longest > maxImageSide {
            let factor = maxImageSide / longest
            width *= factor
            height *= factor
        }
        return CGSize(width: width.rounded(), height: height.rounded())
    }

    private static func writePNG(_ image: NSImage, size: CGSize, to url: URL) -> Bool {
        guard let rep = NSBitmapImageRep(
            bitmapDataPlanes: nil,
            pixelsWide: Int(size.width), pixelsHigh: Int(size.height),
            bitsPerSample: 8, samplesPerPixel: 4, hasAlpha: true, isPlanar: false,
            colorSpaceName: .deviceRGB, bytesPerRow: 0, bitsPerPixel: 0
        ) else { return false }

        rep.size = size
        NSGraphicsContext.saveGraphicsState()
        defer { NSGraphicsContext.restoreGraphicsState() }
        guard let context = NSGraphicsContext(bitmapImageRep: rep) else { return false }
        NSGraphicsContext.current = context
        image.draw(
            in: NSRect(origin: .zero, size: size),
            from: .zero, operation: .copy, fraction: 1
        )
        context.flushGraphics()

        guard let data = rep.representation(using: .png, properties: [:]) else { return false }
        return (try? data.write(to: url, options: .atomic)) != nil
    }

    // MARK: PDFs

    /// Renders every page of a PDF into the note as annotatable images,
    /// stacked vertically — the same shape the Android importer produces.
    static func importPDF(
        from source: URL, noteId: String, store: VaultStore, at point: CGPoint
    ) -> [ImageElement] {
        guard let document = PDFDocument(url: source) else { return [] }
        var elements: [ImageElement] = []
        var top = point.y

        for index in 0..<document.pageCount {
            guard let page = document.page(at: index) else { continue }
            let pageRect = page.bounds(for: .mediaBox)
            guard pageRect.width > 0, pageRect.height > 0 else { continue }

            // 2× for readable text, capped like the image import.
            let scale = min(2, maxImageSide / max(pageRect.width, pageRect.height))
            let pixelSize = CGSize(
                width: (pageRect.width * scale).rounded(),
                height: (pageRect.height * scale).rounded()
            )
            let thumbnail = page.thumbnail(of: pixelSize, for: .mediaBox)

            let fileName = "pdf_\(UUID().uuidString.lowercased())_p\(index).png"
            let destination = store.assetURL(noteId: noteId, fileName: fileName)
            guard writePNG(thumbnail, size: pixelSize, to: destination) else { continue }

            let height = pdfPageWidth * pageRect.height / pageRect.width
            elements.append(
                ImageElement(
                    x: Float(point.x), y: Float(top),
                    width: Float(pdfPageWidth), height: Float(height),
                    fileName: fileName,
                    isPdfPage: true,
                    pdfPage: index + 1
                )
            )
            top += height + 24
        }
        return elements
    }

    // MARK: Entry points

    /// Handles a file dropped on the canvas or picked from the panel.
    /// Returns the elements to append, or an error message.
    static func importAny(
        from source: URL, noteId: String, store: VaultStore, at point: CGPoint
    ) -> Result<[NoteElement], String> {
        let type = UTType(filenameExtension: source.pathExtension.lowercased())

        if type == .pdf || source.pathExtension.lowercased() == "pdf" {
            let pages = importPDF(from: source, noteId: noteId, store: store, at: point)
            guard !pages.isEmpty else {
                return .failure("PDF non leggibile: \(source.lastPathComponent)")
            }
            return .success(pages.map { .image($0) })
        }

        if let type, type.conforms(to: .image) || imageTypes.contains(type) {
            guard let element = importImage(
                from: source, noteId: noteId, store: store, at: point
            ) else {
                return .failure("Immagine non leggibile: \(source.lastPathComponent)")
            }
            return .success([.image(element)])
        }

        // Anything else is attached as a plain file card.
        let fileName = "file_\(UUID().uuidString.lowercased())_\(source.lastPathComponent)"
        let destination = store.assetURL(noteId: noteId, fileName: fileName)
        do {
            try Data(contentsOf: source).write(to: destination, options: .atomic)
        } catch {
            return .failure("Allegato non copiato: \(error.localizedDescription)")
        }
        let size = (try? source.resourceValues(forKeys: [.fileSizeKey]).fileSize) ?? 0
        return .success([
            .file(
                FileElement(
                    x: Float(point.x), y: Float(point.y),
                    fileName: fileName,
                    displayName: source.lastPathComponent,
                    mimeType: type?.preferredMIMEType ?? "",
                    sizeBytes: Int64(size)
                )
            ),
        ])
    }

    /// Opens the file picker; returns the chosen files.
    static func runOpenPanel(allowing types: [UTType], message: String) -> [URL] {
        let panel = NSOpenPanel()
        panel.canChooseFiles = true
        panel.canChooseDirectories = false
        panel.allowsMultipleSelection = true
        panel.allowedContentTypes = types
        panel.message = message
        panel.prompt = "Aggiungi"
        return panel.runModal() == .OK ? panel.urls : []
    }
}
