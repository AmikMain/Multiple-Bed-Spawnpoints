package net.amro.multiplebedspawnpoints.network;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import net.amro.multiplebedspawnpoints.ModInfo;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RespawnAtBedPayload(UUID id) implements CustomPacketPayload {
    public static final Type<RespawnAtBedPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ModInfo.MODID, "respawn_at_bed"));
    public static final StreamCodec<ByteBuf, RespawnAtBedPayload> STREAM_CODEC =
            UUIDUtil.STREAM_CODEC.map(RespawnAtBedPayload::new, RespawnAtBedPayload::id);

    @Override
    public Type<RespawnAtBedPayload> type() {
        return TYPE;
    }
}
