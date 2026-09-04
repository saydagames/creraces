package mc.sayda.creraces.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.Util;

import java.util.Calendar;
import javax.annotation.Nonnull;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

import mc.sayda.creraces.client.ModKeyMappings;

public class MenuGUIScreen extends Screen {
    private static final ResourceLocation SELECTION_BG = ResourceLocation.fromNamespaceAndPath("creraces",
            "textures/screens/selection_bg.png");
    private static final ResourceLocation SELECTION_BORDER = ResourceLocation.fromNamespaceAndPath("creraces",
            "textures/screens/selection_border.png");
    private static final ResourceLocation WELCOME_LOGO = ResourceLocation.fromNamespaceAndPath("creraces",
            "textures/screens/welcome_logo.png");

    // Decorations
    private static final ResourceLocation DECO_CHRISTMAS = ResourceLocation.fromNamespaceAndPath("creraces",
            "textures/screens/christmas_decoration.png");
    private static final ResourceLocation DECO_HALLOWEEN = ResourceLocation.fromNamespaceAndPath("creraces",
            "textures/screens/halloween_decoration.png");
    private static final ResourceLocation DECO_MIDSUMMER = ResourceLocation.fromNamespaceAndPath("creraces",
            "textures/screens/midsummer_decoration.png");

    private static final ResourceLocation M_ICON = ResourceLocation.fromNamespaceAndPath("creraces", "textures/screens/m.png");
    private static final ResourceLocation F_ICON = ResourceLocation.fromNamespaceAndPath("creraces", "textures/screens/f.png");
    private static final ResourceLocation MF_BUTTON = ResourceLocation.fromNamespaceAndPath("creraces",
            "textures/screens/atlas/button_mf.png");

    // Panel metrics that AbstractContainerScreen used to provide. This is a menu, not a
    // container, so it is a plain Screen opened client side and tracks its own origin.
    private static final int IMAGE_WIDTH = 176;
    private static final int IMAGE_HEIGHT = 166;
    private int leftPos;
    private int topPos;

    // Easter egg: typing this anywhere on the screen opens BadAppleScreen.
    private static final String BADAPPLE_TRIGGER = "badapple";
    private final StringBuilder typedBuffer = new StringBuilder();

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
        this.leftPos = (this.width - IMAGE_WIDTH) / 2;
        this.topPos = (this.height - IMAGE_HEIGHT) / 2;

