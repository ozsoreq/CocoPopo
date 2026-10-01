class_name Prop
extends Thing
## A draggable object with optional states (lamp on, TV channel, ...) and contents.

const GLOW_SHADER := preload("res://scripts/glow.gdshader")

var spr: Sprite2D
var front: Sprite2D       # drawn over a bather / driver
var blanket: Sprite2D     # drawn over a sleeper
var inside: Node2D        # peeking contents
var glow := 0.0
var ds := 1.0             # extra draw scale (held items)
var walking := false      # vehicles driving
var tx := 0.0
var ty := 0.0
var speed := 420.0        # walking speed (vehicles fast, pets slower)
var cook_t := 0.0         # kitchen device busy time left
var cook_out := ""        # what it is making
var close_t := 0.0        # doors swing shut after a moment
var pet_t := 1.0          # pets: time to the next idea
var pend: Thing = null    # pets: where they are heading (pet bed)
var light: PointLight2D
var _shown := ""
static var _light_tex: Texture2D

func _init(prop_id := "ball") -> void:
	id = prop_id
	if Rules.pet(prop_id):
		speed = 170.0
	var d := Rules.prop_def(prop_id)
	bw = d[3]
	bh = d[4]

func _ready() -> void:
	spr = Sprite2D.new()
	add_child(spr)
	var mat := ShaderMaterial.new()
	mat.shader = GLOW_SHADER
	spr.material = mat
	inside = Node2D.new()
	add_child(inside)
	if Art.has("front_" + id):
		front = Art.sprite("front_" + id)
		front.z_index = 3
		front.visible = false
		add_child(front)
	if Art.has("blanket_" + id):
		blanket = Art.sprite("blanket_" + id)
		blanket.z_index = 3
		blanket.visible = false
		add_child(blanket)
	refresh_art()

func art_name() -> String:
	if pstate > 0 and Art.has("prop_%s_%d" % [id, pstate]):
		return "prop_%s_%d" % [id, pstate]
	return "prop_" + id

func refresh_art() -> void:
	var n := art_name()
	if n != _shown:
		Art.set_art(spr, n)
		_shown = n
	for c in inside.get_children():
		c.queue_free()
	if contents.is_empty() or (id == "fridge" and pstate == 0) or (id == "gift" and pstate == 0):
		return
	var base := -bh * 0.6
	match id:
		"cart": base = -96
		"crate": base = -70
		"backpack": base = -130
		"tub": base = -126
		"gift": base = -80
		"fridge": base = -250
		"shelf": base = -262
	var n2: int = min(3, contents.size())
	for i in n2:
		var cid: String = contents[contents.size() - 1 - i]
		var d := Rules.prop_def(cid)
		var k := minf(0.5, 70.0 / max(d[3], d[4]))
		var holder := Node2D.new()
		holder.scale = Vector2.ONE * k
		var off := 110.0 if id == "tub" else 0.0
		holder.position = Vector2((i - (n2 - 1) / 2.0) * 46.0 + off, base)
		holder.add_child(Art.sprite("prop_" + cid))
		holder.z_index = 4 if id == "tub" else 0
		inside.add_child(holder)

## Warm light around lit things at night (def from Rules.light_of: [offset, colour, size]).
func set_light(def: Array, on: bool) -> void:
	if not on or def.is_empty():
		if light != null:
			light.visible = false
		return
	if light == null:
		light = PointLight2D.new()
		light.texture = Prop.light_texture()
		light.energy = 0.6
		light.range_z_min = -4096   # reach the scenery and everything in the room
		light.range_z_max = 4096
		add_child(light)
	light.visible = true
	light.position = def[0]
	light.color = def[1]
	light.texture_scale = def[2]


static func light_texture() -> Texture2D:
	if _light_tex == null:
		var g := GradientTexture2D.new()
		g.fill = GradientTexture2D.FILL_RADIAL
		g.fill_from = Vector2(0.5, 0.5)
		g.fill_to = Vector2(1.0, 0.5)
		g.width = 256
		g.height = 256
		var gr := Gradient.new()
		gr.offsets = PackedFloat32Array([0.0, 0.35, 1.0])
		gr.colors = PackedColorArray([Color(1, 1, 1, 1), Color(1, 1, 1, 0.55), Color(1, 1, 1, 0)])
		g.gradient = gr
		_light_tex = g
	return _light_tex

func draw_scale() -> float:
	return ds

func apply_visual(t: float) -> void:
	var s := sc * back_ease(pop) * ds
	var yoff := -lift * 36.0 * sc - hop
	if id == "balloon" and state != HELD:
		yoff -= 8 + sin(t * 1.6 + phase) * 8
	if launch_t > 0:
		var u := 3.0 - launch_t
		yoff -= u * u * 900.0 if u < 1.5 else (3.0 - u) * (3.0 - u) * 900.0
	if id == "tub":
		for c in inside.get_children():
			c.position.y = -126 + sin(t * 3 + c.get_index()) * 5
	if cook_t > 0 and id == "blender":
		yoff -= absf(sin(t * 40)) * 3
	position = Vector2(x, y + yoff)
	rotation = deg_to_rad(tilt + sin(t * 30) * 6.0 * wiggle)
	scale = Vector2(s * (1.0 - sq * 0.5) * (-1.0 if flip else 1.0) * (1.0 + lift * 0.05), s * (1.0 + sq))
	var mat := spr.material as ShaderMaterial
	mat.set_shader_parameter("amount", glow)
	mat.set_shader_parameter("width", (10.0 + sin(t * 10) * 3.0) * Art.k)
