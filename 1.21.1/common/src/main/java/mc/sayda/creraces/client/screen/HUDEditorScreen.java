package mc.sayda.creraces.client.screen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.client.RaceOverlay;
import mc.sayda.creraces.config.CreRacesConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Drag-and-drop editor for the race HUD. Every change is pushed straight into the CreRacesConfig
 * suppliers so RaceOverlay previews it live; Done saves the config, Escape restores the values the
 * editor opened with.
 */
public class HUDEditorScreen extends Screen {
    private static final String[] LABEL_MODES = { "name_value", "name", "value", "hidden" };
    private static final String[] SLOT_LABEL_MODES = { "below", "side", "top", "left", "none" };
    private static final String DEFAULT_SLOT_LABEL_MODE = "below";
    private static final double MIN_SCALE = 0.1;
    private static final double MAX_SCALE = 5.0;

    /** Draggable parts of the HUD. The anchor moves all of them and is grabbed from empty space. */
    private enum Group {
        PORTRAIT(0xFF88AAFF, 0x554466CC, "gui.creraces.hud_editor.group.portrait"),
        ABILITIES(0xFF88FFAA, 0x5544CC66, "gui.creraces.hud_editor.group.abilities"),
        BARS(0xFFFFCC66, 0x55CC8833, "gui.creraces.hud_editor.group.bars"),
        ANCHOR(0, 0, "");

        static final Group[] OUTLINED = { PORTRAIT, ABILITIES, BARS };

        final int selectedColor;
        final int idleColor;
        final String labelKey;

        Group(int selectedColor, int idleColor, String labelKey) {
            this.selectedColor = selectedColor;
            this.idleColor = idleColor;
            this.labelKey = labelKey;
        }
    }

    private record Box(int x, int y, int width, int height) {
        boolean contains(double mx, double my) {
            return mx >= x && mx <= x + width && my >= y && my <= y + height;
        }
    }

    private final HudLayout saved = HudLayout.fromConfig();
    private HudLayout layout = saved.copy();
    /** One-step undo: Undo swaps this with the live layout, so pressing it again redoes. */
    @Nullable
    private HudLayout history;

    @Nullable
    private Group dragging;
    @Nullable
    private Group lastSelected;
    private double dragStartMouseX, dragStartMouseY;
    private int dragStartX, dragStartY;

    private Button labelModeButton, timeButton, barsDirButton, abilitiesDirButton, slotLabelButton;
    private EditBox scaleBox;
    private final List<AbstractWidget> toggleableWidgets = new ArrayList<>();
    private boolean widgetsVisible = true;

    public HUDEditorScreen() {
        super(Component.translatable("gui.creraces.hud_editor.title"));
    }

