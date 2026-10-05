package mc.sayda.creraces.client.screen;

import mc.sayda.creraces.race.Race;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Calendar;
import java.util.Locale;

/**
 * Textures and layout shared by the race menu screens (welcome menu, race and sub-race grids, race
 * details). Positions are offsets from the panel origin, which each screen centres itself.
 */
@SuppressWarnings("null")
final class RaceMenuArt {
    static final int PANEL_WIDTH = 176;
    static final int PANEL_HEIGHT = 166;

    static final ResourceLocation ARROW_LEFT = ResourceLocation.fromNamespaceAndPath("creraces",
            "textures/screens/atlas/arrow_left.png");
    static final ResourceLocation ARROW_RIGHT = ResourceLocation.fromNamespaceAndPath("creraces",
            "textures/screens/atlas/arrow_right.png");
    static final ResourceLocation MALE_ICON = ResourceLocation.fromNamespaceAndPath("creraces", "textures/screens/m.png");
    static final ResourceLocation FEMALE_ICON = ResourceLocation.fromNamespaceAndPath("creraces", "textures/screens/f.png");

    private static final ResourceLocation SELECTION_BG = ResourceLocation.fromNamespaceAndPath("creraces",
            "textures/screens/selection_bg.png");
    private static final ResourceLocation SELECTION_BORDER = ResourceLocation.fromNamespaceAndPath("creraces",
            "textures/screens/selection_border.png");
    private static final ResourceLocation EMPTY_SLOT = ResourceLocation.fromNamespaceAndPath("creraces", "textures/screens/race.png");
    private static final ResourceLocation BADGE_NEW = ResourceLocation.fromNamespaceAndPath("creraces",
            "textures/screens/portrait_info.png");
    private static final ResourceLocation BADGE_EXPERIMENTAL = ResourceLocation.fromNamespaceAndPath("creraces",
            "textures/screens/portrait_error.png");
    private static final ResourceLocation BADGE_UNFINISHED = ResourceLocation.fromNamespaceAndPath("creraces",
            "textures/screens/portrait_warning.png");
    private static final ResourceLocation DECO_CHRISTMAS = ResourceLocation.fromNamespaceAndPath("creraces",
            "textures/screens/christmas_decoration.png");
    private static final ResourceLocation DECO_HALLOWEEN = ResourceLocation.fromNamespaceAndPath("creraces",
            "textures/screens/halloween_decoration.png");
    private static final ResourceLocation DECO_MIDSUMMER = ResourceLocation.fromNamespaceAndPath("creraces",
            "textures/screens/midsummer_decoration.png");

    // Race grid: three by three slots per page, matching the cut-outs in the panel art.
    static final int GRID_SLOTS = 9;
    private static final int GRID_COLUMNS = 3;
    private static final int[] PORTRAIT_COLUMNS = { 8, 67, 124 };
    private static final int[] PORTRAIT_ROWS = { -20, 49, 119 };
    private static final int[] BUTTON_COLUMNS = { 5, 63, 120 };
    private static final int[] BUTTON_ROWS = { -19, 50, 120 };
    private static final int PORTRAIT_SIZE = 43;

    private RaceMenuArt() {
    }

    static int panelLeft(int screenWidth) {
        return (screenWidth - PANEL_WIDTH) / 2;
    }

    static int panelTop(int screenHeight) {
        return (screenHeight - PANEL_HEIGHT) / 2;
    }

    /** The standard backdrop plus the frame around it. */
    static void drawPanel(GuiGraphics graphics, int left, int top) {
        drawBackdrop(graphics, SELECTION_BG, left, top);
        drawBorder(graphics, left, top);
    }

    static void drawBackdrop(GuiGraphics graphics, ResourceLocation texture, int left, int top) {
        graphics.blit(texture, left - 4, top - 26, 0, 0, 181, 220, 181, 220);
    }

    static void drawBorder(GuiGraphics graphics, int left, int top) {
        graphics.blit(SELECTION_BORDER, left - 25, top - 47, 0, 0, 225, 264, 225, 264);
    }

