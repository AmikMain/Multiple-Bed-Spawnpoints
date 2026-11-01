package net.amro.multiplebedspawnpoints.platform;

import net.amro.multiplebedspawnpoints.FabricConfig;
import net.amro.multiplebedspawnpoints.data.FabricAttachments;
import net.amro.multiplebedspawnpoints.data.PlayerBeds;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public class FabricPlatform implements LoaderPlatform {
    @Override
    public int respawnCooldownSeconds() {
        return FabricConfig.respawnCooldownSeconds();
    }

    @Override
    public int maxBeds() {
        return FabricConfig.maxBeds();
    }

    @Override
    public PlayerBeds playerBeds(ServerPlayer player) {
        return player.getAttachedOrCreate(FabricAttachments.PLAYER_BEDS);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    // Only ever called from client code, so ClientPlayNetworking is not loaded on a dedicated server.
    @Override
    public void sendToServer(CustomPacketPayload payload) {
        if (ClientPlayNetworking.canSend(payload.type())) {
            ClientPlayNetworking.send(payload);
        }
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }
}
