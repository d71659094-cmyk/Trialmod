package com.savelyich.trials.registry;

import com.savelyich.trials.TrialsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    private ModCreativeTabs() {}

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TrialsMod.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.savelyich_trials"))
                    .icon(() -> new ItemStack(ModItems.CRYSTAL_BLADE.get()))
                    .displayItems((params, out) -> {
                        ModItems.WEAPONS.forEach(w -> out.accept(w.get()));
                        out.accept(ModItems.SOUL_ESSENCE.get());
                        out.accept(ModItems.SAVELYICH_SPAWN_EGG.get());
                    })
                    .build());
}
