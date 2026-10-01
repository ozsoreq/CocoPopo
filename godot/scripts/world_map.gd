class_name WorldMap
extends Node2D
## The rotating little planet: drag to spin, it snaps to a place; tap the building (or play) to go in.

signal enter(place: String)
signal dress_up

const R := 980.0
const STEP := 45.0
var ang := 0.0
var vel := 0.0
var spin_to := NAN
var focus := 0
var t := 0.0
var planet: Node2D
var buildings: Array[Node2D] = []
var walkers: Array = []   # [pivot, character, base_angle, dir]
var ui: CanvasLayer
var name_pill: Label
var down := false
var moved := false
var down_x := 0.0
var last_x := 0.0
var last_ms := 0
var W := 1920.0

func _ready() -> void:
	planet = Node2D.new()
	planet.set_script(load("res://scripts/planet.gd"))
	add_child(planet)
	for k in Rules.PLACES.size():
		# decorations between places
		for j in 2:
			var piv := Node2D.new()
			piv.rotation = deg_to_rad(k * STEP + STEP / 2 + (-9 if j == 0 else 8))
			planet.add_child(piv)
			var s := Art.sprite("prop_" + (["tree", "palm"][k % 2] if j == 0 else "bush"))
			s.position = Vector2(0, -R + 6)
			s.scale *= 0.62
			piv.add_child(s)
	for k in Rules.PLACES.size():
		var piv2 := Node2D.new()
		piv2.rotation = deg_to_rad(k * STEP)
		planet.add_child(piv2)
		var holder := Node2D.new()
		holder.position = Vector2(0, -R + 8)
		piv2.add_child(holder)
		var shadow := Node2D.new()
		holder.add_child(Art.sprite("place_" + Rules.PLACES[k][0]))
		buildings.append(holder)
		var _s := shadow
	for k in 5:
		var piv3 := Node2D.new()
		planet.add_child(piv3)
		var c := Character.new(Look.preset((k * 3) % 10))
		c.sc = 0.3
		c.y = -R + 4
		piv3.add_child(c)
		walkers.append([piv3, c, k * 72.0 + 14, 1.0 if k % 2 == 0 else -1.0])
	for i in 4:
		var cl := Art.sprite("cloud", Color(1, 1, 1, 0.92))
		cl.position = Vector2(randf() * 2400, 60 + randf() * 200)
		cl.set_meta("speed", 10.0 + randf() * 12.0)
		cl.z_index = -10
		add_child(cl)
		cl.add_to_group("mapcloud")
	_build_ui()
	get_viewport().size_changed.connect(_layout)
	_layout()

func _build_ui() -> void:
	ui = CanvasLayer.new()
	ui.layer = 10
	add_child(ui)
	var title := HBoxContainer.new()
	title.name = "Title"
	title.add_theme_constant_override("separation", 2)
	var cols := ["ff5c8a", "ff9a3d", "ffc93c", "3cc5af", "4fb3ff", "8e7bff", "ff5c8a", "ff9a3d"]
	var word := "CocoPopo"
	for i in word.length():
		var l := UI.label(word[i], 96, Color(cols[i]), 22)
		title.add_child(l)
	ui.add_child(title)
	name_pill = UI.label("", 64, Color.WHITE, 18)
	name_pill.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	ui.add_child(name_pill)
	var left := RoundButton.new().setup("icon_15", Color("4fb3ff"), 56)
	left.name = "Left"
	left.pressed.connect(func(): spin_by(-1))
	ui.add_child(left)
	var right := RoundButton.new().setup("icon_15", Color("4fb3ff"), 56)
	right.name = "Right"
	right.mirror = true
	right.pressed.connect(func(): spin_by(1))
	ui.add_child(right)
	var dress := RoundButton.new().setup("icon_21", Color("ff6f8f"), 56)
	dress.name = "Dress"
	dress.pressed.connect(func(): dress_up.emit())
	ui.add_child(dress)
	var dl := UI.label("Dress-up", 30, Color.WHITE, 10)
	dl.name = "DressLabel"
	ui.add_child(dl)
	var play := RoundButton.new().setup("icon_23", Color("3cc57b"), 72)
	play.name = "Play"
	play.pressed.connect(func(): enter.emit(Rules.PLACES[focus][0]))
	ui.add_child(play)

func _layout() -> void:
	var vs := get_viewport().get_visible_rect().size
	W = vs.x
	planet.position = Vector2(W / 2, 650 + R)
	var title: Control = ui.get_node("Title")
	title.reset_size()
	title.position = Vector2((W - title.size.x) / 2, 18)
	(ui.get_node("Left") as Control).position = Vector2(54, 584)
	(ui.get_node("Right") as Control).position = Vector2(W - 166, 584)
	(ui.get_node("Play") as Control).position = Vector2(W / 2 - 72, 905)
	(ui.get_node("Dress") as Control).position = Vector2(44, 36)
	(ui.get_node("DressLabel") as Control).position = Vector2(34, 152)

