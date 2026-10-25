package com.savelyich.trials.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.savelyich.trials.TrialsMod;
import com.savelyich.trials.quest.QuestData;
import com.savelyich.trials.registry.ModEffects;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public final class ClientEvents {
    private ClientEvents() {}

    public static KeyMapping OPEN_LOG;

    /** Шина MOD: клавиша J и плашка текущего задания. */
    @Mod.EventBusSubscriber(modid = TrialsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent e) {
            OPEN_LOG = new KeyMapping("key.savelyich_trials.log", InputConstants.KEY_J, "key.categories.savelyich_trials");
            e.register(OPEN_LOG);
        }

        @SubscribeEvent
        public static void overlays(RegisterGuiOverlaysEvent e) {
            e.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "tracker", (gui, g, pt, w, h) -> drawTracker(g));
        }
    }

    /** Шина FORGE: нажатия и искажение управления. */
    @Mod.EventBusSubscriber(modid = TrialsMod.MODID, value = Dist.CLIENT)
    public static final class ForgeBus {
        @SubscribeEvent
        public static void key(InputEvent.Key e) {
            Minecraft mc = Minecraft.getInstance();
            if (OPEN_LOG != null && OPEN_LOG.consumeClick() && mc.screen == null && mc.player != null) {
                mc.setScreen(new QuestLogScreen());
            }
        }

        /** Эффект «Искажение» Болотного Шёпота: WASD наоборот. */
        @SubscribeEvent
        public static void move(MovementInputUpdateEvent e) {
            if (e.getEntity().hasEffect(ModEffects.INVERTED.get())) {
                var in = e.getInput();
                in.forwardImpulse = -in.forwardImpulse;
                in.leftImpulse = -in.leftImpulse;
                boolean up = in.up;
                in.up = in.down;
                in.down = up;
                boolean left = in.left;
                in.left = in.right;
                in.right = left;
            }
        }
    }

    private static void drawTracker(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.screen != null) return;
        int a = ClientQuestState.active(), r = ClientQuestState.toReport();
        String l1, l2;
        if (r >= 0) { l1 = "§6Задание выполнено"; l2 = "§fВернитесь к Савельевичу"; }
        else if (a >= 0) { l1 = "§eЗадание " + (a + 1) + "/15: §f" + QuestData.BOSS_NAMES[a]; l2 = "§7" + QuestData.ARENA_NAMES[a] + " • J — журнал"; }
        else return;
        int w = Math.max(mc.font.width(l1), mc.font.width(l2)) + 10;
        g.fill(4, 4, 4 + w, 28, 0x88000000);
        g.drawString(mc.font, l1, 9, 8, 0xFFFFFF);
        g.drawString(mc.font, l2, 9, 18, 0xFFFFFF);
    }
}
