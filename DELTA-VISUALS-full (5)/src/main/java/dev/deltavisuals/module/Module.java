package dev.deltavisuals.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.deltavisuals.config.ConfigManager;

import java.util.ArrayList;
import java.util.List;

public class Module {
    public final String name;
    public final String description;
    public final Category category;

    private final boolean defaultEnabled;
    private boolean enabled;
    private boolean alwaysOn;
    private int bind = -1;
    private Runnable onBind;
    private final List<Setting> settings = new ArrayList<>();

    /** UI-only state (not saved). */
    public boolean expanded;

    public Module(String name, String description, Category category, boolean defaultEnabled) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.defaultEnabled = defaultEnabled;
        this.enabled = defaultEnabled;
    }

    public Module alwaysOn() {
        this.alwaysOn = true;
        return this;
    }

    public boolean isAlwaysOn() { return alwaysOn; }

    public <T extends Setting> T add(T setting) {
        settings.add(setting);
        return setting;
    }

    public List<Setting> settings() { return settings; }

    public boolean isEnabled() { return alwaysOn || enabled; }

    public void setEnabled(boolean value) {
        if (alwaysOn || value == enabled) return;
        enabled = value;
        ConfigManager.markDirty();
    }

    public void toggle() { setEnabled(!enabled); }

    public int getBind() { return bind; }

    public void setBind(int key) {
        if (this.bind != key) {
            this.bind = key;
            ConfigManager.markDirty();
        }
    }

    /** For action modules (e.g. Pearl Swap): the bind runs this action instead of toggling the module. */
    public Module setOnBind(Runnable action) {
        this.onBind = action;
        return this;
    }

    public boolean isActionModule() { return onBind != null; }

    public void triggerBind() {
        if (onBind != null) {
            if (isEnabled()) onBind.run();
        } else if (!alwaysOn) {
            toggle();
        }
    }

    public void resetToDefaults() {
        enabled = defaultEnabled;
        bind = -1;
        for (Setting s : settings) s.reset();
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("enabled", enabled);
        o.addProperty("bind", bind);
        JsonObject s = new JsonObject();
        for (Setting setting : settings) s.add(setting.name, setting.toJson());
        o.add("settings", s);
        return o;
    }

    public void fromJson(JsonObject o) {
        try {
            JsonElement en = o.get("enabled");
            if (en != null && en.isJsonPrimitive() && en.getAsJsonPrimitive().isBoolean()) enabled = en.getAsBoolean();
            JsonElement b = o.get("bind");
            if (b != null && b.isJsonPrimitive() && b.getAsJsonPrimitive().isNumber()) {
                int k = b.getAsInt();
                bind = (k >= 32 && k <= 348) ? k : -1;
            }
            JsonElement ss = o.get("settings");
            if (ss != null && ss.isJsonObject()) {
                JsonObject so = ss.getAsJsonObject();
                for (Setting setting : settings) {
                    if (so.has(setting.name)) setting.fromJson(so.get(setting.name));
                }
            }
        } catch (Exception ignored) {
            // A bad entry must never crash the game; defaults were already applied.
        }
    }
}
