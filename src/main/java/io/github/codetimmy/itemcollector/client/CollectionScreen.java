package io.github.codetimmy.itemcollector.client;

import io.github.codetimmy.itemcollector.client.widget.TabButton;
import io.github.codetimmy.itemcollector.client.widget.EntryGrid;
import io.github.codetimmy.itemcollector.collection.CollectionCategory;
import io.github.codetimmy.itemcollector.client.ClientPayloadHandler;
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
    private EntryGrid entryGrid;
    private long lastTabSwitchMillis;

    public CollectionScreen(final CollectionMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        imageWidth = 560;
        imageHeight = 300;
        lastTabSwitchMillis = Util.getMillis();
    }

    @Override
    protected void init() {
        imageWidth = Math.max(240, Math.min(560, width - 24));
        imageHeight = Math.max(136, Math.min(300, height - 24));
        super.init();
        topPos = Math.max(12, Math.min(topPos - 20, height - imageHeight - 12));
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

        entryGrid = new EntryGrid(leftPos + 8, topPos + 84, imageWidth - 16, imageHeight - 92);
        entryGrid.setEntries(ClientPayloadHandler.masterList.getOrDefault(activeCategory, List.of()));
        addRenderableWidget(entryGrid);
    }

    private void selectCategory(final CollectionCategory category) {
        if (activeCategory == category) {
            return;
        }
        activeCategory = category;
        entryGrid.setEntries(ClientPayloadHandler.masterList.getOrDefault(category, List.of()));
        lastTabSwitchMillis = Util.getMillis();
        tabButtons.forEach(button -> button.setSelected(button.category() == category));
    }

    public CollectionCategory activeCategory() {
        return activeCategory;
    }

    public double scrollOffset() {
        return entryGrid == null ? 0 : entryGrid.scrollOffset();
    }

    public long lastTabSwitchMillis() {
        return lastTabSwitchMillis;
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY) {
        if (entryGrid != null && entryGrid.isMouseOver(mouseX, mouseY) && entryGrid.scroll(scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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
        guiGraphics.fill(8, 78, imageWidth - 8, 79, 0xFF59645C);
    }
}