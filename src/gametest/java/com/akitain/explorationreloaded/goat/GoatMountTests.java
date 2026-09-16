package com.akitain.explorationreloaded.goat;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.ProblemReporter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

public class GoatMountTests {
    @GameTest(structure = "exploration-reloaded-gametest:platforms", maxTicks = 80)
    public void mountedRamUsesVanillaDamageAndStopsAtItsFirstTarget(GameTestHelper h) {
        ramFloor(h);
        Goat goat = h.spawn(EntityType.GOAT, 4, 2, 3);
        Player rider = ramRider(h, goat);
        var target = h.spawn(EntityType.COW, 4, 2, 7);
        target.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
        double initialZ = target.getZ();
        GoatMount mount = (GoatMount)goat;
        h.runAtTickTime(16, () -> h.assertTrue(mount.exploration$prepareRam(rider), "Mounted ram accepts its rider on solid ground"));
        h.runAtTickTime(38, () -> h.assertTrue(mount.exploration$releaseRam(rider), "A full native preparation can launch; state=" + mount.exploration$ramState() + " ground=" + goat.onGround()));
        h.runAtTickTime(65, () -> {
            h.assertTrue(target.getHealth() == 8, "An adult goat deals its native two damage, exactly once");
            h.assertTrue(target.getZ() > initialZ, "The target is pushed in the charge direction");
            h.assertTrue(mount.exploration$ramState() == 0, "The first hit ends the charge");
            h.assertFalse(mount.exploration$prepareRam(rider), "Repeated rams respect the server cooldown");
            h.assertTrue(rider.getHealth() == 20, "The rider is never hit by its own charge");
            h.assertTrue(goat.hasLeftHorn() && goat.hasRightHorn(), "Hitting a creature preserves horns");
            h.succeed();
        });
    }

    @GameTest(structure = "exploration-reloaded-gametest:platforms", maxTicks = 80)
    public void rammingStoneDropsOneRealGoatHorn(GameTestHelper h) {
        ramFloor(h);
        for (int x = 3; x <= 5; x++) for (int y = 2; y <= 4; y++) h.setBlock(new BlockPos(x, y, 7), Blocks.STONE);
        Goat goat = h.spawn(EntityType.GOAT, 4, 2, 3);
        Player rider = ramRider(h, goat);
        GoatMount mount = (GoatMount)goat;
        h.runAtTickTime(16, () -> h.assertTrue(mount.exploration$prepareRam(rider), "Preparation starts on settled ground"));
        h.runAtTickTime(38, () -> h.assertTrue(mount.exploration$releaseRam(rider), "The charge is prepared; state=" + mount.exploration$ramState() + " ground=" + goat.onGround()));
        h.runAtTickTime(65, () -> {
            h.assertTrue(goat.hasLeftHorn() != goat.hasRightHorn(), "Only one horn breaks against stone");
            var horns = h.getLevel().getEntitiesOfClass(ItemEntity.class, goat.getBoundingBox().inflate(8), e -> e.getItem().is(Items.GOAT_HORN));
            int dropped = horns.stream().mapToInt(e -> e.getItem().getCount()).sum();
            int collected = rider.getInventory().countItem(Items.GOAT_HORN);
            h.assertTrue(dropped + collected == 1, "One native instrument item drops or is collected; dropped=" + dropped + " collected=" + collected);
            h.assertTrue(mount.exploration$ramState() == 0, "A wall stops the ram");
            h.succeed();
        });
    }

    @GameTest
    public void rammingRequiresPreparationAndCannotOverlapOtherActions(GameTestHelper h) {
        Goat goat = h.spawn(EntityType.GOAT, 1, 2, 1);
        Player rider = ramRider(h, goat);
        Player stranger = h.makeMockPlayer(GameType.SURVIVAL);
        GoatMount mount = (GoatMount)goat;
        h.assertFalse(mount.exploration$prepareRam(stranger), "Another player cannot control this goat");
        h.assertFalse(mount.exploration$releaseRam(rider), "Release without preparation cannot launch");
        goat.setOnGround(false);
        h.assertFalse(mount.exploration$prepareRam(rider), "Preparation requires solid ground");
        goat.setOnGround(true);
        h.assertTrue(mount.exploration$prepareRam(rider), "The rider can prepare");
        h.assertTrue(((PlayerRideableJumping)goat).getJumpCooldown() > 0, "Jumping is unavailable while preparing");
        h.assertFalse(mount.exploration$releaseRam(rider), "A quick release cannot skip the wind-up");
        h.assertTrue(mount.exploration$ramState() == 0, "An incomplete charge cancels cleanly");
        h.succeed();
    }

