package net.amro.multiplebedspawnpoints.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.amro.multiplebedspawnpoints.server.BedRespawnService;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

@Mixin(ServerPlayer.class)
abstract class ServerPlayerMixin {
    // Does what the NeoForge module does in PlayerSetSpawnEvent, which Fabric has no equivalent of.
    // Vanilla's distance, obstruction and charge checks have passed by the time this runs.
    // Forced spawns (/spawnpoint) keep working as in vanilla.
    @Inject(method = "setRespawnPosition", at = @At("HEAD"), cancellable = true)
    private void multiplebedspawnpoints$addSpawnBlock(ResourceKey<Level> dimension, @Nullable BlockPos position, float angle,
                                                      boolean forced, boolean sendMessage, CallbackInfo ci) {
        if (position != null && !forced && BedRespawnService.addSpawnBlock((ServerPlayer) (Object) this, dimension, position)) {
            ci.cancel();
        }
    }
}
