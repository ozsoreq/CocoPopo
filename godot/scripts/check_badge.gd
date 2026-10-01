extends Node2D
## Small green tick shown on a tray card when that character is already in the place.

func _draw() -> void:
	draw_circle(Vector2.ZERO, 21, UI.OUTLINE)
	draw_circle(Vector2.ZERO, 17, Color("3cc57b"))
	draw_polyline(PackedVector2Array([Vector2(-8, 0), Vector2(-2, 7), Vector2(9, -7)]), Color.WHITE, 4.5, true)