    private static Player ramRider(GameTestHelper h, Goat goat) {
        Player rider = h.makeMockPlayer(GameType.SURVIVAL);
        ((GoatMount)goat).exploration$tame(rider);
        goat.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        rider.setYRot(0);
        rider.startRiding(goat);
        goat.setOnGround(true);
        return rider;
    }

    private static void ramFloor(GameTestHelper h) {
        for (int x = 1; x <= 9; x++) for (int z = 1; z <= 14; z++) {
            h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            for (int y = 2; y < 5; y++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
        }
    }

    @GameTest
    public void existingGoatsBecomeMountsWithoutReplacingTheirIdentity(GameTestHelper h) {
        Goat goat = h.spawn(EntityType.GOAT, 1, 2, 1);
        Player rider = h.makeMockPlayer(GameType.SURVIVAL);
        h.assertFalse(((GoatMount)goat).exploration$isTame(), "Wild goats start wild");
        h.assertTrue(goat.getAttributeValue(Attributes.JUMP_STRENGTH) < 0.5, "Unridden goats keep their native ordinary jump strength");
        h.assertFalse(goat.canUseSlot(EquipmentSlot.SADDLE), "Wild goats cannot be saddled");
        goat.interact(rider, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(rider.getVehicle() == goat, "Bare hand mounts an existing vanilla goat");
        h.assertTrue(goat.getControllingPassenger() == null, "Wild riders cannot steer");
        ((GoatMount)goat).exploration$tame(rider);
        h.assertValueEqual(goat.getType(), EntityType.GOAT, "Taming keeps the vanilla entity type");
        h.assertTrue(goat.hasLeftHorn() && goat.hasRightHorn(), "Taming preserves both horns");
        h.assertTrue(goat.getMaxHealth() == 20, "Tamed goats have ten hearts");
        rider.stopRiding();
        rider.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SADDLE));
        goat.interact(rider, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(goat.isSaddled() && rider.getMainHandItem().isEmpty(), "A native saddle equips and is consumed once");
        goat.interact(rider, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(goat.getControllingPassenger() == rider, "A tamed saddled goat is steerable");
        h.assertTrue(((PlayerRideableJumping)goat).canJump(), "Native horse jump controls are available");
        h.succeed();
    }

    @GameTest(maxTicks = 260)
    public void patientRidingActuallyTamesAWildGoat(GameTestHelper h) {
        Goat goat = h.spawn(EntityType.GOAT, 1, 2, 1);
        Player rider = h.makeMockPlayer(GameType.SURVIVAL);
        for (int tick = 1; tick <= 240; tick++) h.runAtTickTime(tick, () -> {
            if (!((GoatMount)goat).exploration$isTame() && !rider.isPassenger()) {
                goat.interact(rider, InteractionHand.MAIN_HAND, Vec3.ZERO);
            }
        });
        h.runAtTickTime(245, () -> {
            h.assertTrue(((GoatMount)goat).exploration$isTame(), "Repeated bareback attempts eventually earn trust");
            h.assertTrue(rider.getVehicle() == goat, "Successful taming keeps the rider mounted");
            h.succeed();
        });
    }

    @GameTest
    public void equippedItemsAreNotLostWhenTheGoatDies(GameTestHelper h) {
        Goat goat = h.spawn(EntityType.GOAT, 1, 2, 1);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        ((GoatMount)goat).exploration$tame(player);
        for (var item : new net.minecraft.world.item.Item[]{Items.SADDLE, Items.DIAMOND_HORSE_ARMOR}) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
            goat.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        }
        goat.hurtServer(h.getLevel(), goat.damageSources().genericKill(), 1000);
        var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, goat.getBoundingBox().inflate(3));
        h.assertTrue(drops.stream().filter(e -> e.getItem().is(Items.SADDLE)).mapToInt(e -> e.getItem().getCount()).sum() == 1, "Death returns exactly one saddle");
        h.assertTrue(drops.stream().filter(e -> e.getItem().is(Items.DIAMOND_HORSE_ARMOR)).mapToInt(e -> e.getItem().getCount()).sum() == 1, "Death returns exactly one armour");
        h.succeed();
    }

