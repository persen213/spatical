package dev.spatial.inversion;

import io.github.pinchan4273.reacademycraft.api.ActivationOutcome;
import io.github.pinchan4273.reacademycraft.api.mc.ActivationContext;
import io.github.pinchan4273.reacademycraft.api.mc.SkillBehavior;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Ability 6: Fold Step.
 * Folds space between two points and traverses the dimensional shortcut instantly.
 */
public final class FoldStepSkill implements SkillBehavior {

    @Override
    public ActivationOutcome start(ActivationContext c) {
        ServerPlayer player = c.naturalUser();
        if (player == null) return ActivationOutcome.REFUSED_INVALID;

        ServerLevel level = player.serverLevel();
        Vec3 startPos = player.position();
        Vec3 desiredTarget = SpatialTargeting.point(player, SpatialInversionConfig.FOLD_MAX_DISTANCE);

        Vec3 safeSpot = SpatialTargeting.safeSpot(level, player, desiredTarget);
        if (safeSpot == null) {
            player.displayClientMessage(Component.literal("§b[Spatial Inversion] §cCannot fold space into an obstructed or hazardous zone."), true);
            return ActivationOutcome.REFUSED_INVALID;
        }

        if (!c.pay(SpatialInversionCosts.FOLD_CP, SpatialInversionCosts.FOLD_OVERLOAD)) {
            return ActivationOutcome.REFUSED_RESOURCE;
        }

        // Execute teleport
        player.teleportTo(safeSpot.x, safeSpot.y, safeSpot.z);
        player.resetFallDistance();

        int visId = SpatialInversionState.nextVisualId();
        SpatialNetwork.broadcastEffect(level, visId, SyncSpatialEffectPacket.TYPE_FOLD_STEP,
                SyncSpatialEffectPacket.ACTION_ADD, startPos, safeSpot, 1.2f, 20);

        level.playSound(null, startPos.x, startPos.y, startPos.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.4f, 1.6f);
        level.playSound(null, safeSpot.x, safeSpot.y, safeSpot.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.4f, 1.6f);

        c.setCooldown(SpatialInversionConfig.FOLD_COOLDOWN_TICKS);
        c.addProficiency(0.015f);
        return ActivationOutcome.STARTED;
    }
}
