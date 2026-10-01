# CocoPopo – Design Proposal: "Everything is Playable" + Character Makeover

Author: product design review, Oct 2026
Scope: review of v1.1 (Avatar / Look / Pose / Game / Life / PropArt / Scenes) against Toca Boca World / Toca Life conventions.
Constraint respected: solo developer, 100 % code-drawn (android.graphics), no image assets.

Owner's complaint, in their words: *"These characters often times cannot even interact with objects around them."*
The review below shows that this is literally true. About 40 % of the props do nothing except get dragged around. The props that can be used are hard to hit when you drop onto them, and nothing on screen shows the child where a drop would work. Fix those three things first. Then do the makeover.

---

## 1. Critique: what feels flat or un-Toca-like today

### 1.1 Interactions (the biggest problem)
Findings from `Life.java` and `Game.drop/trySeat/tryBed/tryGive/tapObj`:

| Problem | Where | Effect for a 5-year-old |
|---|---|---|
| **Only 18 of 62 props have a rule** (7 seats, 2 beds, 4 surfaces, 5 tap-toggles, plus camera). Bathtub, slide, car, rocket, shelf, tree, umbrella, IV stand, cart, crate and others do nothing. | `Life.seat/bed/surface/states` | "I put her in the bath and she just fell on the floor." |
| **Drop targeting uses the feet anchor**, not the finger. To sit, the *feet* (`o.y`) must land between `seatY-150` and `s.y+50`, within 85·scale px on x. Kids hold characters by the head, about 300 units above the feet, and drop them *in front of* furniture (larger y). Both cases miss. | `trySeat`, `tryBed` | Drops onto a chair "randomly" fail. Scaled-down props (cafe chairs at .75) are worse. |
| **No drop preview or highlight.** Nothing happens while you hover over a valid target. | `onMove` | The child can't learn what is interactive. |
| **Holdability depends on prop scale** (`max(bw,bh)*scale <= 200`). A plant at .8 can be held and a plant at 1.0 can't. **Cotton candy (100×210) is food that can't be held, so it can never be eaten** (bug). | `Life.holdable` | Rules seem to change at random. |
| **Food is eaten only by tapping the holder.** Handing food over does nothing visible apart from a star emote. | `tapObj → eat` | Most kids never find out about eating. |
| **No character↔character interactions.** Dropping a character on another one just makes it fall. | `drop` | No hugs, no carrying the baby, no high-fives: the core social play is missing. |
| **Tap-to-walk ignores props.** If a character is selected and you tap a chair, the chair wiggles; the character doesn't walk over and sit. | `tapObj`, `tapFloor` | Kids expect the character to use the thing they tapped. |
| **Characters never use the world on their own.** Idle behaviours are wave, look, dance and hop. They never sit on a free chair, watch a TV that's on, or go to bed. | `pickIdle` | The world feels like a diorama, not a life sim. |
| **One-hand, one-item, no containers.** Nothing can go *into* the fridge, cart, crate, backpack or tub. A character lying in bed can't hold anything. | `tryGive`, `Obj` | Fewer stories to play out (shopping, packing for school, bath toys). |
| **Cosmetic states have no effect on characters.** TV channels change but nobody watches, the stove heats but nothing cooks, the open fridge shows food that can't be taken. | `states` | These look interactive but they're dead ends. |
| **Home floors are disconnected.** `tapFloor` refuses to cross `Life.band`, and the "stairs hint" is decoration only. | `tapFloor` | "She can't go upstairs." |

### 1.2 Character art
Looking at `cast.png`, `g_life.png` and the editor:
- **One silhouette for everyone.** Every character has the same 180×168 head, a 116×112 torso, the same 38-wide legs and the same white sneakers. Grandma "Nana", the kid "Leo" and "Dr. Pip" have the same body. Toca's appeal comes from body variety: babies, lanky teens, round adults, elders.
- **Faces are flat and symmetric.** Eyes are plain ink ovals with no whites, so they can't look at anything. Brows never move. Expressions only swap the mouth (`p.mood` overrides the mouth, not the eyes or brows), so "surprised" and "happy" read almost the same at play size.
- **Always facing straight front.** Walking only mirrors the whole body (`flip`). There's no 3/4 head turn, so characters never seem to look at the object they're using.
- **Sitting reads as standing.** The legs get shorter but the body stays upright with the same arms and no knees, so in `g_life.png` the sofa sitters look like they're standing behind the sofa.
- **Lying = rotating the standing sprite 90°.** The blanket covers the torso, the legs stick out sideways, the hair spills off the pillow, and the face is sideways (see the bedroom in `g_life.png`).
- **Hands are plain circles**, with no thumb or grip. The hold pose always uses the right arm at 48°, so a surfboard and a donut are held the same way.
- **Few wardrobe options.** There are 6 tops (one of which is the doctor coat). Pants are just a colour. There are no shoes, skirts, shorts, patterns, hats beyond 3, or outfits/costumes. The hairstyle thumbnails in the editor are cropped so tightly that the 9 styles look the same, and the bow accessory covers them.

### 1.3 UI / UX
- The selection popup (edit, emote, flip, ±, copy, delete) is an adult editing tool. There is no play-facing affordance such as "eat", "sleep" or "wear".
- Nothing tells the child an object is interactive: no shimmer, no idle wiggle, no hint hand.
- Feedback is uniform: the same `S_DROP` and `S_TICK` sounds play for everything. There's no unique sound per prop and no success "ta-da" when an interaction completes.
- Tray categories have no labels the child can understand, and the props aren't grouped by location.

### 1.4 World
- The map is charming (rotating planet). But characters can't **travel** between locations. Toca's key loop is "put your character in your pocket and take them to the hospital".
- There's no day/night and no weather, so lamps and beds have no reason to be used.
- There are no secrets. Toca rewards poking things (trees drop fruit, bushes hide items). CocoPopo has only the gift box.

---

## 2. Character makeover spec

