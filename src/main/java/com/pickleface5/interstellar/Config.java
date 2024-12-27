package com.pickleface5.interstellar;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = Interstellar.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue NAMED_STARS_ONLY = BUILDER
            .comment("Whether to only render stars with registered names. It is recommended to keep this enabled, as there is ~120000 stars total, ~500 which are named.")
            .define("namedStarsOnly", true);

    private static final ModConfigSpec.BooleanValue CONSTELLATIONS_STARS_ONLY = BUILDER
            .comment("Whether to only render stars in a defined constellation.")
            .define("constellationStarsOnly", false);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean namedStarsOnly;
    public static boolean constellationsStarsOnly;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        namedStarsOnly = NAMED_STARS_ONLY.get();
        constellationsStarsOnly = CONSTELLATIONS_STARS_ONLY.get();
    }
}
