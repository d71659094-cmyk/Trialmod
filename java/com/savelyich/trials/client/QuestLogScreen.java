package com.savelyich.trials.client;

import com.savelyich.trials.net.Net;
import com.savelyich.trials.net.TeleportPacket;
import com.savelyich.trials.quest.QuestData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** Журнал заданий (клавиша J): список 15 испытаний слева, подробности справа. */
public class QuestLogScreen extends Screen {
    private static final int W = 380, H = 230, LIST_W = 140, ROW = 12;

    private int selected = -1;
    private int seen = -1;
    private Button tpBtn, hubBtn;

    public QuestLogScreen() { super(Component.literal("Журнал испытаний")); }

    private int left() { return (width - W) / 2; }
    private int top() { return (height - H) / 2; }

    @Override
    protected void init() {
        if (selected < 0) {
            int a = ClientQuestState.active(), r = ClientQuestState.toReport(), n = ClientQuestState.next();
            selected = a >= 0 ? a : r >= 0 ? r : n >= 0 ? n : 0;
        }
        int x = left() + LIST_W + 20, y = top() + H - 28;
        tpBtn = addRenderableWidget(Button.builder(Component.literal("К арене"), b -> {
            if (selected >= 0) Net.CHANNEL.sendToServer(new TeleportPacket(selected));
            onClose();
        }).bounds(x, y, 90, 20).build());
        hubBtn = addRenderableWidget(Button.builder(Component.literal("В центр"), b -> {
            Net.CHANNEL.sendToServer(new TeleportPacket(-1));
            onClose();
        }).bounds(x + 96, y, 80, 20).build());
        refreshButtons();
    }

    private void refreshButtons() {
        seen = ClientQuestState.version;
        tpBtn.active = ClientQuestState.mapReady && selected >= 0 && ClientQuestState.states[selected] == QuestData.ACTIVE;
        hubBtn.active = ClientQuestState.mapReady;
    }

    private int statusColor(int i) {
        return switch (ClientQuestState.states[i]) {
            case QuestData.DONE -> 0x55FF55;
            case QuestData.KILLED -> 0xFFAA00;
            case QuestData.ACTIVE -> 0xFFFF55;
            default -> i == ClientQuestState.next() && ClientQuestState.active() < 0 && ClientQuestState.toReport() < 0 ? 0xFFFFFF : 0x777777;
        };
    }

    private String statusText(int i) {
        return switch (ClientQuestState.states[i]) {
            case QuestData.DONE -> "§aВыполнено";
            case QuestData.KILLED -> "§6Цель убита — сдайте задание Савельевичу";
            case QuestData.ACTIVE -> "§eВ процессе";
            default -> i == ClientQuestState.next() && ClientQuestState.active() < 0 && ClientQuestState.toReport() < 0
                    ? "§fДоступно — поговорите с Савельевичем" : "§7Закрыто: сначала завершите предыдущие";
        };
    }

    private boolean revealed(int i) {
        return ClientQuestState.states[i] != QuestData.LOCKED || i == ClientQuestState.next();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int x = left() + 8, y = top() + 30;
        for (int i = 0; i < QuestData.COUNT; i++) {
            int ry = y + i * ROW;
            if (mx >= x && mx <= x + LIST_W && my >= ry && my < ry + ROW) { selected = i; refreshButtons(); return true; }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        if (seen != ClientQuestState.version) refreshButtons();
        renderBackground(g);
        int x = left(), y = top();
        g.fill(x - 2, y - 2, x + W + 2, y + H + 2, 0xFF3B2A14);
        g.fill(x, y, x + W, y + H, 0xF01B1409);
        g.fill(x, y, x + W, y + 18, 0xFF5A4020);
        g.drawString(font, "§6Журнал испытаний Савельевича", x + 8, y + 5, 0xFFFFFF);
        g.drawString(font, "§7" + ClientQuestState.doneCount() + "/" + QuestData.COUNT, x + W - 36, y + 5, 0xFFFFFF);

        // список
        for (int i = 0; i < QuestData.COUNT; i++) {
            int ry = y + 30 + i * ROW;
            if (i == selected) g.fill(x + 6, ry - 1, x + 8 + LIST_W, ry + ROW - 2, 0x66FFFFFF);
            String name = revealed(i) ? QuestData.BOSS_NAMES[i] : "???";
            g.drawString(font, (i + 1) + ". " + name, x + 10, ry, statusColor(i));
        }
        g.fill(x + LIST_W + 12, y + 24, x + LIST_W + 13, y + H - 34, 0xFF5A4020);

        // подробности
        if (selected >= 0) {
            int dx = x + LIST_W + 20, dy = y + 26, dw = W - LIST_W - 32;
            boolean open = revealed(selected);
            g.drawString(font, "§l" + (open ? QuestData.BOSS_NAMES[selected] : "???"), dx, dy, statusColor(selected));
            g.drawString(font, "§7Арена: §f" + (open ? QuestData.ARENA_NAMES[selected] : "???"), dx, dy + 13, 0xFFFFFF);
            g.drawString(font, "§7Оружие: §f" + (open ? QuestData.weaponNameForQuest(selected) : "???"), dx, dy + 24, 0xFFFFFF);
            g.drawString(font, statusText(selected), dx, dy + 38, 0xFFFFFF);
            if (open && ClientQuestState.states[selected] != QuestData.LOCKED || selected == ClientQuestState.next()) {
                g.drawString(font, "§eМеханика:", dx, dy + 54, 0xFFFFFF);
                List<FormattedCharSequence> lines = font.split(Component.literal(QuestData.MECHANIC[selected]), dw);
                int ty = dy + 66;
                for (FormattedCharSequence l : lines) { g.drawString(font, l, dx, ty, 0xD8D0B8); ty += 10; }
            }
        }
        g.drawString(font, "§8J — закрыть", x + 8, y + H - 12, 0xFFFFFF);
        super.render(g, mx, my, pt);
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (ClientEvents.OPEN_LOG != null && ClientEvents.OPEN_LOG.matches(key, scan)) { onClose(); return true; }
        return super.keyPressed(key, scan, mods);
    }
}
