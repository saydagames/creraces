package mc.sayda.creraces.client.screen;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.client.media.BadAppleVideoData;
import mc.sayda.creraces.registry.ModSounds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * Easter egg screen: plays the Bad Apple RLE video baked into the mod jar, triggered
 * by typing "badapple" on the race menu screen. Press ESC to exit.
 */
public class BadAppleScreen extends Screen {
    private static final ResourceLocation TEXTURE_LOCATION = new ResourceLocation(CreRaces.MODID,
            "dynamic/badapple");

    // Leaves room for the wood-frame border plus breathing space, and a strip at the
    // bottom for the exit hint and credits, so the panel doesn't touch the screen edges
    // and the game world stays visible around it.
    private static final int SIDE_MARGIN = 20;
    private static final int TOP_MARGIN = 12;
    private static final int BOTTOM_TEXT_HEIGHT = 86;

    private final Screen parent;
    private BadAppleVideoData video;
    private DynamicTexture texture;
    private SoundInstance sound;
    private long startTimeNanos;
    private int lastFrameRendered = -1;

    public BadAppleScreen(Screen parent) {
        super(Component.translatable("screen.creraces.badapple"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        try {
            video = new BadAppleVideoData();
        } catch (IOException e) {
            CreRaces.LOGGER.error("Failed to load Bad Apple video", e);
            if (this.minecraft != null) {
                this.minecraft.setScreen(parent);
            }
            return;
        }

        NativeImage image = new NativeImage(video.width, video.height, false);
        texture = new DynamicTexture(image);

        if (this.minecraft != null) {
            this.minecraft.getTextureManager().register(TEXTURE_LOCATION, texture);
            sound = SimpleSoundInstance.forMusic(ModSounds.BADAPPLE.get());
            this.minecraft.getSoundManager().play(sound);
        }

        startTimeNanos = System.nanoTime();
    }

    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Vanilla's translucent in-game overlay (not an opaque fill), so the world behind
        // stays visible and it's clear this is happening in Minecraft, not a separate video.
        this.renderBackground(graphics);

        if (video == null || texture == null) {
            return;
        }

        double elapsedSeconds = (System.nanoTime() - startTimeNanos) / 1.0e9;
        int targetFrame = (int) (elapsedSeconds * video.fps);

        if (targetFrame >= video.frameCount) {
            this.onClose();
            return;
        }

        if (targetFrame != lastFrameRendered) {
            video.decodeFrame(targetFrame, texture.getPixels());
            texture.upload();
            lastFrameRendered = targetFrame;
        }

        int availableHeight = this.height - TOP_MARGIN - BOTTOM_TEXT_HEIGHT;
        float scale = Math.min((float) (this.width - SIDE_MARGIN * 2) / video.width,
                (float) availableHeight / video.height);
        int drawWidth = Math.round(video.width * scale);
        int drawHeight = Math.round(video.height * scale);
        int offsetX = (this.width - drawWidth) / 2;
        int offsetY = TOP_MARGIN + (availableHeight - drawHeight) / 2;

        // Wood-frame border matching TerritoryMapScreen, so the video panel reads as a
        // clearly bounded frame against the surrounding black instead of bleeding into it.
        graphics.fill(offsetX - 4, offsetY - 4, offsetX + drawWidth + 4, offsetY + drawHeight + 4, 0xFF3D2008);
        graphics.fill(offsetX - 3, offsetY - 3, offsetX + drawWidth + 3, offsetY + drawHeight + 3, 0xFF7A4A1E);
        graphics.fill(offsetX - 2, offsetY - 2, offsetX + drawWidth + 2, offsetY + drawHeight + 2, 0xFF3D2008);
        graphics.fill(offsetX - 1, offsetY - 1, offsetX + drawWidth + 1, offsetY + drawHeight + 1, 0xFF7A4A1E);

        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        graphics.pose().pushPose();
        graphics.pose().translate(offsetX, offsetY, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.blit(TEXTURE_LOCATION, 0, 0, 0, 0, video.width, video.height, video.width, video.height);
        graphics.pose().popPose();

        RenderSystem.disableBlend();

        // Exit hint and source credits, below the framed panel.
        int textY = offsetY + drawHeight + 4 + 6;
        graphics.drawCenteredString(this.font, Component.translatable("screen.creraces.badapple.exit_hint"),
                this.width / 2, textY, 0xAAAAAA);
        textY += 10;
        graphics.drawCenteredString(this.font, Component.translatable("screen.creraces.badapple.credit_touhou"),
                this.width / 2, textY, 0x808080);
        textY += 10;
        graphics.drawCenteredString(this.font, Component.translatable("screen.creraces.badapple.credit_remix"),
                this.width / 2, textY, 0x808080);
        textY += 10;
        graphics.drawCenteredString(this.font, Component.translatable("screen.creraces.badapple.credit_cover"),
                this.width / 2, textY, 0x808080);
        textY += 10;
        graphics.drawCenteredString(this.font,
                Component.translatable("screen.creraces.badapple.credit_cover_author"), this.width / 2, textY,
                0x808080);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        stopPlayback();
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    @Override
    public void removed() {
        stopPlayback();
        super.removed();
    }

    private void stopPlayback() {
        if (sound != null && this.minecraft != null) {
            this.minecraft.getSoundManager().stop(sound);
            sound = null;
        }
        if (texture != null) {
            if (this.minecraft != null) {
                this.minecraft.getTextureManager().release(TEXTURE_LOCATION);
            }
            texture = null;
        }
    }
}
