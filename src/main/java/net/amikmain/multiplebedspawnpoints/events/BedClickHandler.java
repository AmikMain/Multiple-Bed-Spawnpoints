package net.amikmain.multiplebedspawnpoints.events;

import net.amikmain.multiplebedspawnpoints.ISpawnData;
import net.amikmain.multiplebedspawnpoints.PlayerSpawnProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;

@Mod.EventBusSubscriber(modid = "multiplebedspawnpoints")
public class BedClickHandler {

    // Ловим клик по блокам
    @SubscribeEvent
    public static void onRightClickBed(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof BedBlock bed)) return;

        // Определяем позицию головы кровати
        BlockPos headPos = pos;
        if (state.getValue(BedBlock.PART) == BedPart.FOOT) {
            headPos = pos.relative(state.getValue(BedBlock.FACING));
        }
        final  BlockPos finalHeadPos = headPos;

        // DEBUG
        System.out.println("Bed clicked at " + headPos + " by " + player.getName().getString()
                + " | isClientSide=" + level.isClientSide);

        // Добавляем спавнпоинт на сервере
        if (!level.isClientSide) {
            LazyOptional<ISpawnData> cap = player.getCapability(PlayerSpawnProvider.SPAWN_DATA_CAP);
            if (cap.isPresent()) {
                cap.ifPresent(spawnData -> {
                    ResourceKey<Level> dim = level.dimension();

                    boolean exists = spawnData.getSpawnPoints().stream().anyMatch(sp ->
                            sp.getPos().equals(finalHeadPos) && sp.getDimension().equals(dim)
                    );

                    if (!exists) {
                        spawnData.addSpawnPoint(finalHeadPos, dim);
                        player.displayClientMessage(
                                net.minecraft.network.chat.Component.literal("Кровать добавлена в список спавнпоинтов!"),
                                true
                        );
                        System.out.println("SpawnData size now: " + spawnData.getSpawnPoints().size());
                    } else {
                        System.out.println("Эта кровать уже есть в списке спавнпоинтов");
                    }
                });
            } else {
                System.out.println("CAPABILITY ПУСТАЯ! Проверь attach capabilities.");
            }
        }

        // Отменяем ванильную установку spawnpoint
        event.setCanceled(true);
    }

    // Attach capabilities к игроку
    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player player) {
            event.addCapability(PlayerSpawnProvider.IDENTIFIER, new PlayerSpawnProvider());

            // безопасный лог
            if (player.getGameProfile() != null) {
                System.out.println("AttachCapabilitiesEvent fired для игрока: " + player.getName().getString());
            } else {
                System.out.println("AttachCapabilitiesEvent fired для игрока, но gameProfile еще null");
            }
        }
    }
}