package io.github.codetimmy.itemcollector;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(ItemCollectorMod.MODID)
public class ItemCollectorMod {

    public static final String MODID = "itemcollector";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ItemCollectorMod() {
        LOGGER.info("[ItemCollector] Initializing.");
    }
}
