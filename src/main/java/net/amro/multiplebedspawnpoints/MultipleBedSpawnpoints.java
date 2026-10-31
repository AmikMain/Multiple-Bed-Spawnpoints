package net.amro.multiplebedspawnpoints;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.amro.multiplebedspawnpoints.data.ModAttachments;
import net.amro.multiplebedspawnpoints.network.ModPayloads;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(MultipleBedSpawnpoints.MODID)
public class MultipleBedSpawnpoints {
    public static final String MODID = "multiplebedspawnpoints";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MultipleBedSpawnpoints(IEventBus modEventBus, ModContainer modContainer) {
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        modEventBus.addListener(ModPayloads::register);

        // SERVER config lives in <world>/serverconfig and is decided by the server, not by each client.
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
    }
}
