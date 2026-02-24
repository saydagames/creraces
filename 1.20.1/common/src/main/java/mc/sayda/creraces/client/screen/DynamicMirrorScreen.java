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
                // Populate original state ONLY if empty (avoids revert on window resize)
                if (this.originalCustomizations.isEmpty()) {
                    this.originalCustomizations.putAll(vars.getCustomizations());
                    this.tempCustomizations.putAll(vars.getCustomizations());
                }

                // Calculate anchors to match render() logic
                int mirrorWidth = 128;
                int mirrorHeight = 240;
                int mirrorX = this.width / 2 + 60 - (mirrorWidth / 2);
                int mirrorY = this.height / 2 + 25 - (mirrorHeight / 2);

                // Position buttons/labels to the left of the mirror
                int leftAnchorX = mirrorX - 150;
                int yOffset = mirrorY + 10;

                for (RaceCustomization cust : this.race.customization()) {
                    if (cust.hidden())
                        continue;

                    // Use temp map for state
                    if (!tempCustomizations.containsKey(cust.id())) {
                        tempCustomizations.put(cust.id(), cust.defaultValue());
                    }

                    String initialValue = tempCustomizations.getOrDefault(cust.id(), cust.defaultValue());

                    // Widget Label component
                    Component label = Component.translatable("cust.creraces." + cust.id());

                    if (cust.options().isEmpty()) {
                        // Text Entry for Hex/Free choice
                        net.minecraft.client.gui.components.EditBox editBox = new net.minecraft.client.gui.components.EditBox(
                                this.font, leftAnchorX, yOffset + 12, 140, 20, label);
                        editBox.setValue(initialValue);
                        editBox.setResponder(value -> {
                            tempCustomizations.put(cust.id(), value);
                            vars.setCustomization(cust.id(), value);
                            updatePreviewAddons(minecraft.player, vars);
                        });
                        addRenderableWidget(editBox);
                    } else {
                        // Cycle Button for fixed options
                        addRenderableWidget(CycleButton
                                .builder((String val) -> Component
                                        .translatable("gui.creraces.mirror." + cust.id() + "." + val))
                                .withValues(cust.options())
                                .withInitialValue(initialValue)
                                .create(leftAnchorX, yOffset + 12, 140, 20,
                                        Component.empty(), (button, value) -> {
                                            tempCustomizations.put(cust.id(), value);
                                            vars.setCustomization(cust.id(), value);
                                            updatePreviewAddons(minecraft.player, vars);
                                        }));
                    }
                    yOffset += 40; // Spacing for title + widget
                }
                // Initial update for preview
                updatePreviewAddons(minecraft.player, vars);
            }
        });

        // Rotation Buttons
        int mirrorCenterX = this.width / 2 + 60;
        int arrowY = (this.height / 2 - 95) + 160;

        addRenderableWidget(Button.builder(Component.literal("<-"), b -> previewRotation -= 90)
                .bounds(mirrorCenterX - 45, arrowY, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("->"), b -> previewRotation += 90)
                .bounds(mirrorCenterX + 25, arrowY, 20, 20).build());

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
        if (!saved && minecraft != null && minecraft.player != null) {
            DataUtils.getVariables(minecraft.player).ifPresent(vars -> {
                // Clear all current temp changes
                vars.getCustomizations().keySet().forEach(key -> vars.setCustomization(key, null));
                // Restore old values
                originalCustomizations.forEach(vars::setCustomization);
                if (this.race != null) {
                    CosmeticIncidents.applyCustomizations(minecraft.player, originalCustomizations, this.race);
                }
            });
        }
        super.onClose();
    }

    @Override
    @SuppressWarnings("null")
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        int mirrorWidth = 128;
        int mirrorHeight = 240;
        int mirrorY = this.height / 2 + 25 - (mirrorHeight / 2);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, mirrorY - 15, 0xFFFFFF);

        if (minecraft != null && minecraft.player != null) {
            int mirrorX = this.width / 2 + 60 - (mirrorWidth / 2);

            RenderSystem.setShaderTexture(0, MIRROR_TEXTURE);
            graphics.blit(MIRROR_TEXTURE, mirrorX, mirrorY, 0, 0, mirrorWidth, mirrorHeight, 128, 240);

            Player player = minecraft.player;
            float scale = 55.0F;
            int x = this.width / 2 + 60;
            int y = mirrorY + 128;

            float mouseYawOffset = (float) Math.atan((x - mouseX) / 40.0F);
            float mousePitchOffset = (float) Math.atan((y - mouseY) / 40.0F);

            float oldYRot = player.getYRot();
            float oldXRot = player.getXRot();
            float oldYBodyRot = player.yBodyRot;
            float oldYHeadRot = player.yHeadRot;
            float oldYHeadRotO = player.yHeadRotO;

            player.yBodyRot = 180.0F + previewRotation + (mouseYawOffset * 20.0F);
            player.setYRot(180.0F + previewRotation + (mouseYawOffset * 40.0F));
            player.setXRot(-mousePitchOffset * 20.0F);
            player.yHeadRot = player.getYRot();
            player.yHeadRotO = player.getYRot();

            Quaternionf rotation = new Quaternionf().rotationZ((float) Math.PI);
            rotation.mul(new Quaternionf().rotationX((float) Math.toRadians(-10)));

            InventoryScreen.renderEntityInInventory(graphics, x, y, (int) scale, rotation, new Quaternionf(), player);

            // Draw Titles above widgets
            if (this.race != null && this.race.customization() != null) {
                int leftX = mirrorX - 150;
                int labelY = mirrorY + 10;
                for (RaceCustomization cust : this.race.customization()) {
                    if (cust.hidden())
                        continue;
                    graphics.drawString(this.font, Component.translatable("cust.creraces." + cust.id()), leftX, labelY,
                            0xFFAAAAAA);
                    labelY += 40;
                }
            }

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
        CosmeticIncidents.applyCustomizations(player, tempCustomizations, this.race);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
