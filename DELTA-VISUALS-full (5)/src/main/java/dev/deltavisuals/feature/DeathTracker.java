package dev.deltavisuals.feature;

import dev.deltavisuals.marker.Marker;
import dev.deltavisuals.marker.MarkerManager;
import dev.deltavisuals.module.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

public final class DeathTracker {
    private DeathTracker() {}

    private static boolean wasDead;
    private static boolean hasPos;
    private static double lx, ly, lz;

    public static void tick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) {
            wasDead = false;
            hasPos = false;
            return;
        }
        if (p.getHealth() <= 0.0f) {
            if (!wasDead) {
                wasDead = true;
                if (Modules.DEATH_MARKER.isEnabled() && hasPos) {
                    MarkerManager.add(new Marker("Death", lx, ly, lz, 0xFF5555, MarkerManager.currentDimension(mc)));
                }
            }
        } else {
            wasDead = false;
            lx = p.getX();
            ly = p.getY();
            lz = p.getZ();
            hasPos = true;
        }
    }
}
