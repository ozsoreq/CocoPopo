class_name Scenery
extends RefCounted
## Reusable scenery painted by Godot in a soft, modern style: no hard outlines, gentle gradients,
## soft shadows and light. All functions draw around a local origin with a Paint.

const SHADOW := Color(0.18, 0.11, 0.24)
const LEAF := [Color("8fe08a"), Color("4fbf6b")]


# ================================================================ helpers
static func star(c: Vector2, r_out: float, r_in: float, n := 5, rot := -PI / 2) -> PackedVector2Array:
	var pts := PackedVector2Array()
	for i in n * 2:
		var a := rot + i * PI / n
		var r := r_out if i % 2 == 0 else r_in
		pts.append(c + Vector2(cos(a), sin(a)) * r)
	return pts


## Shape a minus shape b (e.g. a crescent moon).
static func cut(p: Paint, a: PackedVector2Array, b: PackedVector2Array, style: Variant) -> void:
	for poly in Geometry2D.clip_polygons(a, b):
		if not Geometry2D.is_polygon_clockwise(poly):
			p.fill(poly, style)


static func hash01(i: int) -> float:
	var v := sin(i * 127.1 + 311.7) * 43758.5453
	return v - floor(v)


## Soft darkening band (ambient occlusion) fading from edge_y over depth (negative depth fades upward).
static func occlusion(p: Paint, x: float, w: float, edge_y: float, depth: float, alpha := 0.12) -> void:
	var y0 := minf(edge_y, edge_y + depth)
	p.fill(Paint.rect(x, y0, w, absf(depth)), Paint.vgrad(Color(SHADOW, alpha), Color(SHADOW, 0.0), edge_y, edge_y + depth), 0.0)


# ================================================================ sky things
static func cloud(p: Paint, s := 1.0, tint := Color.WHITE) -> void:
	var g := Paint.vgrad(tint, tint.lerp(Color("cfe6ff"), 0.55), -60 * s, 40 * s)
	p.soft(Paint.rrect(-40 * s, 14 * s, 168 * s, 30 * s, 15 * s), Color(0.4, 0.55, 0.8), 18 * s, 0.08)
	for c in [[0, 0, 36], [42, -18, 46], [92, 0, 34], [64, 6, 30]]:
		p.fill(Paint.circle(Vector2(c[0], c[1]) * s, c[2] * s), g)
	p.fill(Paint.rrect(-36 * s, 0, 164 * s, 36 * s, 18 * s), g)


static func sun_core(p: Paint, r: float) -> void:
	p.glow(Vector2.ZERO, r * 2.6, Color(1, 0.93, 0.55, 0.45), r)
	p.fill(Paint.circle(Vector2.ZERO, r), Paint.Grad.new(Vector2(-r, -r), Vector2(r, r), Color("fff6b0"), Color("ffcf3f")))
	p.fill(Paint.circle(Vector2(-r * 0.32, -r * 0.34), r * 0.28), Color(1, 1, 1, 0.35))


static func sun_rays(p: Paint, r: float, n := 12, col := Color(1, 0.86, 0.35, 0.55)) -> void:
	for i in n:
		var a := i * TAU / n
		var d := Vector2(cos(a), sin(a))
		p.line(d * (r * 1.32), d * (r * 1.72), col, r * 0.22)


static func moon(p: Paint, r: float) -> void:
	p.glow(Vector2.ZERO, r * 2.4, Color(1, 0.97, 0.8, 0.25), r)
	cut(p, Paint.circle(Vector2.ZERO, r), Paint.circle(Vector2(r * 0.55, -r * 0.3), r * 0.85), Color("fff4c2"))


