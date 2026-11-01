package net.amro.multiplebedspawnpoints.client;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/** The key that opens the bed list. Each loader registers it and calls {@link #handlePresses} every client tick. */
public final class BedListKey {
    private BedListKey() {
    }

    public static KeyMapping create() {
        return new KeyMapping(
                "key.multiplebedspawnpoints.open_bed_list",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                "key.categories.multiplebedspawnpoints");
    }

    /** @param canOpen false when the list can't be used, for example on a server without the mod */
    public static void handlePresses(KeyMapping key, boolean canOpen) {
        Minecraft minecraft = Minecraft.getInstance();
        while (key.consumeClick()) {
            if (canOpen && minecraft.screen == null && minecraft.player != null) {
                minecraft.setScreen(new BedListScreen(null));
            }
        }
    }
}
