extends Node2D
## The ground of the little world (drawn in planet space so it spins).

const R := 980.0

func _draw() -> void:
	draw_circle(Vector2.ZERO, R + 7, UI.OUTLINE)
	draw_circle(Vector2.ZERO, R, Color("94de78"))
	draw_circle(Vector2.ZERO, R - 60, Color("7fcf66"))
	draw_arc(Vector2.ZERO, R - 28, 0, TAU, 256, Color("f7e4b0"), 26, true)
	for k in 8:
		var a := deg_to_rad(k * 45 + 22.5)
		var dist := R - 210 - (k % 2) * 90
		var c := Vector2(sin(a), -cos(a)) * dist
		draw_set_transform(c, a, Vector2(1, 0.4))
		var rx := 120.0 - (k % 3) * 20
		draw_circle(Vector2.ZERO, rx + 5, UI.OUTLINE)
		draw_circle(Vector2.ZERO, rx, Color("6ec8f5"))
		draw_circle(Vector2(-30, -10), 40, Color(1, 1, 1, 0.45))
		draw_set_transform(Vector2.ZERO, 0, Vector2.ONE)
		for j in 6:
			var fa := a + deg_to_rad((j - 2.5) * 2.8)
			var fp := Vector2(sin(fa), -cos(fa)) * (R - 120 - (j % 2) * 30)
			draw_circle(fp, 7, Color.WHITE if j % 2 == 0 else Color("ffe066"))