# ================================================================ nature
## Round friendly tree, origin at the bottom of the trunk.
static func tree(p: Paint, s := 1.0, kind := 0) -> void:
	p.soft(Paint.ellipse(Vector2(0, 0), 90 * s, 16 * s), SHADOW, 22 * s, 0.14)
	var trunk := Paint.Grad.new(Vector2(-20 * s, 0), Vector2(20 * s, 0), Color("b98157"), Color("8f5f3c"))
	p.fill(Paint.rrect2(-18 * s, -190 * s, 36 * s, 190 * s, 14 * s, 4 * s), trunk)
	if kind == 1:  # tall pine-ish cone stack
		for i in 3:
			var y := -150 - i * 70
			var w := 120 - i * 26
			var tri := Paint.xform(Paint.blob(PackedVector2Array([Vector2(-w, y + 40), Vector2(0, y - 90), Vector2(w, y + 40), Vector2(0, y + 56)]), 8), Vector2.ZERO, 0, Vector2(s, s))
			p.fill(tri, Paint.vgrad(LEAF[0], LEAF[1], (y - 90) * s, (y + 56) * s))
		return
	var blobs := [[0, -330, 104], [-86, -255, 72], [86, -258, 70], [-34, -392, 66], [56, -360, 60], [0, -250, 80]]
	var g := Paint.Grad.new(Vector2(-60, -440) * s, Vector2(60, -180) * s, LEAF[0], LEAF[1])
	for b in blobs:
		p.fill(Paint.circle(Vector2(b[0], b[1]) * s, b[2] * s), g)
	p.fill(Paint.circle(Vector2(-40, -380) * s, 30 * s), Color(1, 1, 1, 0.16))
	if kind == 2:  # blossoms
		for i in 7:
			p.fill(Paint.circle(Vector2(-80 + hash01(i) * 160, -400 + hash01(i + 9) * 180) * s, 9 * s), Color("ff8fb1"))


static func palm(p: Paint, s := 1.0) -> void:
	p.soft(Paint.ellipse(Vector2(10 * s, 0), 80 * s, 14 * s), SHADOW, 20 * s, 0.13)
	var trunk := PackedVector2Array()
	var left := Paint.quad(Vector2(-18, 0), Vector2(-6, -220), Vector2(34, -330))
	var right := Paint.quad(Vector2(60, -322), Vector2(24, -210), Vector2(22, 0))
	trunk.append_array(left); trunk.append_array(right)
	p.fill(Paint.xform(trunk, Vector2.ZERO, 0, Vector2(s, s)), Paint.hgrad(Color("d6a774"), Color("a87a4e"), -18 * s, 60 * s))
	for i in 6:
		var y := -40 - i * 48
		p.line(Vector2(-6 + i * 5, y) * s, Vector2(22 + i * 5, y + 8) * s, Color(0.45, 0.3, 0.18, 0.25), 5 * s)
	for i in 7:
		var a := deg_to_rad(-170 + i * 52)
		var leaf := Paint.blob(PackedVector2Array([Vector2(0, 0), Vector2(70, -42), Vector2(150, 6), Vector2(70, 10)]), 8)
		p.fill(Paint.xform(leaf, Vector2(46, -332) * s, a, Vector2(s, s)), LEAF[0] if i % 2 == 0 else LEAF[1])
	p.fill(Paint.circle(Vector2(36, -318) * s, 13 * s), Color("8a5a30"))
	p.fill(Paint.circle(Vector2(58, -312) * s, 13 * s), Color("7a4c27"))


static func bush(p: Paint, s := 1.0, berries := true) -> void:
	p.soft(Paint.ellipse(Vector2(0, 0), 120 * s, 14 * s), SHADOW, 18 * s, 0.13)
	var g := Paint.vgrad(Color("8be38c"), Color("48b765"), -140 * s, 0)
	for b in [[-62, -50, 52], [62, -50, 52], [0, -78, 62], [0, -42, 56]]:
		p.fill(Paint.circle(Vector2(b[0], b[1]) * s, b[2] * s), g)
	p.fill(Paint.circle(Vector2(-14, -110) * s, 22 * s), Color(1, 1, 1, 0.14))
	if berries:
		for b in [[-30, -86], [40, -66], [-66, -44], [18, -30]]:
			p.fill(Paint.circle(Vector2(b[0], b[1]) * s, 7 * s), Color("ff7f9f"))


