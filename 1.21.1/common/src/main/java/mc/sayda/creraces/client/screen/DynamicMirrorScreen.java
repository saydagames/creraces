package mc.sayda.creraces.client.screen;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.network.SetCustomizationPacket;
import mc.sayda.creraces.race.CosmeticIncidents;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceCustomization;
import mc.sayda.creraces.race.RaceRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Mirror for the race's cosmetic customizations, with a live preview of the player. Edits apply
 * to the player immediately for the preview; Done sends them to the server, Cancel restores the
 * values the screen opened with.
 */
@SuppressWarnings("null")
public class DynamicMirrorScreen extends Screen {
    private static final ResourceLocation MIRROR_TEXTURE = ResourceLocation.fromNamespaceAndPath("creraces",
            "textures/screens/mirror.png");
    private static final int MIRROR_WIDTH = 128;
    private static final int MIRROR_HEIGHT = 240;
    /** The mirror sits this far right of the screen centre, leaving room for the options on its left. */
    private static final int MIRROR_OFFSET_X = 60;
    private static final int OPTION_WIDTH = 140;
    /** Vertical space per option: its label, then its widget. */
    private static final int OPTION_SPACING = 40;

    private final Map<String, String> originalCustomizations = new HashMap<>();
    private final Map<String, String> tempCustomizations = new HashMap<>();
    private Race race;
    private float previewRotation = 0;
    private boolean saved = false;
    private boolean initializedRaceWidgets = false;
    private boolean originalsCaptured = false;

    public DynamicMirrorScreen() {
        super(Component.translatable("screen.creraces.mirror"));
    }

    @Override
    protected void init() {
        super.init();
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }

        this.initializedRaceWidgets = false;
        setupRaceWidgets();

        int arrowY = mirrorTop() + 160;
        addRenderableWidget(Button.builder(Component.literal("<-"), b -> previewRotation -= 90)
                .bounds(mirrorCenterX() - 45, arrowY, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("->"), b -> previewRotation += 90)
                .bounds(mirrorCenterX() + 25, arrowY, 20, 20).build());

