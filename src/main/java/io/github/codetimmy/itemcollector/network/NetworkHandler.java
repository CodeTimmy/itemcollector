package io.github.codetimmy.itemcollector.network;

import io.github.codetimmy.itemcollector.ItemCollectorMod;
import io.github.codetimmy.itemcollector.client.ClientPayloadHandler;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = ItemCollectorMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class NetworkHandler {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        // Die "1" ist die Netzwerk-Version dieses Mods.
        final PayloadRegistrar registrar = event.registrar("1");

        // Masterliste an den Client senden
        registrar.playToClient(
                SyncMasterListPayload.TYPE,
                SyncMasterListPayload.STREAM_CODEC,
                ClientPayloadHandler::handleMasterList
        );

        // Fortschritt an den Client senden
        registrar.playToClient(
                SyncProgressPayload.TYPE,
                SyncProgressPayload.STREAM_CODEC,
                ClientPayloadHandler::handleProgress
        );
    }
}
