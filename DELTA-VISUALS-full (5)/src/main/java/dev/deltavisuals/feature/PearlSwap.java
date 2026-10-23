package dev.deltavisuals.feature;

import dev.deltavisuals.module.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

/**
 * Switches the selected hotbar slot to an ender pearl on a key press.
 * It does NOT aim and does NOT throw anything - you still throw the pearl yourself.
 */
public final class PearlSwap {
    private PearlSwap() {}

    private static int previousSlot = -1;

    public static void init() {
        Modules.PEARL_SWAP.setOnBind(PearlSwap::swap);
    }

    public static void swap() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.currentScreen != null) return;

        PlayerInventory inv = p.getInventory();
        int cur = inv.getSelectedSlot();

        if (inv.getStack(cur).isOf(Items.ENDER_PEARL)) {
            if (Modules.PEARL_BACK.get() && previousSlot >= 0 && previousSlot != cur) {
                inv.setSelectedSlot(previousSlot);
                previousSlot = -1;
            }
            return;
        }

        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).isOf(Items.ENDER_PEARL)) {
                previousSlot = cur;
                inv.setSelectedSlot(i);
                return;
            }
        }
        if (Modules.PEARL_MESSAGE.get()) {
            p.sendMessage(Text.literal("Pearl Swap: no ender pearl in your hotbar"), true);
        }
    }
}
