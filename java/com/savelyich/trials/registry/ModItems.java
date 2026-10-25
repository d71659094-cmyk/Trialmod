package com.savelyich.trials.registry;

import com.savelyich.trials.TrialsMod;
import com.savelyich.trials.item.Ranged;
import com.savelyich.trials.item.Weapons;
import com.savelyich.trials.quest.QuestData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TrialsMod.MODID);

    /** Порядок совпадает с QuestData.WEAPON_IDS. */
    public static final List<RegistryObject<Item>> WEAPONS = new ArrayList<>();

    private static RegistryObject<Item> weapon(String id, Supplier<Item> s) {
        RegistryObject<Item> o = ITEMS.register(id, s);
        WEAPONS.add(o);
        return o;
    }

    public static final RegistryObject<Item> CRYSTAL_BLADE = weapon("crystal_blade", Weapons.CrystalBlade::new);
    public static final RegistryObject<Item> GOLEM_SLEDGE = weapon("golem_sledge", Weapons.GolemSledge::new);
    public static final RegistryObject<Item> GHOST_BOW = weapon("ghost_bow", Ranged.GhostBow::new);
    public static final RegistryObject<Item> GUARDIAN_SPEAR = weapon("guardian_spear", Weapons.GuardianSpear::new);
    public static final RegistryObject<Item> FLAME_SICKLE = weapon("flame_sickle", Weapons.FlameSickle::new);
    public static final RegistryObject<Item> FROST_HAMMER = weapon("frost_hammer", Weapons.FrostHammer::new);
    public static final RegistryObject<Item> SHADOW_DAGGER = weapon("shadow_dagger", Weapons.ShadowDagger::new);
    public static final RegistryObject<Item> ECHO_TRIDENT = weapon("echo_trident", Ranged.EchoTrident::new);
    public static final RegistryObject<Item> BLOOD_AXE = weapon("blood_axe", Weapons.BloodAxe::new);
    public static final RegistryObject<Item> SPRING_CROSSBOW = weapon("spring_crossbow", Ranged.SpringCrossbow::new);
    public static final RegistryObject<Item> DUST_STAFF = weapon("dust_staff", Ranged.DustStaff::new);
    public static final RegistryObject<Item> THUNDER_MACE = weapon("thunder_mace", Weapons.ThunderMace::new);
    public static final RegistryObject<Item> REAPER_SCYTHE = weapon("reaper_scythe", Weapons.ReaperScythe::new);
    public static final RegistryObject<Item> KINETIC_BLADE_SHIELD = weapon("kinetic_blade_shield", Ranged.KineticBladeShield::new);
    public static final RegistryObject<Item> CURSED_TOTEM_SHOTGUN = weapon("cursed_totem_shotgun", Ranged.CursedTotemShotgun::new);

    public static final RegistryObject<Item> SOUL_ESSENCE = ITEMS.register("soul_essence", Ranged.SoulEssence::new);

    public static final RegistryObject<Item> SAVELYICH_SPAWN_EGG = ITEMS.register("savelyich_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.SAVELYICH, 0x6B4A2B, 0xD9C7A3, new Item.Properties()));

    public static ItemStack createWeaponStack(int weaponIndex) {
        return new ItemStack(WEAPONS.get(weaponIndex).get());
    }

    static { assert WEAPONS.size() == 0 || QuestData.WEAPON_IDS.length == 15; }
}
