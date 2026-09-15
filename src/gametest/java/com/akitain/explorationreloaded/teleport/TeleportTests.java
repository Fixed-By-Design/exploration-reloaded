package com.akitain.explorationreloaded.teleport;

import com.akitain.explorationreloaded.registry.ExplorationItems;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class TeleportTests {
    private static final String ARENA = "exploration-reloaded-gametest:platforms";
    private static final BlockPos SOURCE = new BlockPos(5, 2, 5);
    private static final BlockPos TARGET = new BlockPos(23, 2, 5);

    @GameTest(structure = ARENA)
    public void flatHomogeneousBeaconBasesAcceptDecoration(GameTestHelper helper) {
        for (Block block : new Block[]{Blocks.IRON_BLOCK, Blocks.GOLD_BLOCK, Blocks.EMERALD_BLOCK, Blocks.DIAMOND_BLOCK, Blocks.NETHERITE_BLOCK}) {
            build(helper, SOURCE, block, Blocks.STONE);
            helper.assertTrue(TeleportPlatform.read(helper.getLevel(), helper.absolutePos(SOURCE)) != null,
                    "A covered single layer of beacon material must be accepted");
            helper.setBlock(SOURCE.below(), Blocks.COPPER_BLOCK);
            helper.assertTrue(TeleportPlatform.read(helper.getLevel(), helper.absolutePos(SOURCE)) == null,
                    "The hidden center block counts among all 49 blocks");
        }
        build(helper, SOURCE, Blocks.IRON_BLOCK, Blocks.AIR);
        helper.setBlock(SOURCE.offset(3, -1, 3), Blocks.GOLD_BLOCK);
        helper.assertTrue(TeleportPlatform.read(helper.getLevel(), helper.absolutePos(SOURCE)) == null,
                "Two beacon materials must never be mixed");
        build(helper, SOURCE, Blocks.COPPER_BLOCK, Blocks.AIR);
        helper.assertTrue(TeleportPlatform.read(helper.getLevel(), helper.absolutePos(SOURCE)) == null,
                "Copper is not a beacon base material");
        helper.succeed();
    }

    @GameTest(structure = ARENA)
    public void exactArrivalsRespectCoversHazardsAndOccupiedSpaces(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        build(helper, SOURCE, Blocks.IRON_BLOCK, Blocks.AIR);
        TeleportPlatform source = TeleportPlatform.read(helper.getLevel(), helper.absolutePos(SOURCE));
        Block[] floors = {Blocks.AIR, Blocks.STONE_SLAB, Blocks.STONE, Blocks.WHITE_CARPET};
        double[] heights = {0, 0.5, 1, 0.0625};
        for (int i = 0; i < floors.length; i++) {
            build(helper, SOURCE, Blocks.IRON_BLOCK, floors[i]);
            build(helper, TARGET, Blocks.IRON_BLOCK, floors[i]);
            TeleportPlatform target = TeleportPlatform.read(helper.getLevel(), helper.absolutePos(TARGET));
            player.setPos(source.lodestone().getX() + 1.375, source.lodestone().getY() + heights[i], source.lodestone().getZ() - 1.625);
            TeleportPlatform.Landing landing = target.relativeLanding(player, player, source, Set.of());
            helper.assertTrue(landing != null, "Matching decorated floors permit exact arrival");
            helper.assertTrue(landing.feet().distanceToSqr(player.position().add(18, 0, 0)) < 1.0E-12,
                    "Fractional coordinates and floor height must remain unchanged");
        }
        build(helper, SOURCE, Blocks.IRON_BLOCK, Blocks.AIR);
        player.setPos(source.lodestone().getX() + 1.5, source.lodestone().getY(), source.lodestone().getZ() - 1.5);
        for (Block hazard : new Block[]{Blocks.LAVA, Blocks.WATER, Blocks.MAGMA_BLOCK, Blocks.POWDER_SNOW, Blocks.CACTUS}) {
            build(helper, TARGET, Blocks.IRON_BLOCK, hazard);
            TeleportPlatform target = TeleportPlatform.read(helper.getLevel(), helper.absolutePos(TARGET));
            helper.assertTrue(target.relativeLanding(player, player, source, Set.of()) == null,
                    "A hazardous floor must prevent arrival: " + hazard);
        }
        build(helper, TARGET, Blocks.IRON_BLOCK, Blocks.STONE);
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            if (x != 0 || z != 0) helper.setBlock(TARGET.offset(x, 1, z), Blocks.WHITE_CARPET);
        }
        TeleportPlatform carpeted = TeleportPlatform.read(helper.getLevel(), helper.absolutePos(TARGET));
        TeleportPlatform.Landing landing = carpeted.relativeLanding(player, player, source, Set.of());
        helper.assertTrue(landing != null, "A full floor plus carpet supports arrival from a bare platform");
        helper.assertTrue(carpeted.surfaceHeight(landing.feet().x, landing.feet().z) > landing.feet().y,
                "The boundary stays visible above the carpet");
        build(helper, TARGET, Blocks.IRON_BLOCK, Blocks.AIR);
        TeleportPlatform target = TeleportPlatform.read(helper.getLevel(), helper.absolutePos(TARGET));
        helper.assertTrue(target.relativeLanding(player, player, source, Set.of()).feet().y == player.getY(),
                "A bare destination keeps the same height as the bare departure");
        player.setPos(player.getX(), source.lodestone().getY(), player.getZ());
        Villager occupant = helper.spawnWithNoFreeWill(EntityType.VILLAGER, TARGET.offset(1, 0, -2));
        occupant.setPos(player.position().add(18, 0, 0));
        helper.assertTrue(target.relativeLanding(player, player, source, Set.of()) == null,
                "An occupied exact arrival cannot use a different free square");
        occupant.discard();
        helper.setBlock(TARGET.offset(1, 1, -2), Blocks.STONE);
        helper.assertTrue(target.relativeLanding(player, player, source, Set.of()) == null,
                "An obstruction at the exact arrival cannot use another free square");
        helper.setBlock(TARGET.offset(1, 1, -2), Blocks.AIR);
        // The boarding zone is defined by feet, including its outermost edge.
        player.setPos(source.lodestone().getX() - 2.99, source.lodestone().getY(), source.lodestone().getZ() + 0.5);
        helper.assertTrue(target.relativeLanding(player, player, source, Set.of()) != null,
                "A valid edge position must preserve its offset even when the body crosses the outline");
        player.setPos(player.getX(), source.lodestone().getY() + 2, player.getZ());
        helper.assertFalse(source.contains(player), "The floor above is outside the boarding zone");
        helper.succeed();
    }

    @GameTest(structure = ARENA)
    public void differentCoverHeightsKeepHorizontalFormation(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Block[] floors = {Blocks.AIR, Blocks.STONE_SLAB, Blocks.WHITE_CARPET, Blocks.STONE, Blocks.STONE};
        double[] heights = {0, 0.5, 0.0625, 1, 1.0625};
        for (int from = 0; from < floors.length; from++) {
            build(helper, SOURCE, Blocks.IRON_BLOCK, floors[from]);
            if (from == 4) helper.setBlock(SOURCE.offset(1, 1, -2), Blocks.WHITE_CARPET);
            TeleportPlatform source = TeleportPlatform.read(helper.getLevel(), helper.absolutePos(SOURCE));
            player.setPos(Vec3.atLowerCornerOf(source.lodestone()).add(1.5, heights[from], -1.5));
            for (int to = 0; to < floors.length; to++) {
                build(helper, TARGET, Blocks.IRON_BLOCK, floors[to]);
                if (to == 4) helper.setBlock(TARGET.offset(1, 1, -2), Blocks.WHITE_CARPET);
                TeleportPlatform target = TeleportPlatform.read(helper.getLevel(), helper.absolutePos(TARGET));
                TeleportPlatform.Landing landing = target.relativeLanding(player, player, source, Set.of());
                helper.assertTrue(landing != null, "Clear coverings must work in both directions: " + from + " to " + to);
                Vec3 expected = Vec3.atLowerCornerOf(target.lodestone()).add(1.5, heights[to], -1.5);
                helper.assertTrue(landing.feet().distanceToSqr(expected) < 1.0E-12,
                        "Only height adjusts to the destination floor: " + from + " to " + to);
            }
        }
        helper.succeed();
    }

    @GameTest(structure = ARENA, maxTicks = 220)
    public void arrivalUsesTheExactPositionAndViewAtDeparture(GameTestHelper helper) {
        build(helper, SOURCE, Blocks.GOLD_BLOCK, Blocks.STONE_SLAB);
        BlockPos raisedTarget = TARGET.above(2);
        build(helper, raisedTarget, Blocks.GOLD_BLOCK, Blocks.STONE);
        helper.setBlock(raisedTarget.offset(2, 1, -2), Blocks.WHITE_CARPET);
        List<ServerPlayer> players = players(helper, 1, false);
        ServerPlayer player = players.getFirst();
        player.setPos(player.position().add(0, 0.5, 0));
        player.getMainHandItem().set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(
                GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(raisedTarget))), true));
        Vec3 departure = Vec3.atLowerCornerOf(helper.absolutePos(SOURCE)).add(2.1875, 0.5, -1.3125);
        helper.assertTrue(TeleportRituals.get(helper.getLevel().getServer()).start(player, helper.absolutePos(SOURCE), InteractionHand.MAIN_HAND), "The ritual starts");
        helper.runAtTickTime(10, () -> {
            player.setPos(departure);
            player.setYRot(137.25f);
            player.setXRot(-21.5f);
        });
        helper.runAtTickTime(80, () -> {
            try {
                helper.assertTrue(player.position().distanceToSqr(departure.add(18, 2.5625, 0)) < 1.0E-10,
                        "Arrival uses the final horizontal position and the destination carpet height");
                helper.assertValueEqual(player.getYRot(), 137.25f, "Yaw is preserved");
                helper.assertValueEqual(player.getXRot(), -21.5f, "Pitch is preserved");
                helper.assertTrue(player.getMainHandItem().is(Items.COMPASS), "Exact arrival consumes the charge");
            } finally { remove(helper, players); }
            helper.succeed();
        });
    }

    @GameTest(structure = ARENA, maxTicks = 220)
    public void overlappingPlayersKeepTheirFormation(GameTestHelper helper) {
        build(helper, SOURCE, Blocks.GOLD_BLOCK, Blocks.AIR);
        build(helper, TARGET, Blocks.GOLD_BLOCK, Blocks.AIR);
        List<ServerPlayer> players = players(helper, 2, false);
        players.get(1).setPos(players.getFirst().position().add(0.125, 0, 0.25));
        List<Vec3> departures = players.stream().map(ServerPlayer::position).toList();
        TeleportRituals manager = TeleportRituals.get(helper.getLevel().getServer());
        manager.start(players.getFirst(), helper.absolutePos(SOURCE), InteractionHand.MAIN_HAND);
        manager.join(players.get(1), helper.absolutePos(SOURCE));
        helper.runAtTickTime(80, () -> {
            try {
                for (int i = 0; i < players.size(); i++) {
                    helper.assertTrue(players.get(i).position().distanceToSqr(departures.get(i).add(18, 0, 0)) < 1.0E-8,
                            "A crowded group keeps its existing formation without being rearranged or rejected");
                }
                helper.assertTrue(players.getFirst().getMainHandItem().is(Items.COMPASS), "The group travels for one charge");
            } finally { remove(helper, players); }
            helper.succeed();
        });
    }

    @GameTest
    public void anvilChargesOneNamedCompassAndOneStarOnShiftClick(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.giveExperienceLevels(10);
        AnvilMenu menu = new AnvilMenu(0, player.getInventory());
        ItemStack compass = bound(helper, Items.COMPASS, 3);
        compass.set(DataComponents.CUSTOM_NAME, Component.literal("Temple du Nord"));
        menu.getSlot(0).set(compass);
        menu.setItemName("Temple du Nord");
        menu.getSlot(1).set(new ItemStack(Items.NETHER_STAR, 64));
        helper.assertTrue(menu.getSlot(2).getItem().is(ExplorationItems.WITHER_COMPASS), "The anvil must produce a charged compass");
        helper.assertValueEqual(menu.getCost(), 1, "Charging uses the vanilla one-level anvil cost");
        menu.quickMoveStack(player, 2);
        helper.assertValueEqual(menu.getSlot(0).getItem().getCount(), 2, "Shift-click must retain unused compasses");
        helper.assertValueEqual(menu.getSlot(1).getItem().getCount(), 63, "Shift-click must consume exactly one star");
        helper.assertValueEqual(player.experienceLevel, 9, "The level cost must be taken only on pickup");
        ItemStack charged = ItemStack.EMPTY;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(ExplorationItems.WITHER_COMPASS)) charged = player.getInventory().getItem(slot);
        }
        helper.assertTrue(charged.is(ExplorationItems.WITHER_COMPASS), "Shift-click must transfer the result");
        helper.assertValueEqual(charged.get(DataComponents.CUSTOM_NAME), compass.get(DataComponents.CUSTOM_NAME), "The name must survive");
        helper.assertValueEqual(charged.get(DataComponents.LODESTONE_TRACKER), compass.get(DataComponents.LODESTONE_TRACKER), "The destination must survive");
        ItemStack picked = menu.getSlot(2).remove(1);
        menu.getSlot(2).onTake(player, picked);
        helper.assertValueEqual(menu.getSlot(0).getItem().getCount(), 1, "Normal pickup must retain unused compasses too");
        helper.assertValueEqual(menu.getSlot(1).getItem().getCount(), 62, "Normal pickup consumes one star");
        menu.getSlot(0).set(new ItemStack(Items.COMPASS));
        helper.assertTrue(menu.getSlot(2).getItem().isEmpty(), "An unbound compass cannot be charged");
        menu.getSlot(0).set(charged.copy());
        helper.assertTrue(menu.getSlot(2).getItem().isEmpty(), "A charged compass cannot be charged twice");
        menu.getSlot(1).set(ItemStack.EMPTY);
        menu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));
        menu.setItemName("Voyageur");
        helper.assertTrue(menu.getSlot(2).getItem().is(Items.DIAMOND_SWORD), "Vanilla renaming must still work");
        helper.succeed();
    }

    @GameTest(structure = ARENA, maxTicks = 400)
    public void emeraldCarriesConsentingPlayersAndVillagers(GameTestHelper helper) {
        build(helper, SOURCE, Blocks.EMERALD_BLOCK, Blocks.STONE);
        build(helper, TARGET, Blocks.EMERALD_BLOCK, Blocks.STONE);
        Villager adult = helper.spawnWithNoFreeWill(EntityType.VILLAGER, SOURCE.offset(-1, 1, 2));
        Villager baby = helper.spawnWithNoFreeWill(EntityType.VILLAGER, SOURCE.offset(1, 1, 2));
        adult.setNoAi(true);
        baby.setNoAi(true);
        baby.setAge(-24000);
        adult.setVillagerData(adult.getVillagerData().withLevel(3));
        adult.setVillagerXp(80);
        MerchantOffers offers = new MerchantOffers();
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 3), new ItemStack(Items.BREAD, 2), 12, 2, 0.05f));
        adult.setOffers(offers);
        adult.getInventory().setItem(0, new ItemStack(Items.BREAD, 7));
        List<ServerPlayer> players = players(helper, 6, true);
        ServerPlayer leader = players.getFirst();
        TeleportRituals manager = TeleportRituals.get(helper.getLevel().getServer());
        helper.assertTrue(manager.start(leader, helper.absolutePos(SOURCE), InteractionHand.MAIN_HAND), "The leader must start the ritual");
        for (int i = 1; i <= 4; i++) {
            ServerPlayer passenger = players.get(i);
            UseBlockCallback.EVENT.invoker().interact(passenger, helper.getLevel(), InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(helper.absolutePos(SOURCE)), Direction.UP, helper.absolutePos(SOURCE), false));
        }
        List<Vec3> departures = players.stream().map(ServerPlayer::position).toList();
        Vec3 adultDeparture = adult.position();
        Vec3 babyDeparture = baby.position();
        helper.runAtTickTime(210, () -> {
            try {
                TeleportPlatform destination = TeleportPlatform.read(helper.getLevel(), helper.absolutePos(TARGET));
                for (int i = 0; i < 5; i++) helper.assertTrue(destination.contains(players.get(i)), "All enrolled players must arrive, including a fifth player");
                for (int i = 0; i < 5; i++) helper.assertTrue(players.get(i).position().distanceToSqr(departures.get(i).add(18, 0, 0)) < 1.0E-8, "The formation must be translated exactly");
                helper.assertTrue(adult.position().distanceToSqr(adultDeparture.add(18, 0, 0)) < 1.0E-8, "Adult villager keeps its place");
                helper.assertTrue(baby.position().distanceToSqr(babyDeparture.add(18, 0, 0)) < 1.0E-8, "Baby villager keeps its place");
                helper.assertFalse(destination.contains(players.get(5)), "A bystander must never be teleported automatically");
                helper.assertTrue(destination.contains(adult) && destination.contains(baby), "Emerald must carry both villagers: adult=" + adult.position() + ", baby=" + baby.position() + ", target=" + destination.lodestone());
                helper.assertValueEqual(adult.getVillagerData().level(), 3, "Villager level must survive teleportation");
                helper.assertValueEqual(adult.getVillagerXp(), 80, "Trade XP must survive teleportation");
                helper.assertTrue(adult.getOffers().getFirst() == offers.getFirst(), "The original trade offers must survive");
                helper.assertValueEqual(adult.getInventory().getItem(0).getCount(), 7, "Villager inventory must survive");
                helper.assertTrue(leader.getMainHandItem().is(Items.COMPASS), "One star must pay for the entire group");
                for (int i = 0; i < 5; i++) helper.assertTrue(players.get(i).hasEffect(MobEffects.BLINDNESS), "Every traveller receives brief blindness");
                helper.assertValueEqual(leader.getMainHandItem().get(DataComponents.LODESTONE_TRACKER),
                        bound(helper, Items.COMPASS, 1).get(DataComponents.LODESTONE_TRACKER), "The returned compass stays bound");
                for (int i = 0; i < 5; i++) for (int j = i + 1; j < 5; j++) {
                    helper.assertFalse(players.get(i).getBoundingBox().intersects(players.get(j).getBoundingBox()), "Arrivals must not overlap");
                }
            } finally {
                remove(helper, players);
                adult.discard();
                baby.discard();
            }
            helper.succeed();
        });
    }

    @GameTest(structure = ARENA, maxTicks = 400)
    public void blockedGroupArrivalPreservesEveryTravellerAndCompass(GameTestHelper helper) {
        build(helper, SOURCE, Blocks.EMERALD_BLOCK, Blocks.AIR);
        build(helper, TARGET, Blocks.EMERALD_BLOCK, Blocks.AIR);
        helper.setBlock(TARGET.offset(0, 1, -2), Blocks.STONE);
        List<ServerPlayer> players = players(helper, 2, false);
        TeleportRituals manager = TeleportRituals.get(helper.getLevel().getServer());
        helper.assertTrue(manager.start(players.getFirst(), helper.absolutePos(SOURCE), InteractionHand.MAIN_HAND), "Start must succeed");
        manager.join(players.get(1), helper.absolutePos(SOURCE));
        helper.runAtTickTime(210, () -> {
            try {
                TeleportPlatform source = TeleportPlatform.read(helper.getLevel(), helper.absolutePos(SOURCE));
                for (ServerPlayer player : players) helper.assertTrue(source.contains(player), "A blocked passenger cannot split the group or move to another square");
                helper.assertTrue(players.getFirst().getMainHandItem().is(ExplorationItems.WITHER_COMPASS), "An unsafe journey must not discharge the compass");
            } finally {
                remove(helper, players);
            }
            helper.succeed();
        });
    }

    @GameTest(structure = ARENA, maxTicks = 220)
    public void changingHeldCompassCancelsTheJourney(GameTestHelper helper) {
        build(helper, SOURCE, Blocks.GOLD_BLOCK, Blocks.AIR);
        build(helper, TARGET, Blocks.GOLD_BLOCK, Blocks.AIR);
        List<ServerPlayer> players = players(helper, 1, false);
        ServerPlayer player = players.getFirst();
        ItemStack charged = player.getMainHandItem();
        TeleportRituals manager = TeleportRituals.get(helper.getLevel().getServer());
        helper.assertTrue(manager.start(player, helper.absolutePos(SOURCE), InteractionHand.MAIN_HAND), "Start must succeed");
        helper.runAtTickTime(10, () -> player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY));
        helper.runAtTickTime(80, () -> {
            try {
                helper.assertTrue(TeleportPlatform.read(helper.getLevel(), helper.absolutePos(SOURCE)).contains(player), "Changing the held item must cancel");
                helper.assertTrue(charged.is(ExplorationItems.WITHER_COMPASS), "The original compass must remain charged");
            } finally { remove(helper, players); }
            helper.succeed();
        });
    }

    @GameTest(structure = ARENA, maxTicks = 220)
    public void differentEndpointMaterialsNeverConnect(GameTestHelper helper) {
        build(helper, SOURCE, Blocks.GOLD_BLOCK, Blocks.AIR);
        build(helper, TARGET, Blocks.IRON_BLOCK, Blocks.AIR);
        List<ServerPlayer> players = players(helper, 1, false);
        ServerPlayer player = players.getFirst();
        TeleportRituals.get(helper.getLevel().getServer()).start(player, helper.absolutePos(SOURCE), InteractionHand.MAIN_HAND);
        helper.runAtTickTime(80, () -> {
            try {
                helper.assertTrue(TeleportPlatform.read(helper.getLevel(), helper.absolutePos(SOURCE)).contains(player), "Gold cannot connect to iron");
                helper.assertTrue(player.getMainHandItem().is(ExplorationItems.WITHER_COMPASS), "A mismatch must preserve the charge");
            } finally { remove(helper, players); }
            helper.succeed();
        });
    }

    @GameTest(structure = ARENA, maxTicks = 400)
    public void leavingTheZoneRemovesOnlyThatPassenger(GameTestHelper helper) {
        build(helper, SOURCE, Blocks.EMERALD_BLOCK, Blocks.AIR);
        build(helper, TARGET, Blocks.EMERALD_BLOCK, Blocks.AIR);
        List<ServerPlayer> players = players(helper, 2, false);
        TeleportRituals manager = TeleportRituals.get(helper.getLevel().getServer());
        manager.start(players.getFirst(), helper.absolutePos(SOURCE), InteractionHand.MAIN_HAND);
        manager.join(players.get(1), helper.absolutePos(SOURCE));
        helper.runAtTickTime(10, () -> players.get(1).setPos(Vec3.atBottomCenterOf(helper.absolutePos(SOURCE.above(2)))));
        helper.runAtTickTime(210, () -> {
            try {
                TeleportPlatform destination = TeleportPlatform.read(helper.getLevel(), helper.absolutePos(TARGET));
                helper.assertTrue(destination.contains(players.getFirst()), "The remaining leader still travels");
                helper.assertFalse(destination.contains(players.get(1)), "The passenger on another floor must be removed");
            } finally { remove(helper, players); }
            helper.succeed();
        });
    }

    @GameTest(structure = ARENA, maxTicks = 400)
    public void brokenDestinationCanBeRepairedWithoutLosingTheCharge(GameTestHelper helper) {
        build(helper, SOURCE, Blocks.GOLD_BLOCK, Blocks.AIR);
        build(helper, TARGET, Blocks.GOLD_BLOCK, Blocks.AIR);
        List<ServerPlayer> players = players(helper, 1, false);
        ServerPlayer player = players.getFirst();
        TeleportRituals manager = TeleportRituals.get(helper.getLevel().getServer());
        manager.start(player, helper.absolutePos(SOURCE), InteractionHand.MAIN_HAND);
        helper.runAtTickTime(10, () -> helper.setBlock(TARGET, Blocks.AIR));
        helper.runAtTickTime(70, () -> {
            helper.assertTrue(player.getMainHandItem().is(ExplorationItems.WITHER_COMPASS), "Breaking the target preserves the charge");
            helper.assertTrue(player.getMainHandItem().get(DataComponents.LODESTONE_TRACKER).target().isPresent(), "A paid compass retains the target for repair");
            helper.setBlock(TARGET, Blocks.LODESTONE);
            helper.assertTrue(manager.start(player, helper.absolutePos(SOURCE), InteractionHand.MAIN_HAND), "Repairing the same target permits another attempt");
        });
        helper.runAtTickTime(140, () -> {
            try {
                helper.assertTrue(TeleportPlatform.read(helper.getLevel(), helper.absolutePos(TARGET)).contains(player), "The repaired destination must be usable");
                helper.assertTrue(player.getMainHandItem().is(Items.COMPASS), "Only the successful attempt consumes the charge");
            } finally { remove(helper, players); }
            helper.succeed();
        });
    }

    @GameTest(structure = ARENA, maxTicks = 400)
    public void roseGoldUsesVanillaMountedTeleportAndPreservesEquipment(GameTestHelper helper) {
        Block roseGold = BuiltInRegistries.BLOCK.getValue(Identifier.parse("additionaladditions:rose_gold_block"));
        build(helper, SOURCE, roseGold, Blocks.AIR);
        build(helper, TARGET, roseGold, Blocks.STONE_SLAB);
        List<ServerPlayer> players = players(helper, 1, false);
        ServerPlayer player = players.getFirst();
        Horse horse = helper.spawnWithNoFreeWill(EntityType.HORSE, SOURCE.offset(2, 0, 2));
        horse.setNoAi(true);
        Vec3 departure = horse.position();
        horse.setTamed(true);
        horse.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        horse.setItemSlot(EquipmentSlot.BODY, new ItemStack(Items.DIAMOND_HORSE_ARMOR));
        player.startRiding(horse);
        helper.assertTrue(TeleportRituals.get(helper.getLevel().getServer()).start(player, helper.absolutePos(SOURCE), InteractionHand.MAIN_HAND), "Mounted rose-gold travel must start");
        helper.runAtTickTime(170, () -> {
            try {
                TeleportPlatform destination = TeleportPlatform.read(helper.getLevel(), helper.absolutePos(TARGET));
                helper.assertTrue(destination.contains(horse), "The living mount must arrive at the matching place");
                helper.assertTrue(horse.position().distanceToSqr(departure.add(18, 0.5, 0)) < 1.0E-8, "The ridden mount keeps its horizontal position and stands on the destination slabs");
                helper.assertTrue(player.getVehicle() == horse, "Native teleport preserves the same ridden entity");
                helper.assertTrue(horse.getItemBySlot(EquipmentSlot.BODY).is(Items.DIAMOND_HORSE_ARMOR), "Mount armor must survive");
                helper.assertTrue(horse.getItemBySlot(EquipmentSlot.SADDLE).is(Items.SADDLE), "The saddle must survive");
                helper.assertTrue(player.getMainHandItem().is(Items.COMPASS), "Mounted travel consumes one charge");
            } finally { remove(helper, players); horse.discard(); }
            helper.succeed();
        });
    }

    @GameTest(structure = ARENA, maxTicks = 500)
    public void onlyNetheriteCanTravelBetweenDimensions(GameTestHelper helper) {
        build(helper, SOURCE, Blocks.DIAMOND_BLOCK, Blocks.AIR);
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        BlockPos target = new BlockPos(helper.absolutePos(SOURCE).getX(), 120, helper.absolutePos(SOURCE).getZ());
        // Fixtures load terrain synchronously; production uses the asynchronous ticket API.
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            nether.getChunk((target.getX() >> 4) + x, (target.getZ() >> 4) + z);
        }
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            nether.setBlockAndUpdate(target.offset(x, -1, z), Blocks.NETHERITE_BLOCK.defaultBlockState());
            for (int y = 0; y < 5; y++) nether.setBlockAndUpdate(target.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
        nether.setBlockAndUpdate(target, Blocks.LODESTONE.defaultBlockState());
        List<ServerPlayer> players = players(helper, 1, false);
        ServerPlayer player = players.getFirst();
        player.getMainHandItem().set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(GlobalPos.of(Level.NETHER, target)), true));
        TeleportRituals manager = TeleportRituals.get(helper.getLevel().getServer());
        helper.assertFalse(manager.start(player, helper.absolutePos(SOURCE), InteractionHand.MAIN_HAND), "Diamond cannot cross dimensions");
        build(helper, SOURCE, Blocks.NETHERITE_BLOCK, Blocks.AIR);
        helper.assertTrue(manager.start(player, helper.absolutePos(SOURCE), InteractionHand.MAIN_HAND), "Netherite can cross dimensions");
        Mob animal = helper.spawnWithNoFreeWill(EntityType.COW, SOURCE.offset(2, 0, 2));
        animal.setNoAi(true);
        animal.setCustomName(Component.literal("Across the Nether"));
        var animalId = animal.getUUID();
        Vec3 animalOffset = animal.position().subtract(Vec3.atLowerCornerOf(helper.absolutePos(SOURCE)));
        Vec3 offset = player.position().subtract(Vec3.atLowerCornerOf(helper.absolutePos(SOURCE)));
        helper.runAtTickTime(180, () -> {
            try {
                helper.assertTrue(player.position().distanceToSqr(Vec3.atLowerCornerOf(target).add(offset)) < 1.0E-8, "Cross-dimensional arrival keeps the same offset");
                Entity arrivedAnimal = nether.getEntity(animalId);
                helper.assertTrue(arrivedAnimal instanceof Mob && arrivedAnimal != animal, "Minecraft recreates the animal in the destination dimension");
                helper.assertTrue(arrivedAnimal.position().distanceToSqr(Vec3.atLowerCornerOf(target).add(animalOffset)) < 1.0E-8, "The animal retains its exact offset across dimensions");
                helper.assertValueEqual(arrivedAnimal.getCustomName().getString(), "Across the Nether", "The recreated animal keeps its data");
                helper.assertTrue(helper.getLevel().getEntity(animalId) == null, "The source animal is removed without duplication");
                helper.assertTrue(player.level() == nether, "Use vanilla ServerPlayer teleport to change dimensions");
                helper.assertTrue(TeleportPlatform.read(nether, target).contains(player), "The arrival remains on the destination platform");
                helper.assertTrue(player.getMainHandItem().is(Items.COMPASS), "Cross-dimensional travel consumes one charge");
            } finally { remove(helper, players); Entity arrivedAnimal = nether.getEntity(animalId); if (arrivedAnimal != null) arrivedAnimal.discard(); if (!animal.isRemoved()) animal.discard(); }
            helper.succeed();
        });
    }

    @GameTest(structure = ARENA)
    public void regionalMaterialsRejectDistantDestinationsBeforeLoading(GameTestHelper helper) {
        List<ServerPlayer> players = players(helper, 1, false);
        ServerPlayer player = players.getFirst();
        try {
            for (Block material : new Block[]{Blocks.IRON_BLOCK, Blocks.GOLD_BLOCK, Blocks.EMERALD_BLOCK}) {
                build(helper, SOURCE, material, Blocks.AIR);
                player.getMainHandItem().set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(
                        GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(SOURCE).east(4001))), true));
                helper.assertFalse(TeleportRituals.get(helper.getLevel().getServer()).start(player, helper.absolutePos(SOURCE), InteractionHand.MAIN_HAND),
                        "Regional travel is limited to 4000 horizontal blocks");
            }
        } finally { remove(helper, players); }
        helper.succeed();
    }

    @GameTest(structure = ARENA, maxTicks = 220)
    public void goldCarriesGroupsButLeavesVillagersBehind(GameTestHelper helper) {
        build(helper, SOURCE, Blocks.GOLD_BLOCK, Blocks.AIR);
        build(helper, TARGET, Blocks.GOLD_BLOCK, Blocks.AIR);
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, SOURCE.offset(1, 0, 2));
        villager.setNoAi(true);
        List<ServerPlayer> players = players(helper, 5, false);
        TeleportRituals manager = TeleportRituals.get(helper.getLevel().getServer());
        helper.useBlock(SOURCE, players.getFirst());
        for (int i = 1; i < players.size(); i++) {
            helper.assertTrue(manager.join(players.get(i), helper.absolutePos(SOURCE)), "Using the charged compass on a Lodestone must open group boarding");
        }
        helper.runAtTickTime(80, () -> {
            try {
                TeleportPlatform destination = TeleportPlatform.read(helper.getLevel(), helper.absolutePos(TARGET));
                for (ServerPlayer player : players) helper.assertTrue(destination.contains(player), "Gold must carry every consenting player");
                helper.assertTrue(TeleportPlatform.read(helper.getLevel(), helper.absolutePos(SOURCE)).contains(villager), "Only emerald carries villagers");
            } finally { remove(helper, players); villager.discard(); }
            helper.succeed();
        });
    }

    @GameTest(structure = ARENA, maxTicks = 400)
    public void villagersAlsoRequireASafeArrivalBeforeAnyoneLeaves(GameTestHelper helper) {
        build(helper, SOURCE, Blocks.EMERALD_BLOCK, Blocks.AIR);
        build(helper, TARGET, Blocks.EMERALD_BLOCK, Blocks.AIR);
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            if (x != 1 || z != 1) helper.setBlock(TARGET.offset(x, 1, z), Blocks.STONE);
        }
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, SOURCE.offset(1, 0, 2));
        villager.setNoAi(true);
        List<ServerPlayer> players = players(helper, 1, false);
        TeleportRituals.get(helper.getLevel().getServer()).start(players.getFirst(), helper.absolutePos(SOURCE), InteractionHand.MAIN_HAND);
        helper.runAtTickTime(210, () -> {
            try {
                TeleportPlatform source = TeleportPlatform.read(helper.getLevel(), helper.absolutePos(SOURCE));
                helper.assertTrue(source.contains(players.getFirst()) && source.contains(villager), "Everyone stays if the villager has no safe place");
                helper.assertTrue(players.getFirst().getMainHandItem().is(ExplorationItems.WITHER_COMPASS), "The charge is preserved");
            } finally { remove(helper, players); villager.discard(); }
            helper.succeed();
        });
    }

    @GameTest(structure = ARENA, maxTicks = 400)
    public void injuredVillagersLeaveTheCurrentDeparture(GameTestHelper helper) {
        build(helper, SOURCE, Blocks.EMERALD_BLOCK, Blocks.AIR);
        build(helper, TARGET, Blocks.EMERALD_BLOCK, Blocks.AIR);
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, SOURCE.offset(1, 0, 2));
        villager.setNoAi(true);
        List<ServerPlayer> players = players(helper, 1, false);
        TeleportRituals.get(helper.getLevel().getServer()).start(players.getFirst(), helper.absolutePos(SOURCE), InteractionHand.MAIN_HAND);
        helper.runAtTickTime(10, () -> helper.assertTrue(villager.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 1),
                "The test must deliver real server-side damage"));
        helper.runAtTickTime(210, () -> {
            try {
                helper.assertTrue(TeleportPlatform.read(helper.getLevel(), helper.absolutePos(TARGET)).contains(players.getFirst()), "The unaffected player still travels");
                helper.assertTrue(TeleportPlatform.read(helper.getLevel(), helper.absolutePos(SOURCE)).contains(villager), "A damaged villager is removed and not automatically re-enrolled");
            } finally { remove(helper, players); villager.discard(); }
            helper.succeed();
        });
    }

    @GameTest(structure = ARENA, maxTicks = 300)
    public void ironLeavesAllCreaturesBehind(GameTestHelper helper) {
        creatureJourney(helper, Blocks.IRON_BLOCK, false, false);
    }

    @GameTest(structure = ARENA, maxTicks = 300)
    public void goldCarriesAnimalsAndEnemies(GameTestHelper helper) {
        creatureJourney(helper, Blocks.GOLD_BLOCK, true, false);
    }

    @GameTest(structure = ARENA, maxTicks = 300)
    public void emeraldCarriesAnimalsEnemiesAndVillagers(GameTestHelper helper) {
        creatureJourney(helper, Blocks.EMERALD_BLOCK, true, true);
    }

    @GameTest(structure = ARENA, maxTicks = 300)
    public void diamondCarriesAnimalsAndEnemiesButNotVillagers(GameTestHelper helper) {
        creatureJourney(helper, Blocks.DIAMOND_BLOCK, true, false);
    }

    @GameTest(structure = ARENA, maxTicks = 300)
    public void netheriteCarriesAnimalsAndEnemiesButNotVillagers(GameTestHelper helper) {
        creatureJourney(helper, Blocks.NETHERITE_BLOCK, true, false);
    }

    @GameTest(structure = ARENA, maxTicks = 300)
    public void roseGoldAlsoCarriesUnriddenAnimalsAndEnemies(GameTestHelper helper) {
        creatureJourney(helper, BuiltInRegistries.BLOCK.getValue(Identifier.parse("additionaladditions:rose_gold_block")), true, false);
    }

    private static void creatureJourney(GameTestHelper helper, Block material, boolean carryCreatures, boolean carryVillagers) {
        build(helper, SOURCE, material, Blocks.AIR);
        build(helper, TARGET, material, Blocks.AIR);
        List<ServerPlayer> players = players(helper, 1, false);
        Mob animal = helper.spawnWithNoFreeWill(EntityType.COW, SOURCE.offset(2, 0, 2));
        Mob enemy = helper.spawnWithNoFreeWill(EntityType.HUSK, SOURCE.offset(-2, 0, 2));
        Mob villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, SOURCE.offset(2, 0, -2));
        Mob trader = helper.spawnWithNoFreeWill(EntityType.WANDERING_TRADER, SOURCE.offset(0, 0, 2));
        List<Mob> creatures = List.of(animal, enemy, villager, trader);
        creatures.forEach(mob -> mob.setNoAi(true));
        enemy.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        enemy.setHealth(13);
        animal.setCustomName(Component.literal("Meadow"));
        List<Vec3> positions = creatures.stream().map(Entity::position).toList();
        TeleportRituals.get(helper.getLevel().getServer()).start(players.getFirst(), helper.absolutePos(SOURCE), InteractionHand.MAIN_HAND);
        helper.runAtTickTime(210, () -> {
            try {
                for (int i = 0; i < creatures.size(); i++) {
                    boolean travels = i < 2 ? carryCreatures : carryVillagers;
                    helper.assertTrue(creatures.get(i).position().distanceToSqr(positions.get(i).add(travels ? 18 : 0, 0, 0)) < 1.0E-8,
                            material + " must apply the correct creature rule for " + creatures.get(i).getType());
                }
                helper.assertValueEqual(enemy.getHealth(), 13f, "Travel preserves enemy health");
                helper.assertTrue(enemy.getItemBySlot(EquipmentSlot.HEAD).is(Items.DIAMOND_HELMET), "Enemy equipment survives travel");
                helper.assertValueEqual(animal.getCustomName().getString(), "Meadow", "Named animals keep their name");
                helper.assertTrue(players.getFirst().getMainHandItem().is(Items.COMPASS), "One charge covers the player and all eligible creatures");
            } finally { remove(helper, players); creatures.forEach(Entity::discard); }
            helper.succeed();
        });
    }

    @GameTest(structure = ARENA, maxTicks = 220)
    public void automaticCreatureBoardingNeverCarriesAnUnenrolledPlayer(GameTestHelper helper) {
        build(helper, SOURCE, Blocks.GOLD_BLOCK, Blocks.AIR);
        build(helper, TARGET, Blocks.GOLD_BLOCK, Blocks.AIR);
        List<ServerPlayer> players = players(helper, 2, false);
        Horse horse = helper.spawnWithNoFreeWill(EntityType.HORSE, SOURCE.offset(2, 0, 2));
        horse.setNoAi(true);
        helper.assertTrue(players.get(1).startRiding(horse, true, false), "The bystander must be mounted");
        TeleportRituals.get(helper.getLevel().getServer()).start(players.getFirst(), helper.absolutePos(SOURCE), InteractionHand.MAIN_HAND);
        helper.runAtTickTime(80, () -> {
            try {
                helper.assertTrue(TeleportPlatform.read(helper.getLevel(), helper.absolutePos(TARGET)).contains(players.getFirst()), "The leader travels");
                helper.assertTrue(TeleportPlatform.read(helper.getLevel(), helper.absolutePos(SOURCE)).contains(horse), "An occupied bystander's mount stays behind");
                helper.assertTrue(players.get(1).getVehicle() == horse, "The bystander remains on the same mount");
            } finally { remove(helper, players); horse.discard(); }
            helper.succeed();
        });
    }

    @GameTest(structure = ARENA, maxTicks = 220)
    public void goldCannotCarryAVillagerAsAnAnimalsPassenger(GameTestHelper helper) {
        build(helper, SOURCE, Blocks.GOLD_BLOCK, Blocks.AIR);
        build(helper, TARGET, Blocks.GOLD_BLOCK, Blocks.AIR);
        List<ServerPlayer> players = players(helper, 1, false);
        Mob animal = helper.spawnWithNoFreeWill(EntityType.COW, SOURCE.offset(2, 0, 2));
        Mob villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, SOURCE.offset(1, 0, 2));
        animal.setNoAi(true);
        villager.setNoAi(true);
        helper.assertTrue(villager.startRiding(animal, true, false), "The villager is a passenger for this regression test");
        TeleportRituals.get(helper.getLevel().getServer()).start(players.getFirst(), helper.absolutePos(SOURCE), InteractionHand.MAIN_HAND);
        helper.runAtTickTime(80, () -> {
            try {
                TeleportPlatform target = TeleportPlatform.read(helper.getLevel(), helper.absolutePos(TARGET));
                helper.assertTrue(target.contains(animal), "The animal travels");
                helper.assertFalse(target.contains(villager), "Native passenger teleport must not bypass emerald-only villagers");
                helper.assertFalse(villager.isPassenger(), "NPC passengers are dismounted for travel");
            } finally { remove(helper, players); animal.discard(); villager.discard(); }
            helper.succeed();
        });
    }

    @GameTest(structure = ARENA, maxTicks = 220)
    public void blockedAnimalArrivalPreservesTheWholeDeparture(GameTestHelper helper) {
        build(helper, SOURCE, Blocks.GOLD_BLOCK, Blocks.AIR);
        build(helper, TARGET, Blocks.GOLD_BLOCK, Blocks.AIR);
        helper.setBlock(TARGET.offset(2, 1, 2), Blocks.STONE);
        List<ServerPlayer> players = players(helper, 1, false);
        Mob animal = helper.spawnWithNoFreeWill(EntityType.COW, SOURCE.offset(2, 0, 2));
        animal.setNoAi(true);
        TeleportRituals.get(helper.getLevel().getServer()).start(players.getFirst(), helper.absolutePos(SOURCE), InteractionHand.MAIN_HAND);
        helper.runAtTickTime(80, () -> {
            try {
                TeleportPlatform source = TeleportPlatform.read(helper.getLevel(), helper.absolutePos(SOURCE));
                helper.assertTrue(source.contains(animal) && source.contains(players.getFirst()), "An animal's blocked arrival cancels everyone's departure");
                helper.assertTrue(players.getFirst().getMainHandItem().is(ExplorationItems.WITHER_COMPASS), "The compass remains charged");
            } finally { remove(helper, players); animal.discard(); }
            helper.succeed();
        });
    }

    private static ItemStack bound(GameTestHelper helper, net.minecraft.world.item.Item item, int count) {
        ItemStack stack = new ItemStack(item, count);
        stack.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(
                GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(TARGET))), true));
        return stack;
    }

    private static List<ServerPlayer> players(GameTestHelper helper, int count, boolean covered) {
        List<ServerPlayer> players = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ServerPlayer player = helper.makeMockServerPlayerInLevel();
            BlockPos center = helper.absolutePos(SOURCE);
            player.setPos(center.getX() - 1.5 + i % 3 * 2, center.getY() + (covered ? 1 : 0), center.getZ() - 1.5 + i / 3 * 3);
            player.setOnGround(true);
            player.setItemInHand(InteractionHand.MAIN_HAND, i == 0 ? bound(helper, ExplorationItems.WITHER_COMPASS, 1) : ItemStack.EMPTY);
            players.add(player);
        }
        return players;
    }

    private static void remove(GameTestHelper helper, List<ServerPlayer> players) {
        for (ServerPlayer player : players) helper.getLevel().getServer().getPlayerList().remove(player);
    }

    private static void build(GameTestHelper helper, BlockPos center, Block material, Block cover) {
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            helper.setBlock(center.offset(x, -1, z), material);
            helper.setBlock(center.offset(x, 0, z), cover);
            for (int y = 1; y < 5; y++) helper.setBlock(center.offset(x, y, z), Blocks.AIR);
        }
        helper.setBlock(center, Blocks.LODESTONE);
    }
}
