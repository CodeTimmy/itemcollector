package io.github.codetimmy.itemcollector.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

public record SyncProgressPayload(
        Set<ResourceLocation> collected
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncProgressPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("itemcollector", "sync_progress"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncProgressPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list())
                            .map(HashSet::new, java.util.ArrayList::new),
                    SyncProgressPayload::collected,
                    SyncProgressPayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
