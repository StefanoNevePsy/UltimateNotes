// MarkdownText.swift
// UltimateNotesMac
//
// Renders a text block's markdown the way the Android editor does:
// headings, bold/italic/strike/code, list bullets with a hanging indent,
// checkboxes, plus the inline {c:…} / {f:…} / {s:…} styling tags.

import SwiftUI

/// Paragraph styles, mirroring `StyleSet.DEFAULTS` on Android.
private let paragraphSizes: [String: CGFloat] = [
    "title1": 32, "title2": 26, "title3": 21, "body": 16, "caption": 13,
]

private let paragraphWeights: [String: Font.Weight] = [
    "title1": .bold, "title2": .bold, "title3": .semibold, "body": .regular, "caption": .regular,
]

/// Base size of a block: an explicit override, else its paragraph style.
func baseFontSize(_ element: TextElement) -> CGFloat {
    if let size = element.fontSize { return CGFloat(size) }
    return paragraphSizes[element.styleId] ?? 16
}

private func baseWeight(_ element: TextElement) -> Font.Weight {
    paragraphWeights[element.styleId] ?? .regular
}

/// One line of a block, already stripped of its markers.
struct MarkdownLine: Identifiable {
    let id = UUID()
    var text: AttributedString
    /// Bullet or number shown in the hanging-indent gutter, if any.
    var marker: String?
    var indent: CGFloat
    var fontSize: CGFloat
    var weight: Font.Weight
}

private let inlineTagPattern = try? NSRegularExpression(
    pattern: "\\{(c|f|s):([^{}]*)\\}(.*?)\\{/\\1\\}",
    options: [.dotMatchesLineSeparators]
)

/// Resolves `{c:#RRGGBB}` and `{c:@n}` (theme accent slot n) to a color.
private func parseColorToken(_ token: String, _ theme: AppStyle) -> Color? {
    if token.hasPrefix("@"), let slot = Int64(token.dropFirst()) {
        return Color(argb: resolveRole(slot, theme.elementColors))
    }
    var hex = token
    if hex.hasPrefix("#") { hex.removeFirst() }
    guard let value = UInt64(hex, radix: 16) else { return nil }
    // 6 digits mean opaque; 8 already carry alpha.
    let argb = hex.count <= 6 ? Int64(value | 0xFF00_0000) : Int64(bitPattern: value)
    return Color(argb: argb)
}

/// Applies the inline tags of one line and returns styled text.
private func applyInlineTags(
    _ raw: String, base: CGFloat, weight: Font.Weight, color: Color, theme: AppStyle
) -> AttributedString {
    var working = raw
    var result = AttributedString()

    // Peel the outermost tag repeatedly; anything left is plain text.
    while let regex = inlineTagPattern,
          let match = regex.firstMatch(
              in: working, range: NSRange(working.startIndex..., in: working)
          ),
          let fullRange = Range(match.range, in: working),
          let kindRange = Range(match.range(at: 1), in: working),
          let valueRange = Range(match.range(at: 2), in: working),
          let innerRange = Range(match.range(at: 3), in: working) {

        let before = String(working[working.startIndex..<fullRange.lowerBound])
        result.append(styledRun(before, size: base, weight: weight, color: color))

        let kind = String(working[kindRange])
        let value = String(working[valueRange])
        let inner = String(working[innerRange])

        var runColor = color
        var runSize = base
        if kind == "c", let parsed = parseColorToken(value, theme) { runColor = parsed }
        if kind == "s", let parsed = Float(value) { runSize = CGFloat(parsed) }
        // {f:…} would need the font catalogue; the run keeps the block font.
        result.append(styledRun(inner, size: runSize, weight: weight, color: runColor))

        working = String(working[fullRange.upperBound...])
    }
    result.append(styledRun(working, size: base, weight: weight, color: color))
    return result
}

/// Handles **bold**, *italic*, ~~strike~~ and `code` inside a plain run.
private func styledRun(
    _ text: String, size: CGFloat, weight: Font.Weight, color: Color
) -> AttributedString {
    guard !text.isEmpty else { return AttributedString() }
    var attributed: AttributedString
    // Markdown parsing handles the emphasis markers; on failure show the
    // text verbatim rather than dropping the line.
    if let parsed = try? AttributedString(
        markdown: text,
        options: .init(interpretedSyntax: .inlineOnlyPreservingWhitespace)
    ) {
        attributed = parsed
    } else {
        attributed = AttributedString(text)
    }
    attributed.font = .system(size: size, weight: weight)
    attributed.foregroundColor = color
    return attributed
}

