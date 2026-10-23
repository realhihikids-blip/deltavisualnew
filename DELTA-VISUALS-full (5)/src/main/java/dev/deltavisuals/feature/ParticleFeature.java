package dev.deltavisuals.feature;

import dev.deltavisuals.module.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.ThreadLocalRandom;

public final class ParticleFeature {
    private ParticleFeature() {}

    private static ParticleEffect pick(int index) {
        switch (index) {
            case 0: return ParticleTypes.END_ROD;
            case 1: return ParticleTypes.FLAME;
            case 2: return ParticleTypes.SOUL_FIRE_FLAME;
            case 3: return ParticleTypes.CRIT;
            case 4: return ParticleTypes.HEART;
            case 5: return ParticleTypes.HAPPY_VILLAGER;
            case 6: return ParticleTypes.ENCHANT;
            case 7: return ParticleTypes.PORTAL;
            default: return ParticleTypes.TOTEM_OF_UNDYING;
        }
    }

    public static void tick(MinecraftClient mc) {
        if (!Modules.PARTICLES.isEnabled()) return;
        if (mc.player == null || mc.world == null || mc.isPaused()) return;

        if (Modules.PARTICLE_MOVING.get()) {
            Vec3d v = mc.player.getVelocity();
            if (v.x * v.x + v.z * v.z < 0.0004) return;
        }

        ThreadLocalRandom r = ThreadLocalRandom.current();
        ParticleEffect type = pick(Modules.PARTICLE_TYPE.index());
        int amount = Modules.PARTICLE_AMOUNT.getInt();
        double spread = Modules.PARTICLE_SPREAD.get();
        double height = Modules.PARTICLE_HEIGHT.get();

        for (int i = 0; i < amount; i++) {
            double x = mc.player.getX() + (r.nextDouble() - 0.5) * spread * 2.0;
            double y = mc.player.getY() + r.nextDouble() * height;
            double z = mc.player.getZ() + (r.nextDouble() - 0.5) * spread * 2.0;
            mc.particleManager.addParticle(type, x, y, z, 0.0, 0.02, 0.0);
        }
    }
}
