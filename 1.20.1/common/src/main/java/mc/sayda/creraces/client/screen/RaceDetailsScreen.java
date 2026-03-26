package mc.sayda.creraces.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.network.SetRacePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.Calendar;

/**
 * Race details screen matching legacy RaceUndeadGUIScreen precisely.
 */
public class RaceDetailsScreen extends Screen {
        @Nonnull
        private static final ResourceLocation SELECTION_BORDER = new ResourceLocation("creraces",
                        "textures/screens/selection_border.png");
        @Nonnull
        private static final ResourceLocation SELECTION_TITLE = new ResourceLocation("creraces",
                        "textures/screens/selection_title.png");

        // Atlas buttons
        @Nonnull
        private static final ResourceLocation ARROW_LEFT = new ResourceLocation("creraces",
                        "textures/screens/atlas/arrow_left.png");
        @Nonnull
        private static final ResourceLocation ARROW_RIGHT = new ResourceLocation("creraces",
                        "textures/screens/atlas/arrow_right.png");

        @Nonnull
        private static final ResourceLocation INFO_ICON = new ResourceLocation("creraces", "textures/screens/info.png");

        @Nonnull
        private static final ResourceLocation REFRESH_ICON = new ResourceLocation("creraces",
                        "textures/screens/refresh.png");

        @Nonnull
        private static final ResourceLocation DECO_CHRISTMAS = new ResourceLocation("creraces",
                        "textures/screens/christmas_decoration.png");
        @Nonnull
        private static final ResourceLocation DECO_HALLOWEEN = new ResourceLocation("creraces",
                        "textures/screens/halloween_decoration.png");
        @Nonnull
        private static final ResourceLocation DECO_MIDSUMMER = new ResourceLocation("creraces",
                        "textures/screens/midsummer_decoration.png");

        @Nonnull
        private static final ResourceLocation M_ICON = new ResourceLocation("creraces", "textures/screens/m.png");
        @Nonnull
        private static final ResourceLocation F_ICON = new ResourceLocation("creraces", "textures/screens/f.png");
        @Nonnull
        private static final ResourceLocation MF_ICON = new ResourceLocation("creraces", "textures/screens/mf.png");

        private final Screen parent;
        private final Race race;
        private int leftPos, topPos;

        public RaceDetailsScreen(Screen parent, Race race) {
                super(race != null ? race.name() : Component.literal("Unknown Race"));
                this.parent = parent;
                this.race = race;
        }

        private int infoPage = 0;
        private int maxInfoPages = 1;
        private double scrollAmount = 0;
        private int maxScroll = 0;

