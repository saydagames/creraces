package mc.sayda.creraces.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceCustomization;
import mc.sayda.creraces.race.RaceRegistry;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.network.SetCustomizationPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import org.joml.Quaternionf;
import mc.sayda.creraces.race.CosmeticIncidents;
import net.minecraft.world.entity.player.Player;
import mc.sayda.creraces.capability.IPlayerVariables;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * A dynamic screen for customizing racial cosmetics.
 */
public class DynamicMirrorScreen extends Screen {
    private static final ResourceLocation MIRROR_TEXTURE = new ResourceLocation("creraces",
            "textures/screens/mirror.png");
    private final Map<String, String> originalCustomizations = new HashMap<>();
    private final Map<String, String> tempCustomizations = new HashMap<>();
    private Race race;
    private float previewRotation = 0;
    private boolean saved = false;

    public DynamicMirrorScreen() {
        super(Component.translatable("creraces.screen.mirror"));
    }

    @Override
    @SuppressWarnings("null")
    protected void init() {
        if (minecraft == null || minecraft.player == null)
            return;

        DataUtils.getVariables(minecraft.player).ifPresent(vars -> {
            this.race = RaceRegistry.get(vars.getRace());
            if (this.race != null && this.race.customization() != null) {
                this.originalCustomizations.clear();
                this.originalCustomizations.putAll(vars.getCustomizations());
                this.tempCustomizations.clear();
                this.tempCustomizations.putAll(vars.getCustomizations());

                // Calculate anchors to match render() logic
                int mirrorWidth = 128;
                int mirrorHeight = 240;
                // Mirror center is at width/2 + 60
                int mirrorX = this.width / 2 + 60 - (mirrorWidth / 2);
                int mirrorY = this.height / 2 + 25 - (mirrorHeight / 2);

                // Position buttons to the left of the mirror
                int leftAnchorX = mirrorX - 150; // 150px left of mirror left edge
                int startY = mirrorY + 20; // Start slightly down from mirror top

                int yOffset = startY;
                for (RaceCustomization cust : this.race.customization()) {
                    if (cust.hidden())
                        continue;

                    // Ensure defaults are present in temp map if missing
                    if (!tempCustomizations.containsKey(cust.id())) {
                        tempCustomizations.put(cust.id(), cust.defaultValue());
                    }

                    String initialValue = tempCustomizations.getOrDefault(cust.id(), cust.defaultValue());

                    addRenderableWidget(CycleButton
                            .builder((String val) -> Component
                                    .translatable("gui.creraces.mirror." + cust.id() + "." + val))
                            .withValues(cust.options())
                            .withInitialValue(initialValue)
                            .create(leftAnchorX, yOffset, 140, 20,
                                    Component.translatable("cust.creraces." + cust.id()), (button, value) -> {
                                        tempCustomizations.put(cust.id(), value);
                                        // Live Update: apply to local variables immediately
                                        vars.setCustomization(cust.id(), value);
                                        // Bridge to Twilight Lib for visual preview
                                        updatePreviewAddons(minecraft.player, vars);
                                    }));
                    yOffset += 25;
                }
                // Initial update for preview
                updatePreviewAddons(minecraft.player, vars);
            }
        });

        // Rotation Buttons - Anchored to Player Render Position
        int mirrorCenterX = this.width / 2 + 60;
        // Mirror Y start is height/2 - 95. +180 puts it near bottom of model
        // (Model is at +128, feet around there).
        // Let's go +190 from mirror top.
        int arrowY = (this.height / 2 - 95) + 160;

        addRenderableWidget(Button.builder(Component.literal("<-"), b -> previewRotation -= 90)
                .bounds(mirrorCenterX - 45, arrowY, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("->"), b -> previewRotation += 90)
                .bounds(mirrorCenterX + 25, arrowY, 20, 20).build());

        // Save & Cancel Buttons - Restored to bottom of screen
        int controlsY = this.height - 30;

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> save())
                .bounds(this.width / 2 - 100, controlsY, 90, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(this.width / 2 + 10, controlsY, 90, 20).build());
    }

    private void save() {
        this.saved = true;
        BoundaryHandler.sendSetCustomization(new SetCustomizationPacket(tempCustomizations));
        onClose();
    }

    @Override
    public void onClose() {
        // Revert live changes if we didn't save
        if (!saved && minecraft != null && minecraft.player != null) {
            DataUtils.getVariables(minecraft.player).ifPresent(vars -> {
                originalCustomizations.forEach(vars::setCustomization);
                // Also remove any new keys that weren't in the original if any were added
                // Re-sync visual preview to original
                if (this.race != null) {
                    CosmeticIncidents.applyCustomizations(minecraft.player, originalCustomizations, this.race);
                }
            });
        }
        super.onClose();
    }

    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        // Title - anchored to top, might desync with centered UI?
        // Let's move title to slightly above the mirror frame top
        int mirrorWidth = 128;
        int mirrorHeight = 240;
        int mirrorY = this.height / 2 + 25 - (mirrorHeight / 2);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, mirrorY - 15, 0xFFFFFF);

        if (minecraft != null && minecraft.player != null) {
            // Render the mirror frame background FIRST to be behind buttons
            int mirrorX = this.width / 2 + 60 - (mirrorWidth / 2);

            RenderSystem.setShaderTexture(0, MIRROR_TEXTURE);
            graphics.blit(MIRROR_TEXTURE, mirrorX, mirrorY, 0, 0, mirrorWidth, mirrorHeight, 128, 240);

            // Preview the player with hybrid rotation (mouse sway + button rotation)
            Player player = minecraft.player;
            float scale = 55.0F;
            int x = this.width / 2 + 60;
            // Legacy render pos was topPos + 108.
            // Relative to mirror start, that's +128px down.
            int y = mirrorY + 128;

            // Calculate mouse offsets for "follow mouse" effect
            float mouseYawOffset = (float) Math.atan((x - mouseX) / 40.0F);
            float mousePitchOffset = (float) Math.atan((y - mouseY) / 40.0F);

            // Store original rotations
            float oldYRot = player.getYRot();
            float oldXRot = player.getXRot();
            float oldYBodyRot = player.yBodyRot;
            float oldYHeadRot = player.yHeadRot;
            float oldYHeadRotO = player.yHeadRotO;

            // Apply hybrid rotation
            // 180 is "front" in InventoryScreen.
            // previewRotation is the base offset from buttons.
            player.yBodyRot = 180.0F + previewRotation + (mouseYawOffset * 20.0F);
            player.setYRot(180.0F + previewRotation + (mouseYawOffset * 40.0F));
            player.setXRot(-mousePitchOffset * 20.0F);
            player.yHeadRot = player.getYRot();
            player.yHeadRotO = player.getYRot();

            // Render with base flip (standard for InventoryScreen)
            Quaternionf rotation = new Quaternionf().rotationZ((float) Math.PI);
            // Tilt slightly to see the model better
            rotation.mul(new Quaternionf().rotationX((float) Math.toRadians(-10)));

            InventoryScreen.renderEntityInInventory(graphics, x, y, (int) scale, rotation, null, player);

            // Restore original rotations immediately
            player.setYRot(oldYRot);
            player.setXRot(oldXRot);
            player.yBodyRot = oldYBodyRot;
            player.yHeadRot = oldYHeadRot;
            player.yHeadRotO = oldYHeadRotO;
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void updatePreviewAddons(Player player, IPlayerVariables vars) {
        if (this.race == null)
            return;

        // Use central logic to ensure consistency, passing the TEMP map which has
        // current state + defaults
        CosmeticIncidents.applyCustomizations(player, tempCustomizations, this.race);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
