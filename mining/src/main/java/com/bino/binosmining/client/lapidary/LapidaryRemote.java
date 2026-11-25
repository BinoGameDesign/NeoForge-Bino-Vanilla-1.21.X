package com.bino.binosmining.client.lapidary;

import com.bino.binosmining.menu.LapidaryMenu;
import com.bino.binosmining.network.LapidaryDisplayPayload;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Mesas que OUTROS jogadores estao usando (o servidor manda LapidaryDisplayPayload): o que esta em cima da mesa,
// quem esta usando (para a pose em terceira pessoa) e a animacao de lapidar quando ela comeca.
public final class LapidaryRemote {
    private LapidaryRemote() {}

    private static final class Table {
        int user = -1;
        ItemStack flint = ItemStack.EMPTY, ore = ItemStack.EMPTY;
        double cutAt = -100;
        boolean all;
        @Nullable SpinSound sound;
    }

    private static final Map<BlockPos, Table> TABLES = new HashMap<>();
    private static final Map<Integer, BlockPos> USERS = new HashMap<>();

    public static double clock() { return Util.getMillis() / 1000.0; }

    public static void receive(LapidaryDisplayPayload p) {
        BlockPos pos = p.pos().immutable();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) mc.level.getBlockEntity(pos);   // mesa antiga sem block entity: cria agora
        Table t = TABLES.computeIfAbsent(pos, k -> new Table());
        if (t.user >= 0) USERS.remove(t.user);
        t.user = p.user();
        if (t.user >= 0) USERS.put(t.user, pos);
        boolean cutting = clock() - t.cutAt < LapidaryCut.length(t.all);
        if (!cutting) { t.flint = p.flint(); t.ore = p.ore(); }   // durante a animacao, segura o que ja estava
        if (p.cut() > 0 && !isMine(p.user())) {
            t.cutAt = clock();
            t.all = p.cut() == 2;
            t.flint = p.flint(); t.ore = p.ore();
            t.sound = SpinSound.start(pos, () -> view(pos).speed());
        }
        if (t.user < 0 && !cutting) TABLES.remove(pos);
    }

    private static boolean isMine(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getId() == entityId;
    }

    // jogador (id da entidade) que esta numa mesa agora, para a pose em terceira pessoa
    @Nullable
    public static BlockPos tableOf(int entityId) { return USERS.get(entityId); }

    // segundos desde que a lapidacao comecou nesta mesa (ou -1)
    public static double cutTime(int entityId) {
        BlockPos pos = USERS.get(entityId);
        Table t = pos == null ? null : TABLES.get(pos);
        if (t == null) return -1;
        double s = clock() - t.cutAt;
        return s >= 0 && s < LapidaryCut.length(t.all) ? s : -1;
    }

    // true = a lapidacao em andamento e' "lapidar tudo" (gira mais tempo)
    public static boolean cutAll(int entityId) {
        BlockPos pos = USERS.get(entityId);
        Table t = pos == null ? null : TABLES.get(pos);
        return t != null && t.all;
    }

    public static LapidaryView view(BlockPos pos) {
        Table t = TABLES.get(pos);
        if (t == null) return LapidaryView.IDLE;
        double s = clock() - t.cutAt;
        boolean cutting = s >= 0 && s < LapidaryCut.length(t.all);
        if (!cutting) {
            boolean ready = !t.ore.isEmpty() && t.flint.is(net.minecraft.world.item.Items.FLINT);
            return new LapidaryView(t.flint, t.ore, 1f, 0f, 0f, 0f, ready, List.of(), 0f);
        }
        ItemStack disc = t.ore;
        if (s >= LapidaryCut.gemAt(t.all)) disc = gemOf(t.ore);
        if (s >= LapidaryCut.pickAt(t.all)) disc = ItemStack.EMPTY;   // a mao (de quem esta la) levou a gema
        return new LapidaryView(t.flint, disc, (float) LapidaryCut.pop(s, t.all), (float) LapidaryCut.angle(s, t.all),
                (float) LapidaryCut.armDrop(s, t.all), (float) LapidaryCut.button(s), true, List.of(), (float) LapidaryCut.speed(s, t.all));
    }

    static ItemStack gemOf(ItemStack ore) {
        Item gem = LapidaryMenu.gemFor(ore);
        return gem == null ? ItemStack.EMPTY : new ItemStack(gem);
    }

    // faiscas das mesas dos outros (a propria cena faz as dela)
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) { TABLES.clear(); USERS.clear(); return; }
        for (Map.Entry<BlockPos, Table> e : TABLES.entrySet()) {
            LapidaryView v = view(e.getKey());
            if (v.speed() > 0.05) LapidaryFx.sparks(mc.level, e.getKey(), v);
        }
    }

    public static void clear() { TABLES.clear(); USERS.clear(); }
}
