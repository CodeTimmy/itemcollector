package io.github.codetimmy.itemcollector.config;

import io.github.codetimmy.itemcollector.collection.CollectionCategory;
import net.neoforged.neoforge.config.ModConfigSpec;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public class CollectionConfig {

    public static final ModConfigSpec SPEC;
    public static final Category CATEGORY;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        CATEGORY = new Category(builder);
        SPEC = builder.build();
    }

    public static class Category {
        public final ModConfigSpec.ConfigValue<Boolean> togetherModeDefault;
        public final ModConfigSpec.ConfigValue<Integer> pollIntervalTicks;
        public final Map<CollectionCategory, ModConfigSpec.ConfigValue<Boolean>> categoryEnabled;

        Category(ModConfigSpec.Builder builder) {
            builder.push("general");
            togetherModeDefault = builder
                    .comment("Whether progress is shared between all players on the server (together mode).")
                    .define("togetherModeDefault", false);
            pollIntervalTicks = builder
                    .comment("Tick interval between biome/structure discovery polls.")
                    .defineInRange("pollIntervalTicks", 20, 1, 3600);
            builder.pop();

            builder.push("categories");
            categoryEnabled = new EnumMap<>(CollectionCategory.class);
            for (CollectionCategory cat : CollectionCategory.values()) {
                categoryEnabled.put(cat, builder
                        .comment("Whether collection is enabled for the %s category.".formatted(cat.name()))
                        .define(cat.name().toLowerCase(Locale.ROOT), true));
            }
            builder.pop();
        }
    }
}
