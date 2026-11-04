package com.bino.binosvanillaplus.journal;

import com.bino.binosvanillaplus.block.custom.CameraTripodBlock;
import com.bino.binosvanillaplus.block.custom.FossilBoneBlock;
import com.bino.binosvanillaplus.block.custom.FossilSkullBlock;
import com.bino.binosvanillaplus.gear.Dynamite;
import com.bino.binosvanillaplus.item.ModDataComponents;
import com.bino.binosvanillaplus.item.ModItems;
import com.bino.binosvanillaplus.item.custom.CameraItem;
import com.bino.binosvanillaplus.network.DiaryDataPayload;
import com.bino.binosvanillaplus.network.PhotoUploadPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

// Logica do diario (servidor): identifica o que foi fotografado, registra descobertas e guarda as fotos livres
public final class Journal {
    private Journal() {}

    public static final double PHOTO_RANGE = 24.0;
    private static final Map<UUID, Long> LAST = new HashMap<>();

    // ---------------------------------------------------------------- o que a camera esta vendo

    private record Found(String key, String name, String desc, String icon, BlockPos pos) {}

    private static Found identify(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 eye = p.getEyePosition();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(p.getLookAngle().scale(PHOTO_RANGE)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, p));
        if (hit.getType() != HitResult.Type.BLOCK) return null;
        BlockPos pos = hit.getBlockPos();
        BlockState s = level.getBlockState(pos);
        Block b = s.getBlock();
        String itemId = BuiltInRegistries.ITEM.getKey(b.asItem()).toString();
        if ((b instanceof FossilBoneBlock || b instanceof FossilSkullBlock) && s.getValue(FossilBoneBlock.NATURAL)) {
            int sp = s.getValue(FossilBoneBlock.SPECIES);
            String part = b instanceof FossilBoneBlock fb ? fb.part() : "skull";
            String key = Fossils.partKey(sp, part);
            return new Found("fossil:" + sp + ":" + part, key, key + ".desc", itemId, pos);
        }
        if (Dynamite.isOre(s)) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(b);
            Block base = b;
            if (id.getPath().startsWith("deepslate_")) {   // minerio de ardosia conta como o mesmo minerio
                ResourceLocation plain = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath().substring("deepslate_".length()));
                base = BuiltInRegistries.BLOCK.getOptional(plain).orElse(b);
            }
            ResourceLocation bid = BuiltInRegistries.BLOCK.getKey(base);
            String desc = "discovery.binosvanillaplus.ore." + bid.getPath();
            return new Found("ore:" + bid, base.getDescriptionId(), desc, BuiltInRegistries.ITEM.getKey(base.asItem()).toString(), pos);
        }
        return null;
    }

    // ---------------------------------------------------------------- foto chegou do cliente

    public static void onPhoto(ServerPlayer p, PhotoUploadPayload msg) {
        long now = p.serverLevel().getGameTime();
        Long last = LAST.get(p.getUUID());
        if (last != null && now - last < 10) return;   // no maximo 2 fotos por segundo
        LAST.put(p.getUUID(), now);
        if (msg.png().length > PhotoUploadPayload.MAX_BYTES || msg.png().length < 8) return;
        ExplorationData data = ExplorationData.get(p.server);
        ExplorationData.Journal j = data.journal(p.getUUID());
        long day = p.serverLevel().getDayTime() / 24000L + 1;

        if (!msg.tripod()) {   // camera na mao: pode ser uma descoberta
            boolean holding = p.getMainHandItem().getItem() instanceof CameraItem || p.getOffhandItem().getItem() instanceof CameraItem;
            if (!holding) return;
            Found f = identify(p);
            if (f != null) {
                if (j.has(f.key())) {
                    p.displayClientMessage(Component.translatable("message.binosvanillaplus.journal.already", Component.translatable(f.name()))
                            .withStyle(ChatFormatting.DARK_GRAY), true);
                    return;
                }
                j.discoveries.add(new ExplorationData.Discovery(f.key(), f.name(), f.desc(), f.icon(),
                        f.pos().getX(), f.pos().getY(), f.pos().getZ(), day));
                data.setDirty();
                p.displayClientMessage(Component.translatable("message.binosvanillaplus.journal.new", Component.translatable(f.name()))
                        .withStyle(ChatFormatting.GOLD), true);
                p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1f);
                checkSkeleton(p, j, f.key());
                return;
            }
        } else if (!(p.serverLevel().getBlockState(msg.tripodPos()).getBlock() instanceof CameraTripodBlock)) {
            return;
        }
        // foto livre: guardada no fim do diario
        if (j.photos.size() >= ExplorationData.MAX_PHOTOS) {
            p.displayClientMessage(Component.translatable("message.binosvanillaplus.journal.full").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        BlockPos at = msg.tripod() ? msg.tripodPos() : p.blockPosition();
        j.photos.add(new ExplorationData.Photo(msg.png(), day, at.getX(), at.getY(), at.getZ()));
        data.setDirty();
        p.displayClientMessage(Component.translatable("message.binosvanillaplus.journal.photo").withStyle(ChatFormatting.GRAY), true);
    }

    // todas as partes de uma especie fotografadas = esqueleto completo
    private static void checkSkeleton(ServerPlayer p, ExplorationData.Journal j, String key) {
        if (!key.startsWith("fossil:")) return;
        int sp = Integer.parseInt(key.split(":")[1]);
        for (String part : Fossils.parts(sp)) if (!j.has("fossil:" + sp + ":" + part)) return;
        p.sendSystemMessage(Component.translatable("message.binosvanillaplus.journal.skeleton", Component.translatable(Fossils.speciesKey(sp)))
                .withStyle(ChatFormatting.GOLD));
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.7f, 1f);
    }

    // ---------------------------------------------------------------- diario aberto / revelar foto

    public static void sendDiary(ServerPlayer p) {
        ExplorationData.Journal j = ExplorationData.get(p.server).journal(p.getUUID());
        List<DiaryDataPayload.Entry> es = new ArrayList<>();
        for (ExplorationData.Discovery d : j.discoveries)
            es.add(new DiaryDataPayload.Entry(d.key(), d.name(), d.desc(), d.icon(), d.x(), d.y(), d.z(), d.day()));
        List<DiaryDataPayload.Pic> ps = new ArrayList<>();
        for (ExplorationData.Photo ph : j.photos) ps.add(new DiaryDataPayload.Pic(ph.png(), ph.day()));
        PacketDistributor.sendToPlayer(p, new DiaryDataPayload(es, ps));
    }

    // revela a foto 'index' do diario num item Foto (custa 1 papel)
    public static void develop(ServerPlayer p, int index) {
        ExplorationData.Journal j = ExplorationData.get(p.server).journal(p.getUUID());
        if (index < 0 || index >= j.photos.size()) return;
        if (!p.getAbilities().instabuild) {
            int slot = p.getInventory().findSlotMatchingItem(new ItemStack(Items.PAPER));
            if (slot < 0) {
                p.displayClientMessage(Component.translatable("message.binosvanillaplus.journal.need_paper").withStyle(ChatFormatting.GRAY), true);
                return;
            }
            p.getInventory().removeItem(slot, 1);
        }
        ExplorationData.Photo ph = j.photos.get(index);
        ItemStack out = new ItemStack(ModItems.PHOTO.get());
        out.set(ModDataComponents.PHOTO.get(), new ModDataComponents.PhotoData(ph.png(), (int) ph.day()));
        if (!p.getInventory().add(out)) p.drop(out, false);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8f, 1.4f);
    }
}
