package com.dedsafio.ruleta;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    private ModSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, RuletaMod.MODID);

    private static RegistryObject<SoundEvent> reg(String n) {
        return SOUNDS.register(n, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(RuletaMod.MODID, n)));
    }

    public static final RegistryObject<SoundEvent> USE = reg("use");
    public static final RegistryObject<SoundEvent> BOOM = reg("boom");
    public static final RegistryObject<SoundEvent> BED = reg("bed");
    public static final RegistryObject<SoundEvent> THUMP = reg("thump");
    public static final RegistryObject<SoundEvent> FINALE = reg("finale");
}
