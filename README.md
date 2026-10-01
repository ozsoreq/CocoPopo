# CocoPopo

A personal Toca Life World–style dollhouse game for Android. Pick a place on the map, build your own
characters, and play: drag people and furniture around, make them emote, take photos.

| Map | Home | School |
|---|---|---|
| ![map](docs/g_map.png) | ![home](docs/g_scene_home.png) | ![school](docs/g_scene_school.png) |

| Café | Character creator |
|---|---|
| ![cafe](docs/g_scene_cafe.png) | ![editor](docs/g_editor_1.png) |

## Install

`release/CocoPopo.apk` is a signed, ready-to-install build (Android 5.0+, landscape).
Copy it to your phone/tablet and open it (allow "install unknown apps" for your file manager/browser).

## How to play

* **Map** – tap one of 8 places: Cozy Home (2-storey cutaway), School, Hospital, Market, Café, Park, Beach, Funfair.
  The pink button (top-left) opens the character creator.
* **Move things** – drag any character or item. Tap one to select it: edit (characters), emote, flip, bigger, smaller, copy, delete.
* **Bottom-left buttons** – people tray (your characters + 10 presets + “New”) and item tray (55+ props in 5 categories).
  Tap a card to add it, or drag it straight into the scene.
* **Character creator** – skin, 9 hairstyles, hair colours, eyes, mouths, 6 outfits, colours and accessories. 🎲 randomises, ♥ saves to *My characters*.
* **Camera** (top-right) saves a UI-free picture to `Pictures/CocoPopo`. **Broom** resets the place (tap twice).
* Every place remembers how you left it.

## Building

No Gradle/Android Studio needed – everything is drawn in code with `android.graphics`, so there are no image assets.

```bash
sudo apt-get install aapt apksigner dalvik-exchange zipalign android-sdk-platform-23   # once
./build.sh                                                                            # -> release/CocoPopo.apk
```

The first build creates `build/cocopopo.keystore` (git-ignored). Keep it if you want future builds to install
over the existing app; an APK signed with a different key must be uninstalled first.

### Desktop preview (no emulator)

`tools/preview.sh g_map g_scene_home g_editor_1 …` renders screens to `preview-out/*.png` using a tiny Java2D
stand-in for `android.graphics` (`tools/preview`). `tools/preview.sh smoke` runs a scripted headless playthrough.

## Layout

```
app/src/main/java/com/cocopopo/app/
  Game.java      all screens, input, tray, editor, persistence (platform independent)
  Avatar.java    character renderer     Look.java   appearance + presets
  PropArt.java   55+ item drawings      Scenes.java location backgrounds + map icons
  Icons.java     button glyphs          Gfx.java    drawing helpers
  GameView.java / MainActivity.java     Android glue
```
