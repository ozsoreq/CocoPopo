class_name Look
extends RefCounted
## A character's appearance; every field indexes into a palette / option list.

const SKIN := [0xFFE3CC, 0xFBCFA8, 0xEBB083, 0xCD8E5E, 0xA66A40, 0x7B4A2B, 0x55321F, 0xC9ECB4, 0xCDB8F2, 0xA8DCF5]
const HAIR := [0x2D2A32, 0x4B2E1E, 0x7C4B2A, 0xD9692B, 0xF3C95E, 0xF48DB6, 0x5BA8F2, 0x9B6CDC, 0xDADAE4, 0x3CC5AF]
const CLOTH := [0xFF5C73, 0xFF9A3D, 0xFFD43B, 0x7ED957, 0x2FC4A0, 0x4FB3FF, 0x6C7BFF, 0xB67CFF, 0xFF8FD0, 0xFFFFFF, 0x4A4F63, 0x9B6B4A]
const PANTS := [0x4B6FD6, 0x2E3550, 0x6B7287, 0xB8895A, 0xE8546B, 0x58B368, 0xF1B84B, 0x9B6CDC]
const SHOES := [0xF7F4F0, 0xFF5C73, 0x4FB3FF, 0xFFD43B, 0xFF8FD0, 0x3A3346, 0x58B368, 0xB67CFF]
const N_HAIR := 13
const N_EYES := 6
const N_MOUTH := 6
const N_TOP := 6
const N_ACC := 11
const N_BODY := 6
const N_HEAD := 3
const N_BSTYLE := 3
## legLen factor, torso width, torso height, head scale
const BODY := [[1.0, 1.0, 1.0, 1.0], [0.55, 0.94, 0.8, 1.02], [1.5, 0.94, 1.18, 0.9],
	[1.42, 1.2, 1.28, 0.9], [1.05, 1.36, 1.12, 0.94], [1.2, 1.1, 1.12, 0.92]]
const TINTED_ACC := [3, 4, 6, 7, 8, 9]

var skin := 0
var hair_style := 0
var hair_color := 0
var eyes := 0
var mouth := 0
var top := 0
var top_color := 0
var bottom := 0
var acc := 0
var acc_color := 0
var body := 0
var head := 0
var bstyle := 0
var shoe := 0
var freckles := 0

static func col(hex: int) -> Color:
	return Color.hex((hex << 8) | 0xFF)

static func make(a: Array) -> Look:
	var l := Look.new()
	l.skin = a[0]; l.hair_style = a[1]; l.hair_color = a[2]; l.eyes = a[3]; l.mouth = a[4]
	l.top = a[5]; l.top_color = a[6]; l.bottom = a[7]; l.acc = a[8]; l.acc_color = a[9]
	l.body = a[10]; l.head = a[11]; l.bstyle = a[12]; l.shoe = a[13]; l.freckles = a[14]
	return l

func to_array() -> Array:
	return [skin, hair_style, hair_color, eyes, mouth, top, top_color, bottom, acc, acc_color, body, head, bstyle, shoe, freckles]

func copy() -> Look:
	return Look.make(to_array())

static func random_look() -> Look:
	var l := Look.new()
	l.skin = randi() % 7 if randi() % 12 != 0 else 7 + randi() % 3
	l.hair_style = randi() % N_HAIR
	l.hair_color = randi() % HAIR.size()
	l.eyes = randi() % N_EYES
	l.mouth = randi() % N_MOUTH
	l.top = randi() % N_TOP
	l.top_color = randi() % CLOTH.size()
	l.bottom = randi() % PANTS.size()
	l.acc = 1 + randi() % (N_ACC - 1) if randi() % 3 == 0 else 0
	l.acc_color = randi() % CLOTH.size()
	l.body = randi() % N_BODY
	l.head = randi() % N_HEAD
	l.bstyle = randi() % N_BSTYLE
	l.shoe = randi() % SHOES.size()
	l.freckles = 1 if randi() % 3 == 0 else 0
	return l

func b() -> Array:
	return BODY[body % BODY.size()]

func hip_y() -> float:
	return -98.0 * b()[0]

func neck_y() -> float:
	return hip_y() - 98.0 * b()[2]

func height() -> float:
	return -neck_y() + 170.0 * b()[3]

const PRESET_NAMES := ["Coco", "Popo", "Mia", "Leo", "Nana", "Dr. Pip", "Teacher", "Baker", "Zed", "Luna", "Kai", "Joy"]
const PRESETS := [
	[1, 7, 1, 1, 1, 2, 0, 0, 6, 8, 0, 0, 2, 4, 0],
	[3, 6, 0, 5, 1, 1, 5, 1, 3, 0, 0, 1, 1, 1, 1],
	[5, 5, 0, 2, 5, 3, 2, 0, 0, 0, 0, 0, 0, 2, 0],
	[0, 1, 4, 0, 0, 4, 3, 0, 0, 0, 1, 2, 0, 3, 1],
	[0, 2, 8, 3, 0, 3, 7, 2, 1, 0, 5, 0, 0, 5, 0],
	[2, 1, 1, 5, 2, 5, 9, 2, 0, 0, 3, 1, 0, 5, 0],
	[4, 10, 1, 5, 1, 0, 4, 1, 1, 0, 3, 0, 2, 5, 0],
	[2, 2, 3, 4, 1, 4, 9, 6, 4, 9, 4, 2, 0, 1, 1],
	[7, 9, 7, 5, 3, 2, 6, 1, 7, 5, 2, 1, 0, 2, 0],
	[1, 11, 5, 2, 4, 3, 8, 7, 10, 2, 0, 0, 0, 4, 1],
	[4, 1, 4, 0, 1, 0, 4, 5, 2, 0, 2, 0, 1, 2, 0],  # Kai - teen surfer, sunglasses, shorts
	[6, 8, 6, 2, 2, 1, 8, 2, 8, 2, 0, 2, 2, 7, 1],  # Joy - pigtails, party hat, skirt
]

static func preset(i: int) -> Look:
	return Look.make(PRESETS[i % PRESETS.size()])
