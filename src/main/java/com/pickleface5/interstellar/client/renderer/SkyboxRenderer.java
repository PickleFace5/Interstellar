package com.pickleface5.interstellar.client.renderer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.pickleface5.interstellar.Config;
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
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SkyboxRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();

    boolean hasCreatedStars = false;
    @Nullable
    VertexBuffer vanillaBuffer;
    VertexBuffer starBuffer;
    VertexBuffer highlightBuffer;

    private static StarData[] starData;
    private int drawnStars = 0;

    public SkyboxRenderer() {
        updateStarData();
    }

    public void reload(Tesselator tesselator) {
        updateStarData();
        createStars(tesselator);
    }

    public StarData[] getStarData() {
        return starData;
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
        int drawnStars = 0;

        List<StarData> highlightStars = new ArrayList<>();
        starBuffer.bind();
        BufferBuilder bufferbuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        for (StarData star : starData) {
            if (Config.highlightedStarNames.contains(star.getName()) || Config.highlightedStarsIds.contains(star.getId())) {
                highlightStars.add(star);
                continue;
            }
            if (Config.namedStarsOnly && star.getName().isEmpty()) continue;
            drawStar(bufferbuilder, star.getX(), star.getY(), star.getZ(), random.nextFloat(), 0.2F + random.nextFloat() * 0.1F);
            drawnStars++;
        }
        starBuffer.upload(bufferbuilder.buildOrThrow());
        VertexBuffer.unbind();

        highlightBuffer.bind();
        bufferbuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        if (!highlightStars.isEmpty()) {
            for (StarData star : highlightStars) {
                drawStar(bufferbuilder, star.getX(), star.getY(), star.getZ(), random.nextFloat(), 0.25F + random.nextFloat() * 0.1F);
                drawnStars++;
            }
        } else {
            bufferbuilder.addVertex(0, 0, 0);
        }
        highlightBuffer.upload(bufferbuilder.buildOrThrow());
        VertexBuffer.unbind();

        this.drawnStars = drawnStars;
    }

    private void drawStar(BufferBuilder bufferBuilder, float x, float y, float z, float rads, float size) {
        Vector3f vector = new Vector3f(x, y, z).normalize(100.0F);
        Quaternionf quaternion = new Quaternionf().rotateTo(new Vector3f(0.0F, 0.0F, -1.0F), vector).rotateZ(rads * (float) Math.PI * 2);
        bufferBuilder.addVertex(vector.add(new Vector3f(size, -size, 0.0F).rotate(quaternion)));
        bufferBuilder.addVertex(vector.add(new Vector3f(size, size, 0.0F).rotate(quaternion)));
        bufferBuilder.addVertex(vector.add(new Vector3f(-size, size, 0.0F).rotate(quaternion)));
        bufferBuilder.addVertex(vector.add(new Vector3f(-size, -size, 0.0F).rotate(quaternion)));
    }

    private static void updateStarData() {
        Gson gson = new GsonBuilder().create();
        try {
            InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(
                    SkyboxRenderer.class.getResourceAsStream("/assets/interstellar/stars.json")));
            starData = gson.fromJson(reader, StarData[].class);
        } catch (NullPointerException e) {
            starData = new StarData[]{};
            LOGGER.error("stars.json not found, destroying the fabric of space");
        }
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
        Camera camera = event.getCamera();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        boolean isFoggy = level.effects().isFoggyAt(Mth.floor(camera.getPosition().x()), Mth.floor(camera.getPosition().y())) || minecraft.gui.getBossOverlay().shouldCreateWorldFog();
        Runnable skyFogSetup = () -> FogRenderer.setupFog(camera, FogRenderer.FogMode.FOG_SKY, minecraft.gameRenderer.getRenderDistance(), isFoggy, partialTick);

        skyFogSetup.run();
        if (!isFoggy) {
            FogType fogtype = camera.getFluidInCamera();
            if (fogtype != FogType.POWDER_SNOW && fogtype != FogType.LAVA && !event.getLevelRenderer().doesMobEffectBlockSky(camera)) {
                PoseStack posestack = event.getPoseStack();
                posestack.mulPose(event.getModelViewMatrix());
                RenderSystem.enableBlend();
                if (minecraft.level.effects().skyType() == DimensionSpecialEffects.SkyType.NORMAL) {
                    FogRenderer.levelFogColor();
                    RenderSystem.depthMask(false);
                    RenderSystem.blendFuncSeparate(
                            GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
                    );
                    //posestack.mulPose(Axis.XP.rotationDegrees(90.0F));
                    //posestack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
                    //posestack.mulPose(Axis.YP.rotationDegrees(-90.0F));
                    posestack.pushPose();
                    float f11 = 1.0F - level.getRainLevel(partialTick);
                    posestack.mulPose(Axis.XP.rotationDegrees(23.5F));
                    posestack.mulPose(Axis.YP.rotationDegrees(180.0F));
                    posestack.mulPose(Axis.ZP.rotationDegrees(level.getTimeOfDay(partialTick) * 360.0F));
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

    private static MeshData createEmptyBuffer(Tesselator pTesselator) {
        BufferBuilder bufferBuilder = pTesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        bufferBuilder.addVertex(0, 0, 0);
        return bufferBuilder.build();
    }

    public int getStarAmount() {
        return drawnStars;
    }

    public static class StarData {
        private final int id;
        private final String name;
        private final float x;
        private final float y;
        private final float z;

        private StarData(int id, String name, int x, int y, int z) {
            this.id = id;
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }

        public float getZ() {
            return z;
        }
    }
}

