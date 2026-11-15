package io.github.codetimmy.itemcollector.event;

import io.github.codetimmy.itemcollector.ItemCollectorMod;
import io.github.codetimmy.itemcollector.collection.CollectionCategory;
import io.github.codetimmy.itemcollector.collection.CollectibleRegistry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.util.List;

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
}
