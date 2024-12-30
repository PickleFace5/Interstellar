package com.pickleface5.interstellar.commands;

import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.brigadier.CommandDispatcher;
import com.pickleface5.interstellar.Interstellar;
import com.pickleface5.interstellar.renderer.SkyboxRenderer;
import com.pickleface5.interstellar.star.Star;
import com.pickleface5.interstellar.star.StarHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class StarsCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("stars")
                        .then(
                                Commands.literal("refresh")
                                        .executes(context -> refreshStarBuffer(context.getSource()))
                        )
                        .then(
                                Commands.literal("get")
                                        .executes(context -> getStarTarget(context.getSource()))
                        )
        );
    }

    private static int refreshStarBuffer(CommandSourceStack source) {
        SkyboxRenderer skyboxRenderer = Interstellar.getSkyboxRenderer();
         if (skyboxRenderer.reload(Tesselator.getInstance())) {
             int starAm = skyboxRenderer.getDrawnStarAmount();
             source.sendSuccess(() -> Component.translatable("commands.stars.refreshed", starAm), true);
             return starAm;
         } else {
             source.sendFailure(Component.translatable("commands.stars.no_change"));
             return 0;
         }
    }

    private static int getStarTarget(CommandSourceStack source) {
        SkyboxRenderer skyboxRenderer = Interstellar.getSkyboxRenderer();
        Star targetStar = skyboxRenderer.getPlayerTargetStar();
        if (targetStar == null) {
            source.sendFailure(Component.translatable("commands.stars.no_target"));
            return 0;
        }
        source.sendSuccess(() -> StarHandler.getTranslatableComponent(targetStar), true);
        return targetStar.getId();
    }

}
