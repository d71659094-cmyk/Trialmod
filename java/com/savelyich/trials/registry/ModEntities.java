package com.savelyich.trials.registry;

import com.savelyich.trials.TrialsMod;
import com.savelyich.trials.entity.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class ModEntities {
    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TrialsMod.MODID);

    /** Типы боссов в порядке квестов (индекс = QuestData.BOSS_IDS). */
    public static final List<Supplier<EntityType<? extends BaseBoss>>> BOSSES = new ArrayList<>();

    private static <T extends BaseBoss> RegistryObject<EntityType<T>> boss(String id, EntityType.EntityFactory<T> f, float w, float h) {
        RegistryObject<EntityType<T>> o = ENTITIES.register(id, () -> EntityType.Builder.of(f, MobCategory.MONSTER)
                .sized(w, h).clientTrackingRange(10).build(TrialsMod.MODID + ":" + id));
        BOSSES.add(o::get);
        return o;
    }

    public static final RegistryObject<EntityType<Bosses1.EchoKeeper>> ECHO_KEEPER = boss("echo_keeper", Bosses1.EchoKeeper::new, 0.6f, 1.95f);
    public static final RegistryObject<EntityType<Bosses1.Wailer>> WAILER = boss("wailer", Bosses1.Wailer::new, 0.6f, 2.2f);
    public static final RegistryObject<EntityType<Bosses1.StoneShepherd>> STONE_SHEPHERD = boss("stone_shepherd", Bosses1.StoneShepherd::new, 1.4f, 2.7f);
    public static final RegistryObject<EntityType<Bosses1.FireflySwarm>> FIREFLY_SWARM = boss("firefly_swarm", Bosses1.FireflySwarm::new, 1.2f, 1.2f);
    public static final RegistryObject<EntityType<Bosses1.MushroomDouble>> MUSHROOM_DOUBLE = boss("mushroom_double", Bosses1.MushroomDouble::new, 0.6f, 1.95f);
    public static final RegistryObject<EntityType<Bosses2.SandSwimmer>> SAND_SWIMMER = boss("sand_swimmer", Bosses2.SandSwimmer::new, 0.7f, 1.8f);
    public static final RegistryObject<EntityType<Bosses2.IcePilgrim>> ICE_PILGRIM = boss("ice_pilgrim", Bosses2.IcePilgrim::new, 0.6f, 2.0f);
    public static final RegistryObject<EntityType<Bosses2.RustyMechanic>> RUSTY_MECHANIC = boss("rusty_mechanic", Bosses2.RustyMechanic::new, 0.8f, 2.2f);
    public static final RegistryObject<EntityType<Bosses2.Dreamer>> DREAMER = boss("dreamer", Bosses2.Dreamer::new, 0.6f, 1.95f);
    public static final RegistryObject<EntityType<Bosses2.RootWanderer>> ROOT_WANDERER = boss("root_wanderer", Bosses2.RootWanderer::new, 0.9f, 2.6f);
    public static final RegistryObject<EntityType<Bosses3.GlassWatcher>> GLASS_WATCHER = boss("glass_watcher", Bosses3.GlassWatcher::new, 1.0f, 0.8f);
    public static final RegistryObject<EntityType<Bosses3.BogWhisper>> BOG_WHISPER = boss("bog_whisper", Bosses3.BogWhisper::new, 0.6f, 1.95f);
    public static final RegistryObject<EntityType<Bosses3.GhostSmith>> GHOST_SMITH = boss("ghost_smith", Bosses3.GhostSmith::new, 0.6f, 1.95f);
    public static final RegistryObject<EntityType<Bosses3.LightDevourer>> LIGHT_DEVOURER = boss("light_devourer", Bosses3.LightDevourer::new, 0.9f, 2.4f);
    public static final RegistryObject<EntityType<Bosses3.MoonDouble>> MOON_DOUBLE = boss("moon_double", Bosses3.MoonDouble::new, 0.6f, 1.95f);

    public static final RegistryObject<EntityType<SavelyichEntity>> SAVELYICH = ENTITIES.register("savelyich", () ->
            EntityType.Builder.of(SavelyichEntity::new, MobCategory.CREATURE).sized(0.6f, 1.95f).clientTrackingRange(10)
                    .build(TrialsMod.MODID + ":savelyich"));

    public static final RegistryObject<EntityType<Bosses1.HerdStone>> HERD_STONE = ENTITIES.register("herd_stone", () ->
            EntityType.Builder.of(Bosses1.HerdStone::new, MobCategory.MISC).sized(0.9f, 0.9f).clientTrackingRange(8)
                    .build(TrialsMod.MODID + ":herd_stone"));

    public static final RegistryObject<EntityType<Bosses2.DreamerClone>> DREAMER_CLONE = ENTITIES.register("dreamer_clone", () ->
            EntityType.Builder.of(Bosses2.DreamerClone::new, MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(8)
                    .build(TrialsMod.MODID + ":dreamer_clone"));

    static {
        Bosses1.HerdStone.TYPE_SUPPLIER = HERD_STONE::get;
        Bosses2.DreamerClone.TYPE_SUPPLIER = DREAMER_CLONE::get;
    }
}
