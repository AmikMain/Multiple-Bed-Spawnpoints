package net.amro.multiplebedspawnpoints.platform;

/** Holds the current loader's platform. Each loader's entry point calls {@link #set} before anything else runs. */
public final class ModPlatform {
    private static LoaderPlatform platform;

    private ModPlatform() {
    }

    public static LoaderPlatform get() {
        return platform;
    }

    public static void set(LoaderPlatform loaderPlatform) {
        platform = loaderPlatform;
    }
}
