package com.pickleface5.interstellar;

import com.mojang.logging.LogUtils;
import com.pickleface5.interstellar.client.star.Star;
import com.pickleface5.interstellar.client.star.StarHandler;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

@EventBusSubscriber(modid = Interstellar.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final Logger LOGGER = LogUtils.getLogger();

    public static boolean namedStarsOnly;
    public static Set<String> highlightedStarNames;
    public static Set<Integer> highlightedStarsIds;

    private static final ModConfigSpec.BooleanValue NAMED_STARS_ONLY = BUILDER
            .comment("Whether to only render stars with registered names. It is recommended to keep this enabled, as there is ~120000 stars total, ~500 which are named.")
            .comment("Unnamed highlighted stars bypass this.")
            .define("namedStarsOnly", true);

    private static final ModConfigSpec.ConfigValue<List<? extends String>> HIGHLIGHTED_STAR_NAMES = BUILDER
            .comment("Stars here will appear brighter in the night sky.")
            .comment("Big Dipper is highlighted by default.")
            .defineListAllowEmpty("highlightedStarNames", List.of("Alkaid", "Mizar", "Alioth", "Megrez", "Phecda", "Merak", "Dubhe"), () -> null, Config::validateStarName);

    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> HIGHLIGHTED_STARS = BUILDER
            .comment("Many stars are unnamed, so as an alternative, input star ids to highlight unnamed stars here.")
            .comment("The north star, Polaris, is highlighted by default as an example.")
            .defineListAllowEmpty("highlightedStars", List.of(11734), () -> null, Config::validateStarId);

    static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean validateStarName(final Object obj) {
        Star[] validNames = StarHandler.getStarsFromJson();
        if (obj instanceof String) return Arrays.stream(validNames).map(Star::getName).anyMatch(obj::equals);
        LOGGER.warn("Highlighted Star \"{}\" not valid", obj);
        return false;
    }

    private static boolean validateStarId(final Object obj) {
        int[] validIds = Arrays.stream(StarHandler.getStarsFromJson()).map(Star::getId).mapToInt(Integer::intValue).toArray();
        if (obj instanceof Integer) return IntStream.of(validIds).anyMatch(id -> id == (int) obj);
        LOGGER.warn("Highlighted Star of id \"{}\" not valid", obj);
        return false;
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        namedStarsOnly = NAMED_STARS_ONLY.get();
        highlightedStarNames = new HashSet<>(HIGHLIGHTED_STAR_NAMES.get());
        highlightedStarsIds = new HashSet<>(HIGHLIGHTED_STARS.get());
    }
}
