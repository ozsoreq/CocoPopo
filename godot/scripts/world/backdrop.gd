class_name Backdrop
extends Node2D
## A place's scenery, painted by Godot at runtime (no image files): soft gradients, light and shadow,
## and live details — drifting clouds, a turning sun, waves, a bobbing boat, a spinning Ferris wheel.
## The layout (walls, floors, the ladder at home) matches Rules.floors so characters walk on it.

const W := 2560.0
const H := 1080.0
const TOP := -560.0     ## scenery runs past the frame so letterboxed screens never see an edge
const BOTTOM := 1640.0
const L := -420.0
const R := W + 420.0

var place := "home"
var t := 0.0
var _drift: Array = []   # [node, speed]
var _spin: Array = []    # [node, deg per second]
var _bob: Array = []     # [node, base, amplitude, speed, phase]
var _pulse: Array = []   # [node, speed, phase, low, high]
var _wheel: Node2D
var _cabins: Array = []
var _blip: Node2D
var night := false
var _day_only: Array[CanvasItem] = []     # sun: fades out at night
var _night_only: Array[CanvasItem] = []   # night sky, fireflies: fade in
var _lights: Array[PointLight2D] = []
var _flies: Array = []                    # [node, base, phase]
static var _unshaded: CanvasItemMaterial


func setup(id: String) -> Backdrop:
	place = id
	return self


func _ready() -> void:
	match place:
		"home": _home()
		"school": _school()
		"hospital": _hospital()
		"market": _market()
		"cafe": _cafe()
		"park": _park()
		"beach": _beach()
		_: _fair()
	_place_lights()


func _process(delta: float) -> void:
	var dt := minf(delta, 0.05)
	t += dt
	for d in _drift:
		var n: Node2D = d[0]
		n.position.x += float(d[1]) * dt
		if n.position.x > R + 100:
			n.position.x = L - 160
	for s in _spin:
		(s[0] as Node2D).rotation_degrees += float(s[1]) * dt
	for b in _bob:
		var n: Node2D = b[0]
		var base: Vector2 = b[1]
		var amp: Vector2 = b[2]
		n.position = base + Vector2(sin(t * b[3] + b[4]) * amp.x, sin(t * b[3] * 1.7 + b[4]) * amp.y)
		n.rotation = sin(t * b[3] * 1.3 + b[4]) * 0.03 * signf(amp.y)
	for q in _pulse:
		(q[0] as CanvasItem).modulate.a = lerpf(q[3], q[4], 0.5 + 0.5 * sin(t * q[1] + q[2]))
	if _wheel != null:
		for i in _cabins.size():
			var a := _wheel.rotation + deg_to_rad(i * 30)
			(_cabins[i] as Node2D).position = _wheel.position + Vector2(cos(a), sin(a)) * 270
	for f in _flies:
		var base: Vector2 = f[1]
		(f[0] as Node2D).position = base + Vector2(sin(t * 0.7 + f[2]) * 60, sin(t * 1.3 + f[2] * 2) * 30)
	if _blip != null:  # heartbeat dot running across the monitor
		var u := fmod(t * 0.45, 1.0)
		_blip.position = Vector2(lerpf(20, 150, u), _ecg(u))


# ================================================================ day & night
func set_night(on: bool, animate := true) -> void:
	night = on
	for n in _day_only:
		_fade(n, 0.0 if on else 1.0, animate)
	for n in _night_only:
		_fade(n, 1.0 if on else 0.0, animate)
	for l in _lights:
		if on:
			l.visible = true
		if animate:
			var tw := create_tween().tween_property(l, "energy", 0.75 if on else 0.0, 1.2)
			if not on:
				tw.finished.connect(func(): l.visible = night)
		else:
			l.energy = 0.75 if on else 0.0
			l.visible = on


func _fade(n: CanvasItem, a: float, animate: bool) -> void:
	if a > 0:
		n.visible = true
	if animate:
		var tw := create_tween().tween_property(n, "modulate:a", a, 1.2)
		if a == 0:
			tw.finished.connect(func(): n.visible = n.modulate.a > 0.01)
	else:
		n.modulate.a = a
		n.visible = a > 0


static func unshaded() -> CanvasItemMaterial:
	if _unshaded == null:
		_unshaded = CanvasItemMaterial.new()
		_unshaded.light_mode = CanvasItemMaterial.LIGHT_MODE_UNSHADED
	return _unshaded


## Deep starry sky (not dimmed by the night tint) fading out towards the horizon at y1.
func _night_sky(y1: float, moon_at := Vector2.ZERO) -> void:
	var sky := _add(func(p: Paint):
		p.fill(Paint.rect(L, TOP, R - L, y1 - TOP), Paint.vgrad(Color("1d2266"), Color(0.32, 0.25, 0.6, 0.0), 0, y1), 0.0)
		for i in 70:
			var c := Vector2(lerpf(L, R, Scenery.hash01(i + 700)), Scenery.hash01(i + 800) * y1 * 0.75)
			var r := 2.0 + Scenery.hash01(i + 900) * 3.0
			p.fill(Scenery.star(c, r * 1.8, r * 0.7, 4), Color(1, 1, 0.92, 0.6 + Scenery.hash01(i) * 0.4))
		if moon_at != Vector2.ZERO:
			p.glow(moon_at, 150, Color(1, 0.97, 0.8, 0.22), 50)
			Scenery.cut(p, Paint.circle(moon_at, 52), Paint.circle(moon_at + Vector2(28, -16), 46), Color("fff4c2")))
	sky.material = unshaded()
	_night_only.append(sky)


func _light(pos: Vector2, col: Color, size: float) -> void:
	var l := PointLight2D.new()
	l.texture = Prop.light_texture()
	l.position = pos
	l.color = col
	l.texture_scale = size
	l.energy = 0.0
	l.visible = false
	l.range_z_min = -4096
	l.range_z_max = 4096
	add_child(l)
	_lights.append(l)