func rel(a: float) -> float:
	return fposmod(a - ang + 180.0, 360.0) - 180.0

func spin_by(d: int) -> void:
	var base := roundf(ang / STEP) * STEP if is_nan(spin_to) else spin_to
	spin_to = base + d * STEP
	vel = 0
	Sfx.play("swoosh_map")

func _process(delta: float) -> void:
	var dt := minf(delta, 0.05)
	t += dt
	if not down:
		if not is_nan(spin_to):
			ang += (spin_to - ang) * minf(1, dt * 7)
			if absf(spin_to - ang) < 0.2:
				ang = spin_to; spin_to = NAN
		else:
			ang += vel * dt
			vel *= pow(0.06, dt)
			if absf(vel) < 40:
				vel = 0
				var target := roundf(ang / STEP) * STEP
				ang += (target - ang) * minf(1, dt * 8)
	var n := Rules.PLACES.size()
	var f := posmod(int(roundf(ang / STEP)), n)
	if f != focus:
		focus = f
		Sfx.play("tick", -8)
	planet.rotation = deg_to_rad(-ang)
	for i in n:
		var r := rel(i * STEP)
		var foc := maxf(0.0, 1.0 - absf(r) / 30.0)
		var s := 1.45 + 0.45 * foc * foc * (3 - 2 * foc)
		buildings[i].scale = Vector2(s, s)
		buildings[i].position.y = -R + 8 - absf(sin(t * 3)) * 10 * foc
	for w in walkers:
		var piv: Node2D = w[0]
		var c: Character = w[1]
		var a: float = w[2] + t * 2.6 * w[3]
		piv.rotation = deg_to_rad(a)
		c.flip = w[3] < 0
		c.walk = 1; c.walk_ph = t * 9 + a
		c.apply_visual(t)
	for cl in get_tree().get_nodes_in_group("mapcloud"):
		cl.position.x += float(cl.get_meta("speed")) * dt
		if cl.position.x > W + 200:
			cl.position.x = -260
	var nf := maxf(0.0, 1.0 - absf(rel(focus * STEP)) / 12.0)
	name_pill.text = Rules.PLACES[focus][1]
	name_pill.add_theme_color_override("font_outline_color", Color.hex((int(Rules.PLACES[focus][2]) << 8) | 0xFF).darkened(0.45))
	name_pill.reset_size()
	name_pill.pivot_offset = name_pill.size / 2
	name_pill.position = Vector2((W - name_pill.size.x) / 2, 200)
	name_pill.scale = Vector2.ONE * (0.6 + 0.4 * nf)
	name_pill.modulate.a = nf
	queue_redraw()

func _draw() -> void:
	var top := Color("6fcbff")
	var bot := Color("d9f5ff")
	draw_polygon(PackedVector2Array([Vector2(0, 0), Vector2(W, 0), Vector2(W, 1080), Vector2(0, 1080)]),
		PackedColorArray([top, top, bot, bot]))
	var sun := Vector2(W - 210, 150)
	for i in 12:
		var a := deg_to_rad(i * 30 + t * 8)
		draw_line(sun + Vector2(cos(a), sin(a)) * 95, sun + Vector2(cos(a), sin(a)) * 125, Color(1, 0.88, 0.4, 0.7), 18, true)
	draw_circle(sun, 72, Color("ffe066"))
	draw_circle(planet.position, R + 90, Color(1, 1, 1, 0.2))
	draw_circle(planet.position, R + 46, Color(1, 1, 1, 0.28))

func _unhandled_input(e: InputEvent) -> void:
	if e is InputEventMouseButton and e.button_index == MOUSE_BUTTON_LEFT:
		if e.pressed:
			down = true; moved = false; down_x = e.position.x; last_x = e.position.x
			vel = 0; spin_to = NAN; last_ms = Time.get_ticks_msec()
		else:
			down = false
			if not moved:
				_tap(e.position)
	elif e is InputEventMouseMotion and down:
		if absf(e.position.x - down_x) > 18:
			moved = true
		var now := Time.get_ticks_msec()
		var mdt := maxf(0.004, (now - last_ms) / 1000.0)
		last_ms = now
		var d_deg: float = -(e.position.x - last_x) / (R * PI / 180.0) * 1.15
		ang += d_deg
		vel = vel * 0.5 + d_deg / mdt * 0.5
		last_x = e.position.x

func _tap(p: Vector2) -> void:
	for i in Rules.PLACES.size():
		var r := rel(i * STEP)
		if absf(r) > 70:
			continue
		var a := deg_to_rad(r)
		var c := planet.position + Vector2(sin(a), -cos(a)) * (R + 150)
		if p.distance_to(c) < 190:
			if i == focus and absf(r) < 6:
				enter.emit(Rules.PLACES[i][0])
			else:
				spin_to = ang + r
				vel = 0
				Sfx.play("swoosh_map")
			return
