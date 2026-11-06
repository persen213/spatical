package dev.spatial.inversion;

import io.github.pinchan4273.reacademycraft.api.ActivationOutcome;
import io.github.pinchan4273.reacademycraft.api.mc.AcademySafety;
import io.github.pinchan4273.reacademycraft.api.mc.ActivationContext;
import io.github.pinchan4273.reacademycraft.api.mc.SkillBehavior;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Ability 5: Inversion Strike.
 * Opens a folded spatial channel and unleashes a concentrated dimensional burst at the target location.
 */
public final class InversionStrikeSkill implements SkillBehavior {

    @Override
    public ActivationOutcome start(ActivationContext c) {
        ServerPlayer player = c.naturalUser();
        if (player == null) return ActivationOutcome.REFUSED_INVALID;

        if (!c.pay(SpatialInversionCosts.STRIKE_CP, SpatialInversionCosts.STRIKE_OVERLOAD)) {
            return ActivationOutcome.REFUSED_RESOURCE;
        }

        ServerLevel level = player.serverLevel();
        Vec3 startPoint = player.getEyePosition().add(player.getLookAngle().scale(1.2));
        Vec3 targetPoint = SpatialTargeting.point(player, SpatialInversionConfig.STRIKE_RANGE);

        // Calculate offset angle for unexpected flank emergence
        Vec3 strikeOrigin = targetPoint.add(0, 3.5, 0);

        int visId = SpatialInversionState.nextVisualId();
        SpatialNetwork.broadcastEffect(level, visId, SyncSpatialEffectPacket.TYPE_INVERSION_STRIKE,
                SyncSpatialEffectPacket.ACTION_ADD, strikeOrigin, targetPoint, 1.8f, 15);

        level.playSound(null, targetPoint.x, targetPoint.y, targetPoint.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.8f, 1.6f);
        level.playSound(null, startPoint.x, startPoint.y, startPoint.z, SoundEvents.RESPAWN_ANCHOR_DEPLETE.get(), SoundSource.PLAYERS, 1.4f, 1.8f);

        // Apply damage to entities within 3.5 blocks of target point
        float damage = SpatialInversionConfig.STRIKE_BASE_DAMAGE + SpatialInversionConfig.STRIKE_PROFICIENCY_BONUS * c.proficiency();
        List<LivingEntity> targets = SpatialTargeting.within(level, targetPoint, 3.5, player);
        int hitCount = 0;

        for (LivingEntity e : targets) {
            if (AcademySafety.attack(c.actor(), e, SpatialInversionMod.INVERSION_STRIKE, damage)) {
                // Downward dimensional impact
                e.setDeltaMovement(new Vec3(0, -0.6, 0));
                e.hurtMarked = true;
                hitCount++;
            }
        }

        if (hitCount > 0) {
            c.addProficiency(0.02f * Math.min(hitCount, 3));
        }

        c.setCooldown(SpatialInversionConfig.STRIKE_COOLDOWN_TICKS);
        return ActivationOutcome.STARTED;
    }
}
