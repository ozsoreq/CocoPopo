extends Node2D
## Screen manager: the rotating world and the places, with a bubbly fade between them.

var screen: Node = null
var fade: ColorRect
var fade_layer: CanvasLayer
var busy := false
var test_mode := ""

func _ready() -> void:
	get_tree().set_auto_accept_quit(false)
	get_tree().set_quit_on_go_back(false)
	fade_layer = CanvasLayer.new()
	fade_layer.layer = 100
	add_child(fade_layer)
	fade = ColorRect.new()
	fade.color = Color("fff1c9")
	fade.mouse_filter = Control.MOUSE_FILTER_IGNORE
	fade.modulate.a = 0
	fade_layer.add_child(fade)
	get_viewport().size_changed.connect(func(): fade.size = get_viewport().get_visible_rect().size)
	fade.size = get_viewport().get_visible_rect().size
	_show_map()
	var args := OS.get_cmdline_user_args()
	for a in args:
		if a.begins_with("--test="):
			test_mode = a.substr(7)
	if test_mode != "":
		var tester := Node.new()
		tester.set_script(load("res://scripts/tester.gd"))
		add_child(tester)
		tester.run(self, test_mode)

func _show_map() -> void:
	var m := WorldMap.new()
	m.enter.connect(go_place)
	m.dress_up.connect(func(): open_editor(Look.random_look(), "New character", _add_to_lib))
	_swap(m)

var editor: DressUp = null

## Opens the dress-up editor over the current screen; on_done(look) runs when the player taps the tick.
func open_editor(start: Look, title: String, on_done: Callable) -> void:
	if editor != null or busy:
		return
	busy = true
	await _fade_to(1.0)
	editor = DressUp.new()
	editor.open(start, title)
	editor.finished.connect(_editor_closed.bind(on_done))
	add_child(editor)
	if screen:
		screen.process_mode = Node.PROCESS_MODE_DISABLED
		_set_screen_ui_visible(false)
	await _fade_to(0.0)
	busy = false

func _editor_closed(look: Look, saved: bool, on_done: Callable) -> void:
	if busy:
		return
	busy = true
	await _fade_to(1.0)
	editor.queue_free()
	editor = null
	if screen:
		screen.process_mode = Node.PROCESS_MODE_INHERIT
		_set_screen_ui_visible(true)
	if saved and look != null:
		on_done.call(look)
	await _fade_to(0.0)
	busy = false

func _set_screen_ui_visible(v: bool) -> void:
	for c in screen.get_children():
		if c is CanvasLayer:
			c.visible = v

func _add_to_lib(l: Look) -> void:
	var lib: Array = Save.data.get("lib", [])
	lib.append(l.to_array())
	Save.data["lib"] = lib
	Save.write()
	Sfx.play("tada")

func go_place(place: String) -> void:
	if busy:
		return
	busy = true
	Sfx.play("whoosh")
	await _fade_to(1.0)
	var l := Location.new()
	l.setup(place)
	l.go_map.connect(go_map)
	l.edit_char.connect(func(c: Character): open_editor(c.look, "Edit character", func(nl: Look): c.set_look(nl); c.hop_v = 600))
	l.new_char.connect(func(): open_editor(Look.random_look(), "New character", func(nl: Look): _add_to_lib(nl); l.spawn_char(nl)))
	_swap(l)
	await _fade_to(0.0)
	busy = false

func go_map() -> void:
	if busy:
		return
	busy = true
	if screen is Location:
		(screen as Location).save_state()
	await _fade_to(1.0)
	_show_map()
	await _fade_to(0.0)
	busy = false

func _swap(n: Node) -> void:
	if screen != null:
		screen.queue_free()
	screen = n
	add_child(n)

func _fade_to(a: float) -> void:
	var tw := create_tween()
	tw.tween_property(fade, "modulate:a", a, 0.22)
	await tw.finished

func _notification(what: int) -> void:
	if what == NOTIFICATION_WM_GO_BACK_REQUEST:
		if editor != null:
			editor.finished.emit(null, false)
		elif screen is Location:
			var l := screen as Location
			if l.ui.tray.tab != 0:
				l.ui._toggle_tray(l.ui.tray.tab)
			else:
				go_map()
		else:
			get_tree().quit()
	elif what == NOTIFICATION_WM_CLOSE_REQUEST:
		if screen is Location:
			(screen as Location).save_state()
		get_tree().quit()
	elif what == NOTIFICATION_APPLICATION_PAUSED or what == NOTIFICATION_APPLICATION_FOCUS_OUT:
		if screen is Location:
			(screen as Location).save_state()
