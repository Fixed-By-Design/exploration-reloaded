package com.akitain.explorationreloaded.flight;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Elytra flight reworked after Aileron: fireworks no longer push, campfires do. Gaining altitude costs
 * infrastructure, which keeps rails and roads worth building.
 *
 * <p>This half runs on the server and owns everything worth cheating for: charges, what the enchantment
 * allows, and when a launch is armed. The velocity itself is applied by {@code FlightPhysics} on the
 * client, because the client simulates gliding and would eat any push sent from here.
 */
public final class ElytraFlight {

    /** Ticks of campfire immunity granted by a launch, so leaving the fire never singes you. */
    private static final int LAUNCH_IMMUNITY_TICKS = 20;
    /**
     * A launch fires while the player is still standing on the fire, and vanilla cancels fall flying for
     * anyone touching the ground. So the wings are re-opened every tick until the thrust has cleared it.
     */
    private static final int LAUNCH_GRACE_TICKS = 6;
    /** Lift a lone campfire gives on release: enough to clear the block, not much more. */
    private static final double LAUNCH_LIFT = 0.45;
    /** Extra lift and burn per point of hearth power, so a wide signal hearth throws you far harder. */
    private static final double LIFT_PER_POWER = 0.10;
    private static final int BOOST_TICKS_PER_POWER = 5;
    /** A launch is a longer burn than a mid-air dash, because it has to get you off the ground. */
    private static final int LAUNCH_BOOST_TICKS = 40;
    private static final int BOOST_COOLDOWN_TICKS = 60;
    /** Ticks a player must already have been gliding before a charge may be spent. */
    private static final int BOOST_STARTUP_TICKS = 10;
    private static final int SMOKE_TRAIL_TICKS = 100;
    /** A spent charge burns shorter than a launch, so the two read differently in the air. */
    private static final int DASH_BOOST_TICKS = 15;

    private ElytraFlight() {
    }

    public static void tick(ServerLevel level, ServerPlayer player) {
        tickCooldown(player);
        tickChargeDecay(player);
        FlightState.setUpdraftsEnabled(player, level.getGameRules().get(FlightRules.CAMPFIRE_UPDRAFTS));
        tickCampfireLaunch(level, player);
        tickCampfireCharging(level, player);
        tickBoost(level, player);
        tickSmokeTrail(level, player);
    }

    private static void tickCooldown(ServerPlayer player) {
        int cooldown = FlightState.boostCooldown(player);
        if (cooldown > 0) {
            FlightState.setBoostCooldown(player, cooldown - 1);
        }
        int immunity = FlightState.launchImmunity(player);
        if (immunity > 0) {
            FlightState.setLaunchImmunity(player, immunity - 1);
        }
        int grace = FlightState.launchGrace(player);
        if (grace > 0) {
            FlightState.setLaunchGrace(player, grace - 1);
        }
    }

    /** Charges are carried into flight, not hoarded: landing with nothing armed empties the tank. */
    private static void tickChargeDecay(ServerPlayer player) {
        if (player.onGround() && !player.isFallFlying() && !FlightState.isCharged(player)
                && !isRidingCampfireSmoke(player)) {
            FlightState.setCharges(player, 0);
        }
    }

