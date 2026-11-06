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
 * Ability 2: Vector Redirect.
 * Creates a directional spatial aperture that catches incoming projectiles and redirects them forward.
 */
public final class VectorRedirectSkill implements SkillBehavior {

    @Override
    public ActivationOutcome start(ActivationContext c) {
        ServerPlayer player = c.naturalUser();
        if (player == null) return ActivationOutcome.REFUSED_INVALID;

        if (!c.pay(SpatialInversionCosts.REDIRECT_CP, SpatialInversionCosts.REDIRECT_OVERLOAD)) {
            return ActivationOutcome.REFUSED_RESOURCE;
        }

        ServerLevel level = player.serverLevel();
        Vec3 aperture = player.getEyePosition().add(player.getLookAngle().scale(1.8));
        Vec3 direction = player.getLookAngle();

        SpatialInversionState state = SpatialInversionState.of(player);
        int visId = SpatialInversionState.nextVisualId();
        state.redirectVisualId = visId;
        state.redirectAperturePos = aperture;
        state.redirectDirection = direction;
        state.redirectRemainingTicks = SpatialInversionConfig.REDIRECT_DURATION_TICKS;

        SpatialNetwork.broadcastEffect(level, visId, SyncSpatialEffectPacket.TYPE_VECTOR_REDIRECT,
                SyncSpatialEffectPacket.ACTION_ADD, aperture, Vec3.ZERO, 1.8f, SpatialInversionConfig.REDIRECT_DURATION_TICKS);

        level.playSound(null, aperture.x, aperture.y, aperture.z, SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.5f, 1.8f);

        c.setCooldown(SpatialInversionConfig.REDIRECT_COOLDOWN_TICKS);
        c.addProficiency(0.015f);
        return ActivationOutcome.STARTED;
    }
}
