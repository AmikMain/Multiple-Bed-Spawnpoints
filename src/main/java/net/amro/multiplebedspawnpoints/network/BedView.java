package net.amro.multiplebedspawnpoints.network;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import net.amro.multiplebedspawnpoints.data.BedEntry;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** What the client needs to show one row of the bed list. */
public record BedView(UUID id, String name, GlobalPos location, boolean obstructed, int cooldownTicks) {
    public static final StreamCodec<ByteBuf, BedView> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, BedView::id,
            ByteBufCodecs.stringUtf8(BedEntry.MAX_NAME_LENGTH), BedView::name,
            GlobalPos.STREAM_CODEC, BedView::location,
            ByteBufCodecs.BOOL, BedView::obstructed,
            ByteBufCodecs.VAR_INT, BedView::cooldownTicks,
            BedView::new);
}
