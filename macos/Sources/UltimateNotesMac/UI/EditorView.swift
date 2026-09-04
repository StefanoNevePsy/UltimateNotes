// EditorView.swift
// UltimateNotesMac
//
// The note editor: title, infinite canvas with the element views layered
// over the drawn canvas, tool palette and debounced saving.

import SwiftUI

enum EditorTool: String, CaseIterable, Identifiable {
    case select, pen, highlighter, eraser, text

    var id: String { rawValue }

    var label: String {
        switch self {
        case .select: return "Seleziona"
        case .pen: return "Penna"
        case .highlighter: return "Evidenziatore"
        case .eraser: return "Gomma"
        case .text: return "Testo"
        }
    }

    var symbol: String {
        switch self {
        case .select: return "hand.point.up.left"
        case .pen: return "pencil.tip"
        case .highlighter: return "highlighter"
        case .eraser: return "eraser"
        case .text: return "textformat"
        }
    }
}

struct EditorView: View {
    let noteId: String
    @ObservedObject var store: VaultStore
    let theme: AppStyle

    @State private var note: VaultNote?
    @State private var tool: EditorTool = .select
    @State private var penWidth: Double = 4
    @State private var selectedElementId: String?
    @State private var selectedStrokeId: String?
    @State private var activeStroke: InkStroke?
    @State private var saveTask: Task<Void, Never>?
    @StateObject private var camera = CanvasState()

    var body: some View {
        Group {
            if let note {
                content(for: note)
            } else {
                ContentUnavailableFallback(
                    title: "Nota non trovata",
                    message: "Potrebbe essere stata eliminata da un altro dispositivo."
                )
            }
        }
        .onAppear(perform: loadNote)
        .onChange(of: noteId) { _ in loadNote() }
    }

    private func loadNote() {
        note = store.note(id: noteId)
        selectedElementId = nil
        selectedStrokeId = nil
    }

    @ViewBuilder
    private func content(for note: VaultNote) -> some View {
        VStack(spacing: 0) {
            titleBar(note)
            Divider()
            canvas(note)
            Divider()
            toolBar
        }
        .background(theme.background)
    }

    // MARK: Title

