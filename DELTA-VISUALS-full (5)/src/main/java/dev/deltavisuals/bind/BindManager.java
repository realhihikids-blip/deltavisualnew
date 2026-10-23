package dev.deltavisuals.bind;

import dev.deltavisuals.gui.ClickGuiScreen;
import dev.deltavisuals.module.Module;
import dev.deltavisuals.module.Modules;
import dev.deltavisuals.util.KeyState;
import net.minecraft.client.MinecraftClient;

public final class BindManager {
    private BindManager() {}

    private static KeyState keys;

    public static void tick(MinecraftClient mc) {
        if (keys == null) keys = new KeyState();
        keys.update();
        // Never react to keys while typing in chat, in menus, or while another screen is open.
        if (mc.currentScreen != null || mc.player == null) return;

        int menuKey = Modules.UI_MENU_KEY.getInt();
        if (menuKey >= 0 && keys.pressed(menuKey)) {
            mc.setScreen(new ClickGuiScreen());
            return;
        }
        for (Module m : Modules.ALL) {
            int b = m.getBind();
            if (b >= 0 && keys.pressed(b)) m.triggerBind();
        }
    }

    /**
     * Assigns a key to a module. If another module already uses this key, its bind is removed
     * (so one key never triggers two modules). Returns false if the key is reserved for the menu.
     */
    public static boolean assign(Module module, int key) {
        if (key < 0) {
            module.setBind(-1);
            return true;
        }
        if (key == Modules.UI_MENU_KEY.getInt()) return false;
        for (Module other : Modules.ALL) {
            if (other != module && other.getBind() == key) other.setBind(-1);
        }
        module.setBind(key);
        return true;
    }

    /** Assigns the key that opens the menu. Removes module binds that use the same key. */
    public static void assignMenuKey(int key) {
        for (Module other : Modules.ALL) {
            if (other.getBind() == key) other.setBind(-1);
        }
        Modules.UI_MENU_KEY.set(key);
    }
}
