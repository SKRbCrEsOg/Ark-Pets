#!/usr/bin/env bash
#
# ArkPets aarch64 + KDE (KWin / Wayland) installer
# Target: Debian 13 (trixie) aarch64, KDE Plasma 6 (Wayland), KWin integration plugin.
#
# Usage:
#   ./install-arkpets.sh [options]
#
# Options:
#   --prefix <dir>        Install root (default: $HOME/.local/opt)
#   --zip <path>          ArkPets app-image zip (default: newest ArkPets-*.zip next to this script)
#   --plugin <path>       KWin plugin .so   (default: ArkPetsIntegration2.so next to this script)
#   --no-plugin           Do not install the KWin integration plugin
#   --with-demo-model     Also download the small Amiya (002_amiya) model so the pet runs out of the box
#   --autostart           Create ~/.config/autostart entry (launch pet on login)
#   -h | --help           Show this help
#
# Notes:
#   * The app-image bundles its own JRE, so no system Java is required.
#   * After installing the plugin you must restart KWin (log out/in or reboot) once, so
#     that KWin scans the new plugin. Afterwards ArkPets loads it automatically.
#   * The KWin main program and its libraries are NOT modified.
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PREFIX="${HOME}/.local/opt"
ZIP=""
PLUGIN=""
DO_PLUGIN=1
DEMO_MODEL=0
AUTOSTART=0

while [[ $# -gt 0 ]]; do
    case "$1" in
        --prefix) PREFIX="$2"; shift 2 ;;
        --zip) ZIP="$2"; shift 2 ;;
        --plugin) PLUGIN="$2"; shift 2 ;;
        --no-plugin) DO_PLUGIN=0; shift ;;
        --with-demo-model) DEMO_MODEL=1; shift ;;
        --autostart) AUTOSTART=1; shift ;;
        -h|--help) sed -n '2,30p' "$0"; exit 0 ;;
        *) echo "Unknown option: $1" >&2; exit 2 ;;
    esac
done

log()  { printf '\033[1;34m[arkpets]\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[arkpets]\033[0m %s\n' "$*"; }

if [[ "$(uname -m)" != "aarch64" ]]; then
    warn "This bundle targets aarch64 but the host is $(uname -m). Continuing anyway."
fi

# Locate the app-image zip.
if [[ -z "$ZIP" ]]; then
    ZIP="$(ls -1t "$SCRIPT_DIR"/ArkPets-*.zip 2>/dev/null | head -1 || true)"
fi
[[ -n "$ZIP" && -f "$ZIP" ]] || { echo "ArkPets zip not found (use --zip <path>)." >&2; exit 1; }
APP_VER="$(basename "$ZIP" | sed -E 's/^ArkPets-v?([0-9][0-9.]*).*\.zip$/\1/')"
[[ -n "$APP_VER" ]] || APP_VER="0.0.0"

APP_ROOT="$PREFIX"
APP="$APP_ROOT/ArkPets"

log "Installing ArkPets from: $ZIP"
log "Install dir: $APP"
mkdir -p "$APP_ROOT"
rm -rf "$APP"
# Extract (the archive contains a top-level 'ArkPets/' directory).
unzip -oq "$ZIP" -d "$APP_ROOT"
[[ -x "$APP/bin/ArkPets" ]] || { echo "Unexpected archive layout: $APP/bin/ArkPets missing." >&2; exit 1; }

mkdir -p "$APP/models" "$APP/logs" "$APP/temp"

