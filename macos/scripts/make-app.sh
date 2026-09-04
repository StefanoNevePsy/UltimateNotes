#!/bin/bash
# Builds the SwiftPM executable and wraps it in a proper .app bundle.
# Must run on macOS (SwiftUI/AppKit are Apple-only).
set -euo pipefail

cd "$(dirname "$0")/.."
CONFIG="${1:-release}"
APP_NAME="Ultimate Notes"
BUNDLE="dist/${APP_NAME}.app"

echo "==> swift build -c ${CONFIG}"
swift build -c "${CONFIG}"

BIN="$(swift build -c "${CONFIG}" --show-bin-path)/UltimateNotesMac"
test -x "${BIN}" || { echo "binary not found at ${BIN}"; exit 1; }

echo "==> assembling ${BUNDLE}"
rm -rf "${BUNDLE}"
mkdir -p "${BUNDLE}/Contents/MacOS" "${BUNDLE}/Contents/Resources"
cp "${BIN}" "${BUNDLE}/Contents/MacOS/UltimateNotesMac"
cp Resources/Info.plist "${BUNDLE}/Contents/Info.plist"
printf 'APPL????' > "${BUNDLE}/Contents/PkgInfo"

# Ad-hoc signature: enough to launch locally without a developer account.
codesign --force --deep --sign - "${BUNDLE}" 2>/dev/null || \
  echo "note: ad-hoc codesign skipped"

echo "==> built ${BUNDLE}"
