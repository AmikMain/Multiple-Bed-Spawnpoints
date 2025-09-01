package net.amikmain.multiplebedspawnpoints;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class SpawnPoint {
    private final BlockPos pos;
    private final ResourceKey<Level> dimension;

    public SpawnPoint(BlockPos pos, ResourceKey<Level> dimension) {
        this.pos = pos;
        this.dimension = dimension;
    }

    public BlockPos getPos() {
        return pos;
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }
}