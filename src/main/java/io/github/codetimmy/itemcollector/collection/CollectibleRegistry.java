package io.github.codetimmy.itemcollector.collection;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Builds the master list of all collectible entries by iterating game registries.
 * <p>
 * The master list is rebuilt on every server start. It is not persisted to disk;
 * only player progress is persisted (see {@link CollectionProgressData}).
 */
public final class CollectibleRegistry {

    private static final Map<CollectionCategory, List<CollectibleEntry>> MASTER_LIST = new EnumMap<>(CollectionCategory.class);

    private CollectibleRegistry() {}

    /**
     * Rebuilds the master list for all categories.
     * Must be called on the logical server after registries are frozen.
     */
    public static void rebuild(MinecraftServer server) {
        MASTER_LIST.clear();

        buildCreativeOrderedCategory(CollectionCategory.ITEMS, BuiltInRegistries.ITEM, Items.AIR);
        buildCreativeOrderedCategory(CollectionCategory.BLOCKS, BuiltInRegistries.BLOCK, Blocks.AIR);

        buildEntities();

        // Placeholders for later phases (see Phase 7 of the roadmap)
        MASTER_LIST.put(CollectionCategory.BIOMES, List.of());
        MASTER_LIST.put(CollectionCategory.STRUCTURES, List.of());
        MASTER_LIST.put(CollectionCategory.EFFECTS, List.of());
        MASTER_LIST.put(CollectionCategory.ENCHANTMENTS, List.of());

        buildAdvancements(server);

        // Freeze each list to prevent accidental mutation by callers.
        MASTER_LIST.replaceAll((cat, list) -> Collections.unmodifiableList(list));
    }

    private static void buildEntities() {
        final List<CollectibleEntry> entries = new ArrayList<>();
        for (Map.Entry<ResourceKey<EntityType<?>>, EntityType<?>> entry : BuiltInRegistries.ENTITY_TYPE.entrySet()) {
            final ResourceLocation id = entry.getKey().location();
            final EntityType<?> entityType = entry.getValue();
            if (entityType == EntityType.PLAYER
                    || (entityType.getCategory() == MobCategory.MISC && entityType != EntityType.ARMOR_STAND)) {
                continue;
            }

            entries.add(new CollectibleEntry(
                    id,
                    Component.translatable(entityType.getDescriptionId()),
                    CollectionCategory.ENTITIES
            ));
        }
        MASTER_LIST.put(CollectionCategory.ENTITIES, entries);
    }

    // ------------------------------------------------------------------
    // Items and Blocks (creative tab ordered)
    // ------------------------------------------------------------------

    /**
     * Builds a list where entries first appear in creative-tab order,
     * then any remaining registry entries are appended in registry order.
     * This matches the visual order players expect from the creative menu.
     */
    private static <T> void buildCreativeOrderedCategory(
            CollectionCategory category,
            net.minecraft.core.Registry<T> registry,
            T airLike
    ) {
        List<CollectibleEntry> entries = new ArrayList<>();
        Set<ResourceLocation> seen = new LinkedHashSet<>();

        // 1. Iterate all creative tabs and extract item stacks in their display order.
        //    getDisplayItems() returns the stacks in the exact order the tab shows them.
        for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
            for (ItemStack stack : tab.getDisplayItems()) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                // For BLOCKS, only include blocks that have a corresponding item form.
                if (category == CollectionCategory.BLOCKS) {
                    if (!(stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem)) continue;
                    id = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
                }
                if (id == null || !seen.add(id)) continue;

                T value = registry.get(id);
                if (value == null || value == airLike) continue;

                entries.add(new CollectibleEntry(
                        id,
                        new ItemStack(stack.getItem()).getHoverName(),
                        category
                ));
            }
        }

        // 2. Append anything that wasn't in a creative tab (rare, but possible).
        for (Map.Entry<ResourceKey<T>, T> entry : registry.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            if (seen.contains(id)) continue;
            T value = entry.getValue();
            if (value == airLike) continue;

            entries.add(new CollectibleEntry(
                    id,
                    Component.literal(id.toString()), // fallback name; refined during UI rendering
                    category
            ));
        }

        MASTER_LIST.put(category, entries);
    }

    // ------------------------------------------------------------------
    // Advancements (not a registry)
    // ------------------------------------------------------------------

    /**
     * Advancements live in {@link net.minecraft.server.ServerAdvancementManager},
     * not in a {@code BuiltInRegistries} table.
     * Recipe advancements (those without a display) are filtered out.
     */
    private static void buildAdvancements(MinecraftServer server) {
        List<CollectibleEntry> entries = new ArrayList<>();

        for (var holder : server.getAdvancements().getAllAdvancements()) {
            var advancement = holder.value();
            // Skip recipe advancements and other hidden entries.
            if (advancement.display().isEmpty()) continue;

            Component title = advancement.display().get().getTitle();
            entries.add(new CollectibleEntry(
                    holder.id(),
                    title,
                    CollectionCategory.ADVANCEMENTS
            ));
        }

        MASTER_LIST.put(CollectionCategory.ADVANCEMENTS, entries);
    }

    // ------------------------------------------------------------------
    // Public accessor
    // ------------------------------------------------------------------

    /** Returns the immutable master list, or {@link Optional#empty()} if not built. */
    public static Optional<List<CollectibleEntry>> get(CollectionCategory category) {
        return Optional.ofNullable(MASTER_LIST.get(category));
    }

    /** Returns the total number of collectible entries across all categories. */
    public static int totalEntryCount() {
        return MASTER_LIST.values().stream().mapToInt(List::size).sum();
    }
}
