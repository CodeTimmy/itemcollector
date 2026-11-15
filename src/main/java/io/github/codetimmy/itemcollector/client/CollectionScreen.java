package io.github.codetimmy.itemcollector.client;

import io.github.codetimmy.itemcollector.client.widget.TabButton;
import io.github.codetimmy.itemcollector.collection.CollectionCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.Util;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public final class CollectionScreen extends AbstractContainerScreen<CollectionMenu> {

    private final List<TabButton> tabButtons = new ArrayList<>();
    private CollectionCategory activeCategory = CollectionCategory.ITEMS;
    private double scrollOffset;
    private long lastTabSwitchMillis;

    public CollectionScreen(final CollectionMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        imageWidth = 400;
        imageHeight = 200;
        lastTabSwitchMillis = Util.getMillis();
    }

    @Override
    protected void init() {
        super.init();
        tabButtons.clear();

        final CollectionCategory[] categories = CollectionCategory.values();
        final int buttonWidth = 40;
        final int buttonHeight = 36;
        final int gap = 6;
        final int rowWidth = categories.length * buttonWidth + (categories.length - 1) * gap;
        final int startX = leftPos + (imageWidth - rowWidth) / 2;
        for (int index = 0; index < categories.length; index++) {
            final CollectionCategory category = categories[index];
            final TabButton button = new TabButton(
                startX + index * (buttonWidth + gap),
                topPos + 30,
                buttonWidth,
                buttonHeight,
                    category,
                    category == activeCategory,
                    this::selectCategory
            );
            tabButtons.add(button);
            addRenderableWidget(button);
        }
    }

    private void selectCategory(final CollectionCategory category) {
        if (activeCategory == category) {
            return;
        }
        activeCategory = category;
        scrollOffset = 0;
        lastTabSwitchMillis = Util.getMillis();
        tabButtons.forEach(button -> button.setSelected(button.category() == category));
    }

    public CollectionCategory activeCategory() {
        return activeCategory;
    }

    public double scrollOffset() {
        return scrollOffset;
    }

    public long lastTabSwitchMillis() {
        return lastTabSwitchMillis;
    }

    @Override
    protected void renderBg(final GuiGraphics guiGraphics, final float partialTick, final int mouseX, final int mouseY) {
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xF0202224);
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + 1, 0xFF8A9A8B);
        guiGraphics.fill(leftPos, topPos + imageHeight - 1, leftPos + imageWidth, topPos + imageHeight, 0xFF8A9A8B);
        guiGraphics.fill(leftPos, topPos, leftPos + 1, topPos + imageHeight, 0xFF8A9A8B);
        guiGraphics.fill(leftPos + imageWidth - 1, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF8A9A8B);
    }

    @Override
    protected void renderLabels(final GuiGraphics guiGraphics, final int mouseX, final int mouseY) {
        guiGraphics.drawString(font, title, 8, 8, 0xFFFFFFFF, false);
        guiGraphics.fill(8, 106, imageWidth - 8, 107, 0xFF59645C);
        guiGraphics.drawString(
            font,
            Component.translatable("collection.category." + activeCategory.name().toLowerCase()),
            10,
            116,
            0xFFFFFFFF,
            false
        );
    }
}