All coordinates are in the existing **local avatar space**: origin between the feet, y up is negative, nominal height 380, `Avatar.W = 190`.
Each item says what to add to `Look` (persisted via `encode/decode`; **append new fields at the end** so old saves still decode with defaults).

### 2.1 Body types (`Look.body`, new)
Draw everything from a small per-body proportion table instead of hard-coded numbers:

```java
// headR (x,y) , torsoW, torsoH, torsoTopY, legLen, legW, armLen, shoulderX, totalScale
static final float[][] BODY = {
 /*0 kid    */ {90,84, 116,112,-196, 74,38, 84,62, 1.00f},  // today's body
 /*1 toddler*/ {92,86, 104, 88,-150, 44,36, 62,54, 0.78f},  // big head, tiny legs, draw at 0.78
 /*2 teen   */ {84,80, 108,138,-246,112,34,108,58, 1.12f},  // long legs, smaller head
 /*3 adult  */ {82,78, 136,150,-262,110,40,112,72, 1.18f},  // wide shoulders
 /*4 round  */ {86,80, 150,130,-226, 86,42, 96,78, 1.10f},  // pear/round torso (rr radius 60)
 /*5 elder  */ {84,80, 128,132,-232, 92,38,100,66, 1.06f},  // slight stoop: rotate torso 4° fwd
};
```

- Head centre y = `torsoTopY - 20 - headRy`. Neck: `rr(-19, torsoTopY-18, 38, 34)`.
- Make `Obj.bh` per body (`H * totalScale`) so hit boxes and emote bubbles follow.
- `Avatar.handX/handY` must read the same table. They are already pure functions of `Pose`, so pass `Look` too.
- In the tray, presets get different bodies: Nana = elder, Teacher = adult, Leo = toddler, Zed = teen, Baker = round. A different silhouette per preset is the single biggest visual gain.

### 2.2 Head shapes (`Look.head`, new, 5 options)
These replace `ov(0,-264,90,84)`. Coordinates assume the kid head (centre 0,-264), scaled by `headR`.
| id | name | construction |
|---|---|---|
| 0 | Round | current oval 90×84 |
| 1 | Squircle | `rr(-92,-346,184,166, r=70)` (wider jaw, reads "boy/adult") |
| 2 | Egg | cubic path: top (0,-352), sides (±86,-280), chin (0,-176); jaw control points (±70,-186) |
| 3 | Heart/pointy chin | top lobes `ci(±40,-318,52)` merged with a quad down to the chin (0,-182) through (±88,-250) |
| 4 | Bean (wide & short) | oval 100×74 centred at (0,-254); ears move to ±98 |

Ears: keep `ci(±(headRx-2), -256, 17)` but **hide the far ear when turned** (see 2.6).

### 2.3 Eyes (`Look.eyes`, extend 5 → 10) and eye whites
New default eye construction (Toca-like):
- White sclera `ov(ex, ey, 17, 19)` in `#FFFFFF`, outlined (Gfx.ol 4).
- Pupil `ov(ex+px, ey+py, 10, 12)` in INK, plus a catch-light `ci(+4,-5,3.5)`.
- **Pupil offset (px, py) comes from `Pose.gazeX/gazeY` (-1..1)** clamped to ±6 / ±5. This is what makes characters *look at* the thing they hold, the TV, or another character.
- Eyelid for blink and sleepy: cover with a skin-coloured `rr` whose height = 38·blink, with a 4-px line in `dk(skin,.35)` at its lower edge.

Styles: 0 round-dot (current), 1 happy arcs, 2 big sparkly, 3 sleepy, 4 wink, **5 sclera+pupil (new default)**, **6 lashes** (3 strokes `ln` at 200°/235°/270° from the top of the sclera, length 10), **7 almond** (path with corner points at ex±18, top quad −16, bottom quad +10), **8 button** (`ci 8` solid, no catch-light), **9 starry** (5-point `poly` r=12 pupil, fantasy).

### 2.4 Brows, nose, extras
- Brows become **expression-driven**: `arc` per side rotated by `Pose.browTilt` (deg, ±25) and lifted by `Pose.browLift` (±10 units). Thickness option `Look.brow` 0 thin(4) / 1 bold(8) / 2 none.
- Nose `Look.nose` (new, 4): 0 arc (current), 1 button `ov(0,-244,9,7, dk(skin,.12))`, 2 triangle `poly(-8,-240, 8,-240, 0,-254)`, 3 snub round with nostril dots.
- Face extras `Look.face` (new, 5): none, freckles (6 dots `ci r=2.5` at (±40..±60, -244..-232)), rosy big cheeks (`ov 22×14`, alpha 120), beauty mark, sticker plaster.

### 2.5 Expression system (replaces mouth-only `mood`)
Add `Pose.face` (enum int) plus a `Face` table so the eyes, brows and mouth change together:

| Face | eyes override | brow tilt/lift | mouth | used when |
|---|---|---|---|---|
| HAPPY | — | 0/0 | open smile (1) | handed a toy, on swing |
| LAUGH | happy arcs | -5/-6 | big grin (5) | high-five, slide, tickle (tap) |
| WOW | sclera, pupil small (r 6) | +15/-10 | oh (3) | gift opens, camera flash, rocket |
| YUM | happy arcs | 0/0 | chewing / tongue (4) | eating |
| SAD | sclera, pupil low | -20 inner-up/-4 | inverted arc | toy taken away, dropped hard |
| GRUMPY | half-lid | +20 inner-down/+4 | neutral (2) | flu (hospital), toilet |
| SLEEPY | sleepy (3) | 0/+4 | small oh | lamp off, in bed, yawn |
| LOVE | heart pupils (`path` heart r=10) | 0/-6 | soft smile | hug, teddy |
| SCARED | wide sclera, tiny pupil | +25/-12 | wobbly line (`quad` zig-zag) | falling, thrown |
| SICK | swirl pupils / green tint cheeks | -10/0 | wavy | IV stand, bed in hospital |