    @GameTest
    public void equipmentOwnerAndVanillaTraitsSurviveSaving(GameTestHelper h) {
        Goat goat = h.spawn(EntityType.GOAT, 1, 2, 1);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        ((GoatMount)goat).exploration$tame(player);
        goat.setScreamingGoat(true);
        goat.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        goat.setItemSlot(EquipmentSlot.BODY, new ItemStack(Items.DIAMOND_HORSE_ARMOR));
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, h.getLevel().registryAccess());
        goat.saveWithoutId(output);
        Goat loaded = EntityType.GOAT.create(h.getLevel(), EntitySpawnReason.LOAD);
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), output.buildResult()));
        h.assertTrue(((GoatMount)loaded).exploration$isTame(), "Taming survives a reload");
        h.assertValueEqual(((OwnableEntity)loaded).getOwnerReference().getUUID(), player.getUUID(), "The owner survives a reload");
        h.assertTrue(loaded.isSaddled() && loaded.isWearingBodyArmor(), "Native equipment survives a reload");
        h.assertTrue(loaded.getBodyArmorItem().is(Items.DIAMOND_HORSE_ARMOR), "The exact armour item is preserved");
        h.assertTrue(loaded.getMaxHealth() == 20, "Health upgrade survives without duplication");
        h.assertTrue(loaded.isScreamingGoat() && loaded.hasLeftHorn() && loaded.hasRightHorn(), "Vanilla goat traits survive");
        h.succeed();
    }

    @GameTest
    public void armourProtectsAndShearsReturnEachPieceOnce(GameTestHelper h) {
        Goat goat = h.spawn(EntityType.GOAT, 1, 2, 1);
        goat.setNoAi(true);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        ((GoatMount)goat).exploration$tame(player);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_HORSE_ARMOR));
        goat.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SADDLE));
        goat.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.runAtTickTime(3, () -> {
            h.assertTrue(goat.getAttributeValue(Attributes.ARMOR) > 0, "Native body equipment really protects the goat");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
            goat.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
            h.assertFalse(goat.isSaddled(), "Vanilla shearing removes the saddle first");
            h.assertTrue(goat.isWearingBodyArmor(), "The armour stays equipped until the next use");
            goat.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
            goat.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
            h.assertFalse(goat.isWearingBodyArmor(), "The next shear removes the armour");
            var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, goat.getBoundingBox().inflate(3));
            h.assertTrue(drops.stream().filter(e -> e.getItem().is(Items.SADDLE)).mapToInt(e -> e.getItem().getCount()).sum() == 1, "Saddle returned exactly once");
            h.assertTrue(drops.stream().filter(e -> e.getItem().is(Items.IRON_HORSE_ARMOR)).mapToInt(e -> e.getItem().getCount()).sum() == 1, "Armour returned exactly once");
            h.getLevel().getServer().getPlayerList().remove(player);
            h.succeed();
        });
    }

    @GameTest
    public void milkBreedingAndBabiesKeepVanillaInteractions(GameTestHelper h) {
        Goat goat = h.spawn(EntityType.GOAT, 1, 2, 1);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        ((GoatMount)goat).exploration$tame(player);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        goat.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(player.getMainHandItem().is(Items.MILK_BUCKET), "Tamed goats still produce milk");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT, 2));
        goat.setHealth(15);
        goat.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(goat.getHealth() == 18 && player.getMainHandItem().getCount() == 1, "Wheat heals three health at its normal item cost");
        goat.setHealth(20);
        goat.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(goat.isInLove(), "A healthy adult still breeds with wheat");
        Goat baby = h.spawn(EntityType.GOAT, 2, 2, 1);
        baby.setBaby(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        baby.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        h.assertTrue(player.getVehicle() == null, "Babies cannot be ridden");
        h.assertFalse(baby.canUseSlot(EquipmentSlot.SADDLE), "Babies cannot wear a saddle");
        h.getLevel().getServer().getPlayerList().remove(player);
            h.succeed();
    }

    @GameTest
    public void mountainLandingsProtectTheRiderButCliffsStillHurt(GameTestHelper h) {
        Goat goat = h.spawn(EntityType.GOAT, 1, 2, 1);
        Player rider = h.makeMockPlayer(GameType.SURVIVAL);
        ((GoatMount)goat).exploration$tame(rider);
        rider.startRiding(goat);
        goat.causeFallDamage(13, 1, goat.damageSources().fall());
        h.assertTrue(goat.getHealth() == 20 && rider.getHealth() == 20, "A normal high jump is safe for mount and rider");
        goat.causeFallDamage(18, 1, goat.damageSources().fall());
        h.assertTrue(goat.getHealth() < 20 && rider.getHealth() < 20, "Mountain mounts do not grant fall immunity");
        h.succeed();
    }

    @GameTest(structure = "exploration-reloaded-gametest:platforms", maxTicks = 100)
    public void mountedJumpMatchesAHorseWithTheSameStrengthOnHoneyAndWithJumpBoost(GameTestHelper h) {
        for (int x = 1; x <= 10; x++) for (int z = 1; z <= 7; z++) {
            h.setBlock(new BlockPos(x, 1, z), Blocks.HONEY_BLOCK);
            for (int y = 2; y <= 22; y++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
        }
        Goat goat = h.spawn(EntityType.GOAT, 3, 2, 3);
        ramRider(h, goat);
        var horse = h.spawn(EntityType.HORSE, 8, 2, 3);
        Player horseRider = h.makeMockPlayer(GameType.SURVIVAL);
        horse.tameWithName(horseRider);
        horse.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        horse.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(GoatJump.MAX_IMPULSE);
        horseRider.startRiding(horse);
        for (var mount : new net.minecraft.world.entity.LivingEntity[]{goat, horse}) {
            mount.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.JUMP_BOOST, 200));
        }
        double[] start = new double[2];
        double[] peak = new double[2];
        h.runAtTickTime(16, () -> {
            start[0] = peak[0] = goat.getY();
            start[1] = peak[1] = horse.getY();
            ((PlayerRideableJumping)goat).onPlayerJump(100);
            horse.onPlayerJump(100);
        });
        for (int tick = 17; tick <= 80; tick++) h.runAtTickTime(tick, () -> {
            peak[0] = Math.max(peak[0], goat.getY());
            peak[1] = Math.max(peak[1], horse.getY());
        });
        h.runAtTickTime(81, () -> {
            double goatHeight = peak[0] - start[0], horseHeight = peak[1] - start[1];
            h.assertTrue(goatHeight > 2 && goatHeight < 6, "Honey still limits the leap despite Jump Boost");
            h.assertTrue(Math.abs(goatHeight - horseHeight) < 0.01,
                    "The goat uses the horse's native jump physics: goat=" + goatHeight + " horse=" + horseHeight);
            h.succeed();
        });
    }

    @GameTest(structure = "exploration-reloaded-gametest:platforms", maxTicks = 100)
    public void fullyChargedJumpReachesTwelveBlocksAndLandsSafely(GameTestHelper h) {
        for (int x = 1; x <= 7; x++) for (int z = 1; z <= 7; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        for (int x = 1; x <= 7; x++) for (int z = 1; z <= 7; z++) for (int y = 3; y <= 22; y++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
        Goat goat = h.spawn(EntityType.GOAT, 4, 2, 4);
        Player passenger = h.makeMockPlayer(GameType.SURVIVAL);
        ((GoatMount)goat).exploration$tame(passenger);
        goat.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        passenger.startRiding(goat);
        double startY = goat.getY();
        double[] peak = {startY};
        h.runAtTickTime(16, () -> ((PlayerRideableJumping)goat).onPlayerJump(100));
        h.runAtTickTime(22, () -> h.assertTrue(((GoatMount)goat).exploration$rearAnimation(1) > 0.8,
                "The normal jump control starts the horse-style rear"));
        h.runAtTickTime(48, () -> h.assertTrue(((GoatMount)goat).exploration$rearAnimation(1) == 0,
                "The jump rear settles after the native stand duration"));
        for (int tick = 17; tick < 81; tick++) {
            h.runAtTickTime(tick, () -> peak[0] = Math.max(peak[0], goat.getY()));
        }
        h.runAtTickTime(82, () -> {
            h.assertTrue(peak[0] - startY > 11.8 && peak[0] - startY < 12.5, "Full charge must reach approximately twelve blocks, actual: " + (peak[0] - startY));
            h.assertTrue(goat.onGround() && goat.getHealth() == 20, "A full jump lands without damage");
            h.succeed();
        });
    }
}
