package com.pickleface5.interstellar.commands;

import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.brigadier.CommandDispatcher;
import com.pickleface5.interstellar.Interstellar;
import com.pickleface5.interstellar.client.renderer.SkyboxRenderer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class StarsCommands {

    public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
        pDispatcher.register(
                Commands.literal("stars")
                        .then(
                                Commands.literal("reload")
                                        .executes(context -> reloadStarBuffer(context.getSource()))
                        )
        );
    }

    private static int reloadStarBuffer(CommandSourceStack pSource) {
        Interstellar.getSkyRenderer().createStars(Tesselator.getInstance());
        int starAm = SkyboxRenderer.getStarAmount();
        pSource.sendSuccess(() -> Component.translatable("commands.stars.reloaded", starAm), true);
        return starAm;
    }

}
