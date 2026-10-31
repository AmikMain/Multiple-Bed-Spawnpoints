package net.amro.multiplebedspawnpoints.network;

import net.amro.multiplebedspawnpoints.client.ClientPayloadHandler;
import net.amro.multiplebedspawnpoints.server.BedRespawnService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModPayloads {
    private static final String PROTOCOL_VERSION = "1";

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        registrar.playToServer(RequestBedListPayload.TYPE, RequestBedListPayload.STREAM_CODEC,
                (payload, context) -> BedRespawnService.sendBedList(serverPlayer(context), ""));
        registrar.playToServer(RenameBedPayload.TYPE, RenameBedPayload.STREAM_CODEC,
                (payload, context) -> BedRespawnService.renameBed(serverPlayer(context), payload.id(), payload.name()));
        registrar.playToServer(DeleteBedPayload.TYPE, DeleteBedPayload.STREAM_CODEC,
                (payload, context) -> BedRespawnService.deleteBed(serverPlayer(context), payload.id()));
        registrar.playToServer(RespawnAtBedPayload.TYPE, RespawnAtBedPayload.STREAM_CODEC,
                (payload, context) -> BedRespawnService.respawnAtBed(serverPlayer(context), payload.id()));

        registrar.playToClient(BedListSyncPayload.TYPE, BedListSyncPayload.STREAM_CODEC, ModPayloads::handleBedList);
    }

    private static ServerPlayer serverPlayer(IPayloadContext context) {
        return (ServerPlayer) context.player();
    }

    // The dist check keeps ClientPayloadHandler from being loaded on a dedicated server.
    private static void handleBedList(BedListSyncPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientPayloadHandler.handleBedList(payload);
        }
    }
}
