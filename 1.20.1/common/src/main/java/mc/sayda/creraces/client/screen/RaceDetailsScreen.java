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
        private static final ResourceLocation TRAITS_BUTTON = new ResourceLocation("creraces",
                        "textures/screens/atlas/imagebutton_traits_button.png");
        @Nonnull
        private static final ResourceLocation INFO_BUTTON = new ResourceLocation("creraces",
                        "textures/screens/atlas/imagebutton_info_button.png");

        @Nonnull
        private static final ResourceLocation INFO_ICON = new ResourceLocation("creraces", "textures/screens/info.png");

        @Nonnull
        private static final ResourceLocation DECO_CHRISTMAS = new ResourceLocation("creraces",
                        "textures/screens/christmas_decoration.png");
        @Nonnull
        private static final ResourceLocation DECO_HALLOWEEN = new ResourceLocation("creraces",
                        "textures/screens/halloween_decoration.png");
        @Nonnull
        private static final ResourceLocation DECO_MIDSUMMER = new ResourceLocation("creraces",
                        "textures/screens/midsummer_decoration.png");

        private final Screen parent;
        private final Race race;
        private int leftPos, topPos;

        private final java.util.Map<ResourceLocation, Component> fetchedDescription = new java.util.concurrent.ConcurrentHashMap<>();
        private final java.util.Set<ResourceLocation> isFetching = java.util.concurrent.ConcurrentHashMap.newKeySet();

        public RaceDetailsScreen(Screen parent, Race race) {
                super(race != null ? race.name() : Component.literal("Unknown Race"));
                this.parent = parent;
                this.race = race;
        }

        private int infoPage = 0;
        private int maxInfoPages = 1;

        @Override
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
                                        }
                                }));

                this.addRenderableWidget(new ImageButton(
                                infoX + 40, infoY, 40, 20,
                                0, 0, 20, ARROW_RIGHT, 40, 40,
                                btn -> {
                                        if (infoPage < maxInfoPages - 1) {
                                                infoPage++;
                                        }
                                }));
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
                graphics.blit(race.bgTexture(), this.leftPos + -4, this.topPos + -26, 0, 0, 181, 220, 181, 220);
                graphics.blit(SELECTION_BORDER, this.leftPos + -25, this.topPos + -47, 0, 0, 225, 264, 225, 264);

                // 2. Splash Art
                graphics.blit(race.splash(), this.leftPos + race.splashX(), this.topPos + race.splashY(), 0, 0,
                                race.splashW(),
                                race.splashH(), race.splashW(), race.splashH());

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

                graphics.blit(INFO_ICON, this.leftPos + 2, this.topPos + -2, 0, 0, 16, 16, 16, 16);

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

                renderInfoPanel(graphics);

                super.render(graphics, mouseX, mouseY, partialTick);
        }

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
                        title = Component.translatable("gui.creraces.race_info.passives");
                        var passives = race.passives();
                        if (passives != null) {
                                if (passives.canBreatheUnderwater())
                                        content.add(Component
                                                        .translatable("gui.creraces.passive.can_breathe_underwater"));
                                if (passives.canFly())
                                        content.add(Component.translatable("gui.creraces.passive.can_fly"));
                                if (passives.immuneToKnockback())
                                        content.add(Component.translatable("gui.creraces.passive.immune_to_knockback"));
                                if (passives.burnsInSunlight())
                                        content.add(Component.translatable("gui.creraces.passive.burns_in_sunlight"));
                                if (passives.noNaturalRegeneration())
                                        content.add(Component
                                                        .translatable("gui.creraces.passive.no_natural_regeneration"));
                                if (passives.nightVision())
                                        content.add(Component.translatable("gui.creraces.passive.night_vision"));
                        }
                } else {
                        int abilityIdx = infoPage - 2;
                        if (race.startingAbilities() != null && abilityIdx < race.startingAbilities().size()) {
                                ResourceLocation abilityId = race.startingAbilities().get(abilityIdx);
                                mc.sayda.creraces.ability.Ability ability = mc.sayda.creraces.ability.AbilityRegistry
                                                .get(abilityId);
                                if (ability != null) {
                                        title = ability.name();
                                        var config = mc.sayda.creraces.ability.AbilityRegistry.getRemoteDoc(abilityId);
                                        content.add(getRemoteDescription(abilityId, config, ability.description()));
                                } else {
                                        title = Component.literal("Unknown Ability");
                                }
                        } else {
                                title = Component.literal("End of Info");
                        }
                }

                // Draw Panel Content
                graphics.drawString(this.font, title, x, y - 15, 0xFFFFFF, true);
                int lineY = y;
                for (Component comp : content) {
                        graphics.drawWordWrap(this.font, comp, x, lineY, width, 0xCCCCCC);
                        lineY += this.font.wordWrapHeight(comp, width) + 5;
                }

                // Draw Pagination Counter
                String counter = (infoPage + 1) + "/" + maxInfoPages;
                graphics.drawCenteredString(this.font, counter, this.leftPos - 110, this.topPos + 180, 0xFFFFFF);
        }

        private Component getRemoteDescription(ResourceLocation id, mc.sayda.creraces.util.RemoteDocConfig config,
                        Component fallback) {
                if (config == null || config.source().isEmpty())
                        return fallback;

                Component alreadyFetched = fetchedDescription.get(id);
                if (alreadyFetched != null) {
                        return alreadyFetched;
                }

                String cached = mc.sayda.creraces.util.DocCache.get(id);
                if (cached != null) {
                        Component comp = Component.literal(cached);
                        fetchedDescription.put(id, comp);
                        return comp;
                }

                if (!isFetching.contains(id)) {
                        isFetching.add(id);
                        mc.sayda.creraces.util.DocFetcher.fetch(config.source(), config.selector())
                                        .handle((result, ex) -> {
                                                try {
                                                        String contentStr = (result != null && !result.isEmpty())
                                                                        ? result
                                                                        : config.fallback();
                                                        if (contentStr == null || contentStr.isEmpty()) {
                                                                if (ex != null) {
                                                                        contentStr = Component.translatable(
                                                                                        "gui.creraces.failed_to_load")
                                                                                        .getString();
                                                                } else {
                                                                        contentStr = fallback.getString();
                                                                }
                                                        }

                                                        final String finalContent = contentStr;
                                                        if (result != null && !result.isEmpty()) {
                                                                mc.sayda.creraces.util.DocCache.store(id, finalContent);
                                                        }
                                                        fetchedDescription.put(id, Component.literal(finalContent));
                                                } finally {
                                                        isFetching.remove(id);
                                                }
                                                return null;
                                        });
                }

                return Component.translatable("gui.creraces.loading");
        }

        @Override
        public boolean isPauseScreen() {
                return false;
        }
}
