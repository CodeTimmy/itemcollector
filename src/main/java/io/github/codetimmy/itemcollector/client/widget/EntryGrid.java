package io.github.codetimmy.itemcollector.client.widget;

import io.github.codetimmy.itemcollector.client.ClientPayloadHandler;
import io.github.codetimmy.itemcollector.collection.CollectibleEntry;
import io.github.codetimmy.itemcollector.collection.CollectionCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public final class EntryGrid extends AbstractWidget {

    private static final int MAX_COLUMNS = 17;
    private static final int CELL_SIZE = 20;
    private static final int CELL_GAP = 2;

    private List<CollectibleEntry> entries = List.of();
    private double scrollOffset;

    public EntryGrid(final int x, final int y, final int width, final int height) {
        super(x, y, width, height, Component.empty());
    }

    public void setEntries(final List<CollectibleEntry> entries) {
        this.entries = entries;
        scrollOffset = 0;
    }

    public double scrollOffset() {
        return scrollOffset;
    }

    public boolean scroll(final double scrollAmount) {
        final double maxOffset = maxScrollOffset();
        if (maxOffset <= 0 || scrollAmount == 0) {
            return false;
        }

        final double nextOffset = Math.max(0, Math.min(maxOffset, scrollOffset - Math.signum(scrollAmount) * (CELL_SIZE + CELL_GAP)));
        if (nextOffset == scrollOffset) {
            return false;
        }
        scrollOffset = nextOffset;
        return true;
    }

    @Override
    protected void renderWidget(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float partialTick) {
        final int columns = columns();
        final int cellStep = CELL_SIZE + CELL_GAP;
        final int gridWidth = columns * CELL_SIZE + (columns - 1) * CELL_GAP;
        final int startX = getX() + (width - gridWidth) / 2;
        final int firstRow = (int) (scrollOffset / cellStep);
        final int rowOffset = (int) (scrollOffset % cellStep);
        final int visibleRows = (height + cellStep - 1) / cellStep + 1;
        CollectibleEntry hoveredEntry = null;

        guiGraphics.enableScissor(getX(), getY(), getX() + width, getY() + height);
        for (int visibleRow = 0; visibleRow < visibleRows; visibleRow++) {
            final int row = firstRow + visibleRow;
            final int y = getY() + visibleRow * cellStep - rowOffset;
            for (int column = 0; column < columns; column++) {
                final int entryIndex = row * columns + column;
                if (entryIndex >= entries.size()) {
                    break;
                }

                final int x = startX + column * cellStep;
                final boolean hovered = mouseX >= x && mouseX < x + CELL_SIZE
                        && mouseY >= y && mouseY < y + CELL_SIZE;
                final CollectibleEntry entry = entries.get(entryIndex);
                guiGraphics.fill(x, y, x + CELL_SIZE, y + CELL_SIZE, hovered ? 0xFF46534A : 0xFF292D2B);
                guiGraphics.renderItem(iconFor(entry), x + 2, y + 2);
                if (!ClientPayloadHandler.collected.contains(entry.id())) {
                    guiGraphics.fill(x, y, x + CELL_SIZE, y + CELL_SIZE, 0x80808080);
                }
                if (hovered) {
                    hoveredEntry = entry;
                }
            }
        }
        guiGraphics.disableScissor();
        if (hoveredEntry != null) {
            guiGraphics.renderTooltip(net.minecraft.client.Minecraft.getInstance().font,
                    hoveredEntry.displayName(), mouseX, mouseY);
        }
    }

    @Override
    protected void updateWidgetNarration(final NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }

    private double maxScrollOffset() {
        final int columns = columns();
        final int rowCount = (entries.size() + columns - 1) / columns;
        final double contentHeight = rowCount * (CELL_SIZE + CELL_GAP) - CELL_GAP;
        return Math.max(0, contentHeight - height);
    }

    private int columns() {
        return Math.max(1, Math.min(MAX_COLUMNS, (width + CELL_GAP) / (CELL_SIZE + CELL_GAP)));
    }

    private static ItemStack iconFor(final CollectibleEntry entry) {
        final Item item = switch (entry.category()) {
            case ITEMS, BLOCKS -> BuiltInRegistries.ITEM.getOptional(entry.id()).orElse(Items.BARRIER);
            case ENTITIES -> BuiltInRegistries.ITEM.getOptional(spawnEggId(entry.id())).orElse(Items.SPAWNER);
            case BIOMES -> Items.OAK_SAPLING;
            case STRUCTURES -> Items.MAP;
            case EFFECTS -> Items.POTION;
            case ENCHANTMENTS -> Items.ENCHANTED_BOOK;
            case ADVANCEMENTS -> Items.NETHER_STAR;
        };
        return new ItemStack(item);
    }

    private static ResourceLocation spawnEggId(final ResourceLocation entityId) {
        return ResourceLocation.fromNamespaceAndPath(entityId.getNamespace(), entityId.getPath() + "_spawn_egg");
    }
}