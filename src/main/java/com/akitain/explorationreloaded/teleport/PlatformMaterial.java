package com.akitain.explorationreloaded.teleport;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.villager.AbstractVillager;

public enum PlatformMaterial {
    IRON("minecraft:iron_block", 120, 0xd8e5e7),
    GOLD("minecraft:gold_block", 40, 0xffd65b),
    EMERALD("minecraft:emerald_block", 160, 0x50dc9b),
    DIAMOND("minecraft:diamond_block", 120, 0x65dfef),
    NETHERITE("minecraft:netherite_block", 120, 0xb99bd1),
    ROSE_GOLD("additionaladditions:rose_gold_block", 120, 0xf0aa91);

    public static final int REGIONAL_RANGE = 4000;
    public final String blockId;
    public final int duration;
    public final int color;

    PlatformMaterial(String blockId, int duration, int color) {
        this.blockId = blockId;
        this.duration = duration;
        this.color = color;
    }

    public static PlatformMaterial of(BlockState state) {
        if (!state.is(BlockTags.BEACON_BASE_BLOCKS)) return null;
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        for (PlatformMaterial material : values()) {
            if (material.blockId.equals(id)) return material;
        }
        // Other mods can extend the beacon tag; unfamiliar materials use the regional rules.
        return IRON;
    }

    public boolean carries(Mob mob) {
        if (mob instanceof AbstractVillager) return this == EMERALD;
        return this != IRON;
    }

    public boolean accepts(boolean sameDimension, double horizontalDistanceSquared) {
        if (!sameDimension) return this == NETHERITE;
        return this == DIAMOND || this == NETHERITE
                || horizontalDistanceSquared <= (double) REGIONAL_RANGE * REGIONAL_RANGE;
    }
}