    /** A rocket lit mid-glide no longer pushes; it just streams smoke behind the player for a while. */
    private static void tickSmokeTrail(ServerLevel level, ServerPlayer player) {
        int remaining = FlightState.smokeTrailTicks(player);
        if (remaining <= 0) {
            return;
        }
        if (!player.isFallFlying()) {
            FlightState.setSmokeTrailTicks(player, 0);
            return;
        }
        FlightState.setSmokeTrailTicks(player, remaining - 1);

        if (player.tickCount % 3 == 0) {
            Vec3 pos = player.position().subtract(player.getLookAngle());
            level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, pos.x, pos.y, pos.z, 2, 0.1, 0.1, 0.1, 0.005);
        }
    }

    public static void startSmokeTrail(ServerPlayer player) {
        FlightState.setSmokeTrailTicks(player, SMOKE_TRAIL_TICKS);
    }

    /**
     * A glider can charge on a lit campfire or on the closed trapdoor directly covering it.
     * Both surfaces grant the same launch and Smokestack charges.
     */
    public static boolean isRidingCampfireSmoke(Player player) {
        return chargingCampfire(player) != null;
    }

    private static @Nullable BlockPos chargingCampfire(Player player) {
        if (!player.onGround() || !player.isShiftKeyDown()
                || !player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
            return null;
        }
        return Hearth.chargingFire(player.level(), player.getOnPos());
    }

    /**
     * Releasing the crouch is what launches you: the smoke column has been charging under you, and
     * standing up lets it go. The launch is the same directional thrust a spent charge gives, so you
     * aim where you want to go before standing up.
     */
    private static void tickCampfireLaunch(ServerLevel level, ServerPlayer player) {
        if (player.isShiftKeyDown() || !FlightState.isCharged(player)) {
            return;
        }

        // Measured while charging, not now: by the time you stand up you may already be off the fire,
        // and reading signal_fire off a grass block throws.
        int power = FlightState.hearthPower(player);
        double lift = LAUNCH_LIFT + LIFT_PER_POWER * power;
        int burn = LAUNCH_BOOST_TICKS + BOOST_TICKS_PER_POWER * power;

        FlightState.setCharged(player, false);
        FlightState.setCampfireChargeTime(player, 0);
        FlightState.setBoostTicks(player, burn);
        FlightState.setLaunchImmunity(player, LAUNCH_IMMUNITY_TICKS);
        FlightState.setLaunchGrace(player, LAUNCH_GRACE_TICKS);

        // Kick upward before opening the wings: fall flying cannot survive a tick spent on the ground.
        Vec3 velocity = player.getDeltaMovement();
        push(player, new Vec3(velocity.x, Math.max(velocity.y, 0.0) + lift, velocity.z));
        player.startFallFlying();

        level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8F, 0.8F);
        Vec3 pos = player.position();
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.x, pos.y, pos.z, 30, 0.3, 0.2, 0.3, 0.05);
    }

    private static void tickCampfireCharging(ServerLevel level, ServerPlayer player) {
        BlockPos firePos = chargingCampfire(player);
        if (firePos == null) {
            FlightState.setCampfireChargeTime(player, 0);
            return;
        }

        FlightState.setHearthPower(player, Hearth.power(level, firePos));

        int elapsed = FlightState.campfireChargeTime(player) + 1;
        FlightState.setCampfireChargeTime(player, elapsed);

        int interval = Math.max(1, level.getGameRules().get(FlightRules.SMOKESTACK_CHARGE_TICKS));
        if (elapsed % interval != 0) {
            return;
        }

        // The first interval always arms the launch, even with no Smokestack: taking off must not
        // depend on a treasure enchantment. Further intervals bank charges up to the enchantment level.
        boolean bankable = FlightState.charges(player) < maxCharges(player);
        if (!bankable && FlightState.isCharged(player)) {
            return;
        }
        FlightState.setCharged(player, true);
        if (bankable) {
            grantCharge(level, player);
        }
    }

    public static void grantCharge(ServerLevel level, ServerPlayer player) {
        int charges = FlightState.charges(player);
        if (charges >= maxCharges(player)) {
            return;
        }

        FlightState.setCharges(player, charges + 1);
        level.playSound(null, player.blockPosition(), SoundEvents.CAMPFIRE_CRACKLE, SoundSource.PLAYERS,
                1.0F, 0.8F + FlightState.charges(player) * 0.2F);

        Vec3 pos = player.position();
        level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z, 20, 0.5, 0.5, 0.5, 0.1);
        level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 50, 0.5, 0.5, 0.5, 0.4);
    }

    /** Spends one charge into a short forward thrust. Returns false when there is nothing to spend. */
    public static boolean spendCharge(ServerLevel level, ServerPlayer player) {
        if (!player.isFallFlying() || FlightState.charges(player) <= 0) {
            return false;
        }
        if (FlightState.boostCooldown(player) > 0 || player.getFallFlyingTicks() <= BOOST_STARTUP_TICKS) {
            return false;
        }
        if (!player.isCreative()) {
            FlightState.setCharges(player, FlightState.charges(player) - 1);
        }
        FlightState.setBoostCooldown(player, BOOST_COOLDOWN_TICKS);

        // A mid-air dash is a short burn rather than a long one; the client turns the ticks into speed.
        FlightState.setBoostTicks(player, DASH_BOOST_TICKS);
        level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8F, 0.4F);
        return true;
    }

    private static void tickBoost(ServerLevel level, ServerPlayer player) {
        int remaining = FlightState.boostTicks(player);
        if (remaining <= 0) {
            return;
        }

        // During the grace window the wings keep being re-opened; after it, losing them ends the burn.
        boolean launching = FlightState.launchGrace(player) > 0;
        if (launching) {
            if (!player.isFallFlying()) {
                player.startFallFlying();
            }
        } else if (!player.isFallFlying()) {
            FlightState.setBoostTicks(player, 0);
            return;
        }
        FlightState.setBoostTicks(player, remaining - 1);

        if (player.tickCount % 3 == 0) {
            Vec3 pos = player.position();
            level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 2, 0.2, 0.2, 0.2, 0.1);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z, 3, 0.2, 0.2, 0.2, 0.1);
        }
    }

    private static void push(ServerPlayer player, Vec3 velocity) {
        player.setDeltaMovement(velocity);
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
    }

    private static int smokestackLevel(Player player) {
        return FlightEnchantments.smokestackLevel(player);
    }

    private static int maxCharges(ServerPlayer player) {
        return smokestackLevel(player);
    }
}
