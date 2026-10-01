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
var sun_rays: Node2D
var sun_core: Node2D
var halo: Node2D
var night_sky: Node2D
var moon: Node2D
var cmod: CanvasModulate
var night_btn: RoundButton

func _ready() -> void:
	# sky, sun and drifting clouds behind the planet
	add_child(Sketch.of(func(p: Paint):
		p.fill(Paint.rect(-2000, -1000, 7000, 3200), Paint.vgrad(Color("6cc8ff"), Color("e3f6ff"), 0, 1080), 0.0)))
	sun_rays = Sketch.of(func(p: Paint): Scenery.sun_rays(p, 70))
	add_child(sun_rays)
	sun_core = Sketch.of(func(p: Paint): Scenery.sun_core(p, 70))
	add_child(sun_core)
	for i in 5:
		var s := 0.8 + Scenery.hash01(i + 3) * 0.6
		var cl := Sketch.of(func(p: Paint): Scenery.cloud(p, s), Vector2(Scenery.hash01(i) * 2400, 60 + Scenery.hash01(i + 7) * 220))
		cl.set_meta("speed", 10.0 + Scenery.hash01(i + 11) * 12.0)
		add_child(cl)
		cl.add_to_group("mapcloud")
	night_sky = Sketch.of(func(p: Paint):
		p.fill(Paint.rect(-2000, -1000, 7000, 1900), Paint.vgrad(Color("1d2266"), Color(0.3, 0.25, 0.6, 0.0), 0, 900), 0.0)
		for i in 90:
			var c := Vector2(Scenery.hash01(i + 700) * 3000, Scenery.hash01(i + 800) * 620)
			var r := 2.0 + Scenery.hash01(i + 900) * 3.0
			p.fill(Scenery.star(c, r * 1.8, r * 0.7, 4), Color(1, 1, 0.92, 0.6 + Scenery.hash01(i) * 0.4)))
	night_sky.material = Backdrop.unshaded()
	add_child(night_sky)
	moon = Sketch.of(func(p: Paint): Scenery.moon(p, 56))
	moon.material = Backdrop.unshaded()
	add_child(moon)
	cmod = CanvasModulate.new()
	add_child(cmod)
	halo = Sketch.of(func(p: Paint):
		p.soft(Paint.circle(Vector2.ZERO, R + 40), Color.WHITE, 90, 0.22))
	add_child(halo)
	planet = Node2D.new()
	planet.set_script(load("res://scripts/planet.gd"))
	add_child(planet)
	for k in Rules.PLACES.size():
		# decorations between places
		for j in 2:
			var piv := Node2D.new()
			piv.rotation = deg_to_rad(k * STEP + STEP / 2 + (-9 if j == 0 else 8))
			planet.add_child(piv)
			var kind := k % 2 if j == 0 else 2
			var s := Sketch.of(_deco.bind(kind, k), Vector2(0, -R + 10))
			piv.add_child(s)
	for k in Rules.PLACES.size():
		var piv2 := Node2D.new()
		piv2.rotation = deg_to_rad(k * STEP)
		planet.add_child(piv2)
		var holder := Node2D.new()
		holder.position = Vector2(0, -R + 8)
		piv2.add_child(holder)
		var id: String = Rules.PLACES[k][0]
		holder.add_child(Sketch.of(func(p: Paint): Vignettes.draw(p, id)))
		buildings.append(holder)
	for k in 5:
		var piv3 := Node2D.new()
		planet.add_child(piv3)
		var c := Character.new(Look.preset((k * 3) % Look.PRESETS.size()))
		c.sc = 0.3
		c.y = -R + 4
		piv3.add_child(c)
		walkers.append([piv3, c, k * 72.0 + 14, 1.0 if k % 2 == 0 else -1.0])
	_build_ui()
	get_viewport().size_changed.connect(_layout)
	_layout()
	_apply_night(false)

func _apply_night(animate: bool) -> void:
	var on := bool(Save.data.get("night", false))
	var dur := 1.0 if animate else 0.0
	var tw := create_tween().set_parallel(true)
	tw.tween_property(cmod, "color", Location.NIGHT if on else Color.WHITE, dur)
	for n in [night_sky, moon]:
		tw.tween_property(n, "modulate:a", 1.0 if on else 0.0, dur)
	for n in [sun_rays, sun_core]:
		tw.tween_property(n, "modulate:a", 0.0 if on else 1.0, dur)
	night_btn.set_icon("icon_32" if on else "icon_31")
	night_btn.color = Color("ffb02e") if on else Color("6c63d9")

func _deco(p: Paint, kind: int, k: int) -> void:
	match kind:
		0: Scenery.tree(p, 0.62, k % 3)
		1: Scenery.palm(p, 0.62)
		_: Scenery.bush(p, 0.62)

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
	night_btn = RoundButton.new().setup("icon_31", Color("6c63d9"), 46)
	night_btn.name = "Night"
	night_btn.pressed.connect(func():
		Save.data["night"] = not bool(Save.data.get("night", false))
		Save.write()
		Sfx.play("yawn" if Save.data["night"] else "tada")
		_apply_night(true))
	ui.add_child(night_btn)
	var play := RoundButton.new().setup("icon_23", Color("3cc57b"), 72)
	play.name = "Play"
	play.pressed.connect(func(): enter.emit(Rules.PLACES[focus][0]))
	ui.add_child(play)

func _layout() -> void:
	var vs := get_viewport().get_visible_rect().size
	W = vs.x
	planet.position = Vector2(W / 2, 650 + R)
	halo.position = planet.position
	sun_rays.position = Vector2(W - 210, 150)
	sun_core.position = sun_rays.position
	moon.position = sun_rays.position
	(ui.get_node("Night") as Control).position = Vector2(W - 152, 262)
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
	sun_rays.rotation += dt * 0.12
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
