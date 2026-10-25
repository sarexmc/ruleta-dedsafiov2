package com.dedsafio.ruleta;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** La Dedita Celeste: objeto 2D de activacion, con nombre en celeste como en el video. */
public class DeditaItem extends Item {
    public DeditaItem(Properties props) {
        super(props);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId(stack)).withStyle(ChatFormatting.AQUA);
    }
}
