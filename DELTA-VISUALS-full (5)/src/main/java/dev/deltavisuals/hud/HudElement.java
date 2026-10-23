package dev.deltavisuals.hud;

import dev.deltavisuals.module.Module;
import dev.deltavisuals.module.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** A draggable HUD widget. Position is stored in two hidden settings of its module. */
public abstract class HudElement {
    public final Module module;
    private final Setting.Num xs;
    private final Setting.Num ys;

    protected HudElement(Module module, Setting.Num xs, Setting.Num ys) {
        this.module = module;
        this.xs = xs;
        this.ys = ys;
    }

    public int x() { return xs.getInt(); }
    public int y() { return ys.getInt(); }

    public void setPos(int x, int y) {
        xs.set(x);
        ys.set(y);
    }

    public abstract int width(MinecraftClient mc);

    public abstract int height(MinecraftClient mc);

    public abstract void draw(DrawContext ctx, MinecraftClient mc, int x, int y);
}
