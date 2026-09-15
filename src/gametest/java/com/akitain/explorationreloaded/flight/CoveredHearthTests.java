package com.akitain.explorationreloaded.flight;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

public class CoveredHearthTests {
    private static final BlockPos FIRE = new BlockPos(1, 1, 1);

    @GameTest
    public void closedLidsSupportChargingOnBothHalves(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
        player.setShiftKeyDown(true);
        helper.setBlock(FIRE, Blocks.CAMPFIRE);
        BlockPos bareFire = helper.absolutePos(FIRE);
        player.setPos(bareFire.getX() + 0.5, bareFire.getY() + 7.0 / 16.0, bareFire.getZ() + 0.5);
        player.setOnGround(true);
        helper.assertTrue(ElytraFlight.isRidingCampfireSmoke(player), "An uncovered campfire must still support charging");
        for (Block fire : new Block[]{Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE}) {
            helper.setBlock(FIRE, fire);
            for (Block lid : new Block[]{Blocks.OAK_TRAPDOOR, Blocks.IRON_TRAPDOOR, Blocks.COPPER_TRAPDOOR}) {
                for (Half half : Half.values()) {
                    helper.setBlock(FIRE.above(), lid.defaultBlockState().setValue(TrapDoorBlock.HALF, half));
                    standOnLid(helper, player, half);
                    helper.assertTrue(ElytraFlight.isRidingCampfireSmoke(player), "A closed lid must support charging");
                    helper.assertTrue(Hearth.isCovered(helper.getLevel(), helper.absolutePos(FIRE)), "The lid must suppress the column");
                    helper.assertValueEqual(Hearth.chargingFire(helper.getLevel(), player.getOnPos()),
                            helper.absolutePos(FIRE), "Charging must use the fire below the lid");
                }
            }
        }
        player.setOnGround(false);
        helper.assertFalse(ElytraFlight.isRidingCampfireSmoke(player), "A hovering player must not recharge");
        player.setOnGround(true);
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        helper.assertFalse(ElytraFlight.isRidingCampfireSmoke(player), "Charging requires elytra");
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
        helper.setBlock(FIRE, Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        helper.assertFalse(ElytraFlight.isRidingCampfireSmoke(player), "An extinguished fire must not charge through a lid");
        helper.setBlock(FIRE, Blocks.CAMPFIRE);
        helper.setBlock(FIRE.above(), Blocks.STONE);
        helper.assertFalse(ElytraFlight.isRidingCampfireSmoke(player), "A solid block must not act as a charging lid");
        helper.succeed();
    }

    @GameTest
    public void lowerLidLaunchesWithoutAnEnchantment(GameTestHelper helper) {
        verifyLaunch(helper, Half.BOTTOM, false);
    }

    @GameTest
    public void upperLidPreservesHearthPowerAndSmokestackCharges(GameTestHelper helper) {
        verifyLaunch(helper, Half.TOP, true);
    }

    @GameTest
    public void redstoneOpensAndClosesTheSmokeLid(GameTestHelper helper) {
        helper.setBlock(FIRE, Blocks.CAMPFIRE);
        BlockPos lidPos = FIRE.above();
        helper.setBlock(lidPos, Blocks.IRON_TRAPDOOR);
        BlockPos absoluteFire = helper.absolutePos(FIRE);
        helper.assertTrue(Hearth.isCovered(helper.getLevel(), absoluteFire), "Unpowered iron trapdoor must suppress smoke");
        helper.assertFalse(Hearth.passesUpdraft(helper.getBlockState(lidPos)), "Closed trapdoor must stop lift");
        helper.setBlock(lidPos.east(), Blocks.REDSTONE_BLOCK);
        helper.assertBlockProperty(lidPos, TrapDoorBlock.OPEN, true);
        helper.assertFalse(Hearth.isCovered(helper.getLevel(), absoluteFire), "Powered trapdoor must restore smoke");
        helper.assertTrue(Hearth.passesUpdraft(helper.getBlockState(lidPos)), "Open trapdoor must let lift through");
        helper.assertTrue(Hearth.chargingFire(helper.getLevel(), helper.absolutePos(lidPos)) == null,
                "An open trapdoor must not be a charging surface");
        helper.setBlock(lidPos.east(), Blocks.AIR);
        helper.assertBlockProperty(lidPos, TrapDoorBlock.OPEN, false);
        helper.assertTrue(Hearth.isCovered(helper.getLevel(), absoluteFire), "Closing the lid must suppress smoke again");
        helper.assertFalse(Hearth.passesUpdraft(Blocks.STONE.defaultBlockState()), "A roof must still block lift");
        helper.assertFalse(Hearth.passesUpdraft(Blocks.OAK_TRAPDOOR.defaultBlockState()
                .setValue(TrapDoorBlock.OPEN, true).setValue(TrapDoorBlock.WATERLOGGED, true)), "Water must still block lift");
        helper.succeed();
    }

    private static void verifyLaunch(GameTestHelper helper, Half half, boolean enchanted) {
        BlockState fire = (enchanted ? Blocks.SOUL_CAMPFIRE : Blocks.CAMPFIRE).defaultBlockState();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos = FIRE.offset(dx, 0, dz);
                helper.setBlock(pos.below(), Blocks.HAY_BLOCK);
                helper.setBlock(pos, fire.setValue(CampfireBlock.SIGNAL_FIRE, true));
                helper.setBlock(pos.above(), Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, half));
            }
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            ItemStack elytra = new ItemStack(Items.ELYTRA);
            if (enchanted) {
                elytra.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(FlightEnchantments.SMOKESTACK), 3);
            }
            player.setItemSlot(EquipmentSlot.CHEST, elytra);
            player.setShiftKeyDown(true);
            standOnLid(helper, player, half);
            int interval = helper.getLevel().getGameRules().get(FlightRules.SMOKESTACK_CHARGE_TICKS);
            for (int tick = 0; tick < interval * (enchanted ? 3 : 1); tick++) {
                ElytraFlight.tick(helper.getLevel(), player);
            }
            helper.assertTrue(FlightState.isCharged(player), "Charging through the lid must arm a launch");
            helper.assertValueEqual(FlightState.hearthPower(player), 12, "The lid must preserve neighbours and hay bonus");
            helper.assertValueEqual(FlightState.charges(player), enchanted ? 3 : 0, "Smokestack must bank the expected charges");
            player.setShiftKeyDown(false);
            ElytraFlight.tick(helper.getLevel(), player);
            helper.assertFalse(FlightState.isCharged(player), "Standing up must consume the armed launch");
            helper.assertTrue(player.isFallFlying(), "Standing up must open the wings");
            helper.assertTrue(player.getDeltaMovement().y > 1.6, "The covered hearth must retain the full launch impulse");
            helper.assertTrue(FlightState.boostTicks(player) > 90, "The launch must retain the hearth's burn bonus");
        } finally {
            helper.getLevel().getServer().getPlayerList().remove(player);
        }
        helper.succeed();
    }

    private static void standOnLid(GameTestHelper helper, Player player, Half half) {
        BlockPos lid = helper.absolutePos(FIRE.above());
        player.setPos(lid.getX() + 0.5, lid.getY() + (half == Half.TOP ? 1.0 : 3.0 / 16.0), lid.getZ() + 0.5);
        player.setOnGround(true);
    }
}