Emote bubbles (`Icons.emote`) should be picked to match the face, not at random (`rnd.nextInt(7)` today).

### 2.6 3/4 turn (cheap fake 3D)
Toca characters turn their heads. Add `Pose.turn` (-1..1, eased):
- Shift face features by `turn*18` on x. Squash the near-side eye by `1-|turn|*.15` and the far-side eye by `1-|turn|*.35`.
- Hide the far ear when `|turn|>.4`; move the near ear inward by `turn*10`.
- Hair-front path control points also shift by `turn*10`. Hair-back shifts by `-turn*8` (parallax).
- Torso: shift the neckline by `turn*8`; draw the far arm *behind* the torso when `|turn|>.5`.
- Drive `turn` from the walking direction (±.6), the gaze target, and the seated direction (sofa slot left/right faces the centre).

### 2.7 Hair (`Look.hairStyle`, 9 → 16)
Keep the back/front split. New styles (front fringe variants reuse `fringe()`):
| new id | name | back layer | front layer |
|---|---|---|---|
| 9 | Curly top | 7 `ci r=30` arranged on arc y=-336, x=-72..72 | fringe 1 |
| 10 | Mohawk | none | `rr(-16,-410,32,90,r16)` + 4 notches |
| 11 | Long wavy | 2 cubic lobes down to y=-120 at x=±104 with 3 waves each | fringe 2 |
| 12 | Braids | ov 94×86 + 2 chains of 5 `ov 18×22` from (±92,-230) down to y=-120, tie bands | fringe 3 |
| 13 | Top bun | ov 94×86 + `ci(0,-372,40)` + band | fringe 0 |
| 14 | Side shave | half fringe on one side only, short stubble lines other side | — |
| 15 | Head wrap / hijab | `rr(-112,-360,224,240,r100)` back + face-opening oval cut (clip) | wrap fold `arc` across the forehead |

Hair colour: add a **highlight streak** `Look.hairTip` (none / dip-dye ends `lt(hair,.45)` on the bottom 30 % / streak `rr` on the fringe). Add 4 colours: auburn `#A0412D`, strawberry `#F2A27A`, mint `#9EE6C8`, rainbow (draw as 3 stacked bands clipped to the hair path).
**Editor fix:** draw hairstyle cards with `acc = 0` (no bow) and zoom out to show the full head + shoulders.

### 2.8 Clothing: split into outfit slots
Replace `top`/`bottom` with: `top`, `bottom`, `shoes`, `pattern`, `outfit` (outfit overrides top+bottom when not 0).

**Tops (8):** tee, long sleeve, hoodie, tank, sweater (ribbed hem: 6 `ln` at y=-90), shirt+collar (2 `poly` triangles at the neck), jacket (open front: two `rr` panels with a `ln` zipper), crop/sport.
**Bottoms (`Look.bottom` becomes a style; colour moves to `Look.bottomColor`):** pants, shorts (leg `rr` height 30, then skin), skirt (trapezoid −100..−60, ±64→±76), leggings (pants colour, no cuff), dungaree shorts.
**Dress / one-piece** becomes an outfit, not a top.
**Patterns (`Look.pattern`, 6):** none, stripes (existing), polka (`ci r=6` grid 22 px), stars (`Icons.STAR` small), hearts, check (2 alpha-40 band sets). Implement once in `Gfx.pattern(Canvas, Path clip, int kind, int col)` using `c.clipPath` and apply it to tops, bottoms and dresses.
**Shoes (`Look.shoes`, 6) + `shoeColor`:** sneaker (current, now coloured), boots (`rr(-20,-46,40,48)` + sole), sandals (straps over skin foot), rain boots (glossy, tall to y=-60), slippers (bunny ears `ov 6×14`), barefoot (skin ovals with 3 toe dots).
**Outfits (`Look.outfit`, 10):** none, dress, doctor coat (existing case 5), pajamas (pattern=stars, cuffs), swimsuit (one-piece, bare legs), chef (white double-breasted with 6 `ci` buttons + chef hat), firefighter (yellow coat, reflective `rr` bands), astronaut (white suit, chest panel, helmet as hat), princess (dress + puffed sleeves `ov 26×20` at shoulders), superhero (cape drawn in `hairBack` layer: trapezoid from shoulders to y=-60).

### 2.9 Headwear and accessories: split into two slots
`hat` (12): none, cap, beanie, crown, **party hat** (cone `poly(-40,-330,40,-330,0,-430)` + pompom), **sun hat** (`ov(0,-318,140,26)` brim + dome), **chef hat** (3 `ci r=40` puff + band), **cat ears** (2 `poly` triangles at x=±60, inner pink), **flower crown** (7 small flowers on arc y=-332), **helmet** (astronaut glass dome `ov 120×120` alpha 70), **bunny ears**, **hair clip**.
`face/neck acc` (8): none, round glasses, sunglasses, headphones, bow, **scarf** (`rr` across the neck + tail), **necklace** (arc + pendant), **eye patch** (pirate).
Hats must sit on the head at `headTopY = headCy - headRy` so they work for all head shapes and body types.

### 2.10 Skin palette
Keep the 7 natural tones and 3 fantasy tones. Add 3 natural tones (`#F6D5C3` very light rosy, `#D9A27E` olive, `#3E2416` deepest) and 3 fantasy tones (peach-pink `#FFC6D9`, yellow `#FFE28A`, grey-blue `#B9C4DD`). Make `random()` pick fantasy tones 1 in 12 (as today).

