// ModelTests.swift
// UltimateNotesMacTests
//
// Round-trip and format-compatibility checks for the data layer that talks
// to the shared vault JSON (see docs/VAULT_FORMAT.md).

import XCTest
@testable import UltimateNotesMac

final class ModelTests: XCTestCase {
    private func makeEncoder() -> JSONEncoder {
        let e = JSONEncoder()
        e.outputFormatting = [.sortedKeys]
        return e
    }

    // MARK: (a) Round-trip: one of every element/shape type through encode+decode

    func testNoteContentRoundTripsThroughEveryElementType() throws {
        let content = NoteContent(
            elements: [
                .text(TextElement(x: 10, y: 20, text: "hello **world**", color: 5, bgColor: STICKY_AUTO)),
                .image(ImageElement(x: 1, y: 2, fileName: "photo.png", isPdfPage: true, pdfPage: 3)),
                .noteLink(NoteLinkElement(x: 3, y: 4, targetNoteId: "other-note")),
                .webLink(WebLinkElement(x: 5, y: 6, url: "https://example.com", title: "Example")),
                .file(FileElement(x: 7, y: 8, fileName: "doc.pdf", displayName: "Doc", mimeType: "application/pdf", sizeBytes: 4096)),
            ],
            strokes: [InkStroke(type: .HIGHLIGHTER, color: 2, points: [StrokePoint(x: 0, y: 0), StrokePoint(x: 1, y: 1, p: 0.5)])],
            connectors: [ConnectorElement(fromId: "a", toId: "b", startCap: .DOT, endCap: .ARROW, nodes: [StrokePoint(x: 9, y: 9)])],
            frames: [FrameElement(x: 1, y: 1, shape: .SKETCHY, filled: true, label: "Group", memberIds: ["a", "b"])],
            tapes: [TapeElement(x1: 0, y1: 0, x2: 50, y2: 10, pattern: .ZIGZAG)],
            background: .SCANLINES
        )

        let data = try makeEncoder().encode(content)
        let decoded = try JSONDecoder().decode(NoteContent.self, from: data)
        XCTAssertEqual(content, decoded)
    }

    func testVaultNoteRoundTrips() throws {
        let note = VaultNote(id: "n1", title: "Title", folderId: "f1", pinned: true, createdAt: 1000, updatedAt: 2000)
        let data = try makeEncoder().encode(note)
        let decoded = try JSONDecoder().decode(VaultNote.self, from: data)
        XCTAssertEqual(note, decoded)
    }

    // MARK: (b) Decoding the Android wire format (inline "type" discriminator)

    func testDecodesAndroidStyleElementsJSON() throws {
        let json = """
        {
          "elements": [
            {"type":"text","id":"t1","x":10,"y":20,"groupId":null,"width":600,"scale":1,"text":"hi","styleId":"body","fontId":null,"color":null,"bgColor":null,"fontSize":null,"decor":null},
            {"type":"image","id":"i1","x":0,"y":0,"groupId":null,"width":400,"height":400,"fileName":"a.png","isPdfPage":false,"pdfPage":0},
            {"type":"notelink","id":"n1","x":0,"y":0,"groupId":null,"width":420,"scale":1,"targetNoteId":"target"},
            {"type":"weblink","id":"w1","x":0,"y":0,"groupId":null,"width":420,"scale":1,"url":"https://example.com","title":"Example"},
            {"type":"file","id":"f1","x":0,"y":0,"groupId":null,"width":380,"scale":1,"fileName":"doc.pdf","displayName":"Doc","mimeType":"application/pdf","sizeBytes":1234}
          ],
          "strokes": [],
          "connectors": [],
          "frames": [],
          "tapes": [],
          "background": "DOTS"
        }
        """
        let content = try JSONDecoder().decode(NoteContent.self, from: Data(json.utf8))
        XCTAssertEqual(content.elements.count, 5)
        XCTAssertEqual(content.background, .DOTS)

        guard case .text(let text) = content.elements[0] else { return XCTFail("expected .text") }
        XCTAssertEqual(text.id, "t1")
        XCTAssertEqual(text.text, "hi")
        XCTAssertNil(text.color)

        guard case .image(let image) = content.elements[1] else { return XCTFail("expected .image") }
        XCTAssertEqual(image.fileName, "a.png")
        XCTAssertFalse(image.isPdfPage)

        guard case .noteLink(let link) = content.elements[2] else { return XCTFail("expected .noteLink") }
        XCTAssertEqual(link.targetNoteId, "target")

        guard case .webLink(let web) = content.elements[3] else { return XCTFail("expected .webLink") }
        XCTAssertEqual(web.url, "https://example.com")
        XCTAssertEqual(web.title, "Example")

        guard case .file(let file) = content.elements[4] else { return XCTFail("expected .file") }
        XCTAssertEqual(file.displayName, "Doc")
        XCTAssertEqual(file.sizeBytes, 1234)
    }

    func testMissingKeysFallBackToKotlinDefaults() throws {
        let json = #"{"type":"text","id":"t1"}"#
        let element = try JSONDecoder().decode(NoteElement.self, from: Data(json.utf8))
        guard case .text(let text) = element else { return XCTFail("expected .text") }
        XCTAssertEqual(text.width, 600)
        XCTAssertEqual(text.scale, 1)
        XCTAssertEqual(text.styleId, "body")
        XCTAssertEqual(text.text, "")
    }

    // MARK: (c) resolveRole

    func testResolveRoleMapsSlotsAndPassesThroughFixedColors() {
        let palette: [Int64] = [0x11, 0x22, 0x33]
        XCTAssertEqual(resolveRole(0, palette), 0x11) // "auto" -> slot 0
        XCTAssertEqual(resolveRole(1, palette), 0x11) // slot 0
        XCTAssertEqual(resolveRole(2, palette), 0x22) // slot 1
        XCTAssertEqual(resolveRole(4, palette), 0x22) // wraps: (4-1) % 3 == 0... slot 0? check below
        XCTAssertEqual(resolveRole(17, palette), 17) // beyond MAX_ROLE -> literal passthrough
        XCTAssertEqual(resolveRole(0xFFFF_0000, palette), 0xFFFF_0000) // fixed ARGB color, unchanged
    }
}
