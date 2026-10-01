## Backgrounds for each place (2560 x 1080), the small place vignettes on the world map, and the cloud.
## Moving bits (drifting clouds, the sun's rays turning, waves) are drawn at rest; the game animates them.
extends RefCounted

const Gfx := preload("res://tools/art/gfx.gd")
const VPath := preload("res://tools/art/vpath.gd")

const WHITE := 0xFFFFFFFF


static func background(g: Gfx, id: String, W: float, H: float) -> void:
	g.ol(5)
	g.shade = true
	match id:
		"home": _home(g, W, H)
		"school": _school(g, W, H)
		"hospital": _hospital(g, W, H)
		"market": _market(g, W, H)
		"cafe": _cafe(g, W, H)
		"park": _park(g, W, H)
		"beach": _beach(g, W, H)
		_: _fair(g, W, H)
	g.ol(0)
	g.shade = false


## Stable pseudo-random 0..1 for scattering decorations.
static func _hash(i: int) -> float:
	var v := sin(i * 127.1 + 311.7) * 43758.5453
	return v - floor(v)


static func cloud(g: Gfx, x: float, y: float, s: float, col: int) -> void:
	var o := g.olw
	g.ol(0)
	g.ci(x, y, 34 * s, col); g.ci(x + 38 * s, y - 14 * s, 42 * s, col); g.ci(x + 84 * s, y, 32 * s, col)
	g.rr(x - 34 * s, y, 150 * s, 34 * s, 17 * s, col)
	g.ol(o)


static func _window(g: Gfx, x: float, y: float, w: float, h: float, curtain: int) -> void:
	g.rr(x - 12, y - 12, w + 24, h + 24, 16, WHITE)
	g.rr(x, y, w, h, 8, 0xFFA8E2FF)
	g.ov(x + w * 0.3, y + h * 0.3, w * 0.18, h * 0.08, Gfx.al(WHITE, 220))
	g.ov(x + w * 0.7, y + h * 0.45, w * 0.14, h * 0.06, Gfx.al(WHITE, 200))
	g.rect(x + w / 2 - 4, y, 8, h, WHITE)
	g.rect(x, y + h / 2 - 4, w, 8, WHITE)
	if curtain != 0:
		g.poly(curtain, [x - 14, y - 22, x + w * 0.3, y - 22, x + w * 0.22, y + h * 0.5, x + w * 0.34, y + h + 8, x - 14, y + h + 8])
		g.poly(curtain, [x + w + 14, y - 22, x + w * 0.7, y - 22, x + w * 0.78, y + h * 0.5, x + w * 0.66, y + h + 8, x + w + 14, y + h + 8])
		g.rr(x - 24, y - 32, w + 48, 12, 6, Gfx.dk(curtain, 0.25))


## Wallpaper + floor; floor_y is where the wall ends. pattern: 0 plain, 1 stripes, 2 dots, 3 tiles.
static func _room(g: Gfx, x: float, y: float, w: float, floor_y: float, bottom: float, wall: int, pattern: int, floor_col: int) -> void:
	g.rect(x, y, w, floor_y - y, wall)
	if pattern == 1:
		var px := x + 30
		while px < x + w:
			g.rect(px, y, 22, floor_y - y, Gfx.al(WHITE, 55))
			px += 60
	if pattern == 2:
		for i in ceili(w / 56 + 1):
			for j in ceili((floor_y - y) / 56 + 1):
				g.ci(x + 28 + i * 56 + (j % 2) * 28, y + 28 + j * 56, 6, Gfx.al(WHITE, 80))
	if pattern == 3:
		var px := x
		while px < x + w:
			g.rect(px, y, 3, floor_y - y, Gfx.al(WHITE, 140))
			px += 70
		var py := y
		while py < floor_y:
			g.rect(x, py, w, 3, Gfx.al(WHITE, 140))
			py += 70
	g.rect(x, floor_y - 22, w, 22, Gfx.dk(wall, 0.10))
	g.grad(x, floor_y, w, bottom - floor_y, floor_col, Gfx.dk(floor_col, 0.14))
	var fy := floor_y + 34
	while fy < bottom:
		g.rect(x, fy, w, 3, Gfx.al(0xFF000000, 22))
		fy += 42
	if g.olw > 0:
		g.ln(x, floor_y, x + w, floor_y, 4, g.olc)
		g.rect_s(x, y, w, bottom - y, g.olc, g.olw * 2)


