package dev.deltavisuals.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.deltavisuals.DeltaVisuals;
import dev.deltavisuals.marker.MarkerManager;
import dev.deltavisuals.module.Module;
import dev.deltavisuals.module.Modules;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Stores configs as JSON in .minecraft/config/deltavisuals/.
 * Everything is managed from the in-game GUI.
 */
public final class ConfigManager {
    private ConfigManager() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_-]{1,24}");
    public static final String DEFAULT_NAME = "default";

    private static Path dir;
    private static String current = DEFAULT_NAME;
    private static boolean dirty;
    private static boolean loading;
    private static long lastSave;

    /** Message shown in the Configs tab. */
    public static String status = "";

    public static void init() {
        try {
            dir = FabricLoader.getInstance().getConfigDir().resolve("deltavisuals");
            Files.createDirectories(dir);
        } catch (IOException e) {
            DeltaVisuals.LOGGER.error("Cannot create config directory", e);
            return;
        }
        readState();
        if (!Files.exists(file(current))) {
            current = DEFAULT_NAME;
            if (!Files.exists(file(DEFAULT_NAME))) {
                save();
            }
        }
        load(current);
    }

    // ---------------------------------------------------------------

    public static String current() { return current; }

    public static void markDirty() {
        if (!loading) dirty = true;
    }

    public static boolean isValidName(String name) {
        return name != null && VALID_NAME.matcher(name).matches();
    }

    public static List<String> list() {
        List<String> names = new ArrayList<>();
        if (dir == null) return names;
        try (Stream<Path> s = Files.list(dir)) {
            s.forEach(p -> {
                String n = p.getFileName().toString();
                if (n.endsWith(".json") && !n.equals("state.json")) {
                    names.add(n.substring(0, n.length() - 5));
                }
            });
        } catch (IOException ignored) {
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    /** Called every client tick: auto-saves changes every few seconds. */
    public static void tick() {
        if (!dirty || dir == null) return;
        if (!Modules.UI_AUTOSAVE.get()) return;
        long now = System.currentTimeMillis();
        if (now - lastSave > 3000) save();
    }

    // ---------------------------------------------------------------

    public static boolean save() {
        if (dir == null) return false;
        try {
            Files.writeString(file(current), GSON.toJson(snapshot()));
            dirty = false;
            lastSave = System.currentTimeMillis();
            writeState();
            return true;
        } catch (Exception e) {
            DeltaVisuals.LOGGER.error("Failed to save config '{}'", current, e);
            status = "Save failed: " + e.getMessage();
            return false;
        }
    }

    /** Creates a new config from the current settings and switches to it. */
    public static boolean create(String name) {
        if (!isValidName(name)) {
            status = "Name: 1-24 chars, letters/digits/_/-";
            return false;
        }
        if (Files.exists(file(name))) {
            status = "Config '" + name + "' already exists";
            return false;
        }
        String old = current;
        current = name;
        if (!save()) {
            current = old;
            return false;
        }
        status = "Created '" + name + "'";
        return true;
    }

    public static boolean load(String name) {
        if (dir == null) return false;
        Path path = file(name);
        if (!Files.exists(path)) {
            status = "Config '" + name + "' not found";
            return false;
        }
        loading = true;
        try {
            String text = Files.readString(path);
            JsonElement root = JsonParser.parseString(text);
            if (root == null || !root.isJsonObject()) throw new IllegalStateException("Root is not an object");
            apply(root.getAsJsonObject());
            current = name;
            dirty = false;
            writeState();
            status = "Loaded '" + name + "'";
            return true;
        } catch (Exception e) {
            DeltaVisuals.LOGGER.error("Config '{}' is corrupted, restoring defaults", name, e);
            try {
                Files.move(path, path.resolveSibling(name + ".json.corrupt"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
            }
            applyDefaults();
            current = name;
            loading = false;
            save();
            status = "'" + name + "' was damaged -> defaults restored (old file kept as .corrupt)";
            return false;
        } finally {
            loading = false;
        }
    }

    public static boolean rename(String oldName, String newName) {
        if (!isValidName(newName)) {
            status = "Name: 1-24 chars, letters/digits/_/-";
            return false;
        }
        if (Files.exists(file(newName))) {
            status = "Config '" + newName + "' already exists";
            return false;
        }
        try {
            if (oldName.equals(current)) save();
            Files.move(file(oldName), file(newName));
            if (oldName.equals(current)) {
                current = newName;
                writeState();
            }
            status = "Renamed to '" + newName + "'";
            return true;
        } catch (IOException e) {
            status = "Rename failed: " + e.getMessage();
            return false;
        }
    }

    public static boolean delete(String name) {
        try {
            Files.deleteIfExists(file(name));
            if (name.equals(current)) {
                current = DEFAULT_NAME;
                if (Files.exists(file(DEFAULT_NAME))) load(DEFAULT_NAME);
                else {
                    applyDefaults();
                    save();
                }
            }
            status = "Deleted '" + name + "'";
            return true;
        } catch (IOException e) {
            status = "Delete failed: " + e.getMessage();
            return false;
        }
    }

    /** "Default Config": resets all modules, settings, binds, colors and markers. */
    public static void resetToDefaults() {
        applyDefaults();
        save();
        status = "All settings reset to default";
    }

    // ---------------------------------------------------------------

    private static JsonObject snapshot() {
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        JsonObject modules = new JsonObject();
        for (Module m : Modules.ALL) modules.add(m.name, m.toJson());
        root.add("modules", modules);
        root.add("markers", MarkerManager.toJson());
        return root;
    }

    private static void apply(JsonObject root) {
        applyDefaults();
        JsonElement mods = root.get("modules");
        if (mods != null && mods.isJsonObject()) {
            JsonObject mo = mods.getAsJsonObject();
            for (Module m : Modules.ALL) {
                JsonElement e = mo.get(m.name);
                if (e != null && e.isJsonObject()) m.fromJson(e.getAsJsonObject());
            }
        }
        MarkerManager.fromJson(root.get("markers"));
    }

    private static void applyDefaults() {
        boolean was = loading;
        loading = true;
        for (Module m : Modules.ALL) m.resetToDefaults();
        MarkerManager.clear();
        loading = was;
    }

    private static Path file(String name) {
        return dir.resolve(name + ".json");
    }

    private static void readState() {
        try {
            Path p = dir.resolve("state.json");
            if (Files.exists(p)) {
                JsonObject o = JsonParser.parseString(Files.readString(p)).getAsJsonObject();
                if (o.has("current")) {
                    String n = o.get("current").getAsString();
                    if (isValidName(n)) current = n;
                }
            }
        } catch (Exception ignored) {
            current = DEFAULT_NAME;
        }
    }

    private static void writeState() {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("current", current);
            Files.writeString(dir.resolve("state.json"), GSON.toJson(o));
        } catch (Exception ignored) {
        }
    }
}
