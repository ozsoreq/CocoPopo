class_name DressUp
extends CanvasLayer
## Dress-up editor: a big live preview on a podium plus tabs of options shown as mini previews.

signal finished(look: Look, saved: bool)

const PANEL_TOP := 230.0
const CARD := 138.0
const GAP := 18.0
const TABS := [["icon_18", "ffa85c"], ["icon_19", "9b6cdc"], ["icon_12", "3cc5af"], ["icon_17", "4fb3ff"], ["icon_16", "ff6f8f"]]

var work: Look
var title_text := "New character"
var tab := 0
var t := 0.0
var root: Control
var stage: Node2D
var preview: Character
var panel: Control
var content: Control
var scroll := 0.0
var scroll_max := 0.0
var tab_buttons: Array[RoundButton] = []
var _press := false
var _press_pos := Vector2.ZERO
var _last := Vector2.ZERO
var _dragged := false
var _pressed_opt: Array = []
var vs := Vector2(1920, 1080)

func open(start: Look, title: String) -> void:
	work = start.copy()
	title_text = title

func _ready() -> void:
	layer = 50
	root = Control.new()
	root.mouse_filter = Control.MOUSE_FILTER_STOP
	add_child(root)
	var bg := Node2D.new()
	bg.set_script(load("res://scripts/editor_bg.gd"))
	root.add_child(bg)
	stage = Node2D.new()
	root.add_child(stage)
	panel = Control.new()
	panel.clip_contents = true
	panel.mouse_filter = Control.MOUSE_FILTER_STOP
	panel.gui_input.connect(_panel_input)
	panel.draw.connect(_panel_draw)
	root.add_child(panel)
	content = Control.new()
	content.mouse_filter = Control.MOUSE_FILTER_IGNORE
	panel.add_child(content)
	var title := UI.label(title_text, 52, Color("c24e6e"))
	title.name = "Title"
	root.add_child(title)
	for i in TABS.size():
		var b := RoundButton.new().setup(TABS[i][0], Color(TABS[i][1]), 46)
		b.pressed.connect(_set_tab.bind(i))
		root.add_child(b)
		tab_buttons.append(b)
	_button("Back", "icon_15", "ff8f5c", 52, func(): finished.emit(null, false))
	_button("Done", "icon_7", "3cc57b", 60, func(): finished.emit(work, true))
	_button("Dice", "icon_8", "ffb02e", 54, _randomize)
	_button("Save", "icon_22", "ff6f8f", 54, _save_to_lib)
	get_viewport().size_changed.connect(_layout)
	_layout()
	_rebuild_preview()
	_set_tab(0)

func _button(n: String, icon: String, col: String, r: float, cb: Callable) -> void:
	var b := RoundButton.new().setup(icon, Color(col), r)
	b.name = n
	b.pressed.connect(cb)
	root.add_child(b)

func _layout() -> void:
	vs = get_viewport().get_visible_rect().size
	root.size = vs
	var x0 := vs.x * 0.44
	panel.position = Vector2(x0, PANEL_TOP)
	panel.size = Vector2(vs.x - 40 - x0, vs.y - 40 - PANEL_TOP)
	var tw := (panel.size.x - 130) / TABS.size()
	for i in tab_buttons.size():
		tab_buttons[i].position = Vector2(x0 + tw * (i + 0.5) - 46, 118)
	(root.get_node("Back") as Control).position = Vector2(44, 38)
	(root.get_node("Done") as Control).position = Vector2(vs.x - 156, 30)
	var px := vs.x * 0.2
	(root.get_node("Dice") as Control).position = Vector2(px - 204, vs.y - 136)
	(root.get_node("Save") as Control).position = Vector2(px + 96, vs.y - 136)
	var title: Label = root.get_node("Title")
	title.reset_size()
	title.position = Vector2(px - title.size.x / 2, 60)
	stage.position = Vector2(px, 930)
	(root.get_child(0) as Node2D).set("size", vs)
	_rebuild_content()

func _set_tab(i: int) -> void:
	tab = i
	scroll = 0
	for k in tab_buttons.size():
		tab_buttons[k].active = k == i
	_rebuild_content()

func _rebuild_preview() -> void:
	if preview:
		preview.queue_free()
	preview = Character.new(work.copy())
	preview.sc = 1.62
	preview.pop = 1
	stage.add_child(preview)

func _randomize() -> void:
	var keep_acc := work.acc
	work = Look.random_look()
	if randi() % 3 != 0:
		work.acc = keep_acc
	_changed()
	Sfx.play("tada")

func _save_to_lib() -> void:
	Save.add_to_lib(work.to_array())
	preview.do_emote(0)
	preview.hop_v = 600
	Sfx.play("spark")

func _changed() -> void:
	_rebuild_preview()
	preview.hop_v = 420
	_rebuild_content()

# ------------------------------------------------------------------ options
## group -> [label, count or palette, kind]
func _sections() -> Array:
	match tab:
		0: return [["Body", 10, Look.N_BODY], ["Head shape", 11, Look.N_HEAD], ["Skin colour", 0, Look.SKIN], ["Freckles", 14, 2]]
		1: return [["Hairstyle", 1, Look.N_HAIR], ["Hair colour", 2, Look.HAIR]]
		2: return [["Eyes", 3, Look.N_EYES], ["Mouth", 4, Look.N_MOUTH]]
		3: return [["Top", 5, Look.N_TOP], ["Top colour", 6, Look.CLOTH], ["Bottoms", 12, Look.N_BSTYLE],
			["Bottoms colour", 7, Look.PANTS], ["Shoes", 13, Look.SHOES]]
	return [["Hats & accessories", 8, Look.N_ACC], ["Accessory colour", 9, Look.CLOTH]]