# ---------------------------------------------------------------- Home
static func _home(g: Gfx, W: float, H: float) -> void:
	g.grad(0, 0, W, H, 0xFF93DAFF, 0xFFE9F8FF)
	g.rect(0, 990, W, H - 990, 0xFF7ED67A)
	g.rect(0, 990, W, 14, 0xFF63C45F)
	for i in 14:
		g.ci(_hash(i) * W, 1010 + _hash(i + 40) * 60, 5, WHITE if i % 2 == 0 else 0xFFFFE066)
	var x0 := W * 0.05
	var x1 := W * 0.95
	var mid := x0 + (x1 - x0) * 0.56
	# roof
	g.poly(0xFFE0675A, [x0 - 40, 132, x1 + 40, 132, x1 - 130, 14, x0 + 130, 14])
	for i in 5:
		g.ln(x0 - 30 + i * 8, 132 - i * 24, x1 + 30 - i * 8, 132 - i * 24, 4, Gfx.al(0xFF000000, 26))
	g.rr(x0 - 56, 124, x1 - x0 + 112, 20, 10, 0xFFC24E44)
	# body
	g.rr(x0 - 14, 138, x1 - x0 + 28, 862, 18, 0xFFFFEBC9)
	# upper rooms
	var uy := 156.0
	var uf := 470.0
	var ub := 560.0
	_room(g, x0 + 6, uy, mid - x0 - 12, uf, ub, 0xFFDCCBFF, 2, 0xFFD9A066)
	_room(g, mid + 12, uy, x1 - mid - 18, uf, ub, 0xFFC8F0E6, 3, 0xFFB7E3F2)
	# lower rooms
	var ly := 604.0
	var lf := 910.0
	var lb := 990.0
	_room(g, x0 + 6, ly, mid - x0 - 12, lf, lb, 0xFFFFDDB5, 1, 0xFFD39A62)
	_room(g, mid + 12, ly, x1 - mid - 18, lf, lb, 0xFFFFF1B8, 0, 0xFFF3D9A4)
	# divider walls & slab
	g.rect(mid - 6, uy, 18, ub - uy, 0xFFFFEBC9)
	g.rect(mid - 6, ly, 18, lb - ly, 0xFFFFEBC9)
	g.rect(x0 - 14, ub, x1 - x0 + 28, ly - ub, 0xFFF2D3A2)
	g.rect(x0 - 14, ub + 18, x1 - x0 + 28, 6, Gfx.al(0xFF000000, 26))
	# windows & decor
	_window(g, x0 + 150, 230, 190, 190, 0xFFFF8FA3)
	_window(g, mid - 380, 640, 200, 200, 0xFF9BD6FF)
	_window(g, x1 - 170, 250, 120, 190, 0)
	# bedroom stars + moon
	g.ci(x0 + 480, 250, 40, 0xFFFFE680); g.ci(x0 + 498, 238, 36, 0xFFDCCBFF)
	for i in 5:
		g.ci(x0 + 400 + i * 54, 330 + (i % 2) * 40, 6, 0xFFFFE680)
	# bathroom mirror
	g.rr(mid + 150, 230, 130, 170, 40, WHITE)
	g.rr(mid + 160, 240, 110, 150, 32, 0xFFD2F1FF)
	# kitchen cabinets
	g.rr(x1 - 640, 640, 560, 110, 14, 0xFFF39AAA)
	for i in 4:
		g.rr(x1 - 632 + i * 138, 648, 130, 94, 10, 0xFFFFB3C0)
		g.ci(x1 - 590 + i * 138, 700, 6, WHITE)
	# living room string lights
	for i in 9:
		g.ci(x0 + 40 + i * 60, 636 + (i % 2) * 14, 8, [0xFFFF6F8F, 0xFFFFE066, 0xFF6FC3FF][i % 3])
	# ladder linking the two floors
	var lx := W * 0.5
	g.rr(lx - 50, 452, 100, 30, 10, 0xFF8A5A30)
	g.ln(lx - 34, 470, lx - 34, 985, 10, 0xFFC98A44); g.ln(lx + 34, 470, lx + 34, 985, 10, 0xFFC98A44)
	var ry := 500.0
	while ry < 980:
		g.ln(lx - 34, ry, lx + 34, ry, 8, 0xFFD9A066)
		ry += 52
	# house frame outline
	g.rrs(x0 - 14, 138, x1 - x0 + 28, 862, 18, 0xFFE5C48E, 8)


