package net.amro.multiplebedspawnpoints.network;

import java.util.List;

import io.netty.buffer.ByteBuf;
import net.amro.multiplebedspawnpoints.MultipleBedSpawnpoints;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** @param messageKey translation key shown in the bed list, or an empty string for no message */
public record BedListSyncPayload(List<BedView> beds, int maxBeds, String messageKey) implements CustomPacketPayload {
    public static final Type<BedListSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MultipleBedSpawnpoints.MODID, "bed_list_sync"));
    public static final StreamCodec<ByteBuf, BedListSyncPayload> STREAM_CODEC = StreamCodec.composite(
            BedView.STREAM_CODEC.apply(ByteBufCodecs.list()), BedListSyncPayload::beds,
            ByteBufCodecs.VAR_INT, BedListSyncPayload::maxBeds,
            ByteBufCodecs.STRING_UTF8, BedListSyncPayload::messageKey,
            BedListSyncPayload::new);

    @Override
    public Type<BedListSyncPayload> type() {
        return TYPE;
    }
}