    /** Christmas, Halloween or midsummer art along the top of the panel, when the date calls for it. */
    static void drawSeasonalDecoration(GuiGraphics graphics, int x, int y) {
        ResourceLocation decoration = seasonalDecoration();
        if (decoration != null) {
            graphics.blit(decoration, x, y, 0, 0, 151, 42, 151, 42);
        }
    }

    @Nullable
    private static ResourceLocation seasonalDecoration() {
        Calendar now = Calendar.getInstance();
        int month = now.get(Calendar.MONTH);
        int day = now.get(Calendar.DAY_OF_MONTH);
        if (month == Calendar.DECEMBER) {
            return DECO_CHRISTMAS;
        }
        if (month == Calendar.OCTOBER) {
            return DECO_HALLOWEEN;
        }
        // Midsummer week: the Nordic celebration around the solstice, not the whole summer.
        if (month == Calendar.JUNE && day >= 19 && day <= 26) {
            return DECO_MIDSUMMER;
        }
        return null;
    }

    /** The large arrow under the panel's left edge, used for Back and previous page. */
    static Button backArrow(int left, int top, Button.OnPress onPress) {
        return textureButton(left - 24, top + 184, 86, 48, 0, 0, 48, ARROW_LEFT, 86, 144, onPress);
    }

    static Button nextArrow(int left, int top, Button.OnPress onPress) {
        return textureButton(left + 113, top + 184, 86, 48, 0, 0, 48, ARROW_RIGHT, 86, 144, onPress);
    }

    /**
     * A button drawn from a texture holding its normal, hovered and disabled frames stacked
     * vertically, {@code frameOffset} pixels apart.
     */
    static Button textureButton(int x, int y, int width, int height, int u, int v, int frameOffset,
            ResourceLocation texture, int textureWidth, int textureHeight, Button.OnPress onPress) {
        return new TextureButton(x, y, width, height, u, v, frameOffset, texture, textureWidth, textureHeight, onPress);
    }

    static int portraitX(int left, int slot) {
        return left + PORTRAIT_COLUMNS[slot % GRID_COLUMNS];
    }

    static int portraitY(int top, int slot) {
        return top + PORTRAIT_ROWS[slot / GRID_COLUMNS];
    }

    /** The Select button under a grid slot's portrait. */
    static Button selectButton(int left, int top, int slot, Button.OnPress onPress) {
        int x = left + BUTTON_COLUMNS[slot % GRID_COLUMNS];
        int y = top + BUTTON_ROWS[slot / GRID_COLUMNS] + 44;
        return new GenericRaceButton(x, y, 50, 20, Component.translatable("gui.creraces.button.select"), onPress);
    }

    /** A race portrait with its development-state badge on top, if the state has one. */
    static void drawPortrait(GuiGraphics graphics, ResourceLocation portrait, Race.RaceState state, int x, int y) {
        drawSquare(graphics, portrait, x, y);
        ResourceLocation badge = stateBadge(state);
        if (badge != null) {
            drawSquare(graphics, badge, x, y);
        }
    }

    static void drawEmptySlot(GuiGraphics graphics, int x, int y) {
        drawSquare(graphics, EMPTY_SLOT, x, y);
    }

    /** The state badge's tooltip while the mouse is over the portrait at x, y; otherwise null. */
    @Nullable
    static Component stateTooltip(Race.RaceState state, int x, int y, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + PORTRAIT_SIZE && mouseY >= y && mouseY < y + PORTRAIT_SIZE;
        if (!hovered || stateBadge(state) == null) {
            return null;
        }
        return Component.translatable("gui.creraces.status." + state.name().toLowerCase(Locale.ROOT));
    }

    @Nullable
    private static ResourceLocation stateBadge(Race.RaceState state) {
        return switch (state) {
            case NEW -> BADGE_NEW;
            case EXPERIMENTAL -> BADGE_EXPERIMENTAL;
            case UNFINISHED -> BADGE_UNFINISHED;
            default -> null;
        };
    }

    private static void drawSquare(GuiGraphics graphics, ResourceLocation texture, int x, int y) {
        graphics.blit(texture, x, y, 0, 0, PORTRAIT_SIZE, PORTRAIT_SIZE, PORTRAIT_SIZE, PORTRAIT_SIZE);
    }
}
