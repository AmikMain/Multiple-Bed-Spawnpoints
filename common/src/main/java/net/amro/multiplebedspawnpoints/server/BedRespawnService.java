package net.amro.multiplebedspawnpoints.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.amro.multiplebedspawnpoints.compat.SubLevelSupport;
import net.amro.multiplebedspawnpoints.compat.SubLevelSupport.SubLevelRespawn;
import net.amro.multiplebedspawnpoints.compat.SubLevelSupport.SubLevelState;
import net.amro.multiplebedspawnpoints.compat.SubLevels;
import net.amro.multiplebedspawnpoints.data.BedEntry;
import net.amro.multiplebedspawnpoints.data.PlayerBeds;
import net.amro.multiplebedspawnpoints.data.SubLevelLink;
import net.amro.multiplebedspawnpoints.network.BedListSyncPayload;
import net.amro.multiplebedspawnpoints.network.BedView;
import net.amro.multiplebedspawnpoints.platform.ModPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

public class BedRespawnService {
    private static final String BED_MISSING = "message.multiplebedspawnpoints.bed_missing";
    private static final String BED_ON_COOLDOWN = "message.multiplebedspawnpoints.bed_on_cooldown";
    private static final String BED_DESTROYED = "message.multiplebedspawnpoints.bed_destroyed";
    private static final String BED_OBSTRUCTED = "message.multiplebedspawnpoints.bed_obstructed";
    private static final String ANCHOR_NO_CHARGE = "message.multiplebedspawnpoints.anchor_no_charge";

    // Only filled while respawnAtBed runs on the server thread, which covers the whole of PlayerList.respawn.
    private static final Map<UUID, PendingRespawn> PENDING_RESPAWNS = new HashMap<>();

    /**
     * Adds the bed or respawn anchor at this position to the player's list.
     *
     * @return false if there is no bed or anchor there
     */
    public static boolean addSpawnBlock(ServerPlayer player, ResourceKey<Level> dimension, BlockPos pos) {
        ServerLevel level = player.server.getLevel(dimension);
        if (level == null) {
            return false;
        }

        // The plot of an unloaded sub-level has no blocks to read.
        boolean onSubLevel = SubLevels.support().isInPlotGrid(level, pos);
        Block block = onSubLevel && !SubLevels.support().isOnLoadedSubLevel(level, pos) ? null : level.getBlockState(pos).getBlock();
        boolean anchor = block instanceof RespawnAnchorBlock;
        if (!anchor && !(block instanceof BedBlock)) {
            return false;
        }

        addBed(player, level, pos, anchor, onSubLevel);
        return true;
    }

    /**
     * Moves the player's vanilla spawn into the list. This covers spawns set before the mod handled them, and spawns
     * on sub-levels: Sable's mixin returns from setRespawnPosition before PlayerSetSpawnEvent is posted, so a click
     * on a sub-level bed or anchor only shows up as a changed vanilla spawn.
     */
    public static void adoptVanillaSpawn(ServerPlayer player) {
        BlockPos pos = player.getRespawnPosition();
        if (pos != null && !player.isRespawnForced() && addSpawnBlock(player, player.getRespawnDimension(), pos)) {
            player.setRespawnPosition(Level.OVERWORLD, null, 0.0F, false, false);
        }
    }

