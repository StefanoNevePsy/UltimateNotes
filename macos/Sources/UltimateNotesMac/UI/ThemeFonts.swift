// ThemeFonts.swift
// UltimateNotesMac
//
// The Android app ships its own font files (Lora, Cinzel, VT323, Patrick
// Hand…). Rather than duplicating those binaries in the .app bundle, each
// theme maps to the closest macOS system face, so a note keeps the
// character its theme intends without a second copy of every font.

import SwiftUI

/// Which face a run of text should use.
enum FontRole {
    case display
    case body
}

/// Font family names available on stock macOS, per theme.
private struct FontPairing {
    var display: String?
    var body: String?
    /// Fallback design when no named family fits.
    var displayDesign: Font.Design = .default
    var bodyDesign: Font.Design = .default
}

private let pairings: [String: FontPairing] = [
    // Book-ish themes: serif titles over a readable serif body.
    "seppia": FontPairing(display: "Palatino", body: "Palatino"),
    "sepia": FontPairing(display: "Palatino", body: "Palatino"),
    "fantasy": FontPairing(display: "Papyrus", body: "Palatino"),
    // Terminal and retro: monospaced throughout.
    "terminal": FontPairing(display: "Menlo", body: "Menlo"),
    "vaporwave": FontPairing(display: "Menlo", body: nil, bodyDesign: .rounded),
    // Handwriting-flavoured themes.
    "quaderno": FontPairing(display: "Bradley Hand", body: "Bradley Hand"),
    "sketch": FontPairing(display: "Bradley Hand", body: "Bradley Hand"),
    // Clean modern themes stay on the system face.
    "latte": FontPairing(display: "New York", body: nil, bodyDesign: .rounded),
    "nordic": FontPairing(display: "New York", body: nil),
    "notte": FontPairing(display: "New York", body: nil),
    "dark": FontPairing(display: "New York", body: nil),
]

/// Font for a block of note text under the given theme.
///
/// An explicit per-block `fontId` wins; otherwise heading styles take the
/// theme's display face and everything else its body face — the same
/// precedence the Android renderer uses.
func themeFont(
    _ theme: AppStyle,
    role: FontRole,
    size: CGFloat,
    weight: Font.Weight,
    fontId: String? = nil
) -> Font {
    // Blank and "default" mean "follow the theme", never a real font.
    if let id = fontId, !id.isEmpty, id != "default",
       let named = fontName(forElementFontId: id) {
        return .custom(named, size: size).weight(weight)
    }

    let pairing = pairings[theme.id] ?? FontPairing()
    let family = role == .display ? pairing.display : pairing.body
    if let family, NSFont(name: family, size: size) != nil {
        return .custom(family, size: size).weight(weight)
    }
    let design = role == .display ? pairing.displayDesign : pairing.bodyDesign
    return .system(size: size, weight: weight, design: design)
}

/// Maps the Android `FontManager` ids that have a stock macOS equivalent.
private func fontName(forElementFontId id: String) -> String? {
    switch id {
    case "lora", "oldbook": return "Palatino"
    case "cinzel", "medieval": return "Papyrus"
    case "caveat", "patrickhand": return "Bradley Hand"
    case "typewriter": return "American Typewriter"
    case "vt323", "mono": return "Menlo"
    case "serif": return "Times New Roman"
    case "cursive": return "Snell Roundhand"
    case "system": return "Helvetica Neue"
    default: return nil // imported .ttf files stay Android-only for now
    }
}