## Lamps of each place that glow at night.
func _place_lights() -> void:
	var warm := Color(1, 0.84, 0.55)
	match place:
		"home":
			var x0 := W * 0.05
			var mid := x0 + (W * 0.9) * 0.56
			_light(Vector2(x0 + 760, 260), warm, 3.2)
			_light(Vector2(x0 + 280, 670), Color(1, 0.7, 0.8), 3.0)
			_light(Vector2(mid + 560, 720), warm, 3.4)
			_light(Vector2(mid + 480, 280), Color(0.8, 0.95, 1), 2.6)
		"school":
			_light(Vector2(W * 0.3, 220), warm, 3.6); _light(Vector2(W * 0.7, 220), warm, 3.6)
		"hospital":
			_light(Vector2(W * 0.5, 260), Color(1, 0.95, 0.95), 3.0)
			_light(Vector2(W * 0.38 + 85, 390), Color(0.6, 1, 0.7), 1.4)
			_light(Vector2(W * 0.8, 500), warm, 3.0)
		"market":
			_light(Vector2(W * 0.35, 320), warm, 3.6); _light(Vector2(W * 0.65, 320), warm, 3.6)
		"cafe":
			for i in 4:
				_light(Vector2(W * (0.14 + i * 0.24), 250), warm, 3.0)
		"beach":
			_light(Vector2(W * 0.8, 560), Color(0.7, 0.8, 1), 3.4)
		"fair":
			_light(Vector2(W * 0.1, 600), Color(1, 0.75, 0.6), 3.0)
			_light(Vector2(W * 0.9, 600), Color(0.7, 0.85, 1), 3.0)
			_light(Vector2(W * 0.5, 400), Color(1, 0.85, 0.6), 4.5)
		"park":
			for i in 14:
				var base := Vector2(lerpf(200, W - 200, Scenery.hash01(i + 600)), 650 + Scenery.hash01(i + 650) * 300)
				var fly := _add(func(p: Paint): p.glow(Vector2.ZERO, 22, Color(0.95, 1, 0.55, 0.9), 4), base)
				fly.material = unshaded()
				_night_only.append(fly)
				_flies.append([fly, base, Scenery.hash01(i) * 6.0])


# ================================================================ building blocks
func _add(fn: Callable, pos := Vector2.ZERO) -> Sketch:
	var s := Sketch.of(fn, pos)
	add_child(s)
	return s


func _clouds(n: int, y0: float, y1: float, s0 := 0.9, s1 := 1.4) -> void:
	for i in n:
		var s := lerpf(s0, s1, Scenery.hash01(i + 3))
		var c := _add(func(p: Paint): Scenery.cloud(p, s), Vector2(lerpf(L, R, (i + Scenery.hash01(i)) / n), lerpf(y0, y1, Scenery.hash01(i + 7))))
		_drift.append([c, 7.0 + Scenery.hash01(i + 11) * 9.0])


func _sun(pos: Vector2, r: float) -> void:
	var rays := _add(func(p: Paint): Scenery.sun_rays(p, r), pos)
	_spin.append([rays, 5.0])
	_day_only.append(rays)
	_day_only.append(_add(func(p: Paint): Scenery.sun_core(p, r), pos))


func _sky(p: Paint, y1: float, top: Color, bottom: Color) -> void:
	p.fill(Paint.rect(L, TOP, R - L, y1 - TOP), Paint.vgrad(top, bottom, 0, y1), 0.0)


func _ground(p: Paint, y0: float, top: Color, bottom: Color) -> void:
	p.fill(Paint.rect(L, y0, R - L, BOTTOM - y0), Paint.vgrad(top, bottom, y0, H), 0.0)


## Indoor room shell: wall above floor_y, floor below; extends past the frame.
func _room(p: Paint, wall_col: Color, pattern: int, floor_y: float, floor_col: Color, floor_kind := 0, alt := Color.TRANSPARENT) -> void:
	Scenery.wall(p, L, TOP, R - L, floor_y - TOP, wall_col, pattern)
	Scenery.floor(p, L, floor_y, R - L, BOTTOM - floor_y, floor_col, floor_kind, alt)
	Scenery.baseboard(p, L, floor_y, R - L, wall_col.darkened(0.12))


