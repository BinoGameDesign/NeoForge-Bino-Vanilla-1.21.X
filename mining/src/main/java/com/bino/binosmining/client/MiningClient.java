package com.bino.binosmining.client;

import com.bino.binosmining.BinosMining;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

import java.io.IOException;

// Shaders do Mining: vidro das janelas (cenas da lapidacao e da bancada) e o contorno da forja
@EventBusSubscriber(modid = BinosMining.MOD_ID, value = Dist.CLIENT)
public final class MiningClient {
    private MiningClient() {}

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(BinosMining.MOD_ID, "ui_blur"), DefaultVertexFormat.POSITION),
                s -> com.bino.binosmining.client.ui.Glass.blur = s);
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(BinosMining.MOD_ID, "ui_glass"), DefaultVertexFormat.POSITION),
                s -> com.bino.binosmining.client.ui.Glass.glass = s);
        // contorno da forja (mascara + borda na tela)
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(BinosMining.MOD_ID, "forge_mask"), DefaultVertexFormat.POSITION_TEX_COLOR),
                s -> com.bino.binosmining.client.gear.ForgeOutline.maskShader = s);
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(BinosMining.MOD_ID, "forge_mask_ndc"), DefaultVertexFormat.POSITION_TEX_COLOR),
                s -> com.bino.binosmining.client.gear.ForgeOutline.maskNdcShader = s);
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(BinosMining.MOD_ID, "forge_edge"), DefaultVertexFormat.POSITION),
                s -> com.bino.binosmining.client.gear.ForgeOutline.edgeShader = s);
    }
}
