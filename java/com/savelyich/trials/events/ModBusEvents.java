package com.savelyich.trials.events;

import com.savelyich.trials.TrialsMod;
import com.savelyich.trials.entity.*;
import com.savelyich.trials.registry.ModEntities;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TrialsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModBusEvents {
    private ModBusEvents() {}

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent e) {
        e.put(ModEntities.ECHO_KEEPER.get(), Bosses1.EchoKeeper.attrs().build());
        e.put(ModEntities.WAILER.get(), Bosses1.Wailer.attrs().build());
        e.put(ModEntities.STONE_SHEPHERD.get(), Bosses1.StoneShepherd.attrs().build());
        e.put(ModEntities.FIREFLY_SWARM.get(), Bosses1.FireflySwarm.attrs().build());
        e.put(ModEntities.MUSHROOM_DOUBLE.get(), Bosses1.MushroomDouble.attrs().build());
        e.put(ModEntities.SAND_SWIMMER.get(), Bosses2.SandSwimmer.attrs().build());
        e.put(ModEntities.ICE_PILGRIM.get(), Bosses2.IcePilgrim.attrs().build());
        e.put(ModEntities.RUSTY_MECHANIC.get(), Bosses2.RustyMechanic.attrs().build());
        e.put(ModEntities.DREAMER.get(), Bosses2.Dreamer.attrs().build());
        e.put(ModEntities.ROOT_WANDERER.get(), Bosses2.RootWanderer.attrs().build());
        e.put(ModEntities.GLASS_WATCHER.get(), Bosses3.GlassWatcher.attrs().build());
        e.put(ModEntities.BOG_WHISPER.get(), Bosses3.BogWhisper.attrs().build());
        e.put(ModEntities.GHOST_SMITH.get(), Bosses3.GhostSmith.attrs().build());
        e.put(ModEntities.LIGHT_DEVOURER.get(), Bosses3.LightDevourer.attrs().build());
        e.put(ModEntities.MOON_DOUBLE.get(), Bosses3.MoonDouble.attrs().build());
        e.put(ModEntities.SAVELYICH.get(), SavelyichEntity.attrs().build());
        e.put(ModEntities.HERD_STONE.get(), Bosses1.HerdStone.attrs().build());
        e.put(ModEntities.DREAMER_CLONE.get(), Bosses2.DreamerClone.attrs().build());
    }
}
