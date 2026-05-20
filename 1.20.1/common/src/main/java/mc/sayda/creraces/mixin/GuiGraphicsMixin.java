package mc.sayda.creraces.mixin;

import mc.sayda.creraces.item.ScrollItem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {

    @Inject(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("TAIL"))
    private void creraces$renderScrollLevelOverlay(Font font, ItemStack stack, int x, int y, String text, CallbackInfo ci) {
        if (stack.getItem() instanceof ScrollItem) {
            int level = ScrollItem.getLevel(stack);
            if (level > 0) {
                mc.sayda.creraces.client.AbilityIconRenderer.renderLevel((GuiGraphics) (Object) this, level, x, y, 16);
            }
        }
    }
}
