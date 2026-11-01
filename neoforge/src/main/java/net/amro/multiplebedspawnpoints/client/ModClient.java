package net.amro.multiplebedspawnpoints.client;

import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.amro.multiplebedspawnpoints.MultipleBedSpawnpoints;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.contents.TranslatableContents;
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
    private static final String RESPAWN_BUTTON_KEY = "deathScreen.respawn";

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
        Screen screen = event.getScreen();
        if (!(screen instanceof DeathScreen || screen instanceof DeathScreen.TitleConfirmScreen)) {
            return;
        }

        for (GuiEventListener listener : List.copyOf(event.getListenersList())) {
            if (listener instanceof Button button && isRespawnButton(button)) {
                button.visible = false;
                event.addListener(new OpenBedListButton(button, screen));
            }
        }
    }

    private static boolean isRespawnButton(Button button) {
        return button.getMessage().getContents() instanceof TranslatableContents contents
                && RESPAWN_BUTTON_KEY.equals(contents.getKey());
    }

    /**
     * Stands in for vanilla's respawn button. DeathScreen keeps a private reference to the original
     * and enables it after a delay, so the original stays in place hidden and this one follows its state.
     */
    private static class OpenBedListButton extends Button {
        private final Button vanillaButton;

        private OpenBedListButton(Button vanillaButton, Screen deathScreen) {
            super(vanillaButton.getX(), vanillaButton.getY(), vanillaButton.getWidth(), vanillaButton.getHeight(),
                    vanillaButton.getMessage(),
                    button -> Minecraft.getInstance().setScreen(new BedListScreen(deathScreen)),
                    DEFAULT_NARRATION);
            this.vanillaButton = vanillaButton;
            this.active = vanillaButton.active;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            this.active = this.vanillaButton.active;
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        }
    }
}
