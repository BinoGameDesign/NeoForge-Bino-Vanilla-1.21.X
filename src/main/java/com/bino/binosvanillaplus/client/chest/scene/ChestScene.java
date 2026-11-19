package com.bino.binosvanillaplus.client.chest.scene;

import com.bino.binosvanillaplus.client.lapidary.LapidaryCut;
import com.bino.binosvanillaplus.client.lapidary.LapidaryHands;
import com.bino.binosvanillaplus.client.lapidary.LapidaryPick;
import com.bino.binosvanillaplus.client.lapidary.LapidaryRemote;
import com.bino.binosvanillaplus.mimic.MimicEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

// Cena "estilo cinema" do bau (mesmo sistema da Mesa de Lapidacao): no lugar da janela, a camera se debruca sobre o
// bau aberto e os itens aparecem em 3D la dentro, em grade (9x3; bau duplo 18x3, metade esquerda = primeiras 27 casas).
//  - Clique num item = pega (fica na mao direita); clique numa casa com item na mao = poe; botao direito = metade/um.
//  - Shift + clique = manda direto para o inventario (o item voa ate a camera).
//  - Barra embaixo: a barra de atalhos do inventario; "+" abre o resto. Shift + clique la = guarda no bau.
//  - Esc ou a tecla de inventario saem. Abrir o bau agachado = janela classica.
//  - Mimico: puxar um item faz o servidor fechar o bau e soltar o mimico. A camera FICA na cena vendo ele abrir a
//    boca, treme na mordida e so entao volta.
public final class ChestScene {
    private ChestScene() {}

    public enum Phase { NONE, INTRO, ACTIVE, OUTRO }

    public static Phase phase = Phase.NONE;
    static BlockPos pos = BlockPos.ZERO;
    static Direction facing = Direction.NORTH;
    static boolean dbl;
    static int rows = 3;
    static Vec3 center = Vec3.ZERO;
    static double phaseAt, holdUntil, biteAt = -100, mimicSeenAt = -100, activeAt = -100, denyAt = -100;
    static boolean leaving, serverClosed;
    @Nullable static CameraType savedCamera;
    @Nullable static BlockPos lastClicked;
    static int lastSlot = -1;
    static List<ItemStack> snapshot = List.of();

    public static int hoverSlot = -1;
    public static boolean overBar;
    public static double weight;

    record Flying(ItemStack stack, Vec3 from, double start) {}
    static final List<Flying> flying = new ArrayList<>();
    // item que a mao leva da barra ate uma casa do bau (a casa fica vazia ate ele chegar)
    record Placing(ItemStack stack, int slot, double start) {}
    static final List<Placing> placing = new ArrayList<>();
    static final double PLACE = 0.34;
    static final double LEAN = 0.75, OUTRO = 0.6, FLY = 0.42, BITE_HOLD = 1.05, BITE_AT = 0.72;

    // medidas do bau (em blocos, a partir do centro do bau; y a partir do chao do bloco)
    static final double FLOOR = 8 / 16.0 + 0.012, HALF_DEPTH = 5.5 / 16.0;
    static double halfWidth() { return dbl ? 14 / 16.0 : 6 / 16.0; }
    static int cols() { return dbl ? 18 : 9; }

    public static double clock() { return LapidaryRemote.clock(); }
    public static boolean active() { return phase != Phase.NONE; }

    private static double clickedAt = -100;
    public static void clicked(BlockPos p) { lastClicked = p.immutable(); clickedAt = clock(); }

