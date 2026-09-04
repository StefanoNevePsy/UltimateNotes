// NoteModel.swift
// UltimateNotesMac
//
// Mirrors app/src/main/java/com/stefanoneve/ultimatenotes/data/model/NoteContent.kt
// 1:1 so the JSON this app reads/writes round-trips byte-for-byte with the
// kotlinx.serialization output the Android app produces. Every Codable
// conformance below is written by hand (never synthesized) so that decoding
// tolerates missing keys the way `encodeDefaults = true` + a Kotlin default
// value does, and so the polymorphic `elements` array uses the same *inline*
// "type" discriminator kotlinx.serialization writes (not a nested payload).

import Foundation

// MARK: - Ink

/// A single sampled point of an ink stroke, in canvas (world) coordinates.
struct StrokePoint: Codable, Hashable {
    var x: Float
    var y: Float
    /// Stylus pressure in 0...1.5, used to modulate stroke width.
    var p: Float = 1

    init(x: Float, y: Float, p: Float = 1) {
        self.x = x
        self.y = y
        self.p = p
    }

    private enum CodingKeys: String, CodingKey { case x, y, p }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        x = try c.decodeIfPresent(Float.self, forKey: .x) ?? 0
        y = try c.decodeIfPresent(Float.self, forKey: .y) ?? 0
        p = try c.decodeIfPresent(Float.self, forKey: .p) ?? 1
    }

    func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(x, forKey: .x)
        try c.encode(y, forKey: .y)
        try c.encode(p, forKey: .p)
    }
}

enum StrokeType: String, Codable { case PEN, HIGHLIGHTER }
enum LineStyle: String, Codable { case SOLID, DASHED, DOTTED }
enum CapStyle: String, Codable { case NONE, ARROW, DOT }
enum FrameShape: String, Codable { case RECT, ROUNDED, ELLIPSE, SKETCHY }
enum TapePattern: String, Codable { case SOLID, STRIPES, DOTS, ZIGZAG, GRID }
enum CanvasBackground: String, Codable { case BLANK, DOTS, GRID, LINES, PAPER, SCANLINES }

struct InkStroke: Codable, Identifiable, Hashable {
    var id: String = UUID().uuidString.lowercased()
    var type: StrokeType = .PEN
    /// ARGB color packed in an Int64 (0xAARRGGBB), or a theme role slot:
    /// 0 = the theme's ink color, 1...16 = accent slot (see Theme.swift).
    var color: Int64 = 0
    /// Base stroke width in canvas units.
    var width: Float = 4
    var points: [StrokePoint] = []
    /// nil = "auto": solid, or the theme's line style for decorative pens.
    var lineStyle: LineStyle? = nil
    /// Marching-dashes animation, like connectors.
    var animated: Bool = false

    init(
        id: String = UUID().uuidString.lowercased(), type: StrokeType = .PEN, color: Int64 = 0,
        width: Float = 4, points: [StrokePoint] = [], lineStyle: LineStyle? = nil, animated: Bool = false
    ) {
        self.id = id
        self.type = type
        self.color = color
        self.width = width
        self.points = points
        self.lineStyle = lineStyle
        self.animated = animated
    }

    private enum CodingKeys: String, CodingKey { case id, type, color, width, points, lineStyle, animated }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decodeIfPresent(String.self, forKey: .id) ?? UUID().uuidString.lowercased()
        type = try c.decodeIfPresent(StrokeType.self, forKey: .type) ?? .PEN
        color = try c.decodeIfPresent(Int64.self, forKey: .color) ?? 0
        width = try c.decodeIfPresent(Float.self, forKey: .width) ?? 4
        points = try c.decodeIfPresent([StrokePoint].self, forKey: .points) ?? []
        lineStyle = try c.decodeIfPresent(LineStyle.self, forKey: .lineStyle) ?? nil
        animated = try c.decodeIfPresent(Bool.self, forKey: .animated) ?? false
    }

    func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(id, forKey: .id)
        try c.encode(type, forKey: .type)
        try c.encode(color, forKey: .color)
        try c.encode(width, forKey: .width)
        try c.encode(points, forKey: .points)
        try c.encode(lineStyle, forKey: .lineStyle)
        try c.encode(animated, forKey: .animated)
    }
}

// MARK: - Canvas elements