# ---------------------------------------------------------------- School
static func _school(g: Gfx, W: float, H: float) -> void:
	_room(g, 0, 0, W, 640, H, 0xFFBFE8D8, 1, 0xFFE0B27A)
	# bunting
	for i in ceili(W / 70):
		var x := 20 + i * 70
		g.poly([0xFFFF6F8F, 0xFFFFD43B, 0xFF4FB3FF, 0xFF7ED957][i % 4], [x, 0, x + 56, 0, x + 28, 70])
	# chalkboard
	var cx := W * 0.5
	g.rr(cx - 400, 130, 800, 360, 18, 0xFFC98A44)
	g.rr(cx - 384, 146, 768, 328, 10, 0xFF2F7A62)
	g.rr(cx - 400, 478, 800, 22, 8, 0xFFB07744)
	g.text("A B C", cx - 200, 260, 90, Gfx.al(WHITE, 235))
	g.text("1 + 2 = 3", cx + 160, 250, 70, Gfx.al(WHITE, 220))
	g.ln(cx - 340, 300, cx + 340, 300, 4, Gfx.al(WHITE, 90))
	g.ci(cx - 200, 390, 38, Gfx.al(0xFFFFE066, 230))
	for i in 8:
		var a := i * PI / 4
		g.ln(cx - 200 + cos(a) * 50, 390 + sin(a) * 50, cx - 200 + cos(a) * 68, 390 + sin(a) * 68, 5, Gfx.al(0xFFFFE066, 230))
	g.text("Hello!", cx + 140, 400, 80, Gfx.al(0xFFFF9EC4, 235))
	g.rr(cx - 380, 480, 60, 10, 5, WHITE)
	# windows
	_window(g, W * 0.84, 160, 220, 260, 0xFFFFB86B)
	_window(g, W * 0.08, 160, 220, 260, 0xFFFFB86B)
	# flag
	g.ln(W * 0.27, 110, W * 0.27, 230, 6, 0xFF8A5A30)
	g.poly(0xFFFF5C73, [W * 0.27, 114, W * 0.27 + 70, 134, W * 0.27, 160])


# ---------------------------------------------------------------- Hospital
static func _hospital(g: Gfx, W: float, H: float) -> void:
	_room(g, 0, 0, W, 660, H, 0xFFE4F6F4, 0, 0xFFE7EFF5)
	g.rect(0, 0, W, 96, 0xFF9ADFD6)
	g.rect(0, 90, W, 10, 0xFF7CCFC4)
	# checker floor
	for i in ceili(W / 120 + 1):
		for j in ceili((H - 660) / 70 + 1):
			if (i + j) % 2 == 0:
				g.rect(i * 120, 660 + j * 70, 120, 70, Gfx.al(0xFFB9D3E3, 140))
	# cross sign
	var cx := W * 0.5
	g.ci(cx, 250, 92, WHITE)
	g.rr(cx - 22, 190, 44, 120, 10, 0xFFFF5C73); g.rr(cx - 60, 228, 120, 44, 10, 0xFFFF5C73)
	_window(g, W * 0.27, 200, 200, 240, 0)
	_window(g, W * 0.84, 200, 200, 240, 0)
	# curtain rail and curtains
	g.rr(W * 0.03, 150, W * 0.94, 10, 5, 0xFFB0BED0)
	for k in 2:
		var x := W * 0.04 if k == 0 else W * 0.62
		var w := W * 0.17
		for i in 6:
			g.rr(x + i * (w / 6), 158, w / 6 + 2, 300, 16, 0xFFFFC1D6 if i % 2 == 0 else 0xFFFFA9C6)
	# heart monitor
	var mx := W * 0.38
	g.rr(mx, 330, 170, 120, 14, 0xFF4B4660)
	g.rr(mx + 10, 340, 150, 100, 8, 0xFF1F2A3A)
	g.path_s(VPath.new().move_to(mx + 20, 392).line_to(mx + 60, 392).line_to(mx + 74, 360).line_to(mx + 88, 424)
		.line_to(mx + 102, 392).line_to(mx + 150, 392), 0xFF7ED957, 5)