### 2.11 Hands and poses
- **Mitten hands:** replace `ci(0,80,17)` with a palm `ov(0,82,17,19)` plus thumb `ov(s*-12,74,8,10)`. Holding: draw the fingers *over* the item as today, plus the thumb behind it.
- **Pose additions** in `Pose.java`:
  - `holdType`: NONE, ONE_HAND (current), TWO_HANDS (both arms angle ±30°, hands meet at (0,-150); for big items: teddy, gift, crate, books, globe, cake), OVERHEAD (arm 170°; balloon, trophy), HUG (both arms crossed at chest, item at (0,-170) scaled 0.8; teddy, duck), SHOULDER (surfboard, guitar strap: item rotated 70° behind the torso).
  - **Left hand** `heldL`: allow one item per hand. The second drop goes to the left hand (mirrored `handX`).
  - `reach` 0..1: both arms extend toward a dragged item that is within 220 px ("gimme" pose) and the gaze follows it.
  - `sitType`: FRONT (sofa/bench) or SIDE (chair, toilet, swing, wheelchair, desk) drawn in profile-ish 3/4.
    - **Front sit (rewrite):** hips at y=-100. Thighs are foreshortened ovals `ov(s*28,-96,26,20)` *coming toward the viewer*. Shins hang `rr(s*28-17,-90,34,56)`. Feet `ov(s*30,-30,26,14)` with toes forward. **Lower the whole body by 40 units** (`bob=+40`) so the seat cushion overlaps the thighs. Hands rest on the knees (arm angle 25° inward).
    - **Side sit:** turn=0.8. Thigh `rr(0,-112,78,38)` horizontal toward the facing direction, knee at x=+70, shin `rr(56,-112,36,82)` vertical, foot at (78,-26). The far leg is drawn darker and offset by -10/-6.
  - **Lie (rewrite, no more 90° sprite rotation):** dedicated `Avatar.drawLying(c,l,pose)`. Head turned 3/4 toward the camera on the pillow at local (-150,-170). Hair-back flattens into an oval behind the head. Body is a rounded bump under the blanket (blanket drawn by the bed as today). One arm out over the blanket holding a held item (teddy!). Feet bump at the far end. Sleep face = SLEEPY with eyes closed and a slow breathing scale on the blanket bump (`1+sin(t*1.6)*.02`).
  - `bathe`: only head + shoulders visible above water, bubbles on the hair.
  - `ride`: seated in car/rocket/cart/wheelchair, legs hidden, both hands forward on a "wheel".
  - `play`: per-prop loop (strum guitar = right hand oscillates ±12°; read book = both hands at chest with book, gaze down; phone/camera = hands at face).
  - Expressive idles: yawn, stretch (arms 170° then down), giggle (shoulders bounce), scratch head, look at watch. Each is about 1 s using the existing `act` slot.

### 2.12 Animation polish
- **Anticipation and squash** on hop (already has `sq`). Also add a 0.08 s pre-crouch before jumps and walking starts.
- **Hair/ear jiggle:** follow-through on hair-back lobes (pigtails, ponytail, braids) with a damped spring `angle += (target-angle)*k`, where the target comes from vx and hop. This is a cheap and strong "alive" cue.
- **Walk:** add arm counter-swing (exists), heel-toe foot rotation ±10°, and head bob delayed by 0.05 s.

---

## 3. Interaction matrix

### 3.1 New core mechanics (needed by the matrix)
1. **Target resolver** (single function used by both hover highlighting and drop):
   ```java
   static final class Target { Obj obj; int kind; int slot; float score; }
   // kind: SIT, LIE, GIVE, PUT_ON, PUT_IN, WEAR, RIDE, USE, HUG, CARRY, BATHE
   Target resolve(Obj dragged, float fingerX, float fingerY);
   ```
   Score candidates by the distance from the **finger position and the dragged object's body centre (`o.y - bh*scale*.45`)** to the target's **interaction zone** (a rect per prop defined in `Life`). Pick the best within a forgiving radius (≥ 120 px at scale 1, scaled by `max(target.scale,.85)`, so small props are not penalised).
2. **Generic prop rules table** in `Life` that replaces the `switch` statements:
   ```java
   // id -> {seat, bed, surface, container, rideable, wearable, food, useAnim, sound, ...}
   static final Map<String, Rule> RULES = ...;
   ```
   `holdable` becomes a per-prop flag, not a function of scale (this fixes the plant and cotton-candy bugs).
3. **Containers:** `Obj.contents` (list, max N). Items dropped on the container zone go *inside*. They're drawn clipped/behind the front lip, and tapping the container pops one out (the gift-box code already does the pop arc).
4. **Walk-and-use:** with a character selected, tapping a prop → the character walks to the prop's use point (`Rule.useX`, on its floor band), then performs `resolve`'s default action for it. This reuses `tx/ty` plus a new `o.pending` target.
5. **Autonomous use:** in `pickIdle`, with a 30 % chance the character picks a nearby (≤ 600 px, same band) free prop with an *auto* flag: free seat, TV when on, bed when the lamp is off or it's night, food on a table when hungry (needs counter). This needs only a tiny "needs" model: `hunger`, `sleepy`, `fun`, each 0..1, drifting slowly.

### 3.2 Per-prop matrix (all 62 props in `PropArt.DEFS`)
Legend for **Today**: S=seat, B=bed, T=surface (items rest on top), H=holdable (scale-dependent!), F=food (tap to eat), X=tap toggles state, W=wall, –=nothing beyond drag/flick.
**Bold rows have no character interaction today.**