/// Sentinel for "themed" sticky background (an impossible real color).
let STICKY_AUTO: Int64 = 1

struct TextElement: Codable, Identifiable, Hashable {
    var id: String = UUID().uuidString.lowercased()
    var x: Float = 0
    var y: Float = 0
    /// Elements sharing a non-nil groupId move together.
    var groupId: String? = nil
    var width: Float = 600
    /// Vector zoom factor of the whole block (text stays crisp).
    var scale: Float = 1
    /// Markdown source of the block (supports inline {c:#hex} / {f:id} tags).
    var text: String = ""
    /// Id of the paragraph style used as the base style.
    var styleId: String = "body"
    /// Optional font id from FontManager; nil follows the active theme.
    var fontId: String? = nil
    /// Optional ARGB color override; nil adapts to the active theme.
    var color: Int64? = nil
    /// Optional sticky-note background; nil = transparent, STICKY_AUTO = themed.
    var bgColor: Int64? = nil
    /// Optional block font size override; nil = paragraph style size.
    var fontSize: Float? = nil
    /// Block "skin": nil = none, "auto" = the theme's signature decor, or an explicit decor id.
    var decor: String? = nil

    init(
        id: String = UUID().uuidString.lowercased(), x: Float = 0, y: Float = 0, groupId: String? = nil,
        width: Float = 600, scale: Float = 1, text: String = "", styleId: String = "body",
        fontId: String? = nil, color: Int64? = nil, bgColor: Int64? = nil, fontSize: Float? = nil,
        decor: String? = nil
    ) {
        self.id = id
        self.x = x
        self.y = y
        self.groupId = groupId
        self.width = width
        self.scale = scale
        self.text = text
        self.styleId = styleId
        self.fontId = fontId
        self.color = color
        self.bgColor = bgColor
        self.fontSize = fontSize
        self.decor = decor
    }

    private enum CodingKeys: String, CodingKey {
        case id, x, y, groupId, width, scale, text, styleId, fontId, color, bgColor, fontSize, decor
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decodeIfPresent(String.self, forKey: .id) ?? UUID().uuidString.lowercased()
        x = try c.decodeIfPresent(Float.self, forKey: .x) ?? 0
        y = try c.decodeIfPresent(Float.self, forKey: .y) ?? 0
        groupId = try c.decodeIfPresent(String.self, forKey: .groupId) ?? nil
        width = try c.decodeIfPresent(Float.self, forKey: .width) ?? 600
        scale = try c.decodeIfPresent(Float.self, forKey: .scale) ?? 1
        text = try c.decodeIfPresent(String.self, forKey: .text) ?? ""
        styleId = try c.decodeIfPresent(String.self, forKey: .styleId) ?? "body"
        fontId = try c.decodeIfPresent(String.self, forKey: .fontId) ?? nil
        color = try c.decodeIfPresent(Int64.self, forKey: .color) ?? nil
        bgColor = try c.decodeIfPresent(Int64.self, forKey: .bgColor) ?? nil
        fontSize = try c.decodeIfPresent(Float.self, forKey: .fontSize) ?? nil
        decor = try c.decodeIfPresent(String.self, forKey: .decor) ?? nil
    }

    func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(id, forKey: .id)
        try c.encode(x, forKey: .x)
        try c.encode(y, forKey: .y)
        try c.encode(groupId, forKey: .groupId)
        try c.encode(width, forKey: .width)
        try c.encode(scale, forKey: .scale)
        try c.encode(text, forKey: .text)
        try c.encode(styleId, forKey: .styleId)
        try c.encode(fontId, forKey: .fontId)
        try c.encode(color, forKey: .color)
        try c.encode(bgColor, forKey: .bgColor)
        try c.encode(fontSize, forKey: .fontSize)
        try c.encode(decor, forKey: .decor)
    }
}

struct ImageElement: Codable, Identifiable, Hashable {
    var id: String = UUID().uuidString.lowercased()
    var x: Float = 0
    var y: Float = 0
    var groupId: String? = nil
    var width: Float = 400
    var height: Float = 400
    /// File name inside the note's asset directory.
    var fileName: String = ""
    /// True when the image is a rendered PDF page (annotatable like any image).
    var isPdfPage: Bool = false
    /// 1-based page number when isPdfPage.
    var pdfPage: Int = 0

