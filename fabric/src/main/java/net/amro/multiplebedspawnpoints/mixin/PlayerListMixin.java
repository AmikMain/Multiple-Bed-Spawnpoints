package net.amro.multiplebedspawnpoints.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.amro.multiplebedspawnpoints.server.BedRespawnService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.portal.DimensionTransition;

@Mixin(PlayerList.class)
abstract class PlayerListMixin {
    // Does what the NeoForge module does in PlayerRespawnPositionEvent, which Fabric has no equivalent of.
    @WrapOperation(
            method = "respawn",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;findRespawnPositionAndUseSpawnBlock(ZLnet/minecraft/world/level/portal/DimensionTransition$PostDimensionTransition;)Lnet/minecraft/world/level/portal/DimensionTransition;"))
    private DimensionTransition multiplebedspawnpoints$useChosenBed(ServerPlayer player, boolean keepInventory,
                                                                    DimensionTransition.PostDimensionTransition postTransition,
                                                                    Operation<DimensionTransition> original) {
        DimensionTransition transition = BedRespawnService.usePendingRespawn(player);
        return transition != null ? transition : original.call(player, keepInventory, postTransition);
    }
}
