package com.savelyich.trials.events;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.savelyich.trials.map.BossSpawner;
import com.savelyich.trials.map.MapBuilder;
import com.savelyich.trials.quest.ArenaLayout;
import com.savelyich.trials.quest.MapData;
import com.savelyich.trials.quest.QuestData;
import com.savelyich.trials.quest.QuestManager;
import com.savelyich.trials.registry.ModItems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * /trial hub — вернуться к Савельевичу (всем).
 * Остальное — только для операторов: build, respawn, reset, tp N, weapons.
 */
public class CommandEvents {

    @SubscribeEvent
    public void register(RegisterCommandsEvent ev) {
        ev.getDispatcher().register(Commands.literal("trial")
                .then(Commands.literal("hub").executes(c -> hub(c.getSource())))
                .then(Commands.literal("build").requires(s -> s.hasPermission(2)).executes(c -> build(c.getSource())))
                .then(Commands.literal("respawn").requires(s -> s.hasPermission(2)).executes(c -> respawn(c.getSource())))
                .then(Commands.literal("reset").requires(s -> s.hasPermission(2)).executes(c -> reset(c.getSource())))
                .then(Commands.literal("weapons").requires(s -> s.hasPermission(2)).executes(c -> weapons(c.getSource())))
                .then(Commands.literal("tp").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("n", IntegerArgumentType.integer(1, 15))
                                .executes(c -> tp(c.getSource(), IntegerArgumentType.getInteger(c, "n") - 1)))));
    }

    private static int hub(CommandSourceStack src) {
        ServerPlayer p = src.getPlayer();
        if (p == null) return 0;
        MapData md = MapData.get(p.server.overworld());
        if (!md.hasOrigin || !md.built) { src.sendFailure(Component.literal("Карта ещё не построена.")); return 0; }
        p.teleportTo(p.server.overworld(), md.origin.getX() + 0.5, md.origin.getY() + 1, md.origin.getZ() + 4.5, 180f, 0f);
        return 1;
    }

    private static int build(CommandSourceStack src) {
        ServerPlayer p = src.getPlayer();
        if (p == null) return 0;
        ServerLevel ow = p.server.overworld();
        if (MapBuilder.isRunning()) { src.sendFailure(Component.literal("Строительство уже идёт.")); return 0; }
        BlockPos at = p.blockPosition().below();
        MapBuilder.start(ow, at);
        src.sendSuccess(() -> Component.literal("Строю карту вокруг " + at.toShortString() + " (центр — под ногами)."), true);
        return 1;
    }

    private static int respawn(CommandSourceStack src) {
        ServerLevel ow = src.getServer().overworld();
        MapData md = MapData.get(ow);
        if (!md.hasOrigin) { src.sendFailure(Component.literal("Карта ещё не построена.")); return 0; }
        int n = BossSpawner.ensureAll(ow, md.origin);
        src.sendSuccess(() -> Component.literal("Возвращено боссов: " + n), true);
        return n;
    }

    private static int reset(CommandSourceStack src) {
        ServerPlayer p = src.getPlayer();
        if (p == null) return 0;
        QuestManager.resetAll(p);
        src.sendSuccess(() -> Component.literal("Прогресс квестов сброшен."), false);
        return 1;
    }

    private static int weapons(CommandSourceStack src) {
        ServerPlayer p = src.getPlayer();
        if (p == null) return 0;
        for (int i = 0; i < QuestData.WEAPON_IDS.length; i++) {
            ItemStack s = ModItems.createWeaponStack(i);
            if (!p.getInventory().add(s)) p.drop(s, false);
        }
        p.getInventory().add(new ItemStack(ModItems.SOUL_ESSENCE.get(), 64));
        return 15;
    }

    private static int tp(CommandSourceStack src, int i) {
        ServerPlayer p = src.getPlayer();
        MapData md = MapData.get(src.getServer().overworld());
        if (p == null || !md.hasOrigin) return 0;
        BlockPos e = ArenaLayout.entrance(md.origin, i);
        p.teleportTo(src.getServer().overworld(), e.getX() + 0.5, e.getY() + 1, e.getZ() + 0.5, p.getYRot(), 0);
        return 1;
    }
}
