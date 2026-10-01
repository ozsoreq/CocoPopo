extends Node2D
## One tray card: a white rounded tile with a preview of a character or item.

var pressed_amt := 0.0
var here := false   # this character is already in the place
var _s := 1.0
var _label := ""
var inner: Node2D

func setup(kind: String, payload, is_here := false) -> void:
	here = is_here
	inner = Node2D.new()
	inner.position = Vector2(100, 100)
	add_child(inner)
	match kind:
		"new":
			var dice := Art.sprite("icon_8", Color("ffb02e"))
			dice.scale *= 1.6
			dice.position = Vector2(0, -12)
			inner.add_child(dice)
			_label = "Surprise!"
		"create":
			var plus := Art.sprite("icon_21", Color("ff6f8f"))
			plus.scale *= 1.5
			plus.position = Vector2(0, -12)
			inner.add_child(plus)
			_label = "Create"
		"lib":
			var cl := Character.new(Look.make(payload["look"]))
			cl.y = 60; cl.sc = 0.38; cl.pop = 1
			inner.add_child(cl)
			cl.apply_visual(0.0)
			_label = "Mine"
		"char":
			var c := Character.new(Look.preset(payload))
			c.y = 60; c.sc = 0.38; c.pop = 1
			inner.add_child(c)
			c.apply_visual(0.0)
			_label = Look.PRESET_NAMES[payload]
		_:
			var d := Rules.prop_def(payload)
			var k := minf(150.0 / d[3], 124.0 / d[4])
			var holder := Node2D.new()
			holder.position = Vector2(0, 52)
			holder.scale = Vector2(k, k)
			holder.add_child(Art.sprite("prop_" + payload))
			inner.add_child(holder)
			_label = d[2]
	var l := UI.label(_label, 26, Color("6b6480"))
	l.size = Vector2(200, 34)
	l.position = Vector2(0, 160)
	l.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	add_child(l)
	var check := Node2D.new()
	check.set_script(load("res://scripts/check_badge.gd"))
	check.position = Vector2(176, 24)
	check.name = "Check"
	add_child(check)

func _process(dt: float) -> void:
	_s = lerpf(_s, 0.93 if pressed_amt > 0 else 1.0, minf(1, dt * 20))
	scale = Vector2(_s, _s)
	position.y = 94 + (1 - _s) * 100
	var ck := get_node_or_null("Check")
	if ck:
		ck.visible = here
	queue_redraw()

func _draw() -> void:
	var sb := StyleBoxFlat.new()
	sb.bg_color = Color(0, 0, 0, 0.12)
	sb.set_corner_radius_all(30)
	sb.draw(get_canvas_item(), Rect2(0, 8, 200, 200))
	var sb2 := StyleBoxFlat.new()
	sb2.bg_color = Color.WHITE
	sb2.border_color = UI.OUTLINE
	sb2.set_border_width_all(4)
	sb2.set_corner_radius_all(30)
	sb2.draw(get_canvas_item(), Rect2(0, 0, 200, 200))
