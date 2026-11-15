package io.github.codetimmy.itemcollector.event;

import io.github.codetimmy.itemcollector.ItemCollectorMod;
import io.github.codetimmy.itemcollector.collection.CollectibleRegistry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = ItemCollectorMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class ServerLifecycleEvents {

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        CollectibleRegistry.rebuild(event.getServer());
        ItemCollectorMod.LOGGER.info(
                "[ItemCollector] Master list built: {} total entries across {} categories.",
                CollectibleRegistry.totalEntryCount(),
                CollectibleRegistry.get(io.github.codetimmy.itemcollector.collection.CollectionCategory.ITEMS)
                        .map(List::size).orElse(0)
        );
    }
}
