package dev.spatial.inversion;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Handles multiplayer packet synchronization for spatial phenomena.
 */
public final class SpatialNetwork {
    private SpatialNetwork() {}

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SpatialInversionMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void init() {
        CHANNEL.messageBuilder(SyncSpatialEffectPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncSpatialEffectPacket::encode)
                .decoder(SyncSpatialEffectPacket::decode)
                .consumerMainThread(SyncSpatialEffectPacket::handle)
                .add();
    }

    public static void broadcastEffect(ServerLevel level, int effectId, byte type, byte action, Vec3 p1, Vec3 p2, float radius, int duration) {
        SyncSpatialEffectPacket pkt = new SyncSpatialEffectPacket(effectId, type, action, p1, p2, radius, duration);
        CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension), pkt);
    }
}
