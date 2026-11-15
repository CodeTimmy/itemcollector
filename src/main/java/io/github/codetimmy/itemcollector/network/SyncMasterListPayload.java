package io.github.codetimmy.itemcollector.network;

import io.github.codetimmy.itemcollector.collection.CollectionCategory;
import io.github.codetimmy.itemcollector.collection.CollectibleEntry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;

public record SyncMasterListPayload(
        Map<CollectionCategory, List<CollectibleEntry>> entries
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncMasterListPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("itemcollector", "sync_master_list"));

    // StreamCodec für einen einzelnen CollectibleEntry
    private static final StreamCodec<RegistryFriendlyByteBuf, CollectibleEntry> ENTRY_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC, CollectibleEntry::id,
                    ComponentSerialization.STREAM_CODEC, CollectibleEntry::displayName,
                    ByteBufCodecs.STRING_UTF8.map(CollectionCategory::valueOf, CollectionCategory::name), CollectibleEntry::category,
                    ByteBufCodecs.COMPOUND_TAG.apply(ByteBufCodecs::optional), entry -> java.util.Optional.ofNullable(entry.extraData()),
                    (id, name, category, extraData) -> new CollectibleEntry(id, name, category, extraData.orElse(null))
            );

    // StreamCodec für die Map<CollectionCategory, List<CollectibleEntry>>
    private static final StreamCodec<RegistryFriendlyByteBuf, Map<CollectionCategory, List<CollectibleEntry>>> MAP_CODEC =
            ByteBufCodecs.map(
                    java.util.HashMap::new,
                    ByteBufCodecs.STRING_UTF8.map(CollectionCategory::valueOf, CollectionCategory::name),
                    ENTRY_CODEC.apply(ByteBufCodecs.list())
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncMasterListPayload> STREAM_CODEC =
            StreamCodec.composite(
                    MAP_CODEC, SyncMasterListPayload::entries,
                    SyncMasterListPayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
