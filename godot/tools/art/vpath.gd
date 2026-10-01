## A vector path (move / line / quadratic / cubic / close) plus helpers for common shapes.
## Angles are in degrees, clockwise from 3 o'clock (y-down), and measured on the circle the ellipse
## is stretched from (45° always points at the corner of the bounding box).
extends RefCounted

var cmds: Array = []


func move_to(x: float, y: float) -> RefCounted:
	cmds.append(["M", Vector2(x, y)])
	return self


func line_to(x: float, y: float) -> RefCounted:
	cmds.append(["L", Vector2(x, y)])
	return self


func quad_to(cx: float, cy: float, x: float, y: float) -> RefCounted:
	cmds.append(["Q", Vector2(cx, cy), Vector2(x, y)])
	return self


func cubic_to(c1x: float, c1y: float, c2x: float, c2y: float, x: float, y: float) -> RefCounted:
	cmds.append(["C", Vector2(c1x, c1y), Vector2(c2x, c2y), Vector2(x, y)])
	return self


func close() -> RefCounted:
	cmds.append(["Z"])
	return self


## Closed polygon from flat x, y pairs.
func poly(p: Array) -> RefCounted:
	move_to(p[0], p[1])
	for i in range(2, p.size() - 1, 2):
		line_to(p[i], p[i + 1])
	return close()


func rect(x: float, y: float, w: float, h: float) -> RefCounted:
	return poly([x, y, x + w, y, x + w, y + h, x, y + h])


## Corner radius is clamped to half the width and half the height separately (elliptic corners if needed).
func round_rect(x: float, y: float, w: float, h: float, r: float) -> RefCounted:
	var rx := minf(r, w / 2)
	var ry := minf(r, h / 2)
	if rx <= 0 or ry <= 0:
		return rect(x, y, w, h)
	move_to(x + rx, y)
	line_to(x + w - rx, y)
	_arc_to(x + w - rx, y + ry, rx, ry, -90, 90)
	line_to(x + w, y + h - ry)
	_arc_to(x + w - rx, y + h - ry, rx, ry, 0, 90)
	line_to(x + rx, y + h)
	_arc_to(x + rx, y + h - ry, rx, ry, 90, 90)
	line_to(x, y + ry)
	_arc_to(x + rx, y + ry, rx, ry, 180, 90)
	return close()


func oval(cx: float, cy: float, rx: float, ry: float) -> RefCounted:
	move_to(cx + rx, cy)
	_arc_to(cx, cy, rx, ry, 0, 360)
	return close()


## Open arc (for strokes).
func arc(cx: float, cy: float, rx: float, ry: float, start: float, sweep: float) -> RefCounted:
	var a := deg_to_rad(start)
	move_to(cx + rx * cos(a), cy + ry * sin(a))
	_arc_to(cx, cy, rx, ry, start, sweep)
	return self


## Pie slice: centre, arc, back to centre.
func pie(cx: float, cy: float, rx: float, ry: float, start: float, sweep: float) -> RefCounted:
	move_to(cx, cy)
	var a := deg_to_rad(start)
	line_to(cx + rx * cos(a), cy + ry * sin(a))
	_arc_to(cx, cy, rx, ry, start, sweep)
	return close()


## Continues the path along an elliptical arc (assumes the current point is the arc's start).
func _arc_to(cx: float, cy: float, rx: float, ry: float, start: float, sweep: float) -> void:
	var n := maxi(1, ceili(absf(sweep) / 90.0 - 0.001))
	var step := deg_to_rad(sweep) / n
	var k := 4.0 / 3.0 * tan(step / 4)
	var a := deg_to_rad(start)
	for i in n:
		var b := a + step
		var p0 := Vector2(cos(a), sin(a))
		var p3 := Vector2(cos(b), sin(b))
		var c1 := p0 + Vector2(-p0.y, p0.x) * k
		var c2 := p3 - Vector2(-p3.y, p3.x) * k
		cubic_to(cx + c1.x * rx, cy + c1.y * ry, cx + c2.x * rx, cy + c2.y * ry, cx + p3.x * rx, cy + p3.y * ry)
		a = b
