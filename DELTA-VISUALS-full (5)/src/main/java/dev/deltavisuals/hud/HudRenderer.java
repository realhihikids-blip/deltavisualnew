package dev.deltavisuals.hud;

import dev.deltavisuals.feature.TrajectoryFeature;
import dev.deltavisuals.marker.Marker;
import dev.deltavisuals.marker.MarkerManager;
import dev.deltavisuals.module.Modules;
import dev.deltavisuals.module.Setting;
import dev.deltavisuals.util.ColorUtil;
import dev.deltavisuals.util.Projection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public final class HudRenderer {
    private HudRenderer() {}

    private static final int BG = 0x90000000;
    public static final List<HudElement> ELEMENTS = new ArrayList<>();

    // ------------------------------------------------------------------
    // Simple text element (FPS, coordinates)
    // ------------------------------------------------------------------
    private static final class TextElement extends HudElement {
        private final Supplier<List<String>> lines;
        private final Setting.Color color;
        private final Setting.Bool bg;

        TextElement(dev.deltavisuals.module.Module m, Setting.Num x, Setting.Num y,
                    Supplier<List<String>> lines, Setting.Color color, Setting.Bool bg) {
            super(m, x, y);
            this.lines = lines;
            this.color = color;
            this.bg = bg;
        }

        @Override public int width(MinecraftClient mc) {
            int w = 20;
            for (String s : lines.get()) w = Math.max(w, mc.textRenderer.getWidth(s));
            return w + 6;
        }

        @Override public int height(MinecraftClient mc) {
            return lines.get().size() * 10 + 3;
        }

        @Override public void draw(DrawContext ctx, MinecraftClient mc, int x, int y) {
            if (bg.get()) ctx.fill(x, y, x + width(mc), y + height(mc), BG);
            int c = ColorUtil.argb(color.get(), 255);
            int yy = y + 2;
            for (String s : lines.get()) {
                ctx.drawText(mc.textRenderer, s, x + 3, yy, c, true);
                yy += 10;
            }
        }
    }

    // ------------------------------------------------------------------
    // Armor durability element
    // ------------------------------------------------------------------
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private static final class ArmorElement extends HudElement {
        ArmorElement() {
            super(Modules.ARMOR_HUD, Modules.ARMOR_X, Modules.ARMOR_Y);
        }

        private boolean horizontal() { return Modules.ARMOR_LAYOUT.index() == 1; }

        private int pct(ItemStack s) {
            int max = s.getMaxDamage();
            if (max <= 0) return 100;
            return Math.max(0, Math.round(100f * (max - s.getDamage()) / max));
        }

        private String text(ItemStack s) {
            if (!Modules.ARMOR_NUMBERS.get() || !s.isDamageable()) return "";
            return pct(s) + "%";
        }

        @Override public int width(MinecraftClient mc) {
            int per = 16 + (Modules.ARMOR_NUMBERS.get() ? 4 + mc.textRenderer.getWidth("100%") : 0);
            return horizontal() ? per * 4 + 6 : per + 6;
        }

        @Override public int height(MinecraftClient mc) {
            return horizontal() ? 20 : 18 * 4 + 2;
        }

        @Override public void draw(DrawContext ctx, MinecraftClient mc, int x, int y) {
            if (Modules.ARMOR_BG.get()) ctx.fill(x, y, x + width(mc), y + height(mc), BG);
            ClientPlayerEntity p = mc.player;
            if (p == null) return;
            int per = 16 + (Modules.ARMOR_NUMBERS.get() ? 4 + mc.textRenderer.getWidth("100%") : 0);
            int cx = x + 3;
            int cy = y + 2;
            for (EquipmentSlot slot : ARMOR_SLOTS) {
                ItemStack s = p.getEquippedStack(slot);
                if (!s.isEmpty()) {
                    ctx.drawItem(s, cx, cy);
                    String t = text(s);
                    if (!t.isEmpty()) {
                        boolean warn = pct(s) < Modules.ARMOR_WARN.getInt();
                        int c = warn ? 0xFFFF5555 : ColorUtil.argb(Modules.ARMOR_COLOR.get(), 255);
                        ctx.drawText(mc.textRenderer, t, cx + 20, cy + 4, c, true);
                    }
                }
                if (horizontal()) cx += per;
                else cy += 18;
            }
        }
    }

    // ------------------------------------------------------------------
    // Held item element
    // ------------------------------------------------------------------
    private static final class HeldElement extends HudElement {
        HeldElement() {
            super(Modules.HELD_HUD, Modules.HELD_X, Modules.HELD_Y);
        }

        private String name(ItemStack s) {
            if (s.isEmpty() || !Modules.HELD_NAME.get()) return "";
            String n = s.getName().getString();
            if (s.getCount() > 1) n += " x" + s.getCount();
            return n;
        }

        private String dura(ItemStack s) {
            if (s.isEmpty() || !Modules.HELD_DURABILITY.get() || !s.isDamageable()) return "";
            return (s.getMaxDamage() - s.getDamage()) + "/" + s.getMaxDamage();
        }

        @Override public int width(MinecraftClient mc) {
            ItemStack s = mc.player == null ? ItemStack.EMPTY : mc.player.getMainHandStack();
            int w = Math.max(mc.textRenderer.getWidth(name(s)), mc.textRenderer.getWidth(dura(s)));
            return Math.max(40, 24 + w + 6);
        }

        @Override public int height(MinecraftClient mc) { return 22; }

        @Override public void draw(DrawContext ctx, MinecraftClient mc, int x, int y) {
            if (Modules.HELD_BG.get()) ctx.fill(x, y, x + width(mc), y + height(mc), BG);
            if (mc.player == null) return;
            ItemStack s = mc.player.getMainHandStack();
            if (s.isEmpty()) return;
            ctx.drawItem(s, x + 3, y + 3);
            int c = ColorUtil.argb(Modules.HELD_COLOR.get(), 255);
            String n = name(s);
            String d = dura(s);
            if (!n.isEmpty() && !d.isEmpty()) {
                ctx.drawText(mc.textRenderer, n, x + 24, y + 3, c, true);
                ctx.drawText(mc.textRenderer, d, x + 24, y + 12, 0xFFAAAAAA, true);
            } else if (!n.isEmpty()) {
                ctx.drawText(mc.textRenderer, n, x + 24, y + 7, c, true);
            } else if (!d.isEmpty()) {
                ctx.drawText(mc.textRenderer, d, x + 24, y + 7, c, true);
            }
        }
    }

    public static void init() {
        ELEMENTS.add(new TextElement(Modules.FPS_HUD, Modules.FPS_X, Modules.FPS_Y, () -> {
            List<String> l = new ArrayList<>();
            l.add("FPS: " + MinecraftClient.getInstance().getCurrentFps());
            return l;
        }, Modules.FPS_COLOR, Modules.FPS_BG));

        ELEMENTS.add(new TextElement(Modules.COORDS_HUD, Modules.COORDS_X, Modules.COORDS_Y, () -> {
            List<String> l = new ArrayList<>();
            ClientPlayerEntity p = MinecraftClient.getInstance().player;
            if (p == null) {
                l.add("XYZ: 0.0 / 0.0 / 0.0");
            } else {
                l.add(String.format(Locale.ROOT, "XYZ: %.1f / %.1f / %.1f", p.getX(), p.getY(), p.getZ()));
                if (Modules.COORDS_FACING.get()) {
                    String f = p.getHorizontalFacing().toString();
                    if (!f.isEmpty()) f = Character.toUpperCase(f.charAt(0)) + f.substring(1);
                    l.add("Facing: " + f);
                }
            }
            return l;
        }, Modules.COORDS_COLOR, Modules.COORDS_BG));

        ELEMENTS.add(new ArmorElement());
        ELEMENTS.add(new HeldElement());
    }

    // ------------------------------------------------------------------

    public static void render(DrawContext ctx, MinecraftClient mc) {
        if (mc.player == null || mc.world == null || mc.options.hudHidden) return;
        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();

        if (Modules.SCREEN_TINT.isEnabled()) {
            int alpha = Math.round(Modules.TINT_STRENGTH.getInt() * 255f / 100f);
            ctx.fill(0, 0, sw, sh, ColorUtil.argb(Modules.TINT_COLOR.get(), alpha));
        }

        if (Modules.CENTER_DOT.isEnabled()) {
            int s = Modules.DOT_SIZE.getInt();
            int cx = sw / 2;
            int cy = sh / 2;
            int c = ColorUtil.argb(Modules.DOT_COLOR.get(), 255);
            ctx.fill(cx - s, cy - s, cx + s, cy + s, c);
        }

        for (HudElement e : ELEMENTS) {
            if (!e.module.isEnabled()) continue;
            int x = clamp(e.x(), 0, Math.max(0, sw - e.width(mc)));
            int y = clamp(e.y(), 0, Math.max(0, sh - e.height(mc)));
            e.draw(ctx, mc, x, y);
        }

        if (Modules.MARKERS_HUD.isEnabled()) renderMarkers(ctx, mc, sw, sh);
        TrajectoryFeature.render(ctx, mc, sw, sh);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // ------------------------------------------------------------------
    // Markers in the world
    // ------------------------------------------------------------------
    private static void renderMarkers(DrawContext ctx, MinecraftClient mc, int sw, int sh) {
        ClientPlayerEntity p = mc.player;
        double maxDist = Modules.MK_MAX_DIST.get();
        for (Marker m : MarkerManager.all()) {
            if (!MarkerManager.isVisibleHere(m, mc)) continue;

            double dx = m.x - p.getX();
            double dy = m.y - p.getEyeY();
            double dz = m.z - p.getZ();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (maxDist > 0 && dist > maxDist) continue;

            int color = ColorUtil.argb(m.color, 255);
            Projection.Result r = Projection.project(mc, m.x, m.y + 1.0, m.z, sw, sh);

            if (r.onScreen(sw, sh, 6)) {
                int x = (int) r.x;
                int y = (int) r.y;
                ctx.fill(x - 4, y - 4, x + 4, y + 4, 0xFF000000);
                ctx.fill(x - 3, y - 3, x + 3, y + 3, color);

                int ty = y - 16;
                String title = m.name;
                if (Modules.MK_DISTANCE.get()) title += "  " + Math.round(dist) + "m";
                int tw = mc.textRenderer.getWidth(title);
                ctx.drawText(mc.textRenderer, title, x - tw / 2, ty, color, true);
                if (Modules.MK_COORDS.get()) {
                    String cs = Math.round(m.x) + " " + Math.round(m.y) + " " + Math.round(m.z);
                    int cw = mc.textRenderer.getWidth(cs);
                    ctx.drawText(mc.textRenderer, cs, x - cw / 2, ty + 10, 0xFFCCCCCC, true);
                }
            } else if (Modules.MK_EDGE.get()) {
                double vx;
                double vy;
                if (r.front) {
                    vx = r.x - sw / 2.0;
                    vy = r.y - sh / 2.0;
                } else {
                    vx = r.dirX;
                    vy = -r.dirY;
                    if (Math.abs(vx) < 1e-3 && Math.abs(vy) < 1e-3) vy = 1.0;
                }
                double len = Math.sqrt(vx * vx + vy * vy);
                if (len < 1e-6) continue;
                vx /= len;
                vy /= len;
                double margin = 14;
                double hw = sw / 2.0 - margin;
                double hh = sh / 2.0 - margin;
                double t = Math.min(Math.abs(vx) < 1e-6 ? 1e9 : hw / Math.abs(vx),
                        Math.abs(vy) < 1e-6 ? 1e9 : hh / Math.abs(vy));
                int ex = (int) (sw / 2.0 + vx * t);
                int ey = (int) (sh / 2.0 + vy * t);
                ctx.fill(ex - 4, ey - 4, ex + 4, ey + 4, 0xFF000000);
                ctx.fill(ex - 3, ey - 3, ex + 3, ey + 3, color);

                String arrow;
                if (Math.abs(vx) > Math.abs(vy)) arrow = vx > 0 ? ">" : "<";
                else arrow = vy > 0 ? "v" : "^";
                String label = arrow + " " + m.name + (Modules.MK_DISTANCE.get() ? " " + Math.round(dist) + "m" : "");
                int lw = mc.textRenderer.getWidth(label);
                int lx = ex - lw / 2;
                lx = clamp(lx, 2, sw - lw - 2);
                int ly = ey < sh / 2 ? ey + 7 : ey - 16;
                ctx.drawText(mc.textRenderer, label, lx, ly, color, true);
            }
        }
    }
}
