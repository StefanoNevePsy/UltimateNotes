// Theme.swift
// UltimateNotesMac
//
// Mirrors the parts of app/.../ui/theme/AppThemes.kt and
// app/.../ui/editor/ThemedResolve.kt that the data layer needs: the palette
// (colors + default shapes/patterns/decor a theme applies to new canvas
// objects) and the "theme role" color resolution used by stored elements.
//
// Kotlin's AppStyle also carries UI-only concerns (fonts, motion springs,
// glass alpha, Compose ColorScheme...) that this app's views don't need from
// the data layer, so this port keeps only the fields the vault format and
// canvas rendering actually depend on.

import SwiftUI

/// A theme's palette plus the graphic defaults it applies to new canvas
/// objects (frame shape, line styles, tape pattern/colors, block decor...).
struct AppStyle: Identifiable {
    var id: String
    var name: String
    var dark: Bool

    var background: Color
    var surface: Color
    var onSurface: Color
    var onSurfaceVariant: Color
    var primary: Color
    var secondary: Color
    var tertiary: Color
    var outline: Color
    var outlineVariant: Color

    /// Element color slots (connectors, frames, text accents): picking a
    /// slot stores its index (see `resolveRole`), so the element re-colors
    /// when the theme changes.
    var elementColors: [Int64]
    /// Sticky-note background colors.
    var stickyColors: [Int64]
    /// Washi-tape colors.
    var tapeColors: [Int64]

    /// Stroke "texture" for connectors/frames (clean/ink/chalk/pixel/neon),
    /// independent of the dash pattern.
    var strokeFlavor: String
    /// Signature block decor for "auto" text-block / frame skins.
    var blockDecor: String

    var frameShape: FrameShape
    var frameLineStyle: LineStyle
    var connectorLineStyle: LineStyle
    var tapePattern: TapePattern
    var canvasBackground: CanvasBackground
}

/// Blends a color toward white for readable sticky/pastel tints — same
/// formula as Kotlin's `softTint` (0..255 channels, truncating, not rounding).
private func softTint(_ color: Int64) -> Int64 {
    let r = Int((color >> 16) & 0xFF)
    let g = Int((color >> 8) & 0xFF)
    let b = Int(color & 0xFF)
    func soften(_ v: Int) -> Int64 {
        Int64(min(max(Double(v) + (255 - Double(v)) * 0.65, 0), 255))
    }
    return 0xFF00_0000 | (soften(r) << 16) | (soften(g) << 8) | soften(b)
}

// MARK: - Themes
//
// Eight representative themes ported from AppThemes.kt, keeping their real
// base colors and their real strokeFlavor/blockDecor/frameShape/tapePattern
// (post the `styled()` per-family overrides in the Kotlin file). Where a
// Kotlin theme leaves `elementColors`/`stickyColors`/`tapeColors` null (so
// Compose derives them from its ColorScheme at render time), the derived
// values are computed here ahead of time from the same formula
// (`resolvedElementColors`/`resolvedStickyColors`/`resolvedTapeColors`).
// Themes that don't set a Material3 `tertiary` explicitly get Material3's
// own baseline default (0xFF7D5260 light / 0xFFEFB8C8 dark), which is what
// Compose's `lightColorScheme()`/`darkColorScheme()` fill in when the
// parameter is omitted.

private let m3TertiaryLight: Int64 = 0xFF7D_5260
private let m3TertiaryDark: Int64 = 0xFFEF_B8C8

let latteTheme = AppStyle(
    id: "latte", name: "Latte", dark: false,
    background: Color(argb: 0xFFFA_F7F2), surface: Color(argb: 0xFFFF_FFFF),
    onSurface: Color(argb: 0xFF2D_2A26), onSurfaceVariant: Color(argb: 0xFF5F_5A52),
    primary: Color(argb: 0xFF63_56E5), secondary: Color(argb: 0xFFE5_735A),
    tertiary: Color(argb: m3TertiaryLight),
    outline: Color(argb: 0xFF9A_938A), outlineVariant: Color(argb: 0xFFDC_D5C9),
    elementColors: [0xFF63_56E5, 0xFFB0_5AE5, 0xFFE5_735A, m3TertiaryLight, 0xFF5F_5A52, 0xFF9A_938A],
    stickyColors: [0xFFFF_F3A8, softTint(0xFF63_56E5), softTint(0xFFE5_735A), 0xFFD9_F2D9, 0xFFFC_E0EC],
    tapeColors: [0xFF63_56E5, 0xFFB0_5AE5, 0xFFF2_C879, 0xFF8F_BC8F, 0xFFEC_4899, 0xFF6B_7280],
    strokeFlavor: "clean", blockDecor: "glass",
    frameShape: .ROUNDED, frameLineStyle: .SOLID, connectorLineStyle: .SOLID,
    tapePattern: .STRIPES, canvasBackground: .DOTS
)

