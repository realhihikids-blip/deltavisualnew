package dev.deltavisuals.gui;

import dev.deltavisuals.bind.BindManager;
import dev.deltavisuals.config.ConfigManager;
import dev.deltavisuals.hud.HudElement;
import dev.deltavisuals.hud.HudRenderer;
import dev.deltavisuals.marker.Marker;
import dev.deltavisuals.marker.MarkerManager;
import dev.deltavisuals.module.Category;
import dev.deltavisuals.module.Module;
import dev.deltavisuals.module.Modules;
import dev.deltavisuals.module.Setting;
import dev.deltavisuals.util.ColorUtil;
import dev.deltavisuals.util.KeyNames;
import dev.deltavisuals.util.KeyState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * DELTA VISUALS ClickGUI. Immediate-mode: every frame the widgets are laid out, drawn and
 * clicked in one pass. Input is polled from GLFW, so it does not depend on Screen input signatures.
 */
public final class ClickGuiScreen extends Screen {

    private enum Tab {
        VISUALS("Visuals", Category.VISUALS),
        RENDER("Render", Category.RENDER),
        HUD("HUD", Category.HUD),
        PLAYER("Player", Category.PLAYER),
        MARKERS("Markers", null),
        CONFIGS("Configs", null),
        SETTINGS("Settings", null);

        final String label;
        final Category category;

        Tab(String label, Category category) {
            this.label = label;
            this.category = category;
        }
    }

    // ---- colors ----
    private static final int TEXT = 0xFFEDE7FA;
    private static final int DIM = 0xFF9F94BD;
    private static final int CARD = 0xFF1B1530;
    private static final int CARD_HOVER = 0xFF261D45;
    private static final int FIELD = 0xFF241D3A;
    private static final int[] PRESETS = {
            0xFFFFFF, 0xFF5555, 0xFFAA33, 0xFFFF55, 0x55FF55, 0x55FFFF, 0x5599FF, 0x9B59FF, 0xFF66CC
    };

    // ---- state ----
    private final KeyState ks = new KeyState();
    private final long openedAt = System.currentTimeMillis();
    private long closeStartedAt = -1L;
    private boolean finishRequested;
    private float anim;

    private Tab tab = Tab.VISUALS;
    private float scroll;
    private int maxScroll;
    private final TextField search = new TextField(24, false);
    private String lastSearch = "";

    private Module listening;
    private boolean listeningMenu;

    private Setting dragSetting;
    private boolean dragHue;
    private int dragX, dragW;
    private double pendingScale;
    private Setting.Mode openMode;

    private boolean hudEdit;
    private HudElement dragEl;
    private int dragOffX, dragOffY;

    // markers tab
    private final TextField mkName = new TextField(24, false);
    private final TextField mkX = new TextField(10, true);
    private final TextField mkY = new TextField(10, true);
    private final TextField mkZ = new TextField(10, true);
    private int mkColor = 4;
    private Marker editing;
    private boolean mkInit;

    // configs tab
    private final TextField cfName = new TextField(24, false);
    private String selectedCfg;
    private String confirm = "";

    private String toast = "";
    private long toastUntil;

    // per-frame values
    private DrawContext ctx;
    private TextRenderer tr;
    private int mx, my;
    private boolean lc, rc, md;
    private float s = 1f;
    private int rh = 18;
    private int viewTop, viewBottom;
    private boolean clip;

