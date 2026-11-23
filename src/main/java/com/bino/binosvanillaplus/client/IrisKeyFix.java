package com.bino.binosvanillaplus.client;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

// Tambem desliga o indicador de recarga do ataque (o combate novo nao usa).
// O Iris usa a R para "recarregar shaders", que e' a tecla Defender do combate. Uma vez por abertura do jogo: se a tecla
// do Iris estiver na R, passa para a V (a pessoa pode trocar de novo nos controles; so volta se ela colocar na R).
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class IrisKeyFix {
    private IrisKeyFix() {}

    private static boolean done;

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;
        // o indicador de recarga do ataque (mira ou barra de atalhos) nao combina com o combate novo: sempre desligado
        if (mc.options.attackIndicator().get() != net.minecraft.client.AttackIndicatorStatus.OFF) {
            mc.options.attackIndicator().set(net.minecraft.client.AttackIndicatorStatus.OFF);
            mc.options.save();
        }
        if (done) return;
        done = true;
        boolean changed = false;
        for (KeyMapping k : mc.options.keyMappings) {
            String n = k.getName();
            if (n.startsWith("iris.") && n.contains("reload") && k.getKey().getValue() == GLFW.GLFW_KEY_R) {
                k.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_V));
                changed = true;
            }
        }
        if (changed) { KeyMapping.resetMapping(); mc.options.save(); }
    }
}
