package net.amro.multiplebedspawnpoints.compat;

import net.amro.multiplebedspawnpoints.MultipleBedSpawnpoints;
import net.neoforged.fml.ModList;

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
        if (!ModList.get().isLoaded(SABLE_MOD_ID)) {
            return SubLevelSupport.NONE;
        }

        try {
            SableSupport.verify();
            MultipleBedSpawnpoints.LOGGER.info("Sable found, beds and respawn anchors on sub-levels are supported");
            return new SableSupport();
        } catch (ReflectiveOperationException | LinkageError e) {
            MultipleBedSpawnpoints.LOGGER.warn("This Sable version is not supported, beds on sub-levels will not work", e);
            return SubLevelSupport.NONE;
        }
    }
}
