package com.pickleface5.interstellar.client.renderer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.pickleface5.interstellar.Config;
import com.pickleface5.interstellar.Interstellar;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.*;
import java.util.Objects;

public class SkyboxRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();

    boolean hasCreatedStars = false;
    @Nullable
    VertexBuffer vanillaBuffer;
    @Nullable
    VertexBuffer starBuffer;

    private static StarData[] starData;
    private static int drawnStars = 0;

    public void createStars(Tesselator pTesselator) {
        hasCreatedStars = true;

        if (vanillaBuffer != null) {
            vanillaBuffer.bind();
            vanillaBuffer.upload(createEmptyBuffer(pTesselator));
            LOGGER.info("Nuked old star buffer");
        }

        updateStarData();

        starBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        starBuffer.bind();
        starBuffer.upload(drawNewStars(pTesselator));
        VertexBuffer.unbind();
        LOGGER.info("Generated new star buffer");
    }

    private MeshData drawNewStars(Tesselator pTesselator) {
        RandomSource randomsource = RandomSource.create(10842L);
        BufferBuilder bufferbuilder = pTesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

        int drawnStars = 0;
        for (StarData star : starData) {
            if (Config.namedStarsOnly && star.getName().isEmpty()) continue;
            float f1 = star.getX();
            float f2 = star.getY();
            float f3 = star.getZ();
            float f4 = 0.2F + randomsource.nextFloat() * 0.1F;

            // Highlight constellations
            Vector3f vector3f = new Vector3f(f1, f2, f3).normalize(100.0F);
            float f6 = (float) (randomsource.nextDouble() * (float) Math.PI * 2.0);
            Quaternionf quaternionf = new Quaternionf().rotateTo(new Vector3f(0.0F, 0.0F, -1.0F), vector3f).rotateZ(f6);
            bufferbuilder.addVertex(vector3f.add(new Vector3f(f4, -f4, 0.0F).rotate(quaternionf)));
            bufferbuilder.addVertex(vector3f.add(new Vector3f(f4, f4, 0.0F).rotate(quaternionf)));
            bufferbuilder.addVertex(vector3f.add(new Vector3f(-f4, f4, 0.0F).rotate(quaternionf)));
            bufferbuilder.addVertex(vector3f.add(new Vector3f(-f4, -f4, 0.0F).rotate(quaternionf)));

            drawnStars++;
        }
        SkyboxRenderer.drawnStars = drawnStars;

        return bufferbuilder.buildOrThrow();
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

        // 1. On first load in to dimension, delete old stars and generate new stars.
        // 2. Render stars (same as vanilla for now)

        if (!hasCreatedStars) {
            createStars(Tesselator.getInstance());
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Camera camera = event.getCamera();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        boolean isFoggy = minecraft.level.effects().isFoggyAt(Mth.floor(camera.getPosition().x()), Mth.floor(camera.getPosition().y())) || minecraft.gui.getBossOverlay().shouldCreateWorldFog();
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
                    posestack.pushPose();
                    float f11 = 1.0F - level.getRainLevel(partialTick);
                    posestack.mulPose(Axis.YP.rotationDegrees(-90.0F));
                    posestack.mulPose(Axis.XP.rotationDegrees(level.getTimeOfDay(partialTick) * 360.0F));
                    float f10 = level.getStarBrightness(partialTick) * f11;
                    if (f10 > 0.0F) {
                        RenderSystem.setShaderColor(f10, f10, f10, f10);
                        FogRenderer.setupNoFog();
                        starBuffer.bind();
                        starBuffer.drawWithShader(posestack.last().pose(), event.getProjectionMatrix(), GameRenderer.getPositionShader());
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

    public static int getStarAmount() {
        return drawnStars;
    }

    public static class StarData {
        private final int id;
        private final String name;
        private final float x;
        private final float y;
        private final float z;

        StarData(int id, String name, int x, int y, int z) {
            this.id = id;
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        int getId() {
            return id;
        }

        String getName() {
            return name;
        }

        float getX() {
            return x;
        }

        float getY() {
            return y;
        }

        float getZ() {
            return z;
        }
    }
}