    // ---------------------------------------------------------------- inicio e fim
    public static boolean begin(ChestMenu menu) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || phase != Phase.NONE || player.isPassenger() || player.isShiftKeyDown()) return false;
        BlockPos at = null;
        if (mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK && isChest(mc.level.getBlockState(hit.getBlockPos())))
            at = hit.getBlockPos();
        if (at == null && lastClicked != null && clock() - clickedAt < 1.0 && isChest(mc.level.getBlockState(lastClicked))) at = lastClicked;
        if (at == null) return false;
        BlockState st = mc.level.getBlockState(at);
        rows = menu.getRowCount();
        facing = st.getValue(BlockStateProperties.HORIZONTAL_FACING);
        center = new Vec3(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        dbl = false;
        if (rows == 6 && st.getBlock() instanceof ChestBlock && st.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            BlockPos o = at.relative(ChestBlock.getConnectedDirection(st));
            center = center.add(Vec3.atLowerCornerOf(o.subtract(at)).scale(0.5));
            dbl = true;
        } else if (rows != 3) return false;

        pos = at.immutable();
        phase = Phase.INTRO;
        phaseAt = clock();
        leaving = serverClosed = false;
        biteAt = mimicSeenAt = activeAt = denyAt = -100;
        hoverSlot = lastSlot = -1;
        overBar = false;
        flying.clear();
        placing.clear();
        snapshot = List.of();
        savedCamera = mc.options.getCameraType();
        if (!savedCamera.isFirstPerson()) mc.options.setCameraType(CameraType.FIRST_PERSON);
        ChestHands.begin();
        return true;
    }

    static boolean isChest(BlockState st) {
        return (st.getBlock() instanceof ChestBlock || st.getBlock() instanceof EnderChestBlock) && st.hasProperty(BlockStateProperties.HORIZONTAL_FACING);
    }

    public static void leave() {
        if (phase != Phase.INTRO && phase != Phase.ACTIVE) return;
        Minecraft mc = Minecraft.getInstance();
        leaving = true;
        startOutro(false);
        if (mc.player != null && mc.player.containerMenu instanceof ChestMenu m) {
            mc.player.connection.send(new ServerboundContainerClosePacket(m.containerId));
            mc.player.containerMenu = mc.player.inventoryMenu;
        }
        if (mc.screen instanceof ChestSceneScreen) mc.setScreen(null);
    }

    // a tela fechou sem ser pelo Esc: o servidor fechou o bau (mimico!) ou outra tela abriu
    public static void screenClosed() {
        if (leaving || phase == Phase.NONE || phase == Phase.OUTRO) return;
        Minecraft mc = Minecraft.getInstance();
        boolean byServer = true;
        if (mc.player != null && mc.player.containerMenu instanceof ChestMenu m) {
            byServer = false;
            mc.player.connection.send(new ServerboundContainerClosePacket(m.containerId));
            mc.player.containerMenu = mc.player.inventoryMenu;
        }
        startOutro(byServer);
    }

    private static void startOutro(boolean byServer) {
        phase = Phase.OUTRO;
        phaseAt = clock();
        serverClosed = byServer;
        holdUntil = phaseAt + (byServer ? 0.3 : 0);   // espera um pouquinho: se for mimico, ele aparece agora
    }

    private static void finish() {
        Minecraft mc = Minecraft.getInstance();
        phase = Phase.NONE;
        leaving = false;
        flying.clear();
        snapshot = List.of();
        if (savedCamera != null && !savedCamera.isFirstPerson()) mc.options.setCameraType(savedCamera);
        savedCamera = null;
    }

    public static void abort() {
        phase = Phase.NONE;
        leaving = false;
        flying.clear();
    }

    // ---------------------------------------------------------------- tick
    public static void tick() {
        if (phase == Phase.NONE) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) { abort(); return; }
        double now = clock();
        if (phase == Phase.OUTRO) {
            if (serverClosed && mimicSeenAt < 0 && now - phaseAt < 1.0
                    && !mc.level.getEntitiesOfClass(MimicEntity.class, new AABB(center, center).inflate(2.5)).isEmpty()) {
                mimicSeenAt = now;
                holdUntil = now + BITE_HOLD;
                biteAt = now + BITE_AT;
            }
            if (now >= holdUntil + OUTRO) finish();
            return;
        }
        if (!(player.containerMenu instanceof ChestMenu)) return;   // screenClosed cuida
        if (phase == Phase.INTRO && now - phaseAt >= LEAN) {
            phase = Phase.ACTIVE;
            phaseAt = activeAt = now;
            float yaw = facing.getOpposite().toYRot();
            player.setYRot(yaw);
            player.yHeadRot = yaw;
            player.yBodyRot = yaw;
            player.setXRot(40f);
        }
    }

    public static void movementInput(Input input) {
        if (phase == Phase.NONE) return;
        input.leftImpulse = 0;
        input.forwardImpulse = 0;
        input.jumping = false;
        input.shiftKeyDown = false;
        input.up = input.down = input.left = input.right = false;
    }

    // ---------------------------------------------------------------- geometria
    static Vec3 local(double x, double y, double z) {
        Vec3 f = Vec3.atLowerCornerOf(facing.getNormal());
        Vec3 r = Vec3.atLowerCornerOf(facing.getCounterClockWise().getNormal());   // direita de quem olha o bau
        return center.add(r.scale(x)).add(0, y, 0).add(f.scale(z));
    }

    // centro da casa do item no fundo do bau
    // ---------------------------------------------------------------- arrumacao: itens ESPALHADOS pelo fundo
    // So os itens que existem entram: eles se espalham pelo bau todo (poucos itens = maiores e mais soltos), cada um
    // com um leve desvio e giro proprios (fixos por casa). Quando um item entra ou sai, os outros deslizam.
    private static final Map<Integer, double[]> TARGET = new HashMap<>();   // casa -> {x, z, giro}
    private static final Map<Integer, Vec3> SHOWN = new HashMap<>();       // posicao suavizada
    static double itemSize = 0.2;
    private static double lastLayout;

    static void layout(List<Integer> occupied) {
        TARGET.clear();
        int n = occupied.size();
        if (n == 0) return;
        double w = 2 * halfWidth() - 0.04, d = 2 * HALF_DEPTH;
        int cols = Math.max(1, (int) Math.ceil(Math.sqrt(n * w / d)));
        int rws = (int) Math.ceil(n / (double) cols);
        double cw = w / cols, cd = d / rws;
        itemSize = Mth.clamp(Math.min(cw * 1.05, cd * 0.9), 0.11, 0.24);
        for (int k = 0; k < n; k++) {
            int slot = occupied.get(k), c = k % cols, r = k / cols;
            int inRow = r == rws - 1 ? n - r * cols : cols;
            double rowOff = (cols - inRow) * cw / 2;
            long h = (slot + 1) * 0x9E3779B97F4A7C15L;
            double jx = (((h >>> 16) & 255) / 255.0 - 0.5) * 0.4 * cw;
            double jz = (((h >>> 24) & 255) / 255.0 - 0.5) * 0.4 * cd;
            double yaw = ((h >>> 40) & 63) - 32;
            TARGET.put(slot, new double[]{-w / 2 + rowOff + (c + 0.5) * cw + jx, -d / 2 + (r + 0.5) * cd + jz, yaw});
        }
    }

    private static void smooth() {
        double now = clock(), dt = Mth.clamp(now - lastLayout, 0, 0.1);
        lastLayout = now;
        double a = 1 - Math.exp(-12 * dt);
        SHOWN.keySet().removeIf(k -> !TARGET.containsKey(k));
        for (var e : TARGET.entrySet()) {
            Vec3 t = local(e.getValue()[0], FLOOR, e.getValue()[1]);
            Vec3 cur = SHOWN.get(e.getKey());
            SHOWN.put(e.getKey(), cur == null ? t : cur.add(t.subtract(cur).scale(a)));
        }
    }

    // onde o item da casa esta no fundo do bau
    static Vec3 cellPoint(int slot) {
        Vec3 v = SHOWN.get(slot);
        if (v != null) return v;
        double[] t = TARGET.get(slot);
        return t != null ? local(t[0], FLOOR, t[1]) : local(0, FLOOR, 0);
    }

    static float itemYaw(int slot) {
        double[] t = TARGET.get(slot);
        return t == null ? 0f : (float) t[2];
    }

    // onde o raio do mouse encontra o fundo do bau (em coordenadas locais {x, z}) ou null
    @Nullable
    static double[] onFloor() {
        Vec3[] ray = LapidaryPick.ray();
        if (ray == null || Math.abs(ray[1].y) < 1e-4) return null;
        double y = center.y + FLOOR + 0.03;
        double k = (y - ray[0].y) / ray[1].y;
        if (k <= 0) return null;
        Vec3 d = ray[0].add(ray[1].scale(k)).subtract(center);
        Vec3 f = Vec3.atLowerCornerOf(facing.getNormal());
        Vec3 r = Vec3.atLowerCornerOf(facing.getCounterClockWise().getNormal());
        return new double[]{d.dot(r), d.dot(f)};
    }

    public static void updateHover() {
        hoverSlot = -1;
        if (phase != Phase.ACTIVE) return;
        Vec3[] ray = LapidaryPick.ray();
        if (ray == null) return;
        double best = Double.MAX_VALUE;
        for (int slot : TARGET.keySet()) {
            Vec3 to = cellPoint(slot).add(0, itemSize * 0.25, 0).subtract(ray[0]);
            double along = to.dot(ray[1]);
            if (along <= 0) continue;
            double off = to.subtract(ray[1].scale(along)).length();
            if (off < itemSize * 0.6 && off < best) { best = off; hoverSlot = slot; }
        }
    }

    // ---------------------------------------------------------------- camera
    public record Pose(Vec3 pos, float yaw, float pitch) {}
    private static double parX, parY, lastFrame, focusW;
    @Nullable private static Vec3 lastFocus;

    static Pose scenePose() {
        // mais de cima (~53 graus): o fundo do bau aparece inteiro e a tampa aberta nao domina a tela
        Vec3 eye = dbl ? local(0, 1.78, 0.98) : local(0, 1.56, 0.8);
        Vec3 lookAt = local(0, 0.5, -0.02);
        // o olhar acompanha um pouco o item sob o mouse (e o item que a mao esta pegando)
        Vec3 focus = hoverSlot >= 0 && phase == Phase.ACTIVE ? cellPoint(hoverSlot) : null;
        focusW += ((focus != null ? 1 : 0) - focusW) * (1 - Math.exp(-5 * Mth.clamp(clock() - lastFrame, 0, 0.1)));
        if (focus != null) lastFocus = focus;
        if (lastFocus != null && focusW > 0.001) lookAt = lookAt.add(lastFocus.subtract(lookAt).scale(0.18 * focusW));
        Vec3 look = lookAt.subtract(eye);
        float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(look.y, Math.hypot(look.x, look.z)));
        double t = clock();
        double dt = Mth.clamp(t - lastFrame, 0, 0.1);
        lastFrame = t;
        double[] ndc = phase == Phase.ACTIVE ? LapidaryPick.mouseNdc() : new double[]{0, 0};
        parX += (Mth.clamp(ndc[0], -1, 1) - parX) * (1 - Math.exp(-4 * dt));
        parY += (Mth.clamp(ndc[1], -1, 1) - parY) * (1 - Math.exp(-4 * dt));
        eye = eye.add(0, Math.sin(t * 1.3) * 0.0035, 0);
        yaw += (float) (Math.sin(t * 0.61) * 0.16 + parX * 1.1);
        pitch += (float) (Math.sin(t * 0.83 + 1.1) * 0.13 - parY * 0.8);
        // mimico: susto quando ele aparece e tremida forte na mordida
        if (mimicSeenAt > 0) {
            double s = t - mimicSeenAt;
            // tranco para tras e a cabeca sobe, olhando a boca abrir; tremedeira crescendo ate a mordida
            eye = eye.add(Vec3.atLowerCornerOf(facing.getNormal()).scale(0.14 * LapidaryCut.inOutCubic(s / 0.18)));
            pitch -= (float) (8 * LapidaryCut.inOutCubic(s / 0.25));
            double pre = biteAt > 0 && t < biteAt ? 0.25 + 0.75 * LapidaryCut.window(s, 0, BITE_AT) : 0;
            yaw += (float) ((Math.sin(t * 43) + Math.sin(t * 71 + 2)) * 0.6 * pre);
            pitch += (float) ((Math.sin(t * 53 + 1) + Math.sin(t * 31)) * 0.5 * pre);
        }
        if (biteAt > 0 && t >= biteAt) {
            double k = Math.max(0, 1 - (t - biteAt) / 0.7);
            k *= k;
            yaw += (float) ((Math.sin(t * 61) + Math.sin(t * 37 + 1)) * 4.5 * k);
            pitch += (float) ((Math.sin(t * 57 + 2) + Math.sin(t * 29)) * 3.6 * k);
            eye = eye.add(0, -0.08 * k, 0).add(Vec3.atLowerCornerOf(facing.getNormal()).scale(-0.1 * k));   // a boca puxa
        }
        return new Pose(eye, yaw, pitch);
    }

    @Nullable
    public static Pose override(Vec3 livePos, float liveYaw, float livePitch) {
        if (phase == Phase.NONE) { weight = 0; return null; }
        double now = clock();
        double w = switch (phase) {
            case INTRO -> LapidaryCut.inOutCubic((now - phaseAt) / LEAN);
            case ACTIVE -> 1;
            default -> now < holdUntil ? 1 : 1 - LapidaryCut.inOutCubic((now - holdUntil) / OUTRO);
        };
        weight = w;
        Pose s = scenePose();
        if (w <= 0.0001) return null;
        Vec3 p = livePos.add(s.pos().subtract(livePos).scale(w));
        float yaw = liveYaw + Mth.wrapDegrees(s.yaw() - liveYaw) * (float) w;
        float pitch = Mth.lerp((float) w, livePitch, s.pitch());
        return new Pose(p, yaw, pitch);
    }

    // zoom do susto: fecha rapido quando o mimico aparece e mais um pouco na mordida
    public static double fov(double vanilla) {
        double scene = 56.0;
        if (mimicSeenAt > 0) {
            double t = clock();
            scene *= 1 - 0.30 * LapidaryCut.inOutCubic((t - mimicSeenAt) / 0.2);
            if (biteAt > 0 && t >= biteAt) scene *= 1 - 0.12 * Math.max(0, 1 - (t - biteAt) / 0.4);
        }
        return Mth.lerp(weight, vanilla, scene);
    }

    // ---------------------------------------------------------------- menu
    @Nullable
    static ChestMenu menu() {
        LocalPlayer p = Minecraft.getInstance().player;
        return p != null && p.containerMenu instanceof ChestMenu m ? m : null;
    }

    static int chestSlots() { return rows * 9; }

    static ItemStack slot(int i) {
        ChestMenu m = menu();
        return m == null || i < 0 || i >= m.slots.size() ? ItemStack.EMPTY : m.getSlot(i).getItem();
    }

    static ItemStack carried() {
        ChestMenu m = menu();
        return m == null ? ItemStack.EMPTY : m.getCarried();
    }

    static void slotClick(int slot, int button, boolean shift) {
        Minecraft mc = Minecraft.getInstance();
        ChestMenu m = menu();
        if (m == null || mc.gameMode == null || mc.player == null) return;
        mc.gameMode.handleInventoryMouseClick(m.containerId, slot, button, shift ? ClickType.QUICK_MOVE : ClickType.PICKUP, mc.player);
        ChestHands.tap();
    }

    // clique dentro do bau: UM clique pega o item - a mao vai ate ele e traz para o inventario.
    // Botao esquerdo = a pilha toda; direito = metade.
    public static void click(int button, boolean shift) {
        if (phase != Phase.ACTIVE) return;
        ItemStack held = carried();
        if (!held.isEmpty()) {   // (sobrou algo na mao: poe na casa ou devolve)
            if (hoverSlot >= 0 && hoverSlot < chestSlots()) slotClick(hoverSlot, button, false);
            else giveBack();
            return;
        }
        if (hoverSlot < 0 || hoverSlot >= chestSlots()) return;
        ItemStack there = slot(hoverSlot);
        if (there.isEmpty()) return;
        if (button == 1 && there.getCount() > 1) {
            flying.add(new Flying(there.copyWithCount((there.getCount() + 1) / 2), cellPoint(hoverSlot), clock()));
            slotClick(hoverSlot, 1, false);   // pega metade...
            giveBack();                       // ...e guarda no inventario
        } else {
            flying.add(new Flying(there.copy(), cellPoint(hoverSlot), clock()));
            slotClick(hoverSlot, 0, true);
        }
        sound(SoundEvents.ITEM_PICKUP, 0.3f, 1.25f);
    }

    // clique na barra do inventario: UM clique guarda o item no bau (a mao leva ate a casa onde ele entrou)
    public static void barClick(int slot, int button, boolean shift) {
        if (phase != Phase.ACTIVE) return;
        if (!carried().isEmpty()) { slotClick(slot, button, false); lastSlot = slot; return; }
        ItemStack s = slot(slot);
        if (s.isEmpty()) return;
        int n = chestSlots();
        int[] before = new int[n];
        for (int i = 0; i < n; i++) before[i] = slot(i).getCount();
        slotClick(slot, 0, true);
        lastSlot = slot;
        int target = -1;
        for (int i = 0; i < n && target < 0; i++) if (slot(i).getCount() > before[i]) target = i;
        if (target < 0) { denyAt = clock(); return; }   // bau cheio
        placing.add(new Placing(s.copy(), target, clock()));
        sound(SoundEvents.BUNDLE_INSERT, 0.35f, 1.2f);
    }

    // posicao do item sendo guardado (da barra, embaixo da camera, ate a casa)
    static Vec3 placingAt(Placing pl, Vec3 cam, float yaw, double now) {
        double u = LapidaryCut.inOutCubic(LapidaryCut.window(now - pl.start(), 0, PLACE));
        Vec3 from = cam.add(LapidaryHands.forward(yaw).scale(0.4)).add(0, -0.3, 0);
        Vec3 to = cellPoint(pl.slot()).add(0, 0.02, 0);
        return from.add(to.subtract(from).scale(u)).add(0, 0.12 * LapidaryCut.hump(u), 0);
    }

    static void giveBack() {
        ChestMenu m = menu();
        if (m == null) return;
        ItemStack held = m.getCarried();
        int first = chestSlots(), target = -1;
        if (lastSlot >= first && lastSlot < m.slots.size()) {
            ItemStack there = m.getSlot(lastSlot).getItem();
            if (there.isEmpty() || ItemStack.isSameItemSameComponents(there, held)) target = lastSlot;
        }
        for (int i = first; i < m.slots.size() && target < 0; i++) {
            ItemStack there = m.getSlot(i).getItem();
            if (ItemStack.isSameItemSameComponents(there, held) && there.getCount() < there.getMaxStackSize()) target = i;
        }
        for (int i = first; i < m.slots.size() && target < 0; i++) if (m.getSlot(i).getItem().isEmpty()) target = i;
        if (target >= 0) slotClick(target, 0, false);
        else denyAt = clock();
    }

    // ---------------------------------------------------------------- dicas
    public static String hint() {
        if (clock() - denyAt < 1.2) return "chest.binosvanillaplus.hint.full";   // (inventario ou bau cheio)
        return carried().isEmpty() ? "chest.binosvanillaplus.hint.take" : "chest.binosvanillaplus.hint.place";
    }

    private static String hintKey = "";
    private static double hintAt;

    public static float hintAlpha(String key) {
        double now = clock();
        if (!key.equals(hintKey)) { hintKey = key; hintAt = now; }
        double life = key.endsWith(".full") ? 1.1 : 3.0;
        return (float) (1 - LapidaryCut.window(now - hintAt, life, life + 0.5));
    }

    public static float leaveAlpha() {
        if (activeAt < 0) return 0f;
        return (float) (1 - LapidaryCut.window(clock() - activeAt, 3.5, 4.2));
    }

    // ---------------------------------------------------------------- desenho no mundo (itens no bau, bracos)
    public static void render(PoseStack ps, MultiBufferSource buf, Camera camera) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !mc.options.getCameraType().isFirstPerson()) return;
        Vec3 cam = camera.getPosition();
        float yaw = camera.getYRot();
        int light = LevelRenderer.getLightColor(mc.level, pos.above());
        double now = clock();

        // itens dentro do bau (na saida, ficam ate a tampa comecar a fechar; se o bau sumiu, somem)
        if (phase == Phase.ACTIVE || phase == Phase.INTRO) {
            List<ItemStack> list = new ArrayList<>();
            for (int i = 0; i < chestSlots(); i++) list.add(slot(i));
            snapshot = list;
        }
        boolean showItems = phase != Phase.OUTRO || (!serverClosed && now - phaseAt < 0.2);
        List<Integer> occ = new ArrayList<>();
        for (int i = 0; i < snapshot.size(); i++) if (!snapshot.get(i).isEmpty()) occ.add(i);
        layout(occ);
        smooth();
        // luz minima dentro do bau (de noite ou na caverna os itens sumiam no escuro)
        int itemLight = LightTexture.pack(Math.max(LightTexture.block(light), 11), LightTexture.sky(light));
        if (showItems) {
            float camYaw = facing.getOpposite().toYRot();
            placing.removeIf(pl -> now - pl.start() > PLACE);
            for (int i = 0; i < snapshot.size(); i++) {
                ItemStack s = snapshot.get(i);
                final int slotI = i;
                if (s.isEmpty() || placing.stream().anyMatch(pl -> pl.slot() == slotI)) continue;
                boolean hov = i == hoverSlot && carried().isEmpty();
                Vec3 at = cellPoint(i).add(0, hov ? 0.035 + Math.sin(now * 5) * 0.006 : 0, 0);
                float k = (float) itemSize * (hov ? 1.12f : 1f);
                drawItem(ps, buf, s, at, cam, camYaw + itemYaw(i), -62f, k, itemLight);
            }
        }
        ChestHands.render(ps, buf, cam, yaw, light);
        for (Placing pl : placing) drawItem(ps, buf, pl.stack(), placingAt(pl, cam, yaw, now), cam, yaw, -30f, (float) itemSize, itemLight);

        // itens mandados para o inventario: voam ate a camera
        if (!flying.isEmpty()) {
            flying.removeIf(fl -> now - fl.start() > FLY);
            for (Flying fl : flying) {
                double u = LapidaryCut.window(now - fl.start(), 0, FLY);
                drawItem(ps, buf, fl.stack(), flyingAt(fl, cam, yaw, now), cam, yaw, -25f, (float) itemSize * (float) (1 - 0.5 * u), itemLight);
            }
        }
    }

    // item pego: espera a mao chegar (0,1 s) e vai com ela ate perto da camera
    static Vec3 flyingAt(Flying fl, Vec3 cam, float yaw, double now) {
        Vec3 to = cam.add(LapidaryHands.forward(yaw).scale(0.3)).add(0, -0.35, 0);
        double u = LapidaryCut.window(now - fl.start(), 0.1, FLY);
        return fl.from().add(to.subtract(fl.from()).scale(u * u)).add(0, 0.14 * LapidaryCut.hump(u), 0);
    }

    // Item com o mesmo "tamanho de icone" do inventario (contexto GUI: todo item, bloco ou modelo especial cabe num
    // quadrado de 1 unidade), virado para quem olha (faceYaw = yaw da camera). 'at' = ponto no chao embaixo dele.
    //  - item chato: deitado para tras (tilt), apoiado no chao;
    //  - bloco / modelo 3D: em pe e sem inclinar (antes os blocos deitavam e apareciam de ponta-cabeca e enormes).
    static void drawItem(PoseStack ps, MultiBufferSource buf, ItemStack stack, Vec3 at, Vec3 cam, float faceYaw, float tilt, float scale, int light) {
        Minecraft mc = Minecraft.getInstance();
        boolean block = mc.getItemRenderer().getModel(stack, mc.level, null, 0).isGui3d();
        ps.pushPose();
        ps.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
        ps.mulPose(Axis.YP.rotationDegrees(180f - faceYaw));
        if (block) {
            ps.translate(0, scale * 0.42, 0);
        } else {
            ps.translate(0, scale * 0.5 * Math.cos(Math.toRadians(Math.abs(tilt))) + 0.004, 0);
            ps.mulPose(Axis.XP.rotationDegrees(tilt));
        }
        ps.scale(scale, scale, scale * (block ? 1 : 0.6f));
        mc.getItemRenderer().renderStatic(stack, ItemDisplayContext.GUI, light, OverlayTexture.NO_OVERLAY, ps, buf, mc.level, 0);
        ps.popPose();
    }

    static void sound(SoundEvent ev, float vol, float pitch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) mc.level.playLocalSound(center.x, center.y + 0.6, center.z, ev, SoundSource.BLOCKS, vol, pitch, false);
    }
}
