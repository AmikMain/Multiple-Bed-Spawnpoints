package net.amro.multiplebedspawnpoints.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.contents.TranslatableContents;

/** Makes vanilla's respawn button open the bed list instead of respawning. */
public final class DeathScreenButtons {
    private static final String RESPAWN_BUTTON_KEY = "deathScreen.respawn";

    private DeathScreenButtons() {
    }

    /**
     * Hides the respawn buttons of a just initialized death screen.
     *
     * @param widgets the screen's current widgets
     * @return the buttons to add to the screen in their place, empty for any other screen
     */
    public static List<Button> replaceRespawnButtons(Screen screen, List<? extends GuiEventListener> widgets) {
        List<Button> replacements = new ArrayList<>();
        if (screen instanceof DeathScreen || screen instanceof DeathScreen.TitleConfirmScreen) {
            for (GuiEventListener widget : widgets) {
                if (widget instanceof Button button && isRespawnButton(button)) {
                    button.visible = false;
                    replacements.add(new OpenBedListButton(button, screen));
                }
            }
        }
        return replacements;
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
