package net.amro.multiplebedspawnpoints.platform;

import net.amro.multiplebedspawnpoints.data.PlayerBeds;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** Everything the common code needs from the mod loader. Each loader provides one implementation. */
public interface LoaderPlatform {
    int respawnCooldownSeconds();

    int maxBeds();

    /** The player's respawn data, which has to survive death and be saved with the player. */
    PlayerBeds playerBeds(ServerPlayer player);

    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

    void sendToServer(CustomPacketPayload payload);

    boolean isModLoaded(String modId);
}
