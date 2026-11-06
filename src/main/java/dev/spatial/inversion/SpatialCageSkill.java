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
 * Ability 7: Spatial Cage.
 * Surrounds an 8-block radius zone with four planar spatial barriers that redirect escaping projectiles and contain foes.
 */
public final class SpatialCageSkill implements SkillBehavior {

    @Override
    public ActivationOutcome start(ActivationContext c) {
        ServerPlayer player = c.naturalUser();
        if (player == null) return ActivationOutcome.REFUSED_INVALID;

        if (!c.pay(SpatialInversionCosts.CAGE_CP, SpatialInversionCosts.CAGE_OVERLOAD)) {
            return ActivationOutcome.REFUSED_RESOURCE;
        }

        ServerLevel level = player.serverLevel();
        Vec3 center = player.position();

        SpatialInversionState state = SpatialInversionState.of(player);
        int visId = SpatialInversionState.nextVisualId();
        state.cageVisualId = visId;
        state.cageCenter = center;
        state.cageRemainingTicks = SpatialInversionConfig.CAGE_DURATION_TICKS;

        SpatialNetwork.broadcastEffect(level, visId, SyncSpatialEffectPacket.TYPE_SPATIAL_CAGE,
                SyncSpatialEffectPacket.ACTION_ADD, center, Vec3.ZERO, (float) SpatialInversionConfig.CAGE_RADIUS,
                SpatialInversionConfig.CAGE_DURATION_TICKS);

        level.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0f, 0.8f);

        c.setCooldown(SpatialInversionConfig.CAGE_COOLDOWN_TICKS);
        c.addProficiency(0.025f);
        return ActivationOutcome.STARTED;
    }
}