# ================================================================ Home
func _home() -> void:
	var x0 := W * 0.05
	var x1 := W * 0.95
	var mid := x0 + (x1 - x0) * 0.56
	_add(func(p: Paint):
		_sky(p, 990, Color("7fd0ff"), Color("e8f8ff"))
		# soft distant hills at the sides
		p.fill(Paint.ellipse(Vector2(-60, 1000), 520, 260), Color("b8ecb0"))
		p.fill(Paint.ellipse(Vector2(W + 80, 990), 560, 280), Color("c4f0bb"))
	)
	_night_sky(760)
	_sun(Vector2(W + 160, 120), 64)
	_clouds(4, 20, 160, 0.8, 1.2)
	_add(func(p: Paint):
		_ground(p, 990, Color("8fdc80"), Color("62c35e"))
		for i in 18:
			var gx := lerpf(L, R, Scenery.hash01(i))
			if gx > x0 - 30 and gx < x1 + 30:
				continue
			Scenery.flower(p, Vector2(gx, 1020 + Scenery.hash01(i + 40) * 50), 1.0, [Color.WHITE, Color("ffe066"), Color("ff9fbf")][i % 3])
		for i in 30:
			Scenery.grass_tuft(p, Vector2(lerpf(L, R, Scenery.hash01(i + 90)), 1030 + Scenery.hash01(i + 60) * 50), 1.0, Color("4fb25a"))
		# house shadow on the lawn
		p.soft(Paint.ellipse(Vector2(W / 2, 1002), (x1 - x0) * 0.52, 22), Scenery.SHADOW, 30, 0.18)
		# chimney (behind the roof)
		p.fill(Paint.rrect(x1 - 420, -40, 70, 120, 8), Paint.hgrad(Color("e8775f"), Color("c95a47"), x1 - 420, x1 - 350))
		p.fill(Paint.rrect(x1 - 430, -52, 90, 20, 8), Color("b9503f"))
		# roof
		var roof := PackedVector2Array([Vector2(x0 - 46, 134), Vector2(x0 + 126, 10), Vector2(x1 - 126, 10), Vector2(x1 + 46, 134)])
		p.shadow(roof, Vector2(0, 14), 24, 0.18)
		p.fill(roof, Paint.vgrad(Color("ff8d74"), Color("e5604f"), 10, 134))
		for i in 4:
			var ry := 36.0 + i * 26
			var k := (ry - 10) / 124.0
			p.line(Vector2(x0 + 126 - 172 * k + 14, ry), Vector2(x1 - 126 + 172 * k - 14, ry), Color(1, 1, 1, 0.16), 4)
		p.fill(Paint.rrect(x0 + 120, 8, x1 - x0 - 240, 10, 5), Color(1, 1, 1, 0.3))
		# house body
		p.shadow(Paint.rrect(x0 - 14, 138, x1 - x0 + 28, 862, 22), Vector2(0, 10), 28, 0.15)
		p.fill(Paint.rrect(x0 - 14, 138, x1 - x0 + 28, 862, 22), Paint.vgrad(Color("fff6e2"), Color("f3ddb6"), 138, 1000))
		# eave
		p.shadow(Paint.rrect(x0 - 62, 122, x1 - x0 + 124, 24, 12), Vector2(0, 10), 16, 0.2)
		p.fill(Paint.rrect(x0 - 62, 122, x1 - x0 + 124, 24, 12), Paint.vgrad(Color("d7584a"), Color("b8463b"), 122, 146))
		# rooms
		_home_room(p, x0 + 6, 156, mid - x0 - 12, 470, 560, Color("e2d4ff"), 2, Color("dca46c"), 0)
		_home_room(p, mid + 12, 156, x1 - mid - 18, 470, 560, Color("ccf2e8"), 3, Color("bfe4f0"), 1)
		_home_room(p, x0 + 6, 604, mid - x0 - 12, 910, 990, Color("ffe1bf"), 1, Color("d69b62"), 0)
		_home_room(p, mid + 12, 604, x1 - mid - 18, 910, 990, Color("fff1c4"), 0, Color("f3dba8"), 2)
		# slab between floors with a soft lip
		p.fill(Paint.rect(x0 - 14, 560, x1 - x0 + 28, 44), Paint.vgrad(Color("f6dcae"), Color("ecc995"), 560, 604), 0.0)
		Scenery.occlusion(p, x0 - 14, x1 - x0 + 28, 604, 14, 0.12)
		# windows
		Scenery.window(p, x0 + 150, 230, 190, 190, Color("ff9fb5"))
		Scenery.window(p, mid - 440, 650, 200, 190, Color("8fcfff"))
		Scenery.window(p, x1 - 170, 250, 120, 180)
		# bedroom: moon, stars, ceiling lamp
		Scenery.cut(p, Paint.circle(Vector2(x0 + 480, 250), 40), Paint.circle(Vector2(x0 + 502, 236), 36), Color("ffe680"))
		for i in 5:
			p.fill(Scenery.star(Vector2(x0 + 400 + i * 54, 330 + (i % 2) * 40), 12, 5), Color("ffe680"))
		Scenery.pendant(p, x0 + 760, 156, 210, Color("c9b6ff"), 40)
		# bathroom mirror
		p.shadow(Paint.rrect(mid + 150, 230, 130, 170, 50), Vector2(0, 8), 16, 0.14)
		p.fill(Paint.rrect(mid + 150, 230, 130, 170, 50), Color.WHITE)
		p.fill(Paint.rrect(mid + 162, 242, 106, 146, 40), Paint.vgrad(Color("e4f7ff"), Color("b9e6fb"), 242, 388))
		p.fill(PackedVector2Array([Vector2(mid + 190, 250), Vector2(mid + 214, 250), Vector2(mid + 180, 360), Vector2(mid + 166, 340)]), Color(1, 1, 1, 0.4))
		# kitchen: wall cabinets
		var kx := x1 - 640
		p.shadow(Paint.rrect(kx, 640, 560, 112, 16), Vector2(0, 12), 20, 0.16)
		p.fill(Paint.rrect(kx, 640, 560, 112, 16), Paint.vgrad(Color("ffb0bd"), Color("f492a3"), 640, 752))
		for i in 4:
			p.fill(Paint.rrect(kx + 8 + i * 138, 648, 130, 96, 12), Paint.vgrad(Color("ffc8d1"), Color("ffadbb"), 648, 744))
			p.fill(Paint.rrect(kx + 60 + i * 138, 726, 28, 7, 3.5), Color.WHITE)
		# living room: party lights
		var lights := Paint.curve(PackedVector2Array([Vector2(x0 + 30, 628), Vector2(x0 + 280, 668), Vector2(x0 + 560, 632)]), 18)
		Scenery.string_lights(p, lights, [Color("ff7f9f"), Color("ffe066"), Color("7fc8ff")], 4)
		# ladder linking the two floors
		var lx := W * 0.5
		p.shadow(Paint.rrect(lx - 50, 452, 100, 30, 12), Vector2(0, 8), 12, 0.2)
		p.fill(Paint.rrect(lx - 50, 452, 100, 30, 12), Paint.vgrad(Color("a7714a"), Color("7d5232"), 452, 482))
		for side in [-1, 1]:
			p.soft(Paint.rrect(lx + side * 34 - 4, 476, 18, 512, 9), Scenery.SHADOW, 10, 0.14)
			p.fill(Paint.rrect(lx + side * 34 - 6, 470, 12, 516, 6), Paint.hgrad(Color("e2a56a"), Color("bf8048"), lx + side * 34 - 6, lx + side * 34 + 6))
		var ry2 := 500.0
		while ry2 < 980:
			p.fill(Paint.rrect(lx - 34, ry2 - 4, 68, 9, 4.5), Paint.vgrad(Color("f0b77c"), Color("cf9258"), ry2 - 4, ry2 + 5))
			ry2 += 52
		# house rim
		p.stroke(Paint.rrect(x0 - 14, 138, x1 - x0 + 28, 862, 22), Color("e6c590"), 6, true)
	)


func _home_room(p: Paint, x: float, y: float, w: float, floor_y: float, bottom: float, wall_col: Color, pattern: int, floor_col: Color, floor_kind: int) -> void:
	Scenery.wall(p, x, y, w, floor_y - y, wall_col, pattern)
	Scenery.floor(p, x, floor_y, w, bottom - floor_y, floor_col, floor_kind, Color(1, 1, 1, 0.45) if floor_kind == 2 else Color.TRANSPARENT)
	Scenery.baseboard(p, x, floor_y, w, wall_col.darkened(0.12))
	# side shadows make the room feel deep
	p.fill(Paint.rect(x, y, 40, bottom - y), Paint.hgrad(Color(Scenery.SHADOW, 0.10), Color(Scenery.SHADOW, 0), x, x + 40), 0.0)
	p.fill(Paint.rect(x + w - 40, y, 40, bottom - y), Paint.hgrad(Color(Scenery.SHADOW, 0), Color(Scenery.SHADOW, 0.10), x + w - 40, x + w), 0.0)


