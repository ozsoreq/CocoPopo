## Records vector drawing (paths, shapes, text) and writes it out as an SVG document.
## Coordinates are y-down. Transforms are similarity transforms (translate / rotate / uniform scale),
## so points are baked into the output and stroke widths are scaled with the transform.
extends RefCounted

const VPath := preload("res://tools/art/vpath.gd")
const FONT_PATH := "res://tools/art/DejaVuSans-Bold.ttf"
const GLYPH_SIZE := 1000  # glyph outlines are fetched big so font hinting doesn't snap them

var _out := PackedStringArray()
var _xf := Transform2D.IDENTITY
var _stack: Array[Transform2D] = []
var _grads := 0
var bounds := Rect2()
var has_bounds := false

static var _font: FontFile


# ---------------------------------------------------------------- transforms
func save() -> void:
	_stack.push_back(_xf)


func restore() -> void:
	_xf = _stack.pop_back()


func translate(x: float, y: float) -> void:
	_xf = _xf * Transform2D(0.0, Vector2(x, y))


## Degrees, clockwise (y-down), optionally around a pivot.
func rotate(deg: float, px := 0.0, py := 0.0) -> void:
	translate(px, py)
	_xf = _xf * Transform2D(deg_to_rad(deg), Vector2.ZERO)
	translate(-px, -py)


func scale(sx: float, sy: float) -> void:
	_xf = _xf * Transform2D(Vector2(sx, 0), Vector2(0, sy), Vector2.ZERO)


# ---------------------------------------------------------------- drawing
## Fills a path. Colours are 0xAARRGGBB ints.
func fill(p: VPath, col: int) -> void:
	if (col >> 24) & 255 == 0 or p.cmds.is_empty():
		return
	_add_bounds(p, 0.0)
	_out.append('<path d="%s"%s/>' % [_d(p), _paint("fill", col)])


func stroke(p: VPath, col: int, width: float) -> void:
	if (col >> 24) & 255 == 0 or p.cmds.is_empty():
		return
	var w := width * _scale()
	_add_bounds(p, w / 2)
	_out.append('<path d="%s" fill="none"%s stroke-width="%s" stroke-linecap="round" stroke-linejoin="round"/>'
		% [_d(p), _paint("stroke", col), _num(w)])


## Vertical linear gradient over a rectangle.
func gradient_rect(x: float, y: float, w: float, h: float, top: int, bottom: int) -> void:
	var p: VPath = VPath.new().rect(x, y, w, h)
	_add_bounds(p, 0.0)
	_grads += 1
	var a := _xf * Vector2(x, y)
	var b := _xf * Vector2(x, y + h)
	_out.append('<linearGradient id="g%d" gradientUnits="userSpaceOnUse" x1="%s" y1="%s" x2="%s" y2="%s">'
		% [_grads, _num(a.x), _num(a.y), _num(b.x), _num(b.y)]
		+ '<stop offset="0"%s/><stop offset="1"%s/></linearGradient>' % [_stop(top), _stop(bottom)])
	_out.append('<path d="%s" fill="url(#g%d)"/>' % [_d(p), _grads])


## Text as filled glyph outlines (bold sans). align: -1 left, 0 centre, 1 right; y is the baseline.
func text(s: String, x: float, y: float, size: float, col: int, align := 0, stroke_w := 0.0) -> void:
	var ts := TextServerManager.get_primary_interface()
	var rid: RID = _get_font().get_rids()[0]
	var k := size / GLYPH_SIZE
	var glyphs := []
	var width := 0.0
	for i in s.length():
		var gi := ts.font_get_glyph_index(rid, GLYPH_SIZE, s.unicode_at(i), 0)
		glyphs.append([gi, width])
		width += ts.font_get_glyph_advance(rid, GLYPH_SIZE, gi).x * k
	if align == 0:
		x -= width / 2
	elif align > 0:
		x -= width
	var p: VPath = VPath.new()
	for g in glyphs:
		_glyph_outline(p, ts.font_get_glyph_contours(rid, GLYPH_SIZE, g[0]), x + g[1], y, k)
	if stroke_w > 0:
		stroke(p, col, stroke_w)
	else:
		fill(p, col)


# ---------------------------------------------------------------- output
func is_empty() -> bool:
	return not has_bounds


## Full SVG document; the view box is the drawing's bounds plus padding, snapped to whole units.
func to_svg(pad: float) -> String:
	var r := view_box(pad)
	return '<svg xmlns="http://www.w3.org/2000/svg" width="%d" height="%d" viewBox="%d %d %d %d">\n%s\n</svg>\n' % [
		r.size.x, r.size.y, r.position.x, r.position.y, r.size.x, r.size.y, "\n".join(_out)]


