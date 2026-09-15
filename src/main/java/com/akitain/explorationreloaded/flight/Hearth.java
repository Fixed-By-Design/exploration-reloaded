package com.akitain.explorationreloaded.flight;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * How much of a hearth a campfire is part of, on a single scale shared by the launch and the updraft so
 * that building bigger pays off the same way in both.
 *
 * <p>All eight surrounding blocks count, so a 3x3 is worth twice a plus, and a hay bale underneath adds
 * on top rather than replacing the count: the best pad is a wide hearth that is also a signal fire.
 */
public final class Hearth {

    /** A signal fire is worth this many neighbouring campfires, on top of the ones it actually has. */
    private static final int SIGNAL_FIRE_WORTH = 4;
    public static final int MAX_POWER = 8 + SIGNAL_FIRE_WORTH;

    private Hearth() {
    }

    /** A horizontal trapdoor immediately above the fire acts as a smoke lid. */
    public static boolean isClosedTrapdoor(BlockState state) {
        return state.getBlock() instanceof TrapDoorBlock && !state.getValue(TrapDoorBlock.OPEN);
    }

    public static boolean isCovered(BlockGetter level, BlockPos firePos) {
        return isClosedTrapdoor(level.getBlockState(firePos.above()));
    }

    /** Open, dry trapdoors let the column through; closed lids and other blocks stop it. */
    public static boolean passesUpdraft(BlockState state) {
        return state.isAir() || (state.getBlock() instanceof TrapDoorBlock
                && state.getValue(TrapDoorBlock.OPEN) && state.getFluidState().isEmpty());
    }

    /** Resolve the actual heat source from the block supporting the player's feet. */
    public static @Nullable BlockPos chargingFire(BlockGetter level, BlockPos supportPos) {
        BlockPos firePos = isClosedTrapdoor(level.getBlockState(supportPos)) ? supportPos.below() : supportPos;
        return CampfireBlock.isLitCampfire(level.getBlockState(firePos)) ? firePos : null;
    }

    /** Zero for a lone campfire, up to {@link #MAX_POWER} for a signal fire ringed by eight others. */
    public static int power(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(FlightTags.CREATES_UPDRAFT) || !state.hasProperty(CampfireBlock.SIGNAL_FIRE)) {
            return 0;
        }

        int power = state.getValue(CampfireBlock.SIGNAL_FIRE) ? SIGNAL_FIRE_WORTH : 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                if (level.getBlockState(pos.offset(dx, 0, dz)).is(FlightTags.CREATES_UPDRAFT)) {
                    power++;
                }
            }
        }
        return power;
    }
}
