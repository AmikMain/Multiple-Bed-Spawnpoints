package net.amro.multiplebedspawnpoints.network;

import io.netty.buffer.ByteBuf;
import net.amro.multiplebedspawnpoints.ModInfo;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RequestBedListPayload() implements CustomPacketPayload {
    public static final RequestBedListPayload INSTANCE = new RequestBedListPayload();
    public static final Type<RequestBedListPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ModInfo.MODID, "request_bed_list"));
    public static final StreamCodec<ByteBuf, RequestBedListPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<RequestBedListPayload> type() {
        return TYPE;
    }
}
