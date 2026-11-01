package net.amro.multiplebedspawnpoints.client;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.lwjgl.glfw.GLFW;

import net.amro.multiplebedspawnpoints.data.BedEntry;
import net.amro.multiplebedspawnpoints.network.BedView;
import net.amro.multiplebedspawnpoints.network.DeleteBedPayload;
import net.amro.multiplebedspawnpoints.network.RenameBedPayload;
import net.amro.multiplebedspawnpoints.network.RespawnAtBedPayload;
import net.amro.multiplebedspawnpoints.platform.ModPlatform;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.RespawnAnchorBlock;

public class BedSelectionList extends ContainerObjectSelectionList<BedSelectionList.Row> {
    private static final int ROW_HEIGHT = 50;
    private static final int RESPAWN_BUTTON_WIDTH = 90;
    private static final int DELETE_BUTTON_WIDTH = 60;
    private static final int BUTTON_GAP = 4;
    private static final int BUTTONS_WIDTH = RESPAWN_BUTTON_WIDTH + BUTTON_GAP + DELETE_BUTTON_WIDTH;

    private final BedListScreen screen;

    public BedSelectionList(Minecraft minecraft, BedListScreen screen, int width, int height, int y) {
        super(minecraft, width, height, y, ROW_HEIGHT);
        this.screen = screen;
    }

    @Override
    public int getRowWidth() {
        return Math.min(380, this.width - 24);
    }

    public void setBeds(List<BedView> beds) {
        List<UUID> currentIds = children().stream().map(row -> row.bed.id()).toList();
        if (currentIds.equals(beds.stream().map(BedView::id).toList())) {
            // Rebuilding the rows would drop keyboard focus from the name box being edited.
            for (int i = 0; i < beds.size(); i++) {
                getEntry(i).update(beds.get(i));
            }
            return;
        }

        commitNames();
        setFocused(null);
        List<Row> rows = new ArrayList<>();
        for (int i = 0; i < beds.size(); i++) {
            rows.add(new Row(beds.get(i), i + 1));
        }
        replaceEntries(rows);
        setScrollAmount(getScrollAmount());
    }

    public void tick() {
        children().forEach(Row::tick);
    }

    public void commitNames() {
        children().forEach(Row::commitName);
    }

    public class Row extends ContainerObjectSelectionList.Entry<Row> {
        private final EditBox nameBox;
        private final Button respawnButton;
        private final Button deleteButton;
        private BedView bed;
        private String savedName;
        private int cooldownTicks;

        private Row(BedView bed, int number) {
            Font font = BedSelectionList.this.minecraft.font;
            // The box needs its real width before setValue: scrolling to the cursor in a zero-width box
            // leaves no visible text, and EditBox then draws the hint over the actual name.
            int nameWidth = getRowWidth() - BUTTONS_WIDTH - BUTTON_GAP - 2;
            this.nameBox = new EditBox(font, nameWidth, 18, Component.translatable("gui.multiplebedspawnpoints.name"));
            this.nameBox.setMaxLength(BedEntry.MAX_NAME_LENGTH);
            String defaultName = bed.isAnchor() ? "gui.multiplebedspawnpoints.default_anchor_name" : "gui.multiplebedspawnpoints.default_name";
            this.nameBox.setHint(Component.translatable(defaultName, number));
            showName(bed.name());
            this.respawnButton = Button.builder(Component.translatable("gui.multiplebedspawnpoints.respawn"), button -> respawn())
                    .size(RESPAWN_BUTTON_WIDTH, 20)
                    .build();
            this.deleteButton = Button.builder(Component.translatable("gui.multiplebedspawnpoints.delete"), button -> delete())
                    .size(DELETE_BUTTON_WIDTH, 20)
                    .build();
            this.bed = bed;
            this.savedName = bed.name();
            this.cooldownTicks = bed.cooldownTicks();
        }

        private void update(BedView bed) {
            this.bed = bed;
            this.savedName = bed.name();
            this.cooldownTicks = bed.cooldownTicks();
            if (!this.nameBox.isFocused()) {
                showName(bed.name());
            }
        }

