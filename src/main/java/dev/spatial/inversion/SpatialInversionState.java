package dev.spatial.inversion;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Server-authoritative state tracking for active spatial links, redirects, fields and manifolds.
 */
public final class SpatialInversionState {
    private static final AtomicInteger ID_COUNTER = new AtomicInteger(1000);
    private static final Map<UUID, SpatialInversionState> STATES = new ConcurrentHashMap<>();

    public static int nextVisualId() {
        return ID_COUNTER.incrementAndGet();
    }

    // Active Spatial Link (two anchors)
    public int linkVisualId = -1;
    public Vec3 linkAnchorA = null;
    public Vec3 linkAnchorB = null;
    public int linkRemainingTicks = 0;

    // Active Vector Redirect
    public int redirectVisualId = -1;
    public Vec3 redirectAperturePos = null;
    public Vec3 redirectDirection = null;
    public int redirectRemainingTicks = 0;

    // Active Distortion Field
    public int distortionVisualId = -1;
    public boolean distortionField = false;
    public int distortionRemainingTicks = 0;

    // Active Spatial Cage
    public int cageVisualId = -1;
    public Vec3 cageCenter = null;
    public int cageRemainingTicks = 0;

    // Active Absolute Inversion (Ultimate)
    public int absoluteVisualId = -1;
    public Vec3 absoluteManifoldA = null;
    public Vec3 absoluteManifoldB = null;
    public int absoluteRemainingTicks = 0;
    public int absoluteCapacity = 0;

    public static SpatialInversionState of(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), id -> new SpatialInversionState());
    }

    @Nullable
    public static SpatialInversionState peek(ServerPlayer player) {
        return STATES.get(player.getUUID());
    }

    public static void forget(UUID playerId) {
        STATES.remove(playerId);
    }

    public static void clearAll() {
        STATES.clear();
    }
}
