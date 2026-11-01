package net.amro.multiplebedspawnpoints.platform;

import net.amro.multiplebedspawnpoints.Config;
import net.amro.multiplebedspawnpoints.data.ModAttachments;
import net.amro.multiplebedspawnpoints.data.PlayerBeds;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;

public class NeoForgePlatform implements LoaderPlatform {
    @Override
    public int respawnCooldownSeconds() {
        return Config.RESPAWN_COOLDOWN_SECONDS.get();
    }

    @Override
    public int maxBeds() {
        return Config.MAX_BEDS.get();
    }

    @Override
    public PlayerBeds playerBeds(ServerPlayer player) {
        return player.getData(ModAttachments.PLAYER_BEDS);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }
}
