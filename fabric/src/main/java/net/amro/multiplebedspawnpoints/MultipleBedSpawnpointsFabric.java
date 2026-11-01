package net.amro.multiplebedspawnpoints;

import net.amro.multiplebedspawnpoints.data.FabricAttachments;
import net.amro.multiplebedspawnpoints.platform.FabricPlatform;
import net.amro.multiplebedspawnpoints.platform.ModPlatform;
import net.amro.multiplebedspawnpoints.server.BedRespawnService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class MultipleBedSpawnpointsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ModPlatform.set(new FabricPlatform());
        FabricAttachments.register();

        FabricConfig.load();
        // Picks up edits to the file each time a world or server starts.
        ServerLifecycleEvents.SERVER_STARTING.register(server -> FabricConfig.load());

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> BedRespawnService.adoptVanillaSpawn(handler.player));
    }
}
