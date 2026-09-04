package mc.sayda.creraces.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

/**
 * Button backed by a plain texture file with UV coordinates, the way vanilla's ImageButton
 * worked before 1.21. ImageButton now takes WidgetSprites (GUI atlas sprites) instead, which
 * would mean authoring sprite JSON for every button texture the mod ships, so this keeps the
 * existing raw-texture assets working unchanged.
 *
 * The hovered state draws from {@code v + hoverVOffset} and the disabled state from
 * {@code v + hoverVOffset * 2}, matching vanilla's {@code AbstractWidget.renderTexture}
 * three-frame layout (normal/hovered/disabled stacked in one file).
 */
public class TextureButton extends Button {

    private final ResourceLocation texture;
    private final int u;
    private final int v;
    private final int hoverVOffset;
    private final int textureWidth;
    private final int textureHeight;

    public TextureButton(int x, int y, int width, int height, int u, int v, int hoverVOffset,
            ResourceLocation texture, int textureWidth, int textureHeight, OnPress onPress) {
        this(x, y, width, height, u, v, hoverVOffset, texture, textureWidth, textureHeight, onPress,
                CommonComponents.EMPTY);
    }

    public TextureButton(int x, int y, int width, int height, int u, int v, int hoverVOffset,
            ResourceLocation texture, int textureWidth, int textureHeight, OnPress onPress,
            Component message) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.texture = texture;
        this.u = u;
        this.v = v;
        this.hoverVOffset = hoverVOffset;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }

    @Override
    public void renderWidget(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int drawV = this.v;
        if (!this.isActive()) {
            drawV += this.hoverVOffset * 2;
        } else if (this.isHoveredOrFocused()) {
            drawV += this.hoverVOffset;
        }

        graphics.blit(this.texture, this.getX(), this.getY(), this.u, drawV,
                this.width, this.height, this.textureWidth, this.textureHeight);
    }
}
