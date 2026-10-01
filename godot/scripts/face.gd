extends Node2D
## Draws a character's face live (eyes that look around and blink, brows, mouth and expressions).
## Coordinates follow the head art: origin at the neck, face centre around y = -66.

const INK := Color("2b2230")
const MOUTH := Color("7a2b3a")
const TONGUE := Color("ff7c8e")
const OUTLINE := Color("2a1f2e")

enum { F_NONE, F_HAPPY, F_LAUGH, F_WOW, F_YUM, F_SAD, F_SLEEPY, F_LOVE, F_SCARED }

var ch  # the Character

func _ell(c: Vector2, rx: float, ry: float, col: Color, seg := 28) -> void:
	var pts := PackedVector2Array()
	for i in seg:
		var a := TAU * i / seg
		pts.append(c + Vector2(cos(a) * rx, sin(a) * ry))
	draw_colored_polygon(pts, col)
	pts.append(pts[0])
	draw_polyline(pts, col, 1.2, true)

func _arc(c: Vector2, rx: float, ry: float, start_deg: float, sweep_deg: float, w: float, col: Color) -> void:
	var pts := PackedVector2Array()
	var n := 16
	for i in n + 1:
		var a := deg_to_rad(start_deg + sweep_deg * i / n)
		pts.append(c + Vector2(cos(a) * rx, sin(a) * ry))
	draw_polyline(pts, col, w, true)
	draw_circle(pts[0], w * 0.5, col)
	draw_circle(pts[n], w * 0.5, col)

func _pie(c: Vector2, rx: float, ry: float, start_deg: float, sweep_deg: float, col: Color) -> void:
	var pts := PackedVector2Array([c])
	var n := 18
	for i in n + 1:
		var a := deg_to_rad(start_deg + sweep_deg * i / n)
		pts.append(c + Vector2(cos(a) * rx, sin(a) * ry))
	draw_colored_polygon(pts, col)
	pts.append(c)
	draw_polyline(pts, col, 1.2, true)

func _rr(r: Rect2, col: Color) -> void:
	draw_rect(r, col)

