package net.amikmain.multiplebedspawnpoints.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.amikmain.multiplebedspawnpoints.PlayerSpawnProvider;
import net.amikmain.multiplebedspawnpoints.ISpawnData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "multiplebedspawnpoints")
public class ListSpawnsCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("listspawns")
                        .executes(ListSpawnsCommand::execute)
        );
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Player player = ctx.getSource().getPlayerOrException();
        LazyOptional<ISpawnData> cap = player.getCapability(PlayerSpawnProvider.SPAWN_DATA_CAP);

        cap.ifPresent(spawnData -> {
            if (spawnData.getSpawnPoints().isEmpty()) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Spawnpoints list is empty!"));
            } else {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Spawnpoints list:"));
                int i = 1;
                for (var sp : spawnData.getSpawnPoints()) {
                    BlockPos pos = sp.getPos();
                    String dim = sp.getDimension().location().toString();
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            i + ". " + dim + " : " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()
                    ));
                    i++;
                }
            }
        });

        return Command.SINGLE_SUCCESS;
    }
}