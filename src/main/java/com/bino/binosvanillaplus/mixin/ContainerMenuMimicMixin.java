package com.bino.binosvanillaplus.mixin;

import com.bino.binosvanillaplus.mimic.MimicChest;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Mimico: qualquer clique num espaco do bau (pegar, shift, trocar com a hotbar...) revela o mimico e o clique nao acontece.
@Mixin(AbstractContainerMenu.class)
public abstract class ContainerMenuMimicMixin {
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void binosvanillaplus$mimic(int slotId, int button, ClickType type, Player player, CallbackInfo ci) {
        if (!((Object) this instanceof ChestMenu menu) || !(player instanceof ServerPlayer sp)) return;
        if (slotId < 0 || slotId >= menu.getRowCount() * 9) return;
        var hs = MimicChest.halves(menu.getContainer());
        if (hs.stream().noneMatch(MimicChest::isMimic)) return;
        ci.cancel();
        MimicChest.reveal(sp, hs);
    }
}
