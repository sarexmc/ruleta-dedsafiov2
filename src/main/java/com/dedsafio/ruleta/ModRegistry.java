package com.dedsafio.ruleta;

import com.dedsafio.ruleta.block.RuletaAltarBlock;
import com.dedsafio.ruleta.block.RuletaTriggerBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRegistry {
    private ModRegistry() {}

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, RuletaMod.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RuletaMod.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RuletaMod.MODID);

    public static final RegistryObject<Block> ALTAR = BLOCKS.register("ruleta_altar",
            () -> new RuletaAltarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK).strength(4.0f, 1200.0f).noOcclusion().lightLevel(s -> 4)));

    public static final RegistryObject<Block> TRIGGER = BLOCKS.register("ruleta_trigger",
            () -> new RuletaTriggerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NONE).strength(-1.0f, 3600000.0f).noOcclusion().noLootTable()));

    public static final RegistryObject<Item> ALTAR_ITEM = ITEMS.register("ruleta_altar",
            () -> new BlockItem(ALTAR.get(), new Item.Properties()));
    public static final RegistryObject<Item> TRIGGER_ITEM = ITEMS.register("ruleta_trigger",
            () -> new BlockItem(TRIGGER.get(), new Item.Properties()));
    public static final RegistryObject<Item> KEY = ITEMS.register("ruleta_llave",
            () -> new DeditaItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("ruleta",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ruleta_dedsafio"))
                    .icon(() -> new ItemStack(KEY.get()))
                    .displayItems((params, out) -> {
                        out.accept(KEY.get());
                        out.accept(ALTAR_ITEM.get());
                        out.accept(TRIGGER_ITEM.get());
                    }).build());

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        TABS.register(bus);
    }
}