    private static void addBed(ServerPlayer player, ServerLevel level, BlockPos pos, boolean anchor, boolean onSubLevel) {
        GlobalPos location = GlobalPos.of(level.dimension(), pos);
        PlayerBeds beds = beds(player);
        removeDestroyed(player, beds);
        if (beds.contains(location)) {
            return;
        }

        int maxBeds = ModPlatform.get().maxBeds();
        Component message;
        if (beds.size() >= maxBeds) {
            message = Component.translatable("message.multiplebedspawnpoints.bed_limit", maxBeds);
        } else {
            Optional<SubLevelLink> link = onSubLevel ? SubLevels.support().createLink(level, pos) : Optional.empty();
            if (onSubLevel && link.isEmpty()) {
                return;
            }
            beds.add(new BedEntry(UUID.randomUUID(), "", location, anchor, link));
            String key = anchor ? "message.multiplebedspawnpoints.anchor_added" : "message.multiplebedspawnpoints.bed_added";
            message = Component.translatable(key, beds.size(), maxBeds);
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
        removeDestroyed(player, beds);

        List<BedView> views = new ArrayList<>();
        for (BedEntry bed : beds.beds()) {
            ServerLevel level = player.server.getLevel(bed.location().dimension());
            boolean checkable = canCheck(level, bed);
            boolean obstructed = checkable && findStandUpPosition(level, bed).isEmpty();
            int remaining = (int) beds.remainingCooldown(bed.location(), gameTime, cooldownTicks);
            views.add(new BedView(bed.id(), bed.name(), placeOf(bed), obstructed, remaining, chargeOf(level, bed, checkable)));
        }

        ModPlatform.get().sendToPlayer(player, new BedListSyncPayload(views, ModPlatform.get().maxBeds(), messageKey));
    }

    private static BedView.Place placeOf(BedEntry bed) {
        return bed.subLevel()
                .map(link -> new BedView.Place(GlobalPos.of(bed.location().dimension(), link.worldPos()), link.name()))
                .orElseGet(() -> new BedView.Place(bed.location(), ""));
    }

    private static int chargeOf(ServerLevel level, BedEntry bed, boolean checkable) {
        if (!bed.anchor()) {
            return BedView.NOT_AN_ANCHOR;
        }
        return checkable ? anchorCharge(level, bed.location().pos()) : BedView.CHARGE_UNKNOWN;
    }

    public static void renameBed(ServerPlayer player, UUID id, String name) {
        String filtered = StringUtil.filterText(name).strip();
        beds(player).rename(id, filtered.substring(0, Math.min(filtered.length(), BedEntry.MAX_NAME_LENGTH)));
        sendBedList(player, "");
    }

    public static void deleteBed(ServerPlayer player, UUID id) {
        PlayerBeds beds = beds(player);
        beds.find(id).ifPresent(bed -> discard(player, beds, bed));
        sendBedList(player, "");
    }

    public static void respawnAtBed(ServerPlayer player, UUID id) {
        if (player.getHealth() > 0.0F || player.server.isHardcore()) {
            return;
        }

        PlayerBeds beds = beds(player);
        Optional<BedEntry> found = beds.find(id);
        if (found.isEmpty()) {
            sendBedList(player, BED_MISSING);
            return;
        }

        if (beds.remainingCooldown(found.get().location(), gameTime(player), cooldownTicks()) > 0) {
            sendBedList(player, BED_ON_COOLDOWN);
            return;
        }

        ServerLevel level = player.server.getLevel(found.get().location().dimension());
        BedEntry bed = level == null ? null : refreshSubLevel(level, beds, found.get());
        if (bed == null) {
            beds.remove(id);
            sendBedList(player, BED_DESTROYED);
            return;
        }

        // An unloaded sub-level has no blocks to check, so the bed is checked once it has loaded again.
        boolean checkable = bed.subLevel().isEmpty() || canCheck(level, bed);
        if (checkable && !isSpawnBlock(level, bed)) {
            discard(player, beds, bed);
            sendBedList(player, BED_DESTROYED);
            return;
        }

        if (checkable && bed.anchor() && anchorCharge(level, bed.location().pos()) <= 0) {
            sendBedList(player, ANCHOR_NO_CHARGE);
            return;
        }

        Optional<Vec3> standUp = checkable ? findStandUpPosition(level, bed) : Optional.empty();
        if (checkable && standUp.isEmpty()) {
            sendBedList(player, BED_OBSTRUCTED);
            return;
        }

        Optional<SubLevelRespawn> subLevelRespawn = bed.subLevel()
                .flatMap(link -> SubLevels.support().findRespawn(level, link, standUp.orElse(null)));
        if (bed.subLevel().isPresent() && subLevelRespawn.isEmpty()) {
            discard(player, beds, bed);
            sendBedList(player, BED_DESTROYED);
            return;
        }

        BlockPos pos = bed.location().pos();
        Vec3 position = subLevelRespawn.map(SubLevelRespawn::position).orElseGet(standUp::orElseThrow);
        float yaw = subLevelRespawn.isPresent() ? 0.0F : yawTowards(position, pos);

        // Going through the vanilla packet handler keeps everything else about respawning as in vanilla.
        PENDING_RESPAWNS.put(player.getUUID(), new PendingRespawn(level, bed.location(), position, yaw));
        try {
            player.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
        } finally {
            PENDING_RESPAWNS.remove(player.getUUID());
        }

        // Respawning replaces the ServerPlayer, the connection already points at the new one.
        subLevelRespawn.ifPresent(respawn -> SubLevels.support().attachPlayer(player.connection.player, respawn));

        // Vanilla only spends the charge of the player's vanilla spawn point, which listed anchors never are.
        if (bed.anchor() && checkable) {
            spendAnchorCharge(level, pos);
        }
    }

    /**
     * Called from inside PlayerList.respawn, while the player being respawned is still the dead one.
     *
     * @return where to put the player if they picked a bed, or null for an ordinary respawn
     */
    @Nullable
    public static DimensionTransition usePendingRespawn(ServerPlayer player) {
        PendingRespawn pending = PENDING_RESPAWNS.get(player.getUUID());
        if (pending == null) {
            return null;
        }

        // Written to the dead player: the loader copies this data over to the new one.
        beds(player).markUsed(pending.location(), gameTime(player));
        return new DimensionTransition(pending.level(), pending.position(), Vec3.ZERO, pending.yaw(), 0.0F, DimensionTransition.DO_NOTHING);
    }

    private static PlayerBeds beds(ServerPlayer player) {
        return ModPlatform.get().playerBeds(player);
    }

    private static long gameTime(ServerPlayer player) {
        return player.server.overworld().getGameTime();
    }

    private static long cooldownTicks() {
        return ModPlatform.get().respawnCooldownSeconds() * 20L;
    }

    private static void removeDestroyed(ServerPlayer player, PlayerBeds beds) {
        for (BedEntry bed : beds.beds()) {
            ServerLevel level = player.server.getLevel(bed.location().dimension());
            if (level == null) {
                beds.remove(bed.id());
                continue;
            }

            BedEntry current = refreshSubLevel(level, beds, bed);
            if (current != null && canCheck(level, current) && !isSpawnBlock(level, current)) {
                discard(player, beds, current);
            }
        }
    }

    /**
     * Brings a sub-level entry up to date with its sub-level.
     *
     * @return the updated entry, or null if it was removed from the list
     */
    @Nullable
    private static BedEntry refreshSubLevel(ServerLevel level, PlayerBeds beds, BedEntry bed) {
        if (bed.subLevel().isEmpty()) {
            return bed;
        }

        SubLevelSupport support = SubLevels.support();
        SubLevelLink link = bed.subLevel().get();
        ResourceKey<Level> dimension = bed.location().dimension();
        BedEntry updated = switch (support.getState(level, link)) {
            case SubLevelState.Gone gone -> {
                support.removeLink(level, link);
                yield null;
            }
            case SubLevelState.Unloaded unloaded -> bed;
            case SubLevelState.Disassembled disassembled -> {
                support.removeLink(level, link);
                yield bed.withPlace(GlobalPos.of(dimension, disassembled.pos()), Optional.empty());
            }
            case SubLevelState.Loaded loaded -> bed.withPlace(GlobalPos.of(dimension, loaded.plotPos()),
                    Optional.of(link.withLastKnown(loaded.name(), loaded.worldPos())));
        };

        if (updated == null) {
            beds.remove(bed.id());
        } else {
            beds.replace(updated);
        }
        return updated;
    }

    private static void discard(ServerPlayer player, PlayerBeds beds, BedEntry bed) {
        beds.remove(bed.id());
        ServerLevel level = player.server.getLevel(bed.location().dimension());
        if (level != null) {
            bed.subLevel().ifPresent(link -> SubLevels.support().removeLink(level, link));
        }
    }

    // Beds in unloaded chunks are not loaded just to draw a list, respawnAtBed checks those when one is picked.
    // Anchors are, since the list has to show their charge and the player needs it to decide.
    private static boolean canCheck(ServerLevel level, BedEntry bed) {
        if (bed.subLevel().isPresent()) {
            return SubLevels.support().getState(level, bed.subLevel().get()) instanceof SubLevelState.Loaded;
        }
        return bed.anchor() || level.isLoaded(bed.location().pos());
    }

    private static boolean isSpawnBlock(ServerLevel level, BedEntry bed) {
        Block block = level.getBlockState(bed.location().pos()).getBlock();
        return bed.anchor() ? block instanceof RespawnAnchorBlock : block instanceof BedBlock;
    }

    private static int anchorCharge(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getValue(RespawnAnchorBlock.CHARGE);
    }

    private static void spendAnchorCharge(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        level.setBlock(pos, state.setValue(RespawnAnchorBlock.CHARGE, state.getValue(RespawnAnchorBlock.CHARGE) - 1), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private static Optional<Vec3> findStandUpPosition(ServerLevel level, BedEntry bed) {
        BlockPos pos = bed.location().pos();
        if (bed.anchor()) {
            return RespawnAnchorBlock.findStandUpPosition(EntityType.PLAYER, level, pos);
        }
        BlockState state = level.getBlockState(pos);
        return BedBlock.findStandUpPosition(EntityType.PLAYER, level, pos, state.getValue(BedBlock.FACING), 0.0F);
    }

    // Same calculation as vanilla's ServerPlayer.RespawnPosAngle: the player wakes up facing the bed.
    private static float yawTowards(Vec3 position, BlockPos bed) {
        Vec3 direction = Vec3.atBottomCenterOf(bed).subtract(position).normalize();
        return (float) Mth.wrapDegrees(Mth.atan2(direction.z, direction.x) * 180.0F / (float) Math.PI - 90.0);
    }

    private record PendingRespawn(ServerLevel level, GlobalPos location, Vec3 position, float yaw) {
    }
}
