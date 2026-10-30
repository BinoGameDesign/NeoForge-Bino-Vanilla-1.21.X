package com.bino.binosvanillaplus.client.recipes;

import com.bino.binosvanillaplus.BinosVanillaMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

// Liga o navegador de receitas ao jogo:
//  - teclas R / U / B sobre qualquer item de qualquer inventario (bau, bancada, criativo...)
//  - tecla Y em jogo abre a lista de todos os itens
//  - botao "Receitas+" ao lado dos inventarios que tem livro de receitas
@EventBusSubscriber(modid = BinosVanillaMod.MOD_ID, value = Dist.CLIENT)
public class LookupEvents {

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(LookupKeys.SHOW_RECIPE);
        event.register(LookupKeys.SHOW_USES);
        event.register(LookupKeys.BOOKMARK);
        event.register(LookupKeys.OPEN);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (LookupKeys.OPEN.consumeClick()) {
            if (mc.screen == null && mc.player != null && mc.level != null)
                mc.setScreen(new RecipeLookupScreen(null, RecipeLookupScreen.Mode.BROWSE, null));
        }
    }

    @SubscribeEvent
    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        Screen screen = event.getScreen();
        if (screen instanceof RecipeLookupScreen || !(screen instanceof AbstractContainerScreen<?> cs)) return;
        if (screen.getFocused() instanceof EditBox box && box.canConsumeInput()) return;   // digitando em uma busca

        Slot slot = cs.getSlotUnderMouse();
        if (slot == null || !slot.hasItem()) return;
        ItemStack stack = slot.getItem();

        if (LookupKeys.SHOW_RECIPE.matches(event.getKeyCode(), event.getScanCode())) {
            Minecraft.getInstance().setScreen(new RecipeLookupScreen(screen, RecipeLookupScreen.Mode.RESULT, stack.getItem()));
            event.setCanceled(true);
        } else if (LookupKeys.SHOW_USES.matches(event.getKeyCode(), event.getScanCode())) {
            Minecraft.getInstance().setScreen(new RecipeLookupScreen(screen, RecipeLookupScreen.Mode.USAGE, stack.getItem()));
            event.setCanceled(true);
        } else if (LookupKeys.BOOKMARK.matches(event.getKeyCode(), event.getScanCode())) {
            FavoritesStore.toggle(stack.getItem());
            event.setCanceled(true);
        }
    }

    // Botao "Receitas+" acima do canto direito dos inventarios com livro de receitas
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof AbstractContainerScreen<?> cs) || !(screen instanceof RecipeUpdateListener)) return;
        event.addListener(new LookupButton(cs));
    }

    private static class LookupButton extends Button {
        private final AbstractContainerScreen<?> screen;

        LookupButton(AbstractContainerScreen<?> screen) {
            super(0, 0, 58, 16, Component.translatableWithFallback("recipes.binosvanillaplus.button", "Receitas+"),
                    b -> Minecraft.getInstance().setScreen(new RecipeLookupScreen(screen, RecipeLookupScreen.Mode.BROWSE, null)),
                    DEFAULT_NARRATION);
            this.screen = screen;
            reposition();
        }

        private void reposition() {
            setPosition(screen.getGuiLeft() + screen.getXSize() - 58, screen.getGuiTop() - 18);
        }

        @Override
        public void renderWidget(GuiGraphics g, int mx, int my, float partial) {
            reposition();   // acompanha o inventario quando o livro de receitas abre/fecha
            super.renderWidget(g, mx, my, partial);
        }
    }
}
