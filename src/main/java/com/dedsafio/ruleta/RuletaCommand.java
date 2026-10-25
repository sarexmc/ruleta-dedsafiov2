package com.dedsafio.ruleta;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RuletaMod.MODID)
public final class RuletaCommand {
    private RuletaCommand() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("ruleta")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("iniciar")
                        .executes(c -> run(c, BlockPos.containing(c.getSource().getPosition())))
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(c -> run(c, BlockPosArgument.getLoadedBlockPos(c, "pos")))))
                .then(Commands.literal("detener").executes(c -> {
                    int n = RuletaManager.abortAll();
                    c.getSource().sendSuccess(() -> Component.literal("Ruletas detenidas: " + n), true);
                    return n;
                })));
    }

    private static int run(CommandContext<CommandSourceStack> c, BlockPos pos) {
        if (RuletaManager.start(c.getSource().getLevel(), pos)) {
            c.getSource().sendSuccess(() -> Component.literal("Ruleta iniciada en " + pos.toShortString()), true);
            return 1;
        }
        c.getSource().sendFailure(Component.translatable("message.ruleta_dedsafio.busy"));
        return 0;
    }
}
