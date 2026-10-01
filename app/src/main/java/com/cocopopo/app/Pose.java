package com.cocopopo.app;

/** Per-frame animation state fed to the avatar renderer. */
final class Pose {
    float t;        // seconds, drives idle sway
    float arm;      // 0 = arms down, 1 = arms raised (being dragged / cheering)
    float swing;    // leg swing -1..1
    float blink;    // 0 open .. 1 closed
    int mood = -1;  // temporary mouth override (-1 none)
}
