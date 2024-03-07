package com.reclizer.csgobox.sounds;

import com.reclizer.csgobox.CsgoBox;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
//@OnlyIn(Dist.CLIENT)
public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, CsgoBox.MODID);
    private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CsgoBox.MODID, name)));
    }
    public static RegistryObject<SoundEvent> CS_DITA = registerSoundEvent("cs_dita");
    public static RegistryObject<SoundEvent> CS_OPEN = registerSoundEvent("cs_open");
    public static RegistryObject<SoundEvent> CS_FINSH = registerSoundEvent("cs_finish");


}