    init(
        id: String = UUID().uuidString.lowercased(), x: Float = 0, y: Float = 0, groupId: String? = nil,
        width: Float = 400, height: Float = 400, fileName: String = "", isPdfPage: Bool = false, pdfPage: Int = 0
    ) {
        self.id = id
        self.x = x
        self.y = y
        self.groupId = groupId
        self.width = width
        self.height = height
        self.fileName = fileName
        self.isPdfPage = isPdfPage
        self.pdfPage = pdfPage
    }

    private enum CodingKeys: String, CodingKey { case id, x, y, groupId, width, height, fileName, isPdfPage, pdfPage }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decodeIfPresent(String.self, forKey: .id) ?? UUID().uuidString.lowercased()
        x = try c.decodeIfPresent(Float.self, forKey: .x) ?? 0
        y = try c.decodeIfPresent(Float.self, forKey: .y) ?? 0
        groupId = try c.decodeIfPresent(String.self, forKey: .groupId) ?? nil
        width = try c.decodeIfPresent(Float.self, forKey: .width) ?? 400
        height = try c.decodeIfPresent(Float.self, forKey: .height) ?? 400
        fileName = try c.decodeIfPresent(String.self, forKey: .fileName) ?? ""
        isPdfPage = try c.decodeIfPresent(Bool.self, forKey: .isPdfPage) ?? false
        pdfPage = try c.decodeIfPresent(Int.self, forKey: .pdfPage) ?? 0
    }

    func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(id, forKey: .id)
        try c.encode(x, forKey: .x)
        try c.encode(y, forKey: .y)
        try c.encode(groupId, forKey: .groupId)
        try c.encode(width, forKey: .width)
        try c.encode(height, forKey: .height)
        try c.encode(fileName, forKey: .fileName)
        try c.encode(isPdfPage, forKey: .isPdfPage)
        try c.encode(pdfPage, forKey: .pdfPage)
    }
}

/// A live link to another note, rendered as a preview card on the canvas.
struct NoteLinkElement: Codable, Identifiable, Hashable {
    var id: String = UUID().uuidString.lowercased()
    var x: Float = 0
    var y: Float = 0
    var groupId: String? = nil
    var width: Float = 420
    var scale: Float = 1
    var targetNoteId: String = ""

    init(
        id: String = UUID().uuidString.lowercased(), x: Float = 0, y: Float = 0, groupId: String? = nil,
        width: Float = 420, scale: Float = 1, targetNoteId: String = ""
    ) {
        self.id = id
        self.x = x
        self.y = y
        self.groupId = groupId
        self.width = width
        self.scale = scale
        self.targetNoteId = targetNoteId
    }

    private enum CodingKeys: String, CodingKey { case id, x, y, groupId, width, scale, targetNoteId }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decodeIfPresent(String.self, forKey: .id) ?? UUID().uuidString.lowercased()
        x = try c.decodeIfPresent(Float.self, forKey: .x) ?? 0
        y = try c.decodeIfPresent(Float.self, forKey: .y) ?? 0
        groupId = try c.decodeIfPresent(String.self, forKey: .groupId) ?? nil
        width = try c.decodeIfPresent(Float.self, forKey: .width) ?? 420
        scale = try c.decodeIfPresent(Float.self, forKey: .scale) ?? 1
        targetNoteId = try c.decodeIfPresent(String.self, forKey: .targetNoteId) ?? ""
    }

    func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(id, forKey: .id)
        try c.encode(x, forKey: .x)
        try c.encode(y, forKey: .y)
        try c.encode(groupId, forKey: .groupId)
        try c.encode(width, forKey: .width)
        try c.encode(scale, forKey: .scale)
        try c.encode(targetNoteId, forKey: .targetNoteId)
    }
}

/// An embedded web link, rendered as a card that opens the browser.
struct WebLinkElement: Codable, Identifiable, Hashable {
    var id: String = UUID().uuidString.lowercased()
    var x: Float = 0
    var y: Float = 0
    var groupId: String? = nil
    var width: Float = 420
    var scale: Float = 1
    var url: String = ""
    /// Page title, fetched best-effort when the link is added.
    var title: String = ""

