package com.bino.binosvanillaplus.client.bench;

import com.bino.binosvanillaplus.client.chest.scene.ChestScene;
import com.bino.binosvanillaplus.client.lapidary.LapidaryCut;
import com.bino.binosvanillaplus.client.lapidary.LapidaryHands;
import com.bino.binosvanillaplus.client.lapidary.LapidaryPick;
import com.bino.binosvanillaplus.client.lapidary.LapidaryRemote;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

// Cena "estilo cinema" da Bancada de Trabalho e da Bigorna (mesmo sistema do bau e da Mesa de Lapidacao).
//  BANCADA: grade 3x3 desenhada no tampo; o resultado flutua acima da bancada, girando.
//    - Clique num item da barra = ele vai para a mao. Com o item na mao: clique numa casa = poe a pilha toda;
//      botao direito = poe um so (da para ir clicando casa por casa). Botao direito fora = devolve o item.
//    - Clique num item da grade (mao vazia) = volta para o inventario.
//    - Clique no resultado = fabrica um (vai para o inventario); Shift = fabrica tudo que der.
//  BIGORNA: item da esquerda e da direita deitados no topo; o resultado flutua acima.
//    - Clique num item da barra = vai direto para a bigorna (esquerda, depois direita). Clique no item da bigorna =
//      volta para o inventario.
//    - O jogador fica do lado comprido; o item fica DEITADO no meio do topo e o material na ponta direita; o
//      resultado aparece flutuando sobre a ponta esquerda assim que da para fazer.
//    - Clique no resultado = a mao pega o material e raspa por cima do item 3 vezes (faiscas); no fim o item sai.
//      Custo em niveis aparece em cima do resultado (vermelho = sem niveis / caro demais). Campo de nome no alto.
//  Sair: clicar fora do bloco ou Esc. Abrir agachado = janela classica.
public final class BenchScene {
    private BenchScene() {}

    public enum Kind { CRAFTING, ANVIL }
    public enum Phase { NONE, INTRO, ACTIVE, OUTRO }

    public static Phase phase = Phase.NONE;
    public static Kind kind = Kind.CRAFTING;
    static BlockPos pos = BlockPos.ZERO;
    static Direction side = Direction.NORTH;   // lado do bloco onde o jogador esta
    static Vec3 center = Vec3.ZERO;
    static double phaseAt, activeAt = -100, denyAt = -100, strikeAt = -100;
    static boolean leaving, strikeAll;
    static int lastPass;
    static final double SCRAPE_START = 0.22;   // a mao pega o material antes de raspar
    static final int SCRAPES = 3;
    @Nullable static CameraType savedCamera;
    @Nullable static BlockPos lastClicked;
    private static double clickedAt = -100;

    public static int hover = -1;          // slot do menu sob o mouse (-1 = nada)
    public static boolean overBar;
    public static double weight;

    record Flying(ItemStack stack, Vec3 from, double start) {}
    static final List<Flying> flying = new ArrayList<>();
    record Placing(ItemStack stack, int slot, double start) {}
    static final List<Placing> placing = new ArrayList<>();
    static final double LEAN = 0.75, OUTRO = 0.6, FLY = 0.42, PLACE = 0.34, STRIKE = 1.25;   // STRIKE = tempo da raspada na bigorna
    static final double TOP = 1.0;   // altura do tampo (bancada e bigorna)

    public static double clock() { return LapidaryRemote.clock(); }
    public static boolean active() { return phase != Phase.NONE; }
    public static void clicked(BlockPos p) { lastClicked = p.immutable(); clickedAt = clock(); }

    // ---------------------------------------------------------------- slots de cada menu
    static int resultSlot() { return kind == Kind.CRAFTING ? 0 : 2; }
    static int invStart() { return kind == Kind.CRAFTING ? 10 : 3; }
    static boolean isInput(int slot) { return kind == Kind.CRAFTING ? slot >= 1 && slot <= 9 : slot == 0 || slot == 1; }

