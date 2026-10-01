## Drawing helpers for the flat "toy" look: shapes get a dark outline and soft puffy shading when the
## outline width (olw) is on. Colours are 0xAARRGGBB ints.
extends RefCounted

const VPath := preload("res://tools/art/vpath.gd")
const SvgCanvas := preload("res://tools/art/svg_canvas.gd")

var c: SvgCanvas
var olw := 0.0               ## outline width in local units (0 = off)
var olc := 0xFF2A1F2E        ## outline colour
var shade := true            ## soft highlight / shadow on big outlined shapes


func _init(canvas: SvgCanvas) -> void:
	c = canvas


func ol(w: float) -> void:
	olw = w


# ---------------------------------------------------------------- colours
static func dk(col: int, f: float) -> int:
	var r := int(((col >> 16) & 255) * (1 - f))
	var g := int(((col >> 8) & 255) * (1 - f))
	var b := int((col & 255) * (1 - f))
	return (col & 0xFF000000) | (r << 16) | (g << 8) | b


static func lt(col: int, f: float) -> int:
	var r := (col >> 16) & 255
	var g := (col >> 8) & 255
	var b := col & 255
	r = int(r + (255 - r) * f)
	g = int(g + (255 - g) * f)
	b = int(b + (255 - b) * f)
	return (col & 0xFF000000) | (r << 16) | (g << 8) | b


static func al(col: int, a: int) -> int:
	return (col & 0x00FFFFFF) | (a << 24)


static func mix(a: int, b: int, t: float) -> int:
	var ar := (a >> 16) & 255
	var ag := (a >> 8) & 255
	var ab := a & 255
	var br := (b >> 16) & 255
	var bg := (b >> 8) & 255
	var bb := b & 255
	return 0xFF000000 | (int(ar + (br - ar) * t) << 16) | (int(ag + (bg - ag) * t) << 8) | int(ab + (bb - ab) * t)


# ---------------------------------------------------------------- transforms
func save() -> void: c.save()
func restore() -> void: c.restore()
func translate(x: float, y: float) -> void: c.translate(x, y)
func rotate(deg: float, px := 0.0, py := 0.0) -> void: c.rotate(deg, px, py)
func scale(sx: float, sy: float) -> void: c.scale(sx, sy)


# ---------------------------------------------------------------- shapes
## Outlines go on shapes that are big, opaque and not too dark.
func _want_ol(min_dim: float, col: int) -> bool:
	if olw <= 0 or min_dim < 22 or (col >> 24) & 255 != 255:
		return false
	var lum := int(((col >> 16) & 255) * 0.3 + ((col >> 8) & 255) * 0.6 + (col & 255) * 0.1)
	return lum > 48


func _outline(p: VPath) -> void:
	c.stroke(p, olc, olw * 2)


## Light crown + soft bottom shadow so flat shapes read as slightly puffy.
func _shade_rr(x: float, y: float, w: float, h: float, r: float, col: int) -> void:
	if not shade or olw <= 0 or minf(w, h) < 46:
		return
	var inset := minf(w, h) * 0.09
	var cw := w - inset * 2
	var ch := h * 0.46 - inset * 0.7
	var rr_ := minf(r, minf(cw, ch) / 2)
	c.fill(VPath.new().round_rect(x + inset, y + inset * 0.7, cw, ch, rr_), lt(col, 0.16))
	var sy := y + h - minf(h * 0.2, 22)
	c.fill(VPath.new().round_rect(x + inset * 0.6, sy, w - inset * 1.2, y + h - inset * 0.5 - sy, rr_), al(dk(col, 0.3), 70))


## Round rect by top-left corner.
func rr(x: float, y: float, w: float, h: float, r: float, col: int) -> void:
	r = minf(r, minf(w, h) / 2)
	var p: VPath = VPath.new().round_rect(x, y, w, h, r)
	var o := _want_ol(minf(w, h), col)
	if o:
		_outline(p)
	c.fill(p, col)
	if o:
		_shade_rr(x, y, w, h, r, col)


