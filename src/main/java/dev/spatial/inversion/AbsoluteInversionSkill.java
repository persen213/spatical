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
 * Ability 8: Absolute Inversion (Ultimate).
 * Deploys two massive spatial inversion manifolds that reverse the directional relationship between two regions.
 * Catches and inverts enemy projectiles and spatial attacks with high capacity.
 */
public final class AbsoluteInversionSkill implements SkillBehavior {

    @Override
    public ActivationOutcome start(ActivationContext c) {
        ServerPlayer player = c.naturalUser();
        if (player == null) return ActivationOutcome.REFUSED_INVALID;

        if (!c.pay(SpatialInversionCosts.ABSOLUTE_CP, SpatialInversionCosts.ABSOLUTE_OVERLOAD)) {
            return ActivationOutcome.REFUSED_RESOURCE;
        }

        ServerLevel level = player.serverLevel();
        Vec3 manifoldA = player.position().add(player.getLookAngle().scale(3.0)).add(0, 1.5, 0);
        Vec3 targetPoint = SpatialTargeting.point(player, SpatialInversionConfig.ABSOLUTE_MAX_RANGE);
        Vec3 manifoldB = targetPoint.add(0, 1.5, 0);

        SpatialInversionState state = SpatialInversionState.of(player);
        int visId = SpatialInversionState.nextVisualId();
        state.absoluteVisualId = visId;
        state.absoluteManifoldA = manifoldA;
        state.absoluteManifoldB = manifoldB;
        state.absoluteRemainingTicks = SpatialInversionConfig.ABSOLUTE_DURATION_TICKS;
        state.absoluteCapacity = SpatialInversionConfig.ABSOLUTE_CAPACITY;

        // Broadcast 3D geometric visual representation
        SpatialNetwork.broadcastEffect(level, visId, SyncSpatialEffectPacket.TYPE_ABSOLUTE_MANIFOLD,
                SyncSpatialEffectPacket.ACTION_ADD, manifoldA, manifoldB, 3.2f, SpatialInversionConfig.ABSOLUTE_DURATION_TICKS);

        level.playSound(null, manifoldA.x, manifoldA.y, manifoldA.z, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 2.5f, 0.5f);
        level.playSound(null, manifoldB.x, manifoldB.y, manifoldB.z, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 2.5f, 0.5f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.8f, 1.2f);

        c.setCooldown(SpatialInversionConfig.ABSOLUTE_COOLDOWN_TICKS);
        c.addProficiency(0.04f);
        return ActivationOutcome.STARTED;
    }
}
