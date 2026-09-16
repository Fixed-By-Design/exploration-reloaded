package com.akitain.explorationreloaded.mixin;

import com.akitain.explorationreloaded.ExplorationReloaded;
import com.akitain.explorationreloaded.goat.GoatJump;
import com.akitain.explorationreloaded.goat.GoatMount;
import com.akitain.explorationreloaded.goat.GoatRam;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Goat.class)
public abstract class GoatMountMixin extends Animal implements GoatMount, PlayerRideableJumping, OwnableEntity {
    @Unique private static final EntityDataAccessor<Optional<EntityReference<LivingEntity>>> EXPLORATION_OWNER =
            SynchedEntityData.defineId(Goat.class, EntityDataSerializers.OPTIONAL_LIVING_ENTITY_REFERENCE);
    @Unique private static final EntityDataAccessor<Boolean> EXPLORATION_CHARGING =
            SynchedEntityData.defineId(Goat.class, EntityDataSerializers.BOOLEAN);
    @Unique private static final EntityDataAccessor<Boolean> EXPLORATION_REARING =
            SynchedEntityData.defineId(Goat.class, EntityDataSerializers.BOOLEAN);
    @Unique private int exploration$rearTicks;
    @Unique private float exploration$rear;
    @Unique private float exploration$oldRear;
    @Unique private static final AttributeModifier MOUNTAIN_STEP = new AttributeModifier(
            ExplorationReloaded.id("goat_step"), 0.4, AttributeModifier.Operation.ADD_VALUE);
    @Unique private static final AttributeModifier MOUNTAIN_JUMP = new AttributeModifier(
            ExplorationReloaded.id("goat_jump"), GoatJump.MAX_IMPULSE - Attributes.JUMP_STRENGTH.value().getDefaultValue(),
            AttributeModifier.Operation.ADD_VALUE);
    @Unique private static final AttributeModifier TAMED_HEALTH = new AttributeModifier(
            ExplorationReloaded.id("goat_health"), 10, AttributeModifier.Operation.ADD_VALUE);
    @Unique private static final EntityDataAccessor<Integer> EXPLORATION_RAM = SynchedEntityData.defineId(Goat.class, EntityDataSerializers.INT);
    @Unique private static final EntityDataAccessor<Float> EXPLORATION_RAM_YAW = SynchedEntityData.defineId(Goat.class, EntityDataSerializers.FLOAT);
    @Unique private final GoatRam exploration$ram = new GoatRam();
    @Unique private int exploration$previousRam;
    @Unique private int exploration$temper;
    @Unique private int exploration$tamingTicks;
    @Unique private int exploration$recovery;
    @Unique private float exploration$pendingJump;
    @Unique private float exploration$charge;
    @Unique private float exploration$oldCharge;
    @Unique private float exploration$landing;
    @Unique private float exploration$oldLanding;
    @Unique private boolean exploration$localCharging;
    @Unique private boolean exploration$wasAirborne;

    protected GoatMountMixin(EntityType<? extends Animal> type, Level level) { super(type, level); }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void exploration$mountData(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(EXPLORATION_OWNER, Optional.empty());
        builder.define(EXPLORATION_CHARGING, false);
        builder.define(EXPLORATION_REARING, false);
        builder.define(EXPLORATION_RAM, 0);
        builder.define(EXPLORATION_RAM_YAW, 0F);
    }

    @Override @Nullable
    public EntityReference<LivingEntity> getOwnerReference() {
        return this.entityData.get(EXPLORATION_OWNER).orElse(null);
    }

    @Override public boolean exploration$isTame() { return getOwnerReference() != null; }

