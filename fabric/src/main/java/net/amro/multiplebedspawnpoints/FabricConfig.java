package net.amro.multiplebedspawnpoints;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Mth;

/** A small properties file, so that players do not need a config library. Limits match the NeoForge config. */
public final class FabricConfig {
    private static final String FILE_NAME = ModInfo.MODID + ".properties";
    private static final String COOLDOWN_KEY = "respawnCooldownSeconds";
    private static final String MAX_BEDS_KEY = "maxBeds";

    private static final int DEFAULT_COOLDOWN = 30;
    private static final int MAX_COOLDOWN = 86400;
    private static final int DEFAULT_MAX_BEDS = 5;
    private static final int MIN_MAX_BEDS = 1;
    private static final int MAX_MAX_BEDS = 100;

    private static int respawnCooldownSeconds = DEFAULT_COOLDOWN;
    private static int maxBeds = DEFAULT_MAX_BEDS;

    private FabricConfig() {
    }

    public static int respawnCooldownSeconds() {
        return respawnCooldownSeconds;
    }

    public static int maxBeds() {
        return maxBeds;
    }

    /** Reads the file, creating it with the default values if it does not exist yet. */
    public static void load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        Properties properties = new Properties();

        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                properties.load(reader);
            } catch (IOException | IllegalArgumentException e) {
                ModInfo.LOGGER.warn("Could not read {}, using default values", file, e);
            }
        }

        respawnCooldownSeconds = Mth.clamp(readInt(properties, COOLDOWN_KEY, DEFAULT_COOLDOWN), 0, MAX_COOLDOWN);
        maxBeds = Mth.clamp(readInt(properties, MAX_BEDS_KEY, DEFAULT_MAX_BEDS), MIN_MAX_BEDS, MAX_MAX_BEDS);

        if (!Files.exists(file)) {
            save(file);
        }
    }

    private static int readInt(Properties properties, String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value.strip());
        } catch (NumberFormatException e) {
            ModInfo.LOGGER.warn("{} in {} is not a number: {}", key, FILE_NAME, value);
            return defaultValue;
        }
    }

    private static void save(Path file) {
        Properties properties = new Properties();
        properties.setProperty(COOLDOWN_KEY, Integer.toString(respawnCooldownSeconds));
        properties.setProperty(MAX_BEDS_KEY, Integer.toString(maxBeds));

        try (Writer writer = Files.newBufferedWriter(file)) {
            properties.store(writer, "Multiple Bed Spawnpoints. Cooldown: 0-" + MAX_COOLDOWN
                    + " seconds per bed or anchor. Max beds: " + MIN_MAX_BEDS + "-" + MAX_MAX_BEDS + " per player.");
        } catch (IOException e) {
            ModInfo.LOGGER.warn("Could not write {}", file, e);
        }
    }
}
