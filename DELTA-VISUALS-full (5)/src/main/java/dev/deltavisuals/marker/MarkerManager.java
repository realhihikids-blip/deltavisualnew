package dev.deltavisuals.marker;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.deltavisuals.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;

import java.util.ArrayList;
import java.util.List;

public final class MarkerManager {
    private MarkerManager() {}

    private static final List<Marker> MARKERS = new ArrayList<>();

    public static final int[] PALETTE = {
            0xFF5555, 0xFFAA33, 0xFFFF55, 0x55FF55, 0x55FFFF, 0x5599FF, 0xB266FF, 0xFF66CC, 0xFFFFFF
    };

    public static List<Marker> all() {
        return MARKERS;
    }

    public static void add(Marker m) {
        MARKERS.add(m);
        ConfigManager.markDirty();
    }

    public static void remove(Marker m) {
        MARKERS.remove(m);
        ConfigManager.markDirty();
    }

    public static void clear() {
        MARKERS.clear();
    }

    public static String currentDimension(MinecraftClient mc) {
        if (mc.world == null) return null;
        return mc.world.getRegistryKey().getValue().toString();
    }

    public static boolean isVisibleHere(Marker m, MinecraftClient mc) {
        if (m.dimension == null) return true;
        String dim = currentDimension(mc);
        return dim == null || dim.equals(m.dimension);
    }

    /** Moves markers that follow a player. Called every client tick. */
    public static void tick(MinecraftClient mc) {
        if (mc.world == null) return;
        boolean any = false;
        for (Marker m : MARKERS) if (m.trackedPlayer != null) { any = true; break; }
        if (!any) return;
        for (Marker m : MARKERS) {
            if (m.trackedPlayer == null) continue;
            for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
                if (p.getName().getString().equalsIgnoreCase(m.trackedPlayer)) {
                    m.x = p.getX();
                    m.y = p.getY();
                    m.z = p.getZ();
                    break;
                }
            }
        }
    }

    public static JsonArray toJson() {
        JsonArray arr = new JsonArray();
        for (Marker m : MARKERS) arr.add(m.toJson());
        return arr;
    }

    public static void fromJson(JsonElement e) {
        MARKERS.clear();
        if (e == null || !e.isJsonArray()) return;
        for (JsonElement el : e.getAsJsonArray()) {
            if (!el.isJsonObject()) continue;
            JsonObject o = el.getAsJsonObject();
            Marker m = Marker.fromJson(o);
            if (m != null) MARKERS.add(m);
            if (MARKERS.size() >= 200) break;
        }
    }

    // ---------- direction helpers ----------

    private static final String[] COMPASS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

    /** Compass heading of the marker from the player, e.g. "NE". */
    public static String compass(double playerX, double playerZ, double mx, double mz) {
        double dx = mx - playerX;
        double dz = mz - playerZ;
        // Minecraft: yaw 0 = south (+Z). Heading from north = yaw + 180.
        double yaw = Math.toDegrees(Math.atan2(-dx, dz));
        double heading = ((yaw + 180.0) % 360.0 + 360.0) % 360.0;
        int idx = (int) Math.round(heading / 45.0) % 8;
        return COMPASS[idx];
    }

    /** Relative direction like "Ahead", "Right", "Behind", "Left". */
    public static String relative(double playerX, double playerZ, float playerYaw, double mx, double mz) {
        double dx = mx - playerX;
        double dz = mz - playerZ;
        double yaw = Math.toDegrees(Math.atan2(-dx, dz));
        double diff = ((yaw - playerYaw) % 360.0 + 540.0) % 360.0 - 180.0; // -180..180, + = to the left
        double a = Math.abs(diff);
        if (a < 30) return "Ahead";
        if (a > 150) return "Behind";
        return diff > 0 ? "Left" : "Right";
    }
}