static func flower(p: Paint, c: Vector2, s: float, col: Color) -> void:
	for i in 5:
		var a := i * TAU / 5
		p.fill(Paint.circle(c + Vector2(cos(a), sin(a)) * 7 * s, 6 * s), col)
	p.fill(Paint.circle(c, 5 * s), Color("ffe066"))


static func grass_tuft(p: Paint, c: Vector2, s: float, col: Color) -> void:
	for i in 3:
		var a := (i - 1) * 0.45
		p.line(c, c + Vector2(sin(a) * 18, -cos(a) * 26) * s, col, 5 * s)


# ================================================================ rooms
## Wall with gentle vertical light falloff. pattern: 0 plain, 1 stripes, 2 dots, 3 tiles, 4 panels.
static func wall(p: Paint, x: float, y: float, w: float, h: float, col: Color, pattern := 0) -> void:
	p.fill(Paint.rect(x, y, w, h), Paint.vgrad(col.lightened(0.08), col.darkened(0.03), y, y + h), 0.0)
	var light := Color(1, 1, 1, 0.22)
	match pattern:
		1:
			var px := x + 26
			while px < x + w - 10:
				p.fill(Paint.rect(px, y, 26, h), Color(1, 1, 1, 0.16), 0.0)
				px += 64
		2:
			var i := 0
			while 28 + i * 64 < w:
				var j := 0
				while 28 + j * 64 < h - 10:
					var c := Vector2(x + 28 + i * 64 + (j % 2) * 32, y + 28 + j * 64)
					if c.x < x + w - 8:
						p.fill(Paint.circle(c, 5), Color(1, 1, 1, 0.42))
					j += 1
				i += 1
		3:
			var px := x
			while px <= x + w:
				p.fill(Paint.rect(px - 1.5, y, 3, h), light, 0.0)
				px += 72
			var py := y
			while py <= y + h:
				p.fill(Paint.rect(x, py - 1.5, w, 3), light, 0.0)
				py += 72
		4:
			var px := x + 40
			while px + 180 < x + w:
				p.fill(Paint.rrect(px, y + h * 0.18, 180, h * 0.64, 14), Color(1, 1, 1, 0.12))
				px += 230
	occlusion(p, x, w, y, 60, 0.10)


## Floor from y to y + h. kind: 0 wood planks, 1 big tiles, 2 checker.
static func floor(p: Paint, x: float, y: float, w: float, h: float, col: Color, kind := 0, alt := Color.TRANSPARENT) -> void:
	p.fill(Paint.rect(x, y, w, h), Paint.vgrad(col.lightened(0.06), col.darkened(0.12), y, y + h), 0.0)
	match kind:
		0:
			var row := 0
			var py := y + 30
			while py < y + h:
				p.fill(Paint.rect(x, py, w, 2.5), Color(SHADOW, 0.10), 0.0)
				var sx := x + fmod(row * 173.0, 260.0)
				while sx < x + w:
					p.fill(Paint.rect(sx, py - 28, 2.5, 28), Color(SHADOW, 0.07), 0.0)  # plank ends
					sx += 260
				py += 30 + row % 2 * 6
				row += 1
		1, 2:
			var c2 := alt if alt.a > 0 else Color(1, 1, 1, 0.35)
			var i := 0
			while x + i * 120 < x + w:
				var j := 0
				while y + j * 70 < y + h:
					var tx := x + i * 120
					var ty := y + j * 70
					var tw := minf(120.0, x + w - tx)  # clip tiles to the floor
					var th := minf(70.0, y + h - ty)
					if kind == 2 and (i + j) % 2 == 0:
						p.fill(Paint.rect(tx, ty, tw, th), c2, 0.0)
					elif kind == 1:
						if tw >= 120:
							p.fill(Paint.rect(tx + 118, ty, 2.5, th), c2, 0.0)
						if th >= 70:
							p.fill(Paint.rect(tx, ty + 68, tw, 2.5), c2, 0.0)
					j += 1
				i += 1
	# soft sheen and the contact shadow under the wall
	p.soft(Paint.ellipse(Vector2(x + w * 0.5, y + h * 0.35), w * 0.32, h * 0.18), Color(1, 1, 1), h * 0.3, 0.07)
	occlusion(p, x, w, y, 26, 0.16)


