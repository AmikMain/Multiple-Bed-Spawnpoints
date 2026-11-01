package net.amro.multiplebedspawnpoints.compat;

import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import org.joml.Vector3d;

import net.amro.multiplebedspawnpoints.data.SubLevelLink;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Everything the mod needs from Sable. Nothing outside {@link SableSupport} may touch Sable classes. */
public interface SubLevelSupport {
    /** Used when Sable is not installed. Links saved in a world that had Sable stay untouched. */
    SubLevelSupport NONE = new SubLevelSupport() {
        @Override
        public boolean isInPlotGrid(Level level, BlockPos pos) {
            return false;
        }

        @Override
        public boolean isOnLoadedSubLevel(Level level, BlockPos pos) {
            return false;
        }

        @Override
        public Optional<SubLevelLink> createLink(ServerLevel level, BlockPos pos) {
            return Optional.empty();
        }

        @Override
        public void removeLink(ServerLevel level, SubLevelLink link) {
        }

        @Override
        public SubLevelState getState(ServerLevel level, SubLevelLink link) {
            return new SubLevelState.Unloaded();
        }

        @Override
        public Optional<SubLevelRespawn> findRespawn(ServerLevel level, SubLevelLink link, @Nullable Vec3 localStandUp) {
            return Optional.empty();
        }

        @Override
        public void attachPlayer(ServerPlayer player, SubLevelRespawn respawn) {
        }
    };

    /** Whether the position lies in the area where sub-level blocks are stored, loaded or not. */
    boolean isInPlotGrid(Level level, BlockPos pos);

    boolean isOnLoadedSubLevel(Level level, BlockPos pos);

    /** @return empty if no loaded sub-level contains the position */
    Optional<SubLevelLink> createLink(ServerLevel level, BlockPos pos);

    void removeLink(ServerLevel level, SubLevelLink link);

    SubLevelState getState(ServerLevel level, SubLevelLink link);

    /**
     * @param localStandUp where to stand in plot coordinates, or null if the sub-level is not loaded and the
     *                     blocks around the bed cannot be read
     * @return empty if the sub-level no longer exists
     */
    Optional<SubLevelRespawn> findRespawn(ServerLevel level, SubLevelLink link, @Nullable Vec3 localStandUp);

    /** Keeps a freshly respawned player on the sub-level instead of leaving them where it was. */
    void attachPlayer(ServerPlayer player, SubLevelRespawn respawn);

    sealed interface SubLevelState {
        /** Sable no longer knows this tracking point. */
        record Gone() implements SubLevelState {
        }

        /** The sub-level was removed, and its blocks may now be part of the world at this position. */
        record Disassembled(BlockPos pos) implements SubLevelState {
        }

        record Unloaded() implements SubLevelState {
        }

        record Loaded(BlockPos plotPos, String name, BlockPos worldPos) implements SubLevelState {
        }
    }

    /**
     * @param subLevelId  the sub-level to attach the player to, or null to leave them at the position
     * @param localAnchor where on the sub-level to attach them, in plot coordinates
     */
    record SubLevelRespawn(Vec3 position, @Nullable UUID subLevelId, @Nullable Vector3d localAnchor) {
    }
}
