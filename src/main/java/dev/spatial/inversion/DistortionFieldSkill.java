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
 * Ability 4: Distortion Field.
 * Generates a spherical spatial curvature field around the player that deflects incoming projectiles.
 */
public final class DistortionFieldSkill implements SkillBehavior {

    @Override
    public ActivationOutcome start(ActivationContext c) {
        ServerPlayer player = c.naturalUser();
        if (player == null) return ActivationOutcome.REFUSED_INVALID;

        if (!c.pay(SpatialInversionCosts.DISTORTION_CP, SpatialInversionCosts.DISTORTION_OVERLOAD)) {
            return ActivationOutcome.REFUSED_RESOURCE;
        }

        ServerLevel level = player.serverLevel();
        SpatialInversionState state = SpatialInversionState.of(player);

        int visId = SpatialInversionState.nextVisualId();
        state.distortionVisualId = visId;
        state.distortionField = true;
        state.distortionRemainingTicks = SpatialInversionConfig.DISTORTION_DURATION_TICKS;

        Vec3 center = player.position().add(0, 1.0, 0);
        SpatialNetwork.broadcastEffect(level, visId, SyncSpatialEffectPacket.TYPE_DISTORTION_FIELD,
                SyncSpatialEffectPacket.ACTION_ADD, center, Vec3.ZERO, (float) SpatialInversionConfig.DISTORTION_RADIUS,
                SpatialInversionConfig.DISTORTION_DURATION_TICKS);

        level.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 1.6f, 0.7f);

        c.setCooldown(SpatialInversionConfig.DISTORTION_COOLDOWN_TICKS);
        c.addProficiency(0.02f);
        return ActivationOutcome.STARTED;
    }
}
