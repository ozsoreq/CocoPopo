class_name Tray
extends Control
## Bottom drawer with character and item cards. Tap a card to add it, or drag it up into the scene.

const H_OPEN := 330.0
const CARD := 200.0
const GAP := 22.0
const X0 := 400.0

var loc: Location
var tab := 0          # 0 closed, 1 characters, 2 items
var cat := 0
var open_a := 0.0
var scroll := 0.0
var scroll_v := 0.0
var strip: Node2D
var chips: HBoxContainer
var title: Label
var cards: Array = []     # [kind, payload]
var _press := false
var _press_pos := Vector2.ZERO
var _last := Vector2.ZERO
var _card := -1
var _decided := false
var _handoff := false
var vs := Vector2(1920, 1080)

func _ready() -> void:
	clip_contents = true
	mouse_filter = Control.MOUSE_FILTER_STOP
	strip = Node2D.new()
	add_child(strip)
	chips = HBoxContainer.new()
	chips.add_theme_constant_override("separation", 14)
	chips.position = Vector2(X0, 22)
	add_child(chips)
	for i in Rules.CATS.size():
		var b := Button.new()
		b.text = Rules.CATS[i]
		b.add_theme_font_override("font", UI.font())
		b.add_theme_font_size_override("font_size", 30)
		b.focus_mode = Control.FOCUS_NONE
		b.pressed.connect(func(): _set_cat(i))
		chips.add_child(b)
	title = UI.label("Characters", 38, Color("c24e6e"))
	title.position = Vector2(X0, 18)
	add_child(title)
	visible = false
	_style_chips()

func _style_chips() -> void:
	for i in chips.get_child_count():
		var b: Button = chips.get_child(i)
		var on := i == cat
		for st in ["normal", "hover", "pressed", "focus"]:
			var sb := StyleBoxFlat.new()
			sb.bg_color = Color("6c7bff") if on else Color.WHITE
			sb.border_color = UI.OUTLINE
			sb.set_border_width_all(4)
			sb.set_corner_radius_all(28)
			sb.content_margin_left = 26; sb.content_margin_right = 26; sb.content_margin_top = 6; sb.content_margin_bottom = 6
			b.add_theme_stylebox_override(st, sb)
		b.add_theme_color_override("font_color", Color.WHITE if on else Color("6b6480"))
		b.add_theme_color_override("font_hover_color", Color.WHITE if on else Color("6b6480"))
		b.add_theme_color_override("font_pressed_color", Color.WHITE)

func layout(v: Vector2) -> void:
	vs = v
	size = Vector2(v.x, H_OPEN + 40)

func set_tab(t: int) -> void:
	tab = t
	scroll = 0
	if t != 0:
		_rebuild()

func _set_cat(i: int) -> void:
	cat = i
	scroll = 0
	_style_chips()
	_rebuild()
	Sfx.play("tick")

func _rebuild() -> void:
	for c in strip.get_children():
		c.queue_free()
	cards.clear()
	chips.visible = tab == 2
	title.visible = tab == 1
	if tab == 1:
		cards.append(["new", null])
		for i in Look.PRESETS.size():
			cards.append(["char", i])
	else:
		for d in Rules.PROPS:
			if d[0] == cat:
				cards.append(["prop", d[1]])
	for i in cards.size():
		var node := Node2D.new()
		node.set_script(load("res://scripts/card.gd"))
		node.position = Vector2(X0 + i * (CARD + GAP), 94)
		strip.add_child(node)
		node.setup(cards[i][0], cards[i][1])

func _process(dt: float) -> void:
	var target := 1.0 if tab != 0 else 0.0
	open_a = lerpf(open_a, target, minf(1, dt * 12))
	visible = open_a > 0.01
	position = Vector2(0, vs.y - H_OPEN * ease_out(open_a))
	if not _press:
		scroll += scroll_v * dt
		scroll_v *= pow(0.02, dt)
	var mx := maxf(0, cards.size() * (CARD + GAP) - (vs.x - X0 - 30))
	scroll = clampf(scroll, 0, mx)
	strip.position.x = -scroll
	queue_redraw()

func ease_out(a: float) -> float:
	return a * a * (3 - 2 * a)

func _draw() -> void:
	var sb := StyleBoxFlat.new()
	sb.bg_color = Color("fff7e6")
	sb.border_color = UI.OUTLINE
	sb.set_border_width_all(5)
	sb.set_corner_radius_all(48)
	sb.draw(get_canvas_item(), Rect2(-40, 0, size.x + 80, H_OPEN + 80))
	draw_rect(Rect2(0, 5, size.x, 14), Color("ffd98a"))

func _card_at(p: Vector2) -> int:
	if p.y < 84 or p.x < X0 - 10:
		return -1
	var rel := p.x - X0 + scroll
	var i := int(floor(rel / (CARD + GAP)))
	if i < 0 or i >= cards.size() or rel - i * (CARD + GAP) > CARD:
		return -1
	return i

func _gui_input(e: InputEvent) -> void:
	if e is InputEventMouseButton and e.button_index == MOUSE_BUTTON_LEFT:
		if e.pressed:
			_press = true; _decided = false; _handoff = false
			_press_pos = e.position; _last = e.position
			_card = _card_at(e.position)
			scroll_v = 0
			_press_card(true)
		else:
			_press_card(false)
			if _handoff:
				loc.external_up(e.global_position)
			elif not _decided and _card >= 0 and _card_at(e.position) == _card:
				_tap_card(_card)
			_press = false; _handoff = false
		accept_event()
	elif e is InputEventMouseMotion and _press:
		if _handoff:
			loc.external_move(e.global_position)
		else:
			var d: Vector2 = e.position - _press_pos
			if not _decided and d.length() > 18:
				_decided = true
				_press_card(false)
				if _card >= 0 and d.y < -20 and absf(d.y) > absf(d.x) * 0.8:
					var th := _spawn(_card)
					if th != null:
						_handoff = true
						loc.begin_external_drag(th, e.global_position)
			if _decided and not _handoff:
				scroll -= e.position.x - _last.x
				scroll_v = -(e.position.x - _last.x) * 40
		_last = e.position
		accept_event()

func _press_card(on: bool) -> void:
	if _card >= 0 and _card < strip.get_child_count():
		strip.get_child(_card).pressed_amt = 1.0 if on else 0.0

func _spawn(i: int) -> Thing:
	var c: Array = cards[i]
	match c[0]:
		"new":
			return loc.make_char(Look.random_look(), 0, 0)
		"char":
			return loc.make_char(Look.preset(c[1]), 0, 0)
		_:
			return loc.make_prop(c[1], 0, 0)

func _tap_card(i: int) -> void:
	var th := _spawn(i)
	if th == null:
		return
	th.x = loc.vis_x(0.5) + (randf() - 0.5) * 500
	th.y = 420 + randf() * 100
	th.pop = 0
	loc.sel = th
	loc.start_fall(th, 0, 0)
	Sfx.play("pop")
