package com.bino.binosvanillaplus.client.eat;

import com.bino.binosvanillaplus.BinosVanillaMod;
import com.bino.binosvanillaplus.item.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Vector3f;

import javax.annotation.Nullable;

// Comer em primeira pessoa com animacao propria (tecnica parecida com a do Immersive Eating, codigo nosso, sem GeckoLib):
// um "rig" feito no Blockbench (tools/blockbench/eat_rig.bbmodel -> EatRigModelData/EatRigAnimations) tem ossos para os
// dois bracos, a comida e a camera. Enquanto o jogador come uma comida da lista, as maos normais nao sao desenhadas:
// no lugar, os BRACOS REAIS do jogador (skin + manga da armadura) e o modelo 3D da comida sao desenhados nos ossos.
// O tempo da animacao = ticks de uso do proprio jogo (sem pacotes). A escala do osso "food" mostra as mordidas.
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public final class EatAnimation {
    private EatAnimation() {}

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "eat_rig"), "main");
    private static final Vector3f CACHE = new Vector3f();
    private static Rig rig;
    private static float camPitch, camRoll;   // graus, do osso "camera"

    // qual animacao cada comida usa (null = comer normal do jogo)
    @Nullable
    public static AnimationDefinition forItem(ItemStack stack) {
        Item i = stack.getItem();
        if (i == ModItems.CHEESE_SLICE.get()) return EatRigAnimations.EAT_CHEESE;
        if (i == ModItems.RAT_MEAT.get() || i == ModItems.COOKED_RAT_MEAT.get()) return EatRigAnimations.EAT_MEAT;
        return null;
    }

    static final class Rig extends HierarchicalModel<Entity> {
        final ModelPart top, root, camera, rightArm, leftArm, food;
        Rig(ModelPart top) {
            this.top = top; root = top.getChild("root"); camera = root.getChild("camera");
            rightArm = root.getChild("right_arm"); leftArm = root.getChild("left_arm"); food = rightArm.getChild("food");
        }
        @Override public ModelPart root() { return top; }
        @Override public void setupAnim(Entity e, float a, float b, float c, float d, float f) {}
    }

    @SubscribeEvent
    public static void onLayers(EntityRenderersEvent.RegisterLayerDefinitions e) { e.registerLayerDefinition(LAYER, EatRigModelData::createBodyLayer); }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers e) { rig = new Rig(e.getEntityModels().bakeLayer(LAYER)); }   // recarrega com os recursos

    @Nullable
    private static AnimationDefinition active(LocalPlayer p) {
        if (p == null || !p.isUsingItem()) return null;
        return forItem(p.getUseItem());
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        AnimationDefinition def = active(player);
        if (def == null || rig == null) { camPitch = camRoll = 0; return; }
        e.setCanceled(true);                                         // nada das maos normais enquanto come
        if (e.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;   // a cena e desenhada uma vez so

        float ticks = player.getTicksUsingItem() + e.getPartialTick();
        rig.root().getAllParts().forEach(ModelPart::resetPose);
        KeyframeAnimations.animate(rig, def, (long) (ticks * 50f), 1.0f, CACHE);
        camPitch = (float) Math.toDegrees(rig.camera.xRot);
        camRoll = (float) Math.toDegrees(rig.camera.zRot);

        PoseStack ps = e.getPoseStack();
        MultiBufferSource buf = e.getMultiBufferSource();
        int light = e.getPackedLight();
        boolean leftHanded = player.getMainArm() == HumanoidArm.LEFT;
        ps.pushPose();
        if (leftHanded) ps.scale(-1, 1, 1);                          // canhoto: cena espelhada
        ps.scale(-1, -1, 1);                                         // espaco do modelo (y para baixo) -> espaco da camera
        ps.translate(0, -1.5, 0);
        if (mc.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer) {
            arm(ps, buf, light, player, renderer, rig.rightArm, !leftHanded);
            arm(ps, buf, light, player, renderer, rig.leftArm, leftHanded);
        }
        food(ps, buf, light, player);
        ps.popPose();
    }

    // braco real: o osso do rig fica no ombro e o braco "pendura" 12 px para +y, igual ao braco do modelo do jogador
    private static void arm(PoseStack ps, MultiBufferSource buf, int light, LocalPlayer player, PlayerRenderer renderer, ModelPart bone, boolean right) {
        if (!bone.visible) return;
        ps.pushPose();
        rig.root.translateAndRotate(ps);
        bone.translateAndRotate(ps);
        ps.translate((right ? 6 : -6) / 16f, 0, 0);              // desfaz o pivo do braco no modelo do jogador (ombro +-5 px, y 2 px)
        if (right) renderer.renderRightHand(ps, buf, light, player);
        else renderer.renderLeftHand(ps, buf, light, player);
        ps.popPose();
    }

    private static void food(PoseStack ps, MultiBufferSource buf, int light, LocalPlayer player) {
        ItemStack stack = player.getUseItem();
        Minecraft mc = Minecraft.getInstance();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        BakedModel solid = mc.getModelManager().getModel(ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath(BinosVanillaMod.MOD_ID, "item/" + id.getPath() + "_3d")));
        ps.pushPose();
        rig.root.translateAndRotate(ps);
        rig.rightArm.translateAndRotate(ps);
        rig.food.translateAndRotate(ps);                            // inclui a escala (mordidas)
        ps.mulPose(Axis.ZP.rotationDegrees(180));                   // volta para y para cima (o item e desenhado em pe)
        ps.scale(0.55f, 0.55f, 0.55f);
        mc.getItemRenderer().render(stack, ItemDisplayContext.NONE, false, ps, buf, light, OverlayTexture.NO_OVERLAY, solid);
        ps.popPose();
    }

    // camera acompanha a mordida/mastigacao
    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles e) {
        if (active(Minecraft.getInstance().player) == null || !Minecraft.getInstance().options.getCameraType().isFirstPerson()) return;
        e.setPitch(e.getPitch() + camPitch);
        e.setRoll(e.getRoll() + camRoll);
    }

    // terceira pessoa: progresso 0..1 e se esta na hora de uma mordida (usado pelo mixin do HumanoidModel)
    public static boolean eatingAnimated(net.minecraft.world.entity.LivingEntity e) {
        return e.isUsingItem() && forItem(e.getUseItem()) != null;
    }
}