    @Override
    protected void init() {
        layout.pushToConfig();
        toggleableWidgets.clear();

        int row0Y = this.height - 72;
        int row1Y = this.height - 48;
        int row2Y = this.height - 24;

        // Row 0: presets and scale
        addToggleable(Button.builder(Component.translatable("gui.creraces.hud_editor.btn.save_preset"),
                btn -> savePreset()).bounds(8, row0Y, 110, 20).build());
        addToggleable(Button.builder(Component.translatable("gui.creraces.hud_editor.btn.load_preset"),
                btn -> loadPreset()).bounds(122, row0Y, 110, 20).build());

        // Typed as a plain multiplier, 1.0 being 100%.
        scaleBox = new EditBox(this.font, 236, row0Y, 40, 20, Component.literal("Scale"));
        scaleBox.setMaxLength(7);
        scaleBox.setValue(formatScale(layout.hudScale));
        scaleBox.setHint(Component.literal("1.0000"));
        addToggleable(scaleBox);

        // Row 1: layout toggles
        barsDirButton = addToggleable(layoutButton(this::barsDirLabel,
                () -> layout.barsGrowUp = !layout.barsGrowUp).bounds(8, row1Y, 110, 20).build());
        abilitiesDirButton = addToggleable(layoutButton(this::abilitiesDirLabel,
                () -> layout.abilitiesVertical = !layout.abilitiesVertical).bounds(122, row1Y, 130, 20).build());
        slotLabelButton = addToggleable(layoutButton(this::slotLabelLabel,
                () -> layout.slotLabelOrientation = nextInCycle(SLOT_LABEL_MODES, layout.slotLabelOrientation))
                .bounds(256, row1Y, 120, 20).build());

        // Row 2: bar label and time format, then undo, reset and done
        labelModeButton = addToggleable(layoutButton(this::labelModeLabel,
                () -> layout.labelMode = nextInCycle(LABEL_MODES, layout.labelMode)).bounds(8, row2Y, 120, 20).build());
        timeButton = addToggleable(layoutButton(this::timeLabel,
                () -> layout.showSeconds = !layout.showSeconds).bounds(132, row2Y, 90, 20).build());
        addToggleable(Button.builder(Component.translatable("gui.creraces.hud_editor.btn.undo"),
                btn -> swapSnapshot()).bounds(226, row2Y, 50, 20).build());
        addToggleable(Button.builder(Component.translatable("gui.creraces.hud_editor.btn.reset"), btn -> {
            saveSnapshot();
            applyLayout(HudLayout.defaults());
        }).bounds(280, row2Y, 50, 20).build());
        addToggleable(Button.builder(Component.translatable("gui.creraces.hud_editor.btn.done"),
                btn -> saveAndClose()).bounds(334, row2Y, 50, 20).build());

        // Always visible, so the hidden controls can be brought back.
        this.addRenderableWidget(Button.builder(Component.literal("T"), btn -> {
            widgetsVisible = !widgetsVisible;
            applyWidgetVisibility();
        }).bounds(this.width - 18, 4, 14, 14).build());

        // Re-applied on every init so a resize keeps the controls hidden.
        applyWidgetVisibility();
    }

    private <T extends AbstractWidget> T addToggleable(T widget) {
        this.addRenderableWidget(widget);
        toggleableWidgets.add(widget);
        return widget;
    }

    private void applyWidgetVisibility() {
        toggleableWidgets.forEach(widget -> widget.visible = widgetsVisible);
    }

