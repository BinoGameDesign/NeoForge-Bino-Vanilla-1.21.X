package com.bino.binosvanillaplus.client;

import com.bino.binosvanillaplus.menu.SpiritMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

// Janela do Espirito: visual de bau duplo + botoes ao lado (Pegar tudo / Pegar XP / Dissipar)
public class SpiritScreen extends AbstractContainerScreen<SpiritMenu> {
    private static final ResourceLocation BG =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int ROWS = 6;

    private Button xpButton;

    public SpiritScreen(SpiritMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = 114 + ROWS * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos + this.imageWidth + 4;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.binosvanillaplus.spirit.take_all"),
                b -> click(SpiritMenu.BUTTON_TAKE_ALL)).bounds(x, this.topPos + 18, 96, 20).build());
        this.xpButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.binosvanillaplus.spirit.take_xp"),
                b -> click(SpiritMenu.BUTTON_TAKE_XP)).bounds(x, this.topPos + 42, 96, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.binosvanillaplus.spirit.dissipate"),
                b -> click(SpiritMenu.BUTTON_DISSIPATE)).bounds(x, this.topPos + 66, 96, 20).build());
    }

    private void click(int buttonId) {
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.xpButton.active = this.menu.getExperience() > 0;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BG, this.leftPos, this.topPos, 0, 0, this.imageWidth, ROWS * 18 + 17);
        graphics.blit(BG, this.leftPos, this.topPos + ROWS * 18 + 17, 0, 126, this.imageWidth, 96);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(this.font, Component.translatable("gui.binosvanillaplus.spirit.xp", this.menu.getExperience()),
                this.leftPos + this.imageWidth + 4, this.topPos + 92, 0xFFFFFF);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
