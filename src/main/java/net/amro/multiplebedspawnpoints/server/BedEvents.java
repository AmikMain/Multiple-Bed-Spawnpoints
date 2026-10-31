package net.amro.multiplebedspawnpoints.server;

import net.amro.multiplebedspawnpoints.MultipleBedSpawnpoints;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.BedBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent;

@EventBusSubscriber(modid = MultipleBedSpawnpoints.MODID)
public class BedEvents {
    // Fires from ServerPlayer.startSleepInBed once vanilla's distance and obstruction checks have passed.
    // Forced spawns (/spawnpoint) and respawn anchors keep working as in vanilla.
    @SubscribeEvent
    static void onPlayerSetSpawn(PlayerSetSpawnEvent event) {
        BlockPos pos = event.getNewSpawn();
        if (pos == null || event.isForced() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ServerLevel level = player.server.getLevel(event.getSpawnLevel());
        if (level != null && level.getBlockState(pos).getBlock() instanceof BedBlock) {
            event.setCanceled(true);
            BedRespawnService.addBed(player, GlobalPos.of(event.getSpawnLevel(), pos));
        }
    }

    @SubscribeEvent
    static void onPlayerRespawnPosition(PlayerRespawnPositionEvent event) {
        BedRespawnService.applyPendingRespawn(event);
    }
}
