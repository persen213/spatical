package dev.spatial.inversion;

import io.github.pinchan4273.reacademycraft.api.ActivationOutcome;
import io.github.pinchan4273.reacademycraft.api.mc.AcademySafety;
import io.github.pinchan4273.reacademycraft.api.mc.ActivationContext;
import io.github.pinchan4273.reacademycraft.api.mc.SkillBehavior;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Ability 3: Spatial Exchange.
 * Swaps positions between the player and a target entity within range.
 */
public final class SpatialExchangeSkill implements SkillBehavior {

    @Override
    public ActivationOutcome start(ActivationContext c) {
        ServerPlayer player = c.naturalUser();
        if (player == null) return ActivationOutcome.REFUSED_INVALID;

        ServerLevel level = player.serverLevel();
        LivingEntity target = SpatialTargeting.lookEntity(player, SpatialInversionConfig.EXCHANGE_RANGE);
        if (target == null) {
            player.displayClientMessage(Component.literal("§b[Spatial Inversion] §cNo valid target in sight."), true);
            return ActivationOutcome.REFUSED_INVALID;
        }

        // Safety & PvP attack authorization
        if (!AcademySafety.mayAttack(c.actor(), target, SpatialInversionMod.SPATIAL_EXCHANGE)) {
            player.displayClientMessage(Component.literal("§b[Spatial Inversion] §cTarget protected by safety rules."), true);
            return ActivationOutcome.REFUSED_INVALID;
        }

        // Boss resistance check
        if (SpatialTargeting.isBoss(target)) {
            player.displayClientMessage(Component.literal("§b[Spatial Inversion] §eTarget resists spatial displacement!"), true);
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.2f, 1.5f);
            c.setCooldown(60);
            return ActivationOutcome.REFUSED_INVALID;
        }

        Vec3 playerOriginal = player.position();
        Vec3 targetOriginal = target.position();

        // Safe landing spots validation
        Vec3 safeForPlayer = SpatialTargeting.safeSpot(level, player, targetOriginal);
        Vec3 safeForTarget = SpatialTargeting.safeSpot(level, target, playerOriginal);

        if (safeForPlayer == null || safeForTarget == null) {
            player.displayClientMessage(Component.literal("§b[Spatial Inversion] §cDestination space is unsafe."), true);
            return ActivationOutcome.REFUSED_INVALID;
        }

        if (!c.pay(SpatialInversionCosts.EXCHANGE_CP, SpatialInversionCosts.EXCHANGE_OVERLOAD)) {
            return ActivationOutcome.REFUSED_RESOURCE;
        }

        // Execute atomic spatial exchange
        player.teleportTo(safeForPlayer.x, safeForPlayer.y, safeForPlayer.z);
        target.teleportTo(safeForTarget.x, safeForTarget.y, safeForTarget.z);

        // Flash 3D visual exchange anchors at both positions
        int visId = SpatialInversionState.nextVisualId();
        SpatialNetwork.broadcastEffect(level, visId, SyncSpatialEffectPacket.TYPE_ANCHOR_PAIR,
                SyncSpatialEffectPacket.ACTION_ADD, playerOriginal, targetOriginal, 1.5f, 25);

        level.playSound(null, playerOriginal.x, playerOriginal.y, playerOriginal.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.5f, 1.2f);
        level.playSound(null, targetOriginal.x, targetOriginal.y, targetOriginal.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.5f, 1.2f);

        c.setCooldown(SpatialInversionConfig.EXCHANGE_COOLDOWN_TICKS);
        c.addProficiency(0.02f);
        return ActivationOutcome.STARTED;
    }
}
