package net.amro.multiplebedspawnpoints.compat;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import org.joml.Vector3d;
import org.joml.Vector3dc;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.mixinterface.player_freezing.PlayerFreezeExtension;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundFreezePlayerPacket;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunkMap;
import dev.ryanhcode.sable.sublevel.tracking_points.SubLevelTrackingPointSavedData;
import dev.ryanhcode.sable.sublevel.tracking_points.TrackingPoint;
import net.amro.multiplebedspawnpoints.data.SubLevelLink;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Only loaded when Sable is installed. Tracking points and player freezing are Sable internals rather than
 * part of its companion API, so {@link #verify()} checks them up front.
 */
public final class SableSupport implements SubLevelSupport {
    // Sable centres a respawning player on the tracking point, which for an anchor is inside a full block.
    private static final double UNLOADED_STAND_UP_HEIGHT = 1.0;

    public static void verify() throws ReflectiveOperationException {
        SubLevelTrackingPointSavedData.class.getMethod("generateTrackingPoint", Vec3.class, ServerSubLevel.class);
        SubLevelTrackingPointSavedData.class.getMethod("take", UUID.class, boolean.class);
        SubLevelTrackingPointSavedData.class.getMethod("getTrackingPoint", UUID.class);
        SubLevelTrackingPointSavedData.class.getMethod("removeTrackingPoint", UUID.class);
        ServerSubLevelContainer.class.getMethod("getHoldingChunkMap");
        SubLevelHoldingChunkMap.class.getMethod("getHoldingSubLevel", UUID.class);
        PlayerFreezeExtension.class.getMethod("sable$freezeTo", UUID.class, Vector3dc.class);
        ClientboundFreezePlayerPacket.class.getConstructor(UUID.class, Vector3dc.class);
    }

    @Override
    public boolean isInPlotGrid(Level level, BlockPos pos) {
        return SableCompanion.INSTANCE.isInPlotGrid(level, pos);
    }

    @Override
    public boolean isOnLoadedSubLevel(Level level, BlockPos pos) {
        return Sable.HELPER.getContaining(level, pos) != null;
    }

    @Override
    public Optional<SubLevelLink> createLink(ServerLevel level, BlockPos pos) {
        if (!(Sable.HELPER.getContaining(level, pos) instanceof ServerSubLevel subLevel)) {
            return Optional.empty();
        }

        UUID trackingPoint = trackingPoints(level).generateTrackingPoint(Vec3.atCenterOf(pos), subLevel);
        if (trackingPoint == null) {
            return Optional.empty();
        }
        return Optional.of(new SubLevelLink(trackingPoint, nameOf(subLevel), worldPos(subLevel, pos)));
    }

    @Override
    public void removeLink(ServerLevel level, SubLevelLink link) {
        trackingPoints(level).removeTrackingPoint(link.trackingPoint());
    }

    @Override
    public SubLevelState getState(ServerLevel level, SubLevelLink link) {
        TrackingPoint point = trackingPoints(level).getTrackingPoint(link.trackingPoint());
        if (point == null) {
            return new SubLevelState.Gone();
        }

        BlockPos pos = BlockPos.containing(point.point().x(), point.point().y(), point.point().z());
        if (!point.inSubLevel()) {
            return new SubLevelState.Disassembled(pos);
        }

        SubLevel subLevel = Sable.HELPER.getContaining(level, point.point());
        if (subLevel != null) {
            return new SubLevelState.Loaded(pos, nameOf(subLevel), worldPos(subLevel, pos));
        }
        return isStored(level, point) ? new SubLevelState.Unloaded() : new SubLevelState.Gone();
    }

    // Unloading saves a sub-level, and saving gives its tracking points a pointer. A sub-level that was removed
    // before its first save leaves points behind that Sable never updates, still marked as in a sub-level.
    private static boolean isStored(ServerLevel level, TrackingPoint point) {
        if (point.lastSavedSubLevelPointer() != null) {
            return true;
        }
        return point.subLevelID() != null
                && ServerSubLevelContainer.getContainer(level).getHoldingChunkMap().getHoldingSubLevel(point.subLevelID()) != null;
    }

    @Override
    public Optional<SubLevelRespawn> findRespawn(ServerLevel level, SubLevelLink link, @Nullable Vec3 localStandUp) {
        SubLevelTrackingPointSavedData trackingPoints = trackingPoints(level);
        TrackingPoint point = trackingPoints.getTrackingPoint(link.trackingPoint());
        SubLevel subLevel = point == null ? null : Sable.HELPER.getContaining(level, point.point());

        if (subLevel != null && localStandUp != null) {
            Vector3d anchor = JOMLConversion.toJOML(localStandUp);
            Vector3d world = subLevel.logicalPose().transformPosition(new Vector3d(anchor));
            return Optional.of(new SubLevelRespawn(JOMLConversion.toMojang(world), subLevel.getUniqueId(), anchor));
        }

        // Not removing the point: unlike Sable's single respawn point, list entries are used again and again.
        SubLevelTrackingPointSavedData.TakenLoginPoint taken = trackingPoints.take(link.trackingPoint(), false);
        // Without a sub-level id, take() fell back to the placeholder it keeps for logging in: where the bed once was.
        if (taken == null || taken.subLevelId() == null) {
            return Optional.empty();
        }

        Vec3 position = JOMLConversion.toMojang(taken.position()).add(0.0, UNLOADED_STAND_UP_HEIGHT, 0.0);
        Vector3d anchor = taken.localAnchor() == null ? null : new Vector3d(taken.localAnchor()).add(0.0, UNLOADED_STAND_UP_HEIGHT, 0.0);
        return Optional.of(new SubLevelRespawn(position, taken.subLevelId(), anchor));
    }

    @Override
    public void attachPlayer(ServerPlayer player, SubLevelRespawn respawn) {
        if (respawn.subLevelId() != null && respawn.localAnchor() != null) {
            ((PlayerFreezeExtension) player).sable$freezeTo(respawn.subLevelId(), respawn.localAnchor());
            player.connection.send(new ClientboundCustomPayloadPacket(new ClientboundFreezePlayerPacket(respawn.subLevelId(), respawn.localAnchor())));
        }
    }

    private static SubLevelTrackingPointSavedData trackingPoints(ServerLevel level) {
        return SubLevelTrackingPointSavedData.getOrLoad(level);
    }

    private static String nameOf(SubLevel subLevel) {
        String name = subLevel.getName();
        return name == null ? "" : name;
    }

    private static BlockPos worldPos(SubLevel subLevel, BlockPos plotPos) {
        Vector3d world = subLevel.logicalPose().transformPosition(JOMLConversion.toJOML(Vec3.atCenterOf(plotPos)));
        return BlockPos.containing(world.x(), world.y(), world.z());
    }
}