    init(
        id: String = UUID().uuidString.lowercased(), x: Float = 0, y: Float = 0, groupId: String? = nil,
        width: Float = 420, scale: Float = 1, url: String = "", title: String = ""
    ) {
        self.id = id
        self.x = x
        self.y = y
        self.groupId = groupId
        self.width = width
        self.scale = scale
        self.url = url
        self.title = title
    }

    private enum CodingKeys: String, CodingKey { case id, x, y, groupId, width, scale, url, title }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decodeIfPresent(String.self, forKey: .id) ?? UUID().uuidString.lowercased()
        x = try c.decodeIfPresent(Float.self, forKey: .x) ?? 0
        y = try c.decodeIfPresent(Float.self, forKey: .y) ?? 0
        groupId = try c.decodeIfPresent(String.self, forKey: .groupId) ?? nil
        width = try c.decodeIfPresent(Float.self, forKey: .width) ?? 420
        scale = try c.decodeIfPresent(Float.self, forKey: .scale) ?? 1
        url = try c.decodeIfPresent(String.self, forKey: .url) ?? ""
        title = try c.decodeIfPresent(String.self, forKey: .title) ?? ""
    }

    func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(id, forKey: .id)
        try c.encode(x, forKey: .x)
        try c.encode(y, forKey: .y)
        try c.encode(groupId, forKey: .groupId)
        try c.encode(width, forKey: .width)
        try c.encode(scale, forKey: .scale)
        try c.encode(url, forKey: .url)
        try c.encode(title, forKey: .title)
    }
}

/// A file attached to the note (any type), openable with the system viewer.
struct FileElement: Codable, Identifiable, Hashable {
    var id: String = UUID().uuidString.lowercased()
    var x: Float = 0
    var y: Float = 0
    var groupId: String? = nil
    var width: Float = 380
    var scale: Float = 1
    /// File name inside the note's asset directory.
    var fileName: String = ""
    /// Original display name.
    var displayName: String = ""
    var mimeType: String = ""
    var sizeBytes: Int64 = 0

    init(
        id: String = UUID().uuidString.lowercased(), x: Float = 0, y: Float = 0, groupId: String? = nil,
        width: Float = 380, scale: Float = 1, fileName: String = "", displayName: String = "",
        mimeType: String = "", sizeBytes: Int64 = 0
    ) {
        self.id = id
        self.x = x
        self.y = y
        self.groupId = groupId
        self.width = width
        self.scale = scale
        self.fileName = fileName
        self.displayName = displayName
        self.mimeType = mimeType
        self.sizeBytes = sizeBytes
    }

    private enum CodingKeys: String, CodingKey {
        case id, x, y, groupId, width, scale, fileName, displayName, mimeType, sizeBytes
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decodeIfPresent(String.self, forKey: .id) ?? UUID().uuidString.lowercased()
        x = try c.decodeIfPresent(Float.self, forKey: .x) ?? 0
        y = try c.decodeIfPresent(Float.self, forKey: .y) ?? 0
        groupId = try c.decodeIfPresent(String.self, forKey: .groupId) ?? nil
        width = try c.decodeIfPresent(Float.self, forKey: .width) ?? 380
        scale = try c.decodeIfPresent(Float.self, forKey: .scale) ?? 1
        fileName = try c.decodeIfPresent(String.self, forKey: .fileName) ?? ""
        displayName = try c.decodeIfPresent(String.self, forKey: .displayName) ?? ""
        mimeType = try c.decodeIfPresent(String.self, forKey: .mimeType) ?? ""
        sizeBytes = try c.decodeIfPresent(Int64.self, forKey: .sizeBytes) ?? 0
    }

    func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(id, forKey: .id)
        try c.encode(x, forKey: .x)
        try c.encode(y, forKey: .y)
        try c.encode(groupId, forKey: .groupId)
        try c.encode(width, forKey: .width)
        try c.encode(scale, forKey: .scale)
        try c.encode(fileName, forKey: .fileName)
        try c.encode(displayName, forKey: .displayName)
        try c.encode(mimeType, forKey: .mimeType)
        try c.encode(sizeBytes, forKey: .sizeBytes)
    }
}

/// Anything placed freely on the infinite canvas. Polymorphic like Kotlin's
/// `sealed interface NoteElement`, keyed by kotlinx.serialization's default
/// discriminator field "type" written *inline* alongside the payload fields
/// (never as a nested object) — so decode/encode share the outer container.
enum NoteElement: Codable, Identifiable, Hashable {
    case text(TextElement)
    case image(ImageElement)
    case noteLink(NoteLinkElement)
    case webLink(WebLinkElement)
    case file(FileElement)

