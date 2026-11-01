package net.amro.multiplebedspawnpoints.network;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import net.amro.multiplebedspawnpoints.data.BedEntry;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What the client needs to show one row of the bed list.
 *
 * @param charge glowstone charge of a respawn anchor, {@link #NOT_AN_ANCHOR} for a bed, or
 *               {@link #CHARGE_UNKNOWN} for an anchor on an unloaded sub-level
 */
public record BedView(UUID id, String name, Place place, boolean obstructed, int cooldownTicks, int charge) {
    public static final int NOT_AN_ANCHOR = -1;
    public static final int CHARGE_UNKNOWN = -2;

    public static final StreamCodec<ByteBuf, BedView> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, BedView::id,
            ByteBufCodecs.stringUtf8(BedEntry.MAX_NAME_LENGTH), BedView::name,
            Place.STREAM_CODEC, BedView::place,
            ByteBufCodecs.BOOL, BedView::obstructed,
            ByteBufCodecs.VAR_INT, BedView::cooldownTicks,
            ByteBufCodecs.VAR_INT, BedView::charge,
            BedView::new);

    public boolean isAnchor() {
        return charge != NOT_AN_ANCHOR;
    }

    /**
     * @param location     world position, last known if the bed is on an unloaded sub-level
     * @param subLevelName name of the sub-level the bed is on, empty if it is not on one or the sub-level has no name
     */
    public record Place(GlobalPos location, String subLevelName) {
        private static final StreamCodec<ByteBuf, Place> STREAM_CODEC = StreamCodec.composite(
                GlobalPos.STREAM_CODEC, Place::location,
                ByteBufCodecs.STRING_UTF8, Place::subLevelName,
                Place::new);
    }
}