# Default config (only if absent).
CONFIG="$APP/ArkPetsConfig.json"
if [[ ! -f "$CONFIG" ]]; then
    log "Writing default config: $CONFIG"
    cat > "$CONFIG" <<'JSON'
{
    "behavior_ai_activation":4,
    "behavior_allow_interact":true,
    "behavior_allow_sit":true,
    "behavior_allow_sleep":false,
    "behavior_allow_special":true,
    "behavior_allow_walk":true,
    "behavior_direction_switching":1,
    "behavior_do_peer_repulsion":true,
    "behavior_walk_speed":30.0,
    "canvas_color":"#00000000",
    "canvas_coverage":0.8,
    "canvas_sampling_interval":4,
    "character_asset":"",
    "character_favorites":{},
    "character_files":{},
    "character_label":"",
    "display_fps":60,
    "display_margin_bottom":96,
    "display_multi_monitors":true,
    "display_scale":1.0,
    "download_mc_cdk":"",
    "eco_mode":false,
    "enable_telemetry":false,
    "initial_position_x":0.5,
    "initial_position_y":0.6,
    "launcher_solid_exit":true,
    "logging_level":"INFO",
    "opacity_dim":0.75,
    "opacity_normal":1.0,
    "physic_air_friction_acc":100.0,
    "physic_gravity_acc":800.0,
    "physic_speed_limit_x":1000.0,
    "physic_speed_limit_y":1000.0,
    "physic_static_friction_acc":500.0,
    "render_animation_mixture":0.3,
    "render_enable_mipmap":true,
    "render_outline":1,
    "render_outline_color":"#FFFF00FF",
    "render_outline_emphasis":3,
    "render_outline_emphasis_color":"#FFBB00FF",
    "render_outline_width":2.0,
    "render_shader_high_quality":true,
    "render_shadow_color":"#000000BB",
    "transition_duration":0.3,
    "transition_type":"EASE_OUT_CUBIC",
    "user_announcement_read":{},
    "window_style_toolwindow":true,
    "window_style_topmost":true,
    "window_system":"KWIN"
}
JSON
fi

# Optional demo model (Amiya 002_amiya, ~270 KB total).
if [[ "$DEMO_MODEL" -eq 1 ]]; then
    log "Downloading demo model (002_amiya)"
    BASE="https://raw.githubusercontent.com/isHarryh/Ark-Models/main/models/002_amiya"
    D="$APP/models/002_amiya"
    mkdir -p "$D"
    for f in build_char_002_amiya.atlas build_char_002_amiya.png build_char_002_amiya.skel; do
        curl -4 -fsSL "$BASE/$f" -o "$D/$f" && log "  got $f" || warn "  failed $f"
    done
    # Point the config at the demo model.
    python3 - "$CONFIG" <<'PY'
import json,sys
p=sys.argv[1]
c=json.load(open(p))
c["character_asset"]="models/002_amiya"
c["character_files"]={".atlas":"build_char_002_amiya.atlas",".png":"build_char_002_amiya.png",".skel":"build_char_002_amiya.skel"}
json.dump(c,open(p,"w"),ensure_ascii=False,indent=4)
PY
fi

# Install the KWin integration plugin.
if [[ "$DO_PLUGIN" -eq 1 ]]; then
    [[ -f "$PLUGIN" ]] || PLUGIN="$SCRIPT_DIR/ArkPetsIntegration2.so"
    if [[ -f "$PLUGIN" ]]; then
        QTPLUG="$(qtpaths6 --query QT_INSTALL_PLUGINS 2>/dev/null || echo "/usr/lib/$(gcc -dumpmachine)/qt6/plugins")"
        DEST="$QTPLUG/kwin/plugins"
        log "Installing KWin plugin -> $DEST/ArkPetsIntegration2.so (needs sudo)"
        sudo install -d -m755 "$DEST"
        sudo install -m644 "$PLUGIN" "$DEST/ArkPetsIntegration2.so"
        # Best-effort: ask the running KWin to load it (a KWin restart is recommended regardless).
        if command -v qdbus6 >/dev/null 2>&1; then
            qdbus6 org.kde.KWin /Plugins org.kde.KWin.Plugins.LoadPlugin ArkPetsIntegration2 >/dev/null 2>&1 \
                && log "Requested KWin to load ArkPetsIntegration2" \
                || warn "Could not load the plugin at runtime; restart KWin (log out/in) once."
        fi
    else
        warn "Plugin .so not found; skipping (use --plugin <path>)."
    fi
fi

