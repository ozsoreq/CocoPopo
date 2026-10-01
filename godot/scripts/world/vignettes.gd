class_name Vignettes
extends RefCounted
## The little buildings standing on the world map, one per place (about 220 x 220, origin at the
## bottom centre), painted by Godot with soft light and shadow.


static func draw(p: Paint, id: String) -> void:
	p.soft(Paint.ellipse(Vector2(0, -2), 118, 16), Scenery.SHADOW, 18, 0.18)
	match id:
		"home": _home(p)
		"school": _school(p)
		"hospital": _hospital(p)
		"market": _market(p)
		"cafe": _cafe(p)
		"park": _park(p)
		"beach": _beach(p)
		_: _fair(p)


static func _box(p: Paint, x: float, y: float, w: float, h: float, col: Color, r := 14.0) -> void:
	p.shadow(Paint.rrect(x, y, w, h, r), Vector2(0, 8), 14, 0.16)
	p.fill(Paint.rrect(x, y, w, h, r), Paint.Grad.new(Vector2(x, y), Vector2(x + w, y + h), col.lightened(0.14), col.darkened(0.06)))


static func _roof(p: Paint, cx: float, y: float, half: float, peak: float, col: Color) -> void:
	var roof := Paint.blob(PackedVector2Array([Vector2(cx - half, y), Vector2(cx, y - peak), Vector2(cx + half, y), Vector2(cx, y + 10)]), 6)
	p.shadow(roof, Vector2(0, 8), 12, 0.18)
	p.fill(roof, Paint.vgrad(col.lightened(0.18), col.darkened(0.04), y - peak, y + 10))


static func _win(p: Paint, x: float, y: float, w: float, h: float, lit := true) -> void:
	p.fill(Paint.rrect(x, y, w, h, 7), Paint.vgrad(Color("fff3b8") if lit else Color("bfe8ff"), Color("ffd66b") if lit else Color("8fd0ff"), y, y + h))
	p.fill(Paint.rect(x + w / 2 - 1.5, y, 3, h), Color(1, 1, 1, 0.7), 0.6)
	p.fill(Paint.rect(x, y + h / 2 - 1.5, w, 3), Color(1, 1, 1, 0.7), 0.6)


static func _door(p: Paint, cx: float, w: float, h: float, col: Color) -> void:
	p.fill(Paint.rrect2(cx - w / 2, -h, w, h, w / 2, 2), Paint.vgrad(col.lightened(0.1), col.darkened(0.1), -h, 0))
	p.fill(Paint.circle(Vector2(cx + w * 0.25, -h * 0.42), 3.5), Color("ffe066"))


static func _home(p: Paint) -> void:
	p.fill(Paint.rrect(36, -186, 24, 50, 5), Color("d9614f"))
	_box(p, -84, -112, 168, 112, Color("fff0d6"))
	_roof(p, 0, -104, 112, 92, Color("ff7d68"))
	_win(p, -66, -88, 38, 36)
	_win(p, 28, -88, 38, 36)
	_door(p, 0, 40, 66, Color("b07a4d"))
	Scenery.bush(p, 0.28)
	p.fill(Paint.circle(Vector2(-92, -12), 14), Color("6fd07c"))


static func _school(p: Paint) -> void:
	_box(p, -96, -104, 192, 104, Color("ffd98a"))
	_roof(p, 0, -100, 110, 56, Color("ff7d68"))
	p.fill(Paint.circle(Vector2(0, -120), 17), Color.WHITE)
	p.line(Vector2(0, -120), Vector2(0, -131), Color("4b4660"), 3)
	p.line(Vector2(0, -120), Vector2(8, -120), Color("4b4660"), 3)
	p.line(Vector2(0, -156), Vector2(0, -206), Color("a06a3c"), 5)
	p.fill(PackedVector2Array([Vector2(2, -206), Vector2(44, -194), Vector2(2, -180)]), Paint.hgrad(Color("ff8fa3"), Color("ff5c73"), 2, 44))
	for i in 2:
		_win(p, -80 + i * 122, -82, 38, 36, false)
	_door(p, 0, 42, 62, Color("6c7bff"))


static func _hospital(p: Paint) -> void:
	_box(p, -90, -136, 180, 136, Color("ffffff"))
	p.fill(Paint.rrect2(-90, -136, 180, 26, 14, 0), Paint.vgrad(Color("8fe6da"), Color("5fcdbd"), -136, -110))
	var red := Paint.vgrad(Color("ff8395"), Color("f04d63"), -104, -30)
	p.fill(Paint.rrect(-13, -104, 26, 74, 8), red)
	p.fill(Paint.rrect(-38, -80, 76, 26, 8), red)
	_win(p, -76, -92, 26, 26, false)
	_win(p, 50, -92, 26, 26, false)
	p.fill(Paint.rrect(-22, -28, 44, 28, 6), Paint.vgrad(Color("d8f3ff"), Color("9fd8f2"), -28, 0))


