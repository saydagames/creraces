package mc.sayda.creraces.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import mc.sayda.creraces.ability.Ability;
import mc.sayda.creraces.ability.AbilityRegistry;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.client.ClientAccess;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.engine.GState;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.network.SetRacePacket;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import mc.sayda.creraces.util.DocCache;
import mc.sayda.creraces.util.RemoteDocFetcher;
import mc.sayda.creraces.util.WikiUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * One race's page: its art and difficulty, a Select button while the player has no race yet, and
 * a paged info panel covering the description, the passive and each starting ability.
 */
@SuppressWarnings("null")
public class RaceDetailsScreen extends Screen {
    @Nonnull
    private static final ResourceLocation SELECTION_TITLE = new ResourceLocation("creraces",
            "textures/screens/selection_title.png");
    @Nonnull
    private static final ResourceLocation INFO_ICON = new ResourceLocation("creraces", "textures/screens/info.png");
    @Nonnull
    private static final ResourceLocation REFRESH_ICON = new ResourceLocation("creraces",
            "textures/screens/refresh.png");
    @Nonnull
    private static final ResourceLocation BOTH_GENDERS_ICON = new ResourceLocation("creraces",
            "textures/screens/mf.png");

    // Info panel pages: the description, the passive, then one page per starting ability.
    private static final int DESCRIPTION_PAGE = 0;
    private static final int PASSIVE_PAGE = 1;
    private static final int FIRST_ABILITY_PAGE = 2;
    private static final int INFO_PANEL_WIDTH = 110;
    private static final int INFO_PANEL_HEIGHT = 170;
    private static final int SCROLL_SPEED = 12;

    private final Screen parent;
    private final Race race;
    private int leftPos;
    private int topPos;
    private int infoPage = 0;
    private int infoPageCount = 1;
    private double scrollAmount = 0;

    public RaceDetailsScreen(Screen parent, Race race) {
        super(race != null ? race.name() : Component.translatable("gui.creraces.race_info.unknown_race"));
        this.parent = parent;
        this.race = race;
    }

