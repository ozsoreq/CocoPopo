class_name Paint
extends RefCounted
## Smooth vector drawing for the world, rendered directly by Godot. Shapes are triangulated and get a thin
## alpha "feather" ring so every edge is anti-aliased; the same ring, made wide, gives soft shadows and glows.
## Colours can be flat or a linear gradient (exact, because colours are interpolated per vertex).
##
##   var p := Paint.new(self)            # inside _draw()
##   p.fill(Paint.rrect(0, 0, 200, 120, 24), Paint.vgrad(Color("ffd6e0"), Color("ff8fa3"), 0, 120))
##   p.shadow(Paint.circle(Vector2(50, 50), 40), Vector2(0, 12), 24, 0.18)

const AA := 1.25  ## edge feather in local units

var _rid: RID
var _item: CanvasItem
static var _font: Font


func _init(item: CanvasItem) -> void:
	_item = item
	_rid = item.get_canvas_item()


# ================================================================ colour styles
## Linear gradient between two points (colour is clamped outside the segment).
class Grad:
	var a: Vector2
	var b: Vector2
	var c0: Color
	var c1: Color

	func _init(from: Vector2, to: Vector2, col0: Color, col1: Color) -> void:
		a = from; b = to; c0 = col0; c1 = col1

	func at(p: Vector2) -> Color:
		var d := b - a
		var t := clampf((p - a).dot(d) / maxf(d.length_squared(), 0.0001), 0.0, 1.0)
		return c0.lerp(c1, t)


static func vgrad(top: Color, bottom: Color, y0: float, y1: float) -> Grad:
	return Grad.new(Vector2(0, y0), Vector2(0, y1), top, bottom)


static func hgrad(left: Color, right: Color, x0: float, x1: float) -> Grad:
	return Grad.new(Vector2(x0, 0), Vector2(x1, 0), left, right)


static func _col(style: Variant, p: Vector2) -> Color:
	return (style as Grad).at(p) if style is Grad else style


# ================================================================ shape builders
static func ellipse(c: Vector2, rx: float, ry: float, a0 := 0.0, a1 := TAU) -> PackedVector2Array:
	var n := clampi(int(maxf(rx, ry) * absf(a1 - a0) / TAU * 0.9), 12, 220)
	var pts := PackedVector2Array()
	var full := absf(a1 - a0) >= TAU - 0.001
	var m := n if full else n + 1
	for i in m:
		var a := a0 + (a1 - a0) * i / n
		pts.append(c + Vector2(cos(a) * rx, sin(a) * ry))
	return pts


static func circle(c: Vector2, r: float) -> PackedVector2Array:
	return ellipse(c, r, r)


## Pie slice (closed: centre + arc).
static func pie(c: Vector2, rx: float, ry: float, a0: float, a1: float) -> PackedVector2Array:
	var pts := PackedVector2Array([c])
	pts.append_array(ellipse(c, rx, ry, a0, a1))
	return pts


static func rect(x: float, y: float, w: float, h: float) -> PackedVector2Array:
	return PackedVector2Array([Vector2(x, y), Vector2(x + w, y), Vector2(x + w, y + h), Vector2(x, y + h)])


## Rounded rectangle; r is clamped to half the shorter side.
static func rrect(x: float, y: float, w: float, h: float, r: float) -> PackedVector2Array:
	r = minf(r, minf(w, h) / 2)
	if r < 0.5:
		return rect(x, y, w, h)
	var pts := PackedVector2Array()
	var n := clampi(int(r * 0.35), 4, 24)
	var cs := [Vector2(x + w - r, y + r), Vector2(x + w - r, y + h - r), Vector2(x + r, y + h - r), Vector2(x + r, y + r)]
	for k in 4:
		var a0 := -PI / 2 + k * PI / 2
		for i in n + 1:
			var a := a0 + PI / 2 * i / n
			pts.append(cs[k] + Vector2(cos(a), sin(a)) * r)
	return pts


