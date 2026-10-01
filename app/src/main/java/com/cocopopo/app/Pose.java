package com.cocopopo.app;

/** Per-frame animation state fed to the avatar renderer. */
final class Pose {
    float t;        // seconds, drives idle sway
    float arm;      // 0 = arms down, 1 = arms raised (being dragged / cheering)
    float swing;    // leg swing -1..1 (dangling)
    float blink;    // 0 open .. 1 closed
    int mood = -1;  // temporary mouth override (-1 none)
    float walk;     // 0..1 walk cycle strength
    float walkPh;   // walk cycle phase (radians)
    boolean sit;    // seated: short bent legs
    boolean sleep;  // eyes closed
    float wave;     // 0..1 right arm waving
    float look;     // -1..1 looking left/right
    float dance;    // 0..1
    boolean chew;
    boolean hold;   // right arm held out holding something

    void reset() {
        t = 0; arm = 0; swing = 0; blink = 0; mood = -1; walk = 0; walkPh = 0; sit = false; sleep = false;
        wave = 0; look = 0; dance = 0; chew = false; hold = false;
    }
}
