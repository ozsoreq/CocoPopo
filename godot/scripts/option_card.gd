class_name OptionCard
extends Control
## A rounded card showing a mini preview of one dress-up option.

var on := false
var pressed_amt := 0.0
var box := Vector2(138, 138)
var _s := 1.0

func setup(l: Look, g: int) -> void:
	size = box
	custom_minimum_size = box
	pivot_offset = box / 2
	clip_contents = true
	mouse_filter = Control.MOUSE_FILTER_IGNORE
	var cy := -290.0
	var k := 0.5
	match g:
		1: cy = -290; k = 0.5
		3: cy = -262; k = 1.08
		4: cy = -230; k = 1.3
		5: cy = -128; k = 0.6
		10: cy = -l.height() / 2 - 8; k = 118.0 / (l.height() + 20)
		11: cy = -270; k = 0.56
		12: cy = -80; k = 0.66
		14: cy = -244; k = 1.3
		_: cy = -300; k = 0.5
	if g not in [5, 10, 12]:
		cy += l.neck_y() + 196
	var c := Character.new(l)
	c.sc = k
	c.y = 0
	c.pop = 1
	var holder := Node2D.new()
	holder.position = Vector2(69, 69 - cy * k)
	holder.add_child(c)
	add_child(holder)
	c.apply_visual(0.0)
	var frame := Frame.new()
	frame.size = box
	frame.on = on
	frame.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(frame)

func _process(dt: float) -> void:
	_s = lerpf(_s, 0.93 if pressed_amt > 0 else 1.0, minf(1, dt * 20))
	scale = Vector2(_s, _s)

func _draw() -> void:
	var sb := StyleBoxFlat.new()
	sb.bg_color = Color("e5e8ff") if on else Color("f5f2fa")
	sb.set_corner_radius_all(28)
	sb.draw(get_canvas_item(), Rect2(Vector2.ZERO, box))

class Frame extends Control:
	var on := false
	func _draw() -> void:
		var sb := StyleBoxFlat.new()
		sb.draw_center = false
		sb.border_color = Color("6c7bff") if on else UI.OUTLINE
		sb.set_border_width_all(7 if on else 4)
		sb.set_corner_radius_all(28)
		sb.draw(get_canvas_item(), Rect2(Vector2.ZERO, size))