# ================================================================ School
func _school() -> void:
	_add(func(p: Paint):
		_room(p, Color("c9efdf"), 1, 640, Color("e6b67f"))
		# swags of bunting
		var cols := [Color("ff7f9f"), Color("ffd84d"), Color("6cc0ff"), Color("8be07a"), Color("c7a2ff")]
		for sw in 4:
			var a := Vector2(-80 + sw * 700, 8)
			var b := a + Vector2(700, 0)
			var pts := Paint.quad(a, (a + b) / 2 + Vector2(0, 90), b, 20)
			p.stroke(pts, Color("7b6a8e"), 3)
			for i in range(1, 20, 2):
				var q := pts[i]
				var flag := PackedVector2Array([q + Vector2(-20, 0), q + Vector2(20, 0), q + Vector2(0, 46)])
				p.shadow(flag, Vector2(0, 5), 8, 0.12)
				p.fill(flag, Paint.vgrad(cols[(i + sw) % 5].lightened(0.15), cols[(i + sw) % 5], q.y, q.y + 46))
		# chalkboard
		var cx := W * 0.5
		p.shadow(Paint.rrect(cx - 404, 126, 808, 368, 26), Vector2(0, 16), 30, 0.2)
		p.fill(Paint.rrect(cx - 404, 126, 808, 368, 26), Paint.vgrad(Color("d9a065"), Color("b77b45"), 126, 494))
		p.fill(Paint.rrect(cx - 384, 146, 768, 328, 14), Paint.vgrad(Color("3f8c72"), Color("2b6a55"), 146, 474))
		for i in 6:  # chalk dust
			p.soft(Paint.ellipse(Vector2(cx - 300 + i * 120, 200 + Scenery.hash01(i) * 220), 70, 26), Color.WHITE, 30, 0.035)
		p.text("A B C", cx - 200, 262, 92, Color(1, 1, 1, 0.92))
		p.text("1 + 2 = 3", cx + 170, 254, 70, Color(1, 1, 1, 0.85))
		p.line(Vector2(cx - 340, 300), Vector2(cx + 340, 300), Color(1, 1, 1, 0.3), 4)
		p.ring(Vector2(cx - 200, 390), 36, Color(1, 0.9, 0.4, 0.9), 8)
		for i in 8:
			var a2 := i * PI / 4
			var d := Vector2(cos(a2), sin(a2))
			p.line(Vector2(cx - 200, 390) + d * 52, Vector2(cx - 200, 390) + d * 70, Color(1, 0.9, 0.4, 0.9), 7)
		p.text("Hello!", cx + 150, 410, 84, Color("ffb0d0"))
		p.shadow(Paint.rrect(cx - 404, 474, 808, 24, 12), Vector2(0, 8), 12, 0.2)
		p.fill(Paint.rrect(cx - 404, 474, 808, 24, 12), Paint.vgrad(Color("c88b52"), Color("a56c3a"), 474, 498))
		for i in 3:
			p.fill(Paint.rrect(cx - 370 + i * 46, 466, 36, 10, 5), [Color.WHITE, Color("ffd84d"), Color("ff9fbf")][i])
		# windows and a pennant
		Scenery.window(p, W * 0.84, 160, 220, 260, Color("ffbe78"))
		Scenery.window(p, W * 0.08, 160, 220, 260, Color("ffbe78"))
		p.line(Vector2(W * 0.27, 110), Vector2(W * 0.27, 232), Color("a06a3c"), 7)
		p.fill(PackedVector2Array([Vector2(W * 0.27, 114), Vector2(W * 0.27 + 80, 136), Vector2(W * 0.27, 160)]), Paint.hgrad(Color("ff8fa3"), Color("ff5c73"), W * 0.27, W * 0.27 + 80))
	)


# ================================================================ Hospital
func _hospital() -> void:
	var mx := W * 0.38
	_add(func(p: Paint):
		_room(p, Color("e4f6f3"), 4, 660, Color("eaf2f8"), 2, Color("d3e3ee"))
		p.fill(Paint.rect(L, TOP, R - L, 96 - TOP), Paint.vgrad(Color("a6e6dc"), Color("86d6ca"), 0, 96), 0.0)
		p.fill(Paint.rect(L, 92, R - L, 8), Color("72c7ba"), 0.0)
		Scenery.occlusion(p, L, R - L, 100, 30, 0.10)
		# cross sign
		var cx := W * 0.5
		p.shadow(Paint.circle(Vector2(cx, 250), 92), Vector2(0, 12), 24, 0.16)
		p.fill(Paint.circle(Vector2(cx, 250), 92), Paint.vgrad(Color.WHITE, Color("eef3f8"), 158, 342))
		var cross := Paint.Grad.new(Vector2(cx, 190), Vector2(cx, 310), Color("ff7f8f"), Color("f04d63"))
		p.fill(Paint.rrect(cx - 22, 190, 44, 120, 12), cross)
		p.fill(Paint.rrect(cx - 60, 228, 120, 44, 12), cross)
		Scenery.window(p, W * 0.27, 200, 200, 240)
		Scenery.window(p, W * 0.84, 200, 200, 240)
		# curtain rail + privacy curtains
		p.fill(Paint.rrect(W * 0.03, 148, W * 0.94, 10, 5), Color("b4c2d3"))
		for k in 2:
			var x := W * 0.04 if k == 0 else W * 0.62
			var w := W * 0.17
			p.shadow(Paint.rect(x, 160, w, 300), Vector2(0, 10), 22, 0.12)
			for i in 6:
				var px := x + i * w / 6
				p.fill(Paint.rrect2(px, 158, w / 6 + 2, 302, 4, 18), Paint.hgrad(Color("ffd1e0"), Color("ffa9c4"), px, px + w / 6))
			for i in 7:
				p.fill(Paint.circle(Vector2(x + i * w / 6, 156), 6), Color("8f9db0"))
		# heart monitor
		p.shadow(Paint.rrect(mx, 330, 170, 120, 16), Vector2(0, 10), 18, 0.2)
		p.fill(Paint.rrect(mx, 330, 170, 120, 16), Paint.vgrad(Color("5c5672"), Color("443f58"), 330, 450))
		p.fill(Paint.rrect(mx + 10, 340, 150, 100, 10), Paint.vgrad(Color("1e3346"), Color("152535"), 340, 440))
		for i in 5:
			p.fill(Paint.rect(mx + 10 + i * 30, 340, 1.5, 100), Color(0.5, 1, 0.7, 0.08), 0.0)
		var ecg := PackedVector2Array()
		for i in 61:
			var u := i / 60.0
			ecg.append(Vector2(mx + lerpf(20, 150, u), 330 + _ecg(u)))
		p.stroke(ecg, Color(0.55, 0.95, 0.55, 0.8), 4)
		p.line(Vector2(mx + 85, 450), Vector2(mx + 85, 640), Color("b4c2d3"), 8)
	)
	var screen := Node2D.new()  # the dot moves in monitor space
	screen.position = Vector2(mx, 330)
	add_child(screen)
	_blip = Sketch.of(func(p: Paint): p.glow(Vector2.ZERO, 16, Color(0.7, 1, 0.7, 0.9), 5))
	screen.add_child(_blip)


