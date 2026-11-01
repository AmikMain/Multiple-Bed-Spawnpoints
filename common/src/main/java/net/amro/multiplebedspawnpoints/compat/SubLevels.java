package net.amro.multiplebedspawnpoints.compat;

import net.amro.multiplebedspawnpoints.platform.ModPlatform;

/** The sub-level integration of the current loader, created the first time it is needed. */
public final class SubLevels {
    private static final SubLevelSupport SUPPORT = ModPlatform.get().createSubLevelSupport();

    private SubLevels() {
    }

    public static SubLevelSupport support() {
        return SUPPORT;
    }
}