    public ClickGuiScreen() {
        super(Text.literal("DELTA VISUALS"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false; // ESC is handled by us so the closing animation can play
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (hudEdit) return false;
        scroll -= (float) (verticalAmount * 18.0);
        clampScroll();
        return true;
    }

    private void clampScroll() {
        if (scroll > maxScroll) scroll = maxScroll;
        if (scroll < 0) scroll = 0;
    }

    // ===================================================================
    // frame
    // ===================================================================
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.ctx = context;
        this.tr = MinecraftClient.getInstance().textRenderer;
        this.mx = mouseX;
        this.my = mouseY;

        ks.update();
        lc = ks.mousePressed(0);
        rc = ks.mousePressed(1);
        md = ks.mouseDown(0);

        if (!md) {
            if (dragSetting == Modules.UI_SCALE) {
                Modules.UI_SCALE.set(pendingScale);
            }
            dragSetting = null;
            dragEl = null;
        }
        if (lc && (listening != null || listeningMenu)) {
            listening = null;
            listeningMenu = false;
        }

        updateAnim();
        s = Modules.UI_SCALE.getInt() / 100f;
        rh = Math.max(16, (int) (18 * s));

        handleKeys();

        if (hudEdit) {
            renderHudEditor();
        } else {
            renderPanel();
        }

        if (finishRequested) {
            finishRequested = false;
            if (Modules.UI_AUTOSAVE.get()) ConfigManager.save();
            MinecraftClient.getInstance().setScreen(null);
        }
    }

    private void updateAnim() {
        long now = System.currentTimeMillis();
        boolean closing = closeStartedAt >= 0;
        if (!Modules.UI_ANIM.get()) {
            anim = closing ? 0f : 1f;
            if (closing) finishRequested = true;
            return;
        }
        if (!closing) {
            float t = Math.min(1f, (now - openedAt) / 200f);
            anim = 1f - (1f - t) * (1f - t) * (1f - t);
        } else {
            float t = Math.min(1f, (now - closeStartedAt) / 150f);
            anim = 1f - t;
            if (t >= 1f) finishRequested = true;
        }
    }

    private void startClose() {
        if (closeStartedAt < 0) closeStartedAt = System.currentTimeMillis();
    }

    private void toast(String msg) {
        toast = msg;
        toastUntil = System.currentTimeMillis() + 3500;
    }

    // ===================================================================
    // keyboard
    // ===================================================================
    private boolean anyFocused() {
        return search.focused || mkName.focused || mkX.focused || mkY.focused || mkZ.focused || cfName.focused;
    }

    private void unfocusAll() {
        search.focused = false;
        mkName.focused = false;
        mkX.focused = false;
        mkY.focused = false;
        mkZ.focused = false;
        cfName.focused = false;
    }

    private void handleKeys() {
        if (listening != null || listeningMenu) {
            int k = ks.firstPressed();
            if (k >= 0) {
                if (k == GLFW.GLFW_KEY_ESCAPE) {
                    listening = null;
                    listeningMenu = false;
                } else if (listeningMenu) {
                    BindManager.assignMenuKey(k);
                    listeningMenu = false;
                    toast("Menu key set to " + KeyNames.name(k));
                } else if (k == GLFW.GLFW_KEY_BACKSPACE || k == GLFW.GLFW_KEY_DELETE) {
                    BindManager.assign(listening, -1);
                    toast(listening.name + ": bind removed");
                    listening = null;
                } else {
                    if (BindManager.assign(listening, k)) {
                        toast(listening.name + " bound to " + KeyNames.name(k));
                    } else {
                        toast("That key opens the menu - choose another");
                    }
                    listening = null;
                }
            }
            return;
        }

        search.update(ks);
        mkName.update(ks);
        mkX.update(ks);
        mkY.update(ks);
        mkZ.update(ks);
        cfName.update(ks);

        boolean focus = anyFocused();
        if (ks.pressed(GLFW.GLFW_KEY_ESCAPE)) {
            if (focus) unfocusAll();
            else if (hudEdit) hudEdit = false;
            else startClose();
            return;
        }
        int menuKey = Modules.UI_MENU_KEY.getInt();
        if (!focus && menuKey >= 0 && ks.pressed(menuKey)) startClose();
    }

    // ===================================================================
    // drawing helpers
    // ===================================================================
    private int c(int argb) {
        return ColorUtil.mulAlpha(argb, anim);
    }

    private int accent() {
        return 0xFF000000 | Modules.UI_ACCENT.get();
    }

    private void rbox(int x, int y, int w, int h, int color) {
        if (w <= 2 || h <= 2) {
            ctx.fill(x, y, x + w, y + h, color);
            return;
        }
        ctx.fill(x + 1, y, x + w - 1, y + h, color);
        ctx.fill(x, y + 1, x + w, y + h - 1, color);
    }

    private void outline(int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    private void txt(String text, int x, int y, int color) {
        ctx.drawText(tr, text, x, y, color, true);
    }

    private boolean hit(int x, int y, int w, int h) {
        if (mx < x || mx >= x + w || my < y || my >= y + h) return false;
        return !clip || (my >= viewTop && my < viewBottom);
    }

    private boolean button(String label, int x, int y, int w, int h, boolean primary) {
        boolean hov = hit(x, y, w, h);
        int bg;
        if (primary) bg = ColorUtil.lerp(CARD, accent(), hov ? 0.6f : 0.4f);
        else bg = hov ? CARD_HOVER : FIELD;
        rbox(x, y, w, h, c(bg));
        String t = tr.trimToWidth(label, Math.max(4, w - 6));
        txt(t, x + (w - tr.getWidth(t)) / 2, y + (h - 8) / 2, c(TEXT));
        return hov && lc;
    }

    private void textField(TextField f, int x, int y, int w, int h, String placeholder) {
        boolean hov = hit(x, y, w, h);
        if (lc) f.focused = hov;
        rbox(x, y, w, h, c(f.focused ? 0xFF2D2450 : FIELD));
        if (f.focused) outline(x, y, w, h, c(ColorUtil.argb(accent(), 200)));
        if (f.text.isEmpty() && !f.focused) {
            txt(placeholder, x + 5, y + (h - 8) / 2, c(0xFF6F6590));
        } else {
            String t = f.text + (f.focused && (System.currentTimeMillis() / 500L) % 2L == 0L ? "_" : "");
            while (t.length() > 1 && tr.getWidth(t) > w - 10) t = t.substring(1);
            txt(t, x + 5, y + (h - 8) / 2, c(TEXT));
        }
    }

    // ===================================================================
    // main panel
    // ===================================================================
    private void renderPanel() {
        int pw = Math.min(this.width - 16, (int) (580 * s));
        int ph = Math.min(this.height - 16, (int) (350 * s));
        int px = (this.width - pw) / 2;
        int py = (this.height - ph) / 2 + (int) ((1f - anim) * 14f);
        int sbW = (int) (106 * s);
        int bgA = Math.round(Modules.UI_OPACITY.getInt() / 100f * 255f);

        rbox(px, py, pw, ph, c(ColorUtil.argb(0x0F0B1A, bgA)));
        outline(px, py, pw, ph, c(ColorUtil.argb(accent(), 150)));
        rbox(px + 1, py + 1, sbW, ph - 2, c(ColorUtil.argb(0x150F26, bgA)));

        // logo
        txt("DELTA", px + 12, py + 9, c(accent()));
        txt("VISUALS", px + 12, py + 19, c(TEXT));

        // tabs
        int ty = py + 40;
        boolean searching = !search.text.isEmpty();
        for (Tab t : Tab.values()) {
            int th = (int) (22 * s);
            boolean sel = t == tab && !searching;
            boolean hov = hit(px + 4, ty, sbW - 8, th);
            if (sel) rbox(px + 4, ty, sbW - 8, th, c(ColorUtil.argb(accent(), 70)));
            else if (hov) rbox(px + 4, ty, sbW - 8, th, c(0x30FFFFFF));
            if (sel) ctx.fill(px + 4, ty + 3, px + 6, ty + th - 3, c(accent()));
            txt(t.label, px + 14, ty + (th - 8) / 2, c(sel ? TEXT : DIM));
            if (hov && lc) {
                tab = t;
                scroll = 0;
                openMode = null;
                search.text = "";
                unfocusAll();
            }
            ty += th + 2;
        }

        // header: search + close
        int cx = px + sbW + 10;
        int cw = pw - sbW - 20;
        int hy = py + 8;
        int hh = Math.max(16, (int) (18 * s));
        textField(search, cx, hy, cw - hh - 6, hh, "Search modules...");
        if (!search.text.equals(lastSearch)) {
            lastSearch = search.text;
            scroll = 0;
        }
        boolean closeHov = hit(cx + cw - hh, hy, hh, hh);
        rbox(cx + cw - hh, hy, hh, hh, c(closeHov ? 0xFF8A2A3A : FIELD));
        txt("x", cx + cw - hh + (hh - 5) / 2, hy + (hh - 8) / 2, c(TEXT));
        if (closeHov && lc) startClose();

        viewTop = hy + hh + 8;
        viewBottom = py + ph - 18;

        // content
        ctx.enableScissor(cx - 2, viewTop, cx + cw + 2, viewBottom);
        clip = true;
        int startY = viewTop - (int) scroll;
        int endY = drawContent(cx, startY, cw);
        clip = false;
        ctx.disableScissor();

        int contentHeight = endY - startY;
        maxScroll = Math.max(0, contentHeight - (viewBottom - viewTop));
        clampScroll();

        if (maxScroll > 0) {
            int trackH = viewBottom - viewTop;
            int thumbH = Math.max(14, trackH * trackH / (trackH + maxScroll));
            int thumbY = viewTop + (int) ((trackH - thumbH) * (scroll / maxScroll));
            ctx.fill(cx + cw + 3, thumbY, cx + cw + 5, thumbY + thumbH, c(ColorUtil.argb(accent(), 160)));
        }

        // footer
        int fy = py + ph - 13;
        String menuName = KeyNames.name(Modules.UI_MENU_KEY.getInt());
        if (System.currentTimeMillis() < toastUntil) {
            txt(tr.trimToWidth(toast, cw), cx, fy, c(accent()));
        } else {
            txt(tr.trimToWidth("LMB: toggle   RMB: settings & bind   " + menuName + " / ESC: close", cw), cx, fy, c(DIM));
        }
    }

    private int drawContent(int x, int y, int w) {
        if (tab != Tab.MARKERS || !search.text.isEmpty()) {
            mkName.focused = false;
            mkX.focused = false;
            mkY.focused = false;
            mkZ.focused = false;
        }
        if (tab != Tab.CONFIGS || !search.text.isEmpty()) cfName.focused = false;

        if (!search.text.isEmpty()) {
            String q = search.text.toLowerCase(Locale.ROOT);
            boolean any = false;
            for (Module m : Modules.ALL) {
                if (m.name.toLowerCase(Locale.ROOT).contains(q) || m.description.toLowerCase(Locale.ROOT).contains(q)) {
                    y += drawModule(m, x, y, w) + 4;
                    any = true;
                }
            }
            if (!any) txt("No modules found.", x + 4, y + 4, c(DIM));
            return y;
        }
        switch (tab) {
            case MARKERS:
                return drawMarkers(x, y, w);
            case CONFIGS:
                return drawConfigs(x, y, w);
            case SETTINGS:
                return drawSettingsTab(x, y, w);
            default:
                for (Module m : Modules.byCategory(tab.category)) {
                    y += drawModule(m, x, y, w) + 4;
                }
                return y;
        }
    }

    // ===================================================================
    // modules & settings
    // ===================================================================
    private int drawModule(Module m, int x, int y, int w) {
        int rowH = Math.max(26, (int) (28 * s));
        boolean hov = hit(x, y, w, rowH);
        int bg = m.isEnabled() && !m.isAlwaysOn()
                ? ColorUtil.lerp(CARD, accent(), 0.22f)
                : (hov ? CARD_HOVER : CARD);
        rbox(x, y, w, rowH, c(bg));
        if (m.isEnabled() && !m.isAlwaysOn()) ctx.fill(x, y + 3, x + 2, y + rowH - 3, c(accent()));

        txt(m.name, x + 9, y + 4, c(TEXT));
        txt(tr.trimToWidth(m.description, Math.max(20, w - 90)), x + 9, y + 14, c(DIM));

        if (!m.isAlwaysOn()) {
            int sx = x + w - 34;
            int sy = y + (rowH - 12) / 2;
            rbox(sx, sy, 26, 12, c(m.isEnabled() ? accent() : 0xFF3A3350));
            int kx = m.isEnabled() ? sx + 14 : sx + 2;
            rbox(kx, sy + 2, 10, 8, c(0xFFFFFFFF));
        }
        if (m.getBind() >= 0) {
            String b = "[" + KeyNames.name(m.getBind()) + "]";
            txt(b, x + w - 42 - tr.getWidth(b), y + (rowH - 8) / 2, c(DIM));
        }

        if (hov) {
            if (lc) {
                if (m.isAlwaysOn()) m.expanded = !m.expanded;
                else m.toggle();
            } else if (rc) {
                m.expanded = !m.expanded;
            }
        }

        int total = rowH;
        if (m.expanded) {
            int inner = measureModule(m);
            rbox(x, y + rowH + 1, w, inner + 8, c(0xA0110D1E));
            int ix = x + 6;
            int iw = w - 12;
            int yy = y + rowH + 5;
            for (Setting st : m.settings()) {
                if (st.hidden) continue;
                yy += drawSetting(st, ix, yy, iw);
            }
            if (!m.isAlwaysOn()) drawBind(m, ix, yy, iw);
            total = rowH + 1 + inner + 8;
        }
        return total;
    }

    private int measureModule(Module m) {
        int h = 0;
        for (Setting st : m.settings()) {
            if (!st.hidden) h += hSetting(st);
        }
        if (!m.isAlwaysOn()) h += rh + 2;
        return h;
    }

    private int hSetting(Setting st) {
        if (st instanceof Setting.Num) return rh + 8 + 2;
        if (st instanceof Setting.Mode) {
            Setting.Mode mo = (Setting.Mode) st;
            return rh + 2 + (openMode == mo ? mo.options.length * rh : 0);
        }
        if (st instanceof Setting.Color) return rh * 2 + 12 + 2;
        return rh + 2;
    }

    private String fmt(Setting.Num n, double v) {
        if (n.step >= 1) return String.valueOf((int) Math.round(v));
        return String.format(Locale.ROOT, "%.2f", v);
    }

    private int drawSetting(Setting st, int x, int y, int w) {
        if (st instanceof Setting.Bool) {
            Setting.Bool b = (Setting.Bool) st;
            boolean hov = hit(x, y, w, rh);
            if (hov) rbox(x, y, w, rh, c(0x25FFFFFF));
            txt(st.name, x + 4, y + (rh - 8) / 2, c(TEXT));
            int sx = x + w - 28;
            int sy = y + (rh - 10) / 2;
            rbox(sx, sy, 22, 10, c(b.get() ? accent() : 0xFF3A3350));
            rbox(b.get() ? sx + 12 : sx + 2, sy + 2, 8, 6, c(0xFFFFFFFF));
            if (hov && lc) b.toggle();

        } else if (st instanceof Setting.Num) {
            Setting.Num n = (Setting.Num) st;
            boolean hov = hit(x, y, w, rh + 8);
            double shown = (n == Modules.UI_SCALE && dragSetting == n) ? pendingScale : n.get();
            txt(st.name, x + 4, y + 3, c(TEXT));
            String v = fmt(n, shown);
            txt(v, x + w - 4 - tr.getWidth(v), y + 3, c(accent()));
            int tx = x + 4;
            int tw = w - 8;
            int ty = y + rh - 2;
            double f = (shown - n.min) / (n.max - n.min);
            f = Math.max(0, Math.min(1, f));
            rbox(tx, ty, tw, 4, c(0xFF2B2440));
            rbox(tx, ty, (int) (tw * f), 4, c(accent()));
            rbox(tx + (int) (tw * f) - 2, ty - 2, 5, 8, c(0xFFFFFFFF));
            if (hov && lc) {
                dragSetting = n;
                dragHue = false;
                pendingScale = n.get();
            }
            if (dragSetting == n && md && !dragHue) {
                dragX = tx;
                dragW = tw;
                double t = (mx - dragX) / (double) Math.max(1, dragW);
                t = Math.max(0, Math.min(1, t));
                double val = n.min + t * (n.max - n.min);
                if (n == Modules.UI_SCALE) {
                    pendingScale = Math.round(val / n.step) * n.step;
                } else {
                    n.set(val);
                }
            }

        } else if (st instanceof Setting.Mode) {
            Setting.Mode mo = (Setting.Mode) st;
            boolean hov = hit(x, y, w, rh);
            if (hov) rbox(x, y, w, rh, c(0x25FFFFFF));
            txt(st.name, x + 4, y + (rh - 8) / 2, c(TEXT));
            String v = mo.get() + (openMode == mo ? " ^" : " v");
            txt(v, x + w - 4 - tr.getWidth(v), y + (rh - 8) / 2, c(accent()));
            if (hov && lc) openMode = (openMode == mo) ? null : mo;
            int total = rh + 2;
            if (openMode == mo) {
                for (int i = 0; i < mo.options.length; i++) {
                    int oy = y + total;
                    boolean oh = hit(x + 8, oy, w - 8, rh);
                    int bg = i == mo.index() ? ColorUtil.lerp(CARD, accent(), 0.35f) : (oh ? CARD_HOVER : CARD);
                    rbox(x + 8, oy, w - 8, rh - 1, c(bg));
                    txt(mo.options[i], x + 14, oy + (rh - 8) / 2, c(TEXT));
                    if (oh && lc) {
                        mo.set(i);
                        openMode = null;
                    }
                    total += rh;
                }
            }
            return total;

        } else if (st instanceof Setting.Color) {
            Setting.Color col = (Setting.Color) st;
            txt(st.name, x + 4, y + (rh - 8) / 2, c(TEXT));
            rbox(x + w - 26, y + 2, 22, rh - 6, c(0xFF000000 | col.get()));
            int y1 = y + rh;
            int sz = Math.max(10, (int) (12 * s));
            for (int i = 0; i < PRESETS.length; i++) {
                int sx = x + 4 + i * (sz + 4);
                boolean hov = hit(sx, y1 + 1, sz, sz);
                rbox(sx, y1 + 1, sz, sz, c(0xFF000000 | PRESETS[i]));
                if (col.get() == PRESETS[i]) outline(sx - 1, y1, sz + 2, sz + 2, c(0xFFFFFFFF));
                else if (hov) outline(sx - 1, y1, sz + 2, sz + 2, c(0x80FFFFFF));
                if (hov && lc) col.set(PRESETS[i]);
            }
            int y2 = y1 + rh;
            int tx = x + 4;
            int tw = w - 8;
            int steps = 36;
            for (int i = 0; i < steps; i++) {
                int x1 = tx + tw * i / steps;
                int x2 = tx + tw * (i + 1) / steps;
                ctx.fill(x1, y2, x2, y2 + 8, c(0xFF000000 | ColorUtil.hsvToRgb(360f * i / steps, 1f, 1f)));
            }
            float hue = ColorUtil.rgbToHue(col.get());
            int kx = tx + (int) (tw * hue / 360f);
            rbox(kx - 2, y2 - 2, 5, 12, c(0xFFFFFFFF));
            if (hit(tx, y2 - 2, tw, 12) && lc) {
                dragSetting = col;
                dragHue = true;
            }
            if (dragSetting == col && md && dragHue) {
                double t = (mx - tx) / (double) Math.max(1, tw);
                t = Math.max(0, Math.min(1, t));
                col.set(ColorUtil.hsvToRgb((float) (t * 359.0), 1f, 1f));
            }
        }
        return hSetting(st);
    }

    private int drawBind(Module m, int x, int y, int w) {
        boolean hov = hit(x, y, w, rh);
        if (hov) rbox(x, y, w, rh, c(0x25FFFFFF));
        txt("Bind", x + 4, y + (rh - 8) / 2, c(TEXT));
        String v;
        if (listening == m) v = "Press a key... (ESC cancel)";
        else if (m.getBind() >= 0) v = "[" + KeyNames.name(m.getBind()) + "]";
        else v = "NONE";
        int bw = tr.getWidth(v) + 12;
        rbox(x + w - bw - 2, y, bw, rh - 2, c(listening == m ? ColorUtil.lerp(CARD, accent(), 0.5f) : FIELD));
        txt(v, x + w - bw + 4, y + (rh - 8) / 2 - 1, c(TEXT));
        if (hov && lc) {
            listening = m;
            listeningMenu = false;
        } else if (hov && rc) {
            BindManager.assign(m, -1);
        }
        return rh + 2;
    }

    // ===================================================================
    // settings tab
    // ===================================================================
    private int drawSettingsTab(int x, int y, int w) {
        txt("Interface", x + 2, y, c(accent()));
        y += 14;
        for (Setting st : Modules.INTERFACE.settings()) {
            if (st.hidden) continue;
            y += drawSetting(st, x, y, w);
        }

        // menu key
        boolean hov = hit(x, y, w, rh);
        if (hov) rbox(x, y, w, rh, c(0x25FFFFFF));
        txt("Menu Key", x + 4, y + (rh - 8) / 2, c(TEXT));
        String v = listeningMenu ? "Press a key... (ESC cancel)" : "[" + KeyNames.name(Modules.UI_MENU_KEY.getInt()) + "]";
        int bw = tr.getWidth(v) + 12;
        rbox(x + w - bw - 2, y, bw, rh - 2, c(listeningMenu ? ColorUtil.lerp(CARD, accent(), 0.5f) : FIELD));
        txt(v, x + w - bw + 4, y + (rh - 8) / 2 - 1, c(TEXT));
        if (hov && lc) {
            listeningMenu = true;
            listening = null;
        }
        y += rh + 10;

        txt("HUD", x + 2, y, c(accent()));
        y += 14;
        if (button("Open HUD Editor", x, y, 150, rh + 4, true)) {
            hudEdit = true;
            unfocusAll();
        }
        y += rh + 8;
        txt(tr.trimToWidth("Drag FPS, coordinates, armor and held item where you want them.", w), x + 2, y, c(DIM));
        y += 14;
        return y;
    }

    // ===================================================================
    // markers tab
    // ===================================================================
    private void fillMyPosition() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        mkX.text = String.valueOf(Math.round(mc.player.getX()));
        mkY.text = String.valueOf(Math.round(mc.player.getY()));
        mkZ.text = String.valueOf(Math.round(mc.player.getZ()));
    }

    private double parse(String text) throws NumberFormatException {
        return Double.parseDouble(text.trim());
    }

    private int drawMarkers(int x, int y, int w) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!mkInit) {
            mkInit = true;
            mkName.text = "Marker " + (MarkerManager.all().size() + 1);
            fillMyPosition();
        }

