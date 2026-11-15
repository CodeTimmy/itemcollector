package io.github.codetimmy.itemcollector.event;

import io.github.codetimmy.itemcollector.ItemCollectorMod;
import io.github.codetimmy.itemcollector.collection.CollectionCategory;
import io.github.codetimmy.itemcollector.collection.CollectionProgressData;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = ItemCollectorMod.MODID)
public final class TickEvents {

    private static final Map<UUID, Integer> TICK_COUNTER = new WeakHashMap<>();
    private static final int POLL_INTERVAL = 20;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;

        int count = TICK_COUNTER.merge(player.getUUID(), 1, Integer::sum);
        if (count < POLL_INTERVAL) return;
        TICK_COUNTER.put(player.getUUID(), 0);

        CollectionProgressData data = CollectionProgressData.get(player.server);
        var level = player.serverLevel();
        var pos = player.blockPosition();

        // --- BIOMES ---
        var biomeHolder = level.getBiome(pos);
        biomeHolder.unwrapKey().ifPresent(key -> {
            ResourceLocation id = key.location();
            if (data.markCollected(player.getUUID(), CollectionCategory.BIOMES, id)) {
                ItemCollectorMod.LOGGER.debug(
                        "[ItemCollector] {} discovered biome {}", player.getName().getString(), id);
            }
        });

        // --- STRUCTURES (1.21.1: datapack registry via RegistryAccess) ---
        var structureManager = level.structureManager();
        var structureStart = structureManager.getStructureWithPieceAt(pos, holder -> true);
        if (structureStart != null && structureStart.isValid()) {
            // Strukturen liegen im datenpaketbasierten Registry "Registries.STRUCTURE".
            // BuiltInRegistries enthält dafür kein Feld.
            var structureRegistry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
            ResourceLocation id = structureRegistry.getKey(structureStart.getStructure());
            if (id != null && data.markCollected(player.getUUID(), CollectionCategory.STRUCTURES, id)) {
                ItemCollectorMod.LOGGER.debug(
                        "[ItemCollector] {} discovered structure {}",
                        player.getName().getString(), id);
            }
        }
    }
}
