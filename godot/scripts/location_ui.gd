class_name LocationUI
extends CanvasLayer
## Buttons, selection menu, item tray and screen flash for a location.

var loc: Location
var btn_home: RoundButton
var btn_reset: RoundButton
var btn_people: RoundButton
var btn_items: RoundButton
var tray: Tray
var popup: HBoxContainer
var popup_for: Thing = null
var flash_rect: ColorRect
var reset_armed := 0.0
var toast: Label
var toast_t := 0.0

func _ready() -> void:
	layer = 10
	btn_home = RoundButton.new().setup("icon_0", Color("ff8f5c"))
	btn_home.pressed.connect(func(): loc.go_map.emit())
	add_child(btn_home)
	btn_reset = RoundButton.new().setup("icon_2", Color("ffb02e"))
	btn_reset.pressed.connect(_on_reset)
	add_child(btn_reset)
	tray = Tray.new()
	tray.loc = loc
	add_child(tray)
	btn_people = RoundButton.new().setup("icon_3", Color("ff6f8f"), 56)
	btn_people.pressed.connect(func(): _toggle_tray(1))
	add_child(btn_people)
	btn_items = RoundButton.new().setup("icon_4", Color("6c7bff"), 56)
	btn_items.pressed.connect(func(): _toggle_tray(2))
	add_child(btn_items)
	popup = HBoxContainer.new()
	popup.add_theme_constant_override("separation", 14)
	popup.visible = false
	add_child(popup)
	toast = UI.label("", 38, Color.WHITE, 12)
	toast.visible = false
	add_child(toast)
	flash_rect = ColorRect.new()
	flash_rect.color = Color(1, 1, 1, 0)
	flash_rect.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(flash_rect)
	get_viewport().size_changed.connect(_layout)
	_layout()

func _layout() -> void:
	var vs := get_viewport().get_visible_rect().size
	btn_home.position = Vector2(48, 40)
	btn_reset.position = Vector2(vs.x - 152, 40)
	btn_people.position = Vector2(54, vs.y - 148)
	btn_items.position = Vector2(194, vs.y - 148)
	flash_rect.size = vs
	tray.layout(vs)

func _toggle_tray(tab: int) -> void:
	tray.set_tab(0 if tray.tab == tab else tab)
	btn_people.active = tray.tab == 1
	btn_items.active = tray.tab == 2

func _on_reset() -> void:
	if reset_armed > 0:
		reset_armed = 0
		loc.reset_place()
		say("Fresh start!")
	else:
		reset_armed = 3
		say("Tap again to reset this place")

func say(s: String) -> void:
	toast.text = s
	toast.visible = true
	toast_t = 2.2
	toast.reset_size()
	toast.position = Vector2((get_viewport().get_visible_rect().size.x - toast.size.x) / 2, 60)

func flash() -> void:
	flash_rect.color.a = 0.85

func update_ui(dt: float) -> void:
	reset_armed = maxf(0, reset_armed - dt)
	flash_rect.color.a = maxf(0, flash_rect.color.a - dt * 2.5)
	if toast_t > 0:
		toast_t -= dt
		toast.modulate.a = clampf(toast_t * 3, 0, 1)
		toast.visible = toast_t > 0
	_update_popup()

func _update_popup() -> void:
	var s := loc.sel
	var show := s != null and loc.things.has(s) and not (loc.drag != null and loc.moved) and s.state != Thing.HELD
	if not show:
		popup.visible = false
		popup_for = null
		return
	if popup_for != s:
		popup_for = s
		for c in popup.get_children():
			c.queue_free()
		var defs := []
		if s.is_char:
			defs.append(["icon_11", "6c7bff", "edit"])
			defs.append(["icon_12", "3cc5af", "emote"])
		defs.append_array([["icon_13", "4fb3ff", "flip"], ["icon_5", "58b368", "big"], ["icon_6", "58b368", "small"]])
		if not s.is_char:
			defs.append(["icon_14", "8e7bff", "copy"])   # characters are unique, props can be copied
		defs.append(["icon_10", "ff5c73", "delete"])
		for d in defs:
			var b := RoundButton.new().setup(d[0], Color(d[1]), 36)
			b.pressed.connect(_popup_action.bind(d[2]))
			popup.add_child(b)
		popup.reset_size()
	popup.visible = true
	var top_y: float = s.y + s.dyo - s.bh * s.sc
	for q in loc.things:
		if q.link == s and q.is_char and q.state == Thing.SIT:
			top_y = minf(top_y, q.y + q.dyo - q.bh * q.sc)
	var sp: Vector2 = loc.world.position + Vector2(s.x, top_y - 120) * loc.world.scale.x
	var vs := get_viewport().get_visible_rect().size
	var w := popup.get_child_count() * 86.0
	if sp.y < 150:
		sp.y = minf(vs.y - 340, loc.world.position.y + (s.y + 30) * loc.world.scale.x)
	popup.position = Vector2(clampf(sp.x - w / 2, 20, vs.x - w - 20), sp.y)

func _popup_action(a: String) -> void:
	var s := loc.sel
	if s == null:
		return
	match a:
		"edit":
			loc.edit_char.emit(s as Character)
		"makeover":
			var c := s as Character
			c.set_look(Look.random_look())
			c.hop_v = 600
			loc.fx.burst(Vector2(c.x, c.y - c.bh * c.sc * 0.5), 16, Color("ffe066"), 2, 500)
			Sfx.play("tada")
		"emote":
			var c2 := s as Character
			c2.do_emote((c2.emote + 1 + randi() % 6) % 7); c2.hop_v = 700
		"flip":
			s.flip = not s.flip
		"big":
			s.sc = minf(2.4 * (Character.SCENE_SCALE if s.is_char else 1.0), s.sc * 1.15)
		"small":
			s.sc = maxf(0.4 * (Character.SCENE_SCALE if s.is_char else 1.0), s.sc / 1.15)
		"copy":
			var n: Thing
			if s.is_char:
				n = loc.make_char((s as Character).look.copy(), minf(loc.x_max, s.x + 90), s.y)
			else:
				n = loc.make_prop(s.id, minf(loc.x_max, s.x + 90), s.y)
			n.sc = s.sc; n.flip = s.flip; n.pop = 0
			loc.sel = n
			Sfx.play("pop")
		"delete":
			if s.held != null:
				loc.put_down(s as Character)
			loc.remove_thing(s)
			Sfx.play("whoosh")
	popup_for = null
