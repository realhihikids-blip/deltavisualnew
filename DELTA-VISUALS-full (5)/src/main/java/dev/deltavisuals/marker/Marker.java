package dev.deltavisuals.marker;

import com.google.gson.JsonObject;

public final class Marker {
    public String name;
    public double x, y, z;
    /** 0xRRGGBB */
    public int color;
    /** Dimension id like "minecraft:overworld"; null = visible everywhere. */
    public String dimension;
    /** If not null, the marker follows this player (set manually by the user). */
    public String trackedPlayer;

    public Marker(String name, double x, double y, double z, int color, String dimension) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = color & 0xFFFFFF;
        this.dimension = dimension;
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("name", name);
        o.addProperty("x", x);
        o.addProperty("y", y);
        o.addProperty("z", z);
        o.addProperty("color", String.format("#%06X", color));
        if (dimension != null) o.addProperty("dimension", dimension);
        if (trackedPlayer != null) o.addProperty("player", trackedPlayer);
        return o;
    }

    public static Marker fromJson(JsonObject o) {
        try {
            String name = o.has("name") ? o.get("name").getAsString() : "Marker";
            if (name.length() > 32) name = name.substring(0, 32);
            double x = o.get("x").getAsDouble();
            double y = o.get("y").getAsDouble();
            double z = o.get("z").getAsDouble();
            if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)) return null;
            int color = 0xFFFFFF;
            if (o.has("color")) {
                String s = o.get("color").getAsString().trim();
                if (s.startsWith("#")) s = s.substring(1);
                if (s.length() == 6) color = Integer.parseInt(s, 16);
            }
            String dim = o.has("dimension") ? o.get("dimension").getAsString() : null;
            Marker m = new Marker(name, x, y, z, color, dim);
            if (o.has("player")) m.trackedPlayer = o.get("player").getAsString();
            return m;
        } catch (Exception e) {
            return null;
        }
    }
}
