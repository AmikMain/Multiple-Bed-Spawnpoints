package net.amro.multiplebedspawnpoints.data;

import net.amro.multiplebedspawnpoints.ModInfo;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.ResourceLocation;

public final class FabricAttachments {
    public static final AttachmentType<PlayerBeds> PLAYER_BEDS = AttachmentRegistry.<PlayerBeds>builder()
            .initializer(PlayerBeds::new)
            .persistent(PlayerBeds.CODEC)
            .copyOnDeath()
            .buildAndRegister(ResourceLocation.fromNamespaceAndPath(ModInfo.MODID, "player_beds"));

    private FabricAttachments() {
    }

    /** Runs the static initializer. The type has to be registered before any player data is loaded. */
    public static void register() {
    }
}
