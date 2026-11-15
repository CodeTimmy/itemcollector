package io.github.codetimmy.itemcollector.client;

import io.github.codetimmy.itemcollector.ItemCollectorMod;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.resources.InputConstants;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = ItemCollectorMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {

    public static final KeyMapping OPEN_COLLECTION = new KeyMapping(
            "key.itemcollector.open_collection",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            "key.categories.itemcollector"
    );

    @SubscribeEvent
    public static void registerKeyMappings(final RegisterKeyMappingsEvent event) {
        event.register(OPEN_COLLECTION);
    }

    @SubscribeEvent
    public static void registerMenuScreens(final RegisterMenuScreensEvent event) {
        event.register(CollectionMenu.MENU_TYPE.get(), CollectionScreen::new);
    }
}