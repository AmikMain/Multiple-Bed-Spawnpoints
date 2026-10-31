package net.amro.multiplebedspawnpoints;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue RESPAWN_COOLDOWN_SECONDS = BUILDER
            .comment("Seconds before a bed can be used again after respawning on it. Each bed has its own timer.")
            .defineInRange("respawnCooldownSeconds", 30, 0, 86400);

    public static final ModConfigSpec.IntValue MAX_BEDS = BUILDER
            .comment("Maximum number of beds a player can have in their respawn list.")
            .defineInRange("maxBeds", 5, 1, 100);

    static final ModConfigSpec SPEC = BUILDER.build();
}
