package com.dedsafio.ruleta;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class RuletaActivation {
    private RuletaActivation() {}

    public static InteractionResult tryActivate(Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        boolean hasKey = held.is(ModRegistry.KEY.get());
        if (level.isClientSide) return hasKey ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        if (!hasKey) {
            player.displayClientMessage(Component.translatable("message.ruleta_dedsafio.need_key"), true);
            return InteractionResult.CONSUME;
        }
        if (!RuletaManager.start((ServerLevel) level, pos)) {
            player.displayClientMessage(Component.translatable("message.ruleta_dedsafio.busy"), true);
            return InteractionResult.CONSUME;
        }
        player.sendSystemMessage(Component.translatable("message.ruleta_dedsafio.accepted").withStyle(ChatFormatting.GREEN));
        return InteractionResult.SUCCESS;
    }
}
