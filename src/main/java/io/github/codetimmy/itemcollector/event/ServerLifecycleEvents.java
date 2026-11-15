package io.github.codetimmy.itemcollector.event;

import io.github.codetimmy.itemcollector.ItemCollectorMod;
import io.github.codetimmy.itemcollector.collection.CollectionCategory;
import io.github.codetimmy.itemcollector.collection.CollectionProgressData;
import io.github.codetimmy.itemcollector.collection.CollectibleEntry;
import io.github.codetimmy.itemcollector.collection.CollectibleRegistry;
import io.github.codetimmy.itemcollector.network.SyncMasterListPayload;
import io.github.codetimmy.itemcollector.network.SyncProgressPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = ItemCollectorMod.MODID)
public final class ServerLifecycleEvents {

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        CollectibleRegistry.rebuild(event.getServer());
        ItemCollectorMod.LOGGER.info(
                "[ItemCollector] Master list built: {} total entries. Items: {}.",
                CollectibleRegistry.totalEntryCount(),
                CollectibleRegistry.get(CollectionCategory.ITEMS).map(List::size).orElse(0)
        );
    }

    /**
     * Sends the master list and the player's current progress as soon as they join.
     * The client caches both, and the GUI in Phase 6 reads from those caches.
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // 1. Master list
        Map<CollectionCategory, List<CollectibleEntry>> masterList =
                new EnumMap<>(CollectionCategory.class);
        for (CollectionCategory cat : CollectionCategory.values()) {
            CollectibleRegistry.get(cat).ifPresent(list -> masterList.put(cat, list));
        }
        PacketDistributor.sendToPlayer(player, new SyncMasterListPayload(masterList));

        // 2. Progress
        PacketDistributor.sendToPlayer(player, buildProgressPayload(player));
    }

    /**
     * Helper to build a {@link SyncProgressPayload} from the persisted data.
     * Used both on join and whenever progress changes (see {@link #sendProgressUpdate(ServerPlayer)}).
     */
    public static SyncProgressPayload buildProgressPayload(ServerPlayer player) {
        CollectionProgressData data = CollectionProgressData.get(player.server);
        Set<ResourceLocation> collected = new HashSet<>();
        for (CollectionCategory cat : CollectionCategory.values()) {
            collected.addAll(data.getCollected(player.getUUID(), cat));
        }
        return new SyncProgressPayload(collected);
    }

    /** Convenience method called by {@link ProgressEvents} and {@link TickEvents} after a mark. */
    public static void sendProgressUpdate(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, buildProgressPayload(player));
    }
}
