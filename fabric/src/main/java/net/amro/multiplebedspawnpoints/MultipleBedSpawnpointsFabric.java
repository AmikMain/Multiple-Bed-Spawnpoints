package net.amro.multiplebedspawnpoints;

import net.amro.multiplebedspawnpoints.compat.SubLevels;
import net.amro.multiplebedspawnpoints.data.FabricAttachments;
import net.amro.multiplebedspawnpoints.network.FabricPayloads;
import net.amro.multiplebedspawnpoints.platform.FabricPlatform;
import net.amro.multiplebedspawnpoints.platform.ModPlatform;
import net.amro.multiplebedspawnpoints.server.BedRespawnService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

public class MultipleBedSpawnpointsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ModPlatform.set(new FabricPlatform());
        FabricAttachments.register();
        FabricPayloads.register();

        FabricConfig.load();
        // Picks up edits to the file each time a world or server starts.
        ServerLifecycleEvents.SERVER_STARTING.register(server -> FabricConfig.load());

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> BedRespawnService.adoptVanillaSpawn(handler.player));

        // Sable can cancel setRespawnPosition before ServerPlayerMixin runs, see BedRespawnService.adoptVanillaSpawn.
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (player instanceof ServerPlayer serverPlayer) {
                BedRespawnService.onBlockUsed(serverPlayer, hitResult.getBlockPos());
            }
            return InteractionResult.PASS;
        });

        // Checks an installed Sable at startup, so an unsupported version shows up in the log right away.
        SubLevels.support();
    }
}
