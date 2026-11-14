package io.github.codetimmy.itemcollector;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Mod(ItemCollectorMod.MODID)
public class ItemCollectorMod {

    public static final String MODID = "itemcollector";
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Public list of all collected items.
     * Key   = ResourceLocation (e.g. "minecraft:dirt", "create:brass_ingot")
     * Value = the Item object itself
     */
    public static final Map<ResourceLocation, Item> ALL_ITEMS = new LinkedHashMap<>();

    public ItemCollectorMod(IEventBus modEventBus) {
        modEventBus.addListener(this::onCommonSetup);
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        // Lifecycle events fire in parallel; run registry access on the main thread.
        event.enqueueWork(this::collectAllItems);
    }

    private void collectAllItems() {
        Map<ResourceLocation, Item> collected = new LinkedHashMap<>();

        for (Map.Entry<ResourceKey<Item>, Item> entry : BuiltInRegistries.ITEM.entrySet()) {
            collected.put(entry.getKey().location(), entry.getValue());
        }

        ALL_ITEMS.clear();
        ALL_ITEMS.putAll(Collections.unmodifiableMap(collected));

        LOGGER.info("[ItemCollector] Collected {} items from all mods (including Vanilla).",
                ALL_ITEMS.size());

        // Log a few mod items as a sanity check.
        ALL_ITEMS.keySet().stream()
                .filter(id -> !id.getNamespace().equals("minecraft"))
                .limit(5)
                .forEach(id -> LOGGER.info("  -> Found mod item: {}", id));

        // Optional: export the list as JSON into the game directory.
        try {
            JsonObject json = new JsonObject();
            for (Map.Entry<ResourceLocation, Item> entry : ALL_ITEMS.entrySet()) {
                json.addProperty(entry.getKey().toString(), entry.getValue().getDescriptionId());
            }
            var path = FMLPaths.GAMEDIR.get().resolve("itemcollector_items.json");
            try (var writer = Files.newBufferedWriter(path)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(json, writer);
            }
            LOGGER.info("[ItemCollector] Exported item list to {}", path);
        } catch (Exception e) {
            LOGGER.error("[ItemCollector] Export failed", e);
        }
    }
}
