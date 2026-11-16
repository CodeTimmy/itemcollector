package io.github.codetimmy.itemcollector.client.widget;

import io.github.codetimmy.itemcollector.collection.CollectionCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Locale;
import java.util.function.Consumer;

public final class TabButton extends AbstractWidget {

    private final CollectionCategory category;
    private final ItemStack icon;
    private final Consumer<CollectionCategory> onSelect;
    private boolean selected;

    public TabButton(
            final int x,
            final int y,
            final int width,
            final int height,
            final CollectionCategory category,
            final boolean selected,
            final Consumer<CollectionCategory> onSelect
    ) {
        super(x, y, width, height, Component.translatable(categoryTranslationKey(category)));
        setTooltip(Tooltip.create(getMessage()));
        this.category = category;
        this.icon = new ItemStack(iconFor(category));
        this.selected = selected;
        this.onSelect = onSelect;
    }

    public CollectionCategory category() {
        return category;
    }

    public void setSelected(final boolean selected) {
        this.selected = selected;
    }

    @Override
    protected void renderWidget(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float partialTick) {
        final int background = selected ? 0xFF526657 : isHoveredOrFocused() ? 0xFF3B4440 : 0xFF292D2B;
        guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, background);
        if (selected) {
            guiGraphics.fill(getX(), getY() + height - 2, getX() + width, getY() + height, 0xFFA8C49E);
        }

        final int x = getX() + (width - 16) / 2;
        final int y = getY() + (height - 16) / 2;
        if (category == CollectionCategory.EFFECTS) {
            renderEffectIcon(guiGraphics, x, y);
            return;
        }
        guiGraphics.renderItem(icon, x, y);
    }

    @Override
    public void onClick(final double mouseX, final double mouseY) {
        onSelect.accept(category);
    }

    @Override
    protected void updateWidgetNarration(final NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }

    private static String categoryTranslationKey(final CollectionCategory category) {
        return "collection.category." + category.name().toLowerCase(Locale.ROOT);
    }

    private static net.minecraft.world.item.Item iconFor(final CollectionCategory category) {
        return switch (category) {
            case ITEMS -> Items.IRON_INGOT;
            case BLOCKS -> Items.BRICKS;
            case ENTITIES -> Items.CREEPER_SPAWN_EGG;
            case BIOMES -> Items.OAK_SAPLING;
            case STRUCTURES -> Items.MAP;
            case EFFECTS -> Items.POTION;
            case ENCHANTMENTS -> Items.ENCHANTED_BOOK;
            case ADVANCEMENTS -> Items.NETHER_STAR;
        };
    }

    private static void renderEffectIcon(final GuiGraphics guiGraphics, final int x, final int y) {
        final var sprite = Minecraft.getInstance().getMobEffectTextures().get(MobEffects.MOVEMENT_SPEED);
        guiGraphics.blit(x, y, 0, 16, 16, sprite);
    }
}