package dev.deltavisuals.module;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Registry of every module and the settings other classes need to read. */
public final class Modules {
    private Modules() {}

    public static final List<Module> ALL = new ArrayList<>();

    private static Module reg(Module m) {
        ALL.add(m);
        return m;
    }

    // ===================== VISUALS =====================
    public static final Module PARTICLES = reg(new Module("Particles",
            "Spawns custom particles around your character.", Category.VISUALS, false));
    public static final Setting.Mode PARTICLE_TYPE = PARTICLES.add(new Setting.Mode("Type", 0,
            "End Rod", "Flame", "Soul Fire", "Crit", "Heart", "Happy Villager", "Enchant", "Portal", "Totem"));
    public static final Setting.Num PARTICLE_AMOUNT = PARTICLES.add(new Setting.Num("Amount", 2, 1, 10, 1));
    public static final Setting.Num PARTICLE_SPREAD = PARTICLES.add(new Setting.Num("Spread", 0.8, 0.2, 3.0, 0.1));
    public static final Setting.Num PARTICLE_HEIGHT = PARTICLES.add(new Setting.Num("Height", 1.0, 0.2, 2.0, 0.1));
    public static final Setting.Bool PARTICLE_MOVING = PARTICLES.add(new Setting.Bool("Only While Moving", true));

    public static final Module TRAJECTORIES = reg(new Module("Trajectories",
            "Shows where arrows and ender pearls will land.", Category.VISUALS, false));
    public static final Setting.Bool TRAJ_HELD = TRAJECTORIES.add(new Setting.Bool("Held Item Prediction", true));
    public static final Setting.Bool TRAJ_FLIGHT = TRAJECTORIES.add(new Setting.Bool("Projectiles In Flight", true));
    public static final Setting.Bool TRAJ_PATH = TRAJECTORIES.add(new Setting.Bool("Draw Path", true));
    public static final Setting.Bool TRAJ_DISTANCE = TRAJECTORIES.add(new Setting.Bool("Show Distance", true));
    public static final Setting.Color TRAJ_COLOR = TRAJECTORIES.add(new Setting.Color("Color", 0xB266FF));

    public static final Module SCREEN_TINT = reg(new Module("Screen Tint",
            "Adds a soft colored overlay to your screen.", Category.VISUALS, false));
    public static final Setting.Color TINT_COLOR = SCREEN_TINT.add(new Setting.Color("Color", 0x7A3CFF));
    public static final Setting.Num TINT_STRENGTH = SCREEN_TINT.add(new Setting.Num("Strength", 15, 1, 60, 1));

    public static final Module CENTER_DOT = reg(new Module("Center Dot",
            "Draws a small dot in the middle of the screen.", Category.VISUALS, false));
    public static final Setting.Color DOT_COLOR = CENTER_DOT.add(new Setting.Color("Color", 0xFFFFFF));
    public static final Setting.Num DOT_SIZE = CENTER_DOT.add(new Setting.Num("Size", 2, 1, 6, 1));

    // ===================== RENDER =====================
    public static final Module HAND_TRANSFORM = reg(new Module("Hand Transform",
            "Changes size and position of your hands and held items.", Category.RENDER, false));
    public static final Setting.Bool HAND_MAIN = HAND_TRANSFORM.add(new Setting.Bool("Main Hand", true));
    public static final Setting.Bool HAND_OFF = HAND_TRANSFORM.add(new Setting.Bool("Off Hand", true));
    public static final Setting.Num HAND_X = HAND_TRANSFORM.add(new Setting.Num("Offset X", 0, -1, 1, 0.05));
    public static final Setting.Num HAND_Y = HAND_TRANSFORM.add(new Setting.Num("Offset Y", 0, -1, 1, 0.05));
    public static final Setting.Num HAND_Z = HAND_TRANSFORM.add(new Setting.Num("Offset Z", 0, -1, 1, 0.05));
    public static final Setting.Num HAND_SCALE = HAND_TRANSFORM.add(new Setting.Num("Scale", 1.0, 0.3, 1.5, 0.05));
    public static final Setting.Num HAND_ROLL = HAND_TRANSFORM.add(new Setting.Num("Roll", 0, -45, 45, 1));

    public static final Module HAND_VISIBILITY = reg(new Module("Hand Visibility",
            "Hide your hands or held items in first person.", Category.RENDER, false));
    public static final Setting.Bool HIDE_MAIN = HAND_VISIBILITY.add(new Setting.Bool("Hide Main Hand Item", false));
    public static final Setting.Bool HIDE_OFF = HAND_VISIBILITY.add(new Setting.Bool("Hide Off Hand Item", false));
    public static final Setting.Bool HIDE_EMPTY = HAND_VISIBILITY.add(new Setting.Bool("Hide Empty Hand", false));

    // ===================== HUD =====================
    public static final Module FPS_HUD = reg(new Module("FPS", "Shows your frames per second.", Category.HUD, true));
    public static final Setting.Color FPS_COLOR = FPS_HUD.add(new Setting.Color("Color", 0xFFFFFF));
    public static final Setting.Bool FPS_BG = FPS_HUD.add(new Setting.Bool("Background", true));
    public static final Setting.Num FPS_X = (Setting.Num) FPS_HUD.add(new Setting.Num("x", 4, 0, 5000, 1)).hide();
    public static final Setting.Num FPS_Y = (Setting.Num) FPS_HUD.add(new Setting.Num("y", 4, 0, 5000, 1)).hide();