        txt(editing == null ? "New marker" : "Edit marker: " + editing.name, x + 2, y, c(accent()));
        y += 14;
        int fh = rh + 2;
        int lw = 44;

        txt("Name", x + 2, y + (fh - 8) / 2, c(DIM));
        textField(mkName, x + lw, y, w - lw, fh, "Marker name");
        y += fh + 4;

        int fw = (w - lw - 12) / 3;
        txt("XYZ", x + 2, y + (fh - 8) / 2, c(DIM));
        textField(mkX, x + lw, y, fw, fh, "X");
        textField(mkY, x + lw + fw + 6, y, fw, fh, "Y");
        textField(mkZ, x + lw + (fw + 6) * 2, y, fw, fh, "Z");
        y += fh + 4;

        txt("Color", x + 2, y + (fh - 8) / 2, c(DIM));
        int sz = fh - 4;
        for (int i = 0; i < MarkerManager.PALETTE.length; i++) {
            int sx = x + lw + i * (sz + 4);
            boolean hov = hit(sx, y + 2, sz, sz);
            rbox(sx, y + 2, sz, sz, c(0xFF000000 | MarkerManager.PALETTE[i]));
            if (i == mkColor) outline(sx - 1, y + 1, sz + 2, sz + 2, c(0xFFFFFFFF));
            else if (hov) outline(sx - 1, y + 1, sz + 2, sz + 2, c(0x80FFFFFF));
            if (hov && lc) mkColor = i;
        }
        y += fh + 4;

