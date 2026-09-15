package com.akitain.explorationreloaded.teleport;

import com.akitain.explorationreloaded.ExplorationReloaded;
import com.akitain.explorationreloaded.registry.ExplorationItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class TeleportRituals {
    private static final Map<MinecraftServer, TeleportRituals> SERVERS = new IdentityHashMap<>();
    private static final int LOAD_TIMEOUT = 200;
    private static final int TICKET_RADIUS = 2;
    private static final TicketType DESTINATION_TICKET = Registry.register(BuiltInRegistries.TICKET_TYPE,
            ExplorationReloaded.id("teleport_destination"), new TicketType(400,
                    TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION | TicketType.FLAG_KEEP_DIMENSION_ACTIVE));

    private final MinecraftServer server;
    private final Map<GlobalPos, Ritual> rituals = new LinkedHashMap<>();
    private final Set<UUID> damaged = new HashSet<>();

    private TeleportRituals(MinecraftServer server) {
        this.server = server;
    }

    public static TeleportRituals get(MinecraftServer server) {
        return SERVERS.computeIfAbsent(server, TeleportRituals::new);
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> get(server).tick());
        ServerLifecycleEvents.SERVER_STOPPED.register(SERVERS::remove);
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
            if (damageTaken > 0 && entity.level() instanceof ServerLevel level) {
                get(level.getServer()).damaged.add(entity.getUUID());
            }
        });
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (player.isSpectator() || hand != InteractionHand.MAIN_HAND || !player.getItemInHand(hand).isEmpty()
                    || !level.getBlockState(hit.getBlockPos()).is(Blocks.LODESTONE)) return InteractionResult.PASS;
            if (player instanceof ServerPlayer serverPlayer) {
                return get(serverPlayer.level().getServer()).join(serverPlayer, hit.getBlockPos())
                        ? InteractionResult.SUCCESS : InteractionResult.PASS;
            }
            return InteractionResult.PASS;
        });
    }

    public boolean start(ServerPlayer player, BlockPos pos, InteractionHand hand) {
        ItemStack compass = player.getItemInHand(hand);
        if (!compass.is(ExplorationItems.WITHER_COMPASS) || !available(player)) return false;
        if (rituals.size() >= 32 || membership(player.getUUID()) != null) return fail(player, "busy");
        ServerLevel sourceLevel = player.level();
        GlobalPos sourceKey = GlobalPos.of(sourceLevel.dimension(), pos);
        if (rituals.containsKey(sourceKey)) return fail(player, "busy");
        if (!player.mayInteract(sourceLevel, pos)) return false;
        TeleportPlatform source = TeleportPlatform.read(sourceLevel, pos);
        if (source == null) return fail(player, "invalid_platform");
        Entity root = travelRoot(player, source.material());
        if (root == null) return fail(player, "mount");
        if (!source.contains(root)) return fail(player, "outside");
        LodestoneTracker tracker = compass.get(DataComponents.LODESTONE_TRACKER);
        if (tracker == null || tracker.target().isEmpty()) return fail(player, "unbound");
        GlobalPos target = tracker.target().get();
        if (target.equals(sourceKey)) return fail(player, "same_destination");
        boolean sameDimension = target.dimension().equals(sourceLevel.dimension());
        double dx = (double) target.pos().getX() - pos.getX();
        double dz = (double) target.pos().getZ() - pos.getZ();
        if (!source.material().accepts(sameDimension, dx * dx + dz * dz)) {
            return fail(player, sameDimension ? "range" : "dimension");
        }
        ServerLevel destinationLevel = server.getLevel(target.dimension());
        if (destinationLevel == null || !destinationLevel.isInWorldBounds(target.pos())
                || !destinationLevel.getWorldBorder().isWithinBounds(target.pos())) return fail(player, "destination_missing");
        if (!root.canTeleport(sourceLevel, destinationLevel)) return fail(player, "cannot_travel");
        Component name = compass.get(DataComponents.CUSTOM_NAME);
        if (name == null) name = Component.translatable("teleport.exploration-reloaded.destination",
                target.pos().getX(), target.pos().getY(), target.pos().getZ());
        Ritual ritual = new Ritual(source, target, destinationLevel, player, root, hand, compass.copy(), name);
        rituals.put(sourceKey, ritual);
        TeleportEffects.loading(player);
        return true;
    }

    public boolean join(ServerPlayer player, BlockPos pos) {
        Ritual ritual = rituals.get(GlobalPos.of(player.level().dimension(), pos));
        if (ritual == null || !player.getMainHandItem().isEmpty()) return false;
        if (!available(player)) return true;
        if (ritual.participants.contains(player.getUUID())) {
            TeleportEffects.status(player, ritual.participants.size(), ritual.creatures.size(), ritual.remaining);
            return true;
        }
        if (membership(player.getUUID()) != null) return report(player, "busy");
        Entity root = travelRoot(player, ritual.source.material());
        if (root == null) return report(player, "mount");
        if (!ritual.source.contains(root)) return report(player, "outside");
        ritual.participants.add(player.getUUID());
        ritual.playerRoots.put(player.getUUID(), root.getUUID());
        TeleportEffects.joined(player, ritual.name);
        return true;
    }

    private Ritual membership(UUID player) {
        for (Ritual ritual : rituals.values()) {
            if (ritual.participants.contains(player)) return ritual;
        }
        return null;
    }

    public void tick() {
        List<Ritual> finished = new ArrayList<>();
        for (Ritual ritual : rituals.values()) {
            if (!tick(ritual)) finished.add(ritual);
        }
        for (Ritual ritual : finished) {
            rituals.remove(GlobalPos.of(ritual.source.level().dimension(), ritual.source.lodestone()));
            boolean sharedTicket = rituals.values().stream().anyMatch(other -> other.destinationLevel == ritual.destinationLevel
                    && other.chunk.equals(ritual.chunk));
            if (!sharedTicket) ritual.destinationLevel.getChunkSource()
                    .removeTicketWithRadius(DESTINATION_TICKET, ritual.chunk, TICKET_RADIUS);
        }
        damaged.clear();
    }

    private boolean tick(Ritual ritual) {
        ServerPlayer leader = server.getPlayerList().getPlayer(ritual.leader);
        if (!available(leader) || !ritual.source.intact()) return cancel(ritual, "interrupted");
        Entity root = travelRoot(leader, ritual.source.material());
        if (root == null || !root.getUUID().equals(ritual.root) || !ritual.source.contains(root)
                || damaged.contains(leader.getUUID()) || damaged.contains(root.getUUID())
                || !ItemStack.matches(leader.getItemInHand(ritual.hand), ritual.compass)) return cancel(ritual, "interrupted");
        for (UUID id : List.copyOf(ritual.participants)) {
            if (id.equals(ritual.leader)) continue;
            ServerPlayer passenger = server.getPlayerList().getPlayer(id);
            Entity passengerRoot = available(passenger) ? travelRoot(passenger, ritual.source.material()) : null;
            if (passengerRoot == null || !passengerRoot.getUUID().equals(ritual.playerRoots.get(id))
                    || !ritual.source.contains(passengerRoot) || damaged.contains(id) || damaged.contains(passengerRoot.getUUID())) {
                ritual.participants.remove(id);
                ritual.playerRoots.remove(id);
                if (passenger != null) TeleportEffects.message(passenger, "left");
            }
        }
        if (ritual.destination == null) {
            if (++ritual.loadingTicks > LOAD_TIMEOUT || ritual.loaded.isCompletedExceptionally()) {
                return cancel(ritual, "load_failed");
            }
            if (!ritual.loaded.isDone()) {
                if (ritual.loadingTicks % 20 == 0) TeleportEffects.loading(leader);
                return true;
            }
            ritual.destination = TeleportPlatform.read(ritual.destinationLevel, ritual.target.pos());
            if (ritual.destination == null) return cancel(ritual, "destination_missing");
            if (ritual.destination.block() != ritual.source.block()) return cancel(ritual, "material_mismatch");
            for (Mob mob : ritual.source.level().getEntitiesOfClass(Mob.class,
                    new AABB(ritual.source.lodestone()).inflate(4, 2, 4),
                    entity -> eligibleCreature(entity, ritual.source))) {
                ritual.creatures.add(mob.getUUID());
            }
            TeleportEffects.begin(ritual.source);
        }
        ritual.creatures.removeIf(id -> {
            Entity entity = ritual.source.level().getEntity(id);
            return !(entity instanceof Mob mob) || !eligibleCreature(mob, ritual.source) || damaged.contains(id);
        });
        if (ritual.remaining % 5 == 0) {
            TeleportEffects.boundary(ritual.source, ritual.remaining, ritual.source.material().duration);
            for (UUID id : ritual.participants) {
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                if (player != null) {
                    TeleportEffects.selected(player, ritual.source.material().color, ritual.remaining);
                    if (ritual.remaining % 20 == 0) {
                        if (ritual.remaining == ritual.source.material().duration) {
                            TeleportEffects.destination(player, ritual.name);
                        } else {
                            TeleportEffects.status(player, ritual.participants.size(), ritual.creatures.size(), ritual.remaining);
                        }
                    }
                }
            }
            for (UUID id : ritual.creatures) {
                Entity creature = ritual.source.level().getEntity(id);
                if (creature != null) TeleportEffects.selected(creature, ritual.source.material().color, ritual.remaining);
            }
        }
        if (ritual.remaining % 20 == 0) {
            TeleportEffects.pulse(ritual.source, ritual.remaining, ritual.source.material().duration);
            for (ServerPlayer nearby : ritual.source.level().players()) {
                if (available(nearby) && ritual.source.contains(nearby.getRootVehicle()) && !ritual.participants.contains(nearby.getUUID())) {
                    TeleportEffects.invite(nearby, ritual.name);
                }
            }
        }
        if (--ritual.remaining > 0) return true;
        finish(ritual, leader, root);
        return false;
    }

    private void finish(Ritual ritual, ServerPlayer leader, Entity leaderRoot) {
        if (!ritual.destination.intact()) {
            cancel(ritual, "destination_missing");
            return;
        }
        List<Journey> journeys = new ArrayList<>();
        Set<UUID> travellers = new HashSet<>(ritual.participants);
        travellers.addAll(ritual.playerRoots.values());
        travellers.addAll(ritual.creatures);
        for (UUID id : ritual.participants) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            Entity root = id.equals(ritual.leader) ? leaderRoot : travelRoot(player, ritual.source.material());
            if (!available(player) || root == null || !root.canTeleport(ritual.source.level(), ritual.destinationLevel)) {
                cancel(ritual, "cannot_travel");
                return;
            }
            TeleportPlatform.Landing landing = ritual.destination.relativeLanding(root, player, ritual.source, travellers);
            if (landing == null) {
                cancel(ritual, "arrival_blocked");
                return;
            }
            journeys.add(new Journey(player, root, landing.feet(), root.position(), root.getYRot(), root.getXRot()));
        }
        for (UUID id : ritual.creatures) {
            Entity creature = ritual.source.level().getEntity(id);
            if (!(creature instanceof Mob mob) || !eligibleCreature(mob, ritual.source)
                    || !creature.canTeleport(ritual.source.level(), ritual.destinationLevel)) {
                cancel(ritual, "cannot_travel");
                return;
            }
            TeleportPlatform.Landing landing = ritual.destination.relativeLanding(creature, creature, ritual.source, travellers);
            if (landing == null) {
                cancel(ritual, "arrival_blocked");
                return;
            }
            journeys.add(new Journey(null, creature, landing.feet(), creature.position(), creature.getYRot(), creature.getXRot()));
        }
        Map<UUID, UUID> originalVehicles = new LinkedHashMap<>();
        for (Journey journey : journeys) {
            if (journey.player != null) continue;
            if (journey.root.getVehicle() != null) originalVehicles.put(journey.root.getUUID(), journey.root.getVehicle().getUUID());
            for (Entity passenger : journey.root.getPassengers()) originalVehicles.put(passenger.getUUID(), journey.root.getUUID());
        }
        // Native teleport recursively carries passengers. Detach NPCs only after every arrival is valid.
        for (Journey journey : journeys) {
            if (journey.player == null) {
                journey.root.ejectPassengers();
                journey.root.stopRiding();
            }
        }
        List<Arrival> moved = new ArrayList<>();
        for (Journey journey : journeys) {
            Entity arrived = journey.root.teleport(new TeleportTransition(ritual.destinationLevel, journey.arrival,
                    Vec3.ZERO, journey.yaw, journey.pitch, TeleportTransition.PLACE_PORTAL_TICKET));
            if (arrived == null) {
                for (Arrival previous : moved) {
                    // Cross-dimensional mob travel returns a new entity; the old source entity is removed.
                    previous.entity.teleport(new TeleportTransition(ritual.source.level(), previous.journey.departure,
                            Vec3.ZERO, previous.journey.yaw, previous.journey.pitch, TeleportTransition.PLACE_PORTAL_TICKET));
                }
                originalVehicles.forEach((passengerId, vehicleId) -> {
                    Entity passenger = ritual.source.level().getEntity(passengerId);
                    Entity vehicle = ritual.source.level().getEntity(vehicleId);
                    if (passenger != null && vehicle != null) passenger.startRiding(vehicle, true, false);
                });
                cancel(ritual, "cannot_travel");
                return;
            }
            moved.add(new Arrival(journey, arrived));
            arrived.resetFallDistance();
            if (journey.player != null) {
                journey.player.resetFallDistance();
                if (arrived != journey.player) arrived.addTag("tp");
            }
            if (arrived instanceof PathfinderMob mob) mob.getNavigation().stop();
        }
        leader.setItemInHand(ritual.hand, ritual.compass.transmuteCopy(Items.COMPASS, 1));
        TeleportEffects.depart(ritual.source);
        TeleportEffects.arrive(ritual.destination);
        for (Journey journey : journeys) {
            if (journey.player != null) {
                journey.player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                TeleportEffects.message(journey.player, "arrived");
            }
        }
    }

    private boolean cancel(Ritual ritual, String reason) {
        TeleportEffects.cancel(ritual.source);
        for (UUID id : ritual.participants) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) TeleportEffects.message(player, reason);
        }
        return false;
    }

    private static boolean eligibleCreature(Mob mob, TeleportPlatform platform) {
        return mob.isAlive() && !mob.isRemoved() && platform.material().carries(mob)
                && platform.contains(mob) && mob.countPlayerPassengers() == 0;
    }

    private static boolean available(ServerPlayer player) {
        return player != null && player.isAlive() && !player.isRemoved() && !player.isSpectator()
                && !player.isSleeping() && !player.isFallFlying() && !player.hasDisconnected();
    }

    private static Entity travelRoot(ServerPlayer player, PlatformMaterial material) {
        if (player == null) return null;
        if (!player.isPassenger()) return player;
        Entity root = player.getRootVehicle();
        if (material != PlatformMaterial.ROSE_GOLD || !(root instanceof LivingEntity) || !root.isAlive()
                || root.getControllingPassenger() != player || root.getPassengers().size() != 1
                || root.getPassengers().getFirst() != player) return null;
        return root;
    }

    private static boolean fail(ServerPlayer player, String reason) {
        TeleportEffects.message(player, reason);
        return false;
    }

    private static boolean report(ServerPlayer player, String reason) {
        TeleportEffects.message(player, reason);
        return true;
    }

    private record Arrival(Journey journey, Entity entity) {}

    private record Journey(ServerPlayer player, Entity root, Vec3 arrival, Vec3 departure, float yaw, float pitch) {}

    private static final class Ritual {
        final TeleportPlatform source;
        final GlobalPos target;
        final ServerLevel destinationLevel;
        final ChunkPos chunk;
        final UUID leader;
        final UUID root;
        final InteractionHand hand;
        final ItemStack compass;
        final Component name;
        final Set<UUID> participants = new LinkedHashSet<>();
        final Map<UUID, UUID> playerRoots = new HashMap<>();
        final Set<UUID> creatures = new LinkedHashSet<>();
        final CompletableFuture<?> loaded;
        TeleportPlatform destination;
        int loadingTicks;
        int remaining;

        Ritual(TeleportPlatform source, GlobalPos target, ServerLevel destinationLevel, ServerPlayer player,
               Entity root, InteractionHand hand, ItemStack compass, Component name) {
            this.source = source;
            this.target = target;
            this.destinationLevel = destinationLevel;
            this.chunk = ChunkPos.containing(target.pos());
            this.leader = player.getUUID();
            this.root = root.getUUID();
            this.hand = hand;
            this.compass = compass;
            this.name = name;
            this.remaining = source.material().duration;
            participants.add(leader);
            playerRoots.put(leader, root.getUUID());
            loaded = destinationLevel.getChunkSource().addTicketAndLoadWithRadius(DESTINATION_TICKET, chunk, TICKET_RADIUS);
        }
    }
}
