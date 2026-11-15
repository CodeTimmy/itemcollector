package io.github.codetimmy.itemcollector.client;

import io.github.codetimmy.itemcollector.ItemCollectorMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class CollectionMenu extends AbstractContainerMenu {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, ItemCollectorMod.MODID);
    public static final Supplier<MenuType<CollectionMenu>> MENU_TYPE = MENU_TYPES.register(
            "collection",
            () -> new MenuType<>(CollectionMenu::new, FeatureFlags.DEFAULT_FLAGS)
    );

    public CollectionMenu(final int containerId, final Inventory inventory, final FriendlyByteBuf extraData) {
        this(containerId, inventory);
    }

    public CollectionMenu(final int containerId, final Inventory inventory) {
        super(MENU_TYPE.get(), containerId);
    }

    @Override
    public boolean stillValid(final Player player) {
        return true;
    }
}