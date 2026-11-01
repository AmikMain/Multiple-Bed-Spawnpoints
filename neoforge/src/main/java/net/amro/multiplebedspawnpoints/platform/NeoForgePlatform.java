package net.amro.multiplebedspawnpoints.platform;

import net.amro.multiplebedspawnpoints.Config;
import net.amro.multiplebedspawnpoints.ModInfo;
import net.amro.multiplebedspawnpoints.compat.SableSupport;
import net.amro.multiplebedspawnpoints.compat.SubLevelSupport;
import net.amro.multiplebedspawnpoints.data.ModAttachments;
import net.amro.multiplebedspawnpoints.data.PlayerBeds;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;

public class NeoForgePlatform implements LoaderPlatform {
    private static final String SABLE_MOD_ID = "sable";

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

    // SableSupport stays unloaded unless Sable is installed.
    @Override
    public SubLevelSupport createSubLevelSupport() {
        if (!ModList.get().isLoaded(SABLE_MOD_ID)) {
            return SubLevelSupport.NONE;
        }

        try {
            SableSupport.verify();
            ModInfo.LOGGER.info("Sable found, beds and respawn anchors on sub-levels are supported");
            return new SableSupport();
        } catch (ReflectiveOperationException | LinkageError e) {
            ModInfo.LOGGER.warn("This Sable version is not supported, beds on sub-levels will not work", e);
            return SubLevelSupport.NONE;
        }
    }
}
