package io.github.codetimmy.itemcollector.client;

import io.github.codetimmy.itemcollector.ItemCollectorMod;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = ItemCollectorMod.MODID, value = Dist.CLIENT)
public final class ClientEvents {

    @SubscribeEvent
    public static void onClientTick(final ClientTickEvent.Post event) {
        while (ClientModEvents.OPEN_COLLECTION.consumeClick()) {
            final Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.screen != null) {
                continue;
            }

            final CollectionMenu menu = new CollectionMenu(0, minecraft.player.getInventory());
            minecraft.setScreen(new CollectionScreen(
                    menu,
                    minecraft.player.getInventory(),
                    Component.translatable("screen.itemcollector.collection")
            ));
        }
    }
}