package com.cocopopo.app;

import java.util.Random;

/** A character's appearance. Every field indexes into a palette / option list. */
final class Look {
    static final int[] SKIN = {0xFFFFE3CC, 0xFFFBCFA8, 0xFFEBB083, 0xFFCD8E5E, 0xFFA66A40, 0xFF7B4A2B, 0xFF55321F, 0xFFC9ECB4, 0xFFCDB8F2, 0xFFA8DCF5};
    static final int[] HAIR = {0xFF2D2A32, 0xFF4B2E1E, 0xFF7C4B2A, 0xFFD9692B, 0xFFF3C95E, 0xFFF48DB6, 0xFF5BA8F2, 0xFF9B6CDC, 0xFFDADAE4, 0xFF3CC5AF};
    static final int[] CLOTH = {0xFFFF5C73, 0xFFFF9A3D, 0xFFFFD43B, 0xFF7ED957, 0xFF2FC4A0, 0xFF4FB3FF, 0xFF6C7BFF, 0xFFB67CFF, 0xFFFF8FD0, 0xFFFFFFFF, 0xFF4A4F63, 0xFF9B6B4A};
    static final int[] PANTS = {0xFF4B6FD6, 0xFF2E3550, 0xFF6B7287, 0xFFB8895A, 0xFFE8546B, 0xFF58B368, 0xFFF1B84B, 0xFF9B6CDC};

    static final int[] SHOES = {0xFFF7F4F0, 0xFFFF5C73, 0xFF4FB3FF, 0xFFFFD43B, 0xFFFF8FD0, 0xFF3A3346, 0xFF58B368, 0xFFB67CFF};
    static final int N_HAIR = 13, N_EYES = 6, N_MOUTH = 6, N_TOP = 6, N_ACC = 11, N_BODY = 6, N_HEAD = 3, N_BSTYLE = 3;
    static final String[] HAIR_NAMES = {"Bald", "Short", "Bob", "Long", "Ponytail", "Afro", "Spiky", "Buns", "Pigtails", "Mohawk", "Top bun", "Braids", "Curly"};

    int skin, hairStyle, hairColor, eyes, mouth, top, topColor, bottom, acc, accColor;
    int body, head, bstyle, shoe, freckles;

    Look copy() {
        Look l = new Look();
        l.skin = skin; l.hairStyle = hairStyle; l.hairColor = hairColor; l.eyes = eyes; l.mouth = mouth;
        l.top = top; l.topColor = topColor; l.bottom = bottom; l.acc = acc; l.accColor = accColor;
        l.body = body; l.head = head; l.bstyle = bstyle; l.shoe = shoe; l.freckles = freckles;
        return l;
    }

    String encode() {
        return skin + "," + hairStyle + "," + hairColor + "," + eyes + "," + mouth + "," + top + "," + topColor + "," + bottom + "," + acc + "," + accColor
            + "," + body + "," + head + "," + bstyle + "," + shoe + "," + freckles;
    }

    static Look decode(String s) {
        Look l = new Look();
        int[] v = new int[15];
        try {
            String[] p = s.split(",");
            for (int i = 0; i < p.length && i < v.length; i++) v[i] = Math.abs(Integer.parseInt(p[i].trim()));
        } catch (Exception e) {
            return PRESETS[0].copy();
        }
        l.skin = v[0] % SKIN.length; l.hairStyle = v[1] % N_HAIR; l.hairColor = v[2] % HAIR.length;
        l.eyes = v[3] % N_EYES; l.mouth = v[4] % N_MOUTH; l.top = v[5] % N_TOP; l.topColor = v[6] % CLOTH.length;
        l.bottom = v[7] % PANTS.length; l.acc = v[8] % N_ACC; l.accColor = v[9] % CLOTH.length;
        l.body = v[10] % N_BODY; l.head = v[11] % N_HEAD; l.bstyle = v[12] % N_BSTYLE; l.shoe = v[13] % SHOES.length; l.freckles = v[14] % 2;
        return l;
    }

    Look with(int body, int head, int bstyle, int shoe, int freckles) {
        this.body = body; this.head = head; this.bstyle = bstyle; this.shoe = shoe; this.freckles = freckles;
        return this;
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
        l.body = r.nextInt(N_BODY); l.head = r.nextInt(N_HEAD); l.bstyle = r.nextInt(N_BSTYLE);
        l.shoe = r.nextInt(SHOES.length); l.freckles = r.nextInt(3) == 0 ? 1 : 0;
        return l;
    }

    /** Ready-made cast, shown in the tray. */
    static final String[] PRESET_NAMES = {"Coco", "Popo", "Mia", "Leo", "Nana", "Dr. Pip", "Teacher", "Baker", "Zed", "Luna"};
    static final Look[] PRESETS = {
        make(1, 7, 1, 1, 1, 2, 0, 0, 6, 8).with(0, 0, 2, 4, 0),   // Coco  - buns, hoodie, skirt
        make(3, 6, 0, 5, 1, 1, 5, 1, 3, 0).with(0, 1, 1, 1, 1),   // Popo  - spiky, stripes, cap, shorts
        make(5, 5, 0, 2, 5, 3, 2, 0, 0, 0).with(0, 0, 0, 2, 0),   // Mia   - afro, dress
        make(0, 1, 4, 0, 0, 4, 3, 0, 0, 0).with(1, 2, 0, 3, 1),   // Leo   - toddler, overalls
        make(0, 2, 8, 3, 0, 3, 7, 2, 1, 0).with(5, 0, 0, 5, 0), // Nana - elder
        make(2, 1, 1, 5, 2, 5, 9, 2, 0, 0).with(3, 1, 0, 5, 0),   // Dr Pip - adult, coat
        make(4, 10, 1, 5, 1, 0, 4, 1, 1, 0).with(3, 0, 2, 5, 0),  // Teacher - adult, top bun
        make(2, 2, 3, 4, 1, 4, 9, 6, 4, 9).with(4, 2, 0, 1, 1),   // Baker - round, beanie
        make(7, 9, 7, 5, 3, 2, 6, 1, 7, 5).with(2, 1, 0, 2, 0),   // Zed   - teen, mohawk, headphones
        make(1, 11, 5, 2, 4, 3, 8, 7, 10, 2).with(0, 0, 0, 4, 1)  // Luna  - braids, flower crown
    };
}
