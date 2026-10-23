package dev.deltavisuals.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

/** Projects a world position onto the 2D screen (first-person view) without touching render internals. */
public final class Projection {
    private Projection() {}

    public static final class Result {
        /** Screen coordinates (valid when front == true). */
        public double x, y;
        /** Camera-space direction (right, up) – used for edge arrows when off-screen. */
        public double dirX, dirY;
        /** True if the point is in front of the camera. */
        public boolean front;

        public boolean onScreen(int w, int h, int margin) {
            return front && x >= margin && x <= w - margin && y >= margin && y <= h - margin;
        }
    }

    public static Result project(MinecraftClient mc, double tx, double ty, double tz, int sw, int sh) {
        ClientPlayerEntity p = mc.player;
        Result r = new Result();
        if (p == null) return r;

        double ex = p.getX();
        double ey = p.getEyeY();
        double ez = p.getZ();

        double yaw = Math.toRadians(p.getYaw());
        double pitch = Math.toRadians(p.getPitch());

        // forward
        double fx = -Math.sin(yaw) * Math.cos(pitch);
        double fy = -Math.sin(pitch);
        double fz = Math.cos(yaw) * Math.cos(pitch);
        // right
        double rx = -Math.cos(yaw);
        double ry = 0;
        double rz = -Math.sin(yaw);
        // up = right x forward
        double ux = ry * fz - rz * fy;
        double uy = rz * fx - rx * fz;
        double uz = rx * fy - ry * fx;

        double dx = tx - ex;
        double dy = ty - ey;
        double dz = tz - ez;

        double cz = dx * fx + dy * fy + dz * fz;
        double cx = dx * rx + dy * ry + dz * rz;
        double cy = dx * ux + dy * uy + dz * uz;

        r.dirX = cx;
        r.dirY = cy;

        double fov = Math.toRadians(mc.options.getFov().getValue());
        double tanHalf = Math.tan(fov / 2.0);
        double aspect = sw / (double) Math.max(1, sh);

        if (cz > 0.05) {
            r.front = true;
            r.x = sw / 2.0 + (cx / cz) / (tanHalf * aspect) * (sw / 2.0);
            r.y = sh / 2.0 - (cy / cz) / tanHalf * (sh / 2.0);
        } else {
            r.front = false;
        }
        return r;
    }
}
