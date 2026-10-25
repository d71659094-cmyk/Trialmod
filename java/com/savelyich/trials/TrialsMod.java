package com.savelyich.trials;

import com.savelyich.trials.net.Net;
import com.savelyich.trials.registry.ModCreativeTabs;
import com.savelyich.trials.registry.ModEffects;
import com.savelyich.trials.registry.ModEntities;
import com.savelyich.trials.registry.ModItems;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TrialsMod.MODID)
public class TrialsMod {
    public static final String MODID = "savelyich_trials";

    public TrialsMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(bus);
        ModEntities.ENTITIES.register(bus);
        ModEffects.EFFECTS.register(bus);
        ModCreativeTabs.TABS.register(bus);
        bus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(new com.savelyich.trials.events.WeaponEvents());
        MinecraftForge.EVENT_BUS.register(new com.savelyich.trials.events.WorldEvents());
        MinecraftForge.EVENT_BUS.register(new com.savelyich.trials.events.CommandEvents());
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(Net::register);
    }
}
