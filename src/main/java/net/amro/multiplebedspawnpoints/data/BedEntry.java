package net.amro.multiplebedspawnpoints.data;

import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;

/**
 * A bed or respawn anchor in a player's respawn list. An empty name means the player has not renamed it,
 * and the client shows a translated default instead.
 *
 * @param location where the block is. On a sub-level this is a plot position, far from the sub-level itself
 * @param subLevel set when the block is on a Sable sub-level
 */
public record BedEntry(UUID id, String name, GlobalPos location, boolean anchor, Optional<SubLevelLink> subLevel) {
    public static final int MAX_NAME_LENGTH = 32;

    public static final Codec<BedEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(BedEntry::id),
            Codec.STRING.fieldOf("name").forGetter(BedEntry::name),
            GlobalPos.CODEC.fieldOf("location").forGetter(BedEntry::location),
            // Optional because entries saved by earlier versions do not have these fields.
            Codec.BOOL.optionalFieldOf("anchor", false).forGetter(BedEntry::anchor),
            SubLevelLink.CODEC.optionalFieldOf("sub_level").forGetter(BedEntry::subLevel)
    ).apply(instance, BedEntry::new));

    public BedEntry withName(String newName) {
        return new BedEntry(id, newName, location, anchor, subLevel);
    }

    public BedEntry withPlace(GlobalPos newLocation, Optional<SubLevelLink> newSubLevel) {
        return new BedEntry(id, name, newLocation, anchor, newSubLevel);
    }
}
