package mc.sayda.creraces.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.client.ModKeyMappings;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.engine.GState;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import mc.sayda.creraces.util.WikiUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

/** The race menu's welcome page: start, debug, mirror and wiki buttons, plus the gender toggle. */
@SuppressWarnings("null")
public class MenuGUIScreen extends Screen {
    private static final ResourceLocation WELCOME_LOGO = ResourceLocation.fromNamespaceAndPath("creraces",
            "textures/screens/welcome_logo.png");

    // The player's gState values.
    private static final int MASCULINE = 0;
    private static final int FEMININE = 1;

    // Easter egg: typing this anywhere on the screen opens BadAppleScreen.
    private static final String BADAPPLE_TRIGGER = "badapple";
    private final StringBuilder typedBuffer = new StringBuilder();

    private int leftPos;
    private int topPos;

    public MenuGUIScreen() {
        super(Component.translatable("gui.creraces.menu_gui"));
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        typedBuffer.append(Character.toLowerCase(codePoint));
        if (typedBuffer.length() > BADAPPLE_TRIGGER.length()) {
            typedBuffer.delete(0, typedBuffer.length() - BADAPPLE_TRIGGER.length());
        }
        if (typedBuffer.toString().equals(BADAPPLE_TRIGGER)) {
            typedBuffer.setLength(0);
            if (this.minecraft != null) {
                this.minecraft.setScreen(new BadAppleScreen(this));
            }
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = RaceMenuArt.panelLeft(this.width);
        this.topPos = RaceMenuArt.panelTop(this.height);

        addScreenButton("gui.creraces.menu_gui.button_start_your_adventure1", RaceSelectionScreen::new, 21, 97, 133, 20);
        addScreenButton("gui.creraces.menu_gui.button_debug", DebugScreen::new, 21, 124, 63, 20);
        addScreenButton("gui.creraces.menu_gui.button_extras", DynamicMirrorScreen::new, 93, 124, 61, 20);

        Component wikiLabel = Component.translatable("gui.creraces.menu_gui.button_wiki");
        this.addRenderableWidget(Button.builder(wikiLabel,
                b -> ConfirmLinkScreen.confirmLinkNow(this, WikiUtils.getBaseWikiUrl(), true))
                .bounds(this.leftPos + 21, this.topPos + 148, 133, 10).build());

        if (CreRacesConfig.GSTATE_ENABLED.get()) {
            DataUtils.getVariables(this.minecraft.player).ifPresent(this::addGenderToggle);
        }
    }

    private void addScreenButton(String labelKey, Supplier<Screen> target, int x, int y, int width, int height) {
        Component label = Component.translatable(labelKey);
        this.addRenderableWidget(Button.builder(label, b -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(target.get());
            }
        }).bounds(this.leftPos + x, this.topPos + y, width, height).build());
    }

    private void addGenderToggle(IPlayerVariables vars) {
        Race currentRace = RaceRegistry.get(vars.getRace());
        // Races that are always one gender lock the toggle.
        boolean forced = currentRace != null && currentRace.getGState() != GState.BOTH;
        Component tooltip = Component.translatable(forced
                ? "gui.creraces.menu_gui.tooltip_gender_locked"
                : "gui.creraces.menu_gui.tooltip_gender");

        Component label = Component.translatable("gui.creraces.menu_gui.button_mf");
        Button toggle = Button.builder(label, button -> {
            if (this.minecraft != null && this.minecraft.player != null) {
                int nextState = vars.getGState() == MASCULINE ? FEMININE : MASCULINE;
                vars.setGState(nextState);
                BoundaryHandler.sendGStateUpdate(nextState);
            }
        }).bounds(this.leftPos - 70, this.topPos + 16, 40, 20).tooltip(Tooltip.create(tooltip)).build();
        toggle.active = !forced;
        this.addRenderableWidget(toggle);
    }

    /** The gState to show: the player's own, unless their race is always one gender. */
    private static int displayedGState(IPlayerVariables vars) {
        Race race = RaceRegistry.get(vars.getRace());
        if (race != null && race.getGState() == GState.FEMALE) {
            return FEMININE;
        }
        if (race != null && race.getGState() == GState.MALE) {
            return MASCULINE;
        }
        return vars.getGState();
    }

    private boolean isGStateShown() {
        return CreRacesConfig.GSTATE_ENABLED.get() && this.minecraft != null && this.minecraft.player != null;
    }

    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        renderPanel(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        // The hint labels are positioned relative to the panel origin.
        graphics.pose().pushPose();
        graphics.pose().translate(this.leftPos, this.topPos, 0.0F);
        renderLabels(graphics);
        graphics.pose().popPose();

        int centerX = this.leftPos + RaceMenuArt.PANEL_WIDTH / 2;
        graphics.drawCenteredString(this.font, Component.translatable("gui.creraces.menu_gui.label_welcome1"),
                centerX, this.topPos + 60, 0xFFFFFF);
        graphics.drawCenteredString(this.font, Component.translatable("gui.creraces.menu_gui.label_welcome2"),
                centerX, this.topPos + 70, 0xFFFFFF);
        graphics.drawCenteredString(this.font, Component.translatable("gui.creraces.menu_gui.label_welcome3"),
                centerX, this.topPos + 80, 0xFFFFFF);
    }

    private void renderPanel(GuiGraphics graphics) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RaceMenuArt.drawPanel(graphics, this.leftPos, this.topPos);
        graphics.blit(WELCOME_LOGO, this.leftPos + 3, this.topPos - 18, 0, 0, 168, 73, 168, 73);
        RaceMenuArt.drawSeasonalDecoration(graphics, this.leftPos + 11, this.topPos - 56);

        if (isGStateShown()) {
            DataUtils.getVariables(this.minecraft.player).ifPresent(vars -> {
                int gState = displayedGState(vars);
                if (gState == MASCULINE) {
                    graphics.blit(RaceMenuArt.MALE_ICON, this.leftPos - 97, this.topPos - 20, 0, 0, 16, 16, 16, 16);
                } else if (gState == FEMININE) {
                    graphics.blit(RaceMenuArt.FEMALE_ICON, this.leftPos - 97, this.topPos - 20, 0, 0, 16, 16, 16, 16);
                }
            });
        }

        RenderSystem.disableBlend();
    }

    private void renderLabels(GuiGraphics graphics) {
        drawKeybindHint(graphics, 161, "gui.creraces.menu_gui.label_keybind_hint1",
                ModKeyMappings.SKILL_WHEEL.getTranslatedKeyMessage());
        drawKeybindHint(graphics, 171, "gui.creraces.menu_gui.label_keybind_hint2",
                ModKeyMappings.ABILITY_A1.getTranslatedKeyMessage(),
                ModKeyMappings.ABILITY_A2.getTranslatedKeyMessage());
        drawKeybindHint(graphics, 181, "gui.creraces.menu_gui.label_keybind_hint3",
                ModKeyMappings.MENU_GUI.getTranslatedKeyMessage());

        if (isGStateShown()) {
            graphics.drawString(this.font, Component.translatable("gui.creraces.menu_gui.label_gstate_change"),
                    -79, -20, -1, false);
            graphics.drawString(this.font, Component.translatable("gui.creraces.menu_gui.label_gstate_appearance"),
                    -79, -11, -1, false);

            DataUtils.getVariables(this.minecraft.player).ifPresent(vars -> {
                Component state = displayedGState(vars) == FEMININE
                        ? Component.translatable("gui.creraces.menu_gui.gstate_feminine")
                                .withStyle(ChatFormatting.LIGHT_PURPLE)
                        : Component.translatable("gui.creraces.menu_gui.gstate_masculine")
                                .withStyle(ChatFormatting.BLUE);
                graphics.drawString(this.font, state, -73, 2, -1, false);
            });
        }
    }

    private void drawKeybindHint(GuiGraphics graphics, int y, String key, Object... keyNames) {
        Component hint = Component.translatable(key, keyNames);
        graphics.drawString(this.font, hint, 20, y, 0xb0b0b0);
    }

    // Drawn at the top of render() instead, so Screen.render() does not blur over the panel.
    @Override
    public void renderBackground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }
}
