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
    boolean hold;   // holding something
    int holdType;   // 0 one hand, 1 both hands in front, 2 overhead
    float hug;      // 0..1 arms wrapping in a hug
    float strum;    // playing guitar
    int face;       // Avatar.F_* expression
    float gazeX, gazeY;
    float tilt;     // head tilt degrees
    boolean noLegs; // hidden (in the bath)

    void reset() {
        t = 0; arm = 0; swing = 0; blink = 0; mood = -1; walk = 0; walkPh = 0; sit = false; sleep = false;
        wave = 0; look = 0; dance = 0; chew = false; hold = false;
        holdType = 0; hug = 0; strum = 0; face = 0; gazeX = 0; gazeY = 0; tilt = 0; noLegs = false;
    }
}
