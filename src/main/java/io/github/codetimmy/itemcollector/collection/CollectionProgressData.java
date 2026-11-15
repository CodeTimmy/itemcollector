package io.github.codetimmy.itemcollector.collection;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;

/**
 * Persistent per-player collection progress.
 * <p>
 * Stored as {@code <world>/data/collection_progress.dat}.
 * Automatically loaded on server start and written back when {@link #setDirty()} is called.
 */
public class CollectionProgressData extends SavedData {

    /** UUID -> (Category -> Set of collected entry IDs) */
    private final Map<UUID, Map<CollectionCategory, Set<ResourceLocation>>> progress;

    private static final Codec<Map<CollectionCategory, Set<ResourceLocation>>> CATEGORY_MAP_CODEC =
            Codec.unboundedMap(
                    Codec.STRING.xmap(CollectionCategory::valueOf, CollectionCategory::name),
                    ResourceLocation.CODEC.listOf().xmap(HashSet::new, ArrayList::new)
            );

    public static final Codec<CollectionProgressData> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.unboundedMap(UUIDUtil.STRING_CODEC, CATEGORY_MAP_CODEC)
                            .fieldOf("progress")
                            .forGetter(data -> data.progress)
            ).apply(instance, CollectionProgressData::new));

    public static final SavedDataType<CollectionProgressData> TYPE = new SavedDataType<>(
            "collection_progress",
            CollectionProgressData::new,
            CODEC
    );

    public CollectionProgressData() {
        this.progress = new HashMap<>();
    }

    private CollectionProgressData(Map<UUID, Map<CollectionCategory, Set<ResourceLocation>>> progress) {
        this.progress = new HashMap<>(progress);
    }

    // ------------------------------------------------------------------
    // Access
    // ------------------------------------------------------------------

    /** Returns the data attached to the Overworld, creating it if absent. */
    public static CollectionProgressData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(TYPE, "collection_progress");
    }

    public boolean isCollected(UUID playerId, CollectionCategory category, ResourceLocation entryId) {
        return progress.getOrDefault(playerId, Map.of())
                .getOrDefault(category, Set.of())
                .contains(entryId);
    }

    public Set<ResourceLocation> getCollected(UUID playerId, CollectionCategory category) {
        return Collections.unmodifiableSet(
                progress.getOrDefault(playerId, Map.of())
                        .getOrDefault(category, Set.of())
        );
    }

    // ------------------------------------------------------------------
    // Mutation
    // ------------------------------------------------------------------

    /**
     * Marks an entry as collected for the given player.
     * @return {@code true} if this is a new entry (not previously collected).
     */
    public boolean markCollected(UUID playerId, CollectionCategory category, ResourceLocation entryId) {
        boolean added = progress
                .computeIfAbsent(playerId, k -> new EnumMap<>(CollectionCategory.class))
                .computeIfAbsent(category, k -> new HashSet<>())
                .add(entryId);
        if (added) {
            setDirty(); // REQUIRED: tells the game to write to disk
        }
        return added;
    }
}