# ---------------------------------------------------------------- Market
static func _market(g: Gfx, W: float, H: float) -> void:
	_room(g, 0, 0, W, 690, H, 0xFFFFF1CF, 0, 0xFFF2E3C2)
	for i in ceili(W / 110 + 1):
		for j in ceili((H - 690) / 70 + 1):
			if (i + j) % 2 == 0:
				g.rect(i * 110, 690 + j * 70, 110, 70, Gfx.al(WHITE, 120))
	# awning
	for i in ceili(W / 90 + 1):
		var col := 0xFFFF5C73 if i % 2 == 0 else WHITE
		g.rect(i * 90, 0, 90, 56, col)
		g.ov(i * 90 + 45, 56, 45, 26, col)
	g.rr(W * 0.5 - 250, 90, 500, 90, 26, 0xFF58B368)
	g.rrs(W * 0.5 - 250, 90, 500, 90, 26, WHITE, 6)
	g.text("MARKET", W * 0.5, 160, 76, WHITE)
	# shelves
	var cols := [0xFFFF6F8F, 0xFF6FC3FF, 0xFFFFC93C, 0xFF7ED957, 0xFFB67CFF, 0xFFFF9A3D]
	for u in 3:
		var sx := W * (0.2 + u * 0.26)
		var sw := W * 0.24
		g.rr(sx, 230, sw, 440, 14, 0xFFE9EEF5)
		for r in 3:
			var sy := 240 + r * 140
			g.rr(sx + 10, sy, sw - 20, 126, 8, 0xFFD2DBE7)
			g.rr(sx + 6, sy + 122, sw - 12, 12, 5, 0xFFB0BED0)
			var x := sx + 20
			var k := 0
			while x < sx + sw - 50:
				var hh := 54 + int(_hash(u * 40 + r * 9 + k) * 40)
				var col: int = cols[(k + r + u) % 6]
				if (k + r) % 3 == 0:
					g.ci(x + 22, sy + 122 - 22, 22, col)
				else:
					g.rr(x, sy + 122 - hh, 40, hh, 8, col)
					g.rr(x + 6, sy + 122 - hh + 14, 28, 18, 4, Gfx.al(WHITE, 190))
				x += 48
				k += 1


# ---------------------------------------------------------------- Cafe
static func _cafe(g: Gfx, W: float, H: float) -> void:
	_room(g, 0, 0, W, 680, H, 0xFFFFE3C8, 2, 0xFFC38A5A)
	g.rect(0, 400, W, 280, 0xFFD9A06E)
	var px := 0.0
	while px < W:
		g.rect(px, 400, 4, 280, Gfx.al(0xFF000000, 25))
		px += 110
	g.rect(0, 396, W, 16, 0xFFB57B4E)
	# hanging lamps
	for i in 4:
		var x := W * (0.14 + i * 0.24)
		g.ln(x, 0, x, 130, 5, 0xFF5B5470)
		g.pie(x, 190, 64, 60, 180, 180, 0xFFFFC857)
		g.rr(x - 64, 186, 128, 12, 6, 0xFFE8A91F)
		g.ov(x, 215, 70, 14, Gfx.al(0xFFFFF2B0, 120))
	# menu board
	var mx := W * 0.04
	g.rr(mx, 240, 330, 250, 16, 0xFFB07744)
	g.rr(mx + 14, 254, 302, 222, 8, 0xFF37403F)
	g.text("MENU", mx + 165, 320, 54, Gfx.al(WHITE, 235))
	for i in 3:
		g.ln(mx + 40, 366 + i * 40, mx + 200, 366 + i * 40, 5, Gfx.al(WHITE, 160))
		g.text("%d.5" % (2 + i), mx + 260, 378 + i * 40, 32, Gfx.al(0xFFFFE066, 235))
	# window with awning
	var wx := W * 0.3
	_window(g, wx, 240, 260, 200, 0)
	for i in 6:
		g.poly(0xFFFF6F8F if i % 2 == 0 else WHITE, [wx - 30 + i * 53, 200, wx - 30 + (i + 1) * 53, 200, wx - 30 + (i + 1) * 53, 250, wx - 30 + i * 53, 250])
	# counter
	var cx := W * 0.62
	var cw := W * 0.34
	g.rr(cx, 440, cw, 250, 14, 0xFFFF9A7A)
	g.rr(cx - 14, 420, cw + 28, 36, 14, 0xFFFFE9C9)
	for i in 5:
		g.rr(cx + 20 + i * (cw - 40) / 5, 480, (cw - 60) / 5, 190, 8, 0xFFFFB59C)
	# pastry case
	g.rr(cx + 20, 330, 220, 90, 14, Gfx.al(0xFFD2F1FF, 200))
	for i in 4:
		g.ci(cx + 52 + i * 50, 394, 18, 0xFFF3A649)
		g.ci(cx + 52 + i * 50, 384, 10, 0xFFFF9EC4 if i % 2 == 0 else WHITE)
	# coffee machine
	g.rr(cx + cw - 190, 340, 150, 84, 12, 0xFFB4BFCD)
	g.rr(cx + cw - 190, 340, 150, 24, 12, 0xFF6B6480)
	g.rr(cx + cw - 140, 380, 50, 44, 6, 0xFF4B4660)


