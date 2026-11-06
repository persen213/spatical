package dev.spatial.inversion;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Server-side tick processor for projectile deflection, link transport, and spatial containment.
 */
@Mod.EventBusSubscriber(modid = SpatialInversionMod.MOD_ID)
public final class SpatialInversionEvents {
    private SpatialInversionEvents() {}

    private static final String TAG_REDIRECTS = "spatialinversion:hops";
    private static final String TAG_LAST_TICK = "spatialinversion:last_teleport";

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        SpatialScheduler.run(server);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            SpatialInversionState state = SpatialInversionState.peek(player);
            if (state == null) continue;
            ServerLevel level = player.serverLevel();
            long currentTick = server.getTickCount();

            // 1. Spatial Link Tick & Transport
            if (state.linkRemainingTicks > 0 && state.linkAnchorA != null && state.linkAnchorB != null) {
                state.linkRemainingTicks--;
                processSpatialLink(level, player, state, currentTick);
                if (state.linkRemainingTicks == 0) {
                    SpatialNetwork.broadcastEffect(level, state.linkVisualId, SyncSpatialEffectPacket.TYPE_ANCHOR_PAIR,
                            SyncSpatialEffectPacket.ACTION_REMOVE, Vec3.ZERO, Vec3.ZERO, 0f, 0);
                    state.linkAnchorA = null;
                    state.linkAnchorB = null;
                }
            }

            // 2. Vector Redirect Tick & Deflection
            if (state.redirectRemainingTicks > 0 && state.redirectAperturePos != null) {
                state.redirectRemainingTicks--;
                processVectorRedirect(level, player, state, currentTick);
                if (state.redirectRemainingTicks == 0) {
                    SpatialNetwork.broadcastEffect(level, state.redirectVisualId, SyncSpatialEffectPacket.TYPE_VECTOR_REDIRECT,
                            SyncSpatialEffectPacket.ACTION_REMOVE, Vec3.ZERO, Vec3.ZERO, 0f, 0);
                    state.redirectAperturePos = null;
                    state.redirectDirection = null;
                }
            }

            // 3. Distortion Field Tick & Projectile Bending
            if (state.distortionRemainingTicks > 0 && state.distortionField) {
                state.distortionRemainingTicks--;
                processDistortionField(level, player, currentTick);
                if (state.distortionRemainingTicks == 0) {
                    state.distortionField = false;
                    SpatialNetwork.broadcastEffect(level, state.distortionVisualId, SyncSpatialEffectPacket.TYPE_DISTORTION_FIELD,
                            SyncSpatialEffectPacket.ACTION_REMOVE, Vec3.ZERO, Vec3.ZERO, 0f, 0);
                }
            }

            // 4. Spatial Cage Tick & Containment
            if (state.cageRemainingTicks > 0 && state.cageCenter != null) {
                state.cageRemainingTicks--;
                processSpatialCage(level, state);
                if (state.cageRemainingTicks == 0) {
                    SpatialNetwork.broadcastEffect(level, state.cageVisualId, SyncSpatialEffectPacket.TYPE_SPATIAL_CAGE,
                            SyncSpatialEffectPacket.ACTION_REMOVE, Vec3.ZERO, Vec3.ZERO, 0f, 0);
                    state.cageCenter = null;
                }
            }