func _ecg(u: float) -> float:
	# flat, small bump, sharp spike, dip, flat (y within the monitor screen, 340..440 → 62 base)
	var y := 62.0
	if u > 0.30 and u < 0.36:
		y -= sin((u - 0.30) / 0.06 * PI) * 8
	elif u > 0.42 and u < 0.47:
		y -= (1.0 - absf((u - 0.445) / 0.025)) * 36
	elif u > 0.47 and u < 0.52:
		y += sin((u - 0.47) / 0.05 * PI) * 14
	return y


# ================================================================ Market
func _market() -> void:
	_add(func(p: Paint):
		_room(p, Color("fff2d2"), 0, 690, Color("f3e3c0"), 2, Color(1, 1, 1, 0.45))
		# awning with scallops
		var i := 0
		while i * 90 < R - L:
			var x := L + i * 90
			var col := Color("ff6f86") if i % 2 == 0 else Color("fffaf2")
			p.fill(Paint.rect(x, TOP, 90, 56 - TOP), Paint.vgrad(col.lightened(0.1), col, 0, 56), 0.0)
			i += 1
		Scenery.occlusion(p, L, R - L, 56, 40, 0.14)
		i = 0
		while i * 90 < R - L:
			var x := L + i * 90
			var col := Color("ff6f86") if i % 2 == 0 else Color("fffaf2")
			var sc := Paint.pie(Vector2(x + 45, 54), 45, 26, 0, PI)
			p.shadow(sc, Vector2(0, 8), 12, 0.12)
			p.fill(sc, col)
			i += 1
		# sign
		var cx := W * 0.5
		p.shadow(Paint.rrect(cx - 260, 92, 520, 92, 46), Vector2(0, 12), 22, 0.2)
		p.fill(Paint.rrect(cx - 260, 92, 520, 92, 46), Paint.vgrad(Color("6fd07c"), Color("47ad5a"), 92, 184))
		p.stroke(Paint.rrect(cx - 248, 104, 496, 68, 34), Color(1, 1, 1, 0.6), 4, true)
		p.text("MARKET", cx, 162, 70, Color.WHITE)
		# shelf units with goods
		var goods := [Color("ff7f9f"), Color("6fc3ff"), Color("ffc93c"), Color("7ed957"), Color("b67cff"), Color("ff9a3d")]
		for u in 3:
			var sx := W * (0.2 + u * 0.26)
			var sw := W * 0.24
			p.shadow(Paint.rrect(sx, 228, sw, 444, 18), Vector2(0, 14), 26, 0.16)
			p.fill(Paint.rrect(sx, 228, sw, 444, 18), Paint.vgrad(Color("f4f7fb"), Color("dfe6ef"), 228, 672))
			for r in 3:
				var sy := 240.0 + r * 140
				p.fill(Paint.rrect(sx + 12, sy, sw - 24, 124, 10), Paint.vgrad(Color("d3dceb"), Color("e3e9f2"), sy, sy + 124))
				var x := sx + 24
				var k := 0
				while x < sx + sw - 54:
					var hh := 54.0 + int(Scenery.hash01(u * 40 + r * 9 + k) * 40)
					var col: Color = goods[(k + r + u) % 6]
					var base := sy + 122
					if (k + r) % 3 == 0:  # fruit
						p.fill(Paint.circle(Vector2(x + 22, base - 22), 21), Paint.vgrad(col.lightened(0.25), col, base - 44, base))
						p.fill(Paint.circle(Vector2(x + 15, base - 30), 6), Color(1, 1, 1, 0.4))
					else:  # jar / box
						p.fill(Paint.rrect(x, base - hh, 40, hh, 10), Paint.hgrad(col.lightened(0.18), col.darkened(0.05), x, x + 40))
						p.fill(Paint.rrect(x + 4, base - hh - 8, 32, 12, 5), col.darkened(0.25))
						p.fill(Paint.rrect(x + 7, base - hh + 18, 26, 18, 5), Color(1, 1, 1, 0.75))
					x += 48
					k += 1
				p.shadow(Paint.rrect(sx + 6, sy + 120, sw - 12, 14, 7), Vector2(0, 6), 8, 0.14)
				p.fill(Paint.rrect(sx + 6, sy + 120, sw - 12, 14, 7), Paint.vgrad(Color("c4cfdd"), Color("a9b6c8"), sy + 120, sy + 134))
	)


