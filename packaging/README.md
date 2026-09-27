# ArkPets Linux packaging (aarch64 + KDE)

Scripts to install ArkPets and publish the Linux/aarch64 release.

## Files

- `install-arkpets.sh` — installs ArkPets from the app-image zip on a Debian 13
  aarch64 KDE (KWin/Wayland) target: extracts the app, installs the KWin
  integration plugin, adds a KWin no-border rule, and writes launcher wrappers
  (`start-arkpets.sh`, `start-launcher.sh`) and menu entries.
- `create-release.sh` — creates the GitHub releases (with assets) on the fork.

## Build the assets

```bash
# App-image zip + fat jar (self-contained JRE, aarch64 natives)
./gradlew desktop:distZip desktop:distJar
# -> desktop/build/dist/ArkPets-v<ver>.zip  and  .jar

# KWin integration plugin (needs kwin-dev, extra-cmake-modules, cmake, qt6-base-dev)
cd ../Ark-Pets-Integration/KWin && cmake -B build && cmake --build build -j
# -> build/bin/kwin/plugins/ArkPetsIntegration2.so

# Installer / release scripts
cp packaging/install-arkpets.sh packaging/create-release.sh <assets-dir>/
```

## Install on the target

```bash
./install-arkpets.sh --zip ArkPets-v<ver>.zip --plugin ArkPetsIntegration2.so [--with-demo-model] [--autostart]
```

## Publish a release

```bash
git push <fork-remote> <branch> --tags
export GH_TOKEN=<github_pat>
ASSETS_DIR=<assets-dir> ./create-release.sh
```