    @Override public void exploration$tame(Player player) {
        if (this.level().isClientSide()) return;
        this.entityData.set(EXPLORATION_OWNER, Optional.of(EntityReference.of(player)));
        var health = this.getAttribute(Attributes.MAX_HEALTH);
        if (!health.hasModifier(TAMED_HEALTH.id())) health.addPermanentModifier(TAMED_HEALTH);
        this.setHealth(this.getMaxHealth());
        this.setPersistenceRequired();
        this.exploration$tamingTicks = 0;
        this.getNavigation().stop();
        if (player instanceof ServerPlayer serverPlayer) CriteriaTriggers.TAME_ANIMAL.trigger(serverPlayer, this);
        exploration$particles(true);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void exploration$saveMount(ValueOutput output, CallbackInfo ci) {
        EntityReference.store(getOwnerReference(), output, "ExplorationGoatOwner");
        output.putInt("ExplorationGoatTemper", this.exploration$temper);
        // Saddle and body armour already use Minecraft's equipment save and sync paths.
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void exploration$loadMount(ValueInput input, CallbackInfo ci) {
        this.entityData.set(EXPLORATION_OWNER, Optional.ofNullable(EntityReference.readWithOldOwnerConversion(input, "ExplorationGoatOwner", this.level())));
        this.exploration$temper = Mth.clamp(input.getIntOr("ExplorationGoatTemper", 0), 0, 100);
    }

    @Override public boolean canUseSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.SADDLE || slot == EquipmentSlot.BODY
                ? this.isAlive() && !this.isBaby() && exploration$isTame() : super.canUseSlot(slot);
    }

    @Override protected boolean canDispenserEquipIntoSlot(EquipmentSlot slot) {
        return (slot == EquipmentSlot.SADDLE || slot == EquipmentSlot.BODY) && this.canUseSlot(slot)
                || super.canDispenserEquipIntoSlot(slot);
    }

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void exploration$interact(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!this.isAlive() || player.isSpectator()) return;
        ItemStack held = player.getItemInHand(hand);
        // Milk, breeding, baby growth, leads and name tags keep their native interactions.
        if (this.isBaby() || held.is(Items.BUCKET)) return;
        if (this.isFood(held)) {
            if (exploration$isTame() && this.getHealth() < this.getMaxHealth()) {
                if (!this.level().isClientSide()) {
                    this.usePlayerItem(player, hand, held);
                    this.heal(3);
                    this.playEatingSound();
                }
                cir.setReturnValue(InteractionResult.SUCCESS);
            }
            return;
        }
        if (this.isVehicle()) return;
        if (exploration$isTame()) {
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.BODY, EquipmentSlot.SADDLE}) {
                if (!held.isEmpty() && this.isEquippableInSlot(held, slot) && !this.hasItemInSlot(slot)) {
                    if (!this.level().isClientSide()) this.setItemSlotAndDropWhenKilled(slot, held.consumeAndReturn(1, player));
                    cir.setReturnValue(InteractionResult.SUCCESS);
                    return;
                }
            }
            if (held.is(Items.SHEARS)) {
                // Minecraft handles shearing the saddle before mobInteract.
                EquipmentSlot slot = EquipmentSlot.BODY;
                if (this.hasItemInSlot(slot) && (player.isCreative() || !EnchantmentHelper.has(this.getItemBySlot(slot), EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE))) {
                    if (this.level() instanceof ServerLevel server) {
                        ItemStack removed = this.getItemBySlot(slot);
                        this.setItemSlot(slot, ItemStack.EMPTY);
                        this.spawnAtLocation(server, removed);
                        held.hurtAndBreak(1, player, hand.asEquipmentSlot());
                        this.playSound(SoundEvents.SADDLE_UNEQUIP.value(), 1, 1);
                    }
                    cir.setReturnValue(InteractionResult.SUCCESS);
                    return;
                }
            }
        }
        if (held.isEmpty() && !player.isSecondaryUseActive() && !this.isInWater()) {
            if (!this.level().isClientSide()) {
                this.getNavigation().stop();
                this.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                this.getBrain().eraseMemory(MemoryModuleType.RAM_TARGET);
                this.getBrain().eraseMemory(MemoryModuleType.LONG_JUMP_MID_JUMP);
                this.setPose(Pose.STANDING);
                this.level().broadcastEntityEvent(this, (byte)59);
                player.setYRot(this.getYRot());
                player.startRiding(this);
            }
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    @Inject(method = "customServerAiStep", at = @At("HEAD"), cancellable = true)
    private void exploration$riderControlsBrain(ServerLevel level, CallbackInfo ci) {
        if (exploration$isTame()) {
            // A trusted mountain companion only rams when directed by its rider.
            this.getBrain().eraseMemory(MemoryModuleType.RAM_TARGET);
            this.getBrain().setMemory(MemoryModuleType.RAM_COOLDOWN_TICKS, 200);
        }
        if (this.isVehicle()) {
            this.getNavigation().stop();
            this.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            this.setXxa(0);
            this.setZza(0);
            ci.cancel();
        }
    }

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void exploration$tickMount(CallbackInfo ci) {
        this.exploration$ram.tick((Goat)(Object)this);
        int ram = exploration$ramState();
        if (this.exploration$previousRam == 2 && ram == 0 && this.isLocalInstanceAuthoritative()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.2, 1, 0.2));
        }
        this.exploration$previousRam = ram;
        if (this.exploration$recovery > 0) this.exploration$recovery--;
        if ((!this.level().isClientSide() || this.exploration$rearTicks > 0) && this.entityData.get(EXPLORATION_REARING)) {
            if (--this.exploration$rearTicks <= 0 || this.getControllingPassenger() == null) {
                this.entityData.set(EXPLORATION_REARING, false);
            }
        }
        this.exploration$oldRear = this.exploration$rear;
        this.exploration$rear = GoatJump.standAnimation(this.exploration$rear, this.entityData.get(EXPLORATION_REARING));
        this.exploration$oldCharge = this.exploration$charge;
        this.exploration$oldLanding = this.exploration$landing;
        this.exploration$landing = Math.max(0, this.exploration$landing - 0.16F);
        if (this.onGround() && this.exploration$wasAirborne && this.isVehicle()) {
            this.exploration$recovery = GoatJump.RECOVERY_TICKS;
            this.exploration$landing = 1;
            if (!this.level().isClientSide()) this.playSound(SoundEvents.GOAT_STEP, 0.55F, 0.8F);
        }
        this.exploration$wasAirborne = !this.onGround();
        if (!this.level().isClientSide()) {
            boolean charging = this.getControllingPassenger() instanceof ServerPlayer rider
                    && rider.getLastClientInput().jump() && this.onGround() && this.exploration$recovery == 0 && !this.entityData.get(EXPLORATION_REARING) && exploration$ramState() == 0;
            this.entityData.set(EXPLORATION_CHARGING, charging);
            if (this.getFirstPassenger() instanceof Player rider && !exploration$isTame()) {
                if (++this.exploration$tamingTicks >= 40) {
                    this.exploration$tamingTicks = 0;
                    this.exploration$temper = Math.min(100, this.exploration$temper + 20);
                    if (this.random.nextInt(100) < this.exploration$temper) {
                        exploration$tame(rider);
                    } else {
                        this.ejectPassengers();
                        exploration$particles(false);
                        Goat goat = (Goat)(Object)this;
                        this.playSound(goat.isScreamingGoat() ? SoundEvents.GOAT_SCREAMING_PREPARE_RAM : SoundEvents.GOAT_PREPARE_RAM, 0.6F, 1.1F);
                    }
                }
            } else this.exploration$tamingTicks = 0;
        }
        boolean charging = this.level().isClientSide() && this.isLocalInstanceAuthoritative()
                ? this.exploration$localCharging : this.entityData.get(EXPLORATION_CHARGING);
        charging &= this.isVehicle() && this.onGround() && this.exploration$recovery == 0 && !this.entityData.get(EXPLORATION_REARING) && exploration$ramState() == 0;
        this.exploration$charge = Mth.clamp(this.exploration$charge + (charging ? 0.14F : -0.25F), 0, 1);
        var step = this.getAttribute(Attributes.STEP_HEIGHT);
        var jump = this.getAttribute(Attributes.JUMP_STRENGTH);
        if (this.getControllingPassenger() != null) {
            if (!step.hasModifier(MOUNTAIN_STEP.id())) step.addTransientModifier(MOUNTAIN_STEP);
            if (!jump.hasModifier(MOUNTAIN_JUMP.id())) jump.addTransientModifier(MOUNTAIN_JUMP);
        } else {
            step.removeModifier(MOUNTAIN_STEP.id());
            jump.removeModifier(MOUNTAIN_JUMP.id());
            this.exploration$pendingJump = 0;
            this.exploration$localCharging = false;
        }
    }

    @Unique private void exploration$particles(boolean success) {
        if (this.level() instanceof ServerLevel server) server.sendParticles(success ? ParticleTypes.HEART : ParticleTypes.SMOKE,
                this.getX(), this.getY() + 1, this.getZ(), 7, 0.3, 0.3, 0.3, 0.02);
    }

    @Override @Nullable public LivingEntity getControllingPassenger() {
        return exploration$isTame() && this.isSaddled() && this.getFirstPassenger() instanceof Player rider
                ? rider : null;
    }

    @Override protected void tickRidden(Player rider, Vec3 input) {
        super.tickRidden(rider, input);
        this.setRot(exploration$ramState() == 2 ? exploration$ramYaw() : rider.getYRot(), rider.getXRot() * 0.5F);
        this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();
        if (this.isLocalInstanceAuthoritative() && this.exploration$pendingJump > 0) {
            if (this.onGround() && this.exploration$recovery == 0 && exploration$ramState() == 0 && !this.isInWater() && !this.isInLava()) {
                exploration$executeRidersJump(this.exploration$pendingJump, input);
            }
            this.exploration$pendingJump = 0;
        }
    }

    @Override protected Vec3 getRiddenInput(Player rider, Vec3 selfInput) {
        if (exploration$ramState() == 2) return new Vec3(0, 0, 1);
        if (exploration$ramState() == 1) return Vec3.ZERO;
        float forward = rider.zza > 0 ? rider.zza : rider.zza * 0.25F;
        return new Vec3(rider.xxa * 0.5F, 0, forward);
    }

    @Override protected float getRiddenSpeed(Player rider) {
        if (exploration$ramState() == 2) return GoatRam.SPEED;
        return (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED) * (rider.isSprinting() ? 1.25F : 0.9F);
    }

    @Override public boolean canSprint() { return canJump(); }

    @Override public boolean canJump() { return exploration$isTame() && this.isSaddled() && !this.isBaby(); }
    @Override public int getJumpCooldown() { return Math.max(this.exploration$recovery, (exploration$ramState() != 0) ? 1 : 0); }

    @Override public void onPlayerJump(int amount) {
        if (canJump() && this.onGround() && this.exploration$recovery == 0 && exploration$ramState() == 0 && !this.isInWater() && !this.isInLava()) {
            if (amount >= 0) exploration$standForJump();
            this.exploration$pendingJump = this.getPlayerJumpPendingScale(Math.max(0, amount));
        }
    }

    @Override public void handleStartJump(int amount) {
        if (canJump() && this.onGround() && this.exploration$recovery == 0 && exploration$ramState() == 0 && !this.isInWater() && !this.isInLava()) {
            exploration$standForJump();
            Goat goat = (Goat)(Object)this;
            this.playSound(goat.isScreamingGoat() ? SoundEvents.GOAT_SCREAMING_LONG_JUMP : SoundEvents.GOAT_LONG_JUMP, 0.8F, 1);
            this.entityData.set(EXPLORATION_CHARGING, false);
        }
    }
    @Override public void handleStopJump() { }

    @Unique private void exploration$standForJump() {
        // AbstractHorse starts the stand on release, with sliding allowed during the leap.
        this.exploration$rearTicks = GoatJump.STAND_TICKS;
        this.entityData.set(EXPLORATION_REARING, true);
    }

    @Unique private void exploration$executeRidersJump(float amount, Vec3 input) {
        // Use LivingEntity's native block and potion modifiers, as AbstractHorse does.
        Vec3 motion = this.getDeltaMovement();
        this.setDeltaMovement(motion.x, this.getJumpPower(amount), motion.z);
        this.needsSync = true;
        if (input.z > 0) {
            float yaw = this.getYRot() * Mth.DEG_TO_RAD;
            this.setDeltaMovement(this.getDeltaMovement().add(-Mth.sin(yaw) * GoatJump.FORWARD_IMPULSE * amount,
                    0, Mth.cos(yaw) * GoatJump.FORWARD_IMPULSE * amount));
        }
    }

    @Override public boolean exploration$prepareRam(Player rider) { return this.exploration$ram.begin((Goat)(Object)this, rider); }
    @Override public boolean exploration$releaseRam(Player rider) { return this.exploration$ram.release((Goat)(Object)this, rider); }
    @Override public void exploration$cancelRam() { this.exploration$ram.cancel((Goat)(Object)this); }
    @Override public int exploration$ramState() { return this.entityData.get(EXPLORATION_RAM); }
    @Override public float exploration$ramYaw() { return this.entityData.get(EXPLORATION_RAM_YAW); }
    @Override public void exploration$setRamState(int state, float yaw) {
        this.entityData.set(EXPLORATION_RAM_YAW, yaw);
        this.entityData.set(EXPLORATION_RAM, state);
    }

    @Override protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        // Follow the saddle as the goat rises onto its hind legs.
        Vec3 point = super.getPassengerAttachmentPoint(passenger, dimensions, scale);
        float rear = this.exploration$oldRear;
        double fittedScale = scale * GoatJump.RIDER_STAND_SCALE;
        return point.add(new Vec3(0, 0.15 * rear * fittedScale, -0.7 * rear * fittedScale)
                .yRot(-this.getYRot() * Mth.DEG_TO_RAD));
    }

    @Override protected void propagateFallToPassengers(double distance, float multiplier, DamageSource source) {
        super.propagateFallToPassengers(Math.max(0, distance - Goat.GOAT_FALL_DAMAGE_REDUCTION), multiplier, source);
    }

    @Override public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        BlockPos origin = this.blockPosition();
        for (int dy : new int[]{0, 1, -1}) {
            for (int[] offset : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, 1}, {1, -1}, {-1, -1}}) {
                Vec3 candidate = DismountHelper.findSafeDismountLocation(passenger.getType(), this.level(), origin.offset(offset[0], dy, offset[1]), true);
                if (candidate != null) return candidate;
            }
        }
        return super.getDismountLocationForPassenger(passenger);
    }

    @Override public float exploration$rearAnimation(float partialTick) { return Mth.lerp(partialTick, exploration$oldRear, exploration$rear); }
    @Override public float exploration$chargeAnimation(float partialTick) { return Mth.lerp(partialTick, exploration$oldCharge, exploration$charge); }
    @Override public float exploration$landingAnimation(float partialTick) { return Mth.lerp(partialTick, exploration$oldLanding, exploration$landing); }
    @Override public void exploration$setLocalCharging(boolean charging) { this.exploration$localCharging = charging; }
}
