package net.amro.multiplebedspawnpoints.data;

import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;

/**
 * Ties a list entry to a Sable sub-level, which can move or unload while the player is away.
 *
 * @param trackingPoint Sable tracking point that finds the sub-level again
 * @param name          last known sub-level name, empty if it has none
 * @param worldPos      last known position in the world
 */
public record SubLevelLink(UUID trackingPoint, String name, BlockPos worldPos) {
    public static final Codec<SubLevelLink> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("tracking_point").forGetter(SubLevelLink::trackingPoint),
            Codec.STRING.fieldOf("name").forGetter(SubLevelLink::name),
            BlockPos.CODEC.fieldOf("world_pos").forGetter(SubLevelLink::worldPos)
    ).apply(instance, SubLevelLink::new));

    public SubLevelLink withLastKnown(String newName, BlockPos newWorldPos) {
        return new SubLevelLink(trackingPoint, newName, newWorldPos);
    }
}
