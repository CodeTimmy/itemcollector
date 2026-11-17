package io.github.codetimmy.itemcollector.client;

import io.github.codetimmy.itemcollector.client.widget.TabButton;
import io.github.codetimmy.itemcollector.client.widget.EntryGrid;
import io.github.codetimmy.itemcollector.collection.CollectionCategory;
import io.github.codetimmy.itemcollector.client.ClientPayloadHandler;
import io.github.codetimmy.itemcollector.collection.CollectibleEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class CollectionScreen extends AbstractContainerScreen<CollectionMenu> {

    private final List<TabButton> tabButtons = new ArrayList<>();
    private CollectionCategory activeCategory = CollectionCategory.ITEMS;
    private EntryGrid entryGrid;
    private EditBox searchBox;
    private Button collectedToggle;
    private boolean showCollected = true;
    private boolean draggingScrollbar;

    public CollectionScreen(final CollectionMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        imageWidth = 560;
        imageHeight = 300;
    }

    @Override
    protected void init() {
        imageWidth = Math.max(240, Math.min(560, width - 24));
        imageHeight = Math.max(136, Math.min(300, height - 24));
        super.init();
        topPos = Math.max(12, Math.min(topPos - 20, height - imageHeight - 12));
        tabButtons.clear();

        final int toggleWidth = 128;
        searchBox = new EditBox(font, leftPos + 8, topPos + 7, imageWidth - toggleWidth - 24, 18,
            Component.translatable("collection.search"));
        searchBox.setHint(Component.translatable("collection.search"));
        searchBox.setResponder(value -> refreshGrid());
        addRenderableWidget(searchBox);

        collectedToggle = Button.builder(collectedToggleLabel(), button -> {
            showCollected = !showCollected;
            button.setMessage(collectedToggleLabel());
            refreshGrid();
        }).bounds(leftPos + imageWidth - toggleWidth - 8, topPos + 6, toggleWidth, 20).build();
        addRenderableWidget(collectedToggle);

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
        refreshGrid();
        addRenderableWidget(entryGrid);
    }

    private void selectCategory(final CollectionCategory category) {
        if (activeCategory == category) {
            return;
        }
        activeCategory = category;
        refreshGrid();
        tabButtons.forEach(button -> button.setSelected(button.category() == category));
    }

    private void refreshGrid() {
        if (entryGrid == null) {
            return;
        }
        final String query = searchBox == null ? "" : searchBox.getValue().toLowerCase(Locale.ROOT);
        final List<CollectibleEntry> visibleEntries = ClientPayloadHandler.masterList
                .getOrDefault(activeCategory, List.of())
                .stream()
                .filter(entry -> showCollected || !ClientPayloadHandler.collected.contains(entry.id()))
                .filter(entry -> EntryGrid.displayNameFor(entry).getString().toLowerCase(Locale.ROOT).contains(query))
                .sorted(Comparator
                        .comparingInt((CollectibleEntry entry) -> entry.id().getNamespace().equals("minecraft") ? 0 : 1)
                        .thenComparing(entry -> entry.id().getNamespace().toLowerCase(Locale.ROOT))
                    .thenComparing(entry -> EntryGrid.displayNameFor(entry).getString().toLowerCase(Locale.ROOT))
                        .thenComparing(entry -> entry.id().toString()))
                .toList();
        entryGrid.setEntries(visibleEntries);
    }

    private Component collectedToggleLabel() {
        return Component.translatable(showCollected
                ? "collection.show_collected.on"
                : "collection.show_collected.off");
    }

    public CollectionCategory activeCategory() {
        return activeCategory;
    }

    public double scrollOffset() {
        return entryGrid == null ? 0 : entryGrid.scrollOffset();
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY) {
        if (entryGrid != null && entryGrid.isMouseOver(mouseX, mouseY) && entryGrid.scroll(scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (button == 0 && entryGrid != null && entryGrid.beginScrollbarDrag(mouseX, mouseY)) {
            draggingScrollbar = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(final double mouseX, final double mouseY, final int button,
                                final double dragX, final double dragY) {
        if (draggingScrollbar && button == 0 && entryGrid != null) {
            entryGrid.dragScrollbarTo(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        if (button == 0 && draggingScrollbar) {
            draggingScrollbar = false;
            if (entryGrid != null) {
                entryGrid.endScrollbarDrag();
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        if (searchBox != null && searchBox.isFocused()
                && Minecraft.getInstance().options.keyInventory.matches(keyCode, scanCode)) {
            searchBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
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
        guiGraphics.fill(8, 78, imageWidth - 8, 79, 0xFF59645C);
    }
}