# ---------------------------------------------------------------- Park
static func _park(g: Gfx, W: float, H: float) -> void:
	g.grad(0, 0, W, 620, 0xFF8ADBFF, 0xFFE3F8FF)
	g.ol(0)
	# sun
	var sx := W * 0.84
	for i in 12:
		g.save(); g.translate(sx, 150); g.rotate(i * 30)
		g.rr(-10, -126, 20, 40, 10, Gfx.al(0xFFFFE066, 170))
		g.restore()
	g.ci(sx, 150, 70, 0xFFFFE066)
	g.ov(W * 0.25, 640, W * 0.5, 170, 0xFF9DE28B)
	g.ov(W * 0.8, 650, W * 0.45, 150, 0xFF86D87A)
	g.grad(0, 600, W, H - 600, 0xFF84D974, 0xFF5FC25B)
	# path
	g.poly(0xFFF3E1B5, [W * 0.46, 640, W * 0.54, 640, W * 0.78, H, W * 0.22, H])
	g.ol(5)
	# fence
	var fx := 10.0
	while fx < W:
		g.rr(fx, 566, 30, 90, 10, WHITE)
		fx += 54
	g.rr(0, 590, W, 14, 6, 0xFFF0EDE6); g.rr(0, 628, W, 14, 6, 0xFFF0EDE6)
	g.ol(0)
	# pond
	var px := W * 0.86
	g.ov(px, 850, 250, 86, 0xFFBDEBFF)
	g.ov(px, 850, 230, 74, 0xFF6EC8F5)
	g.ov(px - 60, 840, 70, 18, Gfx.al(WHITE, 90))
	g.ov(px + 70, 872, 34, 14, 0xFF4FBF6B); g.ov(px - 20, 880, 28, 11, 0xFF5BD07A)
	g.ci(px + 74, 866, 7, 0xFFFF8FC0)
	g.ol(5)
	for i in 22:
		g.ci(_hash(i + 1) * W, 700 + _hash(i + 70) * 360, 6, [WHITE, 0xFFFFE066, 0xFFFF8FC0][i % 3])


# ---------------------------------------------------------------- Beach
static func _beach(g: Gfx, W: float, H: float) -> void:
	g.ol(0)
	g.grad(0, 0, W, 470, 0xFF7FD8FF, 0xFFFFF4D6)
	for i in 12:
		g.save(); g.translate(W * 0.8, 150); g.rotate(i * 30)
		g.rr(-9, -122, 18, 36, 9, Gfx.al(0xFFFFE066, 150))
		g.restore()
	g.ci(W * 0.8, 150, 68, 0xFFFFE066)
	g.grad(0, 470, W, 270, 0xFF4DC9E6, 0xFF2AA3D4)
	g.ol(5)
	# boat
	var bx := W * 0.3
	var by := 520.0
	g.poly(WHITE, [bx, by - 110, bx, by - 8, bx + 74, by - 8])
	g.poly(0xFFFF6F8F, [bx - 8, by - 90, bx - 8, by - 8, bx - 56, by - 8])
	g.poly(0xFF8A5A30, [bx - 70, by - 6, bx + 90, by - 6, bx + 66, by + 22, bx - 46, by + 22])
	# waves
	for i in 7:
		var wy := 560 + i * 36
		var x := -200.0 + (i * 97) % 220
		while x < W:
			g.arc(x, wy, 50, 10, 200, 140, 5, Gfx.al(WHITE, 140))
			x += 220
	g.ol(0)
	# sand (two wavy layers)
	for layer in 2:
		var off := 36.0 * layer
		var p: VPath = VPath.new().move_to(0, 740 + off)
		for i in 9:
			p.quad_to(W * (i + 0.5) / 8, 700 + (i % 2) * 50 + off, W * (i + 1) / 8, 740 + off)
		p.line_to(W, H).line_to(0, H).close()
		g.path(p, 0xFFF7DFA4 if layer == 1 else 0xFFE8C98A)
	for i in 18:
		g.ci(_hash(i + 5) * W, 860 + _hash(i + 33) * 200, 4 + _hash(i) * 4, Gfx.al(0xFFC9A257, 140))
	# starfish
	g.save(); g.translate(W * 0.46, 1030)
	for i in 5:
		g.rotate(72)
		g.rr(-7, -34, 14, 34, 7, 0xFFFF8F5C)
	g.restore()


