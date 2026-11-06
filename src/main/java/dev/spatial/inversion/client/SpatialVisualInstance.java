package dev.spatial.inversion.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Client-side 3D geometric spatial visual instance.
 * Completely procedural zero-particle geometry in a blue-white dimensional aesthetic.
 */
public final class SpatialVisualInstance {
    public final int id;
    public final byte type;
    public Vec3 pos1;
    public Vec3 pos2;
    public float radius;
    public final int durationTicks;
    public int ageTicks = 0;
    public boolean active = true;

    // Palette: Dimensional Cyan-Blue and Bright White
    private static final float COLOR_CYAN_R = 0.22f;
    private static final float COLOR_CYAN_G = 0.75f;
    private static final float COLOR_CYAN_B = 1.00f;

    private static final float COLOR_WHITE_R = 0.95f;
    private static final float COLOR_WHITE_G = 0.98f;
    private static final float COLOR_WHITE_B = 1.00f;

    public SpatialVisualInstance(int id, byte type, Vec3 pos1, Vec3 pos2, float radius, int durationTicks) {
        this.id = id;
        this.type = type;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.radius = radius;
        this.durationTicks = durationTicks;
    }

    public void tick() {
        ageTicks++;
        if (durationTicks > 0 && ageTicks >= durationTicks) {
            active = false;
        }
    }

    public boolean isVisible(Frustum frustum) {
        double maxDim = Math.max(radius * 2.0, pos1.distanceTo(pos2) + 2.0);
        AABB box = new AABB(pos1.x - maxDim, pos1.y - maxDim, pos1.z - maxDim,
                            pos1.x + maxDim, pos1.y + maxDim, pos1.z + maxDim);
        return frustum.isVisible(box);
    }

    public void render(PoseStack poseStack, VertexConsumer builder, Camera camera, float partialTick) {
        Vec3 camPos = camera.getPosition();
        float animProgress = (ageTicks + partialTick) * 0.15f;
        float fade = 1.0f;
        if (durationTicks > 0) {
            float remain = durationTicks - (ageTicks + partialTick);
            if (remain < 10f) {
                fade = Math.max(0f, remain / 10f);
            }
        }

        switch (type) {
            case 1 -> renderAnchorPair(poseStack, builder, camPos, animProgress, fade);
            case 2 -> renderVectorRedirect(poseStack, builder, camPos, animProgress, fade);
            case 3 -> renderDistortionField(poseStack, builder, camPos, animProgress, fade);
            case 4 -> renderInversionStrike(poseStack, builder, camPos, animProgress, fade);
            case 5 -> renderFoldStep(poseStack, builder, camPos, animProgress, fade);
            case 6 -> renderSpatialCage(poseStack, builder, camPos, animProgress, fade);
            case 7 -> renderAbsoluteInversion(poseStack, builder, camPos, animProgress, fade);
        }
    }

    // 1. Spatial Link: Two connected spatial aperture rings and a continuous connecting beam
    private void renderAnchorPair(PoseStack ms, VertexConsumer b, Vec3 cam, float anim, float fade) {
        renderSpatialDisc(ms, b, pos1.subtract(cam), 1.2f, anim, fade);
        renderSpatialDisc(ms, b, pos2.subtract(cam), 1.2f, -anim, fade);
        renderDimensionalBeam(ms, b, pos1.subtract(cam), pos2.subtract(cam), 0.12f, fade * 0.7f);
    }

    // 2. Vector Redirect: Forward-facing hexagonal deflector aperture
    private void renderVectorRedirect(PoseStack ms, VertexConsumer b, Vec3 cam, float anim, float fade) {
        Vec3 relPos = pos1.subtract(cam);
        renderSpatialDisc(ms, b, relPos, radius, anim * 1.5f, fade);
        renderConcentricRings(ms, b, relPos, radius * 1.3f, 16, anim, fade);
    }

    // 3. Distortion Field: Spherical geodesic wireframe & equatorial rings
    private void renderDistortionField(PoseStack ms, VertexConsumer b, Vec3 cam, float anim, float fade) {
        Vec3 relPos = pos1.subtract(cam);
        ms.pushPose();
        ms.translate(relPos.x, relPos.y, relPos.z);
        Matrix4f mat = ms.last().pose();

        // 3 Orthogonal rotating rings
        renderGeometricRing(mat, b, radius, 32, anim, 0, 0, COLOR_CYAN_R, COLOR_CYAN_G, COLOR_CYAN_B, 0.5f * fade);
        renderGeometricRing(mat, b, radius, 32, 0, anim, 0, COLOR_WHITE_R, COLOR_WHITE_G, COLOR_WHITE_B, 0.4f * fade);
        renderGeometricRing(mat, b, radius * 0.85f, 24, 0, 0, -anim * 1.2f, COLOR_CYAN_R, COLOR_CYAN_G, COLOR_CYAN_B, 0.6f * fade);

        ms.popPose();
    }

