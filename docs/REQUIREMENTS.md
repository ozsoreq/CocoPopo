# CocoPopo – requirements (Toca Boca World–style play)

Status: all items below are implemented in v1.1 (✅).

Derived from Toca Boca World's public description (open-ended drag-and-drop play; characters eat, drink,
sit on chairs/stools, go to sleep, hold and carry items, change expressions; locations reached from a
world map) and its well-known UX conventions.

## World / navigation
- ✅ **W1** Map is a **rotating planet**: locations stand on the surface of a round world.
- ✅ **W2** Drag left/right to spin the world, with inertia, then it **snaps** so one location sits on top.
- ✅ **W3** The focused location is enlarged with its name; tapping it (or any visible building) enters it.
- ✅ **W4** Arrow buttons spin to the previous/next location (for small hands).
- ✅ **W5** The world feels alive: drifting clouds, sun, little characters walking around the planet, buildings bob.
- ✅ **W6** Character creator reachable from the world screen.

## Characters – life & movement
- ✅ **C1** Idle life: breathing, blinking, looking around, waving, dancing, hopping at random.
- ✅ **C2** Characters **wander** on their own now and then, using a walk cycle (alternating legs, swinging arms, bobbing) and turning to face the direction of travel.
- ✅ **C3** Select a character and tap the floor → it **walks there**.
- ✅ **C4** Picked-up characters dangle (legs kick, arms up, tilt with drag speed).
- ✅ **C5** Dropped anywhere above the floor, characters and items **fall** to the floor with a squash on landing.
- ✅ **C6** Drop a character on a seat (chair, sofa, armchair, bench, toilet, swing, wheelchair) → it **sits**.
- ✅ **C7** Drop a character on a bed → it **lies down and sleeps** (eyes closed, "zzz").
- ✅ **C8** Tapping a character makes it react (hop + emote bubble + matching expression).

## Items – interactions
- ✅ **I1** Drop a small item on a character → it **holds** it in its hand and carries it around.
- ✅ **I2** Tap a character holding food/drink → it **eats/drinks** (chewing mouth, bites shrink the food, crumbs, happy face when finished).
- ✅ **I3** Small items can be placed **on top of tables, desks and dressers**.
- ✅ **I4** Flick an item → it is **thrown**, flies with gravity and bounces on the floor.
- ✅ **I5** Tap-interactive props: lamp on/off, TV channels, fridge opens, stove heats, gift box opens and pops out a surprise, camera flashes.
- ✅ **I6** Wall decorations (pictures, clocks) stay where they're placed instead of falling.

## Feel
- ✅ **F1** Toca-style art: thick dark contour on characters, props, scenery and UI; soft shading. *(done earlier)*
- ✅ **F2** Playful sounds for pick-up, drop, pop, bite, toggle and the world spinning (generated at runtime, no assets).
- ✅ **F3** Haptic tick on pick-up and select.

## Persistence
- ✅ **P1** Every location and the custom characters are remembered between sessions, including who sits where and who holds what.

## v1.2 – from the product design review (docs/DESIGN_PROPOSAL.md)
- ✅ **I-01** Forgiving drop-target resolver (finger, body centre or feet; scale-aware zones)
- ✅ **I-02** Drop-target glow + action badge while dragging (sit, sleep, give, put in, bath, slide, hug, shoulder ride, bounce)
- ✅ **I-03** Per-prop rules table (holdability no longer depends on size; fixes cotton candy / plant)
- ✅ **I-04** Bathtub: bathe with bubbles, toys float
- ✅ **I-05** Slide down the slide; ride the car / wheelchair and drive it by tapping the floor; rocket launches
- ✅ **I-06** Handed food gets an automatic first bite
- ✅ **I-07** Containers: fridge, cart, crate, backpack, tub, gift box, shelf (drop in, tap to take out)
- ✅ **I-08** Character ↔ character: hug, shoulder ride
- ✅ **I-09** Walk-and-use: select a character, tap any prop → it walks there (via the ladder if needed) and uses it
- ✅ **I-10** Context reactions: TV watchers, guitar makes friends dance, camera poses, gift WOW, lamp-off yawns, balloon pop
- ✅ **I-11** Autonomy: characters pick free seats, snacks, toys, the tub and the slide on their own; eat, get up, put things down
- ✅ **I-12** Secrets: tree drops apples, palm drops coconuts, bushes hide toys, mushroom is a bounce pad
- ✅ **C-01** Expression system (eyes + brows + mouth together; eyes with whites that look at things)
- ✅ **C-02** Front sitting pose, bath pose, two-hand and overhead holds, hug arms, mitten hands
- ✅ **C-03** Body types: kid, toddler, teen, adult, round, elder (presets now differ)
- ✅ **C-05 (part)** Bottoms: trousers / shorts / skirt; coloured shoes
- ✅ **C-06 (part)** New hats: party hat, cat ears, flower crown
- ✅ **C-07 (part)** Head shapes (round, squircle, bean), freckles
- ✅ **C-08 (part)** New hair: mohawk, top bun, braids, curly top; editor thumbnails fixed
- ✅ **U-01** Shimmer hints on interactive props
- ✅ **W-02** Ladder linking the home's two floors
- ⏳ Still open from the proposal: high-five / hold hands, side sitting & real lying pose, 3/4 head turn, wearables in-world,
  cooking, hospital loop, day/night, pocket to carry characters between places, salon location.