let notteTheme = AppStyle(
    id: "dark", name: "Notte", dark: true,
    background: Color(argb: 0xFF15_151E), surface: Color(argb: 0xFF1C_1C28),
    onSurface: Color(argb: 0xFFE6_E4EE), onSurfaceVariant: Color(argb: 0xFFAF_ACC0),
    primary: Color(argb: 0xFF8E_85FF), secondary: Color(argb: 0xFFFF_9E80),
    tertiary: Color(argb: m3TertiaryDark),
    outline: Color(argb: 0xFF6E_6B80), outlineVariant: Color(argb: 0xFF35_334A),
    elementColors: [0xFF8E_85FF, 0xFFFF_9E80, 0xFFFF_9E80, m3TertiaryDark, 0xFFAF_ACC0, 0xFF6E_6B80],
    stickyColors: [0xFFFF_F3A8, softTint(0xFF8E_85FF), softTint(0xFFFF_9E80), 0xFFD9_F2D9, 0xFFFC_E0EC],
    tapeColors: [0xFF8E_85FF, 0xFFFF_9E80, 0xFFF2_C879, 0xFF8F_BC8F, 0xFFEC_4899, 0xFF6B_7280],
    strokeFlavor: "clean", blockDecor: "glass",
    frameShape: .ROUNDED, frameLineStyle: .SOLID, connectorLineStyle: .SOLID,
    tapePattern: .STRIPES, canvasBackground: .DOTS
)

let seppiaTheme = AppStyle(
    id: "sepia", name: "Seppia", dark: false,
    background: Color(argb: 0xFFF4_EAD8), surface: Color(argb: 0xFFFB_F3E4),
    onSurface: Color(argb: 0xFF43_3726), onSurfaceVariant: Color(argb: 0xFF6E_5F45),
    primary: Color(argb: 0xFF8A_5A2B), secondary: Color(argb: 0xFFA8_552F),
    tertiary: Color(argb: m3TertiaryLight),
    outline: Color(argb: 0xFF8C_7B5D), outlineVariant: Color(argb: 0xFFD6_C8AB),
    // gradient = [secondary, primary] in Kotlin's SepiaTheme, kept verbatim.
    elementColors: [0xFFA8_552F, 0xFF8A_5A2B, 0xFFA8_552F, m3TertiaryLight, 0xFF6E_5F45, 0xFF8C_7B5D],
    stickyColors: [0xFFF2_E0B5, 0xFFE8_D3A0, 0xFFE0_C39B, 0xFFD9_C8AC, 0xFFF0_D8C8],
    tapeColors: [0xFFA8_552F, 0xFF8A_5A2B, 0xFFF2_C879, 0xFF8F_BC8F, 0xFFEC_4899, 0xFF6B_7280],
    strokeFlavor: "ink", blockDecor: "parchment",
    frameShape: .SKETCHY, frameLineStyle: .SOLID, connectorLineStyle: .SOLID,
    tapePattern: .STRIPES, canvasBackground: .PAPER
)

let terminalTheme = AppStyle(
    id: "terminal", name: "Terminal", dark: true,
    background: Color(argb: 0xFF06_0E08), surface: Color(argb: 0xFF0B_1810),
    onSurface: Color(argb: 0xFFB8_F5CC), onSurfaceVariant: Color(argb: 0xFF7F_BF96),
    primary: Color(argb: 0xFF00_FF66), secondary: Color(argb: 0xFF38_E8C2),
    tertiary: Color(argb: m3TertiaryDark),
    outline: Color(argb: 0xFF3E_7A55), outlineVariant: Color(argb: 0xFF1C_3826),
    elementColors: [0xFF00_FF66, 0xFF38_E8C2, 0xFF9C_FF57, 0xFFFF_BF00, 0xFF55_FFAA, 0xFF7F_BF96],
    stickyColors: [0xFF12_251A, 0xFF1C_3826, 0xFF26_402E, 0xFF14_3020, 0xFF0E_2418],
    tapeColors: [0xFF00_FF66, 0xFF38_E8C2, 0xFF9C_FF57, 0xFFFF_BF00, 0xFF2A_5C3F, 0xFF1C_3826],
    strokeFlavor: "neon", blockDecor: "terminal",
    frameShape: .RECT, frameLineStyle: .SOLID, connectorLineStyle: .SOLID,
    tapePattern: .GRID, canvasBackground: .SCANLINES
)

