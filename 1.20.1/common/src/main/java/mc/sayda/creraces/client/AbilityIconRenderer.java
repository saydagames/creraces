package mc.sayda.creraces.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Shared utility for rendering ability icons uniformly across all UI surfaces.
 *
 * An icon can be either an item ID, e.g. {@code "minecraft:feather"}, rendered
 * via {@link GuiGraphics#renderItem}, or an explicit texture path containing
 * {@code "textures/"} or ending in {@code ".png"}, rendered via
 * {@link GuiGraphics#blit}. Both are drawn at {@code size × size} pixels.
 */
public final class AbilityIconRenderer {

    private AbilityIconRenderer() {
    }

    /**
     * Render an ability icon at the given screen position.
     *
     * @param graphics current GuiGraphics
     * @param icon     the {@code icon} field from the
     *                 {@link mc.sayda.creraces.ability.Ability} record
     * @param x        left pixel of the icon slot
     * @param y        top pixel of the icon slot
     * @param size     width and height in pixels
     */
    public static void render(GuiGraphics graphics, ResourceLocation icon, int x, int y, int size) {
        if (icon == null)
            return;

        if (isTexturePath(icon)) {
            // Direct texture blit - used for custom PNGs not tied to an item model
            graphics.blit(icon, x, y, 0, 0, size, size, size, size);
        } else {
            // Item ID path - let MC's item renderer pick the right model/foil/animation.
            // renderItem() always draws at 16×16, so we scale the PoseStack so the icon
            // fills the requested size slot (e.g. 24px on the skill wheel).
            Item item = BuiltInRegistries.ITEM.get(icon);
            if (item == null || item == Items.AIR) {
                item = Items.BARRIER;
            }
            float scale = size / 16f;
            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(x, y, 0);
            pose.scale(scale, scale, 1f);
            graphics.renderItem(new ItemStack(item), 0, 0);
            pose.popPose();
        }
    }

    /**
     * Same as {@link #render(GuiGraphics, ResourceLocation, int, int, int)}, tinted with an RGB
     * colour. Covers both render paths: the texture-blit path reads the shader colour the same
     * way GuiGraphics.blit always has, and the item path's entity/item shaders multiply their
     * output by the same ColorModulator uniform, so both end up tinted the same way.
     *
     * @param color 0xRRGGBB tint; the alpha byte, if any, is ignored
     */
    public static void render(GuiGraphics graphics, ResourceLocation icon, int x, int y, int size, int color) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        RenderSystem.setShaderColor(r, g, b, 1f);
        render(graphics, icon, x, y, size);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    public static boolean isTexturePath(ResourceLocation icon) {
        String path = icon.getPath();
        return path.contains("textures/") || path.endsWith(".png");
    }

    /** Draws the upgrade-level badge (levels 1 to 5) over an icon. */
    public static void renderLevel(GuiGraphics graphics, int level, int x, int y, int size) {
        if (level < 1 || level > 5)
            return;
        ResourceLocation overlay = new ResourceLocation("creraces", "textures/item/upgrade_" + level + ".png");
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(0, 0, 200);
        graphics.blit(overlay, x, y, 0, 0, size, size, size, size);
        pose.popPose();
    }
}
