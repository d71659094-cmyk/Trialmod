package com.savelyich.trials.client;

import com.savelyich.trials.quest.QuestData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** Клиентская копия прогресса, приходит пакетом SyncQuestsPacket. */
public final class ClientQuestState {
    private ClientQuestState() {}

    public static int[] states = new int[QuestData.COUNT];
    public static BlockPos origin = BlockPos.ZERO;
    public static boolean mapReady = false;
    public static boolean met = false;
    public static boolean finaleSeen = false;
    public static int version = 0;

    public static void update(int[] s, BlockPos o, boolean ready, boolean m, boolean f) {
        states = s.length == QuestData.COUNT ? s : new int[QuestData.COUNT];
        origin = o;
        mapReady = ready;
        met = m;
        finaleSeen = f;
        version++;
    }

    public static void openDialog() { Minecraft.getInstance().setScreen(new DialogScreen()); }

    public static int active() { for (int i = 0; i < states.length; i++) if (states[i] == QuestData.ACTIVE) return i; return -1; }

    public static int toReport() { for (int i = 0; i < states.length; i++) if (states[i] == QuestData.KILLED) return i; return -1; }

    public static int next() { for (int i = 0; i < states.length; i++) if (states[i] == QuestData.LOCKED) return i; return -1; }

    public static boolean allDone() { for (int v : states) if (v != QuestData.DONE) return false; return true; }

    public static int doneCount() { int n = 0; for (int v : states) if (v == QuestData.DONE) n++; return n; }
}