# ================================================================ Cafe
func _cafe() -> void:
	_add(func(p: Paint):
		_room(p, Color("ffe3c6"), 2, 680, Color("c88d5c"))
		# wood wainscot
		p.fill(Paint.rect(L, 400, R - L, 280), Paint.vgrad(Color("e2a974"), Color("c98d58"), 400, 680), 0.0)
		var px := L + 20
		while px < R:
			p.fill(Paint.rrect(px, 440, 150, 200, 12), Color(1, 1, 1, 0.1))
			p.fill(Paint.rrect(px, 440, 150, 6, 3), Color(Scenery.SHADOW, 0.08))
			px += 180
		p.shadow(Paint.rect(L, 392, R - L, 18), Vector2(0, 6), 10, 0.15)
		p.fill(Paint.rect(L, 392, R - L, 18), Paint.vgrad(Color("c88b5a"), Color("a86f43"), 392, 410), 0.0)
		# menu board
		var mx := W * 0.04
		p.shadow(Paint.rrect(mx, 240, 330, 250, 20), Vector2(0, 14), 24, 0.2)
		p.fill(Paint.rrect(mx, 240, 330, 250, 20), Paint.vgrad(Color("c78a55"), Color("a46a3b"), 240, 490))
		p.fill(Paint.rrect(mx + 14, 254, 302, 222, 12), Paint.vgrad(Color("434d4b"), Color("2f3736"), 254, 476))
		p.text("MENU", mx + 165, 322, 56, Color(1, 1, 1, 0.92))
		for i in 3:
			p.line(Vector2(mx + 40, 366 + i * 40), Vector2(mx + 196, 366 + i * 40), Color(1, 1, 1, 0.5), 5)
			p.text("%d.5" % (2 + i), mx + 260, 378 + i * 40, 32, Color("ffe27a"))
		# window with a striped awning
		var wx := W * 0.3
		Scenery.window(p, wx, 240, 260, 200)
		for i in 6:
			var col := Color("ff7f9a") if i % 2 == 0 else Color.WHITE
			var ax := wx - 30 + i * 53
			var aw := Paint.rrect2(ax, 196, 53, 58, 0, 22)
			p.shadow(aw, Vector2(0, 8), 12, 0.1)
			p.fill(aw, Paint.vgrad(col, col.darkened(0.06), 196, 254))
		# counter
		var cx := W * 0.62
		var cw := W * 0.34
		p.shadow(Paint.rrect(cx, 440, cw, 250, 18), Vector2(0, 16), 28, 0.2)
		p.fill(Paint.rrect(cx, 440, cw, 250, 18), Paint.vgrad(Color("ffab8a"), Color("f08a68"), 440, 690))
		for i in 5:
			p.fill(Paint.rrect(cx + 20 + i * (cw - 40) / 5, 482, (cw - 60) / 5, 188, 12), Color(1, 1, 1, 0.16))
		p.shadow(Paint.rrect(cx - 16, 418, cw + 32, 38, 16), Vector2(0, 8), 12, 0.18)
		p.fill(Paint.rrect(cx - 16, 418, cw + 32, 38, 16), Paint.vgrad(Color("fff6e8"), Color("f1dfc4"), 418, 456))
		# pastry case
		p.fill(Paint.rrect(cx + 20, 328, 224, 92, 18), Color(0.85, 0.95, 1, 0.55))
		for i in 4:
			var pc := Vector2(cx + 52 + i * 52, 396)
			p.fill(Paint.ellipse(pc, 20, 14), Paint.vgrad(Color("f7bd6a"), Color("e09a46"), pc.y - 14, pc.y + 14))
			p.fill(Paint.ellipse(pc + Vector2(0, -10), 13, 8), Color("ffaecb") if i % 2 == 0 else Color.WHITE)
		p.fill(PackedVector2Array([Vector2(cx + 40, 334), Vector2(cx + 70, 334), Vector2(cx + 46, 414), Vector2(cx + 30, 414)]), Color(1, 1, 1, 0.35))
		# coffee machine
		var kx := cx + cw - 190
		p.shadow(Paint.rrect(kx, 338, 150, 86, 14), Vector2(0, 8), 12, 0.16)
		p.fill(Paint.rrect(kx, 338, 150, 86, 14), Paint.vgrad(Color("c5cfdc"), Color("a7b3c4"), 338, 424))
		p.fill(Paint.rrect(kx, 338, 150, 26, 13), Color("6b6480"))
		p.fill(Paint.rrect(kx + 50, 378, 50, 46, 8), Color("4b4660"))
		p.fill(Paint.circle(Vector2(kx + 26, 390), 8), Color("ff7f9a"))
		for i in 4:
			Scenery.pendant(p, W * (0.14 + i * 0.24), -20, 170, Color("ffc857"), 60)
	)


