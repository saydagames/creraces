package mc.sayda.creraces.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import mc.sayda.creraces.client.RaceOverlay;
import mc.sayda.creraces.config.CreRacesConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class HUDEditorScreen extends Screen {

    private static final int DEFAULT_ANCHOR_X         = -12;
    private static final int DEFAULT_ANCHOR_Y         = -9;
    private static final int DEFAULT_PORTRAIT_X       = 14;
    private static final int DEFAULT_PORTRAIT_Y       = 13;
    private static final int DEFAULT_ABILITIES_X      = 54;
    private static final int DEFAULT_ABILITIES_Y      = 17;
    private static final int DEFAULT_BARS_X           = 16;
    private static final int DEFAULT_BARS_Y           = 62;
    private static final String  DEFAULT_LABEL_MODE   = "name_value";
    private static final boolean DEFAULT_SHOW_SECONDS      = true;
    private static final boolean DEFAULT_BARS_GROW_UP      = false;
    private static final boolean DEFAULT_ABILITIES_VERTICAL = false;
    private static final String  DEFAULT_SLOT_LABEL_ORIENTATION = "below";
    private static final String[] SLOT_LABEL_MODES = { "below", "side", "top", "left", "none" };
    private static final double  DEFAULT_HUD_SCALE           = 1.0;

    private static final String[] LABEL_MODES     = { "name_value", "name", "value", "hidden" };
    private static final String[] LABEL_MODE_KEYS = { "name_value", "name", "value", "hidden" };

    // Saved originals for cancel
    private final int savedAnchorX, savedAnchorY;
    private final int savedPortraitX, savedPortraitY;
    private final int savedAbilitiesX, savedAbilitiesY;
    private final int savedBarsX, savedBarsY;
    private final String  savedLabelMode;
    private final boolean savedShowSeconds, savedBarsGrowUp, savedAbilitiesVertical;
    private final String  savedSlotLabelOrientation;
    private final double  savedHudScale;

    // Live working values
    private int anchorX, anchorY;
    private int portraitX, portraitY;
    private int abilitiesX, abilitiesY;
    private int barsX, barsY;
    private String  labelMode;
    private boolean showSeconds, barsGrowUp, abilitiesVertical;
    private String  slotLabelOrientation;
    private double  hudScale;

    // Drag state: -1 = nothing selected
    private int dragging     = -1;
    private int lastSelected = -1;
    private double dragStartMouseX, dragStartMouseY;
    private int dragStartX, dragStartY;

    // 1-step undo/redo: swap current ↔ snapshot
    private boolean hasHistory = false;
    private int histAnchorX, histAnchorY;
    private int histPortraitX, histPortraitY;
    private int histAbilitiesX, histAbilitiesY;
    private int histBarsX, histBarsY;
    private String  histLabelMode;
    private boolean histShowSeconds, histBarsGrowUp, histAbilitiesVertical;
    private String  histSlotLabelOrientation;
    private double  histHudScale;

    private Button labelModeButton, timeButton, barsDirButton, abilitiesDirButton, slotLabelButton;
    private net.minecraft.client.gui.components.EditBox scaleBox;
    private final java.util.List<net.minecraft.client.gui.components.AbstractWidget> toggleableButtons = new java.util.ArrayList<>();
    private boolean buttonsVisible = true;

    public HUDEditorScreen() {
        super(Component.translatable("gui.creraces.hud_editor.title"));
        savedAnchorX           = CreRacesConfig.HUD_ANCHOR_X.get();
        savedAnchorY           = CreRacesConfig.HUD_ANCHOR_Y.get();
        savedPortraitX         = CreRacesConfig.HUD_PORTRAIT_X.get();
        savedPortraitY         = CreRacesConfig.HUD_PORTRAIT_Y.get();
        savedAbilitiesX        = CreRacesConfig.HUD_ABILITIES_X.get();
        savedAbilitiesY        = CreRacesConfig.HUD_ABILITIES_Y.get();
        savedBarsX             = CreRacesConfig.HUD_BARS_X.get();
        savedBarsY             = CreRacesConfig.HUD_BARS_Y.get();
        savedLabelMode         = CreRacesConfig.BAR_LABEL_MODE.get();
        savedShowSeconds       = CreRacesConfig.BAR_SHOW_SECONDS.get();
        savedBarsGrowUp        = CreRacesConfig.HUD_BARS_GROW_UP.get();
        savedAbilitiesVertical = CreRacesConfig.HUD_ABILITIES_VERTICAL.get();
        savedSlotLabelOrientation = CreRacesConfig.HUD_SLOT_LABEL_SIDE.get();
        savedHudScale          = CreRacesConfig.HUD_SCALE.get();

        anchorX           = savedAnchorX;
        anchorY           = savedAnchorY;
        portraitX         = savedPortraitX;
        portraitY         = savedPortraitY;
        abilitiesX        = savedAbilitiesX;
        abilitiesY        = savedAbilitiesY;
        barsX             = savedBarsX;
        barsY             = savedBarsY;
        labelMode         = savedLabelMode;
        showSeconds       = savedShowSeconds;
        barsGrowUp        = savedBarsGrowUp;
        abilitiesVertical = savedAbilitiesVertical;
        slotLabelOrientation = savedSlotLabelOrientation;
        hudScale          = savedHudScale;
    }

    @Override
    protected void init() {
        syncSuppliersToFields();
        toggleableButtons.clear();

        int row0Y = this.height - 72;
        int row1Y = this.height - 48;
        int row2Y = this.height - 24;

        // Row 0: presets
        reg(Button.builder(Component.translatable("gui.creraces.hud_editor.btn.save_preset"),
            btn -> savePreset()
        ).bounds(8, row0Y, 110, 20).build());

        reg(Button.builder(Component.translatable("gui.creraces.hud_editor.btn.load_preset"),
            btn -> loadPreset()
        ).bounds(122, row0Y, 110, 20).build());

        // Scale EditBox: decimal value (1.0 = 100%)
        scaleBox = new net.minecraft.client.gui.components.EditBox(
                this.font, 236, row0Y, 40, 20, Component.literal("Scale"));
        scaleBox.setMaxLength(7);
        scaleBox.setValue(String.format("%.4f", hudScale));
        scaleBox.setHint(Component.literal("1.0000"));
        this.addRenderableWidget(scaleBox);
        toggleableButtons.add(scaleBox);

        // Row 1: layout toggles
        barsDirButton = reg(Button.builder(barsDirComponent(),
            btn -> { saveSnapshot(); barsGrowUp = !barsGrowUp; syncSuppliersToFields(); btn.setMessage(barsDirComponent()); }
        ).bounds(8, row1Y, 110, 20).build());

        abilitiesDirButton = reg(Button.builder(abilitiesDirComponent(),
            btn -> { saveSnapshot(); abilitiesVertical = !abilitiesVertical; syncSuppliersToFields(); btn.setMessage(abilitiesDirComponent()); }
        ).bounds(122, row1Y, 130, 20).build());

        slotLabelButton = reg(Button.builder(slotLabelComponent(),
            btn -> { saveSnapshot(); cycleSlotLabelOrientation(); btn.setMessage(slotLabelComponent()); }
        ).bounds(256, row1Y, 120, 20).build());

        // Row 2: bar label / time + undo/redo + save controls
        labelModeButton = reg(Button.builder(labelBtnComponent(),
            btn -> { saveSnapshot(); cycleLabelMode(); btn.setMessage(labelBtnComponent()); }
        ).bounds(8, row2Y, 120, 20).build());

        timeButton = reg(Button.builder(timeBtnComponent(),
            btn -> { saveSnapshot(); showSeconds = !showSeconds; syncSuppliersToFields(); btn.setMessage(timeBtnComponent()); }
        ).bounds(132, row2Y, 90, 20).build());

        reg(Button.builder(Component.translatable("gui.creraces.hud_editor.btn.undo"),
            btn -> swapSnapshot()
        ).bounds(226, row2Y, 50, 20).build());

        reg(Button.builder(
            Component.translatable("gui.creraces.hud_editor.btn.reset"), btn -> { saveSnapshot(); resetToDefaults(); }
        ).bounds(280, row2Y, 50, 20).build());

        reg(Button.builder(
            Component.translatable("gui.creraces.hud_editor.btn.done"), btn -> saveAndClose()
        ).bounds(334, row2Y, 50, 20).build());

        // Small square toggle in the top-right; always visible, not in toggleableButtons
        this.addRenderableWidget(Button.builder(Component.literal("T"), btn -> {
            buttonsVisible = !buttonsVisible;
            toggleableButtons.forEach(b -> b.visible = buttonsVisible);
        }).bounds(this.width - 18, 4, 14, 14).build());

        // Sync initial visibility state (handles screen resize)
        toggleableButtons.forEach(b -> b.visible = buttonsVisible);
    }

    /** Registers a button as toggleable and adds it to the screen. */
    private Button reg(Button button) {
        this.addRenderableWidget(button);
        toggleableButtons.add(button);
        return button;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        graphics.fill(0, 0, this.width, this.height, 0x55000000);

        // fill() teardown disables blend; restore before any blit work
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RaceOverlay.render(graphics, partialTick);

        // Bars zone is empty until a real bar has a nonzero value, so an example bar is drawn
        // here to preview placement. Reuses RaceOverlay's own anchor-scale transform.
        graphics.pose().pushPose();
        if (hudScale != 1.0) {
            graphics.pose().translate(anchorX, anchorY, 0);
            graphics.pose().scale((float) hudScale, (float) hudScale, 1.0f);
            graphics.pose().translate(-anchorX, -anchorY, 0);
        }
        RaceOverlay.renderExampleBar(graphics, anchorX + barsX, anchorY + barsY);
        graphics.pose().popPose();

        // Convert HUD-space positions to screen-space using the current scale
        int pXs = sx(anchorX + portraitX),  pYs = sy(anchorY + portraitY);
        int aXs = sx(anchorX + abilitiesX), aYs = sy(anchorY + abilitiesY);
        int bXs = sx(anchorX + barsX),      bYs = sy(anchorY + barsY);

        int slotStep = switch (slotLabelOrientation) { case "side", "left" -> 25; case "none" -> 22; default -> 30; };
        int abilityBoxW = sl(abilitiesVertical ? (slotLabelOrientation.equals("side") ? 72 : 22) : 130);
        int abilityBoxH = sl(abilitiesVertical ? (5 * slotStep) : (slotLabelOrientation.equals("below") || slotLabelOrientation.equals("top") ? 30 : 22));

        int barTopOff = sl(barsGrowUp ? -(5 * 9) : -12);
        int barBotOff = sl(barsGrowUp ? 4 : 40);

        // Always dim; brighten when selected
        drawGroupOutline(graphics, pXs - 4, pYs - 1, sl(44), sl(44),
                lastSelected == 0 ? 0xFF88AAFF : 0x554466CC);
        drawGroupOutline(graphics, aXs - 2, aYs - 2, abilityBoxW + 4, abilityBoxH + 4,
                lastSelected == 1 ? 0xFF88FFAA : 0x5544CC66);
        drawGroupOutline(graphics, bXs - 2, bYs + barTopOff, sl(126), barBotOff - barTopOff,
                lastSelected == 2 ? 0xFFFFCC66 : 0x55CC8833);

        // Labels only when selected
        if (lastSelected == 0)
            graphics.drawString(this.font, Component.translatable("gui.creraces.hud_editor.group.portrait"),
                    pXs - 4, pYs - 11, 0xFF88AAFF, true);
        if (lastSelected == 1)
            graphics.drawString(this.font, Component.translatable("gui.creraces.hud_editor.group.abilities"),
                    aXs - 2, aYs - 12, 0xFF88FFAA, true);
        if (lastSelected == 2)
            graphics.drawString(this.font, Component.translatable("gui.creraces.hud_editor.group.bars"),
                    bXs - 2, bYs + barTopOff - 10, 0xFFFFCC66, true);

        if (buttonsVisible)
            graphics.drawString(this.font, Component.translatable("gui.creraces.hud_editor.hint"),
                    4, 4, 0xFFFFFFFF, false);

        super.render(graphics, mouseX, mouseY, partialTick);
        RenderSystem.disableBlend();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Buttons take priority; check them first before any group drag logic
        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        if (button == 0) {
            if (isHoveringGroup(mouseX, mouseY, 0)) {
                startDrag(0, mouseX, mouseY, portraitX, portraitY); return true;
            } else if (isHoveringGroup(mouseX, mouseY, 1)) {
                startDrag(1, mouseX, mouseY, abilitiesX, abilitiesY); return true;
            } else if (isHoveringGroup(mouseX, mouseY, 2)) {
                startDrag(2, mouseX, mouseY, barsX, barsY); return true;
            }
            startDrag(3, mouseX, mouseY, anchorX, anchorY);
        }
        return false;
    }

    private void startDrag(int group, double mx, double my, int sx, int sy) {
        saveSnapshot();
        dragging = group;
        lastSelected = group;
        dragStartMouseX = mx; dragStartMouseY = my;
        dragStartX = sx;      dragStartY = sy;
    }

    private void saveSnapshot() {
        histAnchorX = anchorX;         histAnchorY = anchorY;
        histPortraitX = portraitX;     histPortraitY = portraitY;
        histAbilitiesX = abilitiesX;   histAbilitiesY = abilitiesY;
        histBarsX = barsX;             histBarsY = barsY;
        histLabelMode = labelMode;
        histShowSeconds = showSeconds; histBarsGrowUp = barsGrowUp;
        histAbilitiesVertical = abilitiesVertical; histSlotLabelOrientation = slotLabelOrientation;
        histHudScale = hudScale;
        hasHistory = true;
    }

    private void swapSnapshot() {
        if (!hasHistory) return;
        int t;
        t = anchorX;    anchorX    = histAnchorX;    histAnchorX = t;
        t = anchorY;    anchorY    = histAnchorY;    histAnchorY = t;
        t = portraitX;  portraitX  = histPortraitX;  histPortraitX = t;
        t = portraitY;  portraitY  = histPortraitY;  histPortraitY = t;
        t = abilitiesX; abilitiesX = histAbilitiesX; histAbilitiesX = t;
        t = abilitiesY; abilitiesY = histAbilitiesY; histAbilitiesY = t;
        t = barsX;      barsX      = histBarsX;      histBarsX = t;
        t = barsY;      barsY      = histBarsY;      histBarsY = t;
        String ts = labelMode; labelMode = histLabelMode; histLabelMode = ts;
        boolean tb;
        tb = showSeconds;       showSeconds       = histShowSeconds;       histShowSeconds       = tb;
        tb = barsGrowUp;        barsGrowUp        = histBarsGrowUp;        histBarsGrowUp        = tb;
        tb = abilitiesVertical; abilitiesVertical = histAbilitiesVertical; histAbilitiesVertical = tb;
        ts = slotLabelOrientation; slotLabelOrientation = histSlotLabelOrientation; histSlotLabelOrientation = ts;
        double td = hudScale; hudScale = histHudScale; histHudScale = td;
        syncSuppliersToFields();
        if (labelModeButton    != null) labelModeButton.setMessage(labelBtnComponent());
        if (timeButton         != null) timeButton.setMessage(timeBtnComponent());
        if (barsDirButton      != null) barsDirButton.setMessage(barsDirComponent());
        if (abilitiesDirButton != null) abilitiesDirButton.setMessage(abilitiesDirComponent());
        if (slotLabelButton    != null) slotLabelButton.setMessage(slotLabelComponent());
        if (scaleBox           != null) scaleBox.setValue(String.format("%.4f", hudScale));
    }

    // -------------------------------------------------------------------------
    // Preset save / load

    private static java.nio.file.Path presetPath() {
        return net.minecraft.client.Minecraft.getInstance()
                .gameDirectory.toPath()
                .resolve("config/creraces/hud_preset.json");
    }

    private void savePreset() {
        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        json.addProperty("hud_anchor_x",           anchorX);
        json.addProperty("hud_anchor_y",           anchorY);
        json.addProperty("hud_portrait_x",         portraitX);
        json.addProperty("hud_portrait_y",         portraitY);
        json.addProperty("hud_abilities_x",        abilitiesX);
        json.addProperty("hud_abilities_y",        abilitiesY);
        json.addProperty("hud_bars_x",             barsX);
        json.addProperty("hud_bars_y",             barsY);
        json.addProperty("bar_label_mode",         labelMode);
        json.addProperty("bar_show_seconds",       showSeconds);
        json.addProperty("hud_bars_grow_up",       barsGrowUp);
        json.addProperty("hud_abilities_vertical", abilitiesVertical);
        json.addProperty("hud_slot_label_side",    slotLabelOrientation);
        json.addProperty("hud_scale",              hudScale);
        try {
            java.nio.file.Path path = presetPath();
            java.nio.file.Files.createDirectories(path.getParent());
            try (java.io.FileWriter w = new java.io.FileWriter(path.toFile())) {
                new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(json, w);
            }
        } catch (Exception e) {
            mc.sayda.creraces.CreRaces.LOGGER.warn("Failed to save HUD preset", e);
        }
    }

    private void loadPreset() {
        java.nio.file.Path path = presetPath();
        if (!java.nio.file.Files.exists(path)) return;
        try (java.io.FileReader r = new java.io.FileReader(path.toFile())) {
            com.google.gson.JsonObject json = new com.google.gson.Gson()
                    .fromJson(r, com.google.gson.JsonObject.class);
            if (json == null) return;
            saveSnapshot();
            anchorX           = getPresetInt(json, "hud_anchor_x",           anchorX);
            anchorY           = getPresetInt(json, "hud_anchor_y",           anchorY);
            portraitX         = getPresetInt(json, "hud_portrait_x",         portraitX);
            portraitY         = getPresetInt(json, "hud_portrait_y",         portraitY);
            abilitiesX        = getPresetInt(json, "hud_abilities_x",        abilitiesX);
            abilitiesY        = getPresetInt(json, "hud_abilities_y",        abilitiesY);
            barsX             = getPresetInt(json, "hud_bars_x",             barsX);
            barsY             = getPresetInt(json, "hud_bars_y",             barsY);
            labelMode         = getPresetStr(json, "bar_label_mode",         labelMode);
            showSeconds       = getPresetBool(json, "bar_show_seconds",      showSeconds);
            barsGrowUp        = getPresetBool(json, "hud_bars_grow_up",      barsGrowUp);
            abilitiesVertical = getPresetBool(json, "hud_abilities_vertical", abilitiesVertical);
            String _slo = getPresetStr(json, "hud_slot_label_side", slotLabelOrientation);
            slotLabelOrientation = java.util.Arrays.asList(SLOT_LABEL_MODES).contains(_slo) ? _slo : DEFAULT_SLOT_LABEL_ORIENTATION;
            hudScale          = getPresetDbl(json,  "hud_scale",             hudScale);
            syncSuppliersToFields();
            if (labelModeButton    != null) labelModeButton.setMessage(labelBtnComponent());
            if (timeButton         != null) timeButton.setMessage(timeBtnComponent());
            if (barsDirButton      != null) barsDirButton.setMessage(barsDirComponent());
            if (abilitiesDirButton != null) abilitiesDirButton.setMessage(abilitiesDirComponent());
            if (slotLabelButton    != null) slotLabelButton.setMessage(slotLabelComponent());
            if (scaleBox           != null) scaleBox.setValue(String.format("%.4f", hudScale));
        } catch (Exception e) {
            mc.sayda.creraces.CreRaces.LOGGER.warn("Failed to load HUD preset", e);
        }
    }

    private static int     getPresetInt(com.google.gson.JsonObject j, String k, int def)      { return j.has(k) ? j.get(k).getAsInt()     : def; }
    private static boolean getPresetBool(com.google.gson.JsonObject j, String k, boolean def) { return j.has(k) ? j.get(k).getAsBoolean() : def; }
    private static String  getPresetStr(com.google.gson.JsonObject j, String k, String def)   { return j.has(k) ? j.get(k).getAsString()  : def; }
    private static double  getPresetDbl(com.google.gson.JsonObject j, String k, double def)   { return j.has(k) ? j.get(k).getAsDouble()  : def; }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (button == 0 && dragging >= 0) {
            // Groups 0-2 store positions in HUD-space; convert screen delta → HUD delta via scale.
            // Group 3 (anchor) is in screen-space; no conversion needed.
            int ddxS = (int)(mouseX - dragStartMouseX);
            int ddyS = (int)(mouseY - dragStartMouseY);
            int ddxH = fromScreenDx(mouseX - dragStartMouseX);
            int ddyH = fromScreenDx(mouseY - dragStartMouseY);
            switch (dragging) {
                case 0 -> { portraitX  = dragStartX + ddxH; portraitY  = dragStartY + ddyH; }
                case 1 -> { abilitiesX = dragStartX + ddxH; abilitiesY = dragStartY + ddyH; }
                case 2 -> { barsX      = dragStartX + ddxH; barsY      = dragStartY + ddyH; }
                case 3 -> { anchorX    = dragStartX + ddxS; anchorY    = dragStartY + ddyS; }
            }
            syncSuppliersToFields();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) dragging = -1;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // EditBox (and other focused widgets) handle keys first; ensures cursor/delete work
        if (scaleBox != null && scaleBox.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                applyScaleFromBox();
                scaleBox.setFocused(false);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { cancelAndClose(); return true; }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        int dx = 0, dy = 0;
        if      (keyCode == GLFW.GLFW_KEY_LEFT)  dx = -1;
        else if (keyCode == GLFW.GLFW_KEY_RIGHT) dx =  1;
        else if (keyCode == GLFW.GLFW_KEY_UP)    dy = -1;
        else if (keyCode == GLFW.GLFW_KEY_DOWN)  dy =  1;

        if (dx != 0 || dy != 0) {
            switch (lastSelected) {
                case 0 -> { portraitX  += dx; portraitY  += dy; }
                case 1 -> { abilitiesX += dx; abilitiesY += dy; }
                case 2 -> { barsX      += dx; barsY      += dy; }
                case 3 -> { anchorX    += dx; anchorY    += dy; }
            }
            syncSuppliersToFields();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { cancelAndClose(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void applyScaleFromBox() {
        if (scaleBox == null) return;
        try {
            double val = Double.parseDouble(scaleBox.getValue().trim());
            saveSnapshot();
            hudScale = Math.max(0.1, Math.min(5.0, val));
            syncSuppliersToFields();
        } catch (NumberFormatException ignored) { /* revert box to current value */ }
        scaleBox.setValue(String.format("%.4f", hudScale));
    }

    // -------------------------------------------------------------------------

    private void syncSuppliersToFields() {
        int ax = anchorX, ay = anchorY;
        int px = portraitX, py = portraitY;
        int abx = abilitiesX, aby = abilitiesY;
        int bx = barsX, by = barsY;
        String lm = labelMode;
        boolean ss = showSeconds, bgu = barsGrowUp, av = abilitiesVertical;
        String slo = slotLabelOrientation;
        double hs = hudScale;
        CreRacesConfig.HUD_ANCHOR_X          = () -> ax;
        CreRacesConfig.HUD_ANCHOR_Y          = () -> ay;
        CreRacesConfig.HUD_PORTRAIT_X        = () -> px;
        CreRacesConfig.HUD_PORTRAIT_Y        = () -> py;
        CreRacesConfig.HUD_ABILITIES_X       = () -> abx;
        CreRacesConfig.HUD_ABILITIES_Y       = () -> aby;
        CreRacesConfig.HUD_BARS_X            = () -> bx;
        CreRacesConfig.HUD_BARS_Y            = () -> by;
        CreRacesConfig.BAR_LABEL_MODE        = () -> lm;
        CreRacesConfig.BAR_SHOW_SECONDS      = () -> ss;
        CreRacesConfig.HUD_BARS_GROW_UP      = () -> bgu;
        CreRacesConfig.HUD_ABILITIES_VERTICAL = () -> av;
        CreRacesConfig.HUD_SLOT_LABEL_SIDE   = () -> slo;
        CreRacesConfig.HUD_SCALE             = () -> hs;
    }

    private void saveAndClose() {
        syncSuppliersToFields();
        CreRacesConfig.saveHudConfig();
        super.onClose();
    }

    private void cancelAndClose() {
        int oax = savedAnchorX, oay = savedAnchorY;
        int opx = savedPortraitX, opy = savedPortraitY;
        int oabx = savedAbilitiesX, oaby = savedAbilitiesY;
        int obx = savedBarsX, oby = savedBarsY;
        String olm = savedLabelMode;
        boolean oss = savedShowSeconds, obgu = savedBarsGrowUp, oav = savedAbilitiesVertical;
        String oslo = savedSlotLabelOrientation;
        double ohs = savedHudScale;
        CreRacesConfig.HUD_ANCHOR_X          = () -> oax;
        CreRacesConfig.HUD_ANCHOR_Y          = () -> oay;
        CreRacesConfig.HUD_PORTRAIT_X        = () -> opx;
        CreRacesConfig.HUD_PORTRAIT_Y        = () -> opy;
        CreRacesConfig.HUD_ABILITIES_X       = () -> oabx;
        CreRacesConfig.HUD_ABILITIES_Y       = () -> oaby;
        CreRacesConfig.HUD_BARS_X            = () -> obx;
        CreRacesConfig.HUD_BARS_Y            = () -> oby;
        CreRacesConfig.BAR_LABEL_MODE        = () -> olm;
        CreRacesConfig.BAR_SHOW_SECONDS      = () -> oss;
        CreRacesConfig.HUD_BARS_GROW_UP      = () -> obgu;
        CreRacesConfig.HUD_ABILITIES_VERTICAL = () -> oav;
        CreRacesConfig.HUD_SLOT_LABEL_SIDE   = () -> oslo;
        CreRacesConfig.HUD_SCALE             = () -> ohs;
        super.onClose();
    }

    @Override
    public void onClose() {
        cancelAndClose();
    }

    private void cycleLabelMode() {
        for (int i = 0; i < LABEL_MODES.length; i++) {
            if (LABEL_MODES[i].equals(labelMode)) {
                labelMode = LABEL_MODES[(i + 1) % LABEL_MODES.length];
                syncSuppliersToFields();
                return;
            }
        }
        labelMode = LABEL_MODES[0];
        syncSuppliersToFields();
    }

    private Component getLabelModeComponent() {
        for (int i = 0; i < LABEL_MODES.length; i++)
            if (LABEL_MODES[i].equals(labelMode))
                return Component.translatable("gui.creraces.hud_editor.label." + LABEL_MODE_KEYS[i]);
        return Component.translatable("gui.creraces.hud_editor.label.name_value");
    }

    private Component labelBtnComponent()     { return Component.translatable("gui.creraces.hud_editor.btn.label", getLabelModeComponent()); }
    private Component timeBtnComponent()      { return Component.translatable("gui.creraces.hud_editor.btn.time",  Component.translatable(showSeconds ? "gui.creraces.hud_editor.time.seconds" : "gui.creraces.hud_editor.time.ticks")); }
    private Component barsDirComponent()      { return Component.translatable("gui.creraces.hud_editor.btn.bars_dir", Component.translatable(barsGrowUp ? "gui.creraces.hud_editor.bars.up" : "gui.creraces.hud_editor.bars.down")); }
    private Component abilitiesDirComponent() { return Component.translatable("gui.creraces.hud_editor.btn.abilities_dir", Component.translatable(abilitiesVertical ? "gui.creraces.hud_editor.abilities.vertical" : "gui.creraces.hud_editor.abilities.horizontal")); }
    private Component slotLabelComponent()    { return Component.translatable("gui.creraces.hud_editor.btn.slot_label", Component.translatable("gui.creraces.hud_editor.slot_label." + slotLabelOrientation)); }

    private void cycleSlotLabelOrientation() {
        for (int i = 0; i < SLOT_LABEL_MODES.length; i++) {
            if (SLOT_LABEL_MODES[i].equals(slotLabelOrientation)) {
                slotLabelOrientation = SLOT_LABEL_MODES[(i + 1) % SLOT_LABEL_MODES.length];
                syncSuppliersToFields();
                return;
            }
        }
        slotLabelOrientation = SLOT_LABEL_MODES[0];
        syncSuppliersToFields();
    }

    private void resetToDefaults() {
        anchorX = DEFAULT_ANCHOR_X;   anchorY = DEFAULT_ANCHOR_Y;
        portraitX = DEFAULT_PORTRAIT_X; portraitY = DEFAULT_PORTRAIT_Y;
        abilitiesX = DEFAULT_ABILITIES_X; abilitiesY = DEFAULT_ABILITIES_Y;
        barsX = DEFAULT_BARS_X;       barsY = DEFAULT_BARS_Y;
        labelMode = DEFAULT_LABEL_MODE; showSeconds = DEFAULT_SHOW_SECONDS;
        barsGrowUp = DEFAULT_BARS_GROW_UP; abilitiesVertical = DEFAULT_ABILITIES_VERTICAL;
        slotLabelOrientation = DEFAULT_SLOT_LABEL_ORIENTATION;
        hudScale = DEFAULT_HUD_SCALE;
        syncSuppliersToFields();
        if (labelModeButton    != null) labelModeButton.setMessage(labelBtnComponent());
        if (timeButton         != null) timeButton.setMessage(timeBtnComponent());
        if (barsDirButton      != null) barsDirButton.setMessage(barsDirComponent());
        if (abilitiesDirButton != null) abilitiesDirButton.setMessage(abilitiesDirComponent());
        if (slotLabelButton    != null) slotLabelButton.setMessage(slotLabelComponent());
        if (scaleBox           != null) scaleBox.setValue(String.format("%.4f", hudScale));
    }

    private boolean isHoveringGroup(double mx, double my, int group) {
        return switch (group) {
            case 0 -> {
                int pXs = sx(anchorX + portraitX), pYs = sy(anchorY + portraitY);
                yield mx >= pXs - 4 && mx <= pXs + sl(40) && my >= pYs - 1 && my <= pYs + sl(43);
            }
            case 1 -> {
                int aXs = sx(anchorX + abilitiesX), aYs = sy(anchorY + abilitiesY);
                int _step = switch (slotLabelOrientation) { case "side", "left" -> 25; case "none" -> 22; default -> 30; };
                int w = sl(abilitiesVertical ? (slotLabelOrientation.equals("side") ? 72 : 22) : 130);
                int h = sl(abilitiesVertical ? (5 * _step) : (slotLabelOrientation.equals("below") || slotLabelOrientation.equals("top") ? 30 : 22));
                yield mx >= aXs - 2 && mx <= aXs + w + 2 && my >= aYs - 2 && my <= aYs + h + 2;
            }
            case 2 -> {
                int bXs = sx(anchorX + barsX), bYs = sy(anchorY + barsY);
                int topOff = sl(barsGrowUp ? -(5 * 9) : -12);
                int botOff = sl(barsGrowUp ? 4 : 40);
                yield mx >= bXs - 2 && mx <= bXs + sl(124) && my >= bYs + topOff && my <= bYs + botOff;
            }
            default -> false;
        };
    }

    // Scale helpers: convert HUD-space → screen-space using the current hudScale + anchor
    private int sx(int hx) { return (int) Math.round((hx - anchorX) * hudScale + anchorX); }
    private int sy(int hy) { return (int) Math.round((hy - anchorY) * hudScale + anchorY); }
    private int sl(int len) { return (int) Math.round(len * hudScale); }
    // Convert screen-space delta → HUD-space delta (for drag math on groups 0-2)
    private int fromScreenDx(double d) { return (int) Math.round(d / hudScale); }

    private static void drawGroupOutline(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x,         y,         x + w,     y + 1,     color);
        graphics.fill(x,         y + h - 1, x + w,     y + h,     color);
        graphics.fill(x,         y + 1,     x + 1,     y + h - 1, color);
        graphics.fill(x + w - 1, y + 1,     x + w,     y + h - 1, color);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    /**
     * This screen deliberately draws no backdrop. 1.21 Screen.render() calls renderBackground()
     * on its own where 1.20.1 did not, so it is suppressed here to keep the view unobstructed.
     */
    @Override
    public void renderBackground(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }
}
