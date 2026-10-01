extends Node2D
## Soft pink backdrop with a spotlight and a podium for the dress-up screen.

var size := Vector2(1920, 1080)

func _process(_dt: float) -> void:
	queue_redraw()

func _draw() -> void:
	var top := Color("ffe8f1")
	var bot := Color("fff6de")
	draw_polygon(PackedVector2Array([Vector2.ZERO, Vector2(size.x, 0), size, Vector2(0, size.y)]),
		PackedColorArray([top, top, bot, bot]))
	var px := size.x * 0.2
	draw_circle(Vector2(px, 600), 430, Color(1, 1, 1, 0.55))
	draw_circle(Vector2(px, 600), 320, Color(1, 1, 1, 0.6))
	for i in 10:
		var h: float = sin(i * 12.9898 + 4.1) * 43758.5453
		var f: float = h - floorf(h)
		var h2: float = sin((i + 9) * 12.9898 + 4.1) * 43758.5453
		var f2: float = h2 - floorf(h2)
		draw_circle(Vector2(f * size.x * 0.42, 150 + f2 * 800), 8 + f * 10, Color(1, 0.56, 0.64, 0.35))
	draw_set_transform(Vector2(px, 944), 0, Vector2(1, 0.23))
	draw_circle(Vector2.ZERO, 236, UI.OUTLINE)
	draw_circle(Vector2.ZERO, 230, Color("e9cfa3"))
	draw_set_transform(Vector2(px, 930), 0, Vector2(1, 0.23))
	draw_circle(Vector2.ZERO, 236, UI.OUTLINE)
	draw_circle(Vector2.ZERO, 230, Color("ffe9c2"))
	draw_circle(Vector2.ZERO, 190, Color("fff6e2"))
	draw_set_transform(Vector2(px, 930), 0, Vector2(1, 0.2))
	draw_circle(Vector2.ZERO, 90, Color(0, 0, 0, 0.16))
	draw_set_transform(Vector2.ZERO, 0, Vector2.ONE)
