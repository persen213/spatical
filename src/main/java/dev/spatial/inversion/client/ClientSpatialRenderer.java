package dev.spatial.inversion.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client rendering manager for all 3D geometric spatial visuals.
 * Enforces zero-particle rendering using modern Forge level stage rendering.
 */
@Mod.EventBusSubscriber(modid = "spatialinversion", value = Dist.CLIENT)
public final class ClientSpatialRenderer {
    private ClientSpatialRenderer() {}

    private static final Map<Integer, SpatialVisualInstance> ACTIVE_VISUALS = new ConcurrentHashMap<>();

    public static void handlePacket(int id, byte type, byte action, Vec3 p1, Vec3 p2, float radius, int duration) {
        if (action == 0) { // ADD
            ACTIVE_VISUALS.put(id, new SpatialVisualInstance(id, type, p1, p2, radius, duration));
        } else if (action == 1) { // REMOVE
            ACTIVE_VISUALS.remove(id);
        } else if (action == 2) { // CLEAR
            ACTIVE_VISUALS.clear();
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || Minecraft.getInstance().isPaused()) return;
        ACTIVE_VISUALS.values().removeIf(v -> {
            v.tick();
            return !v.active;
        });
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (ACTIVE_VISUALS.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Camera camera = event.getCamera();
        PoseStack poseStack = event.getPoseStack();
        Frustum frustum = event.getFrustum();
        float partialTick = event.getPartialTick();

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        // RenderType.lightning() uses additive blending without depth-mask conflicts, perfect for glowing blue-white energy
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());

        for (SpatialVisualInstance visual : ACTIVE_VISUALS.values()) {
            if (visual.active && visual.isVisible(frustum)) {
                visual.render(poseStack, consumer, camera, partialTick);
            }
        }
        bufferSource.endBatch(RenderType.lightning());
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ACTIVE_VISUALS.clear();
    }
}
