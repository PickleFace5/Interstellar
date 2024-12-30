package com.pickleface5.interstellar.star;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import com.pickleface5.interstellar.renderer.SkyboxRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.slf4j.Logger;

import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Objects;

public class StarHandler {
    private static Star[] starData;
    public static final Logger LOGGER = LogUtils.getLogger();

    static {
        updateStarData();
    }

    public static Star[] getStarsFromJson() {
        return starData;
    }

    public static boolean refreshStarData() {
        return !Arrays.equals(updateStarData(), starData);
    }

    private static Star[] updateStarData() {
        Gson gson = new GsonBuilder().create();
        try {
            InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(
                    SkyboxRenderer.class.getResourceAsStream("/assets/interstellar/stars.json")));
            starData = gson.fromJson(reader, Star[].class);
        } catch (NullPointerException e) {
            starData = new Star[]{};
            LOGGER.warn("stars.json not found, destroying the fabric of space");
        }
        return starData;
    }

    public static MutableComponent getTranslatableComponent(Star star) {
        if (star.getName().isEmpty()) return Component.translatable("star_info.no_name", star.getId(), star.getX(), star.getY(), star.getZ());
        else return Component.translatable("star_info.with_name", star.getId(), star.getName(), star.getX(), star.getY(), star.getZ());
    }
}