## Round rect outline only.
func rrs(x: float, y: float, w: float, h: float, r: float, col: int, sw: float) -> void:
	c.stroke(VPath.new().round_rect(x, y, w, h, minf(r, minf(w, h) / 2)), col, sw)


## Plain rectangle with an outline (walls, slabs).
func rect_ol(x: float, y: float, w: float, h: float, col: int) -> void:
	var p: VPath = VPath.new().rect(x, y, w, h)
	if olw > 0:
		_outline(p)
	c.fill(p, col)


func rect(x: float, y: float, w: float, h: float, col: int) -> void:
	c.fill(VPath.new().rect(x, y, w, h), col)


func rect_s(x: float, y: float, w: float, h: float, col: int, sw: float) -> void:
	c.stroke(VPath.new().rect(x, y, w, h), col, sw)


func ci(x: float, y: float, r: float, col: int) -> void:
	var p: VPath = VPath.new().oval(x, y, r, r)
	var o := _want_ol(r * 2, col)
	if o:
		_outline(p)
	c.fill(p, col)
	if o and shade and r >= 26:
		c.fill(VPath.new().oval(x - r * 0.235, y - r * 0.51, r * 0.385, r * 0.29), lt(col, 0.16))


## Circle outline only.
func cis(x: float, y: float, r: float, col: int, sw: float) -> void:
	c.stroke(VPath.new().oval(x, y, r, r), col, sw)


func ov(x: float, y: float, rx: float, ry: float, col: int) -> void:
	var p: VPath = VPath.new().oval(x, y, rx, ry)
	if _want_ol(minf(rx, ry) * 2, col):
		_outline(p)
	c.fill(p, col)


func ovs(x: float, y: float, rx: float, ry: float, col: int, sw: float) -> void:
	c.stroke(VPath.new().oval(x, y, rx, ry), col, sw)


## Round-capped line; thick opaque lines get an outline too.
func ln(x1: float, y1: float, x2: float, y2: float, w: float, col: int) -> void:
	var p: VPath = VPath.new().move_to(x1, y1).line_to(x2, y2)
	if olw > 0 and w >= 9 and (col >> 24) & 255 == 255:
		c.stroke(p, olc, w + olw * 2)
	c.stroke(p, col, w)


func arc(cx: float, cy: float, rx: float, ry: float, start: float, sweep: float, w: float, col: int) -> void:
	c.stroke(VPath.new().arc(cx, cy, rx, ry, start, sweep), col, w)


func pie(cx: float, cy: float, rx: float, ry: float, start: float, sweep: float, col: int) -> void:
	var p: VPath = VPath.new().pie(cx, cy, rx, ry, start, sweep)
	if _want_ol(minf(rx, ry) * 1.2, col):
		_outline(p)
	c.fill(p, col)


## Filled polygon from flat x, y pairs.
func poly(col: int, pts: Array) -> void:
	var lo := Vector2(pts[0], pts[1])
	var hi := lo
	for i in range(2, pts.size() - 1, 2):
		lo = lo.min(Vector2(pts[i], pts[i + 1]))
		hi = hi.max(Vector2(pts[i], pts[i + 1]))
	var p: VPath = VPath.new().poly(pts)
	if _want_ol(maxf(hi.x - lo.x, hi.y - lo.y) * 0.7, col):
		_outline(p)
	c.fill(p, col)


func path(p: VPath, col: int) -> void:
	if _want_ol(40, col):
		_outline(p)
	c.fill(p, col)


func path_s(p: VPath, col: int, w: float) -> void:
	c.stroke(p, col, w)


## Bold text; align -1 left, 0 centre, 1 right; y is the baseline.
func text(s: String, x: float, y: float, size: float, col: int, align := 0) -> void:
	c.text(s, x, y, size, col, align)


## Vertical gradient.
func grad(x: float, y: float, w: float, h: float, top: int, bottom: int) -> void:
	c.gradient_rect(x, y, w, h, top, bottom)
