package mc.sayda.creraces.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Paged grid of the selectable root races. A parent race opens its sub-races instead of the details page. */
public class RaceSelectionScreen extends Screen {
    private int leftPos;
    private int topPos;
    private int page = 0;
    private List<RaceEntry> raceEntries = new ArrayList<>();

    public RaceSelectionScreen() {
        super(Component.translatable("screen.creraces.race_selection_grid"));
    }

    @Override
    protected void init() {
        this.leftPos = RaceMenuArt.panelLeft(this.width);
        this.topPos = RaceMenuArt.panelTop(this.height);

        if (this.minecraft != null) {
            raceEntries = RaceRegistry.getAll().stream()
                    .filter(RaceRegistry::isSelectableRoot)
                    .sorted(Comparator.comparing(Race::index).thenComparing(r -> r.name().getString()))
                    .map(r -> new RaceEntry(r.id(), r.name(), r.portrait(), RaceRegistry.isParent(r.id()), r.state()))
                    .toList();
        }

        rebuildButtons();
    }

    private void rebuildButtons() {
        this.clearWidgets();

        int firstIndex = page * RaceMenuArt.GRID_SLOTS;
        for (int slot = 0; slot < RaceMenuArt.GRID_SLOTS && firstIndex + slot < raceEntries.size(); slot++) {
            RaceEntry entry = raceEntries.get(firstIndex + slot);
            this.addRenderableWidget(RaceMenuArt.selectButton(this.leftPos, this.topPos, slot,
                    btn -> onRaceSelected(entry)));
        }

        this.addRenderableWidget(RaceMenuArt.backArrow(this.leftPos, this.topPos, btn -> {
            if (page > 0) {
                page--;
                rebuildButtons();
            } else if (this.minecraft != null) {
                this.minecraft.setScreen(new MenuGUIScreen());
            }
        }));
        this.addRenderableWidget(RaceMenuArt.nextArrow(this.leftPos, this.topPos, btn -> {
            if ((page + 1) * RaceMenuArt.GRID_SLOTS < raceEntries.size()) {
                page++;
                rebuildButtons();
            }
        }));
    }

    private void onRaceSelected(RaceEntry entry) {
        if (this.minecraft == null) {
            return;
        }
        if (entry.isParentGroup()) {
            this.minecraft.setScreen(new SubRaceScreen(this, entry.name(), RaceRegistry.getSubRaces(entry.id())));
        } else {
            Race race = RaceRegistry.get(entry.id());
            if (race != null) {
                this.minecraft.setScreen(new RaceDetailsScreen(this, race));
            }
        }
    }

    @Override
    @SuppressWarnings("null")
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        renderPanel(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        Component stateTooltip = hoveredStateTooltip(mouseX, mouseY);
        if (stateTooltip != null) {
            graphics.renderTooltip(this.font, stateTooltip, mouseX, mouseY);
        }

        RenderSystem.disableBlend();

        int pageCount = (raceEntries.size() + RaceMenuArt.GRID_SLOTS - 1) / RaceMenuArt.GRID_SLOTS;
        Component pageCounter = Component.translatable("gui.creraces.selection.page", page + 1, pageCount);
        graphics.drawString(this.font, pageCounter, this.leftPos + 73, this.topPos + 205, -1, false);
    }

    private void renderPanel(GuiGraphics graphics) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RaceMenuArt.drawPanel(graphics, this.leftPos, this.topPos);

        int firstIndex = page * RaceMenuArt.GRID_SLOTS;
        for (int slot = 0; slot < RaceMenuArt.GRID_SLOTS; slot++) {
            int x = RaceMenuArt.portraitX(this.leftPos, slot);
            int y = RaceMenuArt.portraitY(this.topPos, slot);
            if (firstIndex + slot < raceEntries.size()) {
                RaceEntry entry = raceEntries.get(firstIndex + slot);
                RaceMenuArt.drawPortrait(graphics, entry.portrait(), entry.state(), x, y);
            } else {
                RaceMenuArt.drawEmptySlot(graphics, x, y);
            }
        }

        RaceMenuArt.drawSeasonalDecoration(graphics, this.leftPos + 11, this.topPos - 56);
    }

    @Nullable
    private Component hoveredStateTooltip(int mouseX, int mouseY) {
        int firstIndex = page * RaceMenuArt.GRID_SLOTS;
        for (int slot = 0; slot < RaceMenuArt.GRID_SLOTS && firstIndex + slot < raceEntries.size(); slot++) {
            Component tooltip = RaceMenuArt.stateTooltip(raceEntries.get(firstIndex + slot).state(),
                    RaceMenuArt.portraitX(this.leftPos, slot), RaceMenuArt.portraitY(this.topPos, slot),
                    mouseX, mouseY);
            if (tooltip != null) {
                return tooltip;
            }
        }
        return null;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record RaceEntry(ResourceLocation id, Component name, ResourceLocation portrait, boolean isParentGroup,
            Race.RaceState state) {
    }
}