# ================================================================ Park
func _park() -> void:
	_add(func(p: Paint):
		_sky(p, 640, Color("72cdff"), Color("e8f9ff"))
	)
	_night_sky(640, Vector2(W * 0.84, 150))
	_sun(Vector2(W * 0.84, 150), 70)
	_clouds(5, 50, 260)
	_add(func(p: Paint):
		# layered hills with haze
		p.fill(Paint.ellipse(Vector2(W * 0.18, 680), W * 0.42, 190), Color("c2efb6"))
		p.fill(Paint.ellipse(Vector2(W * 0.82, 690), W * 0.48, 170), Color("b0e8a3"))
		for i in 7:  # far tree line
			var tx := lerpf(L + 100, R - 100, Scenery.hash01(i + 20))
			p.fill(Paint.circle(Vector2(tx, 560 + Scenery.hash01(i) * 30), 46 + Scenery.hash01(i + 5) * 26), Color("9ddc94"))
		_ground(p, 600, Color("8fdd7e"), Color("5dbf59"))
		# path
		var path := PackedVector2Array([Vector2(W * 0.47, 620), Vector2(W * 0.53, 620)])
		path.append_array(Paint.curve(PackedVector2Array([Vector2(W * 0.56, 700), Vector2(W * 0.66, 880), Vector2(W * 0.8, BOTTOM)]), 10))
		path.append_array(Paint.curve(PackedVector2Array([Vector2(W * 0.2, BOTTOM), Vector2(W * 0.36, 880), Vector2(W * 0.44, 700)]), 10))
		p.fill(path, Paint.vgrad(Color("fbecc6"), Color("efd49a"), 620, H))
		# fence
		var fx := L + 10
		while fx < R:
			var picket := Paint.rrect2(fx, 566, 30, 92, 15, 4)
			p.shadow(picket, Vector2(6, 8), 10, 0.1)
			p.fill(picket, Paint.vgrad(Color.WHITE, Color("e9e4dc"), 566, 658))
			fx += 54
		for ry in [592.0, 628.0]:
			p.fill(Paint.rrect(L, ry, R - L, 14, 7), Paint.vgrad(Color("fbf8f2"), Color("e3ddd2"), ry, ry + 14))
		# pond
		var px := W * 0.86
		p.fill(Paint.ellipse(Vector2(px, 852), 254, 90), Color("c9efff"))
		p.fill(Paint.ellipse(Vector2(px, 856), 232, 76), Paint.vgrad(Color("7fd3f7"), Color("4fb3e8"), 780, 932))
		p.fill(Paint.ellipse(Vector2(px - 60, 836), 80, 14), Color(1, 1, 1, 0.3))
		p.fill(Paint.ellipse(Vector2(px + 40, 868), 50, 8), Color(1, 1, 1, 0.2))
		for lp in [[70, 872, 34], [-20, 882, 28], [-130, 860, 22]]:
			Scenery.cut(p, Paint.ellipse(Vector2(px + lp[0], lp[1]), lp[2], lp[2] * 0.42), Paint.pie(Vector2(px + lp[0], lp[1]), lp[2] + 2, lp[2], -0.3, 0.3), Color("57c46c"))
		p.fill(Paint.circle(Vector2(px + 74, 866), 8), Color("ff9fc4"))
		# flowers and grass
		var off_path := func(q: Vector2) -> bool: return absf(q.x - W * 0.5) > W * 0.05 + (q.y - 620) * 0.42
		for i in 26:
			var fp := Vector2(lerpf(L, R, Scenery.hash01(i + 1)), 700 + Scenery.hash01(i + 70) * 360)
			if (absf(fp.x - px) < 270 and absf(fp.y - 852) < 100) or not off_path.call(fp):
				continue
			Scenery.flower(p, fp, 1.1, [Color.WHITE, Color("ffe066"), Color("ff9fbf"), Color("c7a2ff")][i % 4])
		for i in 40:
			var gp := Vector2(lerpf(L, R, Scenery.hash01(i + 300)), 680 + Scenery.hash01(i + 200) * 400)
			if off_path.call(gp) and not (absf(gp.x - px) < 260 and absf(gp.y - 852) < 95):
				Scenery.grass_tuft(p, gp, 1.0, Color("4aae57"))
	)


# ================================================================ Beach
func _beach() -> void:
	_add(func(p: Paint):
		_sky(p, 470, Color("6ccdff"), Color("fff1d6"))
	)
	_night_sky(470, Vector2(W * 0.8, 150))
	_sun(Vector2(W * 0.8, 150), 68)
	_clouds(4, 40, 220)
	_add(func(p: Paint):
		p.fill(Paint.rect(L, 470, R - L, 280), Paint.vgrad(Color("56cfe8"), Color("2295cc"), 470, 750), 0.0)
		p.fill(Paint.rect(L, 468, R - L, 6), Color(1, 1, 1, 0.5), 0.0)
		p.soft(Paint.ellipse(Vector2(W * 0.8, 500), 180, 10), Color(1, 0.95, 0.7), 30, 0.35)  # sun glitter
	)
	# waves drift back and forth
	for row in 6:
		var wy := 560.0 + row * 34
		var off := fmod(row * 97.0, 220.0)
		var waves := _add(func(p: Paint):
			var x := L - 220 + off
			while x < R + 220:
				p.stroke(Paint.ellipse(Vector2(x, wy), 46, 9, deg_to_rad(200), deg_to_rad(340)), Color(1, 1, 1, 0.5 - row * 0.05), 5)
				x += 220
		)
		_bob.append([waves, Vector2.ZERO, Vector2(26 + row * 4, 0), 0.5 + row * 0.07, row * 1.3])
	# boat
	var boat := _add(func(p: Paint):
		p.fill(PackedVector2Array([Vector2(0, -112), Vector2(0, -10), Vector2(78, -10)]), Paint.hgrad(Color.WHITE, Color("e8eef6"), 0, 78))
		p.fill(PackedVector2Array([Vector2(-8, -92), Vector2(-8, -10), Vector2(-58, -10)]), Paint.hgrad(Color("ff6f8f"), Color("ff9fb1"), -58, -8))
		p.line(Vector2(-4, -120), Vector2(-4, -6), Color("8a5a30"), 6)
		var hull := Paint.rrect2(-74, -8, 168, 32, 6, 16)
		p.fill(hull, Paint.vgrad(Color("b07a4d"), Color("82552f"), -8, 24))
		p.fill(Paint.rect(-70, -4, 160, 5), Color(1, 1, 1, 0.3), 0.0)
	, Vector2(W * 0.3, 520))
	_bob.append([boat, Vector2(W * 0.3, 520), Vector2(24, 4), 0.6, 0.0])
	_add(func(p: Paint):
		# wet sand, foam and dry sand
		for layer in 2:
			var off := 34.0 * layer
			var pts := PackedVector2Array()
			for i in 11:
				pts.append(Vector2(lerpf(L, R, i / 10.0), 742 + off - (i % 2) * 34 + 10))
			var top := Paint.curve(pts, 10)
			var shape := top.duplicate()
			shape.append(Vector2(R, BOTTOM)); shape.append(Vector2(L, BOTTOM))
			if layer == 0:
				p.fill(shape, Paint.vgrad(Color("e2c486"), Color("d9b673"), 700, 800))
				p.stroke(top, Color(1, 1, 1, 0.75), 8)
			else:
				p.fill(shape, Paint.vgrad(Color("fbe6b2"), Color("f2d18f"), 760, H))
		for i in 22:
			p.fill(Paint.circle(Vector2(lerpf(L, R, Scenery.hash01(i + 5)), 860 + Scenery.hash01(i + 33) * 200), 3 + Scenery.hash01(i) * 3), Color(0.75, 0.6, 0.35, 0.35))
		# starfish and shells
		var st := Scenery.star(Vector2(W * 0.46, 1030), 34, 15, 5)
		p.shadow(st, Vector2(0, 5), 8, 0.15)
		p.fill(Paint.blob(st, 4), Paint.vgrad(Color("ffab7a"), Color("ff8a5c"), 996, 1064))
		for sh in [[0.62, 980], [0.14, 1040], [0.9, 1010]]:
			var c := Vector2(W * sh[0], sh[1])
			p.fill(Paint.pie(c, 22, 20, PI, TAU), Paint.vgrad(Color("fff0f4"), Color("ffc9d6"), c.y - 20, c.y))
	)


