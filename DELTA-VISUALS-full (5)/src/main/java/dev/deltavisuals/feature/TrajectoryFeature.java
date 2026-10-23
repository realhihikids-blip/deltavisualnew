package dev.deltavisuals.feature;

import dev.deltavisuals.module.Modules;
import dev.deltavisuals.util.ColorUtil;
import dev.deltavisuals.util.Projection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Predicts where arrows and ender pearls will land and draws the result on the HUD.
 * This is purely visual information - it never aims or throws anything for you.
 */
public final class TrajectoryFeature {
    private TrajectoryFeature() {}

    private static List<Vec3d> simulate(MinecraftClient mc, Vec3d start, Vec3d velocity, double gravity, double drag) {
        List<Vec3d> pts = new ArrayList<>();
        Vec3d pos = start;
        Vec3d vel = velocity;
        pts.add(pos);
        for (int i = 0; i < 160; i++) {
            Vec3d next = pos.add(vel);
            BlockHitResult hit = mc.world.raycast(new RaycastContext(pos, next,
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
            if (hit.getType() != HitResult.Type.MISS) {
                pts.add(hit.getPos());
                return pts;
            }
            pos = next;
            pts.add(pos);
            vel = vel.multiply(drag).add(0.0, -gravity, 0.0);
            if (pos.y < mc.world.getBottomY() - 32) break;
        }
        return pts;
    }

    public static void render(DrawContext ctx, MinecraftClient mc, int sw, int sh) {
        if (!Modules.TRAJECTORIES.isEnabled()) return;
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) return;

        int color = ColorUtil.argb(Modules.TRAJ_COLOR.get(), 255);

        if (Modules.TRAJ_HELD.get()) {
            List<Vec3d> pts = predictHeld(mc, p);
            if (pts != null) draw(ctx, mc, pts, color, sw, sh);
        }

        if (Modules.TRAJ_FLIGHT.get()) {
            int count = 0;
            for (Entity e : mc.world.getEntities()) {
                if (count >= 6) break;
                double dx = e.getX() - p.getX();
                double dy = e.getY() - p.getY();
                double dz = e.getZ() - p.getZ();
                if (dx * dx + dy * dy + dz * dz > 128.0 * 128.0) continue;

                double gravity;
                if (e instanceof EnderPearlEntity) {
                    gravity = 0.03;
                } else if (e instanceof PersistentProjectileEntity) {
                    gravity = 0.05;
                } else {
                    continue;
                }
                Vec3d v = e.getVelocity();
                if (v.lengthSquared() < 0.04) continue; // stuck in a block / not moving

                List<Vec3d> pts = simulate(mc, new Vec3d(e.getX(), e.getY(), e.getZ()), v, gravity, 0.99);
                draw(ctx, mc, pts, color, sw, sh);
                count++;
            }
        }
    }

    private static List<Vec3d> predictHeld(MinecraftClient mc, ClientPlayerEntity p) {
        ItemStack main = p.getMainHandStack();
        ItemStack off = p.getOffHandStack();
        double speed;
        double gravity;

        if (isThrowable(main) || isThrowable(off)) {
            speed = 1.5;
            gravity = 0.03;
        } else if (p.isUsingItem() && p.getActiveItem().isOf(Items.BOW)) {
            float f = p.getItemUseTime() / 20.0f;
            f = (f * f + f * 2.0f) / 3.0f;
            if (f > 1.0f) f = 1.0f;
            if (f < 0.1f) return null;
            speed = f * 3.0;
            gravity = 0.05;
        } else {
            return null;
        }

        Vec3d look = p.getRotationVec(1.0f);
        Vec3d start = new Vec3d(p.getX(), p.getEyeY() - 0.1, p.getZ());
        return simulate(mc, start, look.multiply(speed), gravity, 0.99);
    }

    private static boolean isThrowable(ItemStack s) {
        return s.isOf(Items.ENDER_PEARL) || s.isOf(Items.SNOWBALL) || s.isOf(Items.EGG);
    }

    private static void draw(DrawContext ctx, MinecraftClient mc, List<Vec3d> pts, int color, int sw, int sh) {
        if (pts.size() < 2) return;

        if (Modules.TRAJ_PATH.get()) {
            for (int i = 2; i < pts.size() - 1; i += 3) {
                Vec3d v = pts.get(i);
                Projection.Result r = Projection.project(mc, v.x, v.y, v.z, sw, sh);
                if (r.onScreen(sw, sh, 0)) {
                    int x = (int) r.x;
                    int y = (int) r.y;
                    ctx.fill(x - 1, y - 1, x + 1, y + 1, ColorUtil.mulAlpha(color, 0.6f));
                }
            }
        }

        Vec3d end = pts.get(pts.size() - 1);
        Projection.Result r = Projection.project(mc, end.x, end.y, end.z, sw, sh);
        if (r.onScreen(sw, sh, 2)) {
            int x = (int) r.x;
            int y = (int) r.y;
            ctx.fill(x - 3, y - 3, x + 3, y + 3, 0xFF000000);
            ctx.fill(x - 2, y - 2, x + 2, y + 2, color);
            if (Modules.TRAJ_DISTANCE.get()) {
                double dx = end.x - mc.player.getX();
                double dy = end.y - mc.player.getEyeY();
                double dz = end.z - mc.player.getZ();
                int dist = (int) Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz));
                ctx.drawText(mc.textRenderer, dist + "m", x + 6, y - 4, 0xFFFFFFFF, true);
            }
        }
    }
}