    @Override
    protected void init() {
        this.leftPos = RaceMenuArt.panelLeft(this.width);
        this.topPos = RaceMenuArt.panelTop(this.height);
        int abilityPages = race != null && race.startingAbilities() != null ? race.startingAbilities().size() : 0;
        this.infoPageCount = FIRST_ABILITY_PAGE + abilityPages;

        this.addRenderableWidget(RaceMenuArt.backArrow(this.leftPos, this.topPos, btn -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(parent);
            }
        }));

        DataUtils.getVariables(this.minecraft.player).ifPresent(vars -> {
            if (!vars.hasChosenRace()) {
                this.addRenderableWidget(new GenericRaceButton(this.leftPos - 41, this.topPos - 35, 48, 20,
                        Component.translatable("gui.creraces.button.select"), btn -> {
                            ClientAccess.isWaitingForRaceSelection = true;
                            vars.setHasChosenRace(true); // Optimistic update
                            BoundaryHandler.sendSetRace(new SetRacePacket(race.id()));
                            if (this.minecraft != null && this.minecraft.player != null) {
                                this.minecraft.player.closeContainer();
                                this.minecraft.setScreen(null);
                            }
                        }));
            }
        });

        int pagerX = this.leftPos - 110;
        int pagerY = this.topPos + 175;
        this.addRenderableWidget(RaceMenuArt.textureButton(pagerX - 40, pagerY, 40, 20, 0, 0, 20,
                RaceMenuArt.ARROW_LEFT, 40, 60, btn -> turnInfoPage(-1)));
        this.addRenderableWidget(RaceMenuArt.textureButton(pagerX + 40, pagerY, 40, 20, 0, 0, 20,
                RaceMenuArt.ARROW_RIGHT, 40, 60, btn -> turnInfoPage(1)));

        // The wiki and refresh buttons only make sense for races with a linked wiki page.
        if (race != null && RaceRegistry.getRemoteDoc(race.id()) != null) {
            this.addRenderableWidget(RaceMenuArt.textureButton(this.leftPos + 2, this.topPos - 2, 16, 16, 0, 0, 16,
                    INFO_ICON, 16, 32, btn -> ConfirmLinkScreen.confirmLinkNow(currentPageWikiUrl(), this, true)));
            this.addRenderableWidget(RaceMenuArt.textureButton(this.leftPos + 2, this.topPos + 16, 16, 16, 16, 0, 16,
                    REFRESH_ICON, 16, 32, btn -> {
                        DocCache.clear();
                        RemoteDocFetcher.clearCache();
                    }));
        }
    }

    private void turnInfoPage(int delta) {
        int next = infoPage + delta;
        if (next >= 0 && next < infoPageCount) {
            infoPage = next;
            scrollAmount = 0;
        }
    }

    /** The wiki page for whatever the info panel shows: the race itself, or the current ability. */
    private String currentPageWikiUrl() {
        if (infoPage < FIRST_ABILITY_PAGE) {
            return WikiUtils.getRaceUrl(race.name());
        }
        Ability ability = AbilityRegistry.get(race.startingAbilities().get(infoPage - FIRST_ABILITY_PAGE));
        return ability != null ? WikiUtils.getAbilityUrl(ability.name()) : WikiUtils.getBaseWikiUrl();
    }

    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        if (this.race == null) {
            graphics.drawCenteredString(this.font, Component.translatable("gui.creraces.race_info.missing"),
                    this.width / 2, this.height / 2, 0xFF0000);
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }

        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        renderRaceArt(graphics);
        RaceMenuArt.drawSeasonalDecoration(graphics, this.leftPos + 11, this.topPos - 65);
        if (CreRacesConfig.GSTATE_ENABLED.get()) {
            renderGenderIndicator(graphics, mouseX, mouseY);
        }
        renderInfoPanel(graphics);

        super.render(graphics, mouseX, mouseY, partialTick);
        RenderSystem.disableBlend();
    }

    private void renderRaceArt(GuiGraphics graphics) {
        if (race.bgTexture() != null) {
            RaceMenuArt.drawBackdrop(graphics, race.bgTexture(), this.leftPos, this.topPos);
        }
        RaceMenuArt.drawBorder(graphics, this.leftPos, this.topPos);

        if (race.splash() != null) {
            graphics.blit(race.splash(), this.leftPos + race.splashX(), this.topPos + race.splashY(), 0, 0,
                    race.splashW(), race.splashH(), race.splashW(), race.splashH());
        }

        graphics.blit(SELECTION_TITLE, this.leftPos - 7, this.topPos - 54, 0, 0, 188, 60, 188, 60);
        if (race.nameTexture() != null) {
            graphics.blit(race.nameTexture(), this.leftPos + race.nameTexX(), this.topPos + race.nameTexY(), 0, 0,
                    race.nameTexW(), race.nameTexH(), race.nameTexW(), race.nameTexH());
        } else {
            graphics.drawCenteredString(this.font, race.name(), this.leftPos + 88, this.topPos - 30, 0xFFFFFF);
        }

        ResourceLocation difficulty = new ResourceLocation("creraces",
                "textures/screens/difficulty_" + race.difficulty() + ".png");
        graphics.blit(difficulty, this.leftPos + 41, this.topPos + 179, 0, 0, 93, 12, 93, 12);
    }

    private void renderGenderIndicator(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = this.leftPos - 142;
        int y = this.topPos - 35;
        ResourceLocation icon = race.getGState() == GState.MALE ? RaceMenuArt.MALE_ICON
                : race.getGState() == GState.FEMALE ? RaceMenuArt.FEMALE_ICON
                : BOTH_GENDERS_ICON;
        graphics.blit(icon, x, y, 0, 0, 16, 16, 16, 16);

        if (mouseX > x && mouseX < x + 16 && mouseY > y && mouseY < y + 16) {
            graphics.renderComponentTooltip(this.font, List.of(
                    Component.translatable("gui.creraces.menu_gui.tooltip_gender_star1"),
                    Component.translatable("gui.creraces.menu_gui.tooltip_gender_star2")), mouseX, mouseY);
        }
    }

    private void renderInfoPanel(GuiGraphics graphics) {
        int x = this.leftPos - 140;
        int y = this.topPos;

        Component title;
        Component body = null;
        if (infoPage == DESCRIPTION_PAGE) {
            title = Component.translatable("gui.creraces.race_info.description");
            body = RemoteDocFetcher.getRemoteDescription(race.id(), RaceRegistry.getRemoteDoc(race.id()),
                    race.description());
        } else if (infoPage == PASSIVE_PAGE) {
            title = Component.translatable("gui.creraces.race_info.passive");
            body = RemoteDocFetcher.getRemotePassive(race.id(), RaceRegistry.getRemotePassive(race.id()),
                    Component.translatable("gui.creraces.race_info.no_passive"));
        } else {
            int abilityIndex = infoPage - FIRST_ABILITY_PAGE;
            List<ResourceLocation> abilities = race.startingAbilities();
            if (abilities == null || abilityIndex >= abilities.size()) {
                title = Component.translatable("gui.creraces.race_info.end");
            } else {
                ResourceLocation abilityId = abilities.get(abilityIndex);
                Ability ability = AbilityRegistry.get(abilityId);
                if (ability != null) {
                    title = ability.name();
                    body = abilityDescription(abilityId, ability);
                } else {
                    title = Component.translatable("gui.creraces.race_info.unknown_ability");
                }
            }
        }

        graphics.drawString(this.font, title, x, y - 15, 0xFFFFFF, true);
        renderScrollingBody(graphics, body, x, y);

        String counter = (infoPage + 1) + "/" + infoPageCount;
        graphics.drawCenteredString(this.font, counter, this.leftPos - 110, this.topPos + 180, 0xFFFFFF);
    }

    /** The full remote write-up when there is one, otherwise the short remote description. */
    private static Component abilityDescription(ResourceLocation abilityId, Ability ability) {
        var fullDoc = AbilityRegistry.getRemoteFullDoc(abilityId);
        if (fullDoc != null) {
            return RemoteDocFetcher.getRemoteFullDescription(abilityId, fullDoc, ability.description());
        }
        return RemoteDocFetcher.getRemoteDescription(abilityId, AbilityRegistry.getRemoteDoc(abilityId),
                ability.description());
    }

    private void renderScrollingBody(GuiGraphics graphics, @Nullable Component body, int x, int y) {
        int contentHeight = body != null ? this.font.wordWrapHeight(body, INFO_PANEL_WIDTH) + 5 : 0;
        int maxScroll = Math.max(0, contentHeight - INFO_PANEL_HEIGHT);
        this.scrollAmount = Mth.clamp(this.scrollAmount, 0, maxScroll);

        if (body != null) {
            graphics.enableScissor(x, y, x + INFO_PANEL_WIDTH, y + INFO_PANEL_HEIGHT);
            graphics.pose().pushPose();
            graphics.pose().translate(0, -this.scrollAmount, 0);
            graphics.drawWordWrap(this.font, body, x, y, INFO_PANEL_WIDTH, 0xCCCCCC);
            graphics.pose().popPose();
            graphics.disableScissor();
        }

        if (maxScroll > 0) {
            int scrollbarX = x + INFO_PANEL_WIDTH + 2;
            int barHeight = Math.max(10, INFO_PANEL_HEIGHT * INFO_PANEL_HEIGHT / contentHeight);
            int barTop = y + (int) ((INFO_PANEL_HEIGHT - barHeight) * (this.scrollAmount / maxScroll));
            graphics.fill(scrollbarX, barTop, scrollbarX + 2, barTop + barHeight, 0xAAFFFFFF);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        this.scrollAmount -= delta * SCROLL_SPEED;
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
