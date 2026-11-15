package io.github.codetimmy.itemcollector.event;

import io.github.codetimmy.itemcollector.ItemCollectorMod;
import io.github.codetimmy.itemcollector.collection.CollectionCategory;
import io.github.codetimmy.itemcollector.collection.CollectionProgressData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = ItemCollectorMod.MODID)
public final class ProgressEvents {

    // ------------------------------------------------------------------
    // ITEMS — fired after the item is added to the player's inventory
    // ------------------------------------------------------------------
    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Post event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;

        ItemStack stack = event.getOriginalStack();
        if (stack.isEmpty()) return;

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id != null) {
            mark(player, CollectionCategory.ITEMS, id);
        }

        // ENCHANTMENTS — check the enchantments on the picked-up stack
        ItemEnchantments enchantments = stack.getEnchantments();
        for (var holder : enchantments.keySet()) {
            holder.unwrapKey().ifPresent(key -> {
                ResourceLocation enchId = key.location();
                mark(player, CollectionCategory.ENCHANTMENTS, enchId);
            });
        }
    }

    // ------------------------------------------------------------------
    // BLOCKS — fired when a player breaks a block
    // ------------------------------------------------------------------
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;

        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(event.getState().getBlock());
        if (id != null) {
            mark(player, CollectionCategory.BLOCKS, id);
        }
    }

    // ------------------------------------------------------------------
    // ENTITIES — fired when a living entity dies
    // ------------------------------------------------------------------
    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        LivingEntity entity = event.getEntity();
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (id != null) {
            // Der Spieler, der getötet hat, ist nicht direkt verfügbar.
            // Wir markieren für alle Spieler in der Nähe (vereinfacht).
            // Für eine präzisere Zuordnung müsste man DamageSource auswerten.
            entity.level().getEntitiesOfClass(ServerPlayer.class,
                            entity.getBoundingBox().inflate(32.0))
                    .forEach(player -> mark(player, CollectionCategory.ENTITIES, id));
        }
    }

    // ------------------------------------------------------------------
    // EFFECTS — fired when a mob effect is about to be applied
    // ------------------------------------------------------------------
    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        var effect = event.getEffectInstance().getEffect();
        effect.unwrapKey().ifPresent(key -> {
            ResourceLocation id = key.location();
            mark(player, CollectionCategory.EFFECTS, id);
        });
    }

    // ------------------------------------------------------------------
    // ADVANCEMENTS — fired when the player earns an advancement
    // ------------------------------------------------------------------
    @SubscribeEvent
    public static void onAdvancementEarn(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;

        // Rezept-Advancements haben keinen Display-Eintrag und werden übersprungen.
        if (event.getAdvancement().value().display().isEmpty()) return;

        mark(player, CollectionCategory.ADVANCEMENTS, event.getAdvancement().id());
    }

    // ------------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------------
    private static void mark(ServerPlayer player, CollectionCategory category, ResourceLocation entryId) {
        CollectionProgressData data = CollectionProgressData.get(player.server);
        boolean added = data.markCollected(player.getUUID(), category, entryId);
        if (added) {
            ItemCollectorMod.LOGGER.debug(
                    "[ItemCollector] {} collected {} ({})", player.getName().getString(), entryId, category);
        }
    }
}