    private enum TypeKey: String, CodingKey { case type }

    init(from decoder: Decoder) throws {
        let typeContainer = try decoder.container(keyedBy: TypeKey.self)
        let type = try typeContainer.decode(String.self, forKey: .type)
        switch type {
        case "text": self = .text(try TextElement(from: decoder))
        case "image": self = .image(try ImageElement(from: decoder))
        case "notelink": self = .noteLink(try NoteLinkElement(from: decoder))
        case "weblink": self = .webLink(try WebLinkElement(from: decoder))
        case "file": self = .file(try FileElement(from: decoder))
        default:
            throw DecodingError.dataCorruptedError(
                forKey: .type, in: typeContainer, debugDescription: "Unknown element type '\(type)'"
            )
        }
    }

    func encode(to encoder: Encoder) throws {
        var typeContainer = encoder.container(keyedBy: TypeKey.self)
        switch self {
        case .text(let e):
            try typeContainer.encode("text", forKey: .type)
            try e.encode(to: encoder)
        case .image(let e):
            try typeContainer.encode("image", forKey: .type)
            try e.encode(to: encoder)
        case .noteLink(let e):
            try typeContainer.encode("notelink", forKey: .type)
            try e.encode(to: encoder)
        case .webLink(let e):
            try typeContainer.encode("weblink", forKey: .type)
            try e.encode(to: encoder)
        case .file(let e):
            try typeContainer.encode("file", forKey: .type)
            try e.encode(to: encoder)
        }
    }

    var id: String {
        switch self {
        case .text(let e): return e.id
        case .image(let e): return e.id
        case .noteLink(let e): return e.id
        case .webLink(let e): return e.id
        case .file(let e): return e.id
        }
    }

    var x: Float {
        switch self {
        case .text(let e): return e.x
        case .image(let e): return e.x
        case .noteLink(let e): return e.x
        case .webLink(let e): return e.x
        case .file(let e): return e.x
        }
    }

    var y: Float {
        switch self {
        case .text(let e): return e.y
        case .image(let e): return e.y
        case .noteLink(let e): return e.y
        case .webLink(let e): return e.y
        case .file(let e): return e.y
        }
    }

    /// Elements sharing a non-nil groupId move together.
    var groupId: String? {
        switch self {
        case .text(let e): return e.groupId
        case .image(let e): return e.groupId
        case .noteLink(let e): return e.groupId
        case .webLink(let e): return e.groupId
        case .file(let e): return e.groupId
        }
    }

    /// Returns a copy translated by (dx, dy), preserving every other field.
    func moved(dx: Float, dy: Float) -> NoteElement {
        switch self {
        case .text(var e):
            e.x += dx; e.y += dy
            return .text(e)
        case .image(var e):
            e.x += dx; e.y += dy
            return .image(e)
        case .noteLink(var e):
            e.x += dx; e.y += dy
            return .noteLink(e)
        case .webLink(var e):
            e.x += dx; e.y += dy
            return .webLink(e)
        case .file(var e):
            e.x += dx; e.y += dy
            return .file(e)
        }
    }
}

// MARK: - Connectors, frames, tape

/// Kinopio-style connector between two elements: a quadratic bezier whose
/// control point can be dragged, with configurable stroke and end caps.
struct ConnectorElement: Codable, Identifiable, Hashable {
    var id: String = UUID().uuidString.lowercased()
    /// Id of an element **or** a frame.
    var fromId: String
    var toId: String
    /// 0 = "auto": follows the active theme's accent.
    var color: Int64 = 0
    var width: Float = 3.5
    /// nil = "auto": follows the active theme's connector style.
    var lineStyle: LineStyle? = nil
    /// Marching-dashes animation along the line.
    var animated: Bool = false
    var startCap: CapStyle = .NONE
    var endCap: CapStyle = .ARROW
    /// Offset of the bezier control point from the segment midpoint.
    var curveDx: Float = 0
    var curveDy: Float = 0
    /// Optional intermediate nodes: the line becomes a smooth Catmull-Rom
    /// spline through all of them when non-empty.
    var nodes: [StrokePoint] = []