# ---------------------------------------------------------------- Funfair
static func _fair(g: Gfx, W: float, H: float) -> void:
	g.ol(0)
	g.grad(0, 0, W, 330, 0xFF5E4AE3, 0xFFC864D8)
	g.grad(0, 330, W, 440, 0xFFC864D8, 0xFFFFB27A)
	for i in 36:
		g.ci(_hash(i) * W, _hash(i + 50) * 300, 2 + _hash(i + 7) * 3, Gfx.al(WHITE, 120 + int(100 * sin(i))))
	g.ol(5)
	# ferris wheel
	var cx := W * 0.5
	var cy := 400.0
	var r := 270.0
	g.ln(cx, cy, cx - 150, 900, 18, 0xFFE8EAF6); g.ln(cx, cy, cx + 150, 900, 18, 0xFFE8EAF6)
	g.cis(cx, cy, r, WHITE, 14)
	g.cis(cx, cy, r * 0.55, Gfx.al(WHITE, 200), 8)
	for i in 12:
		var a := deg_to_rad(i * 30)
		var px := cx + cos(a) * r
		var py := cy + sin(a) * r
		g.ln(cx, cy, px, py, 6, Gfx.al(WHITE, 220))
		g.ln(px, py, px, py + 30, 5, 0xFFE8EAF6)
		g.rr(px - 30, py + 28, 60, 52, 16, [0xFFFF5C73, 0xFFFFD43B, 0xFF4FB3FF, 0xFF7ED957][i % 4])
		g.rr(px - 22, py + 36, 44, 22, 8, Gfx.al(WHITE, 190))
	g.ci(cx, cy, 34, 0xFFFFD43B); g.ci(cx, cy, 16, 0xFFFF5C73)
	g.ol(0)
	# ground
	g.grad(0, 760, W, H - 760, 0xFFFFE0B8, 0xFFFFC98C)
	g.rect(0, 756, W, 12, 0xFFB86BD0)
	g.ol(5)
	# stalls
	for k in 2:
		var sx := W * 0.1 if k == 0 else W * 0.9
		g.rr(sx - 130, 570, 260, 190, 10, 0xFFFF8FA3 if k == 0 else 0xFF6FC3FF)
		for i in 6:
			g.poly(0xFFFF5C73 if i % 2 == 0 else WHITE, [sx - 150 + i * 50, 520, sx - 100 + i * 50, 520, sx - 100 + i * 50, 590, sx - 150 + i * 50, 590])
		g.poly(0xFFFFD43B, [sx - 150, 520, sx + 150, 520, sx, 470])
		g.rr(sx - 100, 640, 200, 30, 8, WHITE)
	g.ol(0)
	# string lights
	g.path_s(VPath.new().move_to(0, 30).quad_to(W * 0.25, 130, W * 0.5, 40).quad_to(W * 0.75, 130, W, 30), 0xFF3A2F5A, 4)
	for i in 22:
		var u := i / 21.0
		g.ci(W * u, 30 + 46 * absf(sin(u * TAU)) + 8, 10, [0xFFFFE066, 0xFFFF8FA3, 0xFF7FD8FF][i % 3])