## Rounded rect with different top / bottom radii (e.g. arched windows, awnings).
static func rrect2(x: float, y: float, w: float, h: float, rt: float, rb: float) -> PackedVector2Array:
	var pts := PackedVector2Array()
	rt = minf(rt, minf(w / 2, h)); rb = minf(rb, minf(w / 2, h - rt))
	var corners := [[Vector2(x + w - rt, y + rt), rt, -PI / 2], [Vector2(x + w - rb, y + h - rb), rb, 0.0],
		[Vector2(x + rb, y + h - rb), rb, PI / 2], [Vector2(x + rt, y + rt), rt, PI]]
	for cdef in corners:
		var r: float = cdef[1]
		if r < 0.5:
			var cc: Vector2 = cdef[0]
			pts.append(cc + Vector2(cos(cdef[2] + PI / 4), sin(cdef[2] + PI / 4)) * r * 1.414)
			continue
		var n := clampi(int(r * 0.35), 4, 24)
		for i in n + 1:
			var a: float = cdef[2] + PI / 2 * i / n
			pts.append(cdef[0] + Vector2(cos(a), sin(a)) * r)
	return pts


## Smooth closed blob through the given points (Catmull-Rom).
static func blob(ctrl: PackedVector2Array, seg := 10) -> PackedVector2Array:
	var pts := PackedVector2Array()
	var n := ctrl.size()
	for i in n:
		var p0 := ctrl[(i - 1 + n) % n]
		var p1 := ctrl[i]
		var p2 := ctrl[(i + 1) % n]
		var p3 := ctrl[(i + 2) % n]
		for s in seg:
			var t := float(s) / seg
			var t2 := t * t
			var t3 := t2 * t
			pts.append(0.5 * (2.0 * p1 + (p2 - p0) * t + (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * t2 + (3.0 * p1 - p0 - 3.0 * p2 + p3) * t3))
	return pts


## Open smooth curve through points (Catmull-Rom, end points repeated).
static func curve(ctrl: PackedVector2Array, seg := 12) -> PackedVector2Array:
	var pts := PackedVector2Array()
	var n := ctrl.size()
	for i in n - 1:
		var p0 := ctrl[maxi(i - 1, 0)]
		var p1 := ctrl[i]
		var p2 := ctrl[i + 1]
		var p3 := ctrl[mini(i + 2, n - 1)]
		for s in seg:
			var t := float(s) / seg
			var t2 := t * t
			var t3 := t2 * t
			pts.append(0.5 * (2.0 * p1 + (p2 - p0) * t + (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * t2 + (3.0 * p1 - p0 - 3.0 * p2 + p3) * t3))
	pts.append(ctrl[n - 1])
	return pts


static func quad(p0: Vector2, c: Vector2, p1: Vector2, seg := 16) -> PackedVector2Array:
	var pts := PackedVector2Array()
	for i in seg + 1:
		var t := float(i) / seg
		pts.append(p0.lerp(c, t).lerp(c.lerp(p1, t), t))
	return pts


static func xform(pts: PackedVector2Array, pos: Vector2, rot := 0.0, scl := Vector2.ONE) -> PackedVector2Array:
	return Transform2D(rot, scl, 0.0, pos) * pts


# ================================================================ drawing
## Filled shape with anti-aliased edges. style: Color or Grad.
func fill(pts: PackedVector2Array, style: Variant, feather := AA) -> void:
	_soft(pts, style, feather, 1.0, true)


## The shape with a soft edge fading out over blur units beyond it (shadows, glows).
## alpha scales the colour's alpha.
func soft(pts: PackedVector2Array, col: Color, blur: float, alpha := 1.0) -> void:
	_soft(pts, col, blur, alpha, false)


func shadow(pts: PackedVector2Array, offset: Vector2, blur: float, alpha := 0.16, col := Color(0.16, 0.1, 0.2)) -> void:
	_soft(Paint.xform(pts, offset), col, blur, alpha, false)


## Round radial glow of radius r (solid core of radius core, then fading out).
func glow(c: Vector2, r: float, col: Color, core := 0.0) -> void:
	core = maxf(core, 2.0)
	_soft(Paint.circle(c, core), col, maxf(r - core, 1.0), 1.0, false)


## Thick smooth line along points, with round ends. closed joins the last point to the first.
func stroke(pts: PackedVector2Array, style: Variant, width: float, closed := false, caps := true) -> void:
	pts = _dedupe(pts, closed)
	var n := pts.size()
	if n < 2:
		return
	var hw := width / 2.0
	var f := AA / 2.0
	var verts := PackedVector2Array()
	var cols := PackedColorArray()
	var idx := PackedInt32Array()
	for i in n:
		var nv := _vertex_normal(pts, i, closed)
		var c := _col(style, pts[i])
		var clear := Color(c, 0.0)
		# rows: outer feather, outer, inner, inner feather
		verts.append_array([pts[i] + nv * (hw + f), pts[i] + nv * (hw - f), pts[i] - nv * (hw - f), pts[i] - nv * (hw + f)])
		cols.append_array([clear, c, c, clear])
	var segs := n if closed else n - 1
	for i in segs:
		var a := i * 4
		var b := ((i + 1) % n) * 4
		for r in 3:
			idx.append_array([a + r, b + r, b + r + 1, a + r, b + r + 1, a + r + 1])
	RenderingServer.canvas_item_add_triangle_array(_rid, idx, verts, cols)
	if caps and not closed:
		fill(Paint.circle(pts[0], hw), _col(style, pts[0]))
		fill(Paint.circle(pts[n - 1], hw), _col(style, pts[n - 1]))


func line(a: Vector2, b: Vector2, style: Variant, width: float) -> void:
	stroke(PackedVector2Array([a, b]), style, width)


func ring(c: Vector2, r: float, style: Variant, width: float) -> void:
	stroke(Paint.circle(c, r), style, width, true)


## Rounded bold text centred on x (y is the baseline).
func text(s: String, x: float, y: float, size: int, col: Color) -> void:
	if _font == null:
		_font = UI.font()
	_item.draw_string(_font, Vector2(x - 2000, y), s, HORIZONTAL_ALIGNMENT_CENTER, 4000, size, col)


# ================================================================ internals
static func _dedupe(pts: PackedVector2Array, closed := true) -> PackedVector2Array:
	var out := PackedVector2Array()
	for p in pts:
		if out.is_empty() or out[out.size() - 1].distance_squared_to(p) > 0.01:
			out.append(p)
	if closed and out.size() > 2 and out[0].distance_squared_to(out[out.size() - 1]) < 0.01:
		out.remove_at(out.size() - 1)
	return out


static func _area(pts: PackedVector2Array) -> float:
	var a := 0.0
	var n := pts.size()
	for i in n:
		a += pts[i].cross(pts[(i + 1) % n])
	return a * 0.5


## Unit normal at vertex i, scaled for the miter (pointing "left" of the walking direction for open lines;
## outward for closed shapes when the caller orients them).
static func _vertex_normal(pts: PackedVector2Array, i: int, closed: bool) -> Vector2:
	var n := pts.size()
	var prev := pts[i - 1] if (i > 0 or closed) else pts[i]
	var next := pts[(i + 1) % n] if (i < n - 1 or closed) else pts[i]
	var d0 := (pts[i] - prev).normalized() if prev != pts[i] else (next - pts[i]).normalized()
	var d1 := (next - pts[i]).normalized() if next != pts[i] else d0
	var n0 := Vector2(d0.y, -d0.x)
	var n1 := Vector2(d1.y, -d1.x)
	var nv := (n0 + n1).normalized()
	if nv == Vector2.ZERO:
		nv = n1
	var m := 1.0 / maxf(nv.dot(n1), 0.35)
	return nv * m


## centered: the feather straddles the edge (anti-aliasing); otherwise it extends outward (blur).
func _soft(pts: PackedVector2Array, style: Variant, feather: float, alpha: float, centered: bool) -> void:
	pts = _dedupe(pts)
	var n := pts.size()
	if n < 3:
		return
	var sign_ := 1.0 if _area(pts) > 0 else -1.0
	var tri := Geometry2D.triangulate_polygon(pts)
	if tri.is_empty():
		return
	var verts := PackedVector2Array()
	var cols := PackedColorArray()
	var ins := feather / 2.0 if centered else 0.0
	var outs := feather / 2.0 if centered else feather
	var inner := PackedVector2Array()
	var outer := PackedVector2Array()
	for i in n:
		var nv := _vertex_normal(pts, i, true) * sign_
		inner.append(pts[i] - nv * ins)
		outer.append(pts[i] + nv * outs)
	for i in n:
		var c := _col(style, inner[i])
		c.a *= alpha
		verts.append(inner[i])
		cols.append(c)
	for i in n:
		var c := _col(style, outer[i])
		verts.append(outer[i])
		cols.append(Color(c, 0.0))
	var idx := PackedInt32Array(tri)
	for i in n:
		var j := (i + 1) % n
		idx.append_array([i, j, n + j, i, n + j, n + i])
	RenderingServer.canvas_item_add_triangle_array(_rid, idx, verts, cols)
