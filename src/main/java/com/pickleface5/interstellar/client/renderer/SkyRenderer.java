package com.pickleface5.interstellar.client.renderer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

import java.io.*;
import java.util.Objects;

@EventBusSubscriber(modid = Interstellar.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class SkyRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();

    static boolean hasGeneratedNewStarBuffer = false;
    static VertexBuffer oldStarBuffer = null;
    static VertexBuffer newStarBuffer = null;

    public static StarData[] starData;
    private static int drawnStars = 0;

    public static void generateNewStarBuffer(Tesselator pTesselator) {
        hasGeneratedNewStarBuffer = true;

        if (oldStarBuffer != null) {
            oldStarBuffer.bind();
            oldStarBuffer.upload(createEmptyBuffer(pTesselator));
            LOGGER.info("Nuked old star buffer");
        }

        newStarBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        newStarBuffer.bind();
        newStarBuffer.upload(drawNewStars(pTesselator));
        VertexBuffer.unbind();
        LOGGER.info("Generated new star buffer");
    }

    private static MeshData drawNewStars(Tesselator pTesselator) {
        RandomSource randomsource = RandomSource.create(10842L);
        BufferBuilder bufferbuilder = pTesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

        updateStarData();

        int drawnStars = 0;
        for (StarData star : starData) {
            if (Config.namedStarsOnly && star.getName().isEmpty()) continue;
            float f1 = star.getX() / 360;
            float f2 = star.getY() / 360;
            float f3 = star.getZ() / 360;
            float f4 = 0.1F; //0.15F + randomsource.nextFloat() * 0.1F;

            Vector3f vector3f = new Vector3f(f1, f2, f3).normalize(100.0F);
            float f6 = (float) (/*randomsource.nextDouble()*/0 * (float) Math.PI * 2.0);
            Quaternionf quaternionf = new Quaternionf().rotateTo(new Vector3f(0.0F, 0.0F, -1.0F), vector3f).rotateZ(f6);
            bufferbuilder.addVertex(vector3f.add(new Vector3f(f4, -f4, 0.0F).rotate(quaternionf)));
            bufferbuilder.addVertex(vector3f.add(new Vector3f(f4, f4, 0.0F).rotate(quaternionf)));
            bufferbuilder.addVertex(vector3f.add(new Vector3f(-f4, f4, 0.0F).rotate(quaternionf)));
            bufferbuilder.addVertex(vector3f.add(new Vector3f(-f4, -f4, 0.0F).rotate(quaternionf)));

            drawnStars++;
        }
        SkyRenderer.drawnStars = drawnStars;

        return bufferbuilder.buildOrThrow();
    }

    public static void updateStarData() {
        Gson gson = new GsonBuilder().create();
        try {
            InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(
                    SkyRenderer.class.getResourceAsStream("/assets/interstellar/stars.json")));
            starData = gson.fromJson(reader, StarData[].class);
        } catch (NullPointerException e) {
            starData = new StarData[]{};
        }
    }

    @SubscribeEvent
    public static void onRenderLevelAfterSky(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        if (oldStarBuffer == null) oldStarBuffer = Minecraft.getInstance().levelRenderer.starBuffer;

        // 1. On first load in to dimension, delete old stars and generate new stars.
        // 2. Render stars (same as vanilla for now)

        if (!hasGeneratedNewStarBuffer) generateNewStarBuffer(Tesselator.getInstance());

        Minecraft pMinecraft = Minecraft.getInstance();
        ClientLevel pLevel = pMinecraft.level;
        Camera pCamera = event.getCamera();
        float pPartialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        boolean pIsFoggy = pMinecraft.level.effects().isFoggyAt(Mth.floor(pCamera.getPosition().x()), Mth.floor(pCamera.getPosition().y())) || pMinecraft.gui.getBossOverlay().shouldCreateWorldFog();
        Runnable pSkyFogSetup = () -> FogRenderer.setupFog(pCamera, FogRenderer.FogMode.FOG_SKY, pMinecraft.gameRenderer.getRenderDistance(), pIsFoggy, pPartialTick);

        pSkyFogSetup.run();
        if (!pIsFoggy) {
            FogType fogtype = pCamera.getFluidInCamera();
            if (fogtype != FogType.POWDER_SNOW && fogtype != FogType.LAVA && !event.getLevelRenderer().doesMobEffectBlockSky(pCamera)) {
                PoseStack posestack = new PoseStack();
                posestack.mulPose(event.getModelViewMatrix());
                if (pMinecraft.level.effects().skyType() == DimensionSpecialEffects.SkyType.NORMAL) {
                    float f11 = 1.0F - pLevel.getRainLevel(pPartialTick);
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, f11);
                    posestack.mulPose(Axis.YP.rotationDegrees(-90.0F));
                    posestack.mulPose(Axis.XP.rotationDegrees(pLevel.getTimeOfDay(pPartialTick) * 360.0F));
                    float f10 = pLevel.getStarBrightness(pPartialTick) * f11;
                    if (f10 > 0.0F) {
                        RenderSystem.setShaderColor(f10, f10, f10, f10);
                        FogRenderer.setupNoFog();
                        newStarBuffer.bind();
                        newStarBuffer.drawWithShader(posestack.last().pose(), event.getProjectionMatrix(), GameRenderer.getPositionShader());
                        VertexBuffer.unbind();
                        pSkyFogSetup.run();
                    }
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    RenderSystem.disableBlend();
                    RenderSystem.defaultBlendFunc();
                    posestack.popPose();
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
        private final String name;
        private final float x;
        private final float y;
        private final float z;

        StarData(String name, int x, int y, int z) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
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

