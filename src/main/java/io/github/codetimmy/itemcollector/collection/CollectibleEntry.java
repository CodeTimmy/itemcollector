package io.github.codetimmy.itemcollector.collection;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * A single collectible thing — an item, block, entity variant, biome, etc.
 *
 * <p>{@code id} is the registry key (e.g. {@code minecraft:stone}).
 * {@code displayName} is what the player sees in the UI.
 * {@code extraData} is used for variant-specific information or display metadata
 * (e.g. sheep color, villager profession, advancement icon) and may be null.</p>
 *
 * <p>This record is immutable. Do not add setters; create a new instance
 * if an entry needs to change (e.g. after a localization reload).</p>
 */
public record CollectibleEntry(
        ResourceLocation id,
        Component displayName,
        CollectionCategory category,
        @Nullable CompoundTag extraData
) {
    // Compact constructor: enforce non-null invariants early.
    public CollectibleEntry {
        if (id == null) throw new IllegalArgumentException("id cannot be null");
        if (displayName == null) throw new IllegalArgumentException("displayName cannot be null");
        if (category == null) throw new IllegalArgumentException("category cannot be null");
    }

    /** Convenience constructor for entries without variant data. */
    public CollectibleEntry(ResourceLocation id, Component displayName, CollectionCategory category) {
        this(id, displayName, category, null);
    }

    /** True if this entry describes a specific variant (e.g. a red sheep). */
    public boolean hasVariantData() {
        return category != CollectionCategory.ADVANCEMENTS && extraData != null && !extraData.isEmpty();
    }
}