## Skirting board along the bottom of a wall.
static func baseboard(p: Paint, x: float, y: float, w: float, col: Color) -> void:
	p.fill(Paint.rect(x, y - 18, w, 18), Paint.vgrad(col.lightened(0.25), col, y - 18, y), 0.0)
	p.fill(Paint.rect(x, y - 18, w, 2), Color(1, 1, 1, 0.5), 0.0)


static func _drape(p: Paint, x_out: float, x_in: float, y0: float, y1: float, col: Color) -> void:
	# curtain gathered at the window side, flaring at the bottom
	var mid := y0 + (y1 - y0) * 0.55
	var pts := PackedVector2Array([Vector2(x_out, y0)])
	pts.append_array(Paint.curve(PackedVector2Array([Vector2(x_in, y0), Vector2(lerpf(x_in, x_out, 0.42), mid), Vector2(lerpf(x_out, x_in, 0.78), y1)]), 10))
	pts.append(Vector2(x_out, y1))
	p.shadow(pts, Vector2(0, 8), 18, 0.12)
	p.fill(pts, Paint.vgrad(col.lightened(0.18), col.darkened(0.05), y0, y1))
	for k in 3:
		var fx := lerpf(x_out, x_in, 0.2 + k * 0.2)
		var fx2 := lerpf(x_out, lerpf(x_out, x_in, 0.78), 0.2 + k * 0.2)
		p.line(Vector2(fx, y0 + 10), Vector2(lerpf(fx, fx2, 0.9), y1 - 12), Color(SHADOW, 0.08), 6)
	var edge := lerpf(x_in, x_out, 0.42)  # curtain edge at the tie-back
	p.fill(Paint.rrect(minf(x_out, edge) - 4, mid - 8, absf(x_out - edge) + 8, 16, 8), col.darkened(0.18))


