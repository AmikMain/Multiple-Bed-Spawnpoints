package net.amro.multiplebedspawnpoints.client;

import java.util.List;

import net.amro.multiplebedspawnpoints.network.BedListSyncPayload;
import net.amro.multiplebedspawnpoints.network.RequestBedListPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.AbstractWidget;

public class MultipleBedSpawnpointsFabricClient implements ClientModInitializer {
    private static final KeyMapping OPEN_BED_LIST = KeyBindingHelper.registerKeyBinding(BedListKey.create());

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(BedListSyncPayload.TYPE,
                (payload, context) -> ClientPayloadHandler.handleBedList(payload));

        ClientTickEvents.END_CLIENT_TICK.register(client -> BedListKey.handlePresses(OPEN_BED_LIST, serverHasMod()));

        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (serverHasMod()) {
                List<AbstractWidget> buttons = Screens.getButtons(screen);
                buttons.addAll(DeathScreenButtons.replaceRespawnButtons(screen, buttons));
            }
        });
    }

    // Unlike NeoForge, Fabric lets a client join a server without the mod. Vanilla's respawn button has to stay then.
    private static boolean serverHasMod() {
        return ClientPlayNetworking.canSend(RequestBedListPayload.TYPE);
    }
}
