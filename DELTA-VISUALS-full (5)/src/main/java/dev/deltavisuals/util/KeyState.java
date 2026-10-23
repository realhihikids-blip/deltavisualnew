package dev.deltavisuals.util;

import org.lwjgl.glfw.GLFW;

/**
 * Polls GLFW directly, so the mod does not depend on version-specific Minecraft input classes.
 * Call update() once per frame/tick; then use pressed()/down().
 */
public final class KeyState {
    private final boolean[] cur = new boolean[GLFW.GLFW_KEY_LAST + 1];
    private final boolean[] prev = new boolean[GLFW.GLFW_KEY_LAST + 1];
    private final boolean[] mCur = new boolean[8];
    private final boolean[] mPrev = new boolean[8];

    public KeyState() {
        update();
        update();
    }

    public void update() {
        long w = GLFW.glfwGetCurrentContext();
        if (w == 0L) return;
        System.arraycopy(cur, 0, prev, 0, cur.length);
        System.arraycopy(mCur, 0, mPrev, 0, mCur.length);
        for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_LAST; k++) {
            cur[k] = GLFW.glfwGetKey(w, k) == GLFW.GLFW_PRESS;
        }
        for (int b = 0; b < 8; b++) {
            mCur[b] = GLFW.glfwGetMouseButton(w, b) == GLFW.GLFW_PRESS;
        }
    }

    public boolean down(int key) {
        return key >= 0 && key < cur.length && cur[key];
    }

    public boolean pressed(int key) {
        return key >= 0 && key < cur.length && cur[key] && !prev[key];
    }

    public boolean mouseDown(int button) {
        return button >= 0 && button < 8 && mCur[button];
    }

    public boolean mousePressed(int button) {
        return button >= 0 && button < 8 && mCur[button] && !mPrev[button];
    }

    /** Returns the first key that was newly pressed this frame, or -1. */
    public int firstPressed() {
        for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_LAST; k++) {
            if (cur[k] && !prev[k]) return k;
        }
        return -1;
    }
}