    // 4. Inversion Strike: Dimensional fold tear and focused laser strike
    private void renderInversionStrike(PoseStack ms, VertexConsumer b, Vec3 cam, float anim, float fade) {
        Vec3 p1 = pos1.subtract(cam);
        Vec3 p2 = pos2.subtract(cam);
        renderSpatialDisc(ms, b, p1, 1.5f, anim * 2f, fade);
        renderDimensionalBeam(ms, b, p1, p2, 0.22f, fade);
        renderConcentricRings(ms, b, p2, 1.8f, 20, -anim * 2f, fade);
    }

    // 5. Fold Step: Vertical folding doorway
    private void renderFoldStep(PoseStack ms, VertexConsumer b, Vec3 cam, float anim, float fade) {
        renderVerticalDoorway(ms, b, pos1.subtract(cam), 0.9f, 2.2f, anim, fade);
        renderVerticalDoorway(ms, b, pos2.subtract(cam), 0.9f, 2.2f, -anim, fade);
    }

    // 6. Spatial Cage: 4 Planar boundary barriers
    private void renderSpatialCage(PoseStack ms, VertexConsumer b, Vec3 cam, float anim, float fade) {
        Vec3 center = pos1.subtract(cam);
        double r = radius;
        Vec3[] corners = new Vec3[] {
            center.add(-r, 0, -r),
            center.add(r, 0, -r),
            center.add(r, 0, r),
            center.add(-r, 0, r)
        };

        for (int i = 0; i < 4; i++) {
            Vec3 c1 = corners[i];
            Vec3 c2 = corners[(i + 1) % 4];
            renderSpatialWall(ms, b, c1, c2, 3.5, fade * 0.65f, anim);
        }
    }

    // 7. Absolute Inversion: Two massive spatial inversion manifolds
    private void renderAbsoluteInversion(PoseStack ms, VertexConsumer b, Vec3 cam, float anim, float fade) {
        renderLargeManifold(ms, b, pos1.subtract(cam), 3.0f, anim, fade);
        renderLargeManifold(ms, b, pos2.subtract(cam), 3.0f, -anim, fade);
        renderDimensionalBeam(ms, b, pos1.subtract(cam), pos2.subtract(cam), 0.35f, fade * 0.5f);
    }

    // --- Procedural Geometry Helpers ---

    private void renderSpatialDisc(PoseStack ms, VertexConsumer b, Vec3 at, float size, float rot, float alpha) {
        ms.pushPose();
        ms.translate(at.x, at.y, at.z);
        Matrix4f mat = ms.last().pose();

        int segments = 24;
        // Central glowing core
        for (int i = 0; i < segments; i++) {
            float a1 = (float) (i * Math.PI * 2 / segments) + rot;
            float a2 = (float) ((i + 1) * Math.PI * 2 / segments) + rot;

            float x1 = Mth.cos(a1) * size;
            float z1 = Mth.sin(a1) * size;
            float x2 = Mth.cos(a2) * size;
            float z2 = Mth.sin(a2) * size;

            // Center vertex
            b.vertex(mat, 0, 0, 0).color(COLOR_WHITE_R, COLOR_WHITE_G, COLOR_WHITE_B, 0.85f * alpha).endVertex();
            b.vertex(mat, x1, 0, z1).color(COLOR_CYAN_R, COLOR_CYAN_G, COLOR_CYAN_B, 0.35f * alpha).endVertex();
            b.vertex(mat, x2, 0, z2).color(COLOR_CYAN_R, COLOR_CYAN_G, COLOR_CYAN_B, 0.35f * alpha).endVertex();
            b.vertex(mat, 0, 0, 0).color(COLOR_WHITE_R, COLOR_WHITE_G, COLOR_WHITE_B, 0.85f * alpha).endVertex();
        }

        // Perimeter ring
        renderGeometricRing(mat, b, size, segments, 0, rot, 0, COLOR_WHITE_R, COLOR_WHITE_G, COLOR_WHITE_B, 0.9f * alpha);
        ms.popPose();
    }

