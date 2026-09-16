package com.akitain.explorationreloaded.goat;

import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

/** Player-directed counterpart of vanilla PrepareRamNearestTarget and RamTarget. */
public final class GoatRam {
    public static final int PREPARE_TICKS = 20;
    public static final int COOLDOWN_TICKS = 100;
    public static final float SPEED = 0.6F;
    private int ticks;
    private int cooldown;
    private Vec3 origin = Vec3.ZERO;
    private Vec3 previous = Vec3.ZERO;

    public boolean begin(Goat goat, Player rider) {
        GoatMount mount = (GoatMount)goat;
        if (!(goat.level() instanceof ServerLevel level) || goat.getControllingPassenger() != rider
                || !goat.isAlive() || !goat.onGround() || goat.isInWater() || goat.isInLava()
                || cooldown > 0 || mount.exploration$ramState() != 0 || goat.hurtTime > 0
                || mount.exploration$chargeAnimation(1) > 0 || mount.exploration$rearAnimation(1) > 0
                || ((net.minecraft.world.entity.PlayerRideableJumping)goat).getJumpCooldown() > 0) return false;
        ticks = 0;
        mount.exploration$setRamState(1, goat.getYRot());
        level.broadcastEntityEvent(goat, (byte)58);
        goat.playSound(goat.isScreamingGoat() ? SoundEvents.GOAT_SCREAMING_PREPARE_RAM : SoundEvents.GOAT_PREPARE_RAM, 0.8F, 1);
        return true;
    }

    public boolean release(Goat goat, Player rider) {
        GoatMount mount = (GoatMount)goat;
        if (goat.level().isClientSide() || goat.getControllingPassenger() != rider || mount.exploration$ramState() != 1) return false;
        if (ticks < PREPARE_TICKS || !goat.onGround() || goat.isInWater() || goat.isInLava() || goat.hurtTime > 0) {
            cancel(goat);
            return false;
        }
        origin = previous = goat.position();
        ticks = 0;
        cooldown = COOLDOWN_TICKS;
        mount.exploration$setRamState(2, rider.getYRot());
        return true;
    }

    public void cancel(Goat goat) {
        if (goat.level() instanceof ServerLevel level && ((GoatMount)goat).exploration$ramState() != 0) {
            ((GoatMount)goat).exploration$setRamState(0, goat.getYRot());
            level.broadcastEntityEvent(goat, (byte)59);
            cooldown = Math.max(cooldown, 10);
            ticks = 0;
        }
    }

    public void tick(Goat goat) {
        if (!(goat.level() instanceof ServerLevel level)) return;
        if (cooldown > 0) cooldown--;
        GoatMount mount = (GoatMount)goat;
        int state = mount.exploration$ramState();
        if (state == 0) return;
        if (!(goat.getControllingPassenger() instanceof Player rider) || !goat.isAlive()
                || !goat.onGround() || goat.isInWater() || goat.isInLava() || goat.hurtTime > 0) {
            cancel(goat);
            return;
        }
        ticks++;
        if (state == 1) {
            if (ticks > 80) cancel(goat);
            return;
        }
        float yaw = mount.exploration$ramYaw() * Mth.DEG_TO_RAD;
        Vec3 direction = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 last = previous;
        var sweep = goat.getBoundingBox().expandTowards(previous.subtract(goat.position())).inflate(0.1);
        previous = goat.position();
        var targets = level.getEntitiesOfClass(LivingEntity.class, sweep, target ->
                target != rider && target != goat && target.getType() != EntityType.GOAT
                        && target.isAlive() && !target.isSpectator() && target.isAttackable()
                        && !target.isPassengerOfSameVehicle(goat) && !rider.isAlliedTo(target)
                        && (target.getType() != EntityType.ARMOR_STAND || level.getGameRules().get(GameRules.MOB_GRIEFING))
                        && (!(target instanceof Player player) || (!player.isCreative() && level.getGameRules().get(GameRules.PVP) && rider.canHarmPlayer(player)))
                        && level.getWorldBorder().isWithinBounds(target.getBoundingBox()) && goat.hasLineOfSight(target));
        targets.sort(Comparator.comparingDouble(target -> target.position().distanceToSqr(last)));
        if (!targets.isEmpty()) {
            LivingEntity target = targets.getFirst();
            // Same damage attribute, blocking factor and knockback formula as vanilla RamTarget.
            var source = level.damageSources().noAggroMobAttack(goat);
            float damage = (float)goat.getAttributeValue(Attributes.ATTACK_DAMAGE);
            if (target.hurtServer(level, source, damage)) EnchantmentHelper.doPostAttackEffects(level, target, source);
            int fast = goat.hasEffect(MobEffects.SPEED) ? goat.getEffect(MobEffects.SPEED).getAmplifier() + 1 : 0;
            int slow = goat.hasEffect(MobEffects.SLOWNESS) ? goat.getEffect(MobEffects.SLOWNESS).getAmplifier() + 1 : 0;
            float force = Mth.clamp(SPEED * 1.65F, 0.2F, 3) + 0.25F * (fast - slow);
            float block = target.applyItemBlocking(level, level.damageSources().mobAttack(goat), damage) > 0 ? 0.5F : 1;
            target.knockback(block * force * 2.5F, -direction.x, -direction.z);
            impact(goat);
            cancel(goat);
        } else if (goat.horizontalCollision || !level.noCollision(goat, goat.getBoundingBox().move(direction.scale(0.35)))) {
            BlockPos ahead = BlockPos.containing(goat.position().add(direction));
            if (level.getBlockState(ahead).is(BlockTags.SNAPS_GOAT_HORN)
                    || level.getBlockState(ahead.above()).is(BlockTags.SNAPS_GOAT_HORN)) {
                if (goat.dropHorn()) goat.playSound(SoundEvents.GOAT_HORN_BREAK, 1, 1);
            }
            impact(goat);
            cancel(goat);
        } else if (ticks >= 20 || goat.position().subtract(origin).horizontalDistanceSqr() >= 49) {
            cancel(goat);
        }
    }

    private static void impact(Goat goat) {
        goat.playSound(goat.isScreamingGoat() ? SoundEvents.GOAT_SCREAMING_RAM_IMPACT : SoundEvents.GOAT_RAM_IMPACT, 1, 1);
    }
}
