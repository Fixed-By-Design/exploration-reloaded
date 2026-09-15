package com.akitain.explorationreloaded.teleport;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

final class TeleportEffects {
    private TeleportEffects() {}

    static void message(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.translatable("teleport.exploration-reloaded." + key), true);
    }

    static void loading(ServerPlayer player) {
        message(player, "loading");
    }

    static void destination(ServerPlayer player, Component name) {
        player.sendSystemMessage(Component.translatable("teleport.exploration-reloaded.travel_destination", name), true);
    }

    static void status(ServerPlayer player, int count, int creatures, int remaining) {
        Component countdown = Component.translatable("teleport.exploration-reloaded.countdown", (remaining + 19) / 20);
        if (count > 1 || creatures > 0) {
            var travellers = Component.translatable(count == 1
                    ? "teleport.exploration-reloaded.one_player" : "teleport.exploration-reloaded.players", count);
            if (creatures > 0) {
                travellers.append(Component.translatable("teleport.exploration-reloaded.with_creatures",
                        Component.translatable(creatures == 1
                                ? "teleport.exploration-reloaded.one_creature" : "teleport.exploration-reloaded.creatures", creatures)));
            }
            countdown = Component.translatable("teleport.exploration-reloaded.group_countdown", countdown, travellers);
        }
        player.sendSystemMessage(countdown, true);
    }

    static void invite(ServerPlayer player, Component name) {
        player.sendSystemMessage(Component.translatable("teleport.exploration-reloaded.invite", name), true);
    }

    static void begin(TeleportPlatform platform) {
        sound(platform, SoundEvents.BEACON_ACTIVATE, 0.65f, 0.7f);
    }

    static void joined(ServerPlayer player, Component name) {
        player.level().playSound(null, player.blockPosition(), SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.BLOCKS, 0.55f, 1.2f);
        destination(player, name);
    }

    static void pulse(TeleportPlatform platform, int remaining, int duration) {
        float progress = 1f - (float) remaining / duration;
        sound(platform, SoundEvents.AMETHYST_BLOCK_CHIME, 0.35f + progress * 0.15f, 0.7f + progress * 0.9f);
    }

    static void boundary(TeleportPlatform platform, int remaining, int duration) {
        double minX = platform.lodestone().getX() - 3 + 0.06;
        double minZ = platform.lodestone().getZ() - 3 + 0.06;
        double side = 6.88;
        DustParticleOptions dust = new DustParticleOptions(platform.material().color, 0.65f);
        for (int i = 0; i < 12; i++) {
            double offset = ((i + (duration - remaining) / 20.0) % 12) / 12.0 * side;
            point(platform, dust, minX + offset, minZ);
            point(platform, dust, minX + side, minZ + offset);
            point(platform, dust, minX + side - offset, minZ + side);
            point(platform, dust, minX, minZ + side - offset);
        }
        platform.level().sendParticles(ParticleTypes.ENCHANT,
                platform.lodestone().getX() + 0.5, platform.lodestone().getY() + 1.15,
                platform.lodestone().getZ() + 0.5, 2, 0.25, 0.15, 0.25, 0.1);
    }

    private static void point(TeleportPlatform platform, DustParticleOptions dust, double x, double z) {
        platform.level().sendParticles(dust, x, platform.surfaceHeight(x, z), z, 1, 0, 0, 0, 0);
    }

    static void selected(Entity player, int color, int remaining) {
        ServerLevel level = (ServerLevel) player.level();
        DustParticleOptions dust = new DustParticleOptions(color, 0.8f);
        double y = (player instanceof ServerPlayer ? player.getRootVehicle().getY() : player.getY()) + 0.12;
        for (int i = 0; i < 6; i++) {
            double angle = i * Math.PI / 3 + remaining * 0.06;
            level.sendParticles(dust, player.getX() + Math.cos(angle) * 0.45, y,
                    player.getZ() + Math.sin(angle) * 0.45, 1, 0, 0, 0, 0);
        }
        if (remaining % 10 == 0) level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                player.getX(), player.getY() + 0.6, player.getZ(), 2, 0.2, 0.35, 0.2, 0.02);
    }

    static void depart(TeleportPlatform platform) {
        burst(platform, ParticleTypes.REVERSE_PORTAL);
        sound(platform, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), 0.65f, 1.2f);
    }

    static void arrive(TeleportPlatform platform) {
        burst(platform, ParticleTypes.PORTAL);
        sound(platform, SoundEvents.ENDERMAN_TELEPORT, 0.8f, 0.85f);
    }

    static void cancel(TeleportPlatform platform) {
        sound(platform, SoundEvents.BEACON_DEACTIVATE, 0.5f, 1f);
    }

    private static void burst(TeleportPlatform platform, net.minecraft.core.particles.SimpleParticleType particle) {
        platform.level().sendParticles(particle, platform.lodestone().getX() + 0.5,
                platform.lodestone().getY() + 1.2, platform.lodestone().getZ() + 0.5, 70, 1.8, 0.45, 1.8, 0.12);
    }

    private static void sound(TeleportPlatform platform, SoundEvent sound, float volume, float pitch) {
        platform.level().playSound(null, platform.lodestone(), sound, SoundSource.BLOCKS, volume, pitch);
    }
}
