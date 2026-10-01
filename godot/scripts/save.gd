extends Node
## Persists every place and the custom characters in user://.

const PATH := "user://cocopopo.json"
var data := {}

const VERSION := 2

func _ready() -> void:
	if FileAccess.file_exists(PATH):
		var d = JSON.parse_string(FileAccess.get_file_as_string(PATH))
		if d is Dictionary:
			data = d
	_migrate()

func _migrate() -> void:
	var ver := int(data.get("ver", 1))
	if ver < 2:
		# v2: characters are half size in places; library entries get identities
		for k in data.keys():
			if data[k] is Array and k != "lib":
				for r in data[k]:
					if r is Dictionary and r.get("c", false):
						r["sc"] = float(r["sc"]) * 0.5
		var lib: Array = data.get("lib", [])
		var nl := []
		for e in lib:
			nl.append(e if e is Dictionary else {"id": new_id(), "look": e})
		data["lib"] = nl
	data["ver"] = VERSION

func new_id() -> String:
	return "%d%04d" % [Time.get_unix_time_from_system(), randi() % 10000]

func lib() -> Array:
	return data.get("lib", [])

## Adds a look to My characters and returns its identity ("l<id>").
func add_to_lib(look_arr: Array) -> String:
	var e := {"id": new_id(), "look": look_arr}
	var l := lib()
	l.append(e)
	data["lib"] = l
	write()
	return "l" + e["id"]

func update_lib(cid: String, look_arr: Array) -> void:
	for e in lib():
		if "l" + str(e["id"]) == cid:
			e["look"] = look_arr
	write()

## True if a character with this identity is saved in any place.
func cid_anywhere(cid: String) -> bool:
	for k in data.keys():
		if data[k] is Array and k != "lib":
			for r in data[k]:
				if r is Dictionary and r.get("cid", "") == cid:
					return true
	return false

## Removes a character from whichever other place it was saved in; returns its look (or null).
func take_from_other_places(cid: String, here: String):
	for k in data.keys():
		if k == here or k == "lib" or not (data[k] is Array):
			continue
		var recs: Array = data[k]
		for i in recs.size():
			var r = recs[i]
			if r is Dictionary and r.get("cid", "") == cid:
				var look = r["look"]
				recs.remove_at(i)
				for q in recs:
					var ln := int(q["ln"])
					if ln == i:
						q["ln"] = -1; q["st"] = 0
					elif ln > i:
						q["ln"] = ln - 1
				write()
				return look
	return null

func write() -> void:
	var f := FileAccess.open(PATH, FileAccess.WRITE)
	if f:
		f.store_string(JSON.stringify(data))
