extends Node2D
## The ground of the little world (drawn in planet space so it spins), painted by Godot.
## Everything here is rotation-symmetric in its shading so turning the planet never looks odd.

const R := 980.0


func _draw() -> void:
	var p := Paint.new(self)
	p.soft(Paint.circle(Vector2.ZERO, R), Color(0.3, 0.55, 0.35), 40, 0.25)
	p.fill(Paint.circle(Vector2.ZERO, R), Color("9be47e"))
	p.fill(Paint.circle(Vector2.ZERO, R - 50), Color("86d76c"))
	p.fill(Paint.circle(Vector2.ZERO, R - 260), Color("78cc62"))
	p.ring(Vector2.ZERO, R - 8, Color(1, 1, 1, 0.35), 6)
	p.ring(Vector2.ZERO, R - 30, Color("f7e6b8"), 24)
	p.ring(Vector2.ZERO, R - 30, Color(1, 1, 1, 0.35), 6)
	for k in 8:
		var a := deg_to_rad(k * 45 + 22.5)
		var dist := R - 210 - (k % 2) * 90
		var c := Vector2(sin(a), -cos(a)) * dist
		var rx := 120.0 - (k % 3) * 20
		var pond := Paint.xform(Paint.ellipse(Vector2.ZERO, rx, rx * 0.42), c, a)
		p.fill(Paint.xform(Paint.ellipse(Vector2.ZERO, rx + 10, rx * 0.42 + 8), c, a), Color("c9f0ff"))
		p.fill(pond, Color("6fc8f2"))
		p.fill(Paint.xform(Paint.ellipse(Vector2(-rx * 0.3, -rx * 0.1), rx * 0.34, rx * 0.1), c, a), Color(1, 1, 1, 0.4))
		for j in 6:
			var fa := a + deg_to_rad((j - 2.5) * 2.8)
			var fp := Vector2(sin(fa), -cos(fa)) * (R - 120 - (j % 2) * 30)
			Scenery.flower(p, fp, 0.9, [Color.WHITE, Color("ffe066"), Color("ff9fbf")][j % 3])
	for i in 60:
		var a := TAU * Scenery.hash01(i + 900)
		var d := R - 70 - Scenery.hash01(i + 950) * 300
		var c := Vector2(sin(a), -cos(a)) * d
		for j in 3:  # blades point away from the planet's centre
			p.line(c, c + Vector2(sin(a + (j - 1) * 0.45), -cos(a + (j - 1) * 0.45)) * 24, Color(0.25, 0.6, 0.3, 0.5), 5)
