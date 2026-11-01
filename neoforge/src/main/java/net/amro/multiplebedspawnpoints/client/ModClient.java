package net.amro.multiplebedspawnpoints.client;

import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.amro.multiplebedspawnpoints.MultipleBedSpawnpoints;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = MultipleBedSpawnpoints.MODID, dist = Dist.CLIENT)
public class ModClient {
    private static final KeyMapping OPEN_BED_LIST = new KeyMapping(
            "key.multiplebedspawnpoints.open_bed_list",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            "key.categories.multiplebedspawnpoints");

    public ModClient(IEventBus modEventBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        modEventBus.addListener(ModClient::registerKeyMappings);
        NeoForge.EVENT_BUS.addListener(ModClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(ModClient::onScreenInit);
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_BED_LIST);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (OPEN_BED_LIST.consumeClick()) {
            if (minecraft.screen == null && minecraft.player != null) {
                minecraft.setScreen(new BedListScreen(null));
            }
        }
    }

    private static void onScreenInit(ScreenEvent.Init.Post event) {
        DeathScreenButtons.replaceRespawnButtons(event.getScreen(), List.copyOf(event.getListenersList()))
                .forEach(event::addListener);
    }
}
