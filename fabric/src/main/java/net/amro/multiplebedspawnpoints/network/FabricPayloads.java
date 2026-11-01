package net.amro.multiplebedspawnpoints.network;

import net.amro.multiplebedspawnpoints.server.BedRespawnService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class FabricPayloads {
    private FabricPayloads() {
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(RequestBedListPayload.TYPE, RequestBedListPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(RenameBedPayload.TYPE, RenameBedPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(DeleteBedPayload.TYPE, DeleteBedPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(RespawnAtBedPayload.TYPE, RespawnAtBedPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(BedListSyncPayload.TYPE, BedListSyncPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(RequestBedListPayload.TYPE,
                (payload, context) -> BedRespawnService.sendBedList(context.player(), ""));
        ServerPlayNetworking.registerGlobalReceiver(RenameBedPayload.TYPE,
                (payload, context) -> BedRespawnService.renameBed(context.player(), payload.id(), payload.name()));
        ServerPlayNetworking.registerGlobalReceiver(DeleteBedPayload.TYPE,
                (payload, context) -> BedRespawnService.deleteBed(context.player(), payload.id()));
        ServerPlayNetworking.registerGlobalReceiver(RespawnAtBedPayload.TYPE,
                (payload, context) -> BedRespawnService.respawnAtBed(context.player(), payload.id()));
    }
}
