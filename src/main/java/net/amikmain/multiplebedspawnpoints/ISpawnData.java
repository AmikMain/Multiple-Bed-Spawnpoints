package net.amikmain.multiplebedspawnpoints;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;

public interface ISpawnData {
    List<SpawnPoint> getSpawnPoints();
    void addSpawnPoint(BlockPos pos, ResourceKey<Level> dimension);
}