    private void renderConcentricRings(PoseStack ms, VertexConsumer b, Vec3 at, float maxRad, int segs, float rot, float alpha) {
        ms.pushPose();
        ms.translate(at.x, at.y, at.z);
        Matrix4f mat = ms.last().pose();
        renderGeometricRing(mat, b, maxRad * 0.6f, segs, 0, rot, 0, COLOR_CYAN_R, COLOR_CYAN_G, COLOR_CYAN_B, 0.7f * alpha);
        renderGeometricRing(mat, b, maxRad, segs, 0, -rot * 0.8f, 0, COLOR_WHITE_R, COLOR_WHITE_G, COLOR_WHITE_B, 0.9f * alpha);
        ms.popPose();
    }

    private void renderGeometricRing(Matrix4f mat, VertexConsumer b, float radius, int segs, float rotX, float rotY, float rotZ, float r, float g, float bl, float a) {
        float width = 0.04f;
        for (int i = 0; i < segs; i++) {
            float a1 = (float) (i * Math.PI * 2 / segs) + rotY;
            float a2 = (float) ((i + 1) * Math.PI * 2 / segs) + rotY;

            float x1 = Mth.cos(a1);
            float z1 = Mth.sin(a1);
            float x2 = Mth.cos(a2);
            float z2 = Mth.sin(a2);

            b.vertex(mat, x1 * (radius - width), 0, z1 * (radius - width)).color(r, g, bl, a).endVertex();
            b.vertex(mat, x1 * (radius + width), 0, z1 * (radius + width)).color(r, g, bl, a).endVertex();
            b.vertex(mat, x2 * (radius + width), 0, z2 * (radius + width)).color(r, g, bl, a).endVertex();
            b.vertex(mat, x2 * (radius - width), 0, z2 * (radius - width)).color(r, g, bl, a).endVertex();
        }
    }

