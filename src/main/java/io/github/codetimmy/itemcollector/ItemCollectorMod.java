package io.github.codetimmy.itemcollector;

import com.mojang.logging.LogUtils;
import io.github.codetimmy.itemcollector.client.CollectionMenu;
import io.github.codetimmy.itemcollector.config.CollectionConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.config.ModConfigEvent;
import org.slf4j.Logger;

@Mod(ItemCollectorMod.MODID)
public class ItemCollectorMod {

    public static final String MODID = "itemcollector";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ItemCollectorMod(final IEventBus modEventBus) {
        CollectionMenu.MENU_TYPES.register(modEventBus);
        modEventBus.addListener(this::registerConfig);
        LOGGER.info("[ItemCollector] Initializing.");
    }

    private void registerConfig(final ModConfigEvent.Register event) {
        event.register(net.neoforged.neoforge.common.ModConfig.Type.SERVER, CollectionConfig.SPEC, "collection");
    }
}