    init(
        id: String = UUID().uuidString.lowercased(), fromId: String, toId: String, color: Int64 = 0,
        width: Float = 3.5, lineStyle: LineStyle? = nil, animated: Bool = false,
        startCap: CapStyle = .NONE, endCap: CapStyle = .ARROW, curveDx: Float = 0, curveDy: Float = 0,
        nodes: [StrokePoint] = []
    ) {
        self.id = id
        self.fromId = fromId
        self.toId = toId
        self.color = color
        self.width = width
        self.lineStyle = lineStyle
        self.animated = animated
        self.startCap = startCap
        self.endCap = endCap
        self.curveDx = curveDx
        self.curveDy = curveDy
        self.nodes = nodes
    }

    private enum CodingKeys: String, CodingKey {
        case id, fromId, toId, color, width, lineStyle, animated, startCap, endCap, curveDx, curveDy, nodes
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decodeIfPresent(String.self, forKey: .id) ?? UUID().uuidString.lowercased()
        // fromId/toId have no Kotlin default (required constructor params): a
        // note missing either is malformed, so let decoding fail for it — the
        // caller (VaultStore) skips files it cannot decode rather than crash.
        fromId = try c.decode(String.self, forKey: .fromId)
        toId = try c.decode(String.self, forKey: .toId)
        color = try c.decodeIfPresent(Int64.self, forKey: .color) ?? 0
        width = try c.decodeIfPresent(Float.self, forKey: .width) ?? 3.5
        lineStyle = try c.decodeIfPresent(LineStyle.self, forKey: .lineStyle) ?? nil
        animated = try c.decodeIfPresent(Bool.self, forKey: .animated) ?? false
        startCap = try c.decodeIfPresent(CapStyle.self, forKey: .startCap) ?? .NONE
        endCap = try c.decodeIfPresent(CapStyle.self, forKey: .endCap) ?? .ARROW
        curveDx = try c.decodeIfPresent(Float.self, forKey: .curveDx) ?? 0
        curveDy = try c.decodeIfPresent(Float.self, forKey: .curveDy) ?? 0
        nodes = try c.decodeIfPresent([StrokePoint].self, forKey: .nodes) ?? []
    }

    func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(id, forKey: .id)
        try c.encode(fromId, forKey: .fromId)
        try c.encode(toId, forKey: .toId)
        try c.encode(color, forKey: .color)
        try c.encode(width, forKey: .width)
        try c.encode(lineStyle, forKey: .lineStyle)
        try c.encode(animated, forKey: .animated)
        try c.encode(startCap, forKey: .startCap)
        try c.encode(endCap, forKey: .endCap)
        try c.encode(curveDx, forKey: .curveDx)
        try c.encode(curveDy, forKey: .curveDy)
        try c.encode(nodes, forKey: .nodes)
    }
}

/// A decorative frame that visually groups a region of the canvas. Moving a
/// frame drags along every element currently inside it.
struct FrameElement: Codable, Identifiable, Hashable {
    var id: String = UUID().uuidString.lowercased()
    var x: Float = 0
    var y: Float = 0
    var width: Float = 400
    var height: Float = 300
    /// nil = "auto": follows the active theme.
    var shape: FrameShape? = nil
    var lineStyle: LineStyle? = nil
    var animated: Bool = false
    /// 0 = "auto": follows the active theme's accent.
    var color: Int64 = 0
    var strokeWidth: Float = 3
    /// Fill the frame with a translucent tint of `color`.
    var filled: Bool = false
    var label: String = ""
    /// nil = plain outline, "auto" = the theme's signature decor, else a skin id.
    var decor: String? = nil
    /// When true the frame grows to always contain the elements inside it
    /// (its stored size is the manual minimum).
    var autoFit: Bool = true
    /// Elements explicitly captured by this frame; empty = geometric containment.
    var memberIds: [String] = []

    init(
        id: String = UUID().uuidString.lowercased(), x: Float = 0, y: Float = 0, width: Float = 400,
        height: Float = 300, shape: FrameShape? = nil, lineStyle: LineStyle? = nil, animated: Bool = false,
        color: Int64 = 0, strokeWidth: Float = 3, filled: Bool = false, label: String = "",
        decor: String? = nil, autoFit: Bool = true, memberIds: [String] = []
    ) {
        self.id = id
        self.x = x
        self.y = y
        self.width = width
        self.height = height
        self.shape = shape
        self.lineStyle = lineStyle
        self.animated = animated
        self.color = color
        self.strokeWidth = strokeWidth
        self.filled = filled
        self.label = label
        self.decor = decor
        self.autoFit = autoFit
        self.memberIds = memberIds
    }

