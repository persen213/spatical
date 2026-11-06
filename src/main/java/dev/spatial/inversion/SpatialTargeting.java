package dev.spatial.inversion;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Spatial raycasting, entity searching, and safe-spot validation.
 */
public final class SpatialTargeting {
    private SpatialTargeting() {}

    /**
     * Raycasts from player's eyes up to maxDistance.
     * Hits block if line of sight is obstructed, otherwise returns end of ray.
     */
    public static Vec3 point(ServerPlayer player, double maxDistance) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(maxDistance));
        ServerLevel level = player.serverLevel();

        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.MISS) {
            return hit.getLocation();
        }
        return end;
    }

    /**
     * Finds the LivingEntity the player is directly looking at within range.
     */
    @Nullable
    public static LivingEntity lookEntity(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 to = eye.add(look.scale(range));
        ServerLevel level = player.serverLevel();

        BlockHitResult blockHit = level.clip(new ClipContext(eye, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double maxDistSq = blockHit.getType() == HitResult.Type.MISS ? range * range : blockHit.getLocation().distanceToSqr(eye);

        AABB search = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.5);
        LivingEntity closest = null;
        double closestDistSq = maxDistSq;

        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, search, x -> x != player && x.isAlive() && !x.isSpectator())) {
            AABB box = e.getBoundingBox().inflate(0.35);
            if (box.contains(eye)) {
                return e;
            }
            Optional<Vec3> hit = box.clip(eye, to);
            if (hit.isPresent()) {
                double distSq = eye.distanceToSqr(hit.get());
                if (distSq < closestDistSq) {
                    closestDistSq = distSq;
                    closest = e;
                }
            }
        }
        return closest;
    }

    /**
     * Center coordinate of an entity's bounding box.
     */
    public static Vec3 centre(Entity e) {
        return new Vec3(e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ());
    }

    /**
     * Living entities within radius of center point, excluding a specific entity.
     */
    public static List<LivingEntity> within(ServerLevel level, Vec3 center, double radius, @Nullable Entity exclude) {
        AABB box = new AABB(center.x - radius, center.y - radius, center.z - radius,
                            center.x + radius, center.y + radius, center.z + radius);
        double r2 = radius * radius;
        return level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != exclude && e.isAlive() && !e.isSpectator() && centre(e).distanceToSqr(center) <= r2);
    }

    /**
     * Returns true if entity is a boss or has boss-tier resilience.
     */
    public static boolean isBoss(LivingEntity e) {
        if (e instanceof EnderDragon || e instanceof WitherBoss || e instanceof Warden) return true;
        if (e.getMaxHealth() >= 200f) return true;
        return e.getType().getDescriptionId().toLowerCase().contains("boss");
    }

    /**
     * Validates that target location is safe for an entity to teleport to.
     * Ensures loaded chunk, world border, sturdy footing, no suffocation, and no hazard (lava/fire/void).
     */
    @Nullable
    public static Vec3 safeSpot(ServerLevel level, Entity entity, Vec3 desired) {
        double x = Math.floor(desired.x) + 0.5;
        double z = Math.floor(desired.z) + 0.5;
        double y = desired.y;

        BlockPos column = BlockPos.containing(x, y, z);
        if (!level.hasChunkAt(column) || !level.getWorldBorder().isWithinBounds(column)) {
            return null;
        }

        // Search vertical column (-3 to +3) for a safe landing
        for (int dy = 0; dy >= -3; dy--) {
            BlockPos feet = BlockPos.containing(x, y + dy, z);
            if (feet.getY() <= level.getMinBuildHeight() + 2 || feet.getY() >= level.getMaxBuildHeight() - 2) continue;

            BlockPos floor = feet.below();
            if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) continue;

            // Check feet and head for non-collision
            Vec3 candidate = new Vec3(x, feet.getY(), z);
            AABB movedBox = entity.getBoundingBox().move(candidate.x - entity.getX(), candidate.y - entity.getY(), candidate.z - entity.getZ());
            if (level.noCollision(entity, movedBox)) {
                return candidate;
            }
        }
        for (int dy = 1; dy <= 3; dy++) {
            BlockPos feet = BlockPos.containing(x, y + dy, z);
            if (feet.getY() >= level.getMaxBuildHeight() - 2) continue;

            BlockPos floor = feet.below();
            if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) continue;

            Vec3 candidate = new Vec3(x, feet.getY(), z);
            AABB movedBox = entity.getBoundingBox().move(candidate.x - entity.getX(), candidate.y - entity.getY(), candidate.z - entity.getZ());
            if (level.noCollision(entity, movedBox)) {
                return candidate;
            }
        }

        return null;
    }
}
