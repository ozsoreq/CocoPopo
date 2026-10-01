## Generates all of the game's vector art as SVG files in res://art, plus manifest.json with each
## file's view box [x, y, w, h] (the game uses x, y as the pivot offset of the drawing's origin).
## Entry points: export_art.gd (Godot editor: File > Run) and export_art_cli.gd (command line).
extends RefCounted

const SvgCanvas := preload("res://tools/art/svg_canvas.gd")
const Gfx := preload("res://tools/art/gfx.gd")
const PropArt := preload("res://tools/art/prop_art.gd")
const IconArt := preload("res://tools/art/icon_art.gd")
const CharArt := preload("res://tools/art/char_art.gd")
const Rules := preload("res://scripts/rules.gd")

const PAD := 14.0
const WHITE := 0xFFFFFFFF

var dir := "res://art"
var manifest := {}
var written := 0


## Draws one piece of art and writes <name>.svg. Each drawing starts with outlines off and shading on.
func export(name: String, draw: Callable) -> void:
	var c := SvgCanvas.new()
	var g := Gfx.new(c)
	draw.call(g)
	if c.is_empty():
		return
	FileAccess.open(dir.path_join(name + ".svg"), FileAccess.WRITE).store_string(c.to_svg(PAD))
	# raw SVG is shipped and rasterised at runtime for the screen size, so Godot must not import it
	var imp := dir.path_join(name + ".svg.import")
	if not FileAccess.file_exists(imp):
		FileAccess.open(imp, FileAccess.WRITE).store_string('[remap]\n\nimporter="keep"\n')
	var r := c.view_box(PAD)
	manifest[name] = [float(r.position.x), float(r.position.y), float(r.size.x), float(r.size.y)]
	written += 1


func run() -> int:
	DirAccess.make_dir_recursive_absolute(dir)

	# ---------------- props (+ states, fronts, blankets)
	for d in Rules.PROPS:
		var id: String = d[1]
		export("prop_" + id, func(g: Gfx): _prop(g, id, 0))
		for st in range(1, maxi(Rules.states(id), 1)):
			export("prop_%s_%d" % [id, st], func(g: Gfx): _prop(g, id, st))
	for id in ["tub", "car"]:
		export("front_" + id, func(g: Gfx): g.ol(5); PropArt.front(g, id))
	for id in ["bed", "hbed"]:
		export("blanket_" + id, func(g: Gfx): g.ol(5); PropArt.blanket(g, id))

	# (place backgrounds, the world map and clouds are painted live by scripts/world/)

	# ---------------- UI glyphs + emote bubbles
	for i in IconArt.COUNT:
		export("icon_%d" % i, func(g: Gfx): IconArt.icon(g, i, WHITE))
	for i in IconArt.EMOTES:
		export("emote_%d" % i, func(g: Gfx): IconArt.emote(g, i))

	# ---------------- character parts (white, tinted in game)
	# torso space: origin = hips
	for top in CharArt.N_TOP:
		export("torso_%d" % top, func(g: Gfx): _part(g, 98); CharArt.torso(g, top))
	export("torso_1s", func(g: Gfx): _part(g, 98, 0); CharArt.stripes(g))
	export("torso_4b", func(g: Gfx): _part(g, 98); CharArt.overalls_bib(g))
	export("buttons_4", func(g: Gfx): _part(g, 98, 0); CharArt.overalls_buttons(g))
	export("skirt", func(g: Gfx): _part(g, 98); CharArt.skirt(g))
	# arms: origin = shoulder
	export("sleeve", func(g: Gfx): _part(g, 0); CharArt.sleeve(g))
	export("arm_bare", func(g: Gfx): _part(g, 0); CharArt.arm_bare(g))
	export("mitten_r", func(g: Gfx): _part(g, 0); CharArt.mitten(g, 1))
	export("mitten_l", func(g: Gfx): _part(g, 0); CharArt.mitten(g, -1))
	# legs: origin = hip joint; shoe: origin = shoe centre
	export("leg_pants", func(g: Gfx): _part(g, 0); CharArt.leg_pants(g))
	export("leg_bare", func(g: Gfx): _part(g, 0); CharArt.leg_bare(g))
	export("shorts", func(g: Gfx): _part(g, 0); CharArt.shorts(g))
	export("shoe", func(g: Gfx): _part(g, 0); CharArt.shoe(g))
	# head space: origin = neck
	export("neck", func(g: Gfx): _part(g, 196); CharArt.neck(g))
	for h in CharArt.N_HEAD:
		export("head_%d" % h, func(g: Gfx): _part(g, 196); CharArt.head(g, h))
	for hs in CharArt.N_HAIR:
		export("hairb_%d" % hs, func(g: Gfx): _part(g, 196); CharArt.hair_back(g, hs))
		export("hairf_%d" % hs, func(g: Gfx): _part(g, 196); CharArt.hair_front(g, hs))
	for ac in range(1, CharArt.N_ACC):
		export("acc_%d" % ac, func(g: Gfx): _part(g, 196); CharArt.accessory(g, ac))

	# ---------------- manifest
	var keys := manifest.keys()
	var lines := PackedStringArray()
	for k in keys:
		var v: Array = manifest[k]
		lines.append('  "%s": [%.1f, %.1f, %.1f, %.1f]' % [k, v[0], v[1], v[2], v[3]])
	FileAccess.open(dir.path_join("manifest.json"), FileAccess.WRITE).store_string("{\n" + ",\n".join(lines) + "\n}\n")
	print("exported %d svgs to %s" % [written, dir])
	return written


static func _prop(g: Gfx, id: String, st: int) -> void:
	g.ol(5)
	PropArt.draw(g, id, st)


## Character part setup: thin outlines, flat fills, origin moved to the part's pivot.
static func _part(g: Gfx, dy: float, outline := CharArt.OL) -> void:
	g.ol(outline)
	g.shade = false
	g.translate(0, dy)