    /** A button that changes one layout setting, recording undo history and relabelling itself. */
    private Button.Builder layoutButton(Supplier<Component> label, Runnable change) {
        return Button.builder(label.get(), btn -> {
            saveSnapshot();
            change.run();
            layout.pushToConfig();
            btn.setMessage(label.get());
        });
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        resetBlendState();
        graphics.fill(0, 0, this.width, this.height, 0x55000000);
        // fill() switches blending off when it flushes, so restore it before RaceOverlay blits.
        resetBlendState();

        RaceOverlay.render(graphics, partialTick);
        renderExampleBar(graphics);

        for (Group group : Group.OUTLINED) {
            Box box = groupBox(group);
            graphics.renderOutline(box.x(), box.y(), box.width(), box.height(),
                    lastSelected == group ? group.selectedColor : group.idleColor);
        }
        if (lastSelected != null && lastSelected != Group.ANCHOR) {
            Box box = groupBox(lastSelected);
            graphics.drawString(this.font, Component.translatable(lastSelected.labelKey), box.x(), box.y() - 10,
                    lastSelected.selectedColor, true);
        }

        if (widgetsVisible) {
            graphics.drawString(this.font, Component.translatable("gui.creraces.hud_editor.hint"), 4, 4,
                    0xFFFFFFFF, false);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
        RenderSystem.disableBlend();
    }

    private static void resetBlendState() {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    /**
     * The bars zone stays empty until a real bar has a value, so an example bar previews where it
     * goes, scaled around the anchor the same way RaceOverlay scales the HUD.
     */
    private void renderExampleBar(GuiGraphics graphics) {
        graphics.pose().pushPose();
        if (layout.hudScale != 1.0) {
            graphics.pose().translate(layout.anchorX, layout.anchorY, 0);
            graphics.pose().scale((float) layout.hudScale, (float) layout.hudScale, 1.0f);
            graphics.pose().translate(-layout.anchorX, -layout.anchorY, 0);
        }
        RaceOverlay.renderExampleBar(graphics, layout.anchorX + layout.barsX, layout.anchorY + layout.barsY);
        graphics.pose().popPose();
    }

    /** A group's outline in screen space, which is also its drag handle. */
    private Box groupBox(Group group) {
        int x = toScreenX(layout.anchorX + groupX(group));
        int y = toScreenY(layout.anchorY + groupY(group));
        return switch (group) {
            case PORTRAIT -> new Box(x - 4, y - 1, scaled(44), scaled(44));
            case ABILITIES -> {
                String labels = layout.slotLabelOrientation;
                int width = scaled(layout.abilitiesVertical ? (labels.equals("side") ? 72 : 22) : 130);
                int height = scaled(layout.abilitiesVertical ? 5 * abilitySlotStep()
                        : (labels.equals("below") || labels.equals("top") ? 30 : 22));
                yield new Box(x - 2, y - 2, width + 4, height + 4);
            }
            case BARS -> {
                int top = scaled(layout.barsGrowUp ? -(5 * 9) : -12);
                int bottom = scaled(layout.barsGrowUp ? 4 : 40);
                yield new Box(x - 2, y + top, scaled(126), bottom - top);
            }
            case ANCHOR -> throw new IllegalArgumentException("The anchor has no outline");
        };
    }

    /** Matches RaceOverlay's spacing between vertically stacked ability slots. */
    private int abilitySlotStep() {
        return switch (layout.slotLabelOrientation) {
            case "side", "left" -> 25;
            case "none" -> 22;
            default -> 30;
        };
    }

    private int groupX(Group group) {
        return switch (group) {
            case PORTRAIT -> layout.portraitX;
            case ABILITIES -> layout.abilitiesX;
            case BARS -> layout.barsX;
            case ANCHOR -> layout.anchorX;
        };
    }

    private int groupY(Group group) {
        return switch (group) {
            case PORTRAIT -> layout.portraitY;
            case ABILITIES -> layout.abilitiesY;
            case BARS -> layout.barsY;
            case ANCHOR -> layout.anchorY;
        };
    }

    private void moveGroup(Group group, int x, int y) {
        switch (group) {
            case PORTRAIT -> { layout.portraitX = x; layout.portraitY = y; }
            case ABILITIES -> { layout.abilitiesX = x; layout.abilitiesY = y; }
            case BARS -> { layout.barsX = x; layout.barsY = y; }
            case ANCHOR -> { layout.anchorX = x; layout.anchorY = y; }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Buttons first, so clicking one never starts a drag.
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (button == 0) {
            for (Group group : Group.OUTLINED) {
                if (groupBox(group).contains(mouseX, mouseY)) {
                    startDrag(group, mouseX, mouseY);
                    return true;
                }
            }
            startDrag(Group.ANCHOR, mouseX, mouseY);
        }
        return false;
    }

    private void startDrag(Group group, double mouseX, double mouseY) {
        saveSnapshot();
        dragging = group;
        lastSelected = group;
        dragStartMouseX = mouseX;
        dragStartMouseY = mouseY;
        dragStartX = groupX(group);
        dragStartY = groupY(group);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (button == 0 && dragging != null) {
            // Group offsets are in HUD space, so the mouse delta is unscaled; the anchor is in screen space.
            double deltaX = mouseX - dragStartMouseX;
            double deltaY = mouseY - dragStartMouseY;
            if (dragging == Group.ANCHOR) {
                moveGroup(dragging, dragStartX + (int) deltaX, dragStartY + (int) deltaY);
            } else {
                moveGroup(dragging, dragStartX + toHudDelta(deltaX), dragStartY + toHudDelta(deltaY));
            }
            layout.pushToConfig();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = null;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // While the scale box has focus it gets the keys, so the cursor and delete keys work.
        if (scaleBox.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                applyScaleFromBox();
                scaleBox.setFocused(false);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                cancelAndClose();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        int dx = 0;
        int dy = 0;
        if (keyCode == GLFW.GLFW_KEY_LEFT) dx = -1;
        else if (keyCode == GLFW.GLFW_KEY_RIGHT) dx = 1;
        else if (keyCode == GLFW.GLFW_KEY_UP) dy = -1;
        else if (keyCode == GLFW.GLFW_KEY_DOWN) dy = 1;

        if (dx != 0 || dy != 0) {
            if (lastSelected != null) {
                moveGroup(lastSelected, groupX(lastSelected) + dx, groupY(lastSelected) + dy);
            }
            layout.pushToConfig();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            cancelAndClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void applyScaleFromBox() {
        try {
            double value = Double.parseDouble(scaleBox.getValue().trim());
            saveSnapshot();
            layout.hudScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, value));
            layout.pushToConfig();
        } catch (NumberFormatException ignored) {
            // Unparseable input: the box is reset to the current scale below.
        }
        scaleBox.setValue(formatScale(layout.hudScale));
    }

    private void saveSnapshot() {
        history = layout.copy();
    }

    private void swapSnapshot() {
        if (history == null) {
            return;
        }
        HudLayout previous = history;
        history = layout;
        applyLayout(previous);
    }

    private void applyLayout(HudLayout newLayout) {
        layout = newLayout;
        layout.pushToConfig();
        labelModeButton.setMessage(labelModeLabel());
        timeButton.setMessage(timeLabel());
        barsDirButton.setMessage(barsDirLabel());
        abilitiesDirButton.setMessage(abilitiesDirLabel());
        slotLabelButton.setMessage(slotLabelLabel());
        scaleBox.setValue(formatScale(layout.hudScale));
    }

    private void saveAndClose() {
        layout.pushToConfig();
        CreRacesConfig.saveHudConfig();
        super.onClose();
    }

    private void cancelAndClose() {
        saved.pushToConfig();
        super.onClose();
    }

    @Override
    public void onClose() {
        cancelAndClose();
    }

    private static Path presetPath() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config/creraces/hud_preset.json");
    }

    private void savePreset() {
        try {
            Path path = presetPath();
            Files.createDirectories(path.getParent());
            try (FileWriter writer = new FileWriter(path.toFile())) {
                new GsonBuilder().setPrettyPrinting().create().toJson(layout.toJson(), writer);
            }
        } catch (Exception e) {
            CreRaces.LOGGER.warn("Failed to save HUD preset", e);
        }
    }

    private void loadPreset() {
        Path path = presetPath();
        if (!Files.exists(path)) {
            return;
        }
        try (FileReader reader = new FileReader(path.toFile())) {
            JsonObject json = new Gson().fromJson(reader, JsonObject.class);
            if (json == null) {
                return;
            }
            HudLayout loaded = HudLayout.fromJson(json, layout);
            saveSnapshot();
            applyLayout(loaded);
        } catch (Exception e) {
            // Hand-edited presets can fail in many ways (bad JSON, wrong value types); none should crash.
            CreRaces.LOGGER.warn("Failed to load HUD preset", e);
        }
    }

    private Component labelModeLabel() {
        String mode = Arrays.asList(LABEL_MODES).contains(layout.labelMode) ? layout.labelMode : LABEL_MODES[0];
        return Component.translatable("gui.creraces.hud_editor.btn.label",
                Component.translatable("gui.creraces.hud_editor.label." + mode));
    }

    private Component timeLabel() {
        return Component.translatable("gui.creraces.hud_editor.btn.time", Component.translatable(
                layout.showSeconds ? "gui.creraces.hud_editor.time.seconds" : "gui.creraces.hud_editor.time.ticks"));
    }

    private Component barsDirLabel() {
        return Component.translatable("gui.creraces.hud_editor.btn.bars_dir", Component.translatable(
                layout.barsGrowUp ? "gui.creraces.hud_editor.bars.up" : "gui.creraces.hud_editor.bars.down"));
    }

    private Component abilitiesDirLabel() {
        return Component.translatable("gui.creraces.hud_editor.btn.abilities_dir", Component.translatable(
                layout.abilitiesVertical ? "gui.creraces.hud_editor.abilities.vertical"
                        : "gui.creraces.hud_editor.abilities.horizontal"));
    }

    private Component slotLabelLabel() {
        return Component.translatable("gui.creraces.hud_editor.btn.slot_label",
                Component.translatable("gui.creraces.hud_editor.slot_label." + layout.slotLabelOrientation));
    }

    /** The mode after the current one, or the first mode if the current one is not in the list. */
    private static String nextInCycle(String[] modes, String current) {
        int index = Arrays.asList(modes).indexOf(current);
        return modes[(index + 1) % modes.length];
    }

    private static String formatScale(double scale) {
        return String.format(Locale.ROOT, "%.4f", scale);
    }

    // HUD offsets are scaled around the anchor; these convert them to and from screen space.
    private int toScreenX(int hudX) {
        return (int) Math.round((hudX - layout.anchorX) * layout.hudScale + layout.anchorX);
    }

    private int toScreenY(int hudY) {
        return (int) Math.round((hudY - layout.anchorY) * layout.hudScale + layout.anchorY);
    }

    private int scaled(int length) {
        return (int) Math.round(length * layout.hudScale);
    }

    private int toHudDelta(double screenDelta) {
        return (int) Math.round(screenDelta / layout.hudScale);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // No backdrop, as on 1.20.1. Screen.render() calls this itself on 1.21, and vanilla's would blur the HUD.
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    /** Every setting the editor changes, so the saved, live and undo states share one shape. */
    private static final class HudLayout {
        int anchorX, anchorY;
        int portraitX, portraitY;
        int abilitiesX, abilitiesY;
        int barsX, barsY;
        String labelMode;
        boolean showSeconds, barsGrowUp, abilitiesVertical;
        String slotLabelOrientation;
        double hudScale;

        static HudLayout fromConfig() {
            HudLayout layout = new HudLayout();
            layout.anchorX = CreRacesConfig.HUD_ANCHOR_X.get();
            layout.anchorY = CreRacesConfig.HUD_ANCHOR_Y.get();
            layout.portraitX = CreRacesConfig.HUD_PORTRAIT_X.get();
            layout.portraitY = CreRacesConfig.HUD_PORTRAIT_Y.get();
            layout.abilitiesX = CreRacesConfig.HUD_ABILITIES_X.get();
            layout.abilitiesY = CreRacesConfig.HUD_ABILITIES_Y.get();
            layout.barsX = CreRacesConfig.HUD_BARS_X.get();
            layout.barsY = CreRacesConfig.HUD_BARS_Y.get();
            layout.labelMode = CreRacesConfig.BAR_LABEL_MODE.get();
            layout.showSeconds = CreRacesConfig.BAR_SHOW_SECONDS.get();
            layout.barsGrowUp = CreRacesConfig.HUD_BARS_GROW_UP.get();
            layout.abilitiesVertical = CreRacesConfig.HUD_ABILITIES_VERTICAL.get();
            layout.slotLabelOrientation = CreRacesConfig.HUD_SLOT_LABEL_SIDE.get();
            layout.hudScale = CreRacesConfig.HUD_SCALE.get();
            return layout;
        }

        /** The same values CreRacesConfig starts with. */
        static HudLayout defaults() {
            HudLayout layout = new HudLayout();
            layout.anchorX = -12;
            layout.anchorY = -9;
            layout.portraitX = 14;
            layout.portraitY = 13;
            layout.abilitiesX = 54;
            layout.abilitiesY = 17;
            layout.barsX = 16;
            layout.barsY = 62;
            layout.labelMode = "name_value";
            layout.showSeconds = true;
            layout.barsGrowUp = false;
            layout.abilitiesVertical = false;
            layout.slotLabelOrientation = DEFAULT_SLOT_LABEL_MODE;
            layout.hudScale = 1.0;
            return layout;
        }

        /** Preset keys missing from the file keep the fallback's values. */
        static HudLayout fromJson(JsonObject json, HudLayout fallback) {
            HudLayout layout = fallback.copy();
            layout.anchorX = intOr(json, "hud_anchor_x", layout.anchorX);
            layout.anchorY = intOr(json, "hud_anchor_y", layout.anchorY);
            layout.portraitX = intOr(json, "hud_portrait_x", layout.portraitX);
            layout.portraitY = intOr(json, "hud_portrait_y", layout.portraitY);
            layout.abilitiesX = intOr(json, "hud_abilities_x", layout.abilitiesX);
            layout.abilitiesY = intOr(json, "hud_abilities_y", layout.abilitiesY);
            layout.barsX = intOr(json, "hud_bars_x", layout.barsX);
            layout.barsY = intOr(json, "hud_bars_y", layout.barsY);
            layout.labelMode = stringOr(json, "bar_label_mode", layout.labelMode);
            layout.showSeconds = booleanOr(json, "bar_show_seconds", layout.showSeconds);
            layout.barsGrowUp = booleanOr(json, "hud_bars_grow_up", layout.barsGrowUp);
            layout.abilitiesVertical = booleanOr(json, "hud_abilities_vertical", layout.abilitiesVertical);
            String slotLabels = stringOr(json, "hud_slot_label_side", layout.slotLabelOrientation);
            layout.slotLabelOrientation = Arrays.asList(SLOT_LABEL_MODES).contains(slotLabels)
                    ? slotLabels : DEFAULT_SLOT_LABEL_MODE;
            layout.hudScale = doubleOr(json, "hud_scale", layout.hudScale);
            return layout;
        }

        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("hud_anchor_x", anchorX);
            json.addProperty("hud_anchor_y", anchorY);
            json.addProperty("hud_portrait_x", portraitX);
            json.addProperty("hud_portrait_y", portraitY);
            json.addProperty("hud_abilities_x", abilitiesX);
            json.addProperty("hud_abilities_y", abilitiesY);
            json.addProperty("hud_bars_x", barsX);
            json.addProperty("hud_bars_y", barsY);
            json.addProperty("bar_label_mode", labelMode);
            json.addProperty("bar_show_seconds", showSeconds);
            json.addProperty("hud_bars_grow_up", barsGrowUp);
            json.addProperty("hud_abilities_vertical", abilitiesVertical);
            json.addProperty("hud_slot_label_side", slotLabelOrientation);
            json.addProperty("hud_scale", hudScale);
            return json;
        }

        HudLayout copy() {
            HudLayout copy = new HudLayout();
            copy.anchorX = anchorX;
            copy.anchorY = anchorY;
            copy.portraitX = portraitX;
            copy.portraitY = portraitY;
            copy.abilitiesX = abilitiesX;
            copy.abilitiesY = abilitiesY;
            copy.barsX = barsX;
            copy.barsY = barsY;
            copy.labelMode = labelMode;
            copy.showSeconds = showSeconds;
            copy.barsGrowUp = barsGrowUp;
            copy.abilitiesVertical = abilitiesVertical;
            copy.slotLabelOrientation = slotLabelOrientation;
            copy.hudScale = hudScale;
            return copy;
        }

        /** Points the config suppliers at a snapshot of this layout, which RaceOverlay then draws. */
        void pushToConfig() {
            HudLayout snapshot = copy();
            CreRacesConfig.HUD_ANCHOR_X = () -> snapshot.anchorX;
            CreRacesConfig.HUD_ANCHOR_Y = () -> snapshot.anchorY;
            CreRacesConfig.HUD_PORTRAIT_X = () -> snapshot.portraitX;
            CreRacesConfig.HUD_PORTRAIT_Y = () -> snapshot.portraitY;
            CreRacesConfig.HUD_ABILITIES_X = () -> snapshot.abilitiesX;
            CreRacesConfig.HUD_ABILITIES_Y = () -> snapshot.abilitiesY;
            CreRacesConfig.HUD_BARS_X = () -> snapshot.barsX;
            CreRacesConfig.HUD_BARS_Y = () -> snapshot.barsY;
            CreRacesConfig.BAR_LABEL_MODE = () -> snapshot.labelMode;
            CreRacesConfig.BAR_SHOW_SECONDS = () -> snapshot.showSeconds;
            CreRacesConfig.HUD_BARS_GROW_UP = () -> snapshot.barsGrowUp;
            CreRacesConfig.HUD_ABILITIES_VERTICAL = () -> snapshot.abilitiesVertical;
            CreRacesConfig.HUD_SLOT_LABEL_SIDE = () -> snapshot.slotLabelOrientation;
            CreRacesConfig.HUD_SCALE = () -> snapshot.hudScale;
        }

        private static int intOr(JsonObject json, String key, int fallback) {
            return json.has(key) ? json.get(key).getAsInt() : fallback;
        }

        private static boolean booleanOr(JsonObject json, String key, boolean fallback) {
            return json.has(key) ? json.get(key).getAsBoolean() : fallback;
        }

        private static String stringOr(JsonObject json, String key, String fallback) {
            return json.has(key) ? json.get(key).getAsString() : fallback;
        }

        private static double doubleOr(JsonObject json, String key, double fallback) {
            return json.has(key) ? json.get(key).getAsDouble() : fallback;
        }
    }
}
