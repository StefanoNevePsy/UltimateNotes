// CanvasRender.swift
// UltimateNotesMac
//
// Draws a note on the infinite canvas, in the same z-order as the Android
// app (see docs/VAULT_FORMAT.md): background, frames, connectors, frame
// labels, elements, tape, and ink always on top.

import SwiftUI

// MARK: - Camera

/// Pan/zoom of the infinite canvas. World → screen: `p * scale + offset`.
final class CanvasState: ObservableObject {
    @Published var offset: CGSize = .zero
    @Published var scale: CGFloat = 1

    func toWorld(_ screen: CGPoint) -> CGPoint {
        CGPoint(
            x: (screen.x - offset.width) / scale,
            y: (screen.y - offset.height) / scale
        )
    }

    func toScreen(_ world: CGPoint) -> CGPoint {
        CGPoint(
            x: world.x * scale + offset.width,
            y: world.y * scale + offset.height
        )
    }

    /// Zooms around a screen anchor so the point under the cursor stays put.
    func zoom(by factor: CGFloat, around anchor: CGPoint) {
        let newScale = min(max(scale * factor, 0.1), 8)
        let effective = newScale / scale
        offset = CGSize(
            width: anchor.x - (anchor.x - offset.width) * effective,
            height: anchor.y - (anchor.y - offset.height) * effective
        )
        scale = newScale
    }
}

// MARK: - Geometry shared with the Android renderer

/// Padding an auto-fitting frame keeps around its content.
private let framePadding: CGFloat = 22

func elementRect(_ element: NoteElement) -> CGRect {
    let x = CGFloat(element.x)
    let y = CGFloat(element.y)
    switch element {
    case .text(let e):
        // Height is unknown until the text is laid out; estimate from the
        // content so frames and connectors have something stable to anchor to.
        let lines = max(1, e.text.split(separator: "\n", omittingEmptySubsequences: false).count)
        let lineHeight = CGFloat(e.fontSize ?? 16) * 1.35
        return CGRect(
            x: x, y: y,
            width: CGFloat(e.width * e.scale),
            height: max(40, CGFloat(Float(lines) * Float(lineHeight)) * CGFloat(e.scale))
        )
    case .image(let e):
        return CGRect(x: x, y: y, width: CGFloat(e.width), height: CGFloat(e.height))
    case .noteLink(let e):
        return CGRect(x: x, y: y, width: CGFloat(e.width * e.scale), height: 120 * CGFloat(e.scale))
    case .webLink(let e):
        return CGRect(x: x, y: y, width: CGFloat(e.width * e.scale), height: 110 * CGFloat(e.scale))
    case .file(let e):
        return CGRect(x: x, y: y, width: CGFloat(e.width * e.scale), height: 100 * CGFloat(e.scale))
    }
}

func frameRect(_ frame: FrameElement) -> CGRect {
    CGRect(
        x: CGFloat(frame.x), y: CGFloat(frame.y),
        width: CGFloat(frame.width), height: CGFloat(frame.height)
    )
}

/// Bounds a frame actually draws at. With `autoFit` the box is anchored to
/// its content — the stored size only acts as a manual minimum — which is
/// what keeps it from snapping back to its original size.
func effectiveFrameRect(_ frame: FrameElement, _ content: NoteContent) -> CGRect {
    let stored = frameRect(frame)
    guard frame.autoFit else { return stored }

    let members: [NoteElement]
    if !frame.memberIds.isEmpty {
        let ids = Set(frame.memberIds)
        members = content.elements.filter { ids.contains($0.id) }
    } else {
        members = content.elements.filter { stored.contains(elementRect($0).center) }
    }
    guard !members.isEmpty else { return stored }

    var box = elementRect(members[0])
    for element in members.dropFirst() {
        box = box.union(elementRect(element))
    }
    box = box.insetBy(dx: -framePadding, dy: -framePadding)
    return CGRect(
        x: box.minX, y: box.minY,
        width: max(box.width, stored.width),
        height: max(box.height, stored.height)
    )
}

