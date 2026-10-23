package dev.deltavisuals.gui;

import dev.deltavisuals.util.KeyState;
import org.lwjgl.glfw.GLFW;

/** Tiny text input that reads keys by polling GLFW (letters, digits, space, - . _). */
public final class TextField {
    public String text = "";
    public boolean focused;
    public final int maxLen;
    public final boolean numeric;
    private int holdTicks;

    public TextField(int maxLen, boolean numeric) {
        this.maxLen = maxLen;
        this.numeric = numeric;
    }

    public void update(KeyState ks) {
        if (!focused) {
            holdTicks = 0;
            return;
        }
        boolean shift = ks.down(GLFW.GLFW_KEY_LEFT_SHIFT) || ks.down(GLFW.GLFW_KEY_RIGHT_SHIFT);

        for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_KP_SUBTRACT; k++) {
            if (ks.pressed(k)) type(k, shift);
        }

        if (ks.pressed(GLFW.GLFW_KEY_BACKSPACE)) {
            deleteLast();
            holdTicks = 0;
        } else if (ks.down(GLFW.GLFW_KEY_BACKSPACE)) {
            holdTicks++;
            if (holdTicks > 25 && holdTicks % 3 == 0) deleteLast();
        } else {
            holdTicks = 0;
        }
    }

    private void deleteLast() {
        if (!text.isEmpty()) text = text.substring(0, text.length() - 1);
    }

    private void type(int k, boolean shift) {
        char c = 0;
        if (k >= GLFW.GLFW_KEY_A && k <= GLFW.GLFW_KEY_Z) {
            c = (char) (shift ? k : k + 32);
        } else if (k >= GLFW.GLFW_KEY_0 && k <= GLFW.GLFW_KEY_9) {
            c = (char) k;
        } else if (k >= GLFW.GLFW_KEY_KP_0 && k <= GLFW.GLFW_KEY_KP_9) {
            c = (char) ('0' + (k - GLFW.GLFW_KEY_KP_0));
        } else if (k == GLFW.GLFW_KEY_SPACE) {
            c = ' ';
        } else if (k == GLFW.GLFW_KEY_MINUS || k == GLFW.GLFW_KEY_KP_SUBTRACT) {
            c = shift ? '_' : '-';
        } else if (k == GLFW.GLFW_KEY_PERIOD || k == GLFW.GLFW_KEY_KP_DECIMAL) {
            c = '.';
        }
        if (c == 0) return;
        if (numeric && !(Character.isDigit(c) || c == '-' || c == '.')) return;
        if (text.length() < maxLen) text += c;
    }
}
