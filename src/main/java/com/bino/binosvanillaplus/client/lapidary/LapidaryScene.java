package com.bino.binosvanillaplus.client.lapidary;

import com.bino.binosvanillaplus.block.ModBlocks;
import com.bino.binosvanillaplus.menu.LapidaryMenu;
import com.bino.binosvanillaplus.network.LapidaryCutPayload;
import com.bino.binosvanillaplus.sound.ModSounds;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

// Cena em primeira pessoa da Mesa de Lapidacao (no lugar da janela). Ideia e ritmo inspirados no mod Cinematic
// Interactions (MIT); codigo proprio.
//  - O servidor abre o menu normal (LapidaryMenu). No cliente, a janela e' trocada pela LapidarySceneScreen
//    (invisivel: so a barra de itens e as dicas) e todo clique vira clique de slot do menu (o servidor manda nos itens).
//  - Entrada: o jogador anda ate a frente da mesa, a camera desliza ate olhar o tampo (CameraLapidaryMixin).
//  - Barra no alto: silex e minerios brutos do inventario (silex primeiro). Clicar pega o item (fica na mao).
//    Silhueta branca mostra onde por: silex na ponta do lapidador, minerio no centro do disco; com os dois, o visor
//    acende e a silhueta vai para o botao. Botao = lapidar uma; Shift + botao = lapidar tudo.
//  - Animacao: ver LapidaryCut. Saida: Esc (os itens que estavam na mesa "voltam" para a camera).
public final class LapidaryScene {
    private LapidaryScene() {}

    public enum Phase { NONE, INTRO, ACTIVE, OUTRO }
    public enum Target { NONE, FLINT, ORE, BUTTON, PILE }

    public static Phase phase = Phase.NONE;
    static BlockPos pos = BlockPos.ZERO;
    static Direction facing = Direction.WEST;
    static double phaseAt, leanFrom = -1, walkUntil;
    static boolean walking, leaving;
    @Nullable static CameraType savedCamera;
    @Nullable static BlockPos lastClicked;

    // lapidacao em andamento
    static double cutAt = -100;
    static boolean cutAll, beat, contactPlayed, donePlayed;
    static ItemStack cutFlint = ItemStack.EMPTY, cutOre = ItemStack.EMPTY;
    @Nullable static SpinSound spin;
    static final List<ItemStack> pile = new ArrayList<>();
    static boolean wasReady;
    static int lastSlot = -1;
    static double denyAt = -100;
    static String hintKey = "";
    static double hintAt = -100, activeAt = -100;

    public static Target hover = Target.NONE;
    // o mouse esta sobre a barra de itens (a mao direita sobe ate la)
    public static boolean overBar;

    // gemas que o jogador acabou de recolher da mesa e que voam para o inventario
    record Flying(ItemStack stack, Vec3 from, double start) {}
    static final List<Flying> flying = new ArrayList<>();
    static final double FLY = 0.45;

    // itens que estavam na mesa ao sair (voam para a camera)
    record Leaving(ItemStack stack, Vec3 from, float size) {}
    static List<Leaving> leavingItems = List.of();

    public static double clock() { return LapidaryRemote.clock(); }
    public static boolean active() { return phase != Phase.NONE; }

    // ---------------------------------------------------------------- inicio e fim
    public static void clicked(BlockPos p) { lastClicked = p.immutable(); }