    private enum CodingKeys: String, CodingKey {
        case id, x, y, width, height, shape, lineStyle, animated, color, strokeWidth, filled, label, decor,
             autoFit, memberIds
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decodeIfPresent(String.self, forKey: .id) ?? UUID().uuidString.lowercased()
        x = try c.decodeIfPresent(Float.self, forKey: .x) ?? 0
        y = try c.decodeIfPresent(Float.self, forKey: .y) ?? 0
        width = try c.decodeIfPresent(Float.self, forKey: .width) ?? 400
        height = try c.decodeIfPresent(Float.self, forKey: .height) ?? 300
        shape = try c.decodeIfPresent(FrameShape.self, forKey: .shape) ?? nil
        lineStyle = try c.decodeIfPresent(LineStyle.self, forKey: .lineStyle) ?? nil
        animated = try c.decodeIfPresent(Bool.self, forKey: .animated) ?? false
        color = try c.decodeIfPresent(Int64.self, forKey: .color) ?? 0
        strokeWidth = try c.decodeIfPresent(Float.self, forKey: .strokeWidth) ?? 3
        filled = try c.decodeIfPresent(Bool.self, forKey: .filled) ?? false
        label = try c.decodeIfPresent(String.self, forKey: .label) ?? ""
        decor = try c.decodeIfPresent(String.self, forKey: .decor) ?? nil
        autoFit = try c.decodeIfPresent(Bool.self, forKey: .autoFit) ?? true
        memberIds = try c.decodeIfPresent([String].self, forKey: .memberIds) ?? []
    }

    func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(id, forKey: .id)
        try c.encode(x, forKey: .x)
        try c.encode(y, forKey: .y)
        try c.encode(width, forKey: .width)
        try c.encode(height, forKey: .height)
        try c.encode(shape, forKey: .shape)
        try c.encode(lineStyle, forKey: .lineStyle)
        try c.encode(animated, forKey: .animated)
        try c.encode(color, forKey: .color)
        try c.encode(strokeWidth, forKey: .strokeWidth)
        try c.encode(filled, forKey: .filled)
        try c.encode(label, forKey: .label)
        try c.encode(decor, forKey: .decor)
        try c.encode(autoFit, forKey: .autoFit)
        try c.encode(memberIds, forKey: .memberIds)
    }
}

/// A straight strip of washi tape: decorative, semi-translucent, patterned.
struct TapeElement: Codable, Identifiable, Hashable {
    var id: String = UUID().uuidString.lowercased()
    var x1: Float = 0
    var y1: Float = 0
    var x2: Float = 100
    var y2: Float = 0
    /// Strip thickness in canvas units.
    var thickness: Float = 36
    /// 0 = "auto": follows the active theme's tape colors.
    var color: Int64 = 0
    /// nil = "auto": follows the active theme's tape pattern.
    var pattern: TapePattern? = nil
    var alpha: Float = 0.85

    init(
        id: String = UUID().uuidString.lowercased(), x1: Float = 0, y1: Float = 0, x2: Float = 100,
        y2: Float = 0, thickness: Float = 36, color: Int64 = 0, pattern: TapePattern? = nil, alpha: Float = 0.85
    ) {
        self.id = id
        self.x1 = x1
        self.y1 = y1
        self.x2 = x2
        self.y2 = y2
        self.thickness = thickness
        self.color = color
        self.pattern = pattern
        self.alpha = alpha
    }