func _draw() -> void:
	if ch == null:
		return
	var l: Look = ch.look
	var skin := Look.col(Look.SKIN[l.skin])
	var hair := Look.col(Look.HAIR[l.hair_color])
	var face: int = F_SLEEPY if ch.sleep else ch.face
	var blink: float = 1.0 if ch.sleep else ch.blink
	var look: float = ch.look_amt
	var oy := 196.0  # java head coords -> local (neck at origin)

	# cheeks + freckles
	var blush := 0.6 if face in [F_LOVE, F_LAUGH] else 0.33
	_ell(Vector2(-52, -236 + oy), 17, 11, Color(1, 0.43, 0.56, blush))
	_ell(Vector2(52, -236 + oy), 17, 11, Color(1, 0.43, 0.56, blush))
	if l.freckles == 1:
		for s in [-1, 1]:
			for i in 3:
				_ell(Vector2(s * (40 + i * 10), -244 + (i % 2) * 7 + oy), 2.6, 2.6, skin.darkened(0.3))

	var style: int = l.eyes
	if face in [F_LAUGH, F_YUM]:
		style = 1
	elif face == F_SLEEPY:
		style = 3
	var gx := clampf(ch.gaze_x + look * 0.6, -1, 1)
	var gy := clampf(ch.gaze_y, -1, 1)
	var brow_lift := 0.0
	var brow_tilt := 0.0
	match face:
		F_WOW: brow_lift = -10
		F_SCARED: brow_lift = -12; brow_tilt = -12
		F_SAD: brow_lift = -4; brow_tilt = -18
		F_LAUGH: brow_lift = -6
		F_SLEEPY: brow_lift = 4

	for s in [-1, 1]:
		var e := Vector2(s * 35 + look * 6, -262 + oy)
		if face == F_LOVE:
			var pts := PackedVector2Array()
			for i in 24:
				var t := TAU * i / 24.0
				pts.append(e + Vector2(16 * pow(sin(t), 3), -(13 * cos(t) - 5 * cos(2 * t) - 2 * cos(3 * t) - cos(4 * t))) * 0.9)
			draw_colored_polygon(pts, Color("ff4d6d"))
		elif face in [F_WOW, F_SCARED]:
			_ell(e, 21, 24, OUTLINE)
			_ell(e, 18, 21, Color.WHITE)
			_ell(e + Vector2(gx * 5, gy * 4), 5 if face == F_SCARED else 7, 5 if face == F_SCARED else 7, INK)
		else:
			match style:
				0:
					var ry := 14.0 * (1 - 0.85 * blink)
					_ell(e + Vector2(gx * 4, gy * 3), 11, ry, INK)
					if blink < 0.5:
						_ell(e + Vector2(gx * 4 + 3.5, -5), 4, 4, Color.WHITE)
				1:
					_arc(e + Vector2(0, 6), 13, 13, 200, 140, 6, INK)
				2:
					var ry2 := 19.0 * (1 - 0.85 * blink)
					_ell(e + Vector2(gx * 4, gy * 3), 15, ry2, INK)
					if blink < 0.5:
						_ell(e + Vector2(gx * 4 + 5, -7), 6, 6, Color.WHITE)
						_ell(e + Vector2(gx * 4 - 5, 6), 3, 3, Color.WHITE)
					draw_line(e + Vector2(s * 12, -12), e + Vector2(s * 20, -17), INK, 4, true)
				3:
					_ell(e + Vector2(0, 2), 12, 12 * (1 - 0.7 * blink), INK)
					_rr(Rect2(e.x - 15, e.y - 15, 30, 15), skin)
					draw_line(e + Vector2(-14, -1), e + Vector2(14, -1), skin.darkened(0.35), 4, true)
				4:
					if s < 0:
						_ell(e, 11, 14 * (1 - 0.85 * blink), INK)
						if blink < 0.5:
							_ell(e + Vector2(3.5, -5), 4, 4, Color.WHITE)
					else:
						_arc(e + Vector2(0, 4), 12, 10, 200, 140, 6, INK)
				_:
					_ell(e, 20.5, 22.5, OUTLINE)
					_ell(e, 17, 19, Color.WHITE)
					_ell(e + Vector2(gx * 6, gy * 5 + 2), 10, 12, INK)
					_ell(e + Vector2(gx * 6 + 4, gy * 5 - 3), 3.5, 3.5, Color.WHITE)
					if blink > 0.05:
						_rr(Rect2(e.x - 21, e.y - 23, 42, 44 * blink), skin)
						draw_line(Vector2(e.x - 17, e.y - 23 + 42 * blink), Vector2(e.x + 17, e.y - 23 + 42 * blink), skin.darkened(0.35), 4, true)
		# brows
		draw_set_transform(e + Vector2(0, -26 + brow_lift), deg_to_rad(s * brow_tilt), Vector2.ONE)
		_arc(Vector2(0, 4), 14, 8, 205, 130, 5, hair.darkened(0.05))
		draw_set_transform(Vector2.ZERO, 0, Vector2.ONE)

	# nose
	_arc(Vector2(0, -246 + oy), 7, 5, 20, 140, 4, skin.darkened(0.22))

	# mouth
	var m: int = l.mouth
	match face:
		F_HAPPY: m = 1
		F_LAUGH: m = 5
		F_WOW: m = 3
		F_YUM: m = 4
		F_SAD: m = 6
		F_SLEEPY: m = 3
		F_LOVE: m = 0
		F_SCARED: m = 7
	if ch.chew_t > 0:
		m = 3 if sin(ch.anim_t * 16) > 0 else 2
	var my := -222.0 + oy
	match m:
		0: _arc(Vector2(0, my - 6), 20, 14, 30, 120, 5, MOUTH)
		1:
			_pie(Vector2(0, my - 8), 24, 24, 0, 180, MOUTH)
			_pie(Vector2(0, my + 2), 13, 11, 0, 180, TONGUE)
			_rr(Rect2(-17, my - 8, 34, 6), Color.WHITE)
		2: draw_line(Vector2(-12, my), Vector2(12, my), MOUTH, 5, true)
		3:
			var small: bool = face == F_SLEEPY or ch.sleep
			_ell(Vector2(0, my + 2), 6 if small else 9, 7 if small else 12, MOUTH)
		4:
			_arc(Vector2(0, my - 6), 22, 14, 20, 140, 5, MOUTH)
			_ell(Vector2(9.5, my + 14), 7.5, 10, TONGUE)
		6: _arc(Vector2(0, my + 8), 18, 12, 200, 140, 5, MOUTH)
		7:
			var pts := PackedVector2Array()
			for i in 13:
				pts.append(Vector2(-18 + i * 3, my + sin(i * PI / 3) * -5))
			draw_polyline(pts, MOUTH, 5, true)
		_:
			_pie(Vector2(0, my - 12), 30, 32, 0, 180, MOUTH)
			_rr(Rect2(-26, my - 12, 52, 12), Color.WHITE)
