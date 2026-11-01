package net.amro.multiplebedspawnpoints.data;

import java.util.function.Supplier;

import net.amro.multiplebedspawnpoints.MultipleBedSpawnpoints;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MultipleBedSpawnpoints.MODID);

    public static final Supplier<AttachmentType<PlayerBeds>> PLAYER_BEDS = ATTACHMENT_TYPES.register("player_beds",
            () -> AttachmentType.builder(() -> new PlayerBeds()).serialize(PlayerBeds.CODEC).copyOnDeath().build());
}