    private enum CodingKeys: String, CodingKey { case id, x1, y1, x2, y2, thickness, color, pattern, alpha }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decodeIfPresent(String.self, forKey: .id) ?? UUID().uuidString.lowercased()
        x1 = try c.decodeIfPresent(Float.self, forKey: .x1) ?? 0
        y1 = try c.decodeIfPresent(Float.self, forKey: .y1) ?? 0
        x2 = try c.decodeIfPresent(Float.self, forKey: .x2) ?? 100
        y2 = try c.decodeIfPresent(Float.self, forKey: .y2) ?? 0
        thickness = try c.decodeIfPresent(Float.self, forKey: .thickness) ?? 36
        color = try c.decodeIfPresent(Int64.self, forKey: .color) ?? 0
        pattern = try c.decodeIfPresent(TapePattern.self, forKey: .pattern) ?? nil
        alpha = try c.decodeIfPresent(Float.self, forKey: .alpha) ?? 0.85
    }

    func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(id, forKey: .id)
        try c.encode(x1, forKey: .x1)
        try c.encode(y1, forKey: .y1)
        try c.encode(x2, forKey: .x2)
        try c.encode(y2, forKey: .y2)
        try c.encode(thickness, forKey: .thickness)
        try c.encode(color, forKey: .color)
        try c.encode(pattern, forKey: .pattern)
        try c.encode(alpha, forKey: .alpha)
    }
}

// MARK: - Note content

/// Full drawable/editable content of a note.
struct NoteContent: Codable, Hashable {
    var elements: [NoteElement] = []
    var strokes: [InkStroke] = []
    var connectors: [ConnectorElement] = []
    var frames: [FrameElement] = []
    var tapes: [TapeElement] = []
    var background: CanvasBackground = .DOTS

    init(
        elements: [NoteElement] = [], strokes: [InkStroke] = [], connectors: [ConnectorElement] = [],
        frames: [FrameElement] = [], tapes: [TapeElement] = [], background: CanvasBackground = .DOTS
    ) {
        self.elements = elements
        self.strokes = strokes
        self.connectors = connectors
        self.frames = frames
        self.tapes = tapes
        self.background = background
    }

    private enum CodingKeys: String, CodingKey { case elements, strokes, connectors, frames, tapes, background }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        elements = try c.decodeIfPresent([NoteElement].self, forKey: .elements) ?? []
        strokes = try c.decodeIfPresent([InkStroke].self, forKey: .strokes) ?? []
        connectors = try c.decodeIfPresent([ConnectorElement].self, forKey: .connectors) ?? []
        frames = try c.decodeIfPresent([FrameElement].self, forKey: .frames) ?? []
        tapes = try c.decodeIfPresent([TapeElement].self, forKey: .tapes) ?? []
        background = try c.decodeIfPresent(CanvasBackground.self, forKey: .background) ?? .DOTS
    }

    func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(elements, forKey: .elements)
        try c.encode(strokes, forKey: .strokes)
        try c.encode(connectors, forKey: .connectors)
        try c.encode(frames, forKey: .frames)
        try c.encode(tapes, forKey: .tapes)
        try c.encode(background, forKey: .background)
    }

    /// Plain text extraction used for search and previews.
    func plainText() -> String {
        elements
            .compactMap { element -> String? in
                switch element {
                case .text(let t): return stripMarkup(t.text)
                case .webLink(let w): return "\(w.title) \(w.url)".trimmingCharacters(in: .whitespaces)
                case .file(let f): return f.displayName
                case .image, .noteLink: return nil
                }
            }
            .filter { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
            .joined(separator: "\n")
    }
}

// MARK: - Markup stripping

/// Inline tags like {c:#ff0000}...{/c}, {f:serif}...{/f}, {s:24}...{/s}.
private let inlineTagPattern = #"\{[cfs]:[^{}]*\}|\{/[cfs]\}"#
private let lineMarkerPattern = #"^\s*(#{1,6}\s+|>\s+|[-*+]\s+|\d{1,3}[.)]\s+|\[[ xX]\]\s*)+"#

private func regexReplace(_ pattern: String, in text: String) -> String {
    guard let re = try? NSRegularExpression(pattern: pattern) else { return text }
    let range = NSRange(text.startIndex..., in: text)
    return re.stringByReplacingMatches(in: text, range: range, withTemplate: "")
}

/// Removes markdown markers and inline styling tags so search and the home
/// previews see (and match) only the words the user actually wrote.
func stripMarkup(_ text: String) -> String {
    let withoutTags = regexReplace(inlineTagPattern, in: text)
    return withoutTags
        .components(separatedBy: "\n")
        .map { line -> String in
            regexReplace(lineMarkerPattern, in: line)
                .replacingOccurrences(of: "**", with: "")
                .replacingOccurrences(of: "~~", with: "")
                .replacingOccurrences(of: "`", with: "")
        }
        .joined(separator: "\n")
}
