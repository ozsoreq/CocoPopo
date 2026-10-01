extends Node
## Tiny sound manager: a pool of players and pitch-varied one-shots.

var _streams := {}
var _players: Array[AudioStreamPlayer] = []
var _last := {}

func _ready() -> void:
	for n in ["pop", "drop", "bite", "toggle", "tick", "bounce", "spark", "whoosh", "splash", "tada",
			"strum", "squeak", "wheee", "vroom", "yawn", "swoosh_map", "meow", "woof", "sizzle", "blend"]:
		var s = load("res://sfx/%s.wav" % n)
		if s:
			_streams[n] = s
	for i in 8:
		var p := AudioStreamPlayer.new()
		add_child(p)
		_players.append(p)

func play(name: String, vol_db := -4.0) -> void:
	if not _streams.has(name):
		return
	var now := Time.get_ticks_msec()
	if now - int(_last.get(name, -1000)) < 60:
		return
	_last[name] = now
	for p in _players:
		if not p.playing:
			p.stream = _streams[name]
			p.pitch_scale = randf_range(0.92, 1.1)
			p.volume_db = vol_db
			p.play()
			return
