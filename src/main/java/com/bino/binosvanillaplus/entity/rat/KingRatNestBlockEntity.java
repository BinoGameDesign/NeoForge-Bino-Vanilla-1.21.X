package com.bino.binosvanillaplus.entity.rat;

import com.bino.binosvanillaplus.block.entity.ModBlockEntities;
import com.bino.binosvanillaplus.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

// Ninho do King Rat (fica escondido embaixo da cama dele no covil). Regras do Bino (27/11):
//  - jogador (sobrevivencia/aventura) a ~10 blocos em qualquer direcao: o rei nasce em cima do ninho e acorda;
//  - so um rei por vez; se ninguem fica perto (40 blocos) por 30 s sem matar ele, ele some e nasce de novo depois;
//  - morto: o ninho fica vazio para sempre.
public class KingRatNestBlockEntity extends BlockEntity {
    public static final int WAKE_RANGE = 10, KEEP_RANGE = 40, AWAY_TICKS = 600, LOST_TICKS = 200;

    private boolean defeated;
    private UUID king;
    private int away, lost;

    public KingRatNestBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.KING_RAT_NEST.get(), pos, state); }

    public void defeated() { defeated = true; king = null; setChanged(); }
    public boolean isDefeated() { return defeated; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, KingRatNestBlockEntity be) {
        if (!(level instanceof ServerLevel sl) || be.defeated || level.getGameTime() % 20 != 0) return;
        Entity k = be.king != null ? sl.getEntity(be.king) : null;
        if (k != null && k.isAlive()) {
            be.lost = 0;
            boolean near = !level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(KEEP_RANGE), p -> !p.isSpectator() && !p.isCreative()).isEmpty();
            be.away = near ? 0 : be.away + 20;
            if (be.away >= AWAY_TICKS) { k.discard(); be.king = null; be.away = 0; be.setChanged(); }   // volta depois, inteiro
            return;
        }
        if (be.king != null) {   // o rei nao esta carregado: espera um pouco antes de dar como perdido
            be.lost += 20;
            if (be.lost < LOST_TICKS) return;
            be.king = null; be.lost = 0; be.setChanged();
        }
        boolean close = !level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(WAKE_RANGE), p -> !p.isSpectator() && !p.isCreative()).isEmpty();
        if (!close) return;
        KingRatEntity rat = ModEntities.KING_RAT.get().create(sl);
        if (rat == null) return;
        rat.moveTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, level.random.nextFloat() * 360f, 0f);
        rat.setNest(pos);
        rat.finalizeSpawn(sl, sl.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
        sl.addFreshEntity(rat);
        rat.startWake();
        be.king = rat.getUUID();
        be.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        tag.putBoolean("Defeated", defeated);
        if (king != null) tag.putUUID("King", king);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        defeated = tag.getBoolean("Defeated");
        king = tag.hasUUID("King") ? tag.getUUID("King") : null;
    }
}
