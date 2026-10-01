# CocoPopo – roadmap: making the game deeper

What the game has today: 8 places on a spinning world, unique characters (presets + your own) with a dress-up editor,
~60 props, drag-and-drop with glowing targets, sit/sleep/bath/slide/ride/hug/shoulder ride, holding & eating, containers,
characters that wander and use things on their own, tap secrets, sounds, autosave. The whole world (places, map,
sky) is painted live by Godot with soft gradients, shadows and animation.

Below are the features that would add the most depth, grouped and prioritised.
**P0 = essential next**, P1 = strong follow-ups, P2 = later. Effort: S (≤1 day), M (2–4 days), L (1–2 weeks).

## P0 – essential

| # | Feature | Why it matters | Effort |
|---|---|---|---|
| 1 | **Doors & travel between places** – walk a character through a door/bus stop to arrive in another place (they keep what they hold) | Turns 8 separate rooms into one connected world; the core Toca loop | M |
| 2 | **Pets** – cat, dog, bunny, bird: follow their owner, can be held, fed, petted, sleep in a basket | Kids' favourite; adds a second kind of "living" thing | M |
| 3 | **Cooking** – ingredients + stove/oven/blender produce dishes (bread → toast, egg → fried egg, fruit → smoothie), serve on plates | Gives the kitchen and café real play loops | M |
| 4 | **Home designer** – change wallpaper, floors, windows per room; furniture shop tray; save several homes | Creative ownership; huge replay value | L |
| 5 | **Wardrobes in the world** – closets/racks with clothes & hats that you drop onto characters (no editor needed) | Makeovers during play, connects editor and scenes | M |
| 6 | **Day / night & weather** – sun/moon button, lights matter, characters get sleepy at night, rain/snow at park & beach | Atmosphere + reasons to use lamps, beds, umbrellas | M |
| 7 | **Character names & needs** – names on tap; gentle hunger/sleep/fun bubbles that hint at things to do | Gives each character personality and soft goals | S–M |
| 8 | **Photo mode & album** – camera button, stickers/frames, save to gallery | Kids love capturing their stories | S |
| 9 | **Background music & per-place ambience** with volume controls | Big "feel" upgrade for little effort | S |
| 10 | **Pinch-zoom & pan in places** + multi-touch (move two things at once) | Small characters + bigger rooms need it; feels native | M |

## P1 – strong follow-ups
- **Babies & family** – carryable babies, crib, high chair, stroller; group characters into families who live in a home (M)
- **Shop & money** – play coins, shelves with price tags, cash register "ka-ching", shopping bag container (M)
- **Instruments & music** – piano/xylophone/drums you can actually play; characters dance in sync (S–M)
- **Art easel** – draw with your finger; the painting becomes a framed picture you can hang (M)
- **Garden** – seeds → sprout → flower/vegetable over real minutes; watering can (S–M)
- **Hidden secrets album** – 3–5 secrets per place, a sticker album that fills in (S)
- **More character interactions** – high-five, hold hands & walk together, follow-the-leader, chatting on the sofa (M)
- **New places** – hair salon (styling chair cycles hairstyles), pet shop, apartment block with several flats, school playground (L each)

## P2 – later
- Seasonal decorations & holidays (Christmas tree, Halloween) (S each)
- Story recording: record a scene with voice and replay it (L)
- Hand-drawn art pass with Godot's animation tools for even smoother characters (L)
- Accessibility & parent settings (sound, haptics, reset, time limits) (S)
- Cloud backup of saves (M)

## Godot-powered additions (Godot version only)
Ideas that build on what the engine gives us for free; none of them apply to the classic Java build.

| # | Feature | Godot tech | Effort |
|---|---|---|---|
| G1 | **Real 2D lighting** – lamps, TV and fridge cast warm light; switching the lamp off really darkens the room; window light shafts move with the time of day | `PointLight2D`, `CanvasModulate`, light occluders on furniture | M |
| G2 | **Living weather** – rain with splashes and puddles, snow that settles on roofs, wind that sways trees and curtains, rainbow after rain | `GPUParticles2D`, a sway vertex shader, `Tween` | M |
| G3 | **Day/night cycle on the live world** – the painted sky, sun and clouds recolour smoothly; stars and street lamps appear at night | Animate `Paint` palettes + `CanvasModulate`, `AnimationPlayer` | S–M |
| G4 | **Juicy physics toys** – balls that bounce off walls and each other, blocks that stack and topple, balloons that float to the ceiling | `RigidBody2D` / `PhysicsServer2D` in a toy layer | M |
| G5 | **Squash & stretch everywhere** – wobble when picking up, jelly landing, characters lean into drags, cloth-like curtains | Shaders + `Tween` easing, `Skeleton2D` bones for soft parts | S–M |
| G6 | **Camera that feels alive** – pinch-zoom, follow a walking character, gentle parallax between sky, scenery and room | `Camera2D` smoothing/limits, `Parallax2D` layers | M |
| G7 | **Photo mode with filters & stickers** – freeze, frame, apply cute filters (soft focus, sepia, sparkles), save to gallery | `SubViewport` capture, canvas shaders, Android share intent | S–M |
| G8 | **Dynamic music** – music layers fade in per place, instruments join when characters dance, sounds follow position (left/right) | `AudioStreamInteractive` / `AudioStreamSynchronized`, `AudioStreamPlayer2D` | M |
| G9 | **Smooth spoken & written words** – character names, speech bubbles and UI in several languages | Godot localisation (`TranslationServer`, CSV/PO) | S |
| G10 | **Accessibility & comfort** – bigger touch targets mode, reduced motion, colour-blind friendly highlights, haptics toggle | Theme scaling, project settings, `Input.vibrate_handheld` | S |
| G11 | **Place editor for parents/kids** – drag scenery pieces (windows, shelves, wallpaper) and save custom places, painted live | The `Paint`/`Backdrop` toolkit + `ResourceSaver` | L |
| G12 | **Tablet & foldable layouts + 60/120 Hz** – adaptive UI for big screens, high-refresh smooth motion, battery saver | `Window` content scale, `Engine.max_fps`, low-processor mode | S |

## Suggested next sprint
1 → 2 → 3 → 6 (connected world, pets, cooking, day/night), then 4 (home designer) as its own milestone.
