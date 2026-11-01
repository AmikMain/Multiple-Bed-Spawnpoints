package net.amro.multiplebedspawnpoints.network;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import net.amro.multiplebedspawnpoints.ModInfo;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DeleteBedPayload(UUID id) implements CustomPacketPayload {
    public static final Type<DeleteBedPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ModInfo.MODID, "delete_bed"));
    public static final StreamCodec<ByteBuf, DeleteBedPayload> STREAM_CODEC =
            UUIDUtil.STREAM_CODEC.map(DeleteBedPayload::new, DeleteBedPayload::id);

    @Override
    public Type<DeleteBedPayload> type() {
        return TYPE;
    }
}
