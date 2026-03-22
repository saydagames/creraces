package mc.sayda.creraces.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import mc.sayda.creraces.world.inventory.MenuGUIMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.Util;

import java.util.Calendar;
import javax.annotation.Nonnull;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

import mc.sayda.creraces.client.ModKeyMappings;

public class MenuGUIScreen extends AbstractContainerScreen<MenuGUIMenu> {
    private static final ResourceLocation SELECTION_BG = new ResourceLocation("creraces",
            "textures/screens/selection_bg.png");
    private static final ResourceLocation SELECTION_BORDER = new ResourceLocation("creraces",
            "textures/screens/selection_border.png");
    private static final ResourceLocation WELCOME_LOGO = new ResourceLocation("creraces",
            "textures/screens/welcome_logo.png");

    // Decorations
    private static final ResourceLocation DECO_CHRISTMAS = new ResourceLocation("creraces",
            "textures/screens/christmas_decoration.png");
    private static final ResourceLocation DECO_HALLOWEEN = new ResourceLocation("creraces",
            "textures/screens/halloween_decoration.png");
    private static final ResourceLocation DECO_MIDSUMMER = new ResourceLocation("creraces",
            "textures/screens/midsummer_decoration.png");

    private static final ResourceLocation M_ICON = new ResourceLocation("creraces", "textures/screens/m.png");
    private static final ResourceLocation F_ICON = new ResourceLocation("creraces", "textures/screens/f.png");
    private static final ResourceLocation MF_BUTTON = new ResourceLocation("creraces",
            "textures/screens/atlas/button_mf.png");

    public MenuGUIScreen(MenuGUIMenu container, Inventory inventory, Component text) {
        super(container, inventory, text);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();

        // Start Your Adventure
        this.addRenderableWidget(
                Button.builder(Component.translatable("gui.creraces.menu_gui.button_start_your_adventure1"), b -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new RaceSelectionScreen());
                    }
                }).bounds(this.leftPos + 21, this.topPos + 97, 133, 20).build());

        // Debug
        this.addRenderableWidget(
                Button.builder(Component.translatable("gui.creraces.menu_gui.button_debug"), b -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new DebugScreen());
                    }
                }).bounds(this.leftPos + 21, this.topPos + 124, 63, 20).build());

        // Extras (Mirror)
        this.addRenderableWidget(
                Button.builder(Component.translatable("gui.creraces.menu_gui.button_extras"), b -> {
                    if (this.minecraft != null) {
                        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                        dev.architectury.networking.NetworkManager.sendToServer(mc.sayda.creraces.network.RequestMirrorPacket.ID, buf);
                    }
                }).bounds(this.leftPos + 93, this.topPos + 124, 61, 20).build()
        );

        // Wiki Button
        this.addRenderableWidget(
                (Button) Button.builder(Component.translatable("gui.creraces.menu_gui.button_wiki"), b -> {
                    if (this.minecraft != null) {
                        String url = mc.sayda.creraces.util.WikiUtils.getBaseWikiUrl();
                        this.minecraft.setScreen(new ConfirmLinkScreen(confirmed -> {
                            if (confirmed) {
                                Util.getPlatform().openUri(url);
                            }
                            this.minecraft.setScreen(this);
                        }, url, true));
                    }
                }).bounds(this.leftPos + 21, this.topPos + 148, 133, 10).build());

        // Gender System Toggle
        if (mc.sayda.creraces.config.CreRacesConfig.GSTATE_ENABLED.get()) {
            mc.sayda.creraces.capability.DataUtils.getVariables(this.minecraft.player).ifPresent(vars -> {
                mc.sayda.creraces.race.Race currentRace = mc.sayda.creraces.race.RaceRegistry.get(vars.getRace());
                boolean forced = currentRace != null
                        && currentRace.getGState() != mc.sayda.creraces.engine.GState.BOTH;

                Button mfButton = Button.builder(Component.translatable("gui.creraces.menu_gui.button_mf"), button -> {
                    if (this.minecraft != null && this.minecraft.player != null) {
                        int gState = vars.getGState();
                        int nextState = gState == 0 ? 1 : 0;
                        vars.setGState(nextState);
                        mc.sayda.creraces.network.BoundaryHandler.sendGStateUpdate(nextState);
                    }
                }).bounds(this.leftPos - 70, this.topPos + 16, 40, 20).tooltip(
                        net.minecraft.client.gui.components.Tooltip
                                .create(forced ? Component.translatable("gui.creraces.menu_gui.tooltip_gender_locked")
                                        : Component.translatable("gui.creraces.menu_gui.tooltip_gender")))
                        .build();

                if (forced) {
                    mfButton.active = false;
                }

                this.addRenderableWidget(mfButton);
            });
        }
    }

    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);

        // Centered Welcome Text
        graphics.drawCenteredString(this.font, Component.translatable("gui.creraces.menu_gui.label_welcome1"),
                this.leftPos + (this.imageWidth / 2), this.topPos + 60, 0xFFFFFF);
        graphics.drawCenteredString(this.font, Component.translatable("gui.creraces.menu_gui.label_welcome2"),
                this.leftPos + (this.imageWidth / 2), this.topPos + 70, 0xFFFFFF);
        graphics.drawCenteredString(this.font, Component.translatable("gui.creraces.menu_gui.label_welcome3"),
                this.leftPos + (this.imageWidth / 2), this.topPos + 80, 0xFFFFFF);
    }

    @Override
    protected void renderBg(@Nonnull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        graphics.blit(SELECTION_BG, this.leftPos + -4, this.topPos + -26, 0, 0, 181, 220, 181, 220);
        graphics.blit(SELECTION_BORDER, this.leftPos + -25, this.topPos + -47, 0, 0, 225, 264, 225, 264);
        graphics.blit(WELCOME_LOGO, this.leftPos + 3, this.topPos + -18, 0, 0, 168, 73, 168, 73);

        // Decorations
        Calendar now = Calendar.getInstance();
        int month = now.get(Calendar.MONTH);
        if (month == Calendar.DECEMBER) {
            graphics.blit(DECO_CHRISTMAS, this.leftPos + 11, this.topPos + -56, 0, 0, 151, 42, 151, 42);
        } else if (month == Calendar.OCTOBER) {
            graphics.blit(DECO_HALLOWEEN, this.leftPos + 11, this.topPos + -56, 0, 0, 151, 42, 151, 42);
        } else if (month >= Calendar.JUNE && month <= Calendar.AUGUST) {
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

    @Override
    protected void renderLabels(@Nonnull GuiGraphics graphics, int mouseX, int mouseY) {
        // We moved the welcome labels to render() for centering and coloring

        graphics.drawString(this.font, Component.translatable("gui.creraces.menu_gui.label_keybind_hint1",
                ModKeyMappings.SKILL_WHEEL.getTranslatedKeyMessage()), 20, 161, 0xb0b0b0);
        graphics.drawString(this.font, Component.translatable("gui.creraces.menu_gui.label_keybind_hint2",
                ModKeyMappings.ABILITY_A1.getTranslatedKeyMessage(),
                ModKeyMappings.ABILITY_A2.getTranslatedKeyMessage()),
                20, 171, 0xb0b0b0);
        graphics.drawString(this.font, Component.translatable("gui.creraces.menu_gui.label_keybind_hint3",
                ModKeyMappings.MENU_GUI.getTranslatedKeyMessage()), 20, 181, 0xb0b0b0);

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