let quadernoTheme = AppStyle(
    id: "sketch", name: "Quaderno", dark: false,
    background: Color(argb: 0xFFFC_FAF4), surface: Color(argb: 0xFFFF_FFFC),
    onSurface: Color(argb: 0xFF26_303E), onSurfaceVariant: Color(argb: 0xFF56_5F6E),
    primary: Color(argb: 0xFF2C_4FD8), secondary: Color(argb: 0xFFD8_3A3A),
    tertiary: Color(argb: m3TertiaryLight),
    outline: Color(argb: 0xFF88_93A3), outlineVariant: Color(argb: 0xFFD9_DCE2),
    elementColors: [0xFF2C_4FD8, 0xFFD8_3A3A, 0xFFD8_3A3A, m3TertiaryLight, 0xFF56_5F6E, 0xFF88_93A3],
    stickyColors: [0xFFFF_F3A8, softTint(0xFF2C_4FD8), softTint(0xFFD8_3A3A), 0xFFD9_F2D9, 0xFFFC_E0EC],
    tapeColors: [0xFF2C_4FD8, 0xFFD8_3A3A, 0xFFF2_C879, 0xFF8F_BC8F, 0xFFEC_4899, 0xFF6B_7280],
    strokeFlavor: "ink", blockDecor: "sketch",
    frameShape: .SKETCHY, frameLineStyle: .SOLID, connectorLineStyle: .DASHED,
    tapePattern: .DOTS, canvasBackground: .LINES
)

let fantasyTheme = AppStyle(
    id: "fantasy", name: "Pergamena", dark: false,
    background: Color(argb: 0xFFF0_E2C4), surface: Color(argb: 0xFFF7_ECD4),
    onSurface: Color(argb: 0xFF3B_2A1A), onSurfaceVariant: Color(argb: 0xFF6C_5638),
    primary: Color(argb: 0xFF7A_1F1F), secondary: Color(argb: 0xFF9C_6F1E),
    tertiary: Color(argb: m3TertiaryLight),
    outline: Color(argb: 0xFF8D_744E), outlineVariant: Color(argb: 0xFFD2_BE96),
    elementColors: [0xFF7A_1F1F, 0xFF9C_6F1E, 0xFF3F_5C3A, 0xFF34_425E, 0xFF6B_4A2F, 0xFF55_2E5E],
    stickyColors: [0xFFEF_DFB9, 0xFFE6_CE9E, 0xFFD9_BC85, 0xFFE8_D5C0, 0xFFD7_C5A8],
    tapeColors: [0xFF7A_1F1F, 0xFF9C_6F1E, 0xFF3F_5C3A, 0xFF34_425E, 0xFF6B_4A2F, 0xFF8D_744E],
    strokeFlavor: "ink", blockDecor: "parchment",
    frameShape: .SKETCHY, frameLineStyle: .SOLID, connectorLineStyle: .DASHED,
    tapePattern: .SOLID, canvasBackground: .PAPER
)

let vaporwaveTheme = AppStyle(
    id: "vaporwave", name: "Vaporwave", dark: true,
    background: Color(argb: 0xFF1A_0B2E), surface: Color(argb: 0xFF24_1240),
    onSurface: Color(argb: 0xFFEF_E3FF), onSurfaceVariant: Color(argb: 0xFFC2_AEE0),
    primary: Color(argb: 0xFFFF_71CE), secondary: Color(argb: 0xFF01_CDFE),
    tertiary: Color(argb: 0xFF05_FFA1),
    outline: Color(argb: 0xFF7C_63A8), outlineVariant: Color(argb: 0xFF3E_2A63),
    elementColors: [0xFFFF_71CE, 0xFF01_CDFE, 0xFF05_FFA1, 0xFFB9_67FF, 0xFFFF_FB96, 0xFFFE_4164],
    stickyColors: [0xFFFF_F3A8, softTint(0xFFFF_71CE), softTint(0xFF01_CDFE), 0xFFD9_F2D9, 0xFFFC_E0EC],
    tapeColors: [0xFFFF_71CE, 0xFF01_CDFE, 0xFF05_FFA1, 0xFFB9_67FF, 0xFFFF_FB96, 0xFF7C_63A8],
    strokeFlavor: "neon", blockDecor: "glass",
    frameShape: .RECT, frameLineStyle: .SOLID, connectorLineStyle: .SOLID,
    tapePattern: .ZIGZAG, canvasBackground: .GRID
)

