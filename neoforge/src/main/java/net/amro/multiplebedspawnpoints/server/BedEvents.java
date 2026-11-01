package net.amro.multiplebedspawnpoints.server;

import net.amro.multiplebedspawnpoints.MultipleBedSpawnpoints;
import net.amro.multiplebedspawnpoints.compat.SubLevels;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.DimensionTransition;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent;

@EventBusSubscriber(modid = MultipleBedSpawnpoints.MODID)
public class BedEvents {
    // Fires once vanilla's own checks have passed: distance and obstruction for beds, charge for anchors.
    // Forced spawns (/spawnpoint) keep working as in vanilla.
    @SubscribeEvent
    static void onPlayerSetSpawn(PlayerSetSpawnEvent event) {
        BlockPos pos = event.getNewSpawn();
        if (pos == null || event.isForced() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (BedRespawnService.addSpawnBlock(player, event.getSpawnLevel(), pos)) {
            event.setCanceled(true);
        }
    }

    // Sable sets the vanilla spawn without posting PlayerSetSpawnEvent, see BedRespawnService.adoptVanillaSpawn.
    // Queued so that it runs once the block has handled the click.
    @SubscribeEvent
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && SubLevels.support().isInPlotGrid(player.level(), event.getPos())) {
            MinecraftServer server = player.server;
            server.tell(new TickTask(server.getTickCount(), () -> BedRespawnService.adoptVanillaSpawn(player)));
        }
    }

    @SubscribeEvent
    static void onPlayerRespawnPosition(PlayerRespawnPositionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DimensionTransition transition = BedRespawnService.usePendingRespawn(player);
            if (transition != null) {
                event.setDimensionTransition(transition);
            }
        }
    }

    @SubscribeEvent
    static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BedRespawnService.adoptVanillaSpawn(player);
        }
    }
}
