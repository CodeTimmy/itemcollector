package io.github.codetimmy.itemcollector.client.widget;

import io.github.codetimmy.itemcollector.client.ClientPayloadHandler;
import io.github.codetimmy.itemcollector.collection.CollectibleEntry;
import io.github.codetimmy.itemcollector.collection.CollectionCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;

import java.util.List;
import java.util.Optional;

public final class EntryGrid extends AbstractWidget {

    private static final int MAX_COLUMNS = 17;
    private static final int CELL_SIZE = 20;
    private static final int CELL_GAP = 2;
    private static final int SCROLLBAR_WIDTH = 5;
    private static final int SCROLLBAR_GAP = 4;
        private static final ResourceLocation UNKNOWN_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "itemcollector", "textures/gui/unknown.png");

    private List<CollectibleEntry> entries = List.of();
    private double scrollOffset;
    private boolean scrollbarDragActive;
    private double scrollbarDragOffset;

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
        final int contentWidth = width - SCROLLBAR_WIDTH - SCROLLBAR_GAP;
        final int startX = getX() + (contentWidth - gridWidth) / 2;
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
                guiGraphics.fill(x, y, x + CELL_SIZE, y + CELL_SIZE,
                        hovered ? 0xFF46534A : 0xFF292D2B);
                if (entry.category() == CollectionCategory.EFFECTS) {
                    renderEffectIcon(guiGraphics, entry, x + 2, y + 2);
                } else {
                    final Item icon = iconFor(entry);
                    if (icon == null) {
                        guiGraphics.blit(UNKNOWN_TEXTURE, x + 2, y + 2, 0, 0, 16, 16, 16, 16);
                    } else {
                        guiGraphics.renderItem(new ItemStack(icon), x + 2, y + 2);
                    }
                }
                if (!ClientPayloadHandler.collected.contains(entry.id())) {
                    guiGraphics.fill(x, y, x + CELL_SIZE, y + CELL_SIZE, 0x80808080);
                }
                if (hovered) {
                    hoveredEntry = entry;
                }
            }
        }
        guiGraphics.disableScissor();
        renderScrollbar(guiGraphics);
        if (hoveredEntry != null) {
            guiGraphics.renderTooltip(net.minecraft.client.Minecraft.getInstance().font,
                    tooltipLines(hoveredEntry), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    protected void updateWidgetNarration(final NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }

    public boolean beginScrollbarDrag(final double mouseX, final double mouseY) {
        final int scrollbarX = scrollbarX();
        if (mouseX >= scrollbarX - 2 && mouseX <= scrollbarX + SCROLLBAR_WIDTH + 2
                && mouseY >= getY() && mouseY <= getY() + height && maxScrollOffset() > 0) {
            final int thumbHeight = scrollbarThumbHeight();
            final int thumbY = scrollbarThumbY();
            scrollbarDragActive = true;
            scrollbarDragOffset = mouseY >= thumbY && mouseY <= thumbY + thumbHeight
                    ? mouseY - thumbY
                    : thumbHeight / 2.0;
            setScrollFromMouse(mouseY);
            return true;
        }
        return false;
    }

    public void dragScrollbarTo(final double mouseY) {
        if (scrollbarDragActive) {
            setScrollFromMouse(mouseY);
        }
    }

    public void endScrollbarDrag() {
        scrollbarDragActive = false;
    }

    private double maxScrollOffset() {
        final int columns = columns();
        final int rowCount = (entries.size() + columns - 1) / columns;
        final double contentHeight = rowCount * (CELL_SIZE + CELL_GAP) - CELL_GAP;
        return Math.max(0, contentHeight - height);
    }

    private int columns() {
        final int contentWidth = width - SCROLLBAR_WIDTH - SCROLLBAR_GAP;
        return Math.max(1, Math.min(MAX_COLUMNS, (contentWidth + CELL_GAP) / (CELL_SIZE + CELL_GAP)));
    }

    private void renderScrollbar(final GuiGraphics guiGraphics) {
        final int x = scrollbarX();
        final int thumbHeight = scrollbarThumbHeight();
        final int thumbY = scrollbarThumbY();
        guiGraphics.fill(x, getY(), x + SCROLLBAR_WIDTH, getY() + height, 0xFF171A18);
        guiGraphics.fill(x, thumbY, x + SCROLLBAR_WIDTH, thumbY + thumbHeight,
                maxScrollOffset() > 0 ? 0xFF8A9A8B : 0xFF39413C);
    }

    private int scrollbarX() {
        return getX() + width - SCROLLBAR_WIDTH;
    }

    private int scrollbarThumbHeight() {
        final double contentHeight = contentHeight();
        if (contentHeight <= 0) {
            return height;
        }
        return Math.min(height, Math.max(18, (int) (height * height / contentHeight)));
    }

    private int scrollbarThumbY() {
        final int thumbHeight = scrollbarThumbHeight();
        final int travel = height - thumbHeight;
        final double maxOffset = maxScrollOffset();
        if (travel <= 0 || maxOffset <= 0) {
            return getY();
        }
        return getY() + (int) Math.round(travel * scrollOffset / maxOffset);
    }

    private double contentHeight() {
        final int columns = columns();
        final int rowCount = (entries.size() + columns - 1) / columns;
        return Math.max(height, rowCount * (CELL_SIZE + CELL_GAP) - CELL_GAP);
    }

    private void setScrollFromMouse(final double mouseY) {
        final int travel = height - scrollbarThumbHeight();
        final double maxOffset = maxScrollOffset();
        if (travel <= 0 || maxOffset <= 0) {
            scrollOffset = 0;
            return;
        }
        final double thumbTop = Math.max(0, Math.min(travel, mouseY - getY() - scrollbarDragOffset));
        scrollOffset = thumbTop / travel * maxOffset;
    }

    private static Item iconFor(final CollectibleEntry entry) {
        return switch (entry.category()) {
            case ITEMS -> BuiltInRegistries.ITEM.getOptional(entry.id()).orElse(null);
            case BLOCKS -> BuiltInRegistries.BLOCK.getOptional(entry.id())
                    .map(block -> block.asItem())
                    .filter(item -> item != Items.AIR)
                    .orElse(null);
            case ENTITIES -> BuiltInRegistries.ITEM.getOptional(spawnEggId(entry.id())).orElse(null);
            case BIOMES -> null;
            case STRUCTURES -> null;
            case EFFECTS -> null;
            case ENCHANTMENTS -> Items.ENCHANTED_BOOK;
            case ADVANCEMENTS -> Items.NETHER_STAR;
        };
    }

    private static void renderEffectIcon(final GuiGraphics guiGraphics, final CollectibleEntry entry, final int x, final int y) {
        final MobEffect effect = BuiltInRegistries.MOB_EFFECT.getOptional(entry.id()).orElse(null);
        if (effect == null) {
            guiGraphics.blit(UNKNOWN_TEXTURE, x, y, 0, 0, 16, 16, 16, 16);
            return;
        }
        final var sprite = Minecraft.getInstance().getMobEffectTextures().get(effect);
        guiGraphics.blit(x, y, 0, 16, 16, sprite);
    }

    private static List<Component> tooltipLines(final CollectibleEntry entry) {
        final String namespace = entry.id().getNamespace();
        final String modName = namespace.equals("minecraft")
                ? "Minecraft"
                : ModList.get().getModContainerById(namespace)
                        .map(container -> container.getModInfo().getDisplayName())
                        .orElse(namespace);
        return List.of(
            displayNameFor(entry),
            Component.literal(modName).withStyle(ChatFormatting.BLUE)
        );
    }

    public static Component displayNameFor(final CollectibleEntry entry) {
        if (!entry.displayName().getString().equals(entry.id().toString())) {
            return entry.displayName();
        }
        return switch (entry.category()) {
            case ITEMS -> BuiltInRegistries.ITEM.getOptional(entry.id())
                    .map(item -> new ItemStack(item).getHoverName())
                    .orElseGet(() -> Component.literal(entry.id().getPath()));
            case BLOCKS -> BuiltInRegistries.BLOCK.getOptional(entry.id())
                    .map(block -> block.getName())
                    .orElseGet(() -> Component.literal(entry.id().getPath()));
            default -> Component.literal(entry.id().getPath());
        };
    }

    private static ResourceLocation spawnEggId(final ResourceLocation entityId) {
        return ResourceLocation.fromNamespaceAndPath(entityId.getNamespace(), entityId.getPath() + "_spawn_egg");
    }
}