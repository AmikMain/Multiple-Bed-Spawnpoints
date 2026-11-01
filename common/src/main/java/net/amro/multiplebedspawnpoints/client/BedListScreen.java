package net.amro.multiplebedspawnpoints.client;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.amro.multiplebedspawnpoints.network.BedListSyncPayload;
import net.amro.multiplebedspawnpoints.network.BedView;
import net.amro.multiplebedspawnpoints.network.RequestBedListPayload;
import net.amro.multiplebedspawnpoints.platform.ModPlatform;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class BedListScreen extends Screen {
    private static final int LIST_TOP = 40;
    private static final int FOOTER_HEIGHT = 36;
    private static final int FOOTER_BUTTON_WIDTH = 150;

    // The death screen to go back to, or null when opened by a living player with the key.
    @Nullable
    private final Screen parent;
    private List<BedView> beds = List.of();
    private int maxBeds;
    private Component message = CommonComponents.EMPTY;
    private boolean respawnRequested;
    private BedSelectionList list;

    public BedListScreen(@Nullable Screen parent) {
        super(Component.translatable("gui.multiplebedspawnpoints.title"));
        this.parent = parent;
        ModPlatform.get().sendToServer(RequestBedListPayload.INSTANCE);
    }

    @Override
    protected void init() {
        this.list = addRenderableWidget(new BedSelectionList(this.minecraft, this, this.width, this.height - LIST_TOP - FOOTER_HEIGHT, LIST_TOP));
        this.list.setBeds(this.beds);

        int buttonY = this.height - 28;
        if (isPlayerDead()) {
            addRenderableWidget(Button.builder(Component.translatable("gui.multiplebedspawnpoints.world_spawn"), button -> respawnAtWorldSpawn())
                    .bounds(this.width / 2 - FOOTER_BUTTON_WIDTH - 4, buttonY, FOOTER_BUTTON_WIDTH, 20)
                    .build());
            addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> onClose())
                    .bounds(this.width / 2 + 4, buttonY, FOOTER_BUTTON_WIDTH, 20)
                    .build());
        } else {
            addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                    .bounds(this.width / 2 - FOOTER_BUTTON_WIDTH / 2, buttonY, FOOTER_BUTTON_WIDTH, 20)
                    .build());
        }
    }

    public void update(BedListSyncPayload payload) {
        this.beds = payload.beds();
        this.maxBeds = payload.maxBeds();
        this.message = payload.messageKey().isEmpty()
                ? CommonComponents.EMPTY
                : Component.translatable(payload.messageKey()).withStyle(ChatFormatting.RED);
        this.respawnRequested = false;
        if (this.list != null) {
            this.list.setBeds(this.beds);
        }
    }

    public boolean canRespawn() {
        return isPlayerDead() && !this.respawnRequested;
    }

    public void onRespawnRequested() {
        this.respawnRequested = true;
    }

    private boolean isPlayerDead() {
        return this.minecraft.player != null && this.minecraft.player.isDeadOrDying();
    }

    private void respawnAtWorldSpawn() {
        if (canRespawn()) {
            onRespawnRequested();
            this.minecraft.player.respawn();
        }
    }

    @Override
    public void tick() {
        // Vanilla only closes its own death screens on respawn.
        if (this.parent != null && !isPlayerDead()) {
            this.minecraft.setScreen(null);
            return;
        }
        this.list.tick();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        Component header = this.maxBeds > 0
                ? this.title.copy().append(" (" + this.beds.size() + "/" + this.maxBeds + ")")
                : this.title;
        guiGraphics.drawCenteredString(this.font, header, this.width / 2, 12, 0xFFFFFFFF);
        guiGraphics.drawCenteredString(this.font, this.message, this.width / 2, 26, 0xFFFFFFFF);

        if (this.beds.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("gui.multiplebedspawnpoints.empty"),
                    this.width / 2, this.height / 2 - 4, 0xFFAAAAAA);
        }
    }

    @Override
    public void removed() {
        this.list.commitNames();
    }

    @Override
    public void onClose() {
        // setScreen(null) while dead would make Minecraft open a new DeathScreen without the cause of death.
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        // Pausing a singleplayer world would also freeze the cooldowns on screen.
        return false;
    }
}