## A window with a little outdoor view, frame, reflections and optional curtains.
static func window(p: Paint, x: float, y: float, w: float, h: float, curtain := Color.TRANSPARENT, night := false) -> void:
	p.shadow(Paint.rrect(x - 16, y - 16, w + 32, h + 32, 24), Vector2(0, 12), 26, 0.16)
	p.fill(Paint.rrect(x - 16, y - 16, w + 32, h + 32, 24), Paint.vgrad(Color("ffffff"), Color("e9edf5"), y - 16, y + h + 16))
	var sky_top := Color("4a5aa8") if night else Color("7ccfff")
	var sky_bot := Color("8a7ad0") if night else Color("dff5ff")
	p.fill(Paint.rrect(x, y, w, h, 12), Paint.vgrad(sky_top, sky_bot, y, y + h))
	# a hill and a cloud through the glass
	var hill := PackedVector2Array([Vector2(x, y + h)])
	hill.append_array(Paint.curve(PackedVector2Array([Vector2(x, y + h * 0.78), Vector2(x + w * 0.45, y + h * 0.66), Vector2(x + w, y + h * 0.82)]), 8))
	hill.append(Vector2(x + w, y + h))
	p.fill(hill, Color("9fe39a") if not night else Color("5f7fa8"))
	if w > 100:
		p.fill(Paint.circle(Vector2(x + w * 0.3, y + h * 0.3), h * 0.07), Color(1, 1, 1, 0.9))
		p.fill(Paint.circle(Vector2(x + w * 0.38, y + h * 0.27), h * 0.09), Color(1, 1, 1, 0.9))
		p.fill(Paint.rrect(x + w * 0.24, y + h * 0.3, w * 0.24, h * 0.07, h * 0.035), Color(1, 1, 1, 0.9))
	# reflections
	p.fill(PackedVector2Array([Vector2(x + w * 0.58, y + 6), Vector2(x + w * 0.74, y + 6), Vector2(x + w * 0.3, y + h - 6), Vector2(x + w * 0.14, y + h - 6)]), Color(1, 1, 1, 0.16))
	p.fill(PackedVector2Array([Vector2(x + w * 0.8, y + 6), Vector2(x + w * 0.86, y + 6), Vector2(x + w * 0.42, y + h - 6), Vector2(x + w * 0.36, y + h - 6)]), Color(1, 1, 1, 0.12))
	# mullions
	p.fill(Paint.rrect(x + w / 2 - 5, y, 10, h, 5), Color("f4f6fb"))
	p.fill(Paint.rrect(x, y + h / 2 - 5, w, 10, 5), Color("f4f6fb"))
	occlusion(p, x, w, y, 16, 0.12)
	# sill
	p.shadow(Paint.rrect(x - 30, y + h + 10, w + 60, 18, 9), Vector2(0, 8), 14, 0.15)
	p.fill(Paint.rrect(x - 30, y + h + 10, w + 60, 18, 9), Paint.vgrad(Color("ffffff"), Color("dfe5ef"), y + h + 10, y + h + 28))
	if curtain.a > 0:
		_drape(p, x - 34, x + w * 0.24, y - 24, y + h + 24, curtain)
		_drape(p, x + w + 34, x + w * 0.76, y - 24, y + h + 24, curtain)
		p.fill(Paint.rrect(x - 52, y - 40, w + 104, 14, 7), Paint.vgrad(curtain.darkened(0.1), curtain.darkened(0.3), y - 40, y - 26))
		p.fill(Paint.circle(Vector2(x - 56, y - 33), 11), curtain.darkened(0.3))
		p.fill(Paint.circle(Vector2(x + w + 56, y - 33), 11), curtain.darkened(0.3))


## Pendant lamp hanging from y0 to the shade at y; warm light pool.
static func pendant(p: Paint, x: float, y0: float, y: float, col: Color, r := 56.0) -> void:
	p.glow(Vector2(x, y + 40), r * 4.2, Color(1, 0.92, 0.6, 0.22), r * 0.6)
	p.line(Vector2(x, y0), Vector2(x, y - r * 0.55), Color("6b6480"), 4)
	var shade := Paint.pie(Vector2(x, y + 8), r, r * 0.9, PI, TAU)
	p.shadow(shade, Vector2(0, 8), 14, 0.14)
	p.fill(shade, Paint.vgrad(col.lightened(0.25), col.darkened(0.1), y - r, y + 8))
	p.fill(Paint.ellipse(Vector2(x, y + 10), r * 0.5, 9), Color("fff7d0"))
	p.glow(Vector2(x, y + 12), r * 0.9, Color(1, 0.97, 0.75, 0.6), 6)


## Soft string of party lights along a curve.
static func string_lights(p: Paint, pts: PackedVector2Array, cols: Array, every := 3, bulb := 9.0) -> void:
	p.stroke(pts, Color("5b4a6e"), 3)
	var k := 0
	for i in range(every, pts.size() - 1, every):
		var c: Color = cols[k % cols.size()]
		var at := pts[i] + Vector2(0, bulb)
		p.glow(at, bulb * 3.4, Color(c, 0.35), bulb * 0.8)
		p.fill(Paint.ellipse(at, bulb * 0.8, bulb), c.lightened(0.15))
		k += 1
