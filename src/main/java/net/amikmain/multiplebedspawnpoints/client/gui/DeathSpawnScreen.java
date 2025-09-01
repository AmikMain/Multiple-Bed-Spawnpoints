package net.amikmain.multiplebedspawnpoints.client.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class DeathSpawnScreen extends Screen {

    private static final ResourceLocation BACKGROUND = ResourceLocation.tryParse("multiplebedspawnpoints:textures/gui/dirt_background.png");

    protected DeathSpawnScreen() {
        super(Component.literal("Выбор спавна"));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // рисуем фон
        guiGraphics.blit(BACKGROUND, 0, 0, 0, 0, this.width, this.height, this.width, this.height);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
