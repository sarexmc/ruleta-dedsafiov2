package com.dedsafio.ruleta;

import com.dedsafio.ruleta.network.NetHandler;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(RuletaMod.MODID)
public class RuletaMod {
    public static final String MODID = "ruleta_dedsafio";

    public RuletaMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModRegistry.register(bus);
        ModSounds.SOUNDS.register(bus);
        NetHandler.register();
    }
}
