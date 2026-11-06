package dev.spatial.inversion;

import dev.spatial.inversion.client.ClientSpatialRenderer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Network packet synchronizing 3D spatial effect geometry from server to clients.
 */
public final class SyncSpatialEffectPacket {
    public static final byte ACTION_ADD = 0;
    public static final byte ACTION_REMOVE = 1;
    public static final byte ACTION_CLEAR = 2;

    public static final byte TYPE_ANCHOR_PAIR = 1;
    public static final byte TYPE_VECTOR_REDIRECT = 2;
    public static final byte TYPE_DISTORTION_FIELD = 3;
    public static final byte TYPE_INVERSION_STRIKE = 4;
    public static final byte TYPE_FOLD_STEP = 5;
    public static final byte TYPE_SPATIAL_CAGE = 6;
    public static final byte TYPE_ABSOLUTE_MANIFOLD = 7;

    private final int effectId;
    private final byte effectType;
    private final byte action;
    private final Vec3 pos1;
    private final Vec3 pos2;
    private final float radius;
    private final int durationTicks;

    public SyncSpatialEffectPacket(int effectId, byte effectType, byte action, Vec3 pos1, Vec3 pos2, float radius, int durationTicks) {
        this.effectId = effectId;
        this.effectType = effectType;
        this.action = action;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.radius = radius;
        this.durationTicks = durationTicks;
    }

    public static void encode(SyncSpatialEffectPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.effectId);
        buf.writeByte(msg.effectType);
        buf.writeByte(msg.action);
        buf.writeDouble(msg.pos1.x);
        buf.writeDouble(msg.pos1.y);
        buf.writeDouble(msg.pos1.z);
        buf.writeDouble(msg.pos2.x);
        buf.writeDouble(msg.pos2.y);
        buf.writeDouble(msg.pos2.z);
        buf.writeFloat(msg.radius);
        buf.writeInt(msg.durationTicks);
    }

    public static SyncSpatialEffectPacket decode(FriendlyByteBuf buf) {
        int id = buf.readInt();
        byte type = buf.readByte();
        byte act = buf.readByte();
        Vec3 p1 = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Vec3 p2 = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        float rad = buf.readFloat();
        int dur = buf.readInt();
        return new SyncSpatialEffectPacket(id, type, act, p1, p2, rad, dur);
    }

    public static void handle(SyncSpatialEffectPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ClientSpatialRenderer.handlePacket(msg.effectId, msg.effectType, msg.action, msg.pos1, msg.pos2, msg.radius, msg.durationTicks);
        }));
        ctx.setPacketHandled(true);
    }
}
