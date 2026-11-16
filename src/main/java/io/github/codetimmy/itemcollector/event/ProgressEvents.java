package io.github.codetimmy.itemcollector.event;

import io.github.codetimmy.itemcollector.ItemCollectorMod;
import io.github.codetimmy.itemcollector.collection.CollectionCategory;
import io.github.codetimmy.itemcollector.collection.CollectionProgressData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = ItemCollectorMod.MODID)
public final class ProgressEvents {

    private static final Map<UUID, Map<ResourceLocation, Integer>> OPEN_CHEST_ITEMS = new HashMap<>();
    private static final Map<UUID, Map<ResourceLocation, Integer>> OPEN_CHEST_ENCHANTMENTS = new HashMap<>();

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

        markEnchantments(player, stack);
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        markItemStack(player, event.getCrafting());
    }

    @SubscribeEvent
    public static void onVillagerTrade(TradeWithVillagerEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        markItemStack(player, event.getMerchantOffer().getResult());
    }

    @SubscribeEvent
    public static void onFishBucketed(PlayerInteractEvent.EntityInteract event) {
        if (event.isCanceled()) return;
        markFishBucketInteraction(event.getEntity(), event.getTarget(), event.getItemStack());
    }

    @SubscribeEvent
    public static void onFishBucketedSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.isCanceled()) return;
        markFishBucketInteraction(event.getEntity(), event.getTarget(), event.getItemStack());
    }

    @SubscribeEvent
    public static void onFluidBucketed(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled()) return;
        markFluidBucketInteraction(event.getEntity(), event.getItemStack(), event.getPos());
    }

    @SubscribeEvent
    public static void onFluidBucketedInAir(PlayerInteractEvent.RightClickItem event) {
        if (event.isCanceled()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!event.getItemStack().is(Items.BUCKET)) return;

        final Vec3 start = player.getEyePosition();
        final Vec3 end = start.add(player.getViewVector(1.0F).scale(5.0D));
        final HitResult hitResult = player.level().clip(new ClipContext(
            start,
            end,
            ClipContext.Block.OUTLINE,
            ClipContext.Fluid.SOURCE_ONLY,
            player
        ));
        if (hitResult instanceof BlockHitResult blockHit && hitResult.getType() == HitResult.Type.BLOCK) {
            markFluidBucketInteraction(player, event.getItemStack(), blockHit.getBlockPos());
        }
    }

    private static void markFishBucketInteraction(final Player player, final net.minecraft.world.entity.Entity target,
                                                  final ItemStack heldItem) {
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.isSpectator()) return;
        if (!(target instanceof Bucketable bucketable) || !(target instanceof LivingEntity entity) || !entity.isAlive()) return;
        if (!heldItem.is(Items.BUCKET)) return;

        final ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (entityId != null) {
            mark(serverPlayer, CollectionCategory.ENTITIES, entityId);
        }
        markItemStack(serverPlayer, heldItem);
        markItemStack(serverPlayer, bucketable.getBucketItemStack());
    }

    private static void markFluidBucketInteraction(final Player player, final ItemStack heldItem,
                                                   final net.minecraft.core.BlockPos pos) {
        if (!(player instanceof ServerPlayer serverPlayer) || !heldItem.is(Items.BUCKET)) return;

        final var state = serverPlayer.level().getBlockState(pos);
        final var fluid = state.getFluidState();
        if (!fluid.isSource()) return;

        if (state.is(Blocks.WATER) && fluid.is(Fluids.WATER)) {
            markBlock(serverPlayer, Blocks.WATER);
            markItemStack(serverPlayer, heldItem);
            markItemStack(serverPlayer, new ItemStack(Items.WATER_BUCKET));
        } else if (state.is(Blocks.LAVA) && fluid.is(Fluids.LAVA)) {
            markBlock(serverPlayer, Blocks.LAVA);
            markItemStack(serverPlayer, heldItem);
            markItemStack(serverPlayer, new ItemStack(Items.LAVA_BUCKET));
        }
    }

    @SubscribeEvent
    public static void onChestOpened(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getContainer() instanceof ChestMenu menu)) return;
        OPEN_CHEST_ITEMS.put(player.getUUID(), countChestItems(menu));
        OPEN_CHEST_ENCHANTMENTS.put(player.getUUID(), countChestEnchantments(menu));
    }

    @SubscribeEvent
    public static void onChestClosed(PlayerContainerEvent.Close event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getContainer() instanceof ChestMenu menu)) return;

        recordChestLoot(player, menu);
        OPEN_CHEST_ITEMS.remove(player.getUUID());
        OPEN_CHEST_ENCHANTMENTS.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onChestLootTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.containerMenu instanceof ChestMenu menu)) return;
        recordChestLoot(player, menu);
    }

    @SubscribeEvent
    public static void onEnchantingTableTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.containerMenu instanceof EnchantmentMenu menu)) return;
        markEnchantments(player, menu.getSlot(0).getItem());
    }

    private static void recordChestLoot(final ServerPlayer player, final ChestMenu menu) {
        final Map<ResourceLocation, Integer> before = OPEN_CHEST_ITEMS.get(player.getUUID());
        if (before == null) return;

        final Map<ResourceLocation, Integer> after = countChestItems(menu);
        before.forEach((id, count) -> {
            if (after.getOrDefault(id, 0) < count) {
                mark(player, CollectionCategory.ITEMS, id);
            }
        });
        OPEN_CHEST_ITEMS.put(player.getUUID(), after);

        final Map<ResourceLocation, Integer> enchantmentsBefore = OPEN_CHEST_ENCHANTMENTS.get(player.getUUID());
        if (enchantmentsBefore != null) {
            final Map<ResourceLocation, Integer> enchantmentsAfter = countChestEnchantments(menu);
            enchantmentsBefore.forEach((id, count) -> {
                if (enchantmentsAfter.getOrDefault(id, 0) < count) {
                    mark(player, CollectionCategory.ENCHANTMENTS, id);
                }
            });
            OPEN_CHEST_ENCHANTMENTS.put(player.getUUID(), enchantmentsAfter);
        }
    }

    @SubscribeEvent
    public static void onBlockMiningStarted(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(
                player.level().getBlockState(event.getPos()).getBlock());
        if (id != null) {
            mark(player, CollectionCategory.BLOCKS, id);
        }
    }

    // ------------------------------------------------------------------
    // Non-boss entities count when a player successfully damages them.
    // ------------------------------------------------------------------
    @SubscribeEvent
    public static void onLivingEntityDamaged(LivingIncomingDamageEvent event) {
        if (event.getAmount() <= 0.0F) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        final LivingEntity entity = event.getEntity();
        if (entity.getType().is(Tags.EntityTypes.BOSSES)) return;

        final ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (id != null) {
            mark(player, CollectionCategory.ENTITIES, id);
        }
    }

    @SubscribeEvent
    public static void onBossDeath(LivingDeathEvent event) {
        final LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !entity.getType().is(Tags.EntityTypes.BOSSES)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        final ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (id != null) {
            mark(player, CollectionCategory.ENTITIES, id);
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
                "[ItemCollector] {} collected {} ({})",
                player.getName().getString(), entryId, category);
            // Push the updated progress to the client so the GUI refreshes live.
            ServerLifecycleEvents.sendProgressUpdate(player);
        }
    }

    private static void markItemStack(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return;
        final ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id != null) {
            mark(player, CollectionCategory.ITEMS, id);
        }
        markEnchantments(player, stack);
    }

    private static void markEnchantments(final ServerPlayer player, final ItemStack stack) {
        if (stack.isEmpty()) return;
        for (ResourceLocation id : enchantmentIds(stack)) {
            mark(player, CollectionCategory.ENCHANTMENTS, id);
        }
    }

    private static Set<ResourceLocation> enchantmentIds(final ItemStack stack) {
        final Set<ResourceLocation> ids = new HashSet<>();
        addEnchantmentIds(ids, stack.getEnchantments());
        addEnchantmentIds(ids, stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY));
        return ids;
    }

    private static void addEnchantmentIds(final Set<ResourceLocation> ids, final ItemEnchantments enchantments) {
        for (var holder : enchantments.keySet()) {
            holder.unwrapKey().ifPresent(key -> ids.add(key.location()));
        }
    }

    private static void markBlock(final ServerPlayer player, final net.minecraft.world.level.block.Block block) {
        final ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        if (id != null) {
            mark(player, CollectionCategory.BLOCKS, id);
        }
    }

    private static Map<ResourceLocation, Integer> countChestItems(final AbstractContainerMenu menu) {
        final Map<ResourceLocation, Integer> counts = new HashMap<>();
        final int containerSlotCount = Math.max(0, menu.slots.size() - 36);
        for (int slotIndex = 0; slotIndex < containerSlotCount; slotIndex++) {
            final ItemStack stack = menu.slots.get(slotIndex).getItem();
            if (stack.isEmpty()) continue;

            final ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id != null) {
                counts.merge(id, stack.getCount(), Integer::sum);
            }
        }
        return counts;
    }

    private static Map<ResourceLocation, Integer> countChestEnchantments(final AbstractContainerMenu menu) {
        final Map<ResourceLocation, Integer> counts = new HashMap<>();
        final int containerSlotCount = Math.max(0, menu.slots.size() - 36);
        for (int slotIndex = 0; slotIndex < containerSlotCount; slotIndex++) {
            final ItemStack stack = menu.slots.get(slotIndex).getItem();
            if (stack.isEmpty()) continue;

            for (ResourceLocation id : enchantmentIds(stack)) {
                counts.merge(id, stack.getCount(), Integer::sum);
            }
        }
        return counts;
    }
}
