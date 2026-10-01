class_name Rules
extends RefCounted
## The rules of the dollhouse world (ported from the Java version's Life.java).
const CATS := ["Home", "Food", "Toys", "Outdoors", "Stuff", "Pets"]
## [category, id, name, width, height]
const PROPS := [
	[0, "bed", "Bed", 370, 240],
	[0, "sofa", "Sofa", 350, 190],
	[0, "armchair", "Armchair", 200, 190],
	[0, "table", "Table", 310, 160],
	[0, "chair", "Chair", 120, 190],
	[0, "shelf", "Bookshelf", 230, 350],
	[0, "lamp", "Lamp", 100, 310],
	[0, "tv", "TV", 270, 230],
	[0, "fridge", "Fridge", 180, 350],
	[0, "tub", "Bathtub", 350, 200],
	[0, "toilet", "Toilet", 130, 170],
	[0, "dresser", "Dresser", 240, 210],
	[0, "stove", "Stove", 200, 230],
	[0, "desk", "School desk", 240, 180],
	[0, "plant", "Plant", 130, 210],
	[0, "rug", "Rug", 380, 110],
	[1, "cake", "Cake", 150, 150],
	[1, "pizza", "Pizza", 140, 110],
	[1, "burger", "Burger", 130, 120],
	[1, "icecream", "Ice cream", 90, 170],
	[1, "donut", "Donut", 120, 90],
	[1, "apple", "Apple", 90, 100],
	[1, "juice", "Juice", 80, 130],
	[1, "cupcake", "Cupcake", 100, 120],
	[2, "ball", "Ball", 110, 110],
	[2, "teddy", "Teddy", 140, 180],
	[2, "balloon", "Balloon", 100, 290],
	[2, "car", "Toy car", 210, 110],
	[2, "blocks", "Blocks", 160, 140],
	[2, "guitar", "Guitar", 110, 250],
	[2, "duck", "Duck", 120, 110],
	[2, "rocket", "Rocket", 110, 250],
	[3, "tree", "Tree", 290, 450],
	[3, "palm", "Palm", 270, 450],
	[3, "flower", "Flower", 90, 160],
	[3, "bush", "Bush", 240, 140],
	[3, "mushroom", "Mushroom", 110, 120],
	[3, "rock", "Rock", 160, 110],
	[3, "umbrella", "Umbrella", 280, 300],
	[3, "sandcastle", "Sand castle", 190, 160],
	[3, "swing", "Swing", 320, 340],
	[3, "slide", "Slide", 320, 330],
	[4, "backpack", "Backpack", 120, 150],
	[4, "books", "Books", 140, 90],
	[4, "globe", "Globe", 120, 160],
	[4, "bench", "Bench", 320, 180],
	[4, "frame", "Picture", 130, 160],
	[4, "gift", "Gift", 130, 130],
	[4, "trophy", "Trophy", 120, 170],
	[4, "camera", "Camera", 130, 100],
	[0, "hbed", "Hospital bed", 380, 240],
	[4, "ivstand", "IV stand", 110, 350],
	[4, "crate", "Fruit crate", 180, 120],
	[4, "register", "Register", 200, 180],
	[1, "coffee", "Coffee", 80, 90],
	[3, "surfboard", "Surf board", 110, 300],
	[1, "popcorn", "Popcorn", 110, 160],
	[1, "cotton", "Cotton candy", 100, 210],
	[1, "coconut", "Coconut", 80, 80],
	[4, "medkit", "Med kit", 130, 110],
	[4, "cart", "Cart", 200, 190],
	[4, "wheelchair", "Wheelchair", 170, 210],
	[4, "clock", "Clock", 120, 120],
	[1, "egg", "Egg", 60, 70],
	[1, "bread", "Bread", 110, 80],
	[1, "banana", "Banana", 110, 70],
	[1, "strawberry", "Strawberry", 70, 80],
	[1, "tomato", "Tomato", 80, 90],
	[1, "corn", "Corn", 80, 130],
	[1, "friedegg", "Fried egg", 120, 55],
	[1, "toast", "Toast", 100, 105],
	[1, "soup", "Soup", 130, 95],
	[1, "smoothie", "Smoothie", 70, 150],
	[1, "milkshake", "Milkshake", 80, 170],
	[0, "blender", "Blender", 100, 200],
	[5, "cat", "Cat", 140, 150],
	[5, "dog", "Dog", 150, 150],
	[5, "bunny", "Bunny", 120, 170],
	[5, "petbed", "Pet bed", 200, 70],
	[4, "door", "Door", 180, 285],
	[4, "busstop", "Bus stop", 170, 315],
]
## [id, name, colour]
const PLACES := [
	["home", "Cozy Home", 0xFF8FA3],
	["school", "School", 0x5BC0F8],
	["hospital", "Hospital", 0x6DD3C8],
	["market", "Market", 0xFFC043],
	["cafe", "Café", 0xE09A62],
	["park", "Park", 0x7ED957],
	["beach", "Beach", 0x4FD0E6],
	["fair", "Funfair", 0xB67CFF],
]
## starting contents: "prop:xFraction:y:scale" or "@presetIndex:xFraction:y:scale"
const DEFAULTS := {
	"home": ["rug:.17:520:1", "bed:.12:530:1", "dresser:.33:535:.95", "lamp:.41:535:.9", "plant:.02:545:.8", "tub:.66:540:.95", "toilet:.88:545:.85", "frame:.28:360:.9", "rug:.2:975:1", "sofa:.18:965:1", "tv:.38:960:.95", "plant:.05:970:.9", "fridge:.62:965:1", "stove:.75:962:.95", "table:.84:970:.85", "chair:.78:975:.8", "@0:.42:525:1", "@1:.3:968:1", "duck:.66:500:1", "cat:.47:540:1", "petbed:.245:548:.9", "blender:.84:836:.85", "door:.955:968:.85"],
	"school": ["desk:.22:860:1", "desk:.42:860:1", "desk:.62:860:1", "desk:.22:1010:1", "desk:.42:1010:1", "desk:.62:1010:1", "shelf:.93:800:.9", "globe:.82:760:.8", "clock:.6:200:.8", "backpack:.08:830:.8", "@6:.76:800:1", "@3:.52:945:1", "bunny:.86:880:1", "door:.97:690:.95"],
	"hospital": ["hbed:.2:830:1", "hbed:.64:830:1", "ivstand:.36:820:1", "medkit:.88:800:.9", "plant:.94:820:.9", "wheelchair:.8:1000:1", "@5:.5:860:1", "@4:.14:850:1", "door:.97:715:.95"],
	"market": ["cart:.2:950:1", "crate:.45:800:1", "crate:.6:800:1", "register:.86:790:1", "fridge:.07:770:.9", "apple:.15:700:1", "juice:.36:780:.8", "@7:.74:810:1", "@8:.34:930:1", "door:.97:735:.95"],
	"cafe": ["table:.2:900:.85", "chair:.1:910:.75", "chair:.3:910:.75", "table:.5:1000:.85", "plant:.04:760:.9", "coffee:.16:830:1", "cake:.56:900:1", "cupcake:.64:760:.9", "register:.8:760:.95", "@9:.36:975:1", "blender:.7:440:.85", "door:.22:712:.95"],
	"park": ["tree:.08:700:1", "tree:.92:690:1.1", "swing:.3:760:1", "slide:.64:760:1", "bench:.5:960:1", "flower:.2:900:1", "flower:.24:915:.8", "bush:.78:930:1", "ball:.42:1020:.8", "rock:.9:950:.8", "car:.86:1050:1.5", "mushroom:.58:1010:1", "@2:.7:930:1", "dog:.36:980:1", "busstop:.95:700:1"],
	"beach": ["umbrella:.2:880:1", "palm:.06:740:1", "palm:.93:760:1", "sandcastle:.5:960:1", "surfboard:.78:900:.9", "rug:.22:990:.8", "ball:.65:1010:.8", "icecream:.4:870:.9", "@10:.34:960:1", "busstop:.9:830:1"],
	"fair": ["balloon:.1:900:1", "balloon:.14:910:.9", "balloon:.9:900:1", "popcorn:.3:900:1", "cotton:.7:900:1", "teddy:.55:900:1", "gift:.82:1010:.9", "@11:.35:960:1", "busstop:.95:820:1"],
}

