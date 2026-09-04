// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "UltimateNotesMac",
    platforms: [.macOS(.v13)],
    targets: [
        .executableTarget(
            name: "UltimateNotesMac",
            path: "Sources/UltimateNotesMac"
        ),
        .testTarget(
            name: "UltimateNotesMacTests",
            dependencies: ["UltimateNotesMac"],
            path: "Tests/UltimateNotesMacTests"
        ),
    ]
)