| # | Prop | Today | Proposed character actions → visible result |
|---|---|---|---|
| 1 | bed | B | Lie & sleep (new lying pose; blanket breathes; SLEEPY face; "zzz"). **Two slots** (sleepover). Tap a sleeper → wakes with a stretch. Jump on bed: drop a character from high above → bounces 2× with LAUGH. Holding a teddy in bed → hugs it. |
| 2 | sofa | S | Sit FRONT (2 slots). Lie on sofa (drop horizontally / long-press) → nap. If a TV in the scene is on, sitters turn & watch (gaze toward TV, mood follows channel: cartoon=LAUGH, nature=WOW). |
| 3 | armchair | S | Sit FRONT; held book → reads (book at chest, gaze down, page-flip particle). |
| 4 | table | T | Put items on top (exists). **Characters seated at a chair next to a table auto-face it; food on the table within reach is picked up when the seated character is tapped.** "Set the table": plates appear under food items placed on it. |
| 5 | chair | S | Sit SIDE facing the nearest table/desk. Stand on chair: drop with feet above the seat while holding the drag ≥ 0.6 s → stands on it (cheers). |
| 6 | **shelf** | – | **Container/surface with 4 shelf rows** (y = −96, −176, −256, −336): drop small items onto a shelf row → snaps there. Tap → a random book pops out. |
| 7 | lamp | X | Toggle on/off (exists). **Off + night tint → characters nearby yawn and the bed glows as a hint.** A character standing under a lit lamp gets a warm light circle. |
| 8 | tv | X | Channels (exists). **Watchers:** characters within 500 px turn toward it and react per channel. Item on top already returns 0: make it a surface at −230. |
| 9 | fridge | X | Opens (exists). **Container:** while open, its shelves show its real contents. Tap a shelf item → pops out into the nearest character's hand. Drop food in → stored (persisted). Juice/icecream inside get a "cold" frost tint when taken out. |
| 10 | **tub** | – | **Bathe:** drop a character → `bathe` pose (head and shoulders above the water), bubbles particle, splash sound, LAUGH. Duck dropped in → floats and bobs. Tap tub → more bubbles / a bubble-blowing emote. Leaving the tub → character "sparkles clean" for 3 s. |
| 11 | toilet | S | Sit SIDE; after 2 s → flush sound + swirl particle + HAPPY. Tap toilet → flush. |
| 12 | dresser | T | Surface (exists). **Wardrobe:** tap → drawers open and a random hat/outfit item pops out (wearable item, see §3.3). Drop a character on it → "makeover sparkle": cycles to a random top colour (quick dress-up without the editor). |
| 13 | stove | X | Heat (exists). **Cook:** food placed on top while hot → after 2 s steam particles and the item swaps to a "cooked" variant (pizza gets bubbly cheese, burger gets a grill mark). A character standing in front while it's hot holds a pan (two-hand pose). |
| 14 | desk | T | Surface (exists). **Sit at desk:** drop a character → sits SIDE behind it (desk drawn in front, character at slot y=−96). Books on the desk → the character reads/writes (pencil scribble anim). |
| 15 | **plant** | (H) | Make it **always holdable** (two-hand pose). Water it: a held juice/watering can dropped on it → grows one step (scale +5 %, max 3) with leaf sparkle. Tap → leaves rustle. |
| 16 | **rug** | – | Characters standing on it can **sit cross-legged** (drop onto rug → "floor sit": both legs as horizontal ovals). Tap rug → it flaps/ripples. |
| 17–24 | cake, pizza, burger, icecream, donut, apple, juice, cupcake | H,F | **Auto first bite when handed** (YUM face). After that, tapping the holder takes more bites (exists). Each food gets distinct crumbs/colour and a "yum" sound pitch. Ice cream melts drips if held > 20 s. Cake candle: tap the cake → blow out the candle → party confetti. Juice → slurp sound with a straw-shrink fill-level animation. Sharing: drag a held food onto another character → they take a bite (both YUM). |
| 25 | ball | H | **Kick:** a walking character touching a free ball kicks it (the ball gets vx). Throw to a friend: flick a held ball toward another character → they catch it (catch pose). Tap → bounce (exists). |
| 26 | teddy | H | HUG hold. In bed → cuddled in the lying pose. Can be seated on chairs/sofas as if it were a mini character (seat rule for small "toy" objects). |
| 27 | balloon | H, floats | OVERHEAD hold, the holder hops lightly (low gravity). Release while not held → floats to the ceiling, stays bobbing. Tap a free balloon → pops (confetti, SCARED-then-LAUGH faces from nearby characters). |
| 28 | **car** | – (210 > 200) | **Ride:** drop a character → `ride` pose, `vroom` sound. Selected rider + tap the floor → drives there (faster walk, wheels spin). Toy-sized when scaled < .6 → holdable. |
| 29 | blocks | H | Stack: dropping blocks on blocks → stack (up to 5). Tap a stack → tumbles (physics toss of each). A seated character with blocks in front → "builds" (hand up/down loop). |
| 30 | guitar | H | SHOULDER hold. Tap holder → strum (pluck synth, music-note emotes; nearby characters dance — reuse `act=3`). |
| 31 | duck | H | HUG/bath toy: floats in the tub; squeak sound on tap. |
| 32 | **rocket** | – (250) | **Ride:** drop a character on top → seat at the window. Tap → countdown 3-2-1, flame particles, flies off the top of the screen and lands back (WOW face). |
| 33 | **tree** | – | **Climb:** drop a character in the canopy → sits on a branch (seat at −300). Tap the tree → an apple drops (pool: apple, sometimes a bird emote). |
| 34 | **palm** | – | Tap → a coconut drops (new tiny prop or reuse ball tinted brown). Climb like the tree. |
| 35 | flower | H | Smell: holder raises it to the nose → LOVE face + heart emote. Give to a character → LOVE. Wear: drop on a head → hair flower (wearable slot). |
| 36 | **bush** | – | **Hide:** drop a character in → only eyes peek out, giggle sound. Tap → rustles and sometimes reveals a hidden item (secrets). |
| 37 | mushroom | H | **Bounce pad** when free on the floor: a character dropped on it bounces high (trampoline). |
| 38 | rock | H | Lift with two hands (strain face). Under the rock: tap → it lifts and a beetle scurries off (tiny drawn bug, 2 s). Sit on it (low seat −60). |
| 39 | **umbrella** | – | Seat-zone **shade**: characters under it get sunglasses-on idle. Holdable at small scale (rain umbrella OVERHEAD). Tap → opens/closes (2 states). |
| 40 | sandcastle | H | Shouldn't be holdable (make it static). Tap → a wave flattens it and it rebuilds (2 states). A character with blocks/bucket near it → "builds" anim. |
| 41 | swing | S | Swing (exists, x sway). **Pump:** tap the sitter → sway amplitude grows (×3, decays), LAUGH. Two-character push: another character standing behind → push anim. |
| 42 | **slide** | – | **Slide down:** drop a character at the top (zone around the ladder top, x −60..−20, y −330) → slides along the chute path (cubic from (-30,-320) to (140,-20)) with arms up, "wheee", lands with a squash. Tap slide → the next character in the queue goes. |
| 43 | backpack | H | **Wear** (drawn behind the torso, straps over the shoulders). **Container:** drop books/apple in → stored. Tap a worn backpack → items pop out. |
| 44 | books | H | **Read:** holder sits down or the seated character reads (two-hand, gaze down, page flip). Put on shelf rows. |
| 45 | globe | H | Tap → spins (pstate rotating map), WOW face from a nearby character. |
| 46 | bench | S | Sit FRONT ×2 (exists). Lie down for a nap (2 slots → 1 lying). |
| 47 | frame | W | Tap → cycles 4 pictures (landscape, cat, portrait of the nearest character's colours, abstract). |
| 48 | gift | X | Opens (exists). **Character opens it:** drop the gift on a character → TWO_HANDS hold, tap → they open it (WOW then LAUGH). Close and refill: drop any item into the open box → it becomes a re-giftable box. |
| 49 | trophy | H | OVERHEAD hold → crowd cheers (nearby characters clap: arms oscillate at the chest). |
| 50 | camera | H | Flash (exists). **Characters pose:** on flash, everyone in view does a random pose (peace sign = arm raised + WOW/LAUGH). Held camera → the holder raises it to the eyes, tap = photo for real (calls `host.photo()`). |
| 51 | hbed | B | Lie (exists) + SICK face. With the IV stand adjacent → tube line drawn to the arm. Tap the patient after the medkit is used → "all better" sparkle + jumps out. |
| 52 | **ivstand** | – | Drag next to the hbed → auto-connects to the patient (`ln` path from bag to hand). Rolling: a character can push it (HOLD with the stand following, wheels). |
| 53 | crate | H | **Container** for fruit (apple, donut…). Tap → an apple pops out. Two-hand carry. |
| 54 | register | T,H | Surface (exists). **Use:** a character behind it + tap → "ka-ching", drawer pops, coin particles. Items dropped on it → beep + shrink-and-return ("scanned"). Should not be holdable (static). |
| 55 | coffee | H,F | Drink with steam; "hot" → first sip shows a SCARED-then-HAPPY quick face. |
| 56 | surfboard | H | SHOULDER carry. Drop on the floor at the beach + drop a character on top → stands surfing (wobble anim, wave particles). |
| 57 | popcorn | H,F | Eat; **share:** characters adjacent grab a piece (hand to mouth). With TV on → auto-munch while watching. |
| 58 | cotton | F (**bug: not holdable**) | Fix holdability. Eat → sticky face (pink smudges on the cheeks for 10 s). |
| 59 | medkit | H | **Use on a character:** drop on a character → a bandage plaster appears on the arm/forehead (face extra), SICK → HAPPY. Tap the medkit → stethoscope shown on the holder. |
| 60 | cart | H (oddly) | Make static. **Container** for groceries (up to 6, drawn stacked in the basket). **Ride:** toddlers fit in the seat. **Push:** drop the cart in front of a character → the character holds the handle and walks with it. |
| 61 | wheelchair | S | Sit (exists). Selected rider + tap the floor → rolls there (wheels rotate). |
| 62 | clock | W | Tap → hands spin forward and the scene toggles day/night (ties into lamp/bed autonomy). |

**Props that are fully inert today (17):** shelf, tub, plant (at scale ≥ 1), rug, car, rocket, tree, palm, bush, umbrella, slide, ivstand, cotton (bug), plus cosmetic-only: fridge, stove, tv (no character effect), frame.
**Props whose interaction is only hold/flick (19):** ball, teddy, blocks, guitar, duck, flower, mushroom, rock, sandcastle, backpack, books, globe, trophy, crate, medkit, cart, surfboard, camera (flash only), register.

### 3.3 Wearables (new kind)
Items with `Rule.wear = HAT|BACK|FACE|HAND`: flower (hair), backpack (back), crown/party hat from the gift pool (head), sunglasses (new small prop), chef hat (new). Drop on a character's **head zone** (top 35 % of the hit box) → worn. This temporarily overrides `Look.hat`, stored in `Obj.worn` and persisted. Drag the worn item off to remove it. This lets kids do a *makeover in the world*, not only in the editor.

### 3.4 Character ↔ character
| Trigger (drag A onto B, or both selected) | Result |
|---|---|
| Drop A on B's **body**, both standing | **Hug:** both turn toward each other (turn ±.7), arms wrap (HUG arm angles), LOVE faces, heart emote, 1.5 s, then step apart 60 px. |
| Drop A on B's **hands/side** while moving fast | **High-five:** both raise their near arm, star burst at the contact point, clap sound. |
| Drop A next to B (≤ 140 px) and hold still 0.5 s | **Hold hands:** the nearer hands join (draw a `ci` overlap). If either walks, the other follows at +130 px. Break by dragging apart. |
| Drop a **toddler** body onto an adult/teen | **Carry:** the toddler goes to TWO_HANDS at the carrier's chest (reuse HELD with `holdType=CARRY`). |
| Drop A onto B's **head** | **Piggyback / shoulder ride:** A sits on B's shoulders (SIT state linked to B, seat y = B.bh·−.9). |
| Drag a held item onto another character | **Give** (works today implicitly). Add an "offer" anim: the receiver reaches, then the item transfers with a small arc. |
| Two characters on the same sofa/bench | Occasionally turn and "chat" (alternating speech bubbles with icons: ?, heart, music). |
| Tap a character while another is adjacent | 30 %: the tapped character turns to the neighbour and the neighbour echoes the emote ("contagious" laugh). |
| Ball/flick an item at a character | **Catch** (arms up, item to hand) or bonk (SCARED → LAUGH). |

### 3.5 Context reactions (no explicit drag needed)
- **Handed food → auto bite once** (YUM), tap for more.
- **In bed → sleepy:** yawn, eyes close after 1.5 s, "zzz"; lamp off → everyone in the room yawns.
- **In tub → wash:** bubbles, splashing hands, LAUGH. Leaving → clean sparkle.
- **TV on → watchers** turn/gaze and their faces follow the channel.
- **Music (guitar strum) → dance** for characters within 400 px.
- **Camera flash → poses.**
- **Gift opened nearby → WOW**, everyone turns to look.
- **Dropped from high (> 400 px fall) → SCARED in the air, dizzy stars on landing.**
- **Hospital bed → SICK until the medkit is used.**
- **Being held/dangled:** today it's a mouth "oh". Upgrade to SCARED/LAUGH alternating with leg kicks (already there).
- **Hungry drift:** after ~3 min of play a character shows a food thought bubble (food icon) if no food is held. It's a hint, not a requirement.

---

## 4. UX enhancements

### 4.1 Drag and drop feedback (top priority)
1. **Drop-target highlight:** while dragging, call `resolve()` every frame. Draw the target with a **pulsing white glow outline** (re-draw the prop with `Gfx.ol(14)` in white at alpha 160 behind the normal render). Add an **action badge** above it: chair icon (sit), zzz (sleep), hand (give), bubbles (bath), wheel (ride), arrow-into-box (put in), hat (wear), heart (hug).
2. **Magnet snap:** inside the target zone, ease the dragged object 30 % toward the snap point (seat slot, hand, shelf row). The ghost previews the final pose: the character already shows sitting legs while hovering over a seat.
3. **The dragged character reacts to targets:** hovering over food → reach + YUM face, over a bed → yawn, over a tub → LAUGH.
4. **Invalid drop softness:** if the drop misses, show a 0.3 s "almost" wobble on the nearest target (≤ 250 px) so the child knows it was close.
5. **Bigger hit zones for small fingers:** characters at scale < .8 get a hit box padded by 30 px. Pick-up prefers characters over props when both are hit.

### 4.2 Item-in-hand cues
- A held food shows a tiny **pulsing mouth icon** on the hand for the first 3 times ever (persisted counter). The same pattern applies to guitar (note icon), camera (flash icon), books (eye icon).
- A held item gets a soft white rim so it reads on busy backgrounds.
- Second hand: a held item shows a faint "+" on the free hand when another item hovers nearby.

### 4.3 Discoverability
- **Shimmer pass:** every 8–10 s, one *unused-this-session* interactive prop plays a 0.6 s sparkle sweep (diagonal white band clipped to the prop's bounds). Over time this teaches the child what is interactive without any text.
- **Hint hand:** on the first scene visit, a ghost hand demo drags a character onto a seat (2.5 s loop) until the child does it once.
- **Secrets counter** per location (e.g. 5 stars on the map label: tree fruit, bush find, rock beetle, rocket launch, gift). This gives soft goals that kids love without breaking open-endedness.
- Make the selection popup kid-first: add **context verbs** for the selected character (sit, sleep, eat, dance) as icon buttons that trigger walk-and-use toward the nearest matching prop. Move editing tools (copy, scale, delete) to a long-press or a sub-row.

### 4.4 Sound and haptics (all synthesised in `Synth`)
- Add a **Karplus-Strong pluck** (guitar, xylophone UI), a **filtered noise splash** (tub, flush, wave), a **two-tone "ta-da"** for every successful interaction, a **squeak** (duck, rubber), a **"wheee"** pitch glide (slide, swing), **"vroom"** (low saw with rising pitch for car/rocket), and a **yawn** (descending vibrato tone).
- Per-character **voice blips**: each character gets a pitch based on `Look` hash, and emotes play a 2–3 note blip so characters feel distinct.
- Haptic on snap-into-target, not only on pick-up.

### 4.5 World and map
1. **Pocket / travel bus:** a bus or pocket slot on the scene's left edge. Drop characters (and held items) in, go to the map, enter another location, and they hop out. This is the defining Toca World loop and it makes locations connect.
2. **Stairs at home:** a ladder/stairs zone connecting the floor bands. Walking to it animates up or down, so `tapFloor` can cross bands via the stairs.
3. **Day/night:** a toggle on the clock or a sun/moon button. It applies a dark tint overlay and lit windows, lamps matter, characters get sleepy, and the map planet shows stars.
4. **New locations** that serve the makeover and interaction goals:
   - **Salon / Boutique:** mirror, styling chair (sit → tap cycles the hairstyle, uses `Look`), clothes rack (tap → wearable pops out), hair-dye bottles (drop on head → hair colour). This is the in-world makeover.
   - **Bedroom upstairs / Playroom:** toy box container, bunk bed (2 beds stacked), slide-down from the bunk.
   - **Pet shop:** pets as mini characters (cat/dog drawn with the Avatar head pipeline at 0.5 scale) that follow a holder.
5. **Map life:** characters placed in a location appear on the planet near that building. Tapping them on the map jumps into that location.
6. **Weather on the beach/park:** a rain cloud you can drag across the sky. It wets characters (drip particles) and the umbrella becomes useful.

---

## 5. Prioritised backlog

Effort: S ≈ ½–1 day, M ≈ 2–4 days, L ≈ 1–2 weeks (solo).
★ = top-12 items with the biggest impact on the owner's complaint ("characters can't interact").

| ID | Title | User value | Effort | Priority |
|---|---|---|---|---|
| ★ I-01 | **Target resolver** using finger + body centre with forgiving, scale-aware zones (replace feet-anchored `trySeat/tryBed/tryGive`) | Drops "just work"; removes the #1 frustration | M | **P0** |
| ★ I-02 | **Drop-target glow + action badge + magnet snap** while dragging | Kids see what's possible before letting go | M | **P0** |
| ★ I-03 | **Rules table in `Life`** (per-prop flags; holdable no longer depends on scale; fixes cotton-candy and plant bugs; makes sandcastle/cart/register static) | Consistent behaviour; foundation for all new verbs | S | **P0** |
| ★ I-04 | **Bathtub bathe + duck float** | Most-requested dollhouse verb; home currently has a dead tub | S | **P0** |
| ★ I-05 | **Slide, car, rocket, wheelchair ride/slide** (`ride` pose + slide path) | Turns the park and toys into play | M | **P0** |
| ★ I-06 | **Auto first bite when food is handed + mouth hint on hand** | Makes eating discoverable | S | **P0** |
| ★ I-07 | **Containers:** fridge (take/put food), cart, crate, backpack, shelf rows, toy box | Shopping, packing, cooking stories | M | **P0** |
| ★ I-08 | **Character↔character: hug, high-five, carry toddler, shoulder ride, hold hands** | Social play, the heart of Toca | M | **P0** |
| ★ I-09 | **Walk-and-use:** with a character selected, tap a prop → it walks there and uses it | Interaction without precise dragging | M | **P1** |
| ★ I-10 | **Context reactions:** TV watchers, music dance, camera poses, gift WOW, lamp-off yawns (gaze + face) | The world reacts; characters feel alive | M | **P1** |
| ★ C-01 | **Expression system** (eyes + brows + mouth sets; sclera eyes with gaze) | Characters visibly *respond* to things | M | **P1** |
| ★ C-02 | **New sit (front & side) and lying poses; two-hand/overhead/hug/shoulder holds** | Interactions *look* right instead of "standing behind the sofa" | L | **P1** |
| I-11 | Autonomous use in `pickIdle` (free seats, TV, bed at night, needs drift) | Living-world feel when the child watches | M | P1 |
| C-03 | Body types (toddler/teen/adult/round/elder) + per-preset bodies | Instantly un-samey cast | M | P1 |
| C-04 | 3/4 head turn (`Pose.turn`) | Characters face what they use | M | P1 |
| U-01 | Shimmer pass on unused interactive props + first-visit hint hand | Discoverability | S | P1 |
| U-02 | Per-prop synth sounds + "ta-da" success + voice blips | Feedback and charm | M | P1 |
| I-12 | Tap-secrets: tree/palm fruit, bush hide/find, rock beetle, mushroom bounce, balloon pop, clock day/night | Rewards curiosity; replay value | M | P1 |
| W-01 | Pocket/bus to carry characters between locations | Connects the world; core Toca loop | M | P1 |
| W-02 | Home stairs linking floor bands | Fixes "can't go upstairs" | S | P1 |
| C-05 | Wardrobe split: bottoms (pants/shorts/skirt), shoes (6 + colour), patterns (Gfx.pattern clip) | Makeover variety | M | P1 |
| C-06 | Hats slot (12) + face/neck acc slot; hats anchored to head top | Makeover variety | M | P2 |
| C-07 | Head shapes (5), noses (4), face extras (freckles, plaster) | Distinct faces | S | P2 |
| C-08 | Hair +7 styles incl. braids, top bun, head wrap; dip-dye; editor thumbnails without acc | Inclusivity and variety | M | P2 |
| C-09 | Outfits/costumes (pajamas, swimsuit, chef, firefighter, astronaut, princess, superhero cape) | Role-play per location | M | P2 |
| I-13 | Wearables in-world (drop hat/flower/backpack on head/back) | Makeover during play | S | P2 |
| I-14 | Stove cooking (cooked variants), cake candle blow-out, popcorn share, melting ice cream | Kitchen and party depth | M | P2 |
| I-15 | Hospital loop: IV auto-connect, SICK face, medkit plaster → healed | Gives the hospital purpose | S | P2 |
| C-10 | Hair/pigtail spring follow-through, walk heel-toe, anticipation crouch | Polish | S | P2 |
| U-03 | Kid-first selection popup (context verbs; editing tools on long-press) | Clearer for small kids | S | P2 |
| W-03 | Day/night tint + lit windows + stars on map | Gives lamps/beds meaning | M | P2 |
| W-04 | Salon/Boutique location (styling chair cycles hair, dye on head, clothes rack) | In-world makeover destination | L | P2 |
| W-05 | Characters shown on the map at their location; tap to jump in | World cohesion | S | P2 |
| W-06 | Draggable rain cloud weather | Delight | S | P2 |

### Suggested sequencing
1. **Sprint 1 (P0 foundation):** I-03 → I-01 → I-02 → I-06 → I-04. After this, every existing interaction becomes reliable and visible, and the dead bathtub comes alive.
2. **Sprint 2 (P0 verbs):** I-05, I-07, I-08 (needs the TWO_HANDS/CARRY hold from C-02, so build a minimal version first).
3. **Sprint 3 (makeover core):** C-01, C-02, C-03, C-04. These pay off because the characters now *do* things worth looking at.
4. **Sprint 4:** I-09, I-10, I-11, U-01, U-02, W-01, W-02.
5. **Later:** the wardrobe/makeover breadth (C-05…C-09), I-12…I-15, world items.

### Implementation notes
- **Save compatibility:** `Look.encode` is comma-positional. Append new fields only and default missing ones in `decode` (today any parse error resets to `PRESETS[0]`, so decode must tolerate short arrays). The `Obj.encode` "extra" block needs `contents` (e.g. `item1;item2`), `worn`, `heldL` and the link for character-linked states (shoulder ride, carry).
- **One draw path per pose:** keep `Avatar.draw` as a dispatcher (`drawStanding / drawSitFront / drawSitSide / drawLying / drawBathe / drawRide`). Share the `head()` code via a `headAt(cx, cy, turn)` helper so all poses get the same face and expression.
- **Preview harness:** extend `tools/preview` with `poses` (each sit/lie/hold type × 3 bodies) and `faces` (all expressions) contact sheets so the art can be iterated without a device.
- **Performance:** glow outlines double-draw only the single current target, so the cost is negligible. Patterns use `clipPath` once per garment.
