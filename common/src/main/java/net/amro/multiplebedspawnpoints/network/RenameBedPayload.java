package net.amro.multiplebedspawnpoints.network;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import net.amro.multiplebedspawnpoints.ModInfo;
import net.amro.multiplebedspawnpoints.data.BedEntry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RenameBedPayload(UUID id, String name) implements CustomPacketPayload {
    public static final Type<RenameBedPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ModInfo.MODID, "rename_bed"));
    public static final StreamCodec<ByteBuf, RenameBedPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, RenameBedPayload::id,
            ByteBufCodecs.stringUtf8(BedEntry.MAX_NAME_LENGTH), RenameBedPayload::name,
            RenameBedPayload::new);

    @Override
    public Type<RenameBedPayload> type() {
        return TYPE;
    }
}