# ================================================================ Funfair
func _fair() -> void:
	var cx := W * 0.5
	var cy := 400.0
	_add(func(p: Paint):
		_sky(p, 770, Color("4a3fbf"), Color("ffb27a"))
		p.fill(Paint.rect(L, 300, R - L, 470), Paint.vgrad(Color(0.85, 0.45, 0.85, 0.0), Color(1, 0.6, 0.6, 0.45), 300, 770), 0.0)
	)
	_night_sky(770)
	for k in 2:  # twinkling stars in two groups
		var stars := _add(func(p: Paint):
			for i in 22:
				var j := i * 2 + k
				var c := Vector2(lerpf(L, R, Scenery.hash01(j)), Scenery.hash01(j + 50) * 320)
				p.fill(Scenery.star(c, 4 + Scenery.hash01(j + 7) * 4, 1.8 + Scenery.hash01(j + 7) * 1.5, 4), Color(1, 1, 0.9, 0.9))
		)
		_pulse.append([stars, 1.6 + k * 0.7, k * 2.0, 0.35, 1.0])
	_add(func(p: Paint): Scenery.moon(p, 46), Vector2(W * 0.14, 130))
	# Ferris wheel: legs, turning wheel, cabins that stay upright
	_add(func(p: Paint):
		for side in [-1, 1]:
			p.line(Vector2(cx, cy), Vector2(cx + side * 160, 900), Paint.hgrad(Color("f2f0ff"), Color("cfc9ef"), cx - 160, cx + 160), 20)
		p.fill(Paint.rrect(cx - 200, 880, 400, 28, 14), Color("8f7fd0"))
	)
	_wheel = _add(func(p: Paint):
		p.ring(Vector2.ZERO, 270, Color(1, 1, 1, 0.95), 14)
		p.ring(Vector2.ZERO, 150, Color(1, 1, 1, 0.75), 8)
		for i in 12:
			var a := deg_to_rad(i * 30)
			p.line(Vector2.ZERO, Vector2(cos(a), sin(a)) * 270, Color(1, 1, 1, 0.8), 6)
		for i in 24:
			var a := deg_to_rad(i * 15 + 7.5)
			p.glow(Vector2(cos(a), sin(a)) * 270, 18, Color(1, 0.9, 0.5, 0.8), 4)
	, Vector2(cx, cy))
	_spin.append([_wheel, 9.0])
	var cab_cols := [Color("ff6f8f"), Color("ffd43b"), Color("4fb3ff"), Color("7ed957")]
	for i in 12:
		var col: Color = cab_cols[i % 4]
		var cab := _add(func(p: Paint):
			p.line(Vector2.ZERO, Vector2(0, 28), Color("eceaf8"), 5)
			p.shadow(Paint.rrect(-30, 28, 60, 52, 18), Vector2(0, 6), 10, 0.2)
			p.fill(Paint.rrect(-30, 28, 60, 52, 18), Paint.vgrad(col.lightened(0.2), col.darkened(0.05), 28, 80))
			p.fill(Paint.rrect(-22, 36, 44, 20, 9), Color(1, 1, 1, 0.75))
		)
		_cabins.append(cab)
	_add(func(p: Paint):
		p.fill(Paint.circle(Vector2(cx, cy), 36), Paint.vgrad(Color("ffe27a"), Color("ffc23b"), cy - 36, cy + 36))
		p.fill(Paint.circle(Vector2(cx, cy), 16), Color("ff6f8f"))
		# ground
		_ground(p, 760, Color("ffe2bd"), Color("ffc790"))
		p.fill(Paint.rect(L, 756, R - L, 12), Color("b981d6"), 0.0)
		p.fill(Paint.ellipse(Vector2(cx, 1000), 900, 160), Color(1, 1, 1, 0.22))
		for i in 60:  # confetti
			var cp := Vector2(lerpf(L, R, Scenery.hash01(i + 400)), 790 + Scenery.hash01(i + 500) * 280)
			var cc: Color = [Color("ff7f9f"), Color("ffd84d"), Color("6cc0ff"), Color("8be07a"), Color("c7a2ff")][i % 5]
			p.fill(Paint.xform(Paint.rrect(-7, -3, 14, 6, 3), cp, Scenery.hash01(i) * TAU), Color(cc, 0.8))
		# stalls
		for k in 2:
			var sx := W * 0.1 if k == 0 else W * 0.9
			var body := Color("ff8fa3") if k == 0 else Color("6fc3ff")
			p.shadow(Paint.rrect(sx - 130, 570, 260, 190, 14), Vector2(0, 12), 22, 0.2)
			p.fill(Paint.rrect(sx - 130, 570, 260, 190, 14), Paint.vgrad(body.lightened(0.15), body.darkened(0.05), 570, 760))
			p.fill(Paint.rrect(sx - 100, 640, 200, 30, 12), Color(1, 1, 1, 0.9))
			for i in 6:
				var col := Color("ff5c73") if i % 2 == 0 else Color.WHITE
				var aw := Paint.rrect2(sx - 150 + i * 50, 520, 50, 70, 0, 22)
				p.shadow(aw, Vector2(0, 8), 12, 0.12)
				p.fill(aw, Paint.vgrad(col, col.darkened(0.08), 520, 590))
			var roof := PackedVector2Array([Vector2(sx - 156, 522), Vector2(sx, 466), Vector2(sx + 156, 522)])
			p.fill(Paint.blob(roof, 6), Paint.vgrad(Color("ffe27a"), Color("ffc23b"), 466, 530))
			for i in 7:
				p.glow(Vector2(sx - 120 + i * 40, 600), 16, Color(1, 0.95, 0.6, 0.85), 4)
	)
	var lights := _add(func(p: Paint):
		var a := Paint.quad(Vector2(L, 24), Vector2(W * 0.25, 140), Vector2(W * 0.5, 36), 24)
		var b := Paint.quad(Vector2(W * 0.5, 36), Vector2(W * 0.75, 140), Vector2(R, 24), 24)
		a.append_array(b)
		Scenery.string_lights(p, a, [Color("ffe066"), Color("ff8fa3"), Color("7fd8ff")], 2, 10)
	)
	_pulse.append([lights, 2.2, 0.0, 0.75, 1.0])
