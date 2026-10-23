package dev.deltavisuals;

import dev.deltavisuals.bind.BindManager;
import dev.deltavisuals.config.ConfigManager;
import dev.deltavisuals.feature.DeathTracker;
import dev.deltavisuals.feature.ParticleFeature;
import dev.deltavisuals.feature.PearlSwap;
import dev.deltavisuals.hud.HudRenderer;
import dev.deltavisuals.marker.MarkerManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Client entrypoint: wires every system together. */
public final class DeltaVisuals implements ClientModInitializer {
    public static final String MOD_ID = "delta_visuals";
    public static final Logger LOGGER = LoggerFactory.getLogger("DELTA VISUALS");

    @Override
    public void onInitializeClient() {
        PearlSwap.init();
        HudRenderer.init();
        ConfigManager.init();

        ClientTickEvents.END_CLIENT_TICK.register(DeltaVisuals::onTick);
        HudRenderCallback.EVENT.register((drawContext, tickCounter) ->
                HudRenderer.render(drawContext, MinecraftClient.getInstance()));

        LOGGER.info("DELTA VISUALS loaded. Open the menu with the key set in Settings (default: Right Shift).");
    }

    private static void onTick(MinecraftClient mc) {
        BindManager.tick(mc);
        ParticleFeature.tick(mc);
        DeathTracker.tick(mc);
        MarkerManager.tick(mc);
        ConfigManager.tick();
    }
}
