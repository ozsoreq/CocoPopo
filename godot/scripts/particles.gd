extends Node2D
## Crumbs, sparkles, bubbles and tap rings.

var ps: Array = []   # [pos, vel, life, size, color, type]

func burst(p: Vector2, n: int, col: Color, type: int, speed: float) -> void:
	for i in n:
		if ps.size() > 300:
			return
		var a := randf() * TAU
		var sp := speed * (0.4 + randf() * 0.6)
		var life := 0.7 if type == 1 else (1.6 + randf() if type == 3 else 0.8 + randf() * 0.5)
		var sz := 30.0 if type == 1 else (8 + randf() * 14 if type == 3 else 6 + randf() * 8)
		ps.append([p, Vector2(cos(a) * sp, sin(a) * sp - speed * 0.6), life, sz, col, type])

func _process(dt: float) -> void:
	var i := 0
	while i < ps.size():
		var q: Array = ps[i]
		q[2] -= dt
		if q[2] <= 0:
			ps.remove_at(i)
			continue
		var v: Vector2 = q[1]
		match q[5]:
			0: v.y += 1800 * dt
			2: v.y -= 200 * dt; v.x *= 0.96
			3: v = v * 0.9 + Vector2(sin(Time.get_ticks_msec() * 0.006 + i) * 4, -80 * dt)
		q[1] = v
		q[0] += v * dt
		i += 1
	queue_redraw()

func _draw() -> void:
	for q in ps:
		var a: float = minf(1.0, q[2] * 3)
		var p: Vector2 = q[0]
		var sz: float = q[3]
		var col: Color = q[4]
		match q[5]:
			1:
				var r: float = sz + (0.7 - q[2]) * 90
				draw_set_transform(p, 0, Vector2(1, 0.32))
				draw_arc(Vector2.ZERO, r, 0, TAU, 40, Color(1, 1, 1, a), 6, true)
				draw_set_transform(Vector2.ZERO, 0, Vector2.ONE)
			2:
				draw_set_transform(p, Time.get_ticks_msec() * 0.003, Vector2.ONE)
				draw_colored_polygon(PackedVector2Array([Vector2(0, -sz * 1.6), Vector2(sz * 0.5, 0), Vector2(0, sz * 1.6), Vector2(-sz * 0.5, 0)]), Color(col, a))
				draw_set_transform(Vector2.ZERO, 0, Vector2.ONE)
			3:
				draw_circle(p, sz, Color(1, 1, 1, a / 3))
				draw_arc(p, sz, 0, TAU, 24, Color(1, 1, 1, a), 3, true)
				draw_circle(p - Vector2(sz, sz) * 0.35, sz * 0.22, Color(1, 1, 1, a))
			_:
				draw_circle(p, sz * 0.5, Color(col, a))
