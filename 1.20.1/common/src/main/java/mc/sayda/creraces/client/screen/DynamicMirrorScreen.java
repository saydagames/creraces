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
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import mc.sayda.creraces.world.inventory.MirrorMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.Minecraft;

/**
 * A dynamic screen for customizing racial cosmetics.
 */
public class DynamicMirrorScreen extends AbstractContainerScreen<MirrorMenu> {
    private static final ResourceLocation MIRROR_TEXTURE = new ResourceLocation("creraces",
            "textures/screens/mirror.png");
    private final Map<String, String> originalCustomizations = new HashMap<>();
    private final Map<String, String> tempCustomizations = new HashMap<>();
    private final Player player;
    private Race race;
    private float previewRotation = 0;
    private boolean saved = false;
    private boolean initializedRaceWidgets = false;

    public DynamicMirrorScreen(MirrorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.player = inventory.player;
        this.imageWidth = 256;
        this.imageHeight = 256;
    }

    @Override
    protected void init() {
        super.init();
        if (this.minecraft == null || this.minecraft.player == null)
            return;

        this.initializedRaceWidgets = false;
        setupRaceWidgets();

        // Rotation Buttons - static
        int mirrorCenterX = this.width / 2 + 60;
        int arrowY = (this.height / 2 - 95) + 160;

        Component backLabel = java.util.Objects.requireNonNull(Component.literal("<-"));
        addRenderableWidget(java.util.Objects.requireNonNull(Button.builder(backLabel, b -> previewRotation -= 90)
                .bounds(mirrorCenterX - 45, arrowY, 20, 20).build()));
        
        Component forwardLabel = java.util.Objects.requireNonNull(Component.literal("->"));
        addRenderableWidget(java.util.Objects.requireNonNull(Button.builder(forwardLabel, b -> previewRotation += 90)
                .bounds(mirrorCenterX + 25, arrowY, 20, 20).build()));

        int controlsY = this.height - 30;
        Component doneMsg = java.util.Objects.requireNonNull(Component.translatable("gui.done"));
        addRenderableWidget(java.util.Objects.requireNonNull(Button.builder(doneMsg, b -> save())
                .bounds(this.width / 2 - 100, controlsY, 90, 20).build()));
        
        Component cancelMsg = java.util.Objects.requireNonNull(Component.translatable("gui.cancel"));
        addRenderableWidget(java.util.Objects.requireNonNull(Button.builder(cancelMsg, b -> onClose())
                .bounds(this.width / 2 + 10, controlsY, 90, 20).build()));
    }

    private void setupRaceWidgets() {
        if (this.initializedRaceWidgets || minecraft == null || minecraft.player == null)
            return;

        DataUtils.getVariables(minecraft.player).ifPresent(vars -> {
            this.race = RaceRegistry.get(vars.getRace());
            if (this.race != null && this.race.customization() != null && !this.race.id().equals(RaceRegistry.NONE)) {
                this.initializedRaceWidgets = true;

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
                    Component label = java.util.Objects.requireNonNull(Component.translatable("cust.creraces." + cust.id()));

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
                        addRenderableWidget(java.util.Objects.requireNonNull(CycleButton
                                .builder((String val) -> java.util.Objects.requireNonNull(Component
                                        .translatable("gui.creraces.mirror." + cust.id() + "." + val)))
                                .withValues(cust.options())
                                .withInitialValue(initialValue)
                                .create(leftAnchorX, yOffset + 12, 140, 20,
                                        Component.empty(), (button, value) -> {
                                            tempCustomizations.put(cust.id(), value);
                                            vars.setCustomization(cust.id(), value);
                                            updatePreviewAddons(this.minecraft.player, vars);
                                        })));
                    }
                    yOffset += 40; // Spacing for title + widget
                }
                // Initial update for preview
                updatePreviewAddons(minecraft.player, vars);
            }
        });
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
                new java.util.ArrayList<>(vars.getCustomizations().keySet()).forEach(key -> vars.setCustomization(key, null));
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
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // No default background needed, we draw ours in render()
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // No labels needed
    }

    @Override
    @SuppressWarnings("null")
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!this.initializedRaceWidgets) {
            setupRaceWidgets();
        }

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