let nordicTheme = AppStyle(
    id: "nordic", name: "Nordic", dark: false,
    background: Color(argb: 0xFFEC_EFF4), surface: Color(argb: 0xFFF8_FAFC),
    onSurface: Color(argb: 0xFF2E_3440), onSurfaceVariant: Color(argb: 0xFF4C_566A),
    primary: Color(argb: 0xFF5E_81AC), secondary: Color(argb: 0xFF88_C0D0),
    tertiary: Color(argb: m3TertiaryLight),
    outline: Color(argb: 0xFF7B_88A1), outlineVariant: Color(argb: 0xFFCD_D6E4),
    elementColors: [0xFF5E_81AC, 0xFF88_C0D0, 0xFF88_C0D0, m3TertiaryLight, 0xFF4C_566A, 0xFF7B_88A1],
    stickyColors: [0xFFFF_F3A8, softTint(0xFF5E_81AC), softTint(0xFF88_C0D0), 0xFFD9_F2D9, 0xFFFC_E0EC],
    tapeColors: [0xFF5E_81AC, 0xFF88_C0D0, 0xFFF2_C879, 0xFF8F_BC8F, 0xFFEC_4899, 0xFF6B_7280],
    strokeFlavor: "clean", blockDecor: "glass",
    frameShape: .ROUNDED, frameLineStyle: .SOLID, connectorLineStyle: .SOLID,
    tapePattern: .STRIPES, canvasBackground: .DOTS
)

/// All ported themes, in the order they were introduced above.
let appThemes: [AppStyle] = [
    latteTheme, notteTheme, seppiaTheme, terminalTheme,
    quadernoTheme, fantasyTheme, vaporwaveTheme, nordicTheme,
]

/// Looks up a theme by id, falling back to the first theme (Latte) for an
/// unknown id — same fallback Kotlin's `themeById` uses.
func themeById(_ id: String) -> AppStyle {
    appThemes.first { $0.id == id } ?? appThemes[0]
}

// MARK: - Theme-role color resolution (mirrors ThemedResolve.kt)
//
// Tiny color values are impossible real ARGB colors (alpha byte 0), so they
// are reserved as palette-slot references:
//   0          -> "auto", slot 0 of the relevant palette
//   1...16     -> slot (value - 1)
//   > 16       -> fixed ARGB color chosen by the user
private let maxRole: Int64 = 16

private func isRole(_ value: Int64) -> Bool { value >= 0 && value <= maxRole }

/// Resolves a stored color value against `palette`: a role in 0...16 maps to
/// a palette slot (wrapping if the value exceeds the palette size); any
/// other value is returned unchanged as a fixed ARGB color.
func resolveRole(_ value: Int64, _ palette: [Int64]) -> Int64 {
    guard isRole(value), !palette.isEmpty else { return value }
    let index = Int(max(value - 1, 0)) % palette.count
    return palette[index]
}

extension Color {
    /// Builds a Color from a packed ARGB Int64 (0xAARRGGBB), matching
    /// Kotlin's Long ARGB color representation.
    init(argb: Int64) {
        let v = UInt32(truncatingIfNeeded: argb)
        let a = Double((v >> 24) & 0xFF) / 255
        let r = Double((v >> 16) & 0xFF) / 255
        let g = Double((v >> 8) & 0xFF) / 255
        let b = Double(v & 0xFF) / 255
        self.init(.sRGB, red: r, green: g, blue: b, opacity: a)
    }
}

/// Ink color: 0 = the theme's ink (onSurface, always readable on the canvas
/// background), 1...16 = an element accent slot, else a fixed user color.
func resolvedInkColor(_ stroke: InkStroke, _ theme: AppStyle) -> Color {
    if stroke.color == 0 {
        return theme.onSurface
    }
    if isRole(stroke.color) {
        return Color(argb: resolveRole(stroke.color, theme.elementColors))
    }
    return Color(argb: stroke.color)
}

/// Resolves a stored color value (connector/frame/text accent) against the
/// theme's element palette.
func resolvedColor(_ value: Int64, _ theme: AppStyle) -> Color {
    Color(argb: resolveRole(value, theme.elementColors))
}

/// Skin a frame actually shows: nil and "auto" both mean the theme's
/// signature decor, "none" means a plain outline, anything else is used as-is.
func resolvedFrameDecor(_ frame: FrameElement, _ theme: AppStyle) -> String? {
    guard let decor = frame.decor, decor != "auto" else { return theme.blockDecor }
    if decor == "none" { return nil }
    return decor
}