        // Start Your Adventure
        Component startAdventure = java.util.Objects.requireNonNull(Component.translatable("gui.creraces.menu_gui.button_start_your_adventure1"));
        this.addRenderableWidget(
                java.util.Objects.requireNonNull(Button.builder(startAdventure, b -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new RaceSelectionScreen());
                    }
                }).bounds(this.leftPos + 21, this.topPos + 97, 133, 20).build()));

        // Debug
        this.addRenderableWidget(
                java.util.Objects.requireNonNull(Button.builder(java.util.Objects.requireNonNull(Component.translatable("gui.creraces.menu_gui.button_debug")), b -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new DebugScreen());
                    }
                }).bounds(this.leftPos + 21, this.topPos + 124, 63, 20).build()));

        // Extras (Mirror)
        this.addRenderableWidget(
                java.util.Objects.requireNonNull(Button.builder(java.util.Objects.requireNonNull(Component.translatable("gui.creraces.menu_gui.button_extras")), b -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new DynamicMirrorScreen());
                    }
                }).bounds(this.leftPos + 93, this.topPos + 124, 61, 20).build())
        );

        // Wiki Button
        Component wikiMsg = java.util.Objects.requireNonNull(Component.translatable("gui.creraces.menu_gui.button_wiki"));
        this.addRenderableWidget(
                java.util.Objects.requireNonNull(Button.builder(wikiMsg, b -> {
                    if (this.minecraft != null) {
                        String url = mc.sayda.creraces.util.WikiUtils.getBaseWikiUrl();
                        this.minecraft.setScreen(new ConfirmLinkScreen(confirmed -> {
                            if (confirmed) {
                                Util.getPlatform().openUri(url);
                            }
                            this.minecraft.setScreen(this);
                        }, url, true));
                    }
                }).bounds(this.leftPos + 21, this.topPos + 148, 133, 10).build()));

        // Gender System Toggle
        if (mc.sayda.creraces.config.CreRacesConfig.GSTATE_ENABLED.get()) {
            mc.sayda.creraces.capability.DataUtils.getVariables(this.minecraft.player).ifPresent(vars -> {
                mc.sayda.creraces.race.Race currentRace = mc.sayda.creraces.race.RaceRegistry.get(vars.getRace());
                boolean forced = currentRace != null
                        && currentRace.getGState() != mc.sayda.creraces.engine.GState.BOTH;

                Button mfButton = java.util.Objects.requireNonNull(Button.builder(java.util.Objects.requireNonNull(Component.translatable("gui.creraces.menu_gui.button_mf")), button -> {
                    if (this.minecraft != null && this.minecraft.player != null) {
                        int gState = vars.getGState();
                        int nextState = gState == 0 ? 1 : 0;
                        vars.setGState(nextState);
                        mc.sayda.creraces.network.BoundaryHandler.sendGStateUpdate(nextState);
                    }
                }).bounds(this.leftPos - 70, this.topPos + 16, 40, 20).tooltip(
                        net.minecraft.client.gui.components.Tooltip
                                .create(forced ? java.util.Objects.requireNonNull(Component.translatable("gui.creraces.menu_gui.tooltip_gender_locked"))
                                        : java.util.Objects.requireNonNull(Component.translatable("gui.creraces.menu_gui.tooltip_gender"))))
                        .build());

                if (forced) {
                    mfButton.active = false;
                }

                this.addRenderableWidget(mfButton);
            });
        }
    }

    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // super.render() runs renderBackground() (blur + panel) and then the widgets.
        super.render(graphics, mouseX, mouseY, partialTick);

        // AbstractContainerScreen used to translate the pose before renderLabels, so the hint
        // coordinates below are all relative to the panel origin. Keep that frame of reference.
        graphics.pose().pushPose();
        graphics.pose().translate(this.leftPos, this.topPos, 0.0F);
        renderLabels(graphics, mouseX, mouseY);
        graphics.pose().popPose();

        // Centered Welcome Text
        graphics.drawCenteredString(this.font, Component.translatable("gui.creraces.menu_gui.label_welcome1"),
                this.leftPos + (IMAGE_WIDTH / 2), this.topPos + 60, 0xFFFFFF);
        graphics.drawCenteredString(this.font, Component.translatable("gui.creraces.menu_gui.label_welcome2"),
                this.leftPos + (IMAGE_WIDTH / 2), this.topPos + 70, 0xFFFFFF);
        graphics.drawCenteredString(this.font, Component.translatable("gui.creraces.menu_gui.label_welcome3"),
                this.leftPos + (IMAGE_WIDTH / 2), this.topPos + 80, 0xFFFFFF);
    }

    @Override
    public void renderBackground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Vanilla blur and gradient first, then the panel on top, so this menu reads like a
        // pause screen instead of having its own art blurred by Screen.render().
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        graphics.blit(SELECTION_BG, this.leftPos + -4, this.topPos + -26, 0, 0, 181, 220, 181, 220);
        graphics.blit(SELECTION_BORDER, this.leftPos + -25, this.topPos + -47, 0, 0, 225, 264, 225, 264);
        graphics.blit(WELCOME_LOGO, this.leftPos + 3, this.topPos + -18, 0, 0, 168, 73, 168, 73);

        // Decorations
        Calendar now = Calendar.getInstance();
        int month = now.get(Calendar.MONTH);
        int day = now.get(Calendar.DAY_OF_MONTH);
        if (month == Calendar.DECEMBER) {
            graphics.blit(DECO_CHRISTMAS, this.leftPos + 11, this.topPos + -56, 0, 0, 151, 42, 151, 42);
        } else if (month == Calendar.OCTOBER) {
            graphics.blit(DECO_HALLOWEEN, this.leftPos + 11, this.topPos + -56, 0, 0, 151, 42, 151, 42);
        } else if (month == Calendar.JUNE && day >= 19 && day <= 26) {
            // Midsummer week: traditional Nordic celebration around the summer solstice, not the whole summer.
            graphics.blit(DECO_MIDSUMMER, this.leftPos + 11, this.topPos + -56, 0, 0, 151, 42, 151, 42);
        }

        // Render Gender Icons Next to Toggle
        if (mc.sayda.creraces.config.CreRacesConfig.GSTATE_ENABLED.get() && this.minecraft != null
                && this.minecraft.player != null) {
            mc.sayda.creraces.capability.DataUtils.getVariables(this.minecraft.player).ifPresent(vars -> {
                mc.sayda.creraces.race.Race currentRace = mc.sayda.creraces.race.RaceRegistry.get(vars.getRace());
                int gState = vars.getGState();
                if (currentRace != null && currentRace.getGState() == mc.sayda.creraces.engine.GState.FEMALE) {
                    gState = 1;
                } else if (currentRace != null && currentRace.getGState() == mc.sayda.creraces.engine.GState.MALE) {
                    gState = 0;
                }

                if (gState == 0) {
                    graphics.blit(M_ICON, this.leftPos - 97, this.topPos - 20, 0, 0, 16, 16, 16, 16);
                } else if (gState == 1) {
                    graphics.blit(F_ICON, this.leftPos - 97, this.topPos - 20, 0, 0, 16, 16, 16, 16);
                }
            });
        }

        RenderSystem.disableBlend();
    }

    private void renderLabels(@Nonnull GuiGraphics graphics, int mouseX, int mouseY) {
        // We moved the welcome labels to render() for centering and coloring

        if (this.font != null) {
            Component hint1 = java.util.Objects.requireNonNull(Component.translatable("gui.creraces.menu_gui.label_keybind_hint1",
                    java.util.Objects.requireNonNull(ModKeyMappings.SKILL_WHEEL.getTranslatedKeyMessage())));
            graphics.drawString(java.util.Objects.requireNonNull(this.font), java.util.Objects.requireNonNull(hint1), 20, 161, 0xb0b0b0);

            Component hint2 = java.util.Objects.requireNonNull(Component.translatable("gui.creraces.menu_gui.label_keybind_hint2",
                    java.util.Objects.requireNonNull(ModKeyMappings.ABILITY_A1.getTranslatedKeyMessage()),
                    java.util.Objects.requireNonNull(ModKeyMappings.ABILITY_A2.getTranslatedKeyMessage())));
            graphics.drawString(java.util.Objects.requireNonNull(this.font), hint2, 20, 171, 0xb0b0b0);

            Component hint3 = java.util.Objects.requireNonNull(Component.translatable("gui.creraces.menu_gui.label_keybind_hint3",
                    java.util.Objects.requireNonNull(ModKeyMappings.MENU_GUI.getTranslatedKeyMessage())));
            graphics.drawString(java.util.Objects.requireNonNull(this.font), hint3, 20, 181, 0xb0b0b0);
        }

        if (mc.sayda.creraces.config.CreRacesConfig.GSTATE_ENABLED.get() && this.minecraft != null
                && this.minecraft.player != null) {
            graphics.drawString(java.util.Objects.requireNonNull(this.font),
                    Component.translatable("gui.creraces.menu_gui.label_gstate_change"), -79, -20, -1, false);
            graphics.drawString(java.util.Objects.requireNonNull(this.font),
                    Component.translatable("gui.creraces.menu_gui.label_gstate_appearance"), -79, -11, -1, false);

            mc.sayda.creraces.capability.DataUtils.getVariables(this.minecraft.player).ifPresent(vars -> {
                mc.sayda.creraces.race.Race currentRace = mc.sayda.creraces.race.RaceRegistry.get(vars.getRace());
                int gState = vars.getGState();
                if (currentRace != null && currentRace.getGState() == mc.sayda.creraces.engine.GState.FEMALE) {
                    gState = 1;
                } else if (currentRace != null && currentRace.getGState() == mc.sayda.creraces.engine.GState.MALE) {
                    gState = 0;
                }
                net.minecraft.network.chat.Component stateComp = (gState == 1)
                        ? Component.translatable("gui.creraces.menu_gui.gstate_feminine")
                                .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE)
                        : Component.translatable("gui.creraces.menu_gui.gstate_masculine")
                                .withStyle(net.minecraft.ChatFormatting.BLUE);
                graphics.drawString(java.util.Objects.requireNonNull(this.font), stateComp, -73, 2, -1, false);
            });
        }
    }
}