# A KWin window rule that removes the titlebar/frame at window creation is the
# most reliable way to get a borderless pet on KWin/Wayland (runtime removal can
# leave the theme's translucent window background behind). Non-destructive: only
# added when the user has no rules yet.
if command -v kwriteconfig6 >/dev/null 2>&1; then
    EXISTING_COUNT="$(kreadconfig6 --file kwinrulesrc --group General --key count 2>/dev/null || echo 0)"
    : "${EXISTING_COUNT:=0}"
    if [[ "$EXISTING_COUNT" == "0" ]]; then
        log "Adding KWin window rule: no titlebar/frame for ArkPets"
        kwriteconfig6 --file kwinrulesrc --group 1 --key Description "ArkPets pet"
        kwriteconfig6 --file kwinrulesrc --group 1 --key noborder true
        kwriteconfig6 --file kwinrulesrc --group 1 --key noborderrule 2
        kwriteconfig6 --file kwinrulesrc --group 1 --key title ArkPets
        kwriteconfig6 --file kwinrulesrc --group 1 --key titlematch 2
        kwriteconfig6 --file kwinrulesrc --group General --key count 1
        kwriteconfig6 --file kwinrulesrc --group General --key rules 1
        command -v qdbus6 >/dev/null 2>&1 && qdbus6 org.kde.KWin /KWin org.kde.KWin.reconfigure >/dev/null 2>&1 || true
    else
        warn "Existing KWin rules detected; skipped adding the ArkPets no-border rule."
    fi
fi

# Launcher script (injects the running KDE session environment).
START="$APP/start-arkpets.sh"
log "Writing launcher: $START"
cat > "$START" <<EOF
#!/usr/bin/env bash
# Launch the ArkPets pet in the current KDE session (native Wayland + KWin backend).
APP="$APP"
UID_="\$(id -u)"
: "\${XDG_RUNTIME_DIR:=/run/user/\$UID_}"
: "\${WAYLAND_DISPLAY:=wayland-0}"
# Wait until the graphical session is ready (max ~40s), to avoid launching during
# early login when plasmashell/XWayland are not up yet.
for _ in \$(seq 1 40); do
  PS="\$(pgrep -x plasmashell | head -1)"
  if [ -n "\$PS" ] && [ -S "\$XDG_RUNTIME_DIR/bus" ] \\
     && [ -S "\$XDG_RUNTIME_DIR/\$WAYLAND_DISPLAY" ] \\
     && ls "\$XDG_RUNTIME_DIR"/xauth_* >/dev/null 2>&1; then
    break
  fi
  sleep 1
done
# Inherit the graphical session environment from plasmashell.
PS="\$(pgrep -x plasmashell | head -1)"
if [ -n "\$PS" ] && [ -r "/proc/\$PS/environ" ]; then
  while IFS= read -r -d "" kv; do
    case "\$kv" in
      DISPLAY=*|XAUTHORITY=*|XDG_RUNTIME_DIR=*|DBUS_SESSION_BUS_ADDRESS=*|XDG_CURRENT_DESKTOP=*|XDG_SESSION_TYPE=*|WAYLAND_DISPLAY=*) export "\$kv" ;;
    esac
  done < "/proc/\$PS/environ"
fi
# Fallbacks, robust against an incomplete session environment.
: "\${DBUS_SESSION_BUS_ADDRESS:=unix:path=\$XDG_RUNTIME_DIR/bus}"
: "\${DISPLAY:=:0}"
if [ -z "\${XAUTHORITY:-}" ]; then
  for f in "\$XDG_RUNTIME_DIR"/xauth_*; do
    [ -f "\$f" ] && { XAUTHORITY="\$f"; break; }
  done
fi
export XDG_RUNTIME_DIR DBUS_SESSION_BUS_ADDRESS WAYLAND_DISPLAY DISPLAY XAUTHORITY
export XDG_SESSION_TYPE=wayland XDG_CURRENT_DESKTOP=KDE
# GLFW treats XCURSOR_SIZE as the base cursor size and doubles it for HiDPI, but
# KDE already exports it as size*scale; halve it so the in-window cursor matches.
export XCURSOR_SIZE=\$(( \${XCURSOR_SIZE:-48} / 2 ))
cd "\$APP" || exit 1
exec "\$APP/bin/ArkPets" --direct-start --config "\$APP/ArkPetsConfig.json" "\$@"
EOF
chmod +x "$START"

