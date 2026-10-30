package com.halex.createrancher.content.satchel;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

// The satchel's screen. Looks like a one-row chest.
public class SatchelScreen extends AbstractContainerScreen<SatchelMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/gui/container/generic_54.png");
    private static final int ROWS = 1;

    public SatchelScreen(SatchelMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = 114 + ROWS * 18;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, ROWS * 18 + 17);
        graphics.blit(TEXTURE, leftPos, topPos + ROWS * 18 + 17, 0, 126, imageWidth, 96);
    }
}