/// Bounding box of a connector endpoint: an element or a frame.
func anchorRect(_ id: String, _ content: NoteContent) -> CGRect? {
    if let element = content.elements.first(where: { $0.id == id }) {
        return elementRect(element)
    }
    if let frame = content.frames.first(where: { $0.id == id }) {
        return effectiveFrameRect(frame, content)
    }
    return nil
}

extension CGRect {
    var center: CGPoint { CGPoint(x: midX, y: midY) }

    /// Where the segment center→target crosses this rect's border.
    func edgePoint(towards target: CGPoint) -> CGPoint {
        let c = center
        let dx = target.x - c.x
        let dy = target.y - c.y
        if dx == 0 && dy == 0 { return c }
        let scaleX = dx != 0 ? (width / 2) / abs(dx) : .greatestFiniteMagnitude
        let scaleY = dy != 0 ? (height / 2) / abs(dy) : .greatestFiniteMagnitude
        let t = min(min(scaleX, scaleY), 1)
        return CGPoint(x: c.x + dx * t, y: c.y + dy * t)
    }
}

// MARK: - Curves

/// Samples a Catmull-Rom spline through `points`, duplicating the ends so
/// the curve starts and finishes exactly on them.
func catmullRom(_ points: [CGPoint], samplesPerSegment: Int = 12) -> [CGPoint] {
    guard points.count > 2 else { return points }
    var pts = points
    pts.insert(points[0], at: 0)
    pts.append(points[points.count - 1])

    var out: [CGPoint] = []
    for i in 0..<(pts.count - 3) {
        let p0 = pts[i], p1 = pts[i + 1], p2 = pts[i + 2], p3 = pts[i + 3]
        for step in 0..<samplesPerSegment {
            let t = CGFloat(step) / CGFloat(samplesPerSegment)
            let t2 = t * t
            let t3 = t2 * t
            let x = 0.5 * ((2 * p1.x) + (-p0.x + p2.x) * t
                + (2 * p0.x - 5 * p1.x + 4 * p2.x - p3.x) * t2
                + (-p0.x + 3 * p1.x - 3 * p2.x + p3.x) * t3)
            let y = 0.5 * ((2 * p1.y) + (-p0.y + p2.y) * t
                + (2 * p0.y - 5 * p1.y + 4 * p2.y - p3.y) * t2
                + (-p0.y + 3 * p1.y - 3 * p2.y + p3.y) * t3)
            out.append(CGPoint(x: x, y: y))
        }
    }
    out.append(points[points.count - 1])
    return out
}

/// Sampled polyline of a connector, in world coordinates.
func connectorSamples(_ connector: ConnectorElement, _ content: NoteContent) -> [CGPoint]? {
    guard let rectA = anchorRect(connector.fromId, content),
          let rectB = anchorRect(connector.toId, content) else { return nil }

    if connector.nodes.isEmpty {
        let control = CGPoint(
            x: (rectA.midX + rectB.midX) / 2 + CGFloat(connector.curveDx),
            y: (rectA.midY + rectB.midY) / 2 + CGFloat(connector.curveDy)
        )
        let start = rectA.edgePoint(towards: control)
        let end = rectB.edgePoint(towards: control)
        return (0...32).map { i in
            let t = CGFloat(i) / 32
            let u = 1 - t
            return CGPoint(
                x: u * u * start.x + 2 * u * t * control.x + t * t * end.x,
                y: u * u * start.y + 2 * u * t * control.y + t * t * end.y
            )
        }
    }

    let nodes = connector.nodes.map { CGPoint(x: CGFloat($0.x), y: CGFloat($0.y)) }
    guard let first = nodes.first, let last = nodes.last else { return nil }
    let start = rectA.edgePoint(towards: first)
    let end = rectB.edgePoint(towards: last)
    return catmullRom([start] + nodes + [end])
}

private func path(through points: [CGPoint]) -> Path {
    var p = Path()
    guard let first = points.first else { return p }
    p.move(to: first)
    for point in points.dropFirst() { p.addLine(to: point) }
    return p
}