# Launcher GUI wrapper: same environment handling as the pet launcher (so pets
# spawned by the GUI inherit the correct XCURSOR_SIZE), and keeps cwd at $APP so
# the GUI stores its config there and can find the AppImage stub below.
LAUNCHER="$APP/start-launcher.sh"
log "Writing launcher GUI wrapper: $LAUNCHER"
cat > "$LAUNCHER" <<EOF
#!/usr/bin/env bash
# Launch the ArkPets launcher GUI in the current KDE session.
APP="$APP"
UID_="\$(id -u)"
: "\${XDG_RUNTIME_DIR:=/run/user/\$UID_}"
: "\${WAYLAND_DISPLAY:=wayland-0}"
PS="\$(pgrep -x plasmashell | head -1)"
if [ -n "\$PS" ] && [ -r "/proc/\$PS/environ" ]; then
  while IFS= read -r -d "" kv; do
    case "\$kv" in
      DISPLAY=*|XAUTHORITY=*|XDG_RUNTIME_DIR=*|DBUS_SESSION_BUS_ADDRESS=*|XDG_CURRENT_DESKTOP=*|XDG_SESSION_TYPE=*|WAYLAND_DISPLAY=*) export "\$kv" ;;
    esac
  done < "/proc/\$PS/environ"
fi
: "\${DBUS_SESSION_BUS_ADDRESS:=unix:path=\$XDG_RUNTIME_DIR/bus}"
: "\${DISPLAY:=:0}"
if [ -z "\${XAUTHORITY:-}" ]; then
  for f in "\$XDG_RUNTIME_DIR"/xauth_*; do [ -f "\$f" ] && { XAUTHORITY="\$f"; break; }; done
fi
export XCURSOR_SIZE=\$(( \${XCURSOR_SIZE:-48} / 2 ))
export XDG_RUNTIME_DIR DBUS_SESSION_BUS_ADDRESS WAYLAND_DISPLAY DISPLAY XAUTHORITY
export XDG_SESSION_TYPE=wayland XDG_CURRENT_DESKTOP=KDE
cd "\$APP" || exit 1
exec "\$APP/bin/ArkPets" "\$@"
EOF
chmod +x "$LAUNCHER"

# AppImage stub named exactly as ArkPets expects. It makes the built-in
# "start pet on boot" option available in the GUI (which only checks for this
# file name) and forwards to the pet launcher.
APPIMAGE="$APP/ArkPets-${APP_VER}.AppImage"
log "Writing AppImage stub: $APPIMAGE"
cat > "$APPIMAGE" <<EOF
#!/usr/bin/env bash
exec "\$(dirname "\$0")/start-arkpets.sh" "\$@"
EOF
chmod +x "$APPIMAGE"

# Optional autostart. Uses ArkPets' built-in autostart file so the in-app setting
# ("start pet on boot") reflects it. Without --autostart the setting is left off.
if [[ "$AUTOSTART" -eq 1 ]]; then
    AS="$HOME/.config/autostart"
    mkdir -p "$AS"
    rm -f "$AS/arkpets-pet.desktop"
    log "Enabling ArkPets built-in autostart: $AS/ArkPetsStartup.desktop"
    cat > "$AS/ArkPetsStartup.desktop" <<EOF
[Desktop Entry]
Name=ArkPetsStartup
Exec=$APPIMAGE --direct-start
Type=Application
Path=$APP
EOF
fi

# Application menu entries (so users can start it from the launcher).
APPS="$HOME/.local/share/applications"
mkdir -p "$APPS"
rm -f "$APPS/arkpets-pet.desktop"
log "Creating menu entries in $APPS"
cat > "$APPS/arkpets-launcher.desktop" <<EOF
[Desktop Entry]
Type=Application
Name=ArkPets Launcher
Comment=Arknights desktop pets launcher
Exec=$LAUNCHER
Icon=$APP/lib/ArkPets.png
Terminal=false
Categories=Utility;
EOF
cat > "$APPS/arkpets-pet.desktop" <<EOF
[Desktop Entry]
Type=Application
Name=ArkPets Pet
Comment=Launch the ArkPets desktop pet
Exec=$START
Icon=$APP/lib/ArkPets.png
Terminal=false
Categories=Utility;
EOF
if command -v update-desktop-database >/dev/null 2>&1; then
    update-desktop-database "$APPS" >/dev/null 2>&1 || true
fi

cat <<EOF

$(log "Done.")
Installed app : $APP
Launcher      : $START
Config        : $CONFIG
Models dir    : $APP/models

Next steps:
  1) Restart KWin once so it scans the integration plugin:
        log out and back in (or reboot).
  2) Start the pet:
        $START
     or open the launcher GUI (to download/select models, change settings):
        $APP/bin/ArkPets
  3) If you did not use --with-demo-model, download models in the launcher
     (模型 -> 模型库管理 -> 下载模型), or drop model folders into $APP/models.
EOF
