package com.pickleface5.interstellar.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.pickleface5.interstellar.Config;
import com.pickleface5.interstellar.star.Star;
import com.pickleface5.interstellar.star.StarHandler;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.FogType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class SkyboxRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();

    private boolean hasCreatedStars = false;
    @Nullable
    private VertexBuffer vanillaBuffer;
    private VertexBuffer starBuffer;
    private VertexBuffer highlightBuffer;

    private Star[] drawnStars;

    private float partialTick;
    private Camera camera;


    public boolean reload(Tesselator tesselator) {
        boolean success = StarHandler.refreshStarData();
        if (success) createStars(tesselator);
        return success;
    }

    private void createStars(Tesselator tesselator) {
        hasCreatedStars = true;

        if (vanillaBuffer != null) {
            vanillaBuffer.bind();
            vanillaBuffer.upload(createEmptyBuffer(tesselator));
        }

        starBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        highlightBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        uploadNewStars(tesselator, starBuffer, highlightBuffer);
    }

    private void uploadNewStars(Tesselator tesselator, VertexBuffer starBuffer, VertexBuffer highlightBuffer) {
        RandomSource random = RandomSource.create(10842L);

        List<Star> drawnStars = new ArrayList<>();
        List<Star> highlightStars = new ArrayList<>();
        starBuffer.bind();
        BufferBuilder bufferbuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        for (Star star : StarHandler.getStarsFromJson()) {
            if (Config.highlightedStarNames.contains(star.getName()) || Config.highlightedStarsIds.contains(star.getId())) {
                highlightStars.add(star);
                continue;
            }
            if (Config.namedStarsOnly && star.getName().isEmpty()) continue;
            drawStar(bufferbuilder, star.getX(), star.getY(), star.getZ(), random.nextFloat(), 0.2F + random.nextFloat() * 0.1F);
            drawnStars.add(star);
        }
        starBuffer.upload(bufferbuilder.buildOrThrow());
        VertexBuffer.unbind();

        highlightBuffer.bind();
        bufferbuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        if (!highlightStars.isEmpty()) {
            for (Star star : highlightStars) {
                drawStar(bufferbuilder, star.getX(), star.getY(), star.getZ(), random.nextFloat(), 0.25F + random.nextFloat() * 0.1F);
                drawnStars.add(star);
            }
        } else {
            bufferbuilder.addVertex(0, 0, 0);
        }
        highlightBuffer.upload(bufferbuilder.buildOrThrow());
        VertexBuffer.unbind();

        this.drawnStars = drawnStars.toArray(new Star[0]);
    }

    private void drawStar(BufferBuilder bufferBuilder, float x, float y, float z, float rads, float size) {
        Vector3f vector = new Vector3f(x, y, z).normalize(100.0F);
        Quaternionf quaternion = new Quaternionf().rotateTo(new Vector3f(0.0F, 0.0F, -1.0F), vector).rotateZ(rads * (float) Math.PI * 2);
        bufferBuilder.addVertex(vector.add(new Vector3f(size, -size, 0.0F).rotate(quaternion)));
        bufferBuilder.addVertex(vector.add(new Vector3f(size, size, 0.0F).rotate(quaternion)));
        bufferBuilder.addVertex(vector.add(new Vector3f(-size, size, 0.0F).rotate(quaternion)));
        bufferBuilder.addVertex(vector.add(new Vector3f(-size, -size, 0.0F).rotate(quaternion)));
    }

    @SubscribeEvent
    public void onRenderLevelAfterSky(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        if (vanillaBuffer == null) vanillaBuffer = Minecraft.getInstance().levelRenderer.starBuffer;

        if (!hasCreatedStars) {
            createStars(Tesselator.getInstance());
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        assert level != null;
        camera = event.getCamera();
        partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        boolean isFoggy = level.effects().isFoggyAt(Mth.floor(camera.getPosition().x()), Mth.floor(camera.getPosition().y())) || minecraft.gui.getBossOverlay().shouldCreateWorldFog();
        Runnable skyFogSetup = () -> FogRenderer.setupFog(camera, FogRenderer.FogMode.FOG_SKY, minecraft.gameRenderer.getRenderDistance(), isFoggy, partialTick);

        skyFogSetup.run();
        if (!isFoggy) {
            FogType fogtype = camera.getFluidInCamera();
            if (fogtype != FogType.POWDER_SNOW && fogtype != FogType.LAVA && !event.getLevelRenderer().doesMobEffectBlockSky(camera)) {
                PoseStack posestack = event.getPoseStack();
                posestack.mulPose(event.getModelViewMatrix());
                RenderSystem.enableBlend();
                if (level.effects().skyType() == DimensionSpecialEffects.SkyType.NORMAL) {
                    FogRenderer.levelFogColor();
                    RenderSystem.depthMask(false);
                    RenderSystem.blendFuncSeparate(
                            GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
                    );
                    posestack.pushPose();
                    float f11 = 1.0F - level.getRainLevel(partialTick);
                    posestack.mulPose(Axis.ZP.rotationDegrees(level.getTimeOfDay(partialTick) * 360.0F));
                    posestack.mulPose(Axis.YP.rotationDegrees(180.0F));
                    float f10 = level.getStarBrightness(partialTick) * f11;
                    float f12 = Math.min(f10 * 1.8F, 1);
                    if (f10 > 0.0F) {
                        RenderSystem.setShaderColor(f10, f10, f10, f10);
                        FogRenderer.setupNoFog();
                        starBuffer.bind();
                        assert GameRenderer.getPositionShader() != null;
                        starBuffer.drawWithShader(posestack.last().pose(), event.getProjectionMatrix(), GameRenderer.getPositionShader());
                        VertexBuffer.unbind();
                        RenderSystem.setShaderColor(f12, f12, f12, f12);
                        highlightBuffer.bind();
                        highlightBuffer.drawWithShader(posestack.last().pose(), event.getProjectionMatrix(), GameRenderer.getPositionShader());
                        VertexBuffer.unbind();
                        skyFogSetup.run();
                    }
                    posestack.popPose();
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    RenderSystem.disableBlend();
                    RenderSystem.defaultBlendFunc();
                    RenderSystem.depthMask(true);
                }
            }
        }
    }

    public Star getPlayerTargetStar() {
        return this.getStarFromAngle(camera);
    }

    private Star getStarFromAngle(Camera camera) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return null;
        Vector3f targetVector = camera.rotation().transform(new Vector3f(0.0F, 0.0F, -1.0F))
                .rotate(Axis.XP.rotationDegrees(level.getTimeOfDay(partialTick) * 360.0F));

        Star closestStar = null;
        float smallestAngle = Float.MAX_VALUE;
        for (Star star : this.getDrawnStars()) {
            Vector3f starVector = new Vector3f(star.getX(), star.getY(), star.getZ()).normalize(100.0F);
            if (targetVector.dot(starVector) <= 0) continue;
            float angle = targetVector.angle(starVector);

            if (angle < smallestAngle) {
                smallestAngle = angle;
                closestStar = star;
            }
        }
        return closestStar;
    }

    private static MeshData createEmptyBuffer(Tesselator pTesselator) {
        BufferBuilder bufferBuilder = pTesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        bufferBuilder.addVertex(0, 0, 0);
        return bufferBuilder.build();
    }

    public int getDrawnStarAmount() {
        return drawnStars.length;
    }

    public Star[] getDrawnStars() {
        return drawnStars;
    }
}
