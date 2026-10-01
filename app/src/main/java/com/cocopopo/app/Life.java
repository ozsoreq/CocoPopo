package com.cocopopo.app;

/** Rules of the dollhouse world: what can be sat on, slept in, stood on, held, eaten or toggled. */
final class Life {
    private Life() {}

    /** Seat surface height (local, negative = up) followed by slot x offsets, or null when not a seat. */
    static float[] seat(String id) {
        switch (id) {
            case "chair": return new float[]{-96, 0};
            case "sofa": return new float[]{-110, -86, 86};
            case "armchair": return new float[]{-106, 0};
            case "bench": return new float[]{-96, -90, 90};
            case "toilet": return new float[]{-104, 4};
            case "swing": return new float[]{-92, 0};
            case "wheelchair": return new float[]{-86, -8};
            default: return null;
        }
    }

    /** Mattress height for beds, or 0 when not a bed. */
    static float bed(String id) {
        if (id.equals("bed")) return -150;
        if (id.equals("hbed")) return -142;
        return 0;
    }

    /** Height of a top surface small items can be placed on, or 0. */
    static float surface(String id) {
        switch (id) {
            case "table": return -158;
            case "desk": return -150;
            case "dresser": return -200;
            case "register": return -122;
            case "tv": return 0;
            default: return 0;
        }
    }

    static boolean wall(String id) { return id.equals("frame") || id.equals("clock"); }

    static boolean floats(String id) { return id.equals("balloon"); }

    static boolean food(String id) {
        switch (id) {
            case "cake": case "pizza": case "burger": case "icecream": case "donut": case "apple":
            case "juice": case "cupcake": case "coffee": case "popcorn": case "cotton":
                return true;
            default: return false;
        }
    }

    static boolean holdable(Obj o) {
        if (o.isChar || wall(o.prop) || seat(o.prop) != null || bed(o.prop) != 0) return false;
        return Math.max(o.bw, o.bh) * o.scale <= 200 || o.prop.equals("balloon") || o.prop.equals("guitar") || o.prop.equals("surfboard");
    }

    /** Small things can be tossed and bounce. */
    static boolean tossable(Obj o) { return !o.isChar && Math.max(o.bw, o.bh) * o.scale <= 260 && !wall(o.prop) && !floats(o.prop); }

    /** Props that react to a tap by changing state; returns number of states (0 = not interactive). */
    static int states(String id) {
        switch (id) {
            case "lamp": return 2;
            case "tv": return 4;
            case "fridge": return 2;
            case "stove": return 2;
            case "gift": return 2;
            default: return 0;
        }
    }

    /** Walkable floor bands for each place: pairs of {top, bottom} y values for feet. */
    static float[] floors(String loc) {
        switch (loc) {
            case "home": return new float[]{470, 560, 912, 1076};
            case "school": return new float[]{662, 1076};
            case "hospital": return new float[]{682, 1076};
            case "market": return new float[]{706, 1076};
            case "cafe": return new float[]{700, 1076};
            case "park": return new float[]{664, 1076};
            case "beach": return new float[]{800, 1076};
            default: return new float[]{790, 1076};
        }
    }

    /** Where something released at (x, y) comes to rest on the floor. */
    static float floorBelow(String loc, float y) {
        float[] f = floors(loc);
        for (int i = 0; i < f.length; i += 2) {
            if (y <= f[i]) return f[i] + 14;
            if (y <= f[i + 1]) return y;
        }
        return f[f.length - 1];
    }

    /** The floor band index containing y (or the one it falls onto). */
    static int band(String loc, float y) {
        float[] f = floors(loc);
        for (int i = 0; i < f.length; i += 2) if (y <= f[i + 1]) return i;
        return f.length - 2;
    }
}