/// Dash pattern for a line style, mirroring the Android `dashIntervals`.
private func dashPattern(_ style: LineStyle, width: CGFloat, animated: Bool) -> [CGFloat] {
    switch style {
    case .SOLID: return animated ? [width * 6, width * 3] : []
    case .DASHED: return [width * 4.5, width * 3.5]
    case .DOTTED: return [0.1, width * 3]
    }
}

private func strokeStyle(_ style: LineStyle, width: CGFloat, animated: Bool) -> StrokeStyle {
    let dash = dashPattern(style, width: width, animated: animated)
    return StrokeStyle(lineWidth: width, lineCap: .round, lineJoin: .round, dash: dash)
}

// MARK: - The canvas

/// Everything that is drawn rather than composed as views: background,
/// frames, connectors, tape and ink.
struct NoteCanvas: View {
    let content: NoteContent
    let theme: AppStyle
    @ObservedObject var camera: CanvasState
    /// Stroke being drawn right now, not yet committed to the note.
    var activeStroke: InkStroke?
    var selectedStrokeId: String?

    var body: some View {
        Canvas { context, size in
            drawBackground(&context, size: size)

            context.translateBy(x: camera.offset.width, y: camera.offset.height)
            context.scaleBy(x: camera.scale, y: camera.scale)

            for frame in content.frames { drawFrame(&context, frame) }
            for connector in content.connectors { drawConnector(&context, connector) }
            for frame in content.frames { drawFrameLabel(&context, frame) }
            for tape in content.tapes { drawTape(&context, tape) }
            for stroke in content.strokes { drawStroke(&context, stroke) }
            if let active = activeStroke { drawStroke(&context, active) }
        }
    }

    // MARK: Background

    private func drawBackground(_ context: inout GraphicsContext, size: CGSize) {
        context.fill(Path(CGRect(origin: .zero, size: size)), with: .color(theme.background))
        guard content.background != .BLANK else { return }

        let color = theme.outlineVariant
        // Keep the apparent density stable across zoom, as on Android.
        var spacing: CGFloat = 56 * camera.scale
        while spacing < 28 { spacing *= 2 }
        while spacing > 112 { spacing /= 2 }
        let startX = camera.offset.width.truncatingRemainder(dividingBy: spacing)
        let startY = camera.offset.height.truncatingRemainder(dividingBy: spacing)

        switch content.background {
        case .BLANK:
            return
        case .DOTS:
            var y = startY
            while y < size.height {
                var x = startX
                while x < size.width {
                    let dot = CGRect(x: x - 1.5, y: y - 1.5, width: 3, height: 3)
                    context.fill(Path(ellipseIn: dot), with: .color(color))
                    x += spacing
                }
                y += spacing
            }
        case .GRID, .LINES:
            var linePath = Path()
            if content.background == .GRID {
                var x = startX
                while x < size.width {
                    linePath.move(to: CGPoint(x: x, y: 0))
                    linePath.addLine(to: CGPoint(x: x, y: size.height))
                    x += spacing
                }
            }
            var y = startY
            while y < size.height {
                linePath.move(to: CGPoint(x: 0, y: y))
                linePath.addLine(to: CGPoint(x: size.width, y: y))
                y += spacing
            }
            context.stroke(linePath, with: .color(color), lineWidth: 1)
        case .PAPER:
            // Deterministic speckle anchored to world coordinates.
            let cell = spacing / 2
            var y = startY - cell
            while y < size.height + cell {
                var x = startX - cell
                while x < size.width + cell {
                    let seed = Int(x / max(cell, 1)) &* 92_821 &+ Int(y / max(cell, 1)) &* 31_337
                    if abs(seed % 100) < 55 {
                        let r = 0.8 + CGFloat(abs(seed % 14)) / 10
                        let dot = CGRect(x: x, y: y, width: r, height: r)
                        context.fill(Path(ellipseIn: dot), with: .color(color.opacity(0.35)))
                    }
                    x += cell
                }
                y += cell
            }
        case .SCANLINES:
            var linePath = Path()
            let gap: CGFloat = 7
            var y = camera.offset.height.truncatingRemainder(dividingBy: gap)
            while y < size.height {
                linePath.move(to: CGPoint(x: 0, y: y))
                linePath.addLine(to: CGPoint(x: size.width, y: y))
                y += gap
            }
            context.stroke(linePath, with: .color(color.opacity(0.35)), lineWidth: 1)
        }
    }

