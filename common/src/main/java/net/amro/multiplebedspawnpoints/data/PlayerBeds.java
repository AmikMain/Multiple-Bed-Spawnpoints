package net.amro.multiplebedspawnpoints.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.GlobalPos;

/**
 * Server-side respawn data of one player. Last-use times are kept by position rather than on
 * {@link BedEntry}, so deleting a bed and adding it again does not reset its cooldown.
 */
public class PlayerBeds {
    public static final Codec<PlayerBeds> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BedEntry.CODEC.listOf().fieldOf("beds").forGetter(data -> data.beds),
            BedUse.CODEC.listOf().fieldOf("uses").forGetter(data -> data.uses)
    ).apply(instance, PlayerBeds::new));

    private final List<BedEntry> beds;
    private final List<BedUse> uses;

    public PlayerBeds() {
        this(List.of(), List.of());
    }

    private PlayerBeds(List<BedEntry> beds, List<BedUse> uses) {
        this.beds = new ArrayList<>(beds);
        this.uses = new ArrayList<>(uses);
    }

    public List<BedEntry> beds() {
        return List.copyOf(beds);
    }

    public int size() {
        return beds.size();
    }

    public Optional<BedEntry> find(UUID id) {
        return beds.stream().filter(bed -> bed.id().equals(id)).findFirst();
    }

    public boolean contains(GlobalPos location) {
        return beds.stream().anyMatch(bed -> bed.location().equals(location));
    }

    public void add(BedEntry bed) {
        beds.add(bed);
    }

    public boolean remove(UUID id) {
        return beds.removeIf(bed -> bed.id().equals(id));
    }

    public void replace(BedEntry updated) {
        beds.replaceAll(bed -> bed.id().equals(updated.id()) ? updated : bed);
    }

    public void rename(UUID id, String name) {
        beds.replaceAll(bed -> bed.id().equals(id) ? bed.withName(name) : bed);
    }

    public void markUsed(GlobalPos location, long gameTime) {
        uses.removeIf(use -> use.location().equals(location));
        uses.add(new BedUse(location, gameTime));
    }

    /**
     * Uses the current cooldown value instead of a stored end time, so a config change applies immediately.
     *
     * @return ticks left before the bed can be used, or 0 if it is available
     */
    public long remainingCooldown(GlobalPos location, long gameTime, long cooldownTicks) {
        return uses.stream()
                .filter(use -> use.location().equals(location))
                .mapToLong(use -> Math.max(0, use.gameTime() + cooldownTicks - gameTime))
                .findFirst()
                .orElse(0);
    }

    public void forgetExpiredUses(long gameTime, long cooldownTicks) {
        uses.removeIf(use -> use.gameTime() + cooldownTicks <= gameTime);
    }

    private record BedUse(GlobalPos location, long gameTime) {
        private static final Codec<BedUse> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                GlobalPos.CODEC.fieldOf("location").forGetter(BedUse::location),
                Codec.LONG.fieldOf("game_time").forGetter(BedUse::gameTime)
        ).apply(instance, BedUse::new));
    }
}
