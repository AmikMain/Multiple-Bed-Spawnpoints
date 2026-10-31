package net.amro.multiplebedspawnpoints.data;

import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;

/**
 * A bed in a player's respawn list. An empty name means the player has not renamed it,
 * and the client shows a translated default instead.
 */
public record BedEntry(UUID id, String name, GlobalPos location) {
    public static final int MAX_NAME_LENGTH = 32;

    public static final Codec<BedEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(BedEntry::id),
            Codec.STRING.fieldOf("name").forGetter(BedEntry::name),
            GlobalPos.CODEC.fieldOf("location").forGetter(BedEntry::location)
    ).apply(instance, BedEntry::new));

    public BedEntry withName(String newName) {
        return new BedEntry(id, newName, location);
    }
}