    private func titleBar(_ note: VaultNote) -> some View {
        HStack {
            TextField("Titolo nota", text: Binding(
                get: { self.note?.title ?? "" },
                set: { newValue in
                    self.note?.title = newValue
                    scheduleSave()
                }
            ))
            .textFieldStyle(.plain)
            .font(.title2)
            Spacer()
            if selectedStrokeId != nil {
                Button(role: .destructive) {
                    deleteSelectedStroke()
                } label: {
                    Label("Elimina tratto", systemImage: "trash")
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
    }

    // MARK: Canvas

    private func canvas(_ note: VaultNote) -> some View {
        GeometryReader { _ in
            ZStack(alignment: .topLeading) {
                NoteCanvas(
                    content: note.content,
                    theme: theme,
                    camera: camera,
                    activeStroke: activeStroke,
                    selectedStrokeId: selectedStrokeId
                )
                elementLayer(note)
            }
            .contentShape(Rectangle())
            .gesture(canvasDrag)
            .gesture(MagnificationGesture().onChanged { value in
                camera.zoom(by: value / max(lastMagnification, 0.01), around: .zero)
                lastMagnification = value
            }.onEnded { _ in lastMagnification = 1 })
        }
        .clipped()
    }

    @State private var lastMagnification: CGFloat = 1
    @State private var dragOrigin: CGSize?

    /// Element views sit above the drawn canvas so text stays selectable and
    /// crisp; ink is redrawn on top of them inside `NoteCanvas`.
    @ViewBuilder
    private func elementLayer(_ note: VaultNote) -> some View {
        ForEach(note.content.elements, id: \.id) { element in
            elementView(element)
                .scaleEffect(camera.scale, anchor: .topLeading)
                .offset(
                    x: CGFloat(element.x) * camera.scale + camera.offset.width,
                    y: CGFloat(element.y) * camera.scale + camera.offset.height
                )
                .allowsHitTesting(tool == .select || tool == .text)
                .onTapGesture { selectedElementId = element.id; selectedStrokeId = nil }
                .overlay(alignment: .topLeading) {
                    if element.id == selectedElementId {
                        RoundedRectangle(cornerRadius: 6)
                            .stroke(theme.primary, lineWidth: 2 / camera.scale)
                    }
                }
        }
    }

    @ViewBuilder
    private func elementView(_ element: NoteElement) -> some View {
        switch element {
        case .text(let e):
            MarkdownBlockView(element: e, theme: theme)
        case .image(let e):
            imageView(e)
        case .noteLink(let e):
            cardView(
                icon: "link", title: store.note(id: e.targetNoteId)?.title ?? "Nota collegata",
                subtitle: nil, width: CGFloat(e.width)
            )
        case .webLink(let e):
            cardView(
                icon: "globe", title: e.title.isEmpty ? e.url : e.title,
                subtitle: e.url, width: CGFloat(e.width)
            )
        case .file(let e):
            cardView(
                icon: "paperclip", title: e.displayName.isEmpty ? e.fileName : e.displayName,
                subtitle: e.mimeType, width: CGFloat(e.width)
            )
        }
    }

    @ViewBuilder
    private func imageView(_ element: ImageElement) -> some View {
        let url = store.assetURL(noteId: noteId, fileName: element.fileName)
        if let image = NSImage(contentsOf: url) {
            Image(nsImage: image)
                .resizable()
                .scaledToFit()
                .frame(width: CGFloat(element.width), height: CGFloat(element.height))
        } else {
            RoundedRectangle(cornerRadius: 8)
                .fill(theme.surface)
                .overlay(
                    VStack(spacing: 4) {
                        Image(systemName: "photo")
                        Text("immagine non sincronizzata").font(.caption)
                    }
                    .foregroundColor(theme.onSurfaceVariant)
                )
                .frame(width: CGFloat(element.width), height: CGFloat(element.height))
        }
    }

    private func cardView(icon: String, title: String, subtitle: String?, width: CGFloat) -> some View {
        HStack(spacing: 10) {
            Image(systemName: icon).foregroundColor(theme.primary)
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.body).lineLimit(2)
                if let subtitle, !subtitle.isEmpty {
                    Text(subtitle).font(.caption).foregroundColor(theme.onSurfaceVariant).lineLimit(1)
                }
            }
            Spacer(minLength: 0)
        }
        .padding(12)
        .frame(width: width, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 12).fill(theme.surface))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(theme.outlineVariant, lineWidth: 1))
    }

    // MARK: Gestures

    private var canvasDrag: some Gesture {
        DragGesture(minimumDistance: 0)
            .onChanged { value in
                switch tool {
                case .select:
                    let origin = dragOrigin ?? camera.offset
                    dragOrigin = origin
                    camera.offset = CGSize(
                        width: origin.width + value.translation.width,
                        height: origin.height + value.translation.height
                    )
                case .pen, .highlighter:
                    appendStrokePoint(camera.toWorld(value.location))
                case .eraser:
                    erase(at: camera.toWorld(value.location))
                case .text:
                    break
                }
            }
            .onEnded { value in
                switch tool {
                case .select:
                    dragOrigin = nil
                case .pen, .highlighter:
                    commitStroke()
                case .eraser:
                    break
                case .text:
                    addTextBlock(at: camera.toWorld(value.location))
                }
            }
    }

    private func appendStrokePoint(_ world: CGPoint) {
        let point = StrokePoint(x: Float(world.x), y: Float(world.y))
        if var stroke = activeStroke {
            stroke.points.append(point)
            activeStroke = stroke
        } else {
            activeStroke = InkStroke(
                type: tool == .highlighter ? .HIGHLIGHTER : .PEN,
                color: 0,
                width: Float(tool == .highlighter ? penWidth * 5 : penWidth),
                points: [point]
            )
        }
    }

    private func commitStroke() {
        guard let stroke = activeStroke, !stroke.points.isEmpty else { return }
        activeStroke = nil
        note?.content.strokes.append(stroke)
        scheduleSave()
    }

    private func erase(at world: CGPoint) {
        guard var current = note else { return }
        let reach = 18 / camera.scale
        let before = current.content.strokes.count
        current.content.strokes.removeAll { stroke in
            distanceToPolyline(world, stroke.points) <= reach + CGFloat(stroke.width) / 2
        }
        if current.content.strokes.count != before {
            note = current
            scheduleSave()
        }
    }

    private func addTextBlock(at world: CGPoint) {
        let element = TextElement(x: Float(world.x), y: Float(world.y), width: 420, text: "")
        note?.content.elements.append(.text(element))
        selectedElementId = element.id
        tool = .select
        scheduleSave()
    }

    private func deleteSelectedStroke() {
        guard let id = selectedStrokeId else { return }
        note?.content.strokes.removeAll { $0.id == id }
        selectedStrokeId = nil
        scheduleSave()
    }

    // MARK: Tools

    private var toolBar: some View {
        HStack(spacing: 12) {
            ForEach(EditorTool.allCases) { item in
                Button {
                    tool = item
                } label: {
                    Label(item.label, systemImage: item.symbol)
                        .labelStyle(.iconOnly)
                        .frame(width: 26, height: 22)
                }
                .buttonStyle(.borderless)
                .background(
                    RoundedRectangle(cornerRadius: 7)
                        .fill(tool == item ? theme.primary.opacity(0.22) : .clear)
                )
                .help(item.label)
            }

            if tool == .pen || tool == .highlighter {
                Slider(value: $penWidth, in: 1...40).frame(width: 130)
                Text("\(Int(penWidth))").font(.caption).monospacedDigit()
            }

            Spacer()

            Button { camera.zoom(by: 1 / 1.25, around: .zero) } label: {
                Image(systemName: "minus.magnifyingglass")
            }
            Text("\(Int(camera.scale * 100))%").font(.caption).monospacedDigit()
            Button { camera.zoom(by: 1.25, around: .zero) } label: {
                Image(systemName: "plus.magnifyingglass")
            }
            Button("Adatta") { zoomToFit() }
        }
        .buttonStyle(.borderless)
        .padding(.horizontal, 14)
        .padding(.vertical, 8)
    }

    /// Frames every piece of content in the viewport.
    private func zoomToFit() {
        guard let note else { return }
        var box: CGRect?
        for element in note.content.elements {
            box = box.map { $0.union(elementRect(element)) } ?? elementRect(element)
        }
        for frame in note.content.frames {
            let r = effectiveFrameRect(frame, note.content)
            box = box.map { $0.union(r) } ?? r
        }
        for stroke in note.content.strokes {
            for point in stroke.points {
                let r = CGRect(x: CGFloat(point.x), y: CGFloat(point.y), width: 1, height: 1)
                box = box.map { $0.union(r) } ?? r
            }
        }
        guard let bounds = box, bounds.width > 0, bounds.height > 0 else { return }
        camera.scale = 1
        camera.offset = CGSize(width: 60 - bounds.minX, height: 60 - bounds.minY)
    }

    // MARK: Saving

    /// Coalesces edits into one write ~600 ms after the user stops.
    private func scheduleSave() {
        saveTask?.cancel()
        let snapshot = note
        saveTask = Task { @MainActor in
            try? await Task.sleep(nanoseconds: 600_000_000)
            guard !Task.isCancelled, let snapshot else { return }
            store.save(snapshot)
        }
    }
}

/// Small stand-in so the app also builds against macOS 13, where
/// `ContentUnavailableView` does not exist yet.
struct ContentUnavailableFallback: View {
    let title: String
    let message: String

    var body: some View {
        VStack(spacing: 8) {
            Text(title).font(.title3)
            Text(message)
                .font(.callout)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(40)
    }
}