    // MARK: Frames

    private func framePath(_ frame: FrameElement, _ rect: CGRect) -> Path {
        switch frame.shape ?? theme.frameShape {
        case .RECT:
            return Path(rect)
        case .ELLIPSE:
            return Path(ellipseIn: rect)
        case .ROUNDED, .SKETCHY:
            return Path(roundedRect: rect, cornerRadius: 28)
        }
    }

    private func drawFrame(_ context: inout GraphicsContext, _ frame: FrameElement) {
        let rect = effectiveFrameRect(frame, content)
        let shape = framePath(frame, rect)
        let color = resolvedColor(frame.color, theme)

        // A skin panel replaces the outline entirely, as on Android.
        if let decor = resolvedFrameDecor(frame, theme) {
            drawDecorPanel(&context, decor, rect)
            return
        }
        if frame.filled {
            context.fill(shape, with: .color(color.opacity(0.08)))
        }
        context.stroke(
            shape,
            with: .color(color),
            style: strokeStyle(
                frame.lineStyle ?? theme.frameLineStyle,
                width: CGFloat(frame.strokeWidth),
                animated: frame.animated
            )
        )
    }

    /// Flat interpretation of each theme skin — the spirit of the Android
    /// decor without reproducing its textures pixel for pixel.
    private func drawDecorPanel(_ context: inout GraphicsContext, _ decor: String, _ rect: CGRect) {
        switch decor {
        case "parchment":
            let panel = Path(roundedRect: rect, cornerRadius: 10)
            context.fill(panel, with: .color(theme.surface))
            context.stroke(panel, with: .color(theme.outline.opacity(0.55)), lineWidth: 1.5)
        case "window":
            let panel = Path(roundedRect: rect, cornerRadius: 8)
            context.fill(panel, with: .color(theme.surface))
            let bar = CGRect(x: rect.minX, y: rect.minY, width: rect.width, height: 26)
            context.fill(Path(bar), with: .color(theme.primary.opacity(0.22)))
            context.stroke(panel, with: .color(theme.outline), lineWidth: 1.5)
        case "terminal":
            let panel = Path(roundedRect: rect, cornerRadius: 6)
            context.fill(panel, with: .color(theme.background.opacity(0.92)))
            context.stroke(panel, with: .color(theme.primary.opacity(0.8)), lineWidth: 1.5)
        case "sketch":
            let panel = Path(roundedRect: rect, cornerRadius: 14)
            context.fill(panel, with: .color(theme.surface.opacity(0.9)))
            context.stroke(
                panel, with: .color(theme.onSurface.opacity(0.7)),
                style: StrokeStyle(lineWidth: 2, dash: [9, 5])
            )
        default: // "glass"
            let panel = Path(roundedRect: rect, cornerRadius: 20)
            context.fill(panel, with: .color(theme.surface.opacity(0.75)))
            context.stroke(panel, with: .color(theme.outlineVariant), lineWidth: 1)
        }
    }

    private func drawFrameLabel(_ context: inout GraphicsContext, _ frame: FrameElement) {
        guard !frame.label.isEmpty else { return }
        let rect = effectiveFrameRect(frame, content)
        context.draw(
            Text(frame.label).font(.system(size: 13, weight: .semibold))
                .foregroundColor(resolvedColor(frame.color, theme)),
            at: CGPoint(x: rect.minX + 4, y: rect.minY - 12),
            anchor: .bottomLeading
        )
    }

    // MARK: Connectors