# ---------------------------------------------------------------- map vignettes
## Small picture of a place (about 200 x 200, origin at the bottom centre).
static func place_icon(g: Gfx, id: String) -> void:
	match id:
		"home":
			g.rr(-80, -110, 160, 110, 10, 0xFFFFEBC9)
			g.poly(0xFFE0675A, [-100, -104, 100, -104, 0, -190])
			g.rr(-20, -70, 40, 70, 8, 0xFF8A5A30)
			g.rr(-70, -90, 36, 36, 6, 0xFFA8E2FF); g.rr(34, -90, 36, 36, 6, 0xFFA8E2FF)
			g.rr(40, -176, 24, 44, 4, 0xFFC24E44)
		"school":
			g.rr(-90, -100, 180, 100, 8, 0xFFFFD98A)
			g.poly(0xFFE0675A, [-100, -100, 100, -100, 0, -150])
			g.rr(-20, -60, 40, 60, 8, 0xFF8A5A30)
			for i in 2:
				g.rr(-78 + i * 118, -80, 36, 36, 6, 0xFFA8E2FF)
			g.rr(-3, -200, 6, 54, 3, 0xFF8A5A30); g.poly(0xFFFF5C73, [3, -200, 44, -186, 3, -172])
		"hospital":
			g.rr(-86, -130, 172, 130, 10, WHITE)
			g.rr(-86, -130, 172, 24, 10, 0xFF6DD3C8)
			g.rr(-14, -108, 28, 80, 6, 0xFFFF5C73); g.rr(-40, -82, 80, 28, 6, 0xFFFF5C73)
			g.rr(-20, -34, 40, 34, 6, 0xFFBDE9FF)
			g.rr(-74, -90, 22, 22, 4, 0xFFA8E2FF); g.rr(52, -90, 22, 22, 4, 0xFFA8E2FF)
		"market":
			g.rr(-86, -110, 172, 110, 8, 0xFFFFF1CF)
			for i in 6:
				g.poly(0xFFFF5C73 if i % 2 == 0 else WHITE, [-96 + i * 32, -150, -64 + i * 32, -150, -64 + i * 32, -100, -96 + i * 32, -100])
			g.rr(-70, -80, 140, 60, 8, 0xFFA8E2FF)
			g.ci(-34, -46, 14, 0xFFFF4D5E); g.ci(0, -46, 14, 0xFFFF9A3D); g.ci(34, -46, 14, 0xFF7ED957)
		"cafe":
			g.rr(-86, -110, 172, 110, 10, 0xFFE9B889)
			for i in 6:
				g.poly(0xFF6DD3C8 if i % 2 == 0 else WHITE, [-96 + i * 32, -140, -64 + i * 32, -140, -64 + i * 32, -100, -96 + i * 32, -100])
			g.rr(-26, -78, 52, 50, 12, WHITE)
			g.ln(26, -66, 40, -58, 7, WHITE); g.ln(40, -58, 26, -42, 7, WHITE)
			for i in 2:
				g.ln(-8 + i * 16, -86, -14 + i * 16 + sin(i) * 3, -112, 4, Gfx.al(WHITE, 220))
		"park":
			g.rr(-12, -90, 24, 90, 8, 0xFF9B6B43)
			g.ci(0, -130, 56, 0xFF4FBF6B); g.ci(-40, -100, 36, 0xFF57CB74); g.ci(40, -100, 36, 0xFF45B561)
			g.ci(-18, -150, 8, 0xFFFF5C73); g.ci(28, -120, 8, 0xFFFF5C73)
			g.rr(56, -40, 70, 12, 5, 0xFFC98A44); g.rr(60, -28, 8, 28, 3, 0xFF6B6480); g.rr(112, -28, 8, 28, 3, 0xFF6B6480)
		"beach":
			g.ov(0, -10, 110, 22, 0xFFF7DFA4)
			g.rr(-4, -150, 8, 150, 3, 0xFFF3F0E8)
			for i in 6:
				g.pie(0, -110, 78, 60, 180 + i * 30, 30, 0xFFFF5C73 if i % 2 == 0 else WHITE)
			for i in 2:
				g.arc(-60 + i * 70, 6, 40, 8, 200, 140, 5, 0xFF4DC9E6)
		_:  # funfair
			g.cis(0, -100, 70, WHITE, 8)
			for i in 6:
				var a := deg_to_rad(i * 60)
				g.ln(0, -100, cos(a) * 70, -100 + sin(a) * 70, 4, WHITE)
				g.ci(cos(a) * 70, -100 + sin(a) * 70, 11, [0xFFFF5C73, 0xFFFFD43B, 0xFF4FB3FF][i % 3])
			g.ln(0, -100, -46, 0, 10, 0xFFE8EAF6); g.ln(0, -100, 46, 0, 10, 0xFFE8EAF6)
			g.ci(0, -100, 10, 0xFFFFD43B)
