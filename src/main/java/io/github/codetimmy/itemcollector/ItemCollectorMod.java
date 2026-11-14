package com.example.itemcollector;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Mod(ItemCollectorMod.MODID)
public class ItemCollectorMod {

    public static final String MODID = "itemcollector";
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Public, immutable list of all collected items.
     * Key   = ResourceLocation (e.g. "minecraft:dirt", "create:brass_ingot")
     * Value = the Item object itself
     */
    public static final Map<ResourceLocation, Item> ALL_ITEMS;

    static {
        // Placeholder; filled in collectAllItems()
        ALL_ITEMS = new LinkedHashMap<>();
    }

    public ItemCollectorMod(IEventBus modEventBus) {
        // Listen to FMLCommonSetupEvent – at this point all registries are populated
        modEventBus.addListener(this::onCommonSetup);
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        // Important: run the work inside enqueueWork because lifecycle events
        // may be fired in parallel.
        event.enqueueWork(this::collectAllItems);
    }

    private void collectAllItems() {
        Map<ResourceLocation, Item> collected = new LinkedHashMap<>();

        for (Map.Entry<ResourceKey<Item>, Item> entry : BuiltInRegistries.ITEM.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            Item item = entry.getValue();
            collected.put(id, item);
        }

        // Move the collected map into the public, immutable reference
        ALL_ITEMS.clear();
        ALL_ITEMS.putAll(Collections.unmodifiableMap(collected));

        LOGGER.info("[ItemCollector] Collected {} items from all mods (including Vanilla).",
                ALL_ITEMS.size());

        // Optional: log a few examples to show that mod items are included
        ALL_ITEMS.keySet().stream()
                .filter(id -> !id.getNamespace().equals("minecraft"))
                .limit(5)
                .forEach(id -> LOGGER.info("  -> Found mod item: {}", id));
    }
}
