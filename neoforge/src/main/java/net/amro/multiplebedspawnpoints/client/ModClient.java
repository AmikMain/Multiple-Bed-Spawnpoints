package net.amro.multiplebedspawnpoints.client;

import java.util.List;

import net.amro.multiplebedspawnpoints.MultipleBedSpawnpoints;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = MultipleBedSpawnpoints.MODID, dist = Dist.CLIENT)
public class ModClient {
    private static final KeyMapping OPEN_BED_LIST = BedListKey.create();

    public ModClient(IEventBus modEventBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        modEventBus.addListener(ModClient::registerKeyMappings);
        NeoForge.EVENT_BUS.addListener(ModClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(ModClient::onScreenInit);
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_BED_LIST);
    }

    // NeoForge refuses connections to servers without the mod, so the list can always be used here.
    private static void onClientTick(ClientTickEvent.Post event) {
        BedListKey.handlePresses(OPEN_BED_LIST, true);
    }

    private static void onScreenInit(ScreenEvent.Init.Post event) {
        DeathScreenButtons.replaceRespawnButtons(event.getScreen(), List.copyOf(event.getListenersList()))
                .forEach(event::addListener);
    }
}
