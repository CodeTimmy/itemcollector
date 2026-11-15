package io.github.codetimmy.itemcollector.collection;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public class CollectionProgressData extends SavedData {

    private final Map<UUID, Map<CollectionCategory, Set<ResourceLocation>>> progress;

    public CollectionProgressData() {
        this.progress = new HashMap<>();
    }

    // ------------------------------------------------------------------
    // Factory & Loading (1.21.1 API)
    // ------------------------------------------------------------------

    public static final SavedData.Factory<CollectionProgressData> FACTORY = new SavedData.Factory<>(
            CollectionProgressData::new,
            CollectionProgressData::load
    );

    private static CollectionProgressData load(CompoundTag tag, HolderLookup.Provider provider) {
        CollectionProgressData data = new CollectionProgressData();
        CompoundTag progressTag = tag.getCompound("progress");

        for (String uuidStr : progressTag.getAllKeys()) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                CompoundTag playerTag = progressTag.getCompound(uuidStr);
                Map<CollectionCategory, Set<ResourceLocation>> catMap = new EnumMap<>(CollectionCategory.class);

                for (String catName : playerTag.getAllKeys()) {
                    try {
                        CollectionCategory category = CollectionCategory.valueOf(catName);
                        ListTag list = playerTag.getList(catName, Tag.TAG_STRING);
                        Set<ResourceLocation> set = new HashSet<>();
                        for (int i = 0; i < list.size(); i++) {
                            set.add(ResourceLocation.parse(list.getString(i)));
                        }
                        catMap.put(category, set);
                    } catch (IllegalArgumentException ignored) {}
                }
                data.progress.put(uuid, catMap);
            } catch (IllegalArgumentException ignored) {}
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag progressTag = new CompoundTag();

        for (var playerEntry : progress.entrySet()) {
            CompoundTag playerTag = new CompoundTag();
            for (var catEntry : playerEntry.getValue().entrySet()) {
                ListTag list = new ListTag();
                for (ResourceLocation id : catEntry.getValue()) {
                    list.add(StringTag.valueOf(id.toString()));
                }
                playerTag.put(catEntry.getKey().name(), list);
            }
            progressTag.put(playerEntry.getKey().toString(), playerTag);
        }

        tag.put("progress", progressTag);
        return tag;
    }

    // ------------------------------------------------------------------
    // Access
    // ------------------------------------------------------------------

    public static CollectionProgressData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(FACTORY, "collection_progress");
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