            // 5. Absolute Inversion Manifold Tick & Inversion
            if (state.absoluteRemainingTicks > 0 && state.absoluteManifoldA != null && state.absoluteManifoldB != null) {
                state.absoluteRemainingTicks--;
                processAbsoluteInversion(level, player, state, currentTick);
                if (state.absoluteRemainingTicks == 0 || state.absoluteCapacity <= 0) {
                    SpatialNetwork.broadcastEffect(level, state.absoluteVisualId, SyncSpatialEffectPacket.TYPE_ABSOLUTE_MANIFOLD,
                            SyncSpatialEffectPacket.ACTION_REMOVE, Vec3.ZERO, Vec3.ZERO, 0f, 0);
                    state.absoluteManifoldA = null;
                    state.absoluteManifoldB = null;
                }
            }
        }
    }

    private static void processSpatialLink(ServerLevel level, ServerPlayer owner, SpatialInversionState s, long tick) {
        double radius = 1.6;
        for (Projectile proj : getNearbyProjectiles(level, s.linkAnchorA, radius)) {
            if (canRedirect(proj, tick)) {
                teleportProjectile(proj, s.linkAnchorA, s.linkAnchorB, tick);
                level.playSound(null, s.linkAnchorA.x, s.linkAnchorA.y, s.linkAnchorA.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.2f, 1.4f);
            }
        }
        for (Projectile proj : getNearbyProjectiles(level, s.linkAnchorB, radius)) {
            if (canRedirect(proj, tick)) {
                teleportProjectile(proj, s.linkAnchorB, s.linkAnchorA, tick);
                level.playSound(null, s.linkAnchorB.x, s.linkAnchorB.y, s.linkAnchorB.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.2f, 1.4f);
            }
        }
    }

    private static void processVectorRedirect(ServerLevel level, ServerPlayer owner, SpatialInversionState s, long tick) {
        Vec3 aperture = s.redirectAperturePos;
        Vec3 dir = s.redirectDirection != null ? s.redirectDirection : owner.getLookAngle();
        for (Projectile proj : getNearbyProjectiles(level, aperture, 3.2)) {
            if (canRedirect(proj, tick)) {
                recordHop(proj, tick);
                double speed = Math.max(1.2, proj.getDeltaMovement().length() * 1.15);
                proj.setDeltaMovement(dir.scale(speed));
                proj.hurtMarked = true;
                level.playSound(null, aperture.x, aperture.y, aperture.z, SoundEvents.SHULKER_BULLET_HIT, SoundSource.PLAYERS, 1.5f, 1.6f);
            }
        }
    }

    private static void processDistortionField(ServerLevel level, ServerPlayer player, long tick) {
        Vec3 center = player.position().add(0, 1.0, 0);
        double rad = SpatialInversionConfig.DISTORTION_RADIUS;
        for (Projectile proj : getNearbyProjectiles(level, center, rad)) {
            if (proj.getOwner() == player) continue;
            Vec3 toProj = proj.position().subtract(center);
            if (toProj.lengthSqr() < 0.01) continue;
            Vec3 radial = toProj.normalize().scale(0.35);
            proj.setDeltaMovement(proj.getDeltaMovement().add(radial));
            proj.hurtMarked = true;
        }
    }

    private static void processSpatialCage(ServerLevel level, SpatialInversionState s) {
        Vec3 center = s.cageCenter;
        double r = SpatialInversionConfig.CAGE_RADIUS;
        AABB box = new AABB(center.x - r, center.y - 1.0, center.z - r,
                            center.x + r, center.y + 4.5, center.z + r);

        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box.inflate(1.0), LivingEntity::isAlive)) {
            double distSq = e.position().subtract(center).horizontalDistanceSqr();
            if (distSq > (r - 0.5) * (r - 0.5)) {
                // Soft boundary repulsion
                Vec3 inward = center.subtract(e.position()).normalize().scale(0.25);
                e.setDeltaMovement(new Vec3(inward.x, 0.05, inward.z));
                e.hurtMarked = true;
            }
        }
    }

    private static void processAbsoluteInversion(ServerLevel level, ServerPlayer owner, SpatialInversionState s, long tick) {
        double radius = 3.2;
        for (Projectile proj : getNearbyProjectiles(level, s.absoluteManifoldA, radius)) {
            if (canRedirect(proj, tick)) {
                teleportProjectile(proj, s.absoluteManifoldA, s.absoluteManifoldB, tick);
                s.absoluteCapacity--;
                level.playSound(null, s.absoluteManifoldA.x, s.absoluteManifoldA.y, s.absoluteManifoldA.z, SoundEvents.RESPAWN_ANCHOR_DEPLETE.get(), SoundSource.PLAYERS, 1.8f, 1.2f);
            }
        }
    }

    private static boolean canRedirect(Projectile proj, long currentTick) {
        CompoundTag tag = proj.getPersistentData();
        int hops = tag.getInt(TAG_REDIRECTS);
        if (hops >= SpatialInversionConfig.MAX_PROJECTILE_HOPS) {
            proj.discard();
            return false;
        }
        long last = tag.getLong(TAG_LAST_TICK);
        return (currentTick - last) >= 2;
    }

    private static void recordHop(Projectile proj, long currentTick) {
        CompoundTag tag = proj.getPersistentData();
        tag.putInt(TAG_REDIRECTS, tag.getInt(TAG_REDIRECTS) + 1);
        tag.putLong(TAG_LAST_TICK, currentTick);
    }

    private static void teleportProjectile(Projectile proj, Vec3 fromAnchor, Vec3 toAnchor, long tick) {
        recordHop(proj, tick);
        Vec3 offset = proj.position().subtract(fromAnchor);
        Vec3 newPos = toAnchor.add(offset.normalize().scale(0.8));
        proj.teleportTo(newPos.x, newPos.y, newPos.z);
        proj.hurtMarked = true;
    }

    private static List<Projectile> getNearbyProjectiles(ServerLevel level, Vec3 center, double radius) {
        AABB box = new AABB(center.x - radius, center.y - radius, center.z - radius,
                            center.x + radius, center.y + radius, center.z + radius);
        return level.getEntitiesOfClass(Projectile.class, box, Projectile::isAlive);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SpatialInversionState.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        SpatialInversionState.clearAll();
        SpatialScheduler.clear();
    }
}