func view_box(pad: float) -> Rect2i:
	var x := floori(bounds.position.x - pad)
	var y := floori(bounds.position.y - pad)
	return Rect2i(x, y, ceili(bounds.end.x + pad) - x, ceili(bounds.end.y + pad) - y)


# ---------------------------------------------------------------- internals
func _scale() -> float:
	return sqrt(absf(_xf.determinant()))


func _paint(attr: String, col: int) -> String:
	var s := ' %s="#%06X"' % [attr, col & 0xFFFFFF]
	var a := (col >> 24) & 255
	if a < 255:
		s += ' %s-opacity="%s"' % [attr, _num(a / 255.0)]
	return s


func _stop(col: int) -> String:
	return ' stop-color="#%06X" stop-opacity="%s"' % [col & 0xFFFFFF, _num(((col >> 24) & 255) / 255.0)]


static func _num(v: float) -> String:
	var r := roundi(v * 100)
	if r % 100 == 0:
		return str(r / 100)
	return str(r / 100.0)


func _pt(v: Vector2) -> String:
	var t := _xf * v
	return _num(t.x) + " " + _num(t.y)


func _d(p: VPath) -> String:
	var d := ""
	for c in p.cmds:
		match c[0]:
			"M": d += "M" + _pt(c[1])
			"L": d += "L" + _pt(c[1])
			"Q": d += "Q" + _pt(c[1]) + " " + _pt(c[2])
			"C": d += "C" + _pt(c[1]) + " " + _pt(c[2]) + " " + _pt(c[3])
			_: d += "Z"
	return d


## Bounds from points sampled along every segment (curves included), grown by the stroke half-width.
func _add_bounds(p: VPath, grow: float) -> void:
	var cur := Vector2.ZERO
	var start := Vector2.ZERO
	var pts := PackedVector2Array()
	for c in p.cmds:
		match c[0]:
			"M":
				cur = c[1]
				start = cur
				pts.append(cur)
			"L":
				cur = c[1]
				pts.append(cur)
			"Q":
				for i in range(1, 13):
					var t := i / 12.0
					pts.append(cur.lerp(c[1], t).lerp(c[1].lerp(c[2], t), t))
				cur = c[2]
			"C":
				for i in range(1, 17):
					pts.append(cur.bezier_interpolate(c[1], c[2], c[3], i / 16.0))
				cur = c[3]
			_:
				cur = start
	for v in pts:
		var t := _xf * v
		var r := Rect2(t - Vector2(grow, grow), Vector2(grow, grow) * 2)
		bounds = bounds.merge(r) if has_bounds else r
		has_bounds = true


static func _get_font() -> FontFile:
	if _font == null:
		_font = FontFile.new()
		_font.hinting = TextServer.HINTING_NONE
		_font.data = FileAccess.get_file_as_bytes(FONT_PATH)
	return _font


## TrueType/CFF contour decoding: tag 1 = on-curve point, 0 = quadratic control, 2 = cubic control.
static func _glyph_outline(p: VPath, g: Dictionary, ox: float, oy: float, k: float) -> void:
	var pts: PackedVector3Array = g.get("points", PackedVector3Array())
	var ends: PackedInt32Array = g.get("contours", PackedInt32Array())
	var first := 0
	for last in ends:
		var n := last - first + 1
		var P := func(i: int) -> Vector2:
			var q := pts[first + posmod(i, n)]
			return Vector2(ox + q.x * k, oy + q.y * k)
		var T := func(i: int) -> int: return int(pts[first + posmod(i, n)].z)
		# start on an on-curve point (or the midpoint of two quadratic controls)
		var s := 0
		while s < n and T.call(s) != 1:
			s += 1
		var start: Vector2 = P.call(s) if s < n else (P.call(0) + P.call(1)) / 2
		p.move_to(start.x, start.y)
		var i := 1
		while i <= n:
			var idx := s + i
			var tag: int = T.call(idx)
			if tag == 1:
				var v: Vector2 = P.call(idx)
				p.line_to(v.x, v.y)
				i += 1
			elif tag == 2:
				var c1: Vector2 = P.call(idx)
				var c2: Vector2 = P.call(idx + 1)
				var e: Vector2 = P.call(idx + 2)
				p.cubic_to(c1.x, c1.y, c2.x, c2.y, e.x, e.y)
				i += 3
			else:
				var c: Vector2 = P.call(idx)
				var e: Vector2
				if T.call(idx + 1) == 0:
					e = (c + P.call(idx + 1)) / 2
					i += 1
				else:
					e = P.call(idx + 1)
					i += 2
				p.quad_to(c.x, c.y, e.x, e.y)
		p.close()
		first = last + 1