        @Override
        @SuppressWarnings("null")
        protected void init() {
                this.leftPos = (this.width - 176) / 2;
                this.topPos = (this.height - 166) / 2;

                updateMaxInfoPages();

                // Big Arrow Left (Back)
                this.addRenderableWidget(new ImageButton(
                                this.leftPos + -24, this.topPos + 184, 86, 48,
                                0, 0, 48, ARROW_LEFT, 86, 96,
                                btn -> {
                                        if (this.minecraft != null) {
                                                this.minecraft.setScreen(parent);
                                        }
                                }));

                // Select Button (GenericRaceButton) - Left Position
                mc.sayda.creraces.capability.DataUtils.getVariables(this.minecraft.player).ifPresent(vars -> {
                        if (!vars.hasChosenRace()) {
                                this.addRenderableWidget(new GenericRaceButton(
                                                this.leftPos + -41, this.topPos + -35, 48, 20,
                                                Component.translatable("gui.creraces.button.select"),
                                                btn -> {
                                                        mc.sayda.creraces.client.ClientAccess.isWaitingForRaceSelection = true;
                                                        vars.setHasChosenRace(true); // Optimistic update
                                                        BoundaryHandler.sendSetRace(new SetRacePacket(race.id()));
                                                        if (this.minecraft != null && this.minecraft.player != null) {
                                                                this.minecraft.player.closeContainer();
                                                                this.minecraft.setScreen(null);
                                                        }
                                                }));
                        }
                });

                // Info Panel Navigation Arrows
                // Positioned on the left side where the info panel is
                int infoX = this.leftPos - 110;
                int infoY = this.topPos + 175;

                this.addRenderableWidget(new ImageButton(
                                infoX - 40, infoY, 40, 20,
                                0, 0, 20, ARROW_LEFT, 40, 40,
                                btn -> {
                                        if (infoPage > 0) {
                                                infoPage--;
                                                scrollAmount = 0;
                                        }
                                }));

                this.addRenderableWidget(new ImageButton(
                                infoX + 40, infoY, 40, 20,
                                0, 0, 20, ARROW_RIGHT, 40, 40,
                                btn -> {
                                        if (infoPage < maxInfoPages - 1) {
                                                infoPage++;
                                                scrollAmount = 0;
                                        }
                                }));

                // Dynamic Wiki and Refresh Buttons- Visible only if WikiPage is linked
                if (race != null && mc.sayda.creraces.race.RaceRegistry.getRemoteDoc(race.id()) != null) {
                        // Dynamic Wiki Button
                        this.addRenderableWidget(new ImageButton(
                                        this.leftPos + 2, this.topPos - 2, 16, 16,
                                        0, 0, 16, INFO_ICON, 16, 32,
                                        btn -> {
                                                String url;
                                                if (infoPage < 2) {
                                                        url = mc.sayda.creraces.util.WikiUtils.getRaceUrl(race.name());
                                                } else {
                                                        int abilityIdx = infoPage - 2;
                                                        ResourceLocation abilityId = race.startingAbilities()
                                                                        .get(abilityIdx);
                                                        mc.sayda.creraces.ability.Ability ability = mc.sayda.creraces.ability.AbilityRegistry
                                                                        .get(abilityId);
                                                        if (ability != null) {
                                                                url = mc.sayda.creraces.util.WikiUtils
                                                                                .getAbilityUrl(ability.name());
                                                        } else {
                                                                url = mc.sayda.creraces.util.WikiUtils.getBaseWikiUrl();
                                                        }
                                                }
                                                if (this.minecraft != null) {
                                                        this.minecraft.setScreen(
                                                                        new net.minecraft.client.gui.screens.ConfirmLinkScreen(
                                                                                        confirmed -> {
                                                                                                if (confirmed) {
                                                                                                        net.minecraft.Util
                                                                                                                        .getPlatform()
                                                                                                                        .openUri(url);
                                                                                                }
                                                                                                this.minecraft.setScreen(
                                                                                                                this);
                                                                                        }, url, true));
                                                }
                                        }));

                        // Doc Refresh Button
                        this.addRenderableWidget(new ImageButton(
                                        this.leftPos + 2, this.topPos + 16, 16, 16,
                                        16, 0, 16, REFRESH_ICON, 16, 32,
                                        btn -> {
                                                mc.sayda.creraces.util.DocCache.clear();
                                                mc.sayda.creraces.util.RemoteDocFetcher.clearCache();
                                        }));
                }
        }

        private void updateMaxInfoPages() {
                // Page 0: Description
                // Page 1: Passives
                // Page 2+: Abilities (one per page)
                int abilityPages = 0;
                if (race != null && race.startingAbilities() != null) {
                        abilityPages = race.startingAbilities().size();
                }
                this.maxInfoPages = 2 + abilityPages;
        }

        @Override
        @SuppressWarnings("null")
        public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                this.renderBackground(graphics);
                if (this.race == null) {
                        graphics.drawCenteredString(this.font, "Error: Race Data Missing", this.width / 2,
                                        this.height / 2, 0xFF0000);
                        super.render(graphics, mouseX, mouseY, partialTick);
                        return;
                }

                RenderSystem.setShaderColor(1, 1, 1, 1);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();

