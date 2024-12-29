package com.pickleface5.interstellar;

import com.mojang.brigadier.CommandDispatcher;
import com.pickleface5.interstellar.client.renderer.SkyboxRenderer;
import com.pickleface5.interstellar.commands.StarsCommands;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(Interstellar.MODID)
public class Interstellar
{
    public static final String MODID = "interstellar";
    private static final Logger LOGGER = LogUtils.getLogger();

    private static SkyboxRenderer skyboxRenderer;

    public Interstellar(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);

        skyboxRenderer = new SkyboxRenderer();
        NeoForge.EVENT_BUS.register(skyboxRenderer);
    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static class ClientGameEvents
    {
        @SubscribeEvent
        public static void registerClientCommands(RegisterClientCommandsEvent event) {
            CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
            StarsCommands.register(dispatcher);
        }
    }

    public static SkyboxRenderer getSkyRenderer() {
        return skyboxRenderer;
    }
}