    private func drawConnector(_ context: inout GraphicsContext, _ connector: ConnectorElement) {
        guard let samples = connectorSamples(connector, content), samples.count > 1 else { return }
        let color = resolvedColor(connector.color, theme)
        let width = CGFloat(connector.width)

        context.stroke(
            path(through: samples),
            with: .color(color),
            style: strokeStyle(
                connector.lineStyle ?? theme.connectorLineStyle,
                width: width,
                animated: connector.animated
            )
        )

        let headLength = max(width * 4.5, 14)
        drawCap(
            &context, connector.startCap, tip: samples[0],
            from: pointAlong(samples, fromEnd: false, distance: headLength),
            color: color, width: width
        )
        drawCap(
            &context, connector.endCap, tip: samples[samples.count - 1],
            from: pointAlong(samples, fromEnd: true, distance: headLength),
            color: color, width: width
        )
    }

    /// Point at `distance` along the polyline from either end — a stable
    /// tangent reference even on tight multi-node curves.
    private func pointAlong(_ samples: [CGPoint], fromEnd: Bool, distance: CGFloat) -> CGPoint {
        let pts = fromEnd ? samples.reversed().map { $0 } : samples
        var remaining = distance
        for i in 0..<(pts.count - 1) {
            let seg = hypot(pts[i + 1].x - pts[i].x, pts[i + 1].y - pts[i].y)
            if seg >= remaining && seg > 0 {
                let t = remaining / seg
                return CGPoint(
                    x: pts[i].x + (pts[i + 1].x - pts[i].x) * t,
                    y: pts[i].y + (pts[i + 1].y - pts[i].y) * t
                )
            }
            remaining -= seg
        }
        return pts[pts.count - 1]
    }

    private func drawCap(
        _ context: inout GraphicsContext, _ cap: CapStyle,
        tip: CGPoint, from: CGPoint, color: Color, width: CGFloat
    ) {
        switch cap {
        case .NONE:
            return
        case .DOT:
            let r = max(width * 1.8, 6)
            let dot = CGRect(x: tip.x - r, y: tip.y - r, width: r * 2, height: r * 2)
            context.fill(Path(ellipseIn: dot), with: .color(color))
        case .ARROW:
            let angle = atan2(tip.y - from.y, tip.x - from.x)
            let length = max(width * 4.5, 14)
            let spread: CGFloat = 0.46
            var head = Path()
            head.move(to: tip)
            head.addLine(to: CGPoint(
                x: tip.x - length * cos(angle - spread),
                y: tip.y - length * sin(angle - spread)
            ))
            head.addLine(to: CGPoint(
                x: tip.x - length * cos(angle + spread),
                y: tip.y - length * sin(angle + spread)
            ))
            head.closeSubpath()
            context.fill(head, with: .color(color))
        }
    }

    // MARK: Tape