                // 1. BG and Border
                if (race.bgTexture() != null) {
                        graphics.blit(race.bgTexture(), this.leftPos + -4, this.topPos + -26, 0, 0, 181, 220, 181, 220);
                }
                graphics.blit(SELECTION_BORDER, this.leftPos + -25, this.topPos + -47, 0, 0, 225, 264, 225, 264);

                // 2. Splash Art
                if (race.splash() != null) {
                        graphics.blit(race.splash(), this.leftPos + race.splashX(), this.topPos + race.splashY(), 0, 0,
                                        race.splashW(),
                                        race.splashH(), race.splashW(), race.splashH());
                }

                // 3. Selection Title and Name Texture
                graphics.blit(SELECTION_TITLE, this.leftPos + -7, this.topPos + -54, 0, 0, 188, 60, 188, 60);
                if (race.nameTexture() != null) {
                        graphics.blit(race.nameTexture(), this.leftPos + race.nameTexX(), this.topPos + race.nameTexY(),
                                        0, 0,
                                        race.nameTexW(), race.nameTexH(), race.nameTexW(), race.nameTexH());
                } else {
                        graphics.drawCenteredString(this.font, race.name(), this.leftPos + 88, this.topPos + -30,
                                        0xFFFFFF);
                }

                // 4. Difficulty
                ResourceLocation diffTex = new ResourceLocation("creraces",
                                "textures/screens/difficulty_" + race.difficulty() + ".png");

                graphics.blit(diffTex, this.leftPos + 41, this.topPos + 179, 0, 0, 93, 12, 93, 12);

                // 6. Decorations
                Calendar now = Calendar.getInstance();
                int month = now.get(Calendar.MONTH);
                if (month == Calendar.DECEMBER) {
                        graphics.blit(DECO_CHRISTMAS, this.leftPos + 11, this.topPos + -65, 0, 0, 151, 42, 151, 42);
                } else if (month == Calendar.OCTOBER) {
                        graphics.blit(DECO_HALLOWEEN, this.leftPos + 11, this.topPos + -65, 0, 0, 151, 42, 151, 42);
                } else if (month >= Calendar.JUNE && month <= Calendar.AUGUST) {
                        graphics.blit(DECO_MIDSUMMER, this.leftPos + 11, this.topPos + -65, 0, 0, 151, 42, 151, 42);
                }

                // 7. Gender Indicator
                if (mc.sayda.creraces.config.CreRacesConfig.GSTATE_ENABLED.get()) {
                        if (race.getGState() == mc.sayda.creraces.engine.GState.MALE) {
                                graphics.blit(M_ICON, this.leftPos - 142, this.topPos - 35, 0, 0, 16, 16, 16, 16);
                        } else if (race.getGState() == mc.sayda.creraces.engine.GState.FEMALE) {
                                graphics.blit(F_ICON, this.leftPos - 142, this.topPos - 35, 0, 0, 16, 16, 16, 16);
                        } else {
                                graphics.blit(MF_ICON, this.leftPos - 142, this.topPos - 35, 0, 0, 16, 16, 16, 16);
                        }

                        if (mouseX > this.leftPos - 142 && mouseX < this.leftPos - 126 &&
                                        mouseY > this.topPos - 35 && mouseY < this.topPos - 19) {
                                java.util.List<Component> tooltip = new java.util.ArrayList<>();
                                tooltip.add(Component.translatable("gui.creraces.menu_gui.tooltip_gender_star1"));
                                tooltip.add(Component.translatable("gui.creraces.menu_gui.tooltip_gender_star2"));
                                graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
                        }
                }

                renderInfoPanel(graphics);