        int controlsY = this.height - 30;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> save())
                .bounds(this.width / 2 - 100, controlsY, 90, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(this.width / 2 + 10, controlsY, 90, 20).build());
    }

    private int mirrorCenterX() {
        return this.width / 2 + MIRROR_OFFSET_X;
    }

    private int mirrorTop() {
        return this.height / 2 + 25 - MIRROR_HEIGHT / 2;
    }

    private int optionsLeft() {
        return mirrorCenterX() - MIRROR_WIDTH / 2 - 150;
    }

    /** Adds a widget per visible customization; render() retries until the player's race data is in. */
    private void setupRaceWidgets() {
        if (this.initializedRaceWidgets || minecraft == null || minecraft.player == null) {
            return;
        }

        DataUtils.getVariables(minecraft.player).ifPresent(vars -> {
            this.race = RaceRegistry.get(vars.getRace());
            if (this.race == null || this.race.customization() == null || this.race.id().equals(RaceRegistry.NONE)) {
                return;
            }
            this.initializedRaceWidgets = true;

            // Only on first open, so a resize (which re-runs init) does not lose the edits so far.
            if (!this.originalsCaptured) {
                this.originalsCaptured = true;
                this.originalCustomizations.putAll(vars.getCustomizations());
                this.tempCustomizations.putAll(vars.getCustomizations());
            }

            int y = mirrorTop() + 10;
            for (RaceCustomization cust : this.race.customization()) {
                if (cust.hidden()) {
                    continue;
                }
                if (!tempCustomizations.containsKey(cust.id())) {
                    tempCustomizations.put(cust.id(), cust.defaultValue());
                }
                String value = tempCustomizations.get(cust.id());
                if (cust.options().isEmpty()) {
                    addRenderableWidget(optionTextBox(vars, cust, value, optionsLeft(), y + 12));
                } else {
                    addRenderableWidget(optionCycleButton(vars, cust, value, optionsLeft(), y + 12));
                }
                y += OPTION_SPACING;
            }
            applyPreview(minecraft.player);
        });
    }

    /** Options without a fixed list, such as a hex colour, are typed in. */
    private EditBox optionTextBox(IPlayerVariables vars, RaceCustomization cust, String value, int x, int y) {
        EditBox box = new EditBox(this.font, x, y, OPTION_WIDTH, 20,
                Component.translatable("cust.creraces." + cust.id()));
        box.setValue(value);
        box.setResponder(newValue -> setOption(vars, cust.id(), newValue));
        return box;
    }

    private CycleButton<String> optionCycleButton(IPlayerVariables vars, RaceCustomization cust, String value,
            int x, int y) {
        return CycleButton.builder((String option) -> Component.translatable(
                        "gui.creraces.mirror." + cust.id() + "." + option))
                .withValues(cust.options())
                .withInitialValue(value)
                .create(x, y, OPTION_WIDTH, 20, Component.empty(),
                        (button, newValue) -> setOption(vars, cust.id(), newValue));
    }

    private void setOption(IPlayerVariables vars, String id, String value) {
        tempCustomizations.put(id, value);
        vars.setCustomization(id, value);
        applyPreview(minecraft.player);
    }

    private void applyPreview(Player player) {
        if (this.race != null) {
            CosmeticIncidents.applyCustomizations(player, tempCustomizations, this.race);
        }
    }

    private void save() {
        this.saved = true;
        BoundaryHandler.sendSetCustomization(new SetCustomizationPacket(tempCustomizations));
        onClose();
    }

    @Override
    public void onClose() {
        if (!saved && minecraft != null && minecraft.player != null) {
            // Cancelled: drop the previewed values and put back the ones the screen opened with.
            DataUtils.getVariables(minecraft.player).ifPresent(vars -> {
                new ArrayList<>(vars.getCustomizations().keySet()).forEach(key -> vars.setCustomization(key, null));
                originalCustomizations.forEach(vars::setCustomization);
                if (this.race != null) {
                    CosmeticIncidents.applyCustomizations(minecraft.player, originalCustomizations, this.race);
                }
            });
        }
        super.onClose();
    }

    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!this.initializedRaceWidgets) {
            setupRaceWidgets();
        }

        super.renderBackground(graphics, mouseX, mouseY, partialTick);

        int mirrorTop = mirrorTop();
        graphics.drawCenteredString(this.font, this.title, this.width / 2, mirrorTop - 15, 0xFFFFFF);

        if (minecraft != null && minecraft.player != null) {
            int mirrorLeft = mirrorCenterX() - MIRROR_WIDTH / 2;
            graphics.blit(MIRROR_TEXTURE, mirrorLeft, mirrorTop, 0, 0, MIRROR_WIDTH, MIRROR_HEIGHT, 128, 240);
            renderPlayerPreview(graphics, minecraft.player, mouseX, mouseY);
            renderOptionLabels(graphics);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /** The player in the mirror, turned by the arrow buttons and leaning a little towards the mouse. */
    private void renderPlayerPreview(GuiGraphics graphics, Player player, int mouseX, int mouseY) {
        float scale = 55.0F;
        int x = mirrorCenterX();
        int y = mirrorTop() + 128;
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
        InventoryScreen.renderEntityInInventory(graphics, x, y, scale, new Vector3f(), rotation, new Quaternionf(),
                player);

        player.setYRot(oldYRot);
        player.setXRot(oldXRot);
        player.yBodyRot = oldYBodyRot;
        player.yHeadRot = oldYHeadRot;
        player.yHeadRotO = oldYHeadRotO;
    }

    private void renderOptionLabels(GuiGraphics graphics) {
        if (this.race == null || this.race.customization() == null) {
            return;
        }
        int y = mirrorTop() + 10;
        for (RaceCustomization cust : this.race.customization()) {
            if (cust.hidden()) {
                continue;
            }
            graphics.drawString(this.font, Component.translatable("cust.creraces." + cust.id()), optionsLeft(), y,
                    0xFFAAAAAA);
            y += OPTION_SPACING;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    // Drawn at the top of render() instead, so Screen.render() does not blur over the mirror.
    @Override
    public void renderBackground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }
}
