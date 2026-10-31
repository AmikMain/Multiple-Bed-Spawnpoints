package net.amro.multiplebedspawnpoints.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.amro.multiplebedspawnpoints.Config;
import net.amro.multiplebedspawnpoints.data.BedEntry;
import net.amro.multiplebedspawnpoints.data.ModAttachments;
import net.amro.multiplebedspawnpoints.data.PlayerBeds;
import net.amro.multiplebedspawnpoints.network.BedListSyncPayload;
import net.amro.multiplebedspawnpoints.network.BedView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public class BedRespawnService {
    private static final String BED_MISSING = "message.multiplebedspawnpoints.bed_missing";
    private static final String BED_ON_COOLDOWN = "message.multiplebedspawnpoints.bed_on_cooldown";
    private static final String BED_DESTROYED = "message.multiplebedspawnpoints.bed_destroyed";
    private static final String BED_OBSTRUCTED = "message.multiplebedspawnpoints.bed_obstructed";

    // Only filled while respawnAtBed runs on the server thread, which covers the whole of PlayerList.respawn.
    private static final Map<UUID, PendingRespawn> PENDING_RESPAWNS = new HashMap<>();

    public static void addBed(ServerPlayer player, GlobalPos location) {
        PlayerBeds beds = beds(player);
        if (beds.contains(location)) {
            return;
        }

        int maxBeds = Config.MAX_BEDS.get();
        Component message;
        if (beds.size() >= maxBeds) {
            message = Component.translatable("message.multiplebedspawnpoints.bed_limit", maxBeds);
        } else {
            beds.add(new BedEntry(UUID.randomUUID(), "", location));
            message = Component.translatable("message.multiplebedspawnpoints.bed_added", beds.size(), maxBeds);
        }

        // BedBlock shows its own overlay message right after this, for example that you can only sleep at night.
        // tell() always queues, so ours is sent after the click is handled and stays on screen.
        MinecraftServer server = player.server;
        server.tell(new TickTask(server.getTickCount(), () -> player.displayClientMessage(message, true)));
    }

    public static void sendBedList(ServerPlayer player, String messageKey) {
        PlayerBeds beds = beds(player);
        long gameTime = gameTime(player);
        long cooldownTicks = cooldownTicks();
        beds.forgetExpiredUses(gameTime, cooldownTicks);

        List<BedView> views = new ArrayList<>();
        for (BedEntry bed : beds.beds()) {
            GlobalPos location = bed.location();
            ServerLevel level = player.server.getLevel(location.dimension());
            // Unloaded chunks are not loaded just to draw a list. respawnAtBed checks those beds when one is picked.
            boolean loaded = level != null && level.isLoaded(location.pos());
            if (level == null || (loaded && !isBed(level, location.pos()))) {
                beds.remove(bed.id());
                continue;
            }

            boolean obstructed = loaded && findStandUpPosition(level, location.pos()).isEmpty();
            int remaining = (int) beds.remainingCooldown(location, gameTime, cooldownTicks);
            views.add(new BedView(bed.id(), bed.name(), location, obstructed, remaining));
        }

        PacketDistributor.sendToPlayer(player, new BedListSyncPayload(views, Config.MAX_BEDS.get(), messageKey));
    }

    public static void renameBed(ServerPlayer player, UUID id, String name) {
        String filtered = StringUtil.filterText(name).strip();
        beds(player).rename(id, filtered.substring(0, Math.min(filtered.length(), BedEntry.MAX_NAME_LENGTH)));
        sendBedList(player, "");
    }

    public static void deleteBed(ServerPlayer player, UUID id) {
        beds(player).remove(id);
        sendBedList(player, "");
    }

    public static void respawnAtBed(ServerPlayer player, UUID id) {
        if (player.getHealth() > 0.0F || player.server.isHardcore()) {
            return;
        }

        PlayerBeds beds = beds(player);
        Optional<BedEntry> bed = beds.find(id);
        if (bed.isEmpty()) {
            sendBedList(player, BED_MISSING);
            return;
        }

        GlobalPos location = bed.get().location();
        if (beds.remainingCooldown(location, gameTime(player), cooldownTicks()) > 0) {
            sendBedList(player, BED_ON_COOLDOWN);
            return;
        }

        ServerLevel level = player.server.getLevel(location.dimension());
        if (level == null || !isBed(level, location.pos())) {
            beds.remove(id);
            sendBedList(player, BED_DESTROYED);
            return;
        }

        Optional<Vec3> position = findStandUpPosition(level, location.pos());
        if (position.isEmpty()) {
            sendBedList(player, BED_OBSTRUCTED);
            return;
        }

        // Going through the vanilla packet handler keeps everything else about respawning as in vanilla.
        PENDING_RESPAWNS.put(player.getUUID(), new PendingRespawn(level, location, position.get()));
        try {
            player.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
        } finally {
            PENDING_RESPAWNS.remove(player.getUUID());
        }
    }

    public static void applyPendingRespawn(PlayerRespawnPositionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        PendingRespawn pending = PENDING_RESPAWNS.get(player.getUUID());
        if (pending != null) {
            float yaw = yawTowards(pending.position(), pending.location().pos());
            event.setDimensionTransition(new DimensionTransition(
                    pending.level(), pending.position(), Vec3.ZERO, yaw, 0.0F, DimensionTransition.DO_NOTHING));
            // This is still the dead player: copyOnDeath carries the data over to the new one.
            beds(player).markUsed(pending.location(), gameTime(player));
        }
    }

    private static PlayerBeds beds(ServerPlayer player) {
        return player.getData(ModAttachments.PLAYER_BEDS);
    }

    private static long gameTime(ServerPlayer player) {
        return player.server.overworld().getGameTime();
    }

    private static long cooldownTicks() {
        return Config.RESPAWN_COOLDOWN_SECONDS.get() * 20L;
    }

    private static boolean isBed(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof BedBlock;
    }

    private static Optional<Vec3> findStandUpPosition(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return BedBlock.findStandUpPosition(EntityType.PLAYER, level, pos, state.getValue(BedBlock.FACING), 0.0F);
    }

    // Same calculation as vanilla's ServerPlayer.RespawnPosAngle: the player wakes up facing the bed.
    private static float yawTowards(Vec3 position, BlockPos bed) {
        Vec3 direction = Vec3.atBottomCenterOf(bed).subtract(position).normalize();
        return (float) Mth.wrapDegrees(Mth.atan2(direction.z, direction.x) * 180.0F / (float) Math.PI - 90.0);
    }

    private record PendingRespawn(ServerLevel level, GlobalPos location, Vec3 position) {
    }
}
