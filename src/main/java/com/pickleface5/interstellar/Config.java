package com.pickleface5.interstellar;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@EventBusSubscriber(modid = Interstellar.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue NAMED_STARS_ONLY = BUILDER
            .comment("Whether to only render stars with registered names. It is recommended to keep this enabled, as there is ~120000 stars total, ~500 which are named.")
            .define("namedStarsOnly", true);

    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> HIGHLIGHTED_STARS = BUILDER
            .comment("Highlighted stars will appear brighter than normal. Enter star ids.")
            .comment("Big dipper and little dipper are highlighted by default as an example.")
            .defineListAllowEmpty("highlightedStars", List.of(67088), () -> null, Config::validateStar);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean namedStarsOnly;
    public static Set<Integer> highlightedStars;

    private static boolean validateStar(final Object obj) {
        return obj instanceof Integer;
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        namedStarsOnly = NAMED_STARS_ONLY.get();

        highlightedStars = new HashSet<>(HIGHLIGHTED_STARS.get());
    }
}