static func prop_def(id: String) -> Array:
	for d in PROPS:
		if d[1] == id:
			return d
	return PROPS[0]

static func seat(id: String) -> Array:
	match id:
		"chair": return [-96.0, 0.0]
		"sofa": return [-110.0, -86.0, 86.0]
		"armchair": return [-106.0, 0.0]
		"bench": return [-96.0, -90.0, 90.0]
		"toilet": return [-104.0, 4.0]
		"swing": return [-92.0, 0.0]
		"wheelchair": return [-86.0, -8.0]
		"car": return [-58.0, -14.0]
		"rock": return [-70.0, 0.0]
	return []

static func vehicle(id: String) -> bool:
	return id == "car" or id == "wheelchair"

static func covers_sitter(id: String) -> bool:
	return id == "car"

static func bed(id: String) -> float:
	if id == "bed": return -150.0
	if id == "hbed": return -142.0
	return 0.0

static func is_tub(id: String) -> bool: return id == "tub"
static func is_slide(id: String) -> bool: return id == "slide"
static func bouncy(id: String) -> bool: return id == "mushroom"

static func surface(id: String) -> float:
	match id:
		"table": return -158.0
		"desk": return -150.0
		"dresser": return -200.0
		"register": return -122.0
		"tv": return -228.0
	return 0.0

