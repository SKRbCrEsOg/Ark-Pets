#!/usr/bin/env bash
#
# Create GitHub releases (with assets) on the SKRbCrEsOg fork.
# Requires a Personal Access Token with "repo" (classic) or "Contents: Read and write"
# (fine-grained) permission. SSH alone is not enough for release creation.
#
# Usage:
#   export GH_TOKEN=<your_pat>
#   ./create-release.sh [arkpets|integration|all]     # default: all
#
# Env overrides:
#   ASSETS_DIR        Directory containing the release assets (default: this script's dir)
#   ARKPETS_TAG       Tag for the Ark-Pets release       (default: v3.13.1-linux-aarch64-r3)
#   INTEGRATION_TAG   Tag for the Ark-Pets-Integration release (default: v2-aarch64)
#
# Tags must already exist and be pushed (e.g. via SSH: git push <remote> <tag>).
#
set -euo pipefail

: "${GH_TOKEN:?Please export GH_TOKEN first (a GitHub Personal Access Token).}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ASSETS_DIR="${ASSETS_DIR:-$SCRIPT_DIR}"
ARKPETS_TAG="${ARKPETS_TAG:-v3.13.1-linux-aarch64-r3}"
INTEGRATION_TAG="${INTEGRATION_TAG:-v2-aarch64}"
OWNER="SKRbCrEsOg"
API="https://api.github.com"
UPLOADS="https://uploads.github.com"

create_release() {
    local repo="$1" tag="$2" name="$3" body="$4"; shift 4
    local payload id resp
    payload="$(python3 - "$tag" "$name" "$body" <<'PY'
import json, sys
print(json.dumps({"tag_name": sys.argv[1], "name": sys.argv[2], "body": sys.argv[3],
                  "draft": False, "prerelease": True}))
PY
)"
    resp="$(curl -fsS -X POST \
        -H "Authorization: Bearer ${GH_TOKEN}" \
        -H "Accept: application/vnd.github+json" \
        "${API}/repos/${OWNER}/${repo}/releases" \
        -d "$payload")" || { echo "Failed to create release for ${repo}"; return 1; }
    id="$(printf '%s' "$resp" | python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])')"
    local f
    for f in "$@"; do
        echo "  uploading $(basename "$f")"
        curl -fsS -X POST \
            -H "Authorization: Bearer ${GH_TOKEN}" \
            -H "Content-Type: application/octet-stream" \
            --data-binary @"$f" \
            "${UPLOADS}/repos/${OWNER}/${repo}/releases/${id}/assets?name=$(basename "$f")" \
            >/dev/null
    done
    echo "Release: https://github.com/${OWNER}/${repo}/releases/tag/${tag}"
}

which="${1:-all}"

if [[ "$which" == "arkpets" || "$which" == "all" ]]; then
    echo "== Ark-Pets =="
    create_release "Ark-Pets" "${ARKPETS_TAG}" \
        "ArkPets 3.13.1 (Linux aarch64 / KDE Plasma, experimental)" \
        "Experimental Linux support, verified on Debian 13 (aarch64) + KDE Plasma 6 (Wayland) with a custom (anland) KWin.

Highlights:
- Native Wayland + KWin integration (window list, move/resize, keep-above, taskbar)
- HiDPI (scale 2) aware physics/window coordinates
- Borderless frameless pet via a KWin window rule
- Mouse passthrough (transparent mode) via GLFW input region
- Tray/right-click popup robust on Wayland

Assets:
- ArkPets-v3.13.1.zip : self-contained app-image (bundled JRE, no system Java needed)
- ArkPets-v3.13.1.jar : fat jar (requires system JDK 21)
- install-arkpets.sh  : installer (app + KWin plugin + launcher wrappers + optional autostart)

Install: see the README (Linux aarch64 + KDE section).
Note: the KWin integration plugin (ArkPetsIntegration2.so) is published in the Ark-Pets-Integration release." \
        "${ASSETS_DIR}/ArkPets-v3.13.1.zip" \
        "${ASSETS_DIR}/ArkPets-v3.13.1.jar" \
        "${ASSETS_DIR}/install-arkpets.sh"
fi

if [[ "$which" == "integration" || "$which" == "all" ]]; then
    echo "== Ark-Pets-Integration =="
    create_release "Ark-Pets-Integration" "${INTEGRATION_TAG}" \
        "ArkPetsIntegration2 (KWin plugin, aarch64)" \
        "KWin integration plugin (ArkPetsIntegration2) for ArkPets on Linux aarch64.
Built against Debian 13 / KWin 6.3.6. Provides window query/control over D-Bus and a NoBorder method.

Install:
  sudo cp ArkPetsIntegration2.so \"\$(qtpaths6 --query QT_INSTALL_PLUGINS)/kwin/plugins/\"
then log out/in (or reboot) once so KWin loads it." \
        "${ASSETS_DIR}/ArkPetsIntegration2.so"
fi
