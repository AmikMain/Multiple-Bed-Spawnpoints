package net.amikmain.multiplebedspawnpoints;


import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


public class PlayerSpawnProvider implements net.minecraftforge.common.capabilities.ICapabilityProvider, INBTSerializable<CompoundTag> {


    public static final ResourceLocation IDENTIFIER = ResourceLocation.tryParse("multiplebedspawnpoints:spawn_data");

    public static final Capability<ISpawnData> SPAWN_DATA_CAP = CapabilityManager.get(new CapabilityToken<>(){});

    private final net.amikmain.multiplebedspawnpoints.SpawnData backend = new SpawnData();
    private final LazyOptional<ISpawnData> holder = LazyOptional.of(() -> backend);

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return SPAWN_DATA_CAP.orEmpty(cap, holder);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (SpawnPoint sp : backend.getSpawnPoints()) {
            CompoundTag e = new CompoundTag();
            BlockPos p = sp.getPos();
            e.putInt("x", p.getX());
            e.putInt("y", p.getY());
            e.putInt("z", p.getZ());
            // сохраняем идентификатор измерения как строку "namespace:path"
            e.putString("dimension", sp.getDimension().location().toString());
            list.add(e);
        }
        tag.put("spawnpoints", list);
        return tag;
    }

    // Десериализация из NBT
    @Override
    public void deserializeNBT(CompoundTag nbt) {
        backend.getSpawnPoints().clear();
        ListTag list = nbt.getList("spawnpoints", 10); // 10 = TAG_Compound
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            int x = e.getInt("x");
            int y = e.getInt("y");
            int z = e.getInt("z");
            String dimStr = e.getString("dimension");
            ResourceLocation dimLoc = ResourceLocation.tryParse(dimStr);
            if (dimLoc == null) continue;
            // восстанавливаем ResourceKey<Level> для измерения
            ResourceKey<Level> rk = ResourceKey.create(Registries.DIMENSION, dimLoc);
            backend.addSpawnPoint(new BlockPos(x, y, z), rk);
        }
    }
}