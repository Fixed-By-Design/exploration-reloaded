package com.akitain.explorationreloaded.goat;

/** Goat tuning around the native horse riding and standing mechanics. */
public final class GoatJump {
    public static final int RECOVERY_TICKS = 12;
    public static final int STAND_TICKS = 20;
    public static final double MAX_IMPULSE = 1.5;
    public static final double FORWARD_IMPULSE = 0.55;
    public static final double RIDER_STAND_SCALE = 0.65;

    private GoatJump() {}

    public static float standAnimation(float current, boolean standing) {
        // AbstractHorse's fast rise and eased return, used by a rider's jump.
        return standing ? Math.min(1, current + (1 - current) * 0.4F + 0.05F)
                : Math.max(0, current + (0.8F * current * current * current - current) * 0.6F - 0.05F);
    }
}
