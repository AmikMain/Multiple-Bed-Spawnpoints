package net.amro.multiplebedspawnpoints.client;

import net.amro.multiplebedspawnpoints.network.BedListSyncPayload;
import net.minecraft.client.Minecraft;

public class ClientPayloadHandler {
    public static void handleBedList(BedListSyncPayload payload) {
        if (Minecraft.getInstance().screen instanceof BedListScreen screen) {
            screen.update(payload);
        }
    }
}