        private void showName(String name) {
            this.nameBox.setValue(name);
            this.nameBox.moveCursorToStart(false);
        }

        private void tick() {
            if (this.cooldownTicks > 0) {
                this.cooldownTicks--;
            }
            if (!this.nameBox.isFocused()) {
                commitName();
            }
        }

        private void commitName() {
            String name = this.nameBox.getValue().strip();
            if (!name.equals(this.savedName)) {
                this.savedName = name;
                ModPlatform.get().sendToServer(new RenameBedPayload(this.bed.id(), name));
            }
        }

        private void respawn() {
            BedSelectionList.this.screen.onRespawnRequested();
            ModPlatform.get().sendToServer(new RespawnAtBedPayload(this.bed.id()));
        }

        private void delete() {
            ModPlatform.get().sendToServer(new DeleteBedPayload(this.bed.id()));
        }

        private Component locationText() {
            BedView.Place place = this.bed.place();
            ResourceLocation dimension = place.location().dimension().location();
            String key = "dimension." + dimension.getNamespace() + "." + dimension.getPath();
            Component dimensionName = Component.translatableWithFallback(key, dimension.toString());
            if (!place.subLevelName().isEmpty()) {
                return Component.translatable("gui.multiplebedspawnpoints.location_sub_level", dimensionName, place.subLevelName());
            }

            BlockPos pos = place.location().pos();
            return Component.translatable("gui.multiplebedspawnpoints.location", dimensionName, pos.getX(), pos.getY(), pos.getZ());
        }

        private Component chargeText() {
            Object charge = this.bed.charge() == BedView.CHARGE_UNKNOWN ? "?" : this.bed.charge();
            return Component.translatable("gui.multiplebedspawnpoints.charge", charge, RespawnAnchorBlock.MAX_CHARGES)
                    .withStyle(ChatFormatting.YELLOW);
        }

        private Component statusText() {
            if (this.bed.charge() == 0) {
                return Component.translatable("gui.multiplebedspawnpoints.status.no_charge").withStyle(ChatFormatting.RED);
            }
            if (this.bed.obstructed()) {
                return Component.translatable("gui.multiplebedspawnpoints.status.obstructed").withStyle(ChatFormatting.RED);
            }
            if (this.cooldownTicks > 0) {
                int seconds = Mth.ceil(this.cooldownTicks / 20.0F);
                String time = String.format("%d:%02d", seconds / 60, seconds % 60);
                return Component.translatable("gui.multiplebedspawnpoints.status.cooldown", time).withStyle(ChatFormatting.RED);
            }
            return Component.translatable("gui.multiplebedspawnpoints.status.available").withStyle(ChatFormatting.GREEN);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            Font font = BedSelectionList.this.minecraft.font;

            this.nameBox.setPosition(left + 1, top + 2);
            this.nameBox.render(guiGraphics, mouseX, mouseY, partialTick);

            this.respawnButton.setPosition(left + width - BUTTONS_WIDTH, top + 1);
            this.respawnButton.active = BedSelectionList.this.screen.canRespawn()
                    && !this.bed.obstructed() && this.cooldownTicks <= 0 && this.bed.charge() != 0;
            this.respawnButton.render(guiGraphics, mouseX, mouseY, partialTick);

            this.deleteButton.setPosition(left + width - DELETE_BUTTON_WIDTH, top + 1);
            this.deleteButton.render(guiGraphics, mouseX, mouseY, partialTick);

            guiGraphics.drawString(font, locationText(), left + 2, top + 24, 0xFFAAAAAA);
            Component status = statusText();
            guiGraphics.drawString(font, status, left + 2, top + 35, 0xFFFFFFFF);
            if (this.bed.isAnchor()) {
                guiGraphics.drawString(font, chargeText(), left + 2 + font.width(status) + 10, top + 35, 0xFFFFFFFF);
            }
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && this.nameBox.isFocused()) {
                setFocused(null);
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(this.nameBox, this.respawnButton, this.deleteButton);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(this.nameBox, this.respawnButton, this.deleteButton);
        }
    }
}