    private func drawTape(_ context: inout GraphicsContext, _ tape: TapeElement) {
        let start = CGPoint(x: CGFloat(tape.x1), y: CGFloat(tape.y1))
        let end = CGPoint(x: CGFloat(tape.x2), y: CGFloat(tape.y2))
        let dx = end.x - start.x
        let dy = end.y - start.y
        let length = hypot(dx, dy)
        guard length > 0 else { return }

        let angle = atan2(dy, dx)
        let thickness = CGFloat(tape.thickness)
        let color = Color(argb: resolveRole(tape.color, theme.tapeColors))
        let strip = CGRect(x: 0, y: -thickness / 2, width: length, height: thickness)

        context.drawLayer { layer in
            layer.translateBy(x: start.x, y: start.y)
            layer.rotate(by: .radians(Double(angle)))
            layer.opacity = Double(tape.alpha)
            layer.fill(Path(strip), with: .color(color))

            var pattern = Path()
            switch tape.pattern ?? theme.tapePattern {
            case .SOLID:
                break
            case .STRIPES:
                var x: CGFloat = 0
                while x < length {
                    pattern.move(to: CGPoint(x: x, y: -thickness / 2))
                    pattern.addLine(to: CGPoint(x: x + thickness * 0.4, y: thickness / 2))
                    x += thickness * 0.55
                }
            case .DOTS:
                var x: CGFloat = thickness * 0.3
                while x < length {
                    let r = thickness * 0.14
                    let dot = CGRect(x: x - r, y: -r, width: r * 2, height: r * 2)
                    pattern.addEllipse(in: dot)
                    x += thickness * 0.5
                }
            case .ZIGZAG:
                var x: CGFloat = 0
                var up = true
                pattern.move(to: CGPoint(x: 0, y: 0))
                while x < length {
                    x += thickness * 0.45
                    pattern.addLine(to: CGPoint(x: x, y: up ? -thickness * 0.28 : thickness * 0.28))
                    up.toggle()
                }
            case .GRID:
                var x: CGFloat = 0
                while x < length {
                    pattern.move(to: CGPoint(x: x, y: -thickness / 2))
                    pattern.addLine(to: CGPoint(x: x, y: thickness / 2))
                    x += thickness * 0.5
                }
                pattern.move(to: CGPoint(x: 0, y: 0))
                pattern.addLine(to: CGPoint(x: length, y: 0))
            }
            if tape.pattern ?? theme.tapePattern == .DOTS {
                layer.fill(pattern, with: .color(.white.opacity(0.45)))
            } else {
                layer.stroke(pattern, with: .color(.white.opacity(0.4)), lineWidth: thickness * 0.12)
            }
        }
    }

    // MARK: Ink

    private func drawStroke(_ context: inout GraphicsContext, _ stroke: InkStroke) {
        let pts = stroke.points.map { CGPoint(x: CGFloat($0.x), y: CGFloat($0.y)) }
        guard !pts.isEmpty else { return }
        let color = resolvedInkColor(stroke, theme)
        let width = CGFloat(stroke.width)

        if pts.count == 1 {
            let r = width / 2
            let dot = CGRect(x: pts[0].x - r, y: pts[0].y - r, width: width, height: width)
            context.fill(Path(ellipseIn: dot), with: .color(color))
            return
        }

        let smooth = catmullRom(pts, samplesPerSegment: 6)
        let line = path(through: smooth)

        if stroke.id == selectedStrokeId {
            context.stroke(
                line, with: .color(color.opacity(0.28)),
                style: StrokeStyle(lineWidth: width + 14, lineCap: .round, lineJoin: .round)
            )
        }

        let style = stroke.lineStyle ?? .SOLID
        let isHighlighter = stroke.type == .HIGHLIGHTER
        context.stroke(
            line,
            with: .color(color),
            style: StrokeStyle(
                lineWidth: width,
                lineCap: isHighlighter ? .square : .round,
                lineJoin: .round,
                dash: dashPattern(style, width: width, animated: stroke.animated)
            )
        )
    }
}

/// Distance from `point` to the polyline, used for stroke hit-testing and
/// the eraser (segment distance, not just to the sampled points).
func distanceToPolyline(_ point: CGPoint, _ points: [StrokePoint]) -> CGFloat {
    guard !points.isEmpty else { return .greatestFiniteMagnitude }
    if points.count == 1 {
        return hypot(CGFloat(points[0].x) - point.x, CGFloat(points[0].y) - point.y)
    }
    var best = CGFloat.greatestFiniteMagnitude
    for i in 1..<points.count {
        let a = CGPoint(x: CGFloat(points[i - 1].x), y: CGFloat(points[i - 1].y))
        let b = CGPoint(x: CGFloat(points[i].x), y: CGFloat(points[i].y))
        let dx = b.x - a.x
        let dy = b.y - a.y
        let lenSq = dx * dx + dy * dy
        let t = lenSq == 0 ? 0 : max(0, min(1, ((point.x - a.x) * dx + (point.y - a.y) * dy) / lenSq))
        best = min(best, hypot(point.x - (a.x + t * dx), point.y - (a.y + t * dy)))
    }
    return best
}