## Option groups use the Look.to_array() order: skin, hair_style, hair_color, eyes, mouth, top, top_color,
## bottom, acc, acc_color, body, head, bstyle, shoe, freckles.
func _get_opt(g: int) -> int:
	return work.to_array()[g]

func _apply(g: int, i: int) -> void:
	var a := work.to_array()
	a[g] = i
	work = Look.make(a)
	_changed()
	Sfx.play("pop", -6)

func _rebuild_content() -> void:
	for c in content.get_children():
		c.queue_free()
	_pressed_opt.clear()
	var y := 30.0
	var left := 30.0
	var cw := panel.size.x - 60
	for sec in _sections():
		var lab := UI.label(sec[0], 36, Color("6b6480"))
		lab.position = Vector2(left, y)
		content.add_child(lab)
		y += 58
		var g: int = sec[1]
		var sel := _get_opt(g)
		if sec[2] is Array:
			var cols: Array = sec[2]
			var step := 92.0
			var per := maxi(1, int(cw / step))
			for i in cols.size():
				var sw := Swatch.new()
				sw.col = Look.col(cols[i])
				sw.on = i == sel
				sw.position = Vector2(left + (i % per) * step, y + (i / per) * step)
				sw.set_meta("opt", [g, i])
				content.add_child(sw)
			y += ceilf(cols.size() / float(per)) * step + 24
		else:
			var n: int = sec[2]
			var per2 := maxi(1, int((cw + GAP) / (CARD + GAP)))
			for i in n:
				var card := OptionCard.new()
				card.position = Vector2(left + (i % per2) * (CARD + GAP), y + (i / per2) * (CARD + GAP))
				card.on = i == sel
				card.set_meta("opt", [g, i])
				content.add_child(card)
				card.setup(_variant(g, i), g)
			y += ceilf(n / float(per2)) * (CARD + GAP) + 20
	scroll_max = maxf(0, y - panel.size.y + 40)
	scroll = clampf(scroll, 0, scroll_max)
	content.position.y = -scroll

func _variant(g: int, i: int) -> Look:
	var a := work.to_array()
	a[g] = i
	var l := Look.make(a)
	if g in [1, 11]:
		l.acc = 0
	return l

func _opt_at(p: Vector2) -> Node:
	var lp := p - content.position
	for c in content.get_children():
		if c.has_meta("opt") and Rect2(c.position, c.get("box")).has_point(lp):
			return c
	return null

func _panel_input(e: InputEvent) -> void:
	if e is InputEventMouseButton and e.button_index == MOUSE_BUTTON_LEFT:
		if e.pressed:
			_press = true; _dragged = false; _press_pos = e.position; _last = e.position
			var o := _opt_at(e.position)
			if o: o.set("pressed_amt", 1.0)
		else:
			for c in content.get_children():
				if c.has_meta("opt"): c.set("pressed_amt", 0.0)
			if _press and not _dragged:
				var o2 := _opt_at(e.position)
				if o2:
					var m: Array = o2.get_meta("opt")
					_apply(m[0], m[1])
			_press = false
	elif e is InputEventMouseMotion and _press:
		if (e.position - _press_pos).length() > 14:
			_dragged = true
			for c in content.get_children():
				if c.has_meta("opt"): c.set("pressed_amt", 0.0)
		if _dragged:
			scroll = clampf(scroll - (e.position.y - _last.y), 0, scroll_max)
			content.position.y = -scroll
		_last = e.position

func _panel_draw() -> void:
	var sb := StyleBoxFlat.new()
	sb.bg_color = Color.WHITE
	sb.border_color = UI.OUTLINE
	sb.set_border_width_all(5)
	sb.set_corner_radius_all(40)
	sb.draw(panel.get_canvas_item(), Rect2(Vector2.ZERO, panel.size))

func _process(delta: float) -> void:
	t += delta
	if preview:
		_animate(preview, delta)

func _animate(c: Character, dt: float) -> void:
	c.hop_v -= 2600 * dt
	c.hop += c.hop_v * dt
	if c.hop < 0:
		c.hop = 0; c.hop_v = 0
	if c.emote_t >= 0:
		c.emote_t += dt
		if c.emote_t > 2: c.emote_t = -1
	c.next_blink -= dt
	if c.next_blink <= 0:
		c.blink_t = 0.14; c.next_blink = 2 + randf() * 3
	if c.blink_t > 0: c.blink_t -= dt
	c.blink = sin(PI * (1 - c.blink_t / 0.14)) if c.blink_t > 0 else 0.0
	c.arm = 1.0 if (c.emote_t >= 0 and c.emote_t < 0.8) else 0.0
	c.face = [7, 3, 1, 3, 0, 6, 2][c.emote % 7] if c.emote_t >= 0 else 0
	c.glance_t -= dt
	if c.glance_t <= 0:
		c.glance = (randf() - 0.5) * 1.6; c.glance_t = 1 + randf() * 3
	c.gaze_x += (c.glance - c.gaze_x) * minf(1, dt * 6)
	c.apply_visual(t)

func _input(e: InputEvent) -> void:
	# tap the big character to make it react
	if e is InputEventMouseButton and e.pressed and e.button_index == MOUSE_BUTTON_LEFT:
		var p: Vector2 = e.position
		if absf(p.x - stage.position.x) < 190 and p.y > 260 and p.y < 960 and p.x < vs.x * 0.42:
			preview.hop_v = 700
			preview.do_emote(randi() % 7)
			Sfx.play("squeak")
