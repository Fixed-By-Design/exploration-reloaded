package com.akitain.explorationreloaded.goat;

import net.minecraft.world.entity.player.Player;

/** Extra behaviour on the existing minecraft:goat, not a replacement entity. */
public interface GoatMount {
    boolean exploration$isTame();
    void exploration$tame(Player player);
    boolean exploration$prepareRam(Player rider);
    boolean exploration$releaseRam(Player rider);
    void exploration$cancelRam();
    int exploration$ramState();
    float exploration$ramYaw();
    void exploration$setRamState(int state, float yaw);
    float exploration$rearAnimation(float partialTick);
    float exploration$chargeAnimation(float partialTick);
    float exploration$landingAnimation(float partialTick);
    void exploration$setLocalCharging(boolean charging);
}