static func _market(p: Paint) -> void:
	_box(p, -90, -112, 180, 112, Color("fff3d6"))
	for i in 6:
		var col := Color("ff6f86") if i % 2 == 0 else Color.WHITE
		var aw := Paint.rrect2(-100 + i * 33.3, -150, 33.3, 48, 0, 15)
		p.shadow(aw, Vector2(0, 6), 8, 0.12)
		p.fill(aw, Paint.vgrad(col, col.darkened(0.06), -150, -102))
	p.fill(Paint.rrect(-74, -84, 148, 62, 10), Paint.vgrad(Color("d8f3ff"), Color("a6dcf5"), -84, -22))
	var fruit := [Color("ff5c6e"), Color("ffa040"), Color("7ed957")]
	for i in 3:
		p.fill(Paint.rrect(-62 + i * 44, -30, 36, 22, 5), Color("c98a52"))
		p.fill(Paint.circle(Vector2(-44 + i * 44, -36), 12), Paint.vgrad(fruit[i].lightened(0.2), fruit[i], -48, -24))


static func _cafe(p: Paint) -> void:
	_box(p, -90, -112, 180, 112, Color("eab88a"))
	for i in 6:
		var col := Color("5fd3c4") if i % 2 == 0 else Color.WHITE
		var aw := Paint.rrect2(-100 + i * 33.3, -142, 33.3, 44, 0, 15)
		p.shadow(aw, Vector2(0, 6), 8, 0.12)
		p.fill(aw, Paint.vgrad(col, col.darkened(0.06), -142, -98))
	p.fill(Paint.rrect(-30, -82, 60, 52, 14), Color.WHITE)
	p.stroke(Paint.ellipse(Vector2(34, -60), 12, 14, -PI / 2, PI / 2), Color.WHITE, 8)
	p.fill(Paint.ellipse(Vector2(0, -78), 24, 6), Color("8a5a30"))
	for i in 2:
		p.stroke(Paint.curve(PackedVector2Array([Vector2(-8 + i * 16, -92), Vector2(-14 + i * 16, -106), Vector2(-6 + i * 16, -120)])), Color(1, 1, 1, 0.8), 4)
	p.fill(Paint.rrect(-80, -22, 160, 22, 6), Color(1, 1, 1, 0.25))


static func _park(p: Paint) -> void:
	Scenery.tree(p, 0.42)
	p.fill(Paint.rrect(54, -42, 74, 12, 6), Paint.vgrad(Color("e6a76a"), Color("c48549"), -42, -30))
	p.fill(Paint.rrect(54, -60, 74, 10, 5), Color("d99a5c"))
	p.fill(Paint.rrect(60, -30, 8, 30, 4), Color("6b6480"))
	p.fill(Paint.rrect(114, -30, 8, 30, 4), Color("6b6480"))
	for f in [[-70, -8, Color("ff9fbf")], [-96, -14, Color("ffe066")], [-50, -16, Color.WHITE]]:
		Scenery.flower(p, Vector2(f[0], f[1]), 0.9, f[2])


static func _beach(p: Paint) -> void:
	p.fill(Paint.ellipse(Vector2(0, -12), 116, 24), Paint.vgrad(Color("fbe6b2"), Color("efcf8c"), -36, 12))
	for i in 2:
		p.stroke(Paint.ellipse(Vector2(-60 + i * 74, 8), 34, 7, PI * 1.1, PI * 1.9), Color("4dc9e6"), 5)
	p.line(Vector2(0, -12), Vector2(0, -150), Color("f3f0e8"), 7)
	for i in 6:
		var col := Color("ff6f86") if i % 2 == 0 else Color.WHITE
		p.fill(Paint.pie(Vector2(0, -112), 82, 62, PI + i * PI / 6, PI + (i + 1) * PI / 6), Paint.vgrad(col.lightened(0.1), col, -174, -112))
	p.fill(Paint.circle(Vector2(0, -174), 7), Color("f3f0e8"))
	p.fill(Paint.circle(Vector2(62, -24), 16), Color("ffd84d"))
	p.fill(Paint.pie(Vector2(62, -24), 16, 16, -PI / 2, PI / 6), Color("ff6f86"))


static func _fair(p: Paint) -> void:
	for side in [-1, 1]:
		p.line(Vector2(0, -104), Vector2(side * 46, 0), Color("b9aee8"), 9)
	p.ring(Vector2(0, -104), 70, Color("9d8cf0"), 8)
	var cols := [Color("ff6f8f"), Color("ffd43b"), Color("4fb3ff")]
	for i in 6:
		var a := deg_to_rad(i * 60 + 15)
		var c := Vector2(0, -104) + Vector2(cos(a), sin(a)) * 70
		p.line(Vector2(0, -104), c, Color("c3b8f5"), 4)
		p.fill(Paint.rrect(c.x - 11, c.y - 2, 22, 18, 7), cols[i % 3])
	p.fill(Paint.circle(Vector2(0, -104), 10), Color("ffd43b"))
	# little tent
	var tent := Paint.blob(PackedVector2Array([Vector2(56, 0), Vector2(86, -70), Vector2(116, 0)]), 5)
	p.shadow(tent, Vector2(0, 6), 8, 0.14)
	p.fill(tent, Paint.hgrad(Color("c7a2ff"), Color("9e74f0"), 56, 116))
