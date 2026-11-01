package net.amro.multiplebedspawnpoints.compat;

import net.amro.multiplebedspawnpoints.ModInfo;
import net.amro.multiplebedspawnpoints.platform.ModPlatform;

/** Picks the sub-level implementation. {@link SableSupport} stays unloaded unless Sable is installed. */
public final class SubLevels {
    private static final String SABLE_MOD_ID = "sable";
    private static final SubLevelSupport SUPPORT = create();

    private SubLevels() {
    }

    public static SubLevelSupport support() {
        return SUPPORT;
    }

    private static SubLevelSupport create() {
        if (!ModPlatform.get().isModLoaded(SABLE_MOD_ID)) {
            return SubLevelSupport.NONE;
        }

        try {
            SableSupport.verify();
            ModInfo.LOGGER.info("Sable found, beds and respawn anchors on sub-levels are supported");
            return new SableSupport();
        } catch (ReflectiveOperationException | LinkageError e) {
            ModInfo.LOGGER.warn("This Sable version is not supported, beds on sub-levels will not work", e);
            return SubLevelSupport.NONE;
        }
    }
}
