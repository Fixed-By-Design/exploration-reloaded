package com.akitain.explorationreloaded.teleport;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Set;
import java.util.UUID;

public record TeleportPlatform(ServerLevel level, BlockPos lodestone, Block block, PlatformMaterial material) {
    public static final int RADIUS = 3;
    public static final double MAX_FLOOR_HEIGHT = 1.25;

    public static TeleportPlatform read(ServerLevel level, BlockPos center) {
        if (!level.hasChunkAt(center) || !level.getBlockState(center).is(Blocks.LODESTONE)) return null;
        BlockState base = level.getBlockState(center.below());
        PlatformMaterial material = PlatformMaterial.of(base);
        if (material == null) return null;
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                BlockPos pos = center.offset(x, -1, z);
                if (!level.hasChunkAt(pos) || !level.getBlockState(pos).is(base.getBlock())) return null;
            }
        }
        return new TeleportPlatform(level, center.immutable(), base.getBlock(), material);
    }

    public boolean intact() {
        return equals(read(level, lodestone));
    }

    public boolean contains(Entity entity) {
        Vec3 feet = entity.position();
        return entity.level() == level && insideHorizontal(feet.x, feet.z)
                && feet.y >= lodestone.getY() - 0.05
                && feet.y <= lodestone.getY() + MAX_FLOOR_HEIGHT;
    }

    private boolean insideHorizontal(double x, double z) {
        return x >= lodestone.getX() - RADIUS && x < lodestone.getX() + RADIUS + 1
                && z >= lodestone.getZ() - RADIUS && z < lodestone.getZ() + RADIUS + 1;
    }

    public double surfaceHeight(double x, double z) {
        double top = lodestone.getY();
        for (int dy = 0; dy <= 1; dy++) {
            BlockPos pos = BlockPos.containing(x, lodestone.getY() + dy, z);
            VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
            if (!shape.isEmpty()) top = Math.max(top, pos.getY() + shape.max(Direction.Axis.Y));
        }
        return Math.min(top, lodestone.getY() + MAX_FLOOR_HEIGHT) + 0.06;
    }

    public Landing relativeLanding(Entity root, Entity rider, TeleportPlatform source, Set<UUID> travellers) {
        Vec3 feet = root.position().add(lodestone.getX() - source.lodestone.getX(),
                lodestone.getY() - source.lodestone.getY(), lodestone.getZ() - source.lodestone.getZ());
        double sourceFloor = source.floorHeight(root, root.position(), root.getY());
        double destinationFloor = floorHeight(root, feet, lodestone.getY() + MAX_FLOOR_HEIGHT);
        if (!Double.isFinite(sourceFloor) || !Double.isFinite(destinationFloor)) return null;
        feet = new Vec3(feet.x, destinationFloor + Math.max(0, root.getY() - sourceFloor), feet.z);
        if (!insideHorizontal(feet.x, feet.z) || feet.y < lodestone.getY() - 0.05
                || feet.y > lodestone.getY() + MAX_FLOOR_HEIGHT) return null;
        AABB body = root.getDimensions(Pose.STANDING).makeBoundingBox(feet);
        AABB occupied = body;
        if (root != rider) {
            Vec3 riderFeet = feet.add(rider.position().subtract(root.position()));
            AABB riderBody = rider.getDimensions(Pose.STANDING).makeBoundingBox(riderFeet);
            if (!clearBody(rider, riderBody)) return null;
            occupied = occupied.minmax(riderBody);
        }
        if (!level.getWorldBorder().isWithinBounds(occupied) || !clearBody(root, body)
                || !safeBlocks(root, occupied) || !safeBlocks(rider, occupied)) return null;
        // Travellers keep any existing overlap. Only entities staying behind can obstruct an arrival.
        if (!level.getEntities(root, occupied, other -> other != rider && !travellers.contains(other.getUUID())
                && !other.isSpectator() && (other instanceof LivingEntity || other.isVehicle())).isEmpty()) return null;
        return new Landing(feet, occupied);
    }

    private double floorHeight(Entity entity, Vec3 feet, double ceiling) {
        AABB body = entity.getDimensions(Pose.STANDING).makeBoundingBox(feet);
        double limit = Math.min(ceiling, lodestone.getY() + MAX_FLOOR_HEIGHT);
        AABB column = new AABB(body.minX, lodestone.getY() - 0.01, body.minZ,
                body.maxX, limit + 1.0E-7, body.maxZ);
        BlockCollisions<VoxelShape> collisions = new BlockCollisions<>(level,
                CollisionContext.withPosition(entity, limit + 1.0E-7), column, false, (pos, shape) -> shape);
        double top = Double.NEGATIVE_INFINITY;
        while (collisions.hasNext()) {
            for (AABB surface : collisions.next().toAabbs()) {
                if (surface.intersects(column) && surface.maxY <= limit + 1.0E-7) {
                    top = Math.max(top, surface.maxY);
                }
            }
        }
        return top;
    }

    private boolean clearBody(Entity entity, AABB body) {
        return !new BlockCollisions<>(level, CollisionContext.withPosition(entity, body.minY),
                body, false, (pos, shape) -> shape).hasNext();
    }

    private boolean safeBlocks(Entity entity, AABB box) {
        // Include the support and any short fall when someone departs above the floor.
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX + 0.001, lodestone.getY() - 0.01, box.minZ + 0.001),
                BlockPos.containing(box.maxX - 0.001, box.maxY - 0.001, box.maxZ - 0.001))) {
            BlockState state = level.getBlockState(pos);
            if (!state.getFluidState().isEmpty() || entity.getType().isBlockDangerous(state)
                    || state.is(Blocks.POWDER_SNOW) || state.is(Blocks.WITHER_ROSE)
                    || state.is(Blocks.SWEET_BERRY_BUSH)) return false;
        }
        return true;
    }

    public record Landing(Vec3 feet, AABB space) {}
}