    public static final Module COORDS_HUD = reg(new Module("Coordinates", "Shows your XYZ position.", Category.HUD, true));
    public static final Setting.Color COORDS_COLOR = COORDS_HUD.add(new Setting.Color("Color", 0xFFFFFF));
    public static final Setting.Bool COORDS_BG = COORDS_HUD.add(new Setting.Bool("Background", true));
    public static final Setting.Bool COORDS_FACING = COORDS_HUD.add(new Setting.Bool("Show Facing", true));
    public static final Setting.Num COORDS_X = (Setting.Num) COORDS_HUD.add(new Setting.Num("x", 4, 0, 5000, 1)).hide();
    public static final Setting.Num COORDS_Y = (Setting.Num) COORDS_HUD.add(new Setting.Num("y", 18, 0, 5000, 1)).hide();

    public static final Module ARMOR_HUD = reg(new Module("Armor Durability",
            "Shows the durability of your armor pieces.", Category.HUD, false));
    public static final Setting.Mode ARMOR_LAYOUT = ARMOR_HUD.add(new Setting.Mode("Layout", 0, "Vertical", "Horizontal"));
    public static final Setting.Bool ARMOR_NUMBERS = ARMOR_HUD.add(new Setting.Bool("Show Percent", true));
    public static final Setting.Num ARMOR_WARN = ARMOR_HUD.add(new Setting.Num("Warn Below %", 20, 0, 100, 5));
    public static final Setting.Color ARMOR_COLOR = ARMOR_HUD.add(new Setting.Color("Color", 0xFFFFFF));
    public static final Setting.Bool ARMOR_BG = ARMOR_HUD.add(new Setting.Bool("Background", true));
    public static final Setting.Num ARMOR_X = (Setting.Num) ARMOR_HUD.add(new Setting.Num("x", 4, 0, 5000, 1)).hide();
    public static final Setting.Num ARMOR_Y = (Setting.Num) ARMOR_HUD.add(new Setting.Num("y", 36, 0, 5000, 1)).hide();

    public static final Module HELD_HUD = reg(new Module("Held Item",
            "Shows the item in your hand with its name and durability.", Category.HUD, false));
    public static final Setting.Bool HELD_NAME = HELD_HUD.add(new Setting.Bool("Show Name", true));
    public static final Setting.Bool HELD_DURABILITY = HELD_HUD.add(new Setting.Bool("Show Durability", true));
    public static final Setting.Color HELD_COLOR = HELD_HUD.add(new Setting.Color("Color", 0xFFFFFF));
    public static final Setting.Bool HELD_BG = HELD_HUD.add(new Setting.Bool("Background", true));
    public static final Setting.Num HELD_X = (Setting.Num) HELD_HUD.add(new Setting.Num("x", 4, 0, 5000, 1)).hide();
    public static final Setting.Num HELD_Y = (Setting.Num) HELD_HUD.add(new Setting.Num("y", 100, 0, 5000, 1)).hide();

    public static final Module MARKERS_HUD = reg(new Module("Markers",
            "Shows your saved markers in the world with distance and direction.", Category.HUD, true));
    public static final Setting.Bool MK_DISTANCE = MARKERS_HUD.add(new Setting.Bool("Show Distance", true));
    public static final Setting.Bool MK_COORDS = MARKERS_HUD.add(new Setting.Bool("Show Coordinates", false));
    public static final Setting.Bool MK_EDGE = MARKERS_HUD.add(new Setting.Bool("Edge Arrows", true));
    public static final Setting.Num MK_MAX_DIST = MARKERS_HUD.add(new Setting.Num("Max Distance (0 = any)", 0, 0, 5000, 50));

    // ===================== PLAYER =====================
    public static final Module PEARL_SWAP = reg(new Module("Pearl Swap",
            "Press the bind to switch your hotbar to an ender pearl. Press again to go back.", Category.PLAYER, false));
    public static final Setting.Bool PEARL_BACK = PEARL_SWAP.add(new Setting.Bool("Swap Back On 2nd Press", true));
    public static final Setting.Bool PEARL_MESSAGE = PEARL_SWAP.add(new Setting.Bool("Show Message", true));

    public static final Module DEATH_MARKER = reg(new Module("Death Marker",
            "Automatically creates a marker where you died.", Category.PLAYER, true));

    // ===================== SETTINGS =====================
    public static final Module INTERFACE = reg(new Module("Interface",
            "Appearance of the DELTA VISUALS menu.", Category.SETTINGS, true).alwaysOn());
    public static final Setting.Color UI_ACCENT = INTERFACE.add(new Setting.Color("Accent Color", 0x9B59FF));
    public static final Setting.Num UI_OPACITY = INTERFACE.add(new Setting.Num("Opacity %", 92, 30, 100, 1));
    public static final Setting.Num UI_SCALE = INTERFACE.add(new Setting.Num("Interface Size %", 100, 80, 140, 5));
    public static final Setting.Bool UI_ANIM = INTERFACE.add(new Setting.Bool("Animations", true));
    public static final Setting.Bool UI_AUTOSAVE = INTERFACE.add(new Setting.Bool("Auto Save", true));
    public static final Setting.Num UI_MENU_KEY = (Setting.Num) INTERFACE.add(
            new Setting.Num("Menu Key", GLFW.GLFW_KEY_RIGHT_SHIFT, -1, 348, 1)).hide();

    public static List<Module> byCategory(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : ALL) if (m.category == c) out.add(m);
        return out;
    }
}
