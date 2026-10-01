package com.cocopopo.app;

import java.util.Random;

/** A character's appearance. Every field indexes into a palette / option list. */
final class Look {
    static final int[] SKIN = {0xFFFFE3CC, 0xFFFBCFA8, 0xFFEBB083, 0xFFCD8E5E, 0xFFA66A40, 0xFF7B4A2B, 0xFF55321F, 0xFFC9ECB4, 0xFFCDB8F2, 0xFFA8DCF5};
    static final int[] HAIR = {0xFF2D2A32, 0xFF4B2E1E, 0xFF7C4B2A, 0xFFD9692B, 0xFFF3C95E, 0xFFF48DB6, 0xFF5BA8F2, 0xFF9B6CDC, 0xFFDADAE4, 0xFF3CC5AF};
    static final int[] CLOTH = {0xFFFF5C73, 0xFFFF9A3D, 0xFFFFD43B, 0xFF7ED957, 0xFF2FC4A0, 0xFF4FB3FF, 0xFF6C7BFF, 0xFFB67CFF, 0xFFFF8FD0, 0xFFFFFFFF, 0xFF4A4F63, 0xFF9B6B4A};
    static final int[] PANTS = {0xFF4B6FD6, 0xFF2E3550, 0xFF6B7287, 0xFFB8895A, 0xFFE8546B, 0xFF58B368, 0xFFF1B84B, 0xFF9B6CDC};

    static final int N_HAIR = 9, N_EYES = 5, N_MOUTH = 6, N_TOP = 6, N_ACC = 8;
    static final String[] HAIR_NAMES = {"Bald", "Short", "Bob", "Long", "Ponytail", "Afro", "Spiky", "Buns", "Pigtails"};

    int skin, hairStyle, hairColor, eyes, mouth, top, topColor, bottom, acc, accColor;

    Look copy() {
        Look l = new Look();
        l.skin = skin; l.hairStyle = hairStyle; l.hairColor = hairColor; l.eyes = eyes; l.mouth = mouth;
        l.top = top; l.topColor = topColor; l.bottom = bottom; l.acc = acc; l.accColor = accColor;
        return l;
    }

    String encode() {
        return skin + "," + hairStyle + "," + hairColor + "," + eyes + "," + mouth + "," + top + "," + topColor + "," + bottom + "," + acc + "," + accColor;
    }

    static Look decode(String s) {
        Look l = new Look();
        try {
            String[] p = s.split(",");
            l.skin = Integer.parseInt(p[0]) % SKIN.length;
            l.hairStyle = Integer.parseInt(p[1]) % N_HAIR;
            l.hairColor = Integer.parseInt(p[2]) % HAIR.length;
            l.eyes = Integer.parseInt(p[3]) % N_EYES;
            l.mouth = Integer.parseInt(p[4]) % N_MOUTH;
            l.top = Integer.parseInt(p[5]) % N_TOP;
            l.topColor = Integer.parseInt(p[6]) % CLOTH.length;
            l.bottom = Integer.parseInt(p[7]) % PANTS.length;
            l.acc = Integer.parseInt(p[8]) % N_ACC;
            l.accColor = Integer.parseInt(p[9]) % CLOTH.length;
        } catch (Exception e) {
            return PRESETS[0].copy();
        }
        return l;
    }

    static Look make(int skin, int hs, int hc, int eyes, int mouth, int top, int tc, int bottom, int acc, int ac) {
        Look l = new Look();
        l.skin = skin; l.hairStyle = hs; l.hairColor = hc; l.eyes = eyes; l.mouth = mouth;
        l.top = top; l.topColor = tc; l.bottom = bottom; l.acc = acc; l.accColor = ac;
        return l;
    }

    static Look random(Random r) {
        Look l = new Look();
        l.skin = r.nextInt(7);
        if (r.nextInt(12) == 0) l.skin = 7 + r.nextInt(3);
        l.hairStyle = r.nextInt(N_HAIR);
        l.hairColor = r.nextInt(HAIR.length);
        l.eyes = r.nextInt(N_EYES);
        l.mouth = r.nextInt(N_MOUTH);
        l.top = r.nextInt(N_TOP);
        l.topColor = r.nextInt(CLOTH.length);
        l.bottom = r.nextInt(PANTS.length);
        l.acc = r.nextInt(3) == 0 ? 1 + r.nextInt(N_ACC - 1) : 0;
        l.accColor = r.nextInt(CLOTH.length);
        return l;
    }

    /** Ready-made cast, shown in the tray. */
    static final String[] PRESET_NAMES = {"Coco", "Popo", "Mia", "Leo", "Nana", "Dr. Pip", "Teacher", "Baker", "Zed", "Luna"};
    static final Look[] PRESETS = {
        make(1, 7, 1, 1, 1, 2, 0, 0, 6, 8),   // Coco  - buns, hoodie
        make(3, 6, 0, 0, 1, 1, 5, 1, 3, 0),   // Popo  - spiky, stripes, cap
        make(5, 5, 0, 2, 5, 3, 2, 0, 0, 0),   // Mia   - afro, dress
        make(0, 1, 4, 0, 0, 4, 3, 0, 0, 0),   // Leo   - short blond, overalls
        make(0, 2, 8, 3, 0, 3, 7, 2, 1, 0),   // Nana  - grey bob, dress, glasses
        make(2, 1, 1, 0, 2, 5, 9, 2, 0, 0),   // Dr Pip - coat
        make(4, 4, 1, 0, 1, 0, 4, 1, 1, 0),   // Teacher
        make(2, 2, 3, 4, 1, 4, 9, 6, 4, 9),   // Baker - beanie
        make(7, 0, 0, 2, 3, 2, 6, 1, 7, 5),   // Zed   - green, headphones
        make(1, 8, 5, 2, 4, 3, 8, 7, 5, 2)    // Luna  - pigtails, crown
    };
}
