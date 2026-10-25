package com.savelyich.trials.client;

import com.savelyich.trials.net.DialogActionPacket;
import com.savelyich.trials.net.Net;
import com.savelyich.trials.quest.QuestData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** Окно разговора с Савельевичем. Текст зависит от прогресса игрока. */
public class DialogScreen extends Screen {
    private static final int W = 300, H = 150;

    private int seenVersion = -1;
    private int introPage = 0;
    private boolean finaleShown = false;
    private String text = "";
    private String title = "Савельевич";

    public DialogScreen() { super(Component.literal("Савельевич")); }

    private int left() { return (width - W) / 2; }
    private int top() { return height - H - 24; }

    @Override
    protected void init() { rebuild(); }

    /** Пересобирает текст и кнопки под текущее состояние. */
    private void rebuild() {
        clearWidgets();
        seenVersion = ClientQuestState.version;
        int bx = left() + 10, by = top() + H - 28;
        int rep = ClientQuestState.toReport();
        int act = ClientQuestState.active();
        int nxt = ClientQuestState.next();

        if (!ClientQuestState.met) {
            text = QuestData.INTRO[Math.min(introPage, QuestData.INTRO.length - 1)];
            boolean last = introPage >= QuestData.INTRO.length - 1;
            addRenderableWidget(Button.builder(Component.literal(last ? "По рукам" : "Дальше"), b -> {
                if (last) {
                    Net.CHANNEL.sendToServer(new DialogActionPacket(DialogActionPacket.MEET, 0));
                    ClientQuestState.met = true;
                    rebuild();
                } else { introPage++; rebuild(); }
            }).bounds(bx, by, 100, 20).build());
            return;
        }
        if (rep >= 0) {
            text = QuestData.TURN_IN[rep];
            addRenderableWidget(Button.builder(Component.literal("Сдать задание"), b ->
                    Net.CHANNEL.sendToServer(new DialogActionPacket(DialogActionPacket.REPORT, rep))).bounds(bx, by, 120, 20).build());
        } else if (act >= 0) {
            text = QuestData.REMIND[act];
            addRenderableWidget(Button.builder(Component.literal("Журнал (J)"), b -> {
                onClose();
                net.minecraft.client.Minecraft.getInstance().setScreen(new QuestLogScreen());
            }).bounds(bx, by, 100, 20).build());
        } else if (nxt >= 0) {
            text = QuestData.OFFER[nxt];
            addRenderableWidget(Button.builder(Component.literal("Взять: " + QuestData.weaponNameForQuest(nxt)), b ->
                    Net.CHANNEL.sendToServer(new DialogActionPacket(DialogActionPacket.ACCEPT, nxt))).bounds(bx, by, 200, 20).build());
        } else {
            text = finaleShown || ClientQuestState.finaleSeen ? QuestData.ALL_DONE_IDLE : QuestData.FINALE;
            if (!finaleShown && !ClientQuestState.finaleSeen) {
                finaleShown = true;
                ClientQuestState.finaleSeen = true;
                Net.CHANNEL.sendToServer(new DialogActionPacket(DialogActionPacket.FINALE, 0));
            }
        }
        addRenderableWidget(Button.builder(Component.literal("Закрыть"), b -> onClose())
                .bounds(left() + W - 90, by, 80, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        if (seenVersion != ClientQuestState.version) rebuild();
        renderBackground(g);
        int x = left(), y = top();
        g.fill(x - 2, y - 2, x + W + 2, y + H + 2, 0xFF3B2A14);
        g.fill(x, y, x + W, y + H, 0xEE1B1409);
        g.fill(x, y, x + W, y + 16, 0xFF5A4020);
        g.drawString(font, "§6" + title, x + 8, y + 4, 0xFFFFFF);
        List<FormattedCharSequence> lines = font.split(Component.literal(text), W - 20);
        int ty = y + 24;
        for (FormattedCharSequence l : lines) { g.drawString(font, l, x + 10, ty, 0xE8DCC0); ty += 11; }
        super.render(g, mx, my, pt);
    }

    @Override public boolean isPauseScreen() { return false; }
}
