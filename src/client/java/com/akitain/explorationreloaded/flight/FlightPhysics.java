package com.akitain.explorationreloaded.flight;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Glide physics runs here, on the client, because the client owns elytra movement: it recomputes its
 * velocity from the glide model every tick and reports the result. Pushing from the server means every
 * impulse is half eaten before it lands, which is why upstream applies all of this locally too.
 *
 * <p>What stays on the server is everything worth cheating for: how many charges you hold, whether you
 * may spend one, and what the enchantment allows. A client that lies here can only fly badly.
 */
public final class FlightPhysics {

    private static final int MAX_UPDRAFT_DEPTH = 38;
    private static final int CAMPFIRE_RANGE = 10;
    /** Same hearth scale as the launch, so building wider pays off in the air too. */
    private static final int RANGE_PER_POWER = 2;
    private static final double MAX_LIFT_SPEED = 1.0;

    private static final double CLOUD_LAYER = 100.0;
    private static final double CLOUDSKIPPER_CEILING = 230.0;
    private static final double GLIDE_DRAG = 0.99;
    private static final double MAX_DRAG_RECOVERY = 0.6;

    private FlightPhysics() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(FlightPhysics::tick);
    }

    private static void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || !player.isFallFlying()) {
            return;
        }

        applyBoost(player);
        applyUpdraft(player);
        applyCloudskipper(player);
    }

    /** Steers towards the look vector rather than simply adding speed, so a boost redirects the glide. */
    private static void applyBoost(LocalPlayer player) {
        if (FlightState.boostTicks(player) <= 0) {
            return;
        }
        Vec3 look = player.getLookAngle();
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.add(
                look.x * 0.1 + (look.x * 1.5 - velocity.x) * 0.5,
                look.y * 0.1 + (look.y * 1.5 - velocity.y) * 0.5,
                look.z * 0.1 + (look.z * 1.5 - velocity.z) * 0.5));
    }

    private static void applyUpdraft(LocalPlayer player) {
        if (!FlightState.updraftsEnabled(player)) {
            return;
        }

        Level level = player.level();
        BlockPos.MutableBlockPos pos = player.blockPosition().mutable();
        int depth = 0;
        while (depth < MAX_UPDRAFT_DEPTH && level.isInsideBuildHeight(pos.getY())
                && Hearth.passesUpdraft(level.getBlockState(pos))) {
            depth++;
            pos.move(Direction.DOWN);
        }

        BlockState state = level.getBlockState(pos);
        if (!state.is(FlightTags.CREATES_UPDRAFT) || !state.getValue(CampfireBlock.LIT)) {
            return;
        }

        int range = CAMPFIRE_RANGE + RANGE_PER_POWER * Hearth.power(level, pos);
        double distance = Math.abs(pos.getY() - player.getY());
        if (distance <= 0 || distance > range) {
            return;
        }

        double lift = Math.min(range / distance / 7.0, 1.0);
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.x, Math.min(velocity.y + lift, MAX_LIFT_SPEED), velocity.z);

    }

    /** Gives back part of the drag instead of adding thrust, so it lengthens a glide rather than speeding it. */
    private static void applyCloudskipper(LocalPlayer player) {
        int enchantLevel = FlightEnchantments.cloudskipperLevel(player);
        if (enchantLevel <= 0) {
            return;
        }

        double y = player.getY();
        if (y < CLOUD_LAYER) {
            return;
        }
        double altitude = y >= CLOUDSKIPPER_CEILING ? 1.0 : 0.00006 * Math.pow(y - CLOUD_LAYER, 2);
        double factor = Math.min(altitude, 1.0) * MAX_DRAG_RECOVERY * (enchantLevel / 3.0);
        double recovered = factor * (1.0 / GLIDE_DRAG - 1.0);

        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.add(velocity.x * recovered, 0.0, velocity.z * recovered));
    }

}
