package com.pickleface5.interstellar;

import com.mojang.brigadier.CommandDispatcher;
import com.pickleface5.interstellar.renderer.SkyboxRenderer;
import com.pickleface5.interstellar.commands.StarsCommand;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Interstellar.MODID)
public class Interstellar
{
    public static final String MODID = "interstellar";
    //private static final Logger LOGGER = LogUtils.getLogger();

    private static SkyboxRenderer skyboxRenderer;

    public Interstellar(IEventBus modEventBus, ModContainer modContainer) {
        skyboxRenderer = new SkyboxRenderer();
        NeoForge.EVENT_BUS.register(skyboxRenderer);

        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static class ClientGameEvents
    {
        @SubscribeEvent
        public static void registerClientCommands(RegisterClientCommandsEvent event) {
            CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
            StarsCommand.register(dispatcher);
        }
    }

    public static SkyboxRenderer getSkyboxRenderer() {
        return skyboxRenderer;
    }
}
