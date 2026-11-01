package net.amro.multiplebedspawnpoints;

import net.amro.multiplebedspawnpoints.compat.SubLevels;
import net.amro.multiplebedspawnpoints.data.ModAttachments;
import net.amro.multiplebedspawnpoints.network.ModPayloads;
import net.amro.multiplebedspawnpoints.platform.ModPlatform;
import net.amro.multiplebedspawnpoints.platform.NeoForgePlatform;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(MultipleBedSpawnpoints.MODID)
public class MultipleBedSpawnpoints {
    public static final String MODID = ModInfo.MODID;

    public MultipleBedSpawnpoints(IEventBus modEventBus, ModContainer modContainer) {
        ModPlatform.set(new NeoForgePlatform());

        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        modEventBus.addListener(ModPayloads::register);
        modEventBus.addListener(this::commonSetup);

        // SERVER config lives in <world>/serverconfig and is decided by the server, not by each client.
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Checks an installed Sable at startup, so an unsupported version shows up in the log right away.
        SubLevels.support();
    }
}
