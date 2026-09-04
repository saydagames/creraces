package mc.sayda.creraces.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

public class GenericRaceButton extends ImageButton {
    private static final ResourceLocation BUTTON_TEX = new ResourceLocation("creraces",
            "textures/screens/atlas/button.png");
    private final Component label;

    public GenericRaceButton(int x, int y, int width, int height, Component label, OnPress onPress) {
        super(x, y, width, height, 0, 0, height, BUTTON_TEX, width, height * 3, onPress);
        this.label = label;
    }

    @Override
    public void renderWidget(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);

        Minecraft mc = Minecraft.getInstance();
        int color = this.active ? (this.isHoveredOrFocused() ? 0xFFFFFF : 0xE0E0E0) : 0xA0A0A0;
        graphics.drawCenteredString(mc.font, this.label, this.getX() + this.width / 2,
                this.getY() + (this.height - 8) / 2, color);
    }
}
