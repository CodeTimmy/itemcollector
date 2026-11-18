package io.github.codetimmy.itemcollector;

import com.mojang.logging.LogUtils;
import io.github.codetimmy.itemcollector.client.CollectionMenu;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(ItemCollectorMod.MODID)
public class ItemCollectorMod {

    public static final String MODID = "itemcollector";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ItemCollectorMod(final IEventBus modEventBus) {
        CollectionMenu.MENU_TYPES.register(modEventBus);
        LOGGER.info("[ItemCollector] Initializing.");
    }
}
