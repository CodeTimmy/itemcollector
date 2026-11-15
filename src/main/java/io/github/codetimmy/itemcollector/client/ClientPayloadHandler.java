package io.github.codetimmy.itemcollector.client;

import io.github.codetimmy.itemcollector.ItemCollectorMod;
import io.github.codetimmy.itemcollector.collection.CollectionCategory;
import io.github.codetimmy.itemcollector.collection.CollectibleEntry;
import io.github.codetimmy.itemcollector.network.SyncMasterListPayload;
import io.github.codetimmy.itemcollector.network.SyncProgressPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.*;

public final class ClientPayloadHandler {

    // Cache für die GUI
    public static Map<CollectionCategory, List<CollectibleEntry>> masterList = new EnumMap<>(CollectionCategory.class);
    public static Set<ResourceLocation> collected = new HashSet<>();

    public static void handleMasterList(final SyncMasterListPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            masterList.clear();
            masterList.putAll(payload.entries());
            ItemCollectorMod.LOGGER.info("[ItemCollector] Client received master list with {} categories.",
                    masterList.size());
        });
    }

    public static void handleProgress(final SyncProgressPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            collected.clear();
            collected.addAll(payload.collected());
            ItemCollectorMod.LOGGER.debug("[ItemCollector] Client progress synced: {} entries.",
                    collected.size());
        });
    }
}
