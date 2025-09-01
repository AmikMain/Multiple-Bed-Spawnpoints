package net.amikmain.multiplebedspawnpoints;

import net.amikmain.multiplebedspawnpoints.ISpawnData;
import net.amikmain.multiplebedspawnpoints.SpawnPoint;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class SpawnData implements ISpawnData {
    private final List<SpawnPoint> spawnPoints = new ArrayList<>();

    @Override
    public List<SpawnPoint> getSpawnPoints() {
        return spawnPoints;
    }

    @Override
    public void addSpawnPoint(BlockPos pos, ResourceKey<Level> dimension) {
        spawnPoints.add(new SpawnPoint(pos, dimension));
    }
}