    // a janela da mesa ia abrir: vira cena. false = deixa a janela normal
    public static boolean begin() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || phase != Phase.NONE || !(player.containerMenu instanceof LapidaryMenu)) return false;
        BlockPos at = null;
        if (mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                && mc.level.getBlockState(hit.getBlockPos()).is(ModBlocks.LAPIDARY_TABLE.get())) at = hit.getBlockPos();
        if (at == null && lastClicked != null && mc.level.getBlockState(lastClicked).is(ModBlocks.LAPIDARY_TABLE.get())) at = lastClicked;
        if (at == null || player.isPassenger()) return false;

        pos = at.immutable();
        facing = LapidaryGeo.facing(mc.level.getBlockState(pos));
        phase = Phase.INTRO;
        phaseAt = clock();
        leaving = false;
        cutAt = -100;
        pile.clear();
        flying.clear();
        lastSlot = -1;
        hintKey = "";
        hintAt = -100;
        activeAt = -100;
        wasReady = ready();
        hover = Target.NONE;
        overBar = false;
        savedCamera = mc.options.getCameraType();
        if (!savedCamera.isFirstPerson()) mc.options.setCameraType(CameraType.FIRST_PERSON);
        Vec3 stand = standPoint(player);
        double d = Math.hypot(stand.x - player.getX(), stand.z - player.getZ());
        walking = d > 0.25 && d < 4.0;
        walkUntil = phaseAt + (walking ? Math.min(1.6, 0.35 + d * 0.45) : 0);
        leanFrom = walking ? -1 : phaseAt;
        mc.level.getBlockEntity(pos);   // mesas antigas (de antes da cena) ganham o block entity aqui
        LapidaryHands.begin();
        sound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 0.35f, 1.1f);
        return true;
    }

    // Esc: sai da cena (devolve o menu ao servidor; os itens que estavam na mesa voltam ao inventario)
    public static void leave() {
        if (phase != Phase.INTRO && phase != Phase.ACTIVE) return;
        Minecraft mc = Minecraft.getInstance();
        LapidaryView v = localView();
        List<Leaving> out = new ArrayList<>();
        if (!v.flint().isEmpty()) out.add(new Leaving(v.flint(), point(LapidaryGeo.FLINT), 0.17f));
        if (!v.disc().isEmpty()) out.add(new Leaving(v.disc(), point(LapidaryGeo.ORE), 0.26f));
        for (int i = 0; i < Math.min(3, pile.size()); i++) out.add(new Leaving(pile.get(i), point(LapidaryGeo.GEMS), 0.19f));
        leavingItems = out;
        startOutro();
        if (mc.player != null && mc.player.containerMenu instanceof LapidaryMenu menu) {
            mc.player.connection.send(new ServerboundContainerClosePacket(menu.containerId));
            mc.player.containerMenu = mc.player.inventoryMenu;
        }
        if (mc.screen instanceof LapidarySceneScreen) mc.setScreen(null);
        sound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 0.3f, 0.9f);
    }

    // a tela fechou por outro motivo (o servidor fechou o menu, outra tela abriu...)
    public static void screenClosed() {
        if (leaving || phase == Phase.NONE || phase == Phase.OUTRO) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.containerMenu instanceof LapidaryMenu menu) {
            mc.player.connection.send(new ServerboundContainerClosePacket(menu.containerId));
            mc.player.containerMenu = mc.player.inventoryMenu;
        }
        leavingItems = List.of();
        startOutro();
    }

    private static void startOutro() {
        leaving = true;
        phase = Phase.OUTRO;
        phaseAt = clock();
        walking = false;
        if (spin != null) { spin.stopNow(); spin = null; }
        cutAt = -100;
    }

    private static void finish() {
        Minecraft mc = Minecraft.getInstance();
        phase = Phase.NONE;
        leaving = false;
        pile.clear();
        flying.clear();
        leavingItems = List.of();
        if (savedCamera != null && !savedCamera.isFirstPerson()) mc.options.setCameraType(savedCamera);
        savedCamera = null;
    }

    public static void abort() {   // mudou de mundo, morreu...
        if (spin != null) { spin.stopNow(); spin = null; }
        phase = Phase.NONE;
        leaving = false;
        pile.clear();
        leavingItems = List.of();
    }

    // ---------------------------------------------------------------- tick
    public static void tick() {
        if (phase == Phase.NONE) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) { abort(); return; }
        double now = clock();
        if (phase == Phase.OUTRO) {
            if (now - phaseAt >= OUTRO) finish();
            return;
        }
        if (!(player.containerMenu instanceof LapidaryMenu) || !mc.level.getBlockState(pos).is(ModBlocks.LAPIDARY_TABLE.get())) {
            leavingItems = List.of();
            if (mc.screen instanceof LapidarySceneScreen) { leaving = true; mc.setScreen(null); }
            startOutro();
            return;
        }
        if (phase == Phase.INTRO) {
            if (walking) {
                Vec3 stand = standPoint(player);
                if (Math.hypot(stand.x - player.getX(), stand.z - player.getZ()) < 0.18 || now >= walkUntil) {
                    walking = false;
                    leanFrom = now;
                }
            } else if (leanFrom >= 0 && now - leanFrom >= LEAN) {
                phase = Phase.ACTIVE;
                phaseAt = now;
                activeAt = now;
                faceTable(player);   // com a camera ja na cena: o giro do corpo nao aparece (os outros veem ele virar)
            }
        }
        boolean ready = ready();
        if (ready && !wasReady && !cutting()) sound(ModSounds.LAPIDARY_READY.get(), 0.6f, 1f);
        wasReady = ready;

        if (cutAt > 0) {
            double t = now - cutAt;
            if (!contactPlayed && t >= LapidaryCut.CONTACT) { contactPlayed = true; sound(ModSounds.LAPIDARY_CONTACT.get(), 0.7f, 1f); sound(SoundEvents.GRINDSTONE_USE, 0.35f, 1.3f); }
            if (!donePlayed && t >= LapidaryCut.gemAt(cutAll)) { donePlayed = true; sound(ModSounds.LAPIDARY_DONE.get(), 0.7f, 1f); }
            if (!beat && t >= LapidaryCut.placeAt(cutAll)) {
                beat = true;
                LapidaryMenu menu = menu();
                if (menu != null && mc.gameMode != null) {
                    mc.gameMode.handleInventoryButtonClick(menu.containerId, cutAll ? 1 : 0);
                    int made = cutAll ? Math.min(cutOre.getCount(), cutFlint.getCount()) : 1;
                    ItemStack gem = LapidaryRemote.gemOf(cutOre);
                    for (int i = 0; i < Math.min(made, 6); i++) pile.add(gem.copy());
                    while (pile.size() > 6) pile.remove(0);
                    sound(SoundEvents.AMETHYST_CLUSTER_PLACE, 0.8f, 1.2f);
                }
            }
            if (t >= LapidaryCut.length(cutAll)) cutAt = -100;
            LapidaryView v = localView();
            if (v.speed() > 0.05) LapidaryFx.sparks(mc.level, pos, v);
        }
    }

    // ---------------------------------------------------------------- andar ate a mesa
    static Vec3 standPoint(LocalPlayer player) {
        Vec3 s = LapidaryGeo.world(pos, facing, LapidaryGeo.STAND);
        return new Vec3(s.x, player.getY(), s.z);
    }

    static void faceTable(LocalPlayer player) {
        float yaw = facing.getOpposite().toYRot();
        player.setYRot(yaw);
        player.yHeadRot = yaw;
        player.yBodyRot = yaw;
        player.setXRot(38f);
    }

    public static void movementInput(Input input) {
        if (phase == Phase.NONE) return;
        LocalPlayer player = Minecraft.getInstance().player;
        input.leftImpulse = 0;
        input.forwardImpulse = 0;
        input.jumping = false;
        input.shiftKeyDown = false;
        input.up = input.down = input.left = input.right = false;
        if (walking && player != null) {
            Vec3 stand = standPoint(player);
            float yaw = (float) Math.toDegrees(Math.atan2(-(stand.x - player.getX()), stand.z - player.getZ()));
            player.setYRot(yaw);
            player.yHeadRot = yaw;
            input.forwardImpulse = 1f;
            input.up = true;
        }
    }

    // ---------------------------------------------------------------- camera
    static final double LEAN = 0.9, OUTRO = 0.8;
    static double weight;
    private static double parX, parY, lastFrame;

    public record Pose(Vec3 pos, float yaw, float pitch) {}

    // pose da camera da cena (com a "respiracao" e um leve deslocamento seguindo o mouse)
    static Pose scenePose() {
        Vec3 eye = point(LapidaryGeo.EYE);
        Vec3 look = point(LapidaryGeo.LOOK).subtract(eye);
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
        double kick = cutKick();
        if (kick > 0) {
            yaw += (float) ((Math.sin(t * 47) + Math.sin(t * 31 + 1)) * 0.15 * kick);
            pitch += (float) ((Math.sin(t * 53 + 2) + Math.sin(t * 29)) * 0.12 * kick);
        }
        return new Pose(eye, yaw, pitch);
    }

    // tremidinha quando o lapidador encosta na pedra
    private static double cutKick() {
        if (cutAt < 0) return 0;
        double t = clock() - cutAt;
        return LapidaryCut.hump(LapidaryCut.window(t, 0.75, 1.10)) * 0.8 + LapidaryCut.speed(t, cutAll) * 0.15;
    }

    @Nullable
    public static Pose override(Vec3 livePos, float liveYaw, float livePitch) {
        if (phase == Phase.NONE) { weight = 0; return null; }
        double now = clock();
        double w = switch (phase) {
            case INTRO -> leanFrom < 0 ? 0 : LapidaryCut.inOutCubic((now - leanFrom) / LEAN);
            case ACTIVE -> 1;
            default -> 1 - LapidaryCut.inOutCubic((now - phaseAt) / OUTRO);
        };
        weight = w;
        Pose s = scenePose();
        if (w <= 0.0001) return null;
        double hop = phase == Phase.INTRO ? 0.03 * LapidaryCut.hump(w) : 0;
        Vec3 p = livePos.add(s.pos().subtract(livePos).scale(w)).add(0, hop, 0);
        float yaw = liveYaw + Mth.wrapDegrees(s.yaw() - liveYaw) * (float) w;
        float pitch = Mth.lerp((float) w, livePitch, s.pitch());
        return new Pose(p, yaw, pitch);
    }

    public static double fov(double vanilla) {
        return Mth.lerp(weight, vanilla, 54.0);
    }

    // ---------------------------------------------------------------- menu
    @Nullable
    static LapidaryMenu menu() {
        LocalPlayer p = Minecraft.getInstance().player;
        return p != null && p.containerMenu instanceof LapidaryMenu m ? m : null;
    }

    static ItemStack slot(int i) {
        LapidaryMenu m = menu();
        return m == null ? ItemStack.EMPTY : m.getSlot(i).getItem();
    }

    public static ItemStack carried() {
        LapidaryMenu m = menu();
        return m == null ? ItemStack.EMPTY : m.getCarried();
    }

    static boolean ready() { return !slot(2).isEmpty(); }
    public static boolean cutting() { return cutAt > 0 && clock() - cutAt < LapidaryCut.length(cutAll); }

    static void slotClick(int slot, int button, boolean shift) {
        Minecraft mc = Minecraft.getInstance();
        LapidaryMenu m = menu();
        if (m == null || mc.gameMode == null || mc.player == null) return;
        mc.gameMode.handleInventoryMouseClick(m.containerId, slot, button, shift ? ClickType.QUICK_MOVE : ClickType.PICKUP, mc.player);
        LapidaryHands.tap();
    }

    // itens da barra: indices dos slots do inventario (no menu: 3 a 38) com silex ou minerio bruto, silex primeiro
    public static List<Integer> barSlots() {
        List<Integer> flint = new ArrayList<>(), ore = new ArrayList<>();
        LapidaryMenu m = menu();
        if (m == null) return List.of();
        for (int i = 3; i < m.slots.size(); i++) {
            ItemStack s = m.getSlot(i).getItem();
            if (s.is(Items.FLINT)) flint.add(i);
            else if (LapidaryMenu.gemFor(s) != null) ore.add(i);
        }
        flint.addAll(ore);
        return flint;
    }

    public static void barClick(int slot, int button) {
        if (phase != Phase.ACTIVE || cutting()) return;
        slotClick(slot, button, false);
        lastSlot = slot;
        sound(SoundEvents.ITEM_PICKUP, 0.25f, 1.4f);
    }

    // ---------------------------------------------------------------- clique na mesa
    public static void click(int button, boolean shift) {
        if (phase != Phase.ACTIVE || cutting()) return;
        ItemStack held = carried();
        switch (hover) {
            case FLINT -> {
                if (held.is(Items.FLINT)) { slotClick(1, button, false); sound(SoundEvents.STONE_PLACE, 0.6f, 1.6f); }
                else if (!held.isEmpty()) deny();
                else if (!slot(1).isEmpty()) slotClick(1, button, false);
            }
            case ORE -> {
                if (LapidaryMenu.gemFor(held) != null) { slotClick(0, button, false); sound(SoundEvents.DEEPSLATE_PLACE, 0.6f, 1.2f); }
                else if (!held.isEmpty()) deny();
                else if (!slot(0).isEmpty()) slotClick(0, button, false);
            }
            case BUTTON -> {
                if (!held.isEmpty() || !ready()) { deny(); return; }
                startCut(shift);
            }
            case PILE -> collect();
            default -> {
                if (button == 1 && !held.isEmpty()) giveBack();
            }
        }
    }

    // recolhe as gemas prontas que estao na mesa (as gemas ja estao no inventario: aqui so a mao as leva)
    static void collect() {
        if (pile.isEmpty()) return;
        double now = clock();
        for (int i = 0; i < pile.size(); i++)
            flying.add(new Flying(pile.get(pile.size() - 1 - i), point(LapidaryGeo.GEMS).add(0, 0.02 * i, 0), now + 0.06 * i));
        pile.clear();
        LapidaryHands.tap();
        sound(SoundEvents.ITEM_PICKUP, 0.35f, 1.25f);
        sound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.4f, 1.5f);
    }

    // devolve o item da mao para o lugar de onde veio (ou para qualquer espaco livre do inventario)
    static void giveBack() {
        LapidaryMenu m = menu();
        if (m == null) return;
        ItemStack held = m.getCarried();
        int target = -1;
        if (lastSlot >= 3 && lastSlot < m.slots.size()) {
            ItemStack there = m.getSlot(lastSlot).getItem();
            if (there.isEmpty() || ItemStack.isSameItemSameComponents(there, held)) target = lastSlot;
        }
        for (int i = 3; i < m.slots.size() && target < 0; i++) {
            ItemStack there = m.getSlot(i).getItem();
            if (ItemStack.isSameItemSameComponents(there, held) && there.getCount() < there.getMaxStackSize()) target = i;
        }
        for (int i = 3; i < m.slots.size() && target < 0; i++) if (m.getSlot(i).getItem().isEmpty()) target = i;
        if (target >= 0) { slotClick(target, 0, false); sound(SoundEvents.ITEM_PICKUP, 0.25f, 1.1f); }
    }

    static void deny() {
        denyAt = clock();
        sound(SoundEvents.NOTE_BLOCK_BASS.value(), 0.4f, 0.6f);
    }

    static void startCut(boolean all) {
        cutFlint = slot(1).copy();
        cutOre = slot(0).copy();
        cutAt = clock();
        cutAll = all;
        beat = contactPlayed = donePlayed = false;
        PacketDistributor.sendToServer(new LapidaryCutPayload(pos, all));
        sound(SoundEvents.STONE_BUTTON_CLICK_ON, 0.6f, 1.1f);
        sound(SoundEvents.PISTON_CONTRACT, 0.25f, 1.6f);
        if (spin != null) spin.stopNow();
        spin = SpinSound.start(pos, () -> localView().speed());
    }

    // ---------------------------------------------------------------- o que desenhar na mesa
    @Nullable
    public static LapidaryView viewFor(BlockPos p) {
        if (phase == Phase.NONE || !p.equals(pos)) return null;
        if (phase == Phase.OUTRO) return LapidaryView.IDLE;
        return localView();
    }

    static LapidaryView localView() {
        if (cutAt > 0) {
            double t = clock() - cutAt;
            if (t < LapidaryCut.length(cutAll)) {
                ItemStack disc;
                if (t < LapidaryCut.gemAt(cutAll)) disc = cutOre;
                else if (t < LapidaryCut.pickAt(cutAll)) disc = LapidaryRemote.gemOf(cutOre);
                else {
                    int used = cutAll ? Math.min(cutOre.getCount(), cutFlint.getCount()) : 1;
                    disc = cutOre.getCount() > used ? cutOre.copyWithCount(cutOre.getCount() - used) : ItemStack.EMPTY;
                }
                int usedFlint = t >= LapidaryCut.placeAt(cutAll) ? (cutAll ? Math.min(cutOre.getCount(), cutFlint.getCount()) : 1) : 0;
                ItemStack flint = cutFlint.getCount() > usedFlint ? cutFlint.copyWithCount(cutFlint.getCount() - usedFlint) : ItemStack.EMPTY;
                return new LapidaryView(flint, disc, (float) LapidaryCut.pop(t, cutAll), (float) LapidaryCut.angle(t, cutAll),
                        (float) LapidaryCut.armDrop(t, cutAll), (float) LapidaryCut.button(t), true, List.copyOf(pile),
                        (float) LapidaryCut.speed(t, cutAll));
            }
        }
        return new LapidaryView(slot(1), slot(0), 1f, 0f, 0f, 0f, ready(), List.copyOf(pile), 0f);
    }

    // onde a silhueta branca aparece agora (o lugar certo para o item na mao, ou o botao quando da para lapidar)
    public static Target ghost() {
        if (phase != Phase.ACTIVE || cutting()) return Target.NONE;
        ItemStack held = carried();
        if (held.is(Items.FLINT)) return fits(slot(1), held) ? Target.FLINT : Target.NONE;
        if (LapidaryMenu.gemFor(held) != null) return fits(slot(0), held) ? Target.ORE : Target.NONE;
        return held.isEmpty() && ready() ? Target.BUTTON : Target.NONE;
    }

    private static boolean fits(ItemStack there, ItemStack held) {
        return there.isEmpty() || ItemStack.isSameItemSameComponents(there, held) && there.getCount() < there.getMaxStackSize();
    }

    // ---------------------------------------------------------------- mouse -> alvo
    public static void updateHover() {
        hover = Target.NONE;
        if (phase != Phase.ACTIVE || cutting()) return;
        Vec3[] ray = LapidaryPick.ray();
        if (ray == null) return;
        double best = Double.MAX_VALUE;
        LapidaryView v = localView();
        Target[] ts = {Target.FLINT, Target.ORE, Target.BUTTON, Target.PILE};
        for (Target t : ts) {
            if (t == Target.PILE && (pile.isEmpty() || !carried().isEmpty())) continue;
            Vec3 p = targetPoint(t, v);
            double r = t == Target.BUTTON ? 0.07 : t == Target.ORE ? 0.11 : t == Target.PILE ? 0.13 : 0.09;
            Vec3 to = p.subtract(ray[0]);
            double along = to.dot(ray[1]);
            if (along <= 0) continue;
            double off = to.subtract(ray[1].scale(along)).length();
            if (off < r && along < best) { best = along; hover = t; }
        }
    }

    static Vec3 targetPoint(Target t, LapidaryView v) {
        return switch (t) {
            case FLINT -> point(LapidaryGeo.FLINT.x, LapidaryGeo.FLINT.y - v.armDrop(), LapidaryGeo.FLINT.z);
            case ORE -> point(LapidaryGeo.ORE);
            case BUTTON -> point(LapidaryGeo.BUTTON);
            case PILE -> point(LapidaryGeo.GEMS);
            default -> point(LapidaryGeo.LOOK);
        };
    }

    // onde o raio do mouse encontra o tampo da mesa (ou null)
    @Nullable
    static Vec3 onTop() {
        Vec3[] ray = LapidaryPick.ray();
        if (ray == null || Math.abs(ray[1].y) < 1e-4) return null;
        double y = pos.getY() + LapidaryGeo.TOP_Y / 16.0;
        double k = (y - ray[0].y) / ray[1].y;
        if (k <= 0) return null;
        Vec3 hit = ray[0].add(ray[1].scale(k));
        double cx = pos.getX() + 0.5, cz = pos.getZ() + 0.5;
        if (Math.abs(hit.x - cx) > 0.6 || Math.abs(hit.z - cz) > 0.6) return null;
        return new Vec3(Mth.clamp(hit.x, cx - 0.42, cx + 0.42), y, Mth.clamp(hit.z, cz - 0.46, cz + 0.46));
    }

    // A legenda aparece quando muda e some sozinha (a "wrong" e' mais curta)
    public static float hintAlpha(String key) {
        double now = clock();
        if (!key.equals(hintKey)) { hintKey = key; hintAt = now; }
        if (key.isEmpty()) return 0f;
        double life = key.endsWith(".wrong") ? 1.1 : 2.4;
        return (float) (1 - LapidaryCut.window(now - hintAt, life, life + 0.5));
    }

    // "Esc: sair..." so aparece no comeco da cena
    public static float leaveAlpha() {
        if (activeAt < 0) return 0f;
        return (float) (1 - LapidaryCut.window(clock() - activeAt, 3.5, 4.2));
    }

    // dica no rodape
    public static String hint() {
        if (clock() - denyAt < 1.2) return "lapidary.binosvanillaplus.hint.wrong";
        if (cutting()) return "lapidary.binosvanillaplus.hint.working";
        ItemStack held = carried();
        if (held.is(Items.FLINT)) return "lapidary.binosvanillaplus.hint.place_flint";
        if (LapidaryMenu.gemFor(held) != null) return "lapidary.binosvanillaplus.hint.place_ore";
        if (slot(1).isEmpty()) return "lapidary.binosvanillaplus.hint.take_flint";
        if (slot(0).isEmpty()) return "lapidary.binosvanillaplus.hint.take_ore";
        return ready() ? "lapidary.binosvanillaplus.hint.press" : "";
    }

    // ---------------------------------------------------------------- ajudas
    static Vec3 point(Vec3 px) { return LapidaryGeo.world(pos, facing, px); }
    static Vec3 point(double x, double y, double z) { return LapidaryGeo.world(pos, facing, x, y, z); }

    static void sound(SoundEvent ev, float vol, float pitch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null)
            mc.level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, ev, SoundSource.BLOCKS, vol, pitch, false);
    }
}
