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
	_swap(m)

func go_place(place: String) -> void:
	if busy:
		return
	busy = true
	Sfx.play("whoosh")
	await _fade_to(1.0)
	var l := Location.new()
	l.setup(place)
	l.go_map.connect(go_map)
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
		if screen is Location:
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