    static boolean blockOk(BlockState st) {
        return kind == Kind.CRAFTING ? st.getBlock() instanceof CraftingTableBlock && st.is(Blocks.CRAFTING_TABLE)
                : st.getBlock() instanceof AnvilBlock;
    }

    // ---------------------------------------------------------------- inicio e fim
    public static boolean begin(AbstractContainerMenu menu) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || phase != Phase.NONE || player.isPassenger() || player.isShiftKeyDown()) return false;
        if (menu instanceof CraftingMenu) kind = Kind.CRAFTING;
        else if (menu instanceof AnvilMenu) kind = Kind.ANVIL;
        else return false;
        BlockPos at = null;
        if (mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK && blockOk(mc.level.getBlockState(hit.getBlockPos())))
            at = hit.getBlockPos();
        if (at == null && lastClicked != null && clock() - clickedAt < 1.0 && blockOk(mc.level.getBlockState(lastClicked))) at = lastClicked;
        if (at == null) return false;
        BlockState st = mc.level.getBlockState(at);
        pos = at.immutable();
        center = new Vec3(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        double dx = player.getX() - center.x, dz = player.getZ() - center.z;
        if (kind == Kind.ANVIL) {
            // o topo da bigorna e' comprido de lado: o jogador fica de frente para o lado comprido
            Direction f = st.getValue(AnvilBlock.FACING).getClockWise();
            side = (f.getStepX() * dx + f.getStepZ() * dz) >= 0 ? f : f.getOpposite();
        } else side = Direction.getNearest(dx, 0, dz);
        if (side.getAxis() == Direction.Axis.Y) side = Direction.NORTH;

        phase = Phase.INTRO;
        phaseAt = clock();
        leaving = false;
        activeAt = denyAt = strikeAt = -100;
        hover = -1;
        overBar = false;
        flying.clear();
        placing.clear();
        savedCamera = mc.options.getCameraType();
        if (!savedCamera.isFirstPerson()) mc.options.setCameraType(CameraType.FIRST_PERSON);
        BenchHands.begin();
        return true;
    }

    public static void leave() {
        if (phase != Phase.INTRO && phase != Phase.ACTIVE) return;
        Minecraft mc = Minecraft.getInstance();
        leaving = true;
        startOutro();
        if (mc.player != null && menu() != null) {
            mc.player.connection.send(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
            mc.player.containerMenu = mc.player.inventoryMenu;
        }
        if (mc.screen instanceof BenchSceneScreen) mc.setScreen(null);
    }

    public static void screenClosed() {
        if (leaving || phase == Phase.NONE || phase == Phase.OUTRO) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && menu() != null) {
            mc.player.connection.send(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
            mc.player.containerMenu = mc.player.inventoryMenu;
        }
        startOutro();
    }

    private static void startOutro() { phase = Phase.OUTRO; phaseAt = clock(); }

    private static void finish() {
        Minecraft mc = Minecraft.getInstance();
        phase = Phase.NONE;
        leaving = false;
        flying.clear();
        placing.clear();
        if (savedCamera != null && !savedCamera.isFirstPerson()) mc.options.setCameraType(savedCamera);
        savedCamera = null;
    }

    public static void abort() { phase = Phase.NONE; leaving = false; flying.clear(); placing.clear(); }

    public static void tick() {
        if (phase == Phase.NONE) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) { abort(); return; }
        double now = clock();
        if (phase == Phase.OUTRO) { if (now - phaseAt >= OUTRO) finish(); return; }
        if (menu() == null) return;   // screenClosed cuida
        if (phase == Phase.INTRO && now - phaseAt >= LEAN) {
            phase = Phase.ACTIVE;
            phaseAt = activeAt = now;
            float yaw = side.getOpposite().toYRot();
            player.setYRot(yaw);
            player.yHeadRot = yaw;
            player.yBodyRot = yaw;
            player.setXRot(45f);
        }
        // raspada da bigorna: a mao passa o material por cima do item 3 vezes (faisca e som a cada passada)
        if (strikeAt > 0) {
            double t = now - strikeAt;
            int pass = (int) Math.floor(Math.max(0, t - SCRAPE_START) / ((STRIKE * 0.88 - SCRAPE_START) / SCRAPES));
            if (t >= SCRAPE_START && pass > lastPass && pass <= SCRAPES) {
                lastPass = pass;
                sparks();
                sound(SoundEvents.GRINDSTONE_USE, 0.35f, 1.4f + pass * 0.08f);
            }
            if (t >= STRIKE * 0.9) {
                sparks();
                ItemStack r = slot(resultSlot());
                if (!r.isEmpty()) flying.add(new Flying(r.copy(), resultPoint(), now));
                slotClick(resultSlot(), 0, true);
                strikeAt = -100;
            }
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

    // ---------------------------------------------------------------- geometria (x = direita de quem olha, z = para o jogador)
    static Vec3 local(double x, double y, double z) {
        Vec3 f = Vec3.atLowerCornerOf(side.getNormal());
        Vec3 r = Vec3.atLowerCornerOf(side.getCounterClockWise().getNormal());
        return center.add(r.scale(x)).add(0, y, 0).add(f.scale(z));
    }

    static double[] toLocal(Vec3 w) {
        Vec3 d = w.subtract(center);
        Vec3 f = Vec3.atLowerCornerOf(side.getNormal());
        Vec3 r = Vec3.atLowerCornerOf(side.getCounterClockWise().getNormal());
        return new double[]{d.dot(r), d.dot(f)};
    }

    static final double CELL = 0.25;

    // onde fica o item de um slot de entrada (no tampo)
    static Vec3 slotPoint(int slot) {
        if (kind == Kind.CRAFTING) {
            int i = slot - 1, r = i / 3, c = i % 3;
            return local((c - 1) * CELL, TOP + 0.002, (r - 1) * CELL);
        }
        // bigorna: o item deitado no meio do topo; o material descansa na ponta direita
        return slot == 0 ? local(-0.04, TOP + 0.004, 0.02) : local(0.36, TOP + 0.004, 0.02);
    }

    // posicao da mao/material durante a raspada (vai e volta por cima do item)
    static Vec3 scrapePoint(double t) {
        if (t < SCRAPE_START) return slotPoint(1).add(0, 0.05, 0);
        double u = (t - SCRAPE_START) / (STRIKE * 0.88 - SCRAPE_START);
        double ph = Mth.clamp(u, 0, 1) * SCRAPES;
        double tri = 1 - Math.abs((ph % 1.0) * 2 - 1);     // 0 -> 1 -> 0 em cada passada
        return local(-0.26 + 0.42 * tri, TOP + 0.045 + 0.012 * Math.sin(ph * Math.PI * 2), 0.02);
    }

    // resultado: flutua acima do bloco, um pouco para tras
    static Vec3 resultPoint() {
        double t = clock();
        // atras do tampo, baixinho (antes ficava alto demais e saia da tela)
        // bancada: atras da grade; bigorna: flutuando por cima da ponta esquerda (aparece assim que da para fazer)
        return kind == Kind.CRAFTING ? local(0, TOP + 0.2 + Math.sin(t * 2.2) * 0.01, -0.47)
                : local(-0.42, TOP + 0.24 + Math.sin(t * 2.2) * 0.01, -0.02);
    }

    @Nullable
    static double[] onTop() {
        Vec3[] ray = LapidaryPick.ray();
        if (ray == null || Math.abs(ray[1].y) < 1e-4) return null;
        double y = center.y + TOP;
        double k = (y - ray[0].y) / ray[1].y;
        if (k <= 0) return null;
        return toLocal(ray[0].add(ray[1].scale(k)));
    }

    public static void updateHover() {
        hover = -1;
        if (phase != Phase.ACTIVE || strikeAt > 0) return;
        Vec3[] ray = LapidaryPick.ray();
        if (ray == null) return;
        // resultado (bolha em volta dele)
        if (!slot(resultSlot()).isEmpty()) {
            Vec3 to = resultPoint().subtract(ray[0]);
            double along = to.dot(ray[1]);
            if (along > 0 && to.subtract(ray[1].scale(along)).length() < 0.17) { hover = resultSlot(); return; }
        }
        double[] p = onTop();
        if (p == null) return;
        if (kind == Kind.CRAFTING) {
            int c = (int) Math.floor((p[0] + 1.5 * CELL) / CELL), r = (int) Math.floor((p[1] + 1.5 * CELL) / CELL);
            if (c >= 0 && c < 3 && r >= 0 && r < 3) hover = 1 + r * 3 + c;
        } else if (Math.abs(p[0]) < 0.52 && Math.abs(p[1]) < 0.34) hover = p[0] < 0.25 ? 0 : 1;
    }

    // fora do bloco (para sair clicando fora)
    public static boolean outside() {
        if (phase != Phase.ACTIVE || hover >= 0 || !carried().isEmpty() || strikeAt > 0) return false;
        double[] p = onTop();
        double hx = 0.6, hz = kind == Kind.ANVIL ? 0.42 : 0.6;
        if (p == null) return LapidaryPick.mouseNdc()[1] < 0.35;   // olhando para o alto (camera levantada) nao sai
        return Math.abs(p[0]) > hx || Math.abs(p[1]) > hz;
    }

    // ---------------------------------------------------------------- camera
    public record Pose(Vec3 pos, float yaw, float pitch) {}
    private static double parX, parY, lastFrame, focusW, lookUp;
    @Nullable private static Vec3 lastFocus;

    static Pose scenePose() {
        Vec3 eye = local(0, TOP + 0.86, 0.72);
        Vec3 lookAt = local(0, TOP, -0.02);
        double t = clock(), dt = Mth.clamp(t - lastFrame, 0, 0.1);
        Vec3 focus = hover >= 0 && phase == Phase.ACTIVE ? (hover == resultSlot() ? resultPoint() : slotPoint(hover)) : null;
        focusW += ((focus != null ? 1 : 0) - focusW) * (1 - Math.exp(-5 * dt));
        if (focus != null) lastFocus = focus;
        if (lastFocus != null && focusW > 0.001) lookAt = lookAt.add(lastFocus.subtract(lookAt).scale(0.15 * focusW));
        Vec3 look = lookAt.subtract(eye);
        float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(look.y, Math.hypot(look.x, look.z)));
        lastFrame = t;
        double[] ndc = phase == Phase.ACTIVE ? LapidaryPick.mouseNdc() : new double[]{0, 0};
        parX += (Mth.clamp(ndc[0], -1, 1) - parX) * (1 - Math.exp(-4 * dt));
        parY += (Mth.clamp(ndc[1], -1, 1) - parY) * (1 - Math.exp(-4 * dt));
        eye = eye.add(0, Math.sin(t * 1.3) * 0.0035, 0);
        yaw += (float) (Math.sin(t * 0.61) * 0.16 + parX * 1.1);
        pitch += (float) (Math.sin(t * 0.83 + 1.1) * 0.13 - parY * 0.8);
        // mouse perto do alto da tela: a camera levanta e olha para cima (ate ~32 graus)
        lookUp += ((phase == Phase.ACTIVE ? Mth.clamp((ndc[1] - 0.45) / 0.5, 0, 1) : 0) - lookUp) * (1 - Math.exp(-3.5 * dt));
        pitch -= (float) (32 * LapidaryCut.inOutCubic(lookUp));
        // vibracao leve durante a raspada
        if (strikeAt > 0 && t - strikeAt > SCRAPE_START) {
            double k = 1 - LapidaryCut.window(t - strikeAt, STRIKE * 0.85, STRIKE);
            yaw += (float) (Math.sin(t * 47) * 0.18 * k);
            pitch += (float) (Math.sin(t * 53 + 1) * 0.15 * k);
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
            default -> 1 - LapidaryCut.inOutCubic((now - phaseAt) / OUTRO);
        };
        weight = w;
        Pose s = scenePose();
        if (w <= 0.0001) return null;
        Vec3 p = livePos.add(s.pos().subtract(livePos).scale(w));
        float yaw = liveYaw + Mth.wrapDegrees(s.yaw() - liveYaw) * (float) w;
        float pitch = Mth.lerp((float) w, livePitch, s.pitch());
        return new Pose(p, yaw, pitch);
    }

    public static double fov(double vanilla) { return Mth.lerp(weight, vanilla, 56.0); }

    // ---------------------------------------------------------------- menu
    @Nullable
    static AbstractContainerMenu menu() {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return null;
        AbstractContainerMenu m = p.containerMenu;
        return kind == Kind.CRAFTING ? (m instanceof CraftingMenu ? m : null) : (m instanceof AnvilMenu ? m : null);
    }

    static ItemStack slot(int i) {
        AbstractContainerMenu m = menu();
        return m == null || i < 0 || i >= m.slots.size() ? ItemStack.EMPTY : m.getSlot(i).getItem();
    }

    static int slotCount() { AbstractContainerMenu m = menu(); return m == null ? 0 : m.slots.size(); }

    public static ItemStack carried() {
        AbstractContainerMenu m = menu();
        return m == null ? ItemStack.EMPTY : m.getCarried();
    }

    static void slotClick(int slot, int button, boolean shift) {
        Minecraft mc = Minecraft.getInstance();
        AbstractContainerMenu m = menu();
        if (m == null || mc.gameMode == null || mc.player == null) return;
        mc.gameMode.handleInventoryMouseClick(m.containerId, slot, button, shift ? ClickType.QUICK_MOVE : ClickType.PICKUP, mc.player);
        BenchHands.tap();
    }

    // custo da bigorna (0 = sem custo / sem resultado)
    public static int cost() { return menu() instanceof AnvilMenu a ? a.getCost() : 0; }

    public static boolean canTake() {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null || slot(resultSlot()).isEmpty()) return false;
        if (kind == Kind.CRAFTING) return true;
        int c = cost();
        return p.getAbilities().instabuild || (c > 0 && c < 40 && p.experienceLevel >= c);
    }

    public static void rename(String name) {
        if (menu() instanceof AnvilMenu a && a.setItemName(name)) {
            LocalPlayer p = Minecraft.getInstance().player;
            if (p != null) p.connection.send(new ServerboundRenameItemPacket(name));
        }
    }

    // ---------------------------------------------------------------- cliques
    public static void click(int button, boolean shift) {
        if (phase != Phase.ACTIVE || strikeAt > 0) return;
        ItemStack held = carried();
        int rs = resultSlot();
        if (hover == rs) {
            if (!held.isEmpty()) giveBack();   // algo na mao: guarda antes de fabricar
            if (!carried().isEmpty() || !canTake()) { deny(); return; }
            if (kind == Kind.ANVIL) { strikeAt = clock(); lastPass = 0; strikeAll = shift; sound(SoundEvents.ARMOR_EQUIP_IRON.value(), 0.4f, 0.8f); return; }
            ItemStack r = slot(rs);
            flying.add(new Flying(r.copy(), resultPoint(), clock()));
            if (shift) slotClick(rs, 0, true);               // fabrica tudo
            else { slotClick(rs, 0, false); giveBack(); }      // fabrica um e guarda
            sound(SoundEvents.VILLAGER_WORK_TOOLSMITH, 0.5f, 1.3f);
            sound(SoundEvents.ITEM_PICKUP, 0.3f, 1.2f);
            return;
        }
        if (hover >= 0 && isInput(hover)) {
            if (!held.isEmpty()) {   // pondo da mao (bancada): esquerdo = pilha, direito = um
                slotClick(hover, button, false);
                sound(SoundEvents.WOOD_HIT, 0.35f, 1.5f);
                return;
            }
            ItemStack there = slot(hover);
            if (there.isEmpty()) return;
            flying.add(new Flying(there.copy(), slotPoint(hover), clock()));
            slotClick(hover, 0, true);
            sound(SoundEvents.ITEM_PICKUP, 0.3f, 1.25f);
            return;
        }
        if (button == 1 && !held.isEmpty()) giveBack();
    }

    // clique num item da barra: bancada = vai para a mao; bigorna = vai direto para a bigorna
    public static void barClick(int slot, int button, boolean shift) {
        if (phase != Phase.ACTIVE || strikeAt > 0) return;
        ItemStack s = slot(slot);
        if (kind == Kind.CRAFTING || !carried().isEmpty()) {
            slotClick(slot, button, false);
            sound(SoundEvents.ITEM_PICKUP, 0.25f, 1.4f);
            return;
        }
        if (s.isEmpty()) return;
        ItemStack a = slot(0), b = slot(1);
        slotClick(slot, 0, true);
        int target = !ItemStack.matches(a, slot(0)) ? 0 : !ItemStack.matches(b, slot(1)) ? 1 : -1;
        if (target < 0) { deny(); return; }
        placing.add(new Placing(s.copy(), target, clock()));
        sound(SoundEvents.ANVIL_STEP, 0.4f, 1.3f);
    }

    static void giveBack() {
        AbstractContainerMenu m = menu();
        if (m == null) return;
        ItemStack held = m.getCarried();
        int target = -1;
        for (int i = invStart(); i < m.slots.size() && target < 0; i++) {
            ItemStack there = m.getSlot(i).getItem();
            if (ItemStack.isSameItemSameComponents(there, held) && there.getCount() < there.getMaxStackSize()) target = i;
        }
        for (int i = invStart(); i < m.slots.size() && target < 0; i++) if (m.getSlot(i).getItem().isEmpty()) target = i;
        if (target >= 0) slotClick(target, 0, false);
        else deny();
    }

    static void deny() { denyAt = clock(); sound(SoundEvents.NOTE_BLOCK_BASS.value(), 0.4f, 0.6f); }

    // ---------------------------------------------------------------- dicas
    public static String hint() {
        String k = kind == Kind.CRAFTING ? "crafting" : "anvil";
        if (clock() - denyAt < 1.2) return "bench.binosvanillaplus." + k + ".no";
        if (!carried().isEmpty()) return "bench.binosvanillaplus.crafting.place";
        if (!slot(resultSlot()).isEmpty()) return "bench.binosvanillaplus." + k + ".take";
        return "bench.binosvanillaplus." + k + ".start";
    }

    private static String hintKey = "";
    private static double hintAt;

    public static float hintAlpha(String key) {
        double now = clock();
        if (!key.equals(hintKey)) { hintKey = key; hintAt = now; }
        double life = key.endsWith(".no") ? 1.1 : 3.0;
        return (float) (1 - LapidaryCut.window(now - hintAt, life, life + 0.5));
    }

    public static float leaveAlpha() {
        if (activeAt < 0) return 0f;
        return (float) (1 - LapidaryCut.window(clock() - activeAt, 3.5, 4.2));
    }

    // ---------------------------------------------------------------- desenho no mundo
    public static void render(PoseStack ps, MultiBufferSource buf, Camera camera) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !mc.options.getCameraType().isFirstPerson()) return;
        Vec3 cam = camera.getPosition();
        float yaw = camera.getYRot();
        int light = LevelRenderer.getLightColor(mc.level, pos.above());
        int itemLight = LightTexture.pack(Math.max(LightTexture.block(light), 11), LightTexture.sky(light));
        double now = clock();
        float faceYaw = side.getOpposite().toYRot();
        boolean show = phase != Phase.OUTRO || now - phaseAt < 0.25;

        if (show && phase != Phase.NONE) {
            placing.removeIf(pl -> now - pl.start() > PLACE);
            // casas da bancada: quadradinhos claros no tampo (a casa sob o mouse brilha mais)
            if (kind == Kind.CRAFTING && phase == Phase.ACTIVE) {
                VertexConsumer vc = buf.getBuffer(RenderType.debugQuads());
                for (int s = 1; s <= 9; s++) {
                    boolean h = s == hover;
                    int a = h ? 90 : carried().isEmpty() ? 22 : 45;
                    quad(vc, ps.last().pose(), cam, slotPoint(s).add(0, 0.003, 0), CELL * 0.44, a);
                }
            }
            for (int s = 0; s < slotCount(); s++) {
                if (!isInput(s)) continue;
                ItemStack st = slot(s);
                final int si = s;
                if (st.isEmpty() || placing.stream().anyMatch(pl -> pl.slot() == si)) continue;
                if (kind == Kind.ANVIL && s == 1 && strikeAt > 0) continue;   // o material esta na mao
                boolean h = s == hover && carried().isEmpty();
                // icone do inventario, deitado e bem maior (na bigorna o item fica ao longo dela)
                float k = (kind == Kind.CRAFTING ? 0.2f : s == 0 ? 0.46f : 0.28f) * (h ? 1.08f : 1f);
                Vec3 at = slotPoint(s).add(0, h ? 0.02 + Math.sin(now * 5) * 0.004 : 0, 0);
                float iy = kind == Kind.ANVIL && s == 0 ? faceYaw - 45f : faceYaw + (s * 29 % 9 - 4);
                drawIcon(ps, buf, st, at, cam, iy, -90f, k, itemLight);
            }
            // resultado girando, com um leve "pulo" quando o mouse esta em cima
            ItemStack r = slot(resultSlot());
            if (!r.isEmpty() && !(kind == Kind.ANVIL && strikeAt > 0)) {
                // contexto GUI: o item fica CENTRADO no ponto (o mesmo ponto que o mouse usa para clicar)
                Vec3 at = resultPoint();
                float k = (kind == Kind.ANVIL ? 0.3f : 0.26f) * (hover == resultSlot() ? 1.15f : 1f);
                ps.pushPose();
                ps.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
                ps.mulPose(Axis.YP.rotationDegrees((float) (now * 70 % 360)));
                ps.scale(k, k, k);
                int lr = canTake() ? LightTexture.FULL_BRIGHT : itemLight;
                mc.getItemRenderer().renderStatic(r, ItemDisplayContext.GUI, lr, OverlayTexture.NO_OVERLAY, ps, buf, mc.level, 0);
                ps.popPose();
            }
        }
        BenchHands.render(ps, buf, cam, yaw, light);
        for (Placing pl : placing) drawIcon(ps, buf, pl.stack(), placingAt(pl, cam, yaw, now), cam, yaw, -30f, 0.2f, itemLight);
        if (!flying.isEmpty()) {
            flying.removeIf(fl -> now - fl.start() > FLY);
            for (Flying fl : flying) {
                double u = LapidaryCut.window(now - fl.start(), 0, FLY);
                drawIcon(ps, buf, fl.stack(), flyingAt(fl, cam, yaw, now), cam, yaw, -25f, 0.2f * (float) (1 - 0.5 * u), itemLight);
            }
        }
    }

    public static int boostLight(int light) { return LightTexture.pack(Math.max(LightTexture.block(light), 11), LightTexture.sky(light)); }

    // Item com o visual do INVENTARIO (contexto GUI: o icone 2D, mesmo de ferramentas que tem modelo 3D na mao).
    // 'size' = lado do icone em blocos; 'at' = ponto no tampo (ou onde ele flutua). Itens chatos deitam com o angulo
    // 'tilt' (-90 = deitado no tampo); blocos ficam em pe como miniatura.
    static void drawIcon(PoseStack ps, MultiBufferSource buf, ItemStack stack, Vec3 at, Vec3 cam, float faceYaw, float tilt, float size, int light) {
        Minecraft mc = Minecraft.getInstance();
        boolean block = stack.getItem() instanceof net.minecraft.world.item.BlockItem
                && mc.getItemRenderer().getModel(stack, mc.level, null, 0).isGui3d();
        ps.pushPose();
        ps.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
        ps.mulPose(Axis.YP.rotationDegrees(180f - faceYaw));
        if (block) {
            ps.translate(0, size * 0.4, 0);
            size *= 0.85f;
        } else {
            ps.translate(0, 0.004, 0);
            ps.mulPose(Axis.XP.rotationDegrees(tilt));
        }
        ps.scale(size, size, block ? size : size * 0.5f);
        mc.getItemRenderer().renderStatic(stack, ItemDisplayContext.GUI, light, OverlayTexture.NO_OVERLAY, ps, buf, mc.level, 0);
        ps.popPose();
    }

    private static void quad(VertexConsumer vc, Matrix4f m, Vec3 cam, Vec3 c, double h, int alpha) {
        Vec3 f = Vec3.atLowerCornerOf(side.getNormal()).scale(h), r = Vec3.atLowerCornerOf(side.getCounterClockWise().getNormal()).scale(h);
        Vec3[] pts = {c.subtract(r).subtract(f), c.add(r).subtract(f), c.add(r).add(f), c.subtract(r).add(f)};
        for (Vec3 p : pts) vc.addVertex(m, (float) (p.x - cam.x), (float) (p.y - cam.y), (float) (p.z - cam.z)).setColor(255, 255, 255, alpha);
    }

    static Vec3 flyingAt(Flying fl, Vec3 cam, float yaw, double now) {
        Vec3 to = cam.add(LapidaryHands.forward(yaw).scale(0.3)).add(0, -0.35, 0);
        double u = LapidaryCut.window(now - fl.start(), 0.1, FLY);
        return fl.from().add(to.subtract(fl.from()).scale(u * u)).add(0, 0.14 * LapidaryCut.hump(u), 0);
    }

    static Vec3 placingAt(Placing pl, Vec3 cam, float yaw, double now) {
        double u = LapidaryCut.inOutCubic(LapidaryCut.window(now - pl.start(), 0, PLACE));
        Vec3 from = cam.add(LapidaryHands.forward(yaw).scale(0.4)).add(0, -0.3, 0);
        Vec3 to = slotPoint(pl.slot()).add(0, 0.02, 0);
        return from.add(to.subtract(from).scale(u)).add(0, 0.12 * LapidaryCut.hump(u), 0);
    }

    static void sparks() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Vec3 c = strikeAt > 0 ? scrapePoint(clock() - strikeAt) : local(0, TOP + 0.02, 0);
        for (int i = 0; i < 14; i++) {
            double vx = (mc.level.random.nextDouble() - 0.5) * 0.25, vy = 0.12 + mc.level.random.nextDouble() * 0.18, vz = (mc.level.random.nextDouble() - 0.5) * 0.25;
            mc.level.addParticle(i % 3 == 0 ? ParticleTypes.LAVA : ParticleTypes.ELECTRIC_SPARK, c.x, c.y, c.z, vx, vy, vz);
        }
    }

    static void sound(SoundEvent ev, float vol, float pitch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) mc.level.playLocalSound(center.x, center.y + 1, center.z, ev, SoundSource.BLOCKS, vol, pitch, false);
    }
}
