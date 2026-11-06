package dev.spatial.inversion;

import io.github.pinchan4273.reacademycraft.api.ActivationOutcome;
import io.github.pinchan4273.reacademycraft.api.mc.ActivationContext;
import io.github.pinchan4273.reacademycraft.api.mc.SkillBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Ability 1: Spatial Link.
 * Creates two connected spatial anchors allowing objects and projectiles to pass between them.
 */
public final class SpatialLinkSkill implements SkillBehavior {

    @Override
    public ActivationOutcome start(ActivationContext c) {
        ServerPlayer player = c.naturalUser();
        if (player == null) return ActivationOutcome.REFUSED_INVALID;

        if (!c.pay(SpatialInversionCosts.LINK_CP, SpatialInversionCosts.LINK_OVERLOAD)) {
            return ActivationOutcome.REFUSED_RESOURCE;
        }

        ServerLevel level = player.serverLevel();
        Vec3 anchorA = player.position().add(player.getLookAngle().scale(1.5)).add(0, 1.0, 0);
        Vec3 targetPoint = SpatialTargeting.point(player, SpatialInversionConfig.LINK_MAX_PLACE_RANGE);

        // Clamp distance between anchors to maximum limit
        if (anchorA.distanceTo(targetPoint) > SpatialInversionConfig.LINK_MAX_BETWEEN_ANCHORS) {
            Vec3 dir = targetPoint.subtract(anchorA).normalize();
            targetPoint = anchorA.add(dir.scale(SpatialInversionConfig.LINK_MAX_BETWEEN_ANCHORS));
        }
        Vec3 anchorB = targetPoint;

        SpatialInversionState state = SpatialInversionState.of(player);
        int visId = SpatialInversionState.nextVisualId();
        state.linkVisualId = visId;
        state.linkAnchorA = anchorA;
        state.linkAnchorB = anchorB;
        state.linkRemainingTicks = SpatialInversionConfig.LINK_DURATION_TICKS;

        // Broadcast 3D geometric visual to clients (Zero Particles)
        SpatialNetwork.broadcastEffect(level, visId, SyncSpatialEffectPacket.TYPE_ANCHOR_PAIR,
                SyncSpatialEffectPacket.ACTION_ADD, anchorA, anchorB, 1.2f, SpatialInversionConfig.LINK_DURATION_TICKS);

        level.playSound(null, anchorA.x, anchorA.y, anchorA.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.4f, 1.5f);
        level.playSound(null, anchorB.x, anchorB.y, anchorB.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.4f, 1.5f);

        c.setCooldown(SpatialInversionConfig.LINK_COOLDOWN_TICKS);
        c.addProficiency(0.015f);
        return ActivationOutcome.STARTED;
    }
}
