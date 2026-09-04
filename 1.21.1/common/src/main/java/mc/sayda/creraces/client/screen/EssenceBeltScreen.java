package mc.sayda.creraces.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import mc.sayda.creraces.world.inventory.EssenceBeltMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class EssenceBeltScreen extends AbstractContainerScreen<EssenceBeltMenu> {

    private static final ResourceLocation BG =
            ResourceLocation.fromNamespaceAndPath("creraces", "textures/screens/essence_belt.png");

    // GUI dimensions: 8-slot row across the top, player inventory below.
    private static final int X_SIZE = 176;
    private static final int Y_SIZE = 133;

    public EssenceBeltScreen(EssenceBeltMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = X_SIZE;
        this.imageHeight = Y_SIZE;
        this.inventoryLabelY = 39; // vanilla hopper formula: imageHeight - 94
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        graphics.blit(BG, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