/// Splits a block into styled lines, resolving markers and inline tags.
func markdownLines(_ element: TextElement, theme: AppStyle) -> [MarkdownLine] {
    let color = element.color.map { Color(argb: resolveRole($0, theme.elementColors)) }
        ?? theme.onSurface
    let base = baseFontSize(element)
    let weight = baseWeight(element)

    return element.text.components(separatedBy: "\n").map { rawLine in
        var line = rawLine
        var marker: String?
        var indent: CGFloat = 0
        var size = base
        var lineWeight = weight

        // Leading spaces nest list items, as on Android.
        let trimmedLeading = line.drop { $0 == " " }
        indent = CGFloat((line.count - trimmedLeading.count) / 2) * 18
        line = String(trimmedLeading)

        if line.hasPrefix("### ") {
            line = String(line.dropFirst(4)); size = base * 1.3; lineWeight = .semibold
        } else if line.hasPrefix("## ") {
            line = String(line.dropFirst(3)); size = base * 1.6; lineWeight = .bold
        } else if line.hasPrefix("# ") {
            line = String(line.dropFirst(2)); size = base * 2; lineWeight = .bold
        } else if line.hasPrefix("> ") {
            line = String(line.dropFirst(2)); marker = "│"
        } else if line.hasPrefix("- [x] ") || line.hasPrefix("- [X] ") {
            line = String(line.dropFirst(6)); marker = "☑"
        } else if line.hasPrefix("- [ ] ") {
            line = String(line.dropFirst(6)); marker = "☐"
        } else if line.hasPrefix("- ") || line.hasPrefix("* ") || line.hasPrefix("+ ") {
            line = String(line.dropFirst(2)); marker = "•"
        } else if let range = line.range(of: "^\\d{1,3}[.)] ", options: .regularExpression) {
            marker = String(line[range]).trimmingCharacters(in: .whitespaces)
            line = String(line[range.upperBound...])
        }

        return MarkdownLine(
            text: applyInlineTags(line, base: size, weight: lineWeight, color: color, theme: theme),
            marker: marker,
            indent: indent,
            fontSize: size,
            weight: lineWeight
        )
    }
}

/// A text block as shown on the canvas, with its sticky background or skin.
struct MarkdownBlockView: View {
    let element: TextElement
    let theme: AppStyle

    var body: some View {
        VStack(alignment: .leading, spacing: 3) {
            ForEach(markdownLines(element, theme: theme)) { line in
                HStack(alignment: .firstTextBaseline, spacing: 6) {
                    if let marker = line.marker {
                        Text(marker)
                            .font(.system(size: line.fontSize, weight: line.weight))
                            .foregroundColor(theme.onSurfaceVariant)
                    }
                    Text(line.text)
                    Spacer(minLength: 0)
                }
                .padding(.leading, line.indent)
            }
        }
        .padding(padding)
        .frame(width: CGFloat(element.width), alignment: .leading)
        .background(background)
        .textSelection(.enabled)
    }

    private var padding: CGFloat {
        if element.decor != nil { return 14 }
        return element.bgColor != nil ? 10 : 4
    }

    @ViewBuilder private var background: some View {
        if let decor = element.decor {
            let skin = decor == "auto" ? theme.blockDecor : decor
            RoundedRectangle(cornerRadius: skin == "glass" ? 20 : 10)
                .fill(theme.surface.opacity(skin == "terminal" ? 0.92 : 0.8))
                .overlay(
                    RoundedRectangle(cornerRadius: skin == "glass" ? 20 : 10)
                        .stroke(theme.outlineVariant, lineWidth: 1)
                )
        } else if let bg = element.bgColor {
            // 1 = STICKY_AUTO: the theme picks the sticky color.
            let argb = bg == 1 ? theme.stickyColors.first ?? 0xFFFFF3B0 : bg
            RoundedRectangle(cornerRadius: 12).fill(Color(argb: argb))
        } else {
            Color.clear
        }
    }
}