    private void renderDimensionalBeam(PoseStack ms, VertexConsumer b, Vec3 start, Vec3 end, float width, float alpha) {
        Vec3 dir = end.subtract(start);
        float len = (float) dir.length();
        if (len < 0.01f) return;

        ms.pushPose();
        ms.translate(start.x, start.y, start.z);

        // Compute cross product for billboarded beam quad
        Vec3 norm = dir.normalize();
        Vec3 up = Math.abs(norm.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 side = norm.cross(up).normalize().scale(width);

        Matrix4f mat = ms.last().pose();

        b.vertex(mat, (float) -side.x, (float) -side.y, (float) -side.z).color(COLOR_WHITE_R, COLOR_WHITE_G, COLOR_WHITE_B, alpha).endVertex();
        b.vertex(mat, (float) side.x, (float) side.y, (float) side.z).color(COLOR_WHITE_R, COLOR_WHITE_G, COLOR_WHITE_B, alpha).endVertex();
        b.vertex(mat, (float) (dir.x + side.x), (float) (dir.y + side.y), (float) (dir.z + side.z)).color(COLOR_CYAN_R, COLOR_CYAN_G, COLOR_CYAN_B, alpha * 0.6f).endVertex();
        b.vertex(mat, (float) (dir.x - side.x), (float) (dir.y - side.y), (float) (dir.z - side.z)).color(COLOR_CYAN_R, COLOR_CYAN_G, COLOR_CYAN_B, alpha * 0.6f).endVertex();

        ms.popPose();
    }

    private void renderVerticalDoorway(PoseStack ms, VertexConsumer b, Vec3 at, float w, float h, float rot, float alpha) {
        ms.pushPose();
        ms.translate(at.x, at.y, at.z);
        Matrix4f mat = ms.last().pose();

        float hw = w * 0.5f;
        // Translucent blue-white quad
        b.vertex(mat, -hw, 0, 0).color(COLOR_CYAN_R, COLOR_CYAN_G, COLOR_CYAN_B, 0.4f * alpha).endVertex();
        b.vertex(mat, hw, 0, 0).color(COLOR_CYAN_R, COLOR_CYAN_G, COLOR_CYAN_B, 0.4f * alpha).endVertex();
        b.vertex(mat, hw, h, 0).color(COLOR_WHITE_R, COLOR_WHITE_G, COLOR_WHITE_B, 0.7f * alpha).endVertex();
        b.vertex(mat, -hw, h, 0).color(COLOR_WHITE_R, COLOR_WHITE_G, COLOR_WHITE_B, 0.7f * alpha).endVertex();

        // Reverse side
        b.vertex(mat, -hw, h, 0).color(COLOR_WHITE_R, COLOR_WHITE_G, COLOR_WHITE_B, 0.7f * alpha).endVertex();
        b.vertex(mat, hw, h, 0).color(COLOR_WHITE_R, COLOR_WHITE_G, COLOR_WHITE_B, 0.7f * alpha).endVertex();
        b.vertex(mat, hw, 0, 0).color(COLOR_CYAN_R, COLOR_CYAN_G, COLOR_CYAN_B, 0.4f * alpha).endVertex();
        b.vertex(mat, -hw, 0, 0).color(COLOR_CYAN_R, COLOR_CYAN_G, COLOR_CYAN_B, 0.4f * alpha).endVertex();

        ms.popPose();
    }

    private void renderSpatialWall(PoseStack ms, VertexConsumer b, Vec3 c1, Vec3 c2, double height, float alpha, float anim) {
        Matrix4f mat = ms.last().pose();
        float h = (float) height;

        b.vertex(mat, (float) c1.x, (float) c1.y, (float) c1.z).color(COLOR_CYAN_R, COLOR_CYAN_G, COLOR_CYAN_B, 0.25f * alpha).endVertex();
        b.vertex(mat, (float) c2.x, (float) c2.y, (float) c2.z).color(COLOR_CYAN_R, COLOR_CYAN_G, COLOR_CYAN_B, 0.25f * alpha).endVertex();
        b.vertex(mat, (float) c2.x, (float) (c2.y + h), (float) c2.z).color(COLOR_WHITE_R, COLOR_WHITE_G, COLOR_WHITE_B, 0.6f * alpha).endVertex();
        b.vertex(mat, (float) c1.x, (float) (c1.y + h), (float) c1.z).color(COLOR_WHITE_R, COLOR_WHITE_G, COLOR_WHITE_B, 0.6f * alpha).endVertex();

        // Double-sided
        b.vertex(mat, (float) c1.x, (float) (c1.y + h), (float) c1.z).color(COLOR_WHITE_R, COLOR_WHITE_G, COLOR_WHITE_B, 0.6f * alpha).endVertex();
        b.vertex(mat, (float) c2.x, (float) (c2.y + h), (float) c2.z).color(COLOR_WHITE_R, COLOR_WHITE_G, COLOR_WHITE_B, 0.6f * alpha).endVertex();
        b.vertex(mat, (float) c2.x, (float) c2.y, (float) c2.z).color(COLOR_CYAN_R, COLOR_CYAN_G, COLOR_CYAN_B, 0.25f * alpha).endVertex();
        b.vertex(mat, (float) c1.x, (float) c1.y, (float) c1.z).color(COLOR_CYAN_R, COLOR_CYAN_G, COLOR_CYAN_B, 0.25f * alpha).endVertex();
    }

    private void renderLargeManifold(PoseStack ms, VertexConsumer b, Vec3 at, float size, float rot, float alpha) {
        ms.pushPose();
        ms.translate(at.x, at.y, at.z);
        Matrix4f mat = ms.last().pose();

        int segs = 32;
        // Dense glowing core
        renderGeometricRing(mat, b, size * 0.4f, segs, 0, rot * 1.5f, 0, COLOR_WHITE_R, COLOR_WHITE_G, COLOR_WHITE_B, 0.95f * alpha);
        renderGeometricRing(mat, b, size * 0.75f, segs, 0, -rot, 0, COLOR_CYAN_R, COLOR_CYAN_G, COLOR_CYAN_B, 0.8f * alpha);
        renderGeometricRing(mat, b, size, segs, 0, rot * 0.7f, 0, COLOR_WHITE_R, COLOR_WHITE_G, COLOR_WHITE_B, 0.9f * alpha);

        // Quad lattice
        float hs = size * 0.7f;
        b.vertex(mat, -hs, 0, -hs).color(COLOR_CYAN_R, COLOR_CYAN_G, COLOR_CYAN_B, 0.45f * alpha).endVertex();
        b.vertex(mat, hs, 0, -hs).color(COLOR_CYAN_R, COLOR_CYAN_G, COLOR_CYAN_B, 0.45f * alpha).endVertex();
        b.vertex(mat, hs, 0, hs).color(COLOR_WHITE_R, COLOR_WHITE_G, COLOR_WHITE_B, 0.7f * alpha).endVertex();
        b.vertex(mat, -hs, 0, hs).color(COLOR_WHITE_R, COLOR_WHITE_G, COLOR_WHITE_B, 0.7f * alpha).endVertex();

        ms.popPose();
    }
}
