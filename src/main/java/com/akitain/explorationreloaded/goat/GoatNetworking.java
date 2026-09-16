package com.akitain.explorationreloaded.goat;

import com.akitain.explorationreloaded.ExplorationReloaded;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class GoatNetworking {
    public record RamPayload(int action) implements CustomPacketPayload {
        public static final Type<RamPayload> ID = new Type<>(ExplorationReloaded.id("goat_ram"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RamPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, RamPayload::action, RamPayload::new);
        @Override public Type<RamPayload> type() { return ID; }
    }

    private GoatNetworking() {}

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(RamPayload.ID, RamPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(RamPayload.ID, (payload, context) -> {
            var rider = context.player();
            if (rider.getControlledVehicle() instanceof GoatMount goat) {
                switch (payload.action()) {
                    case 0 -> goat.exploration$prepareRam(rider);
                    case 1 -> goat.exploration$releaseRam(rider);
                    case 2 -> goat.exploration$cancelRam();
                    default -> { }
                }
            }
        });
    }
}
