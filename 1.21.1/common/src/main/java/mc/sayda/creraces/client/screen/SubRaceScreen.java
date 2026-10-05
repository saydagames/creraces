package mc.sayda.creraces.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import mc.sayda.creraces.race.Race;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

/** One parent race's sub-races, in the same grid as RaceSelectionScreen but on a single page. */
public class SubRaceScreen extends Screen {
    private final Screen parent;
    private final List<Race> subRaces;
    private int leftPos;
    private int topPos;

    public SubRaceScreen(Screen parent, Component groupName, List<Race> subRaces) {
        super(groupName);
        this.parent = parent;
        this.subRaces = subRaces.stream()
                .sorted(Comparator.comparing(Race::index).thenComparing(r -> r.name().getString()))
                .toList();
    }

    @Override
    protected void init() {
        this.leftPos = RaceMenuArt.panelLeft(this.width);
        this.topPos = RaceMenuArt.panelTop(this.height);

        this.addRenderableWidget(RaceMenuArt.backArrow(this.leftPos, this.topPos, btn -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(parent);
            }
        }));

        for (int slot = 0; slot < Math.min(subRaces.size(), RaceMenuArt.GRID_SLOTS); slot++) {
            Race race = subRaces.get(slot);
            this.addRenderableWidget(RaceMenuArt.selectButton(this.leftPos, this.topPos, slot, btn -> {
                if (this.minecraft != null) {
                    this.minecraft.setScreen(new RaceDetailsScreen(this, race));
                }
            }));
        }
    }

    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        renderPanel(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        Component stateTooltip = hoveredStateTooltip(mouseX, mouseY);
        if (stateTooltip != null) {
            graphics.renderTooltip(this.font, stateTooltip, mouseX, mouseY);
        }

        RenderSystem.disableBlend();
    }

    private void renderPanel(GuiGraphics graphics) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RaceMenuArt.drawPanel(graphics, this.leftPos, this.topPos);

        for (int slot = 0; slot < RaceMenuArt.GRID_SLOTS; slot++) {
            int x = RaceMenuArt.portraitX(this.leftPos, slot);
            int y = RaceMenuArt.portraitY(this.topPos, slot);
            if (slot < subRaces.size()) {
                Race race = subRaces.get(slot);
                RaceMenuArt.drawPortrait(graphics, race.portrait(), race.state(), x, y);
            } else {
                RaceMenuArt.drawEmptySlot(graphics, x, y);
            }
        }
    }

    @Nullable
    private Component hoveredStateTooltip(int mouseX, int mouseY) {
        for (int slot = 0; slot < Math.min(subRaces.size(), RaceMenuArt.GRID_SLOTS); slot++) {
            Component tooltip = RaceMenuArt.stateTooltip(subRaces.get(slot).state(),
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

    // Drawn at the top of render() instead, so Screen.render() does not blur over the panel.
    @Override
    public void renderBackground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }
}