                super.render(graphics, mouseX, mouseY, partialTick);
        }

        @SuppressWarnings("null")
        private void renderInfoPanel(GuiGraphics graphics) {
                if (race == null)
                        return;

                // Position on the left side
                int x = this.leftPos - 140;
                int y = this.topPos;
                int width = 110;

                Component title;
                java.util.List<Component> content = new java.util.ArrayList<>();

                if (infoPage == 0) {
                        title = Component.translatable("gui.creraces.race_info.description");
                        var config = mc.sayda.creraces.race.RaceRegistry.getRemoteDoc(race.id());
                        content.add(getRemoteDescription(race.id(), config, race.description()));
                } else if (infoPage == 1) {
                        title = Component.translatable("gui.creraces.race_info.passive");
                        var config = mc.sayda.creraces.race.RaceRegistry.getRemotePassive(race.id());
                        content.add(getRemotePassive(race.id(), config,
                                        Component.literal("No passive information available.")));
                } else {
                        int abilityIdx = infoPage - 2;
                        if (race.startingAbilities() != null && abilityIdx < race.startingAbilities().size()) {
                                ResourceLocation abilityId = race.startingAbilities().get(abilityIdx);
                                mc.sayda.creraces.ability.Ability ability = mc.sayda.creraces.ability.AbilityRegistry
                                                .get(abilityId);
                                if (ability != null) {
                                        title = ability.name();
                                        var fullConfig = mc.sayda.creraces.ability.AbilityRegistry
                                                        .getRemoteFullDoc(abilityId);
                                        if (fullConfig != null) {
                                                content.add(mc.sayda.creraces.util.RemoteDocFetcher
                                                                .getRemoteFullDescription(abilityId, fullConfig,
                                                                                ability.description()));
                                        } else {
                                                var config = mc.sayda.creraces.ability.AbilityRegistry
                                                                .getRemoteDoc(abilityId);
                                                content.add(getRemoteDescription(abilityId, config,
                                                                ability.description()));
                                        }
                                } else {
                                        title = Component.literal("Unknown Ability");
                                }
                        } else {
                                title = Component.literal("End of Info");
                        }
                }

                // Draw Panel Content
                graphics.drawString(this.font, title, x, y - 15, 0xFFFFFF, true);

                int panelHeight = 170;
                int totalContentHeight = 0;
                for (Component comp : content) {
                        totalContentHeight += this.font.wordWrapHeight(comp, width) + 5;
                }

                this.maxScroll = Math.max(0, totalContentHeight - panelHeight);
                this.scrollAmount = net.minecraft.util.Mth.clamp(this.scrollAmount, 0, this.maxScroll);

                graphics.enableScissor(x, y, x + width, y + panelHeight);
                com.mojang.blaze3d.vertex.PoseStack pose = graphics.pose();
                pose.pushPose();
                pose.translate(0, -this.scrollAmount, 0);

                int lineY = y;
                for (Component comp : content) {
                        graphics.drawWordWrap(this.font, comp, x, lineY, width, 0xCCCCCC);
                        lineY += this.font.wordWrapHeight(comp, width) + 5;
                }

                pose.popPose();
                graphics.disableScissor();

                // Draw Scrollbar
                if (maxScroll > 0) {
                        int scrollbarX = x + width + 2;
                        int barHeight = Math.max(10, panelHeight * panelHeight / totalContentHeight);
                        int barTop = y + (int) ((panelHeight - barHeight) * (this.scrollAmount / maxScroll));
                        graphics.fill(scrollbarX, barTop, scrollbarX + 2, barTop + barHeight, 0xAAFFFFFF);
                }

                // Draw Pagination Counter
                String counter = (infoPage + 1) + "/" + maxInfoPages;
                graphics.drawCenteredString(this.font, counter, this.leftPos - 110, this.topPos + 180, 0xFFFFFF);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
                this.scrollAmount -= delta * 12;
                return true;
        }

        private Component getRemoteDescription(ResourceLocation id, mc.sayda.creraces.util.RemoteDocConfig config,
                        Component fallback) {
                return mc.sayda.creraces.util.RemoteDocFetcher.getRemoteDescription(id, config, fallback);
        }

        private Component getRemotePassive(ResourceLocation id, mc.sayda.creraces.util.RemoteDocConfig config,
                        Component fallback) {
                return mc.sayda.creraces.util.RemoteDocFetcher.getRemotePassive(id, config, fallback);
        }

        @Override
        public boolean isPauseScreen() {
                return false;
        }
}