        if (button("My position", x, y, 100, fh, false)) fillMyPosition();
        if (editing == null) {
            if (button("Add marker", x + 106, y, 100, fh, true)) addMarker(mc);
        } else {
            if (button("Save changes", x + 106, y, 110, fh, true)) saveEdit();
            if (button("Cancel", x + 222, y, 70, fh, false)) {
                editing = null;
                mkName.text = "Marker " + (MarkerManager.all().size() + 1);
                fillMyPosition();
            }
        }
        y += fh + 8;

        // players
        if (mc.world != null && mc.player != null) {
            List<String> names = new ArrayList<>();
            for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
                if (p == mc.player) continue;
                names.add(p.getName().getString());
                if (names.size() >= 6) break;
            }
            if (!names.isEmpty()) {
                txt("Mark a nearby player (the marker follows them):", x + 2, y, c(DIM));
                y += 12;
                int bx = x;
                for (String n : names) {
                    int bw = tr.getWidth(n) + 14;
                    if (bx + bw > x + w) {
                        bx = x;
                        y += fh + 3;
                    }
                    if (button(n, bx, y, bw, fh, false)) {
                        Marker m = new Marker(n, mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                                MarkerManager.PALETTE[mkColor], MarkerManager.currentDimension(mc));
                        m.trackedPlayer = n;
                        MarkerManager.add(m);
                        toast("Marker created for " + n);
                    }
                    bx += bw + 4;
                }
                y += fh + 8;
            }
        }

        // list
        txt("Your markers (" + MarkerManager.all().size() + ")", x + 2, y, c(accent()));
        y += 14;
        if (MarkerManager.all().isEmpty()) {
            txt("No markers yet. Fill in the fields above and press Add marker.", x + 2, y, c(DIM));
            y += 14;
        }
        for (Marker m : new ArrayList<>(MarkerManager.all())) {
            int rowH = Math.max(28, (int) (30 * s));
            boolean hov = hit(x, y, w, rowH);
            rbox(x, y, w, rowH, c(hov ? CARD_HOVER : CARD));
            rbox(x + 6, y + 6, 10, 10, c(0xFF000000 | m.color));
            txt(tr.trimToWidth(m.name + (m.trackedPlayer != null ? " (following)" : ""), Math.max(20, w - 130)),
                    x + 22, y + 4, c(TEXT));

            String info = Math.round(m.x) + " " + Math.round(m.y) + " " + Math.round(m.z);
            if (mc.player != null) {
                if (MarkerManager.isVisibleHere(m, mc)) {
                    double dx = m.x - mc.player.getX();
                    double dy = m.y - mc.player.getY();
                    double dz = m.z - mc.player.getZ();
                    long dist = Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz));
                    info += "  |  " + dist + "m  "
                            + MarkerManager.compass(mc.player.getX(), mc.player.getZ(), m.x, m.z)
                            + " (" + MarkerManager.relative(mc.player.getX(), mc.player.getZ(), mc.player.getYaw(), m.x, m.z) + ")";
                } else {
                    info += "  |  other dimension";
                }
            }
            txt(tr.trimToWidth(info, Math.max(20, w - 130)), x + 22, y + 15, c(DIM));

            int by = y + (rowH - 16) / 2;
            if (button("Edit", x + w - 100, by, 46, 16, false)) {
                editing = m;
                mkName.text = m.name;
                mkX.text = String.valueOf(Math.round(m.x));
                mkY.text = String.valueOf(Math.round(m.y));
                mkZ.text = String.valueOf(Math.round(m.z));
                for (int i = 0; i < MarkerManager.PALETTE.length; i++) {
                    if (MarkerManager.PALETTE[i] == m.color) mkColor = i;
                }
            }
            if (button("Delete", x + w - 50, by, 46, 16, false)) {
                MarkerManager.remove(m);
                if (editing == m) editing = null;
            }
            y += rowH + 4;
        }
        return y;
    }

    private void addMarker(MinecraftClient mc) {
        try {
            double px = parse(mkX.text);
            double py = parse(mkY.text);
            double pz = parse(mkZ.text);
            String name = mkName.text.trim().isEmpty() ? "Marker" : mkName.text.trim();
            MarkerManager.add(new Marker(name, px, py, pz, MarkerManager.PALETTE[mkColor],
                    MarkerManager.currentDimension(mc)));
            mkName.text = "Marker " + (MarkerManager.all().size() + 1);
            toast("Marker '" + name + "' added");
        } catch (NumberFormatException e) {
            toast("Coordinates must be numbers");
        }
    }

    private void saveEdit() {
        if (editing == null) return;
        try {
            double px = parse(mkX.text);
            double py = parse(mkY.text);
            double pz = parse(mkZ.text);
            editing.name = mkName.text.trim().isEmpty() ? "Marker" : mkName.text.trim();
            editing.x = px;
            editing.y = py;
            editing.z = pz;
            editing.color = MarkerManager.PALETTE[mkColor];
            ConfigManager.markDirty();
            toast("Marker saved");
            editing = null;
            mkName.text = "Marker " + (MarkerManager.all().size() + 1);
            fillMyPosition();
        } catch (NumberFormatException e) {
            toast("Coordinates must be numbers");
        }
    }

    // ===================================================================
    // configs tab
    // ===================================================================
    private int drawConfigs(int x, int y, int w) {
        List<String> names = ConfigManager.list();
        if (selectedCfg == null || !names.contains(selectedCfg)) selectedCfg = ConfigManager.current();

        txt("Current config: " + ConfigManager.current(), x + 2, y, c(accent()));
        y += 14;
        int fh = rh + 2;

        txt("Name", x + 2, y + (fh - 8) / 2, c(DIM));
        textField(cfName, x + 40, y, Math.max(60, w - 40 - 190), fh, "new_config");
        int bx = x + w - 184;
        if (button("Create", bx, y, 90, fh, true)) {
            if (ConfigManager.create(cfName.text.trim())) {
                selectedCfg = cfName.text.trim();
                cfName.text = "";
            }
            toast(ConfigManager.status);
        }
        if (button("Rename", bx + 94, y, 90, fh, false)) {
            if (ConfigManager.rename(selectedCfg, cfName.text.trim())) {
                selectedCfg = cfName.text.trim();
                cfName.text = "";
            }
            toast(ConfigManager.status);
        }
        y += fh + 4;

        int bw = Math.max(70, (w - 12) / 4);
        if (button("Save", x, y, bw, fh, false)) {
            ConfigManager.save();
            toast("Saved '" + ConfigManager.current() + "'");
        }
        if (button("Load selected", x + bw + 4, y, bw, fh, false)) {
            ConfigManager.load(selectedCfg);
            toast(ConfigManager.status);
        }
        String delLabel = confirm.equals("del") ? "Click to confirm" : "Delete selected";
        if (button(delLabel, x + (bw + 4) * 2, y, bw, fh, confirm.equals("del"))) {
            if (confirm.equals("del")) {
                ConfigManager.delete(selectedCfg);
                selectedCfg = ConfigManager.current();
                confirm = "";
                toast(ConfigManager.status);
            } else {
                confirm = "del";
            }
        }
        String defLabel = confirm.equals("def") ? "Click to confirm" : "Default config";
        if (button(defLabel, x + (bw + 4) * 3, y, bw, fh, confirm.equals("def"))) {
            if (confirm.equals("def")) {
                ConfigManager.resetToDefaults();
                confirm = "";
                toast(ConfigManager.status);
            } else {
                confirm = "def";
            }
        }
        if (lc && !hit(x + (bw + 4) * 2, y, bw * 2 + 4, fh)) confirm = "";
        y += fh + 8;

        y += drawSetting(Modules.UI_AUTOSAVE, x, y, w);
        y += 4;

        if (!ConfigManager.status.isEmpty()) {
            txt(tr.trimToWidth(ConfigManager.status, w), x + 2, y, c(DIM));
            y += 14;
        }

        txt("Saved configs (click to select)", x + 2, y, c(accent()));
        y += 14;
        for (String n : names) {
            int rowH = rh + 4;
            boolean hov = hit(x, y, w, rowH);
            boolean sel = n.equals(selectedCfg);
            int bg = sel ? ColorUtil.lerp(CARD, accent(), 0.3f) : (hov ? CARD_HOVER : CARD);
            rbox(x, y, w, rowH, c(bg));
            txt(n, x + 8, y + (rowH - 8) / 2, c(TEXT));
            if (n.equals(ConfigManager.current())) {
                String tag = "ACTIVE";
                txt(tag, x + w - 8 - tr.getWidth(tag), y + (rowH - 8) / 2, c(accent()));
            }
            if (hov && lc) selectedCfg = n;
            y += rowH + 3;
        }
        return y;
    }

    // ===================================================================
    // HUD editor
    // ===================================================================
    private void renderHudEditor() {
        MinecraftClient mc = MinecraftClient.getInstance();
        int w = this.width;
        int h = this.height;
        ctx.fill(0, 0, w, h, c(0x66000000));

        String msg = "HUD Editor - drag the elements. Enable more in the HUD tab.";
        int bw = tr.getWidth(msg) + 100;
        int bx = (w - bw) / 2;
        rbox(bx, 6, bw, 22, c(0xE0150F26));
        outline(bx, 6, bw, 22, c(ColorUtil.argb(accent(), 150)));
        txt(msg, bx + 8, 13, c(TEXT));
        if (button("Done", bx + bw - 62, 9, 56, 16, true)) hudEdit = false;

        for (HudElement e : HudRenderer.ELEMENTS) {
            if (!e.module.isEnabled()) continue;
            int ew = e.width(mc);
            int eh = e.height(mc);
            int ex = Math.max(0, Math.min(Math.max(0, w - ew), e.x()));
            int ey = Math.max(0, Math.min(Math.max(0, h - eh), e.y()));
            boolean hov = mx >= ex && mx < ex + ew && my >= ey && my < ey + eh;
            outline(ex - 1, ey - 1, ew + 2, eh + 2, c(hov || dragEl == e ? 0xFFFFFFFF : ColorUtil.argb(accent(), 220)));
            txt(e.module.name, ex, ey - 10, c(DIM));
            if (lc && hov && dragEl == null) {
                dragEl = e;
                dragOffX = mx - ex;
                dragOffY = my - ey;
            }
        }
        if (dragEl != null && md) {
            int ew = dragEl.width(mc);
            int eh = dragEl.height(mc);
            int nx = Math.max(0, Math.min(Math.max(0, w - ew), mx - dragOffX));
            int ny = Math.max(0, Math.min(Math.max(0, h - eh), my - dragOffY));
            dragEl.setPos(nx, ny);
        }
    }
}