static func capacity(id: String) -> int:
	match id:
		"fridge": return 8
		"cart", "crate": return 6
		"backpack", "tub": return 4
		"gift": return 3
		"shelf": return 8
	return 0

static func wall(id: String) -> bool: return id == "frame" or id == "clock"
static func floats(id: String) -> bool: return id == "balloon"

static func food(id: String) -> bool:
	return id in ["cake", "pizza", "burger", "icecream", "donut", "apple", "juice", "cupcake", "coffee", "popcorn", "cotton", "coconut",
		"bread", "banana", "strawberry", "tomato", "friedegg", "toast", "soup", "smoothie", "milkshake"]

static func drink(id: String) -> bool: return id in ["juice", "coffee", "smoothie", "milkshake"]

## What a kitchen device makes from an ingredient ("" if nothing).
static func recipe(device: String, ingredient: String) -> String:
	match device:
		"stove":
			match ingredient:
				"egg": return "friedegg"
				"bread": return "toast"
				"tomato": return "soup"
				"corn": return "popcorn"
		"blender":
			match ingredient:
				"banana": return "smoothie"
				"strawberry": return "milkshake"
				"apple": return "juice"
	return ""

static func pet(id: String) -> bool: return id in ["cat", "dog", "bunny"]

static func pet_sound(id: String) -> String:
	match id:
		"cat": return "meow"
		"dog": return "woof"
	return "squeak"

## Doors and bus stops take characters to other places.
static func travel(id: String) -> bool: return id == "door" or id == "busstop"

## Props that land on tables and counters instead of the floor.
static func rests_on_surfaces(id: String) -> bool: return tossable(id) or id == "blender"

## Lit things and where their light comes from (offset, colour, size) when the lights are on.
static func light_of(p: Thing) -> Array:
	match p.id:
		"lamp": return [Vector2(0, -250), Color(1, 0.86, 0.55), 2.6] if p.pstate == 1 else []
		"tv": return [Vector2(0, -150), Color(0.6, 0.8, 1), 2.0] if p.pstate != 1 else []
		"fridge": return [Vector2(-20, -200), Color(0.8, 0.95, 1), 2.0] if p.pstate == 1 else []
		"stove": return [Vector2(0, -110), Color(1, 0.6, 0.3), 1.6] if p.pstate == 1 else []
		"camera", "rocket": return []
	return []

## Things added to an existing save if the place has none yet (new features reach old saves).
const ENSURE := ["door", "busstop", "cat", "dog", "bunny", "petbed", "blender"]

## Starting contents of containers in a place.
static func starter(loc: String, id: String) -> Array:
	match loc + ":" + id:
		"home:fridge": return ["egg", "bread", "banana", "tomato", "corn"]
		"market:crate": return ["banana", "tomato", "corn", "egg", "strawberry", "bread"]
		"cafe:fridge", "market:fridge": return ["strawberry", "banana", "egg"]
	return []

static func holdable(id: String) -> bool:
	return food(id) or pet(id) or id in ["egg", "corn", "ball", "teddy", "balloon", "blocks", "guitar", "duck", "flower", "mushroom", "backpack", "books",
		"globe", "gift", "trophy", "camera", "medkit", "plant", "surfboard", "crate", "rocket"]

## 0 one hand, 1 both hands in front, 2 overhead
static func hold_type(id: String) -> int:
	if pet(id) or id in ["teddy", "gift", "crate", "books", "globe", "cake", "blocks", "plant", "medkit", "pizza", "backpack", "rocket", "soup", "bread"]:
		return 1
	if id in ["balloon", "trophy"]:
		return 2
	return 0

static func tossable(id: String) -> bool: return holdable(id) and not floats(id) and not pet(id)

static func states(id: String) -> int:
	match id:
		"lamp", "fridge", "stove", "gift", "umbrella", "blender", "door", "cat", "dog", "bunny": return 2
		"tv": return 4
	return 0

static func floors(loc: String) -> Array:
	match loc:
		"home": return [470.0, 560.0, 912.0, 1076.0]
		"school": return [662.0, 1076.0]
		"hospital": return [682.0, 1076.0]
		"market": return [706.0, 1076.0]
		"cafe": return [700.0, 1076.0]
		"park": return [664.0, 1076.0]
		"beach": return [800.0, 1076.0]
	return [790.0, 1076.0]

## x fraction of the ladder linking floors, or -1
static func ladder(loc: String) -> float:
	return 0.5 if loc == "home" else -1.0

static func floor_below(loc: String, y: float) -> float:
	var f := floors(loc)
	for i in range(0, f.size(), 2):
		if y <= f[i]: return f[i] + 14.0
		if y <= f[i + 1]: return y
	return f[f.size() - 1]

static func band(loc: String, y: float) -> int:
	var f := floors(loc)
	for i in range(0, f.size(), 2):
		if y <= f[i + 1]: return i
	return f.size() - 2
