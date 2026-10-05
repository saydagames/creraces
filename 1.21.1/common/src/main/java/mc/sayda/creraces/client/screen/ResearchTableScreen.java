package mc.sayda.creraces.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import mc.sayda.creraces.ability.EssenceType;
import mc.sayda.creraces.ability.HexPos;
import mc.sayda.creraces.ability.HexRecipe;
import mc.sayda.creraces.ability.HexRecipeManager;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.network.CraftScrollPacket;
import mc.sayda.creraces.network.PlaceEssencePacket;
import mc.sayda.creraces.network.RemoveEssencePacket;
import mc.sayda.creraces.util.EssenceBeltHelper;
import mc.sayda.creraces.util.ItemNbt;
import mc.sayda.creraces.world.inventory.ResearchTableMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Hex-grid editor for drawing a scroll's essence pattern. Essences are dragged or painted from the
 * sidebar onto the grid; a crafted scroll, or a table without ink, shows its pattern read-only.
 */
public class ResearchTableScreen extends AbstractContainerScreen<ResearchTableMenu> {

    private static final ResourceLocation BG =
            ResourceLocation.fromNamespaceAndPath("creraces", "textures/screens/gui_research.png");
    private static final ResourceLocation SCROLL_OVERLAY =
            ResourceLocation.fromNamespaceAndPath("creraces", "textures/screens/gui_research_scroll.png");
    private static final ResourceLocation SLOT_BORDER_TEX =
            ResourceLocation.fromNamespaceAndPath("creraces", "textures/essence/slot_border.png");
    private static final Map<EssenceType, ResourceLocation> ESSENCE_TEXTURES = new EnumMap<>(EssenceType.class);

    static {
        for (EssenceType e : EssenceType.values()) {
            ESSENCE_TEXTURES.put(e, ResourceLocation.fromNamespaceAndPath("creraces",
                    "textures/essence/" + e.getSerializedName() + ".png"));
        }
    }

    private static final int INK_SLOT = 0;
    private static final int SCROLL_SLOT = 1;
    /** NBT key a crafted scroll stores its ability id under. */
    private static final String SCROLL_ABILITY_TAG = "Ability";

    // Hex grid geometry (pointy-top hexagons)
    private static final int HEX_SIZE = 14;   // center-to-tip (pixels)
    // Hex-of-hexes: all (q,r) where max(|q|, |r|, |q+r|) <= GRID_RADIUS
    private static final int GRID_RADIUS = 3; // 37 cells total (3n²+3n+1 for hexagon radius n)

    // Grid center in GUI-local space (offset from leftPos/topPos)
    private static final int GRID_CX = 248;  // roughly center of right panel
    private static final int GRID_CY = 93;

    // Sidebar origin (GUI-local)
    private static final int SIDEBAR_X = 12;
    private static final int SIDEBAR_Y = 30;
    private static final int ESSENCE_ICON_SIZE = HEX_SIZE * 2; // matches hex cell icon size
    private static final int ESSENCE_COLS = 4;

    // Craft button (GUI-local): fills the designated button strip in the texture
    private static final int CRAFT_BTN_X = 135;
    private static final int CRAFT_BTN_Y = 181;
    private static final int CRAFT_BTN_W = 226;
    private static final int CRAFT_BTN_H = 18;

    private final Map<HexPos, EssenceType> localGrid = new HashMap<>();
    /** Essence being dragged; dropping it on a cell places it. */
    @Nullable private EssenceType heldEssence = null;
    /** Essence in draw mode: clicking or dragging over cells paints it. */
    @Nullable private EssenceType selectedEssence = null;
    @Nullable private HexPos hoveredHex = null;
    private BlockPos tablePos = BlockPos.ZERO;
    @Nullable private Button craftButton = null;

    public ResearchTableScreen(ResearchTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 378;
        this.imageHeight = 378;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos += 2;
        this.topPos += 35;
        this.tablePos = this.menu.getTablePos();
        craftButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.creraces.research"),
                b -> BoundaryHandler.sendCraftScroll(new CraftScrollPacket(this.tablePos)))
            .bounds(leftPos + CRAFT_BTN_X, topPos + CRAFT_BTN_Y, CRAFT_BTN_W, CRAFT_BTN_H)
            .build());
    }

    public void receiveGridSync(Map<HexPos, EssenceType> grid) {
        localGrid.clear();
        localGrid.putAll(grid);
    }

    /** A crafted scroll is shown read-only, and so is everything while the table has no ink. */
    private boolean isViewOnly() {
        return menu.isScrollCrafted() || menu.getSlot(INK_SLOT).getItem().isEmpty();
    }

    /** The crafted scroll's recipe pattern, centred on the grid, or empty for a blank scroll. */
    private Map<HexPos, EssenceType> getScrollPattern() {
        CompoundTag tag = ItemNbt.get(menu.getSlot(SCROLL_SLOT).getItem());
        if (tag == null || !tag.contains(SCROLL_ABILITY_TAG)) {
            return Map.of();
        }
        ResourceLocation abilityId = ResourceLocation.tryParse(tag.getString(SCROLL_ABILITY_TAG));
        return HexRecipeManager.findByAbility(abilityId)
                .map(HexRecipe::pattern)
                .map(this::centerPattern)
                .orElse(Map.of());
    }

    private Map<HexPos, EssenceType> centerPattern(Map<HexPos, EssenceType> pattern) {
        if (pattern.isEmpty()) {
            return pattern;
        }
        int minQ = Integer.MAX_VALUE, maxQ = Integer.MIN_VALUE;
        int minR = Integer.MAX_VALUE, maxR = Integer.MIN_VALUE;
        for (HexPos pos : pattern.keySet()) {
            minQ = Math.min(minQ, pos.q()); maxQ = Math.max(maxQ, pos.q());
            minR = Math.min(minR, pos.r()); maxR = Math.max(maxR, pos.r());
        }
        int baseOffQ = Math.round((minQ + maxQ) / 2.0f);
        int baseOffR = Math.round((minR + maxR) / 2.0f);

        // Try the computed center, then nearby offsets, until every cell fits on the grid.
        int[] deltas = {0, -1, 1, -2, 2};
        for (int dq : deltas) {
            for (int dr : deltas) {
                Map<HexPos, EssenceType> candidate = shiftPattern(pattern, baseOffQ + dq, baseOffR + dr);
                if (candidate.keySet().stream().allMatch(pos -> inHexBounds(pos.q(), pos.r()))) {
                    return candidate;
                }
            }
        }
        // Pattern is larger than the grid; center as best we can
        return shiftPattern(pattern, baseOffQ, baseOffR);
    }

    private static Map<HexPos, EssenceType> shiftPattern(Map<HexPos, EssenceType> pattern, int offQ, int offR) {
        Map<HexPos, EssenceType> shifted = new HashMap<>();
        for (Map.Entry<HexPos, EssenceType> entry : pattern.entrySet()) {
            shifted.put(new HexPos(entry.getKey().q() - offQ, entry.getKey().r() - offR), entry.getValue());
        }
        return shifted;
    }

    private double hexScreenX(int q, int r) {
        return leftPos + GRID_CX + HEX_SIZE * (Math.sqrt(3) * q + Math.sqrt(3) / 2.0 * r);
    }

    private double hexScreenY(int q, int r) {
        return topPos + GRID_CY + HEX_SIZE * (1.5 * r);
    }

    private static boolean inHexBounds(int q, int r) {
        return Math.max(Math.abs(q), Math.max(Math.abs(r), Math.abs(q + r))) <= GRID_RADIUS;
    }

    @Nullable
    private HexPos hexAtMouse(double mx, double my) {
        HexPos best = null;
        double bestDist = HEX_SIZE * 0.95;
        for (int q = -GRID_RADIUS; q <= GRID_RADIUS; q++) {
            for (int r = -GRID_RADIUS; r <= GRID_RADIUS; r++) {
                if (!inHexBounds(q, r)) {
                    continue;
                }
                double dist = Math.hypot(mx - hexScreenX(q, r), my - hexScreenY(q, r));
                if (dist < bestDist) {
                    bestDist = dist;
                    best = new HexPos(q, r);
                }
            }
        }
        return best;
    }

    private int sidebarEssenceX(int idx) {
        return leftPos + SIDEBAR_X + (idx % ESSENCE_COLS) * (ESSENCE_ICON_SIZE + 1);
    }

    private int sidebarEssenceY(int idx) {
        return topPos + SIDEBAR_Y + (idx / ESSENCE_COLS) * (ESSENCE_ICON_SIZE + 1);
    }

    @Nullable
    private EssenceType essenceAtMouse(double mx, double my) {
        EssenceType[] types = EssenceType.values();
        for (int i = 0; i < types.length; i++) {
            int ex = sidebarEssenceX(i);
            int ey = sidebarEssenceY(i);
            if (mx >= ex && mx < ex + ESSENCE_ICON_SIZE && my >= ey && my < ey + ESSENCE_ICON_SIZE) {
                return types[i];
            }
        }
        return null;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Screen.render() draws the background (and renderBg) itself on 1.21.
        boolean editable = menu.hasScroll() && !isViewOnly();
        hoveredHex = editable ? hexAtMouse(mouseX, mouseY) : null;
        if (craftButton != null) {
            craftButton.active = editable;
        }
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);

        if (heldEssence != null) {
            drawEssenceIcon(g, heldEssence, mouseX - ESSENCE_ICON_SIZE / 2, mouseY - ESSENCE_ICON_SIZE / 2,
                    ESSENCE_ICON_SIZE);
        } else {
            EssenceType hover = essenceAtMouse(mouseX, mouseY);
            if (hover != null) {
                g.renderTooltip(this.font,
                        Component.translatable("essence.creraces." + hover.getSerializedName()),
                        mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableBlend();
        g.blit(BG, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        if (menu.hasScroll()) {
            g.blit(SCROLL_OVERLAY, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        }
        RenderSystem.disableBlend();

        renderEssenceSidebar(g);
        // The grid only appears once a scroll is in the table.
        if (menu.hasScroll()) {
            renderHexGrid(g);
        }
    }

    private void renderEssenceSidebar(GuiGraphics g) {
        EssenceType[] types = EssenceType.values();
        for (int i = 0; i < types.length; i++) {
            EssenceType essenceType = types[i];
            int ex = sidebarEssenceX(i);
            int ey = sidebarEssenceY(i);
            int totalCount = getEssenceCount(essenceType);
            int effectiveCount = getEffectiveCount(essenceType);
            boolean depleted = effectiveCount == 0;

            RenderSystem.enableBlend();
            float shade = depleted ? 0.35f : 1.0f;
            RenderSystem.setShaderColor(shade, shade, shade, depleted ? 0.6f : 1.0f);
            blitScaled(g, ESSENCE_TEXTURES.get(essenceType), ex, ey, ESSENCE_ICON_SIZE);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.disableBlend();

            // Gold border on the essence selected for draw mode
            if (essenceType == selectedEssence) {
                RenderSystem.enableBlend();
                RenderSystem.setShaderColor(1.0f, 0.82f, 0.15f, 1f);
                blitScaled(g, SLOT_BORDER_TEX, ex, ey, ESSENCE_ICON_SIZE);
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                RenderSystem.disableBlend();
            }

            drawCountBadge(g, ex, ey, effectiveCount, totalCount);
        }
    }

    /**
     * What is left after the cells already drawn (∞ in creative), in the icon's corner: white while
     * nothing is placed, bright red once the count is going down, dark red when it runs out.
     */
    private void drawCountBadge(GuiGraphics g, int x, int y, int effectiveCount, int totalCount) {
        String countStr = effectiveCount < 0 ? "∞" : String.valueOf(effectiveCount);
        int textColor;
        if (effectiveCount == 0) {
            textColor = 0xAA4444;
        } else if (totalCount >= 0 && effectiveCount < totalCount) {
            textColor = 0xFF6060;
        } else {
            textColor = 0xFFFFFF;
        }
        g.pose().pushPose();
        g.pose().translate(x + ESSENCE_ICON_SIZE - 1, y + ESSENCE_ICON_SIZE - 5, 0);
        g.pose().scale(0.6f, 0.6f, 1f);
        g.drawString(this.font, countStr, -this.font.width(countStr), 0, textColor, true);
        g.pose().popPose();
    }

    private void renderHexGrid(GuiGraphics g) {
        boolean viewOnly = isViewOnly();
        // Holding Ctrl while editing labels every cell with its axial coordinates.
        boolean debug = !viewOnly && Screen.hasControlDown();
        Map<HexPos, EssenceType> displayGrid = viewOnly ? getScrollPattern() : localGrid;
        for (int q = -GRID_RADIUS; q <= GRID_RADIUS; q++) {
            for (int r = -GRID_RADIUS; r <= GRID_RADIUS; r++) {
                if (!inHexBounds(q, r)) {
                    continue;
                }
                HexPos pos = new HexPos(q, r);
                boolean isHovered = !viewOnly && pos.equals(hoveredHex);
                drawHex(g, q, r, (int) hexScreenX(q, r), (int) hexScreenY(q, r), displayGrid.get(pos), isHovered,
                        debug, viewOnly);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // The title and inventory labels are not drawn on this screen.
    }

    private void drawHex(GuiGraphics g, int q, int r, int cx, int cy, @Nullable EssenceType placed, boolean hovered,
            boolean debug, boolean viewOnly) {
        int cellSize = HEX_SIZE * 2;
        int cellX = cx - HEX_SIZE;
        int cellY = cy - HEX_SIZE;

        RenderSystem.enableBlend();

        if (placed != null) {
            float dim = viewOnly ? 0.55f : 1f;
            RenderSystem.setShaderColor(dim, dim, dim, 1f);
            blitScaled(g, ESSENCE_TEXTURES.get(placed), cellX, cellY, cellSize);
        }

        // Slot border: full brightness on hover, dimmed in view-only or idle.
        float t = viewOnly ? 0.45f : (hovered ? 1f : 0.70f);
        RenderSystem.setShaderColor(t, t, t, 1f);
        blitScaled(g, SLOT_BORDER_TEX, cellX, cellY, cellSize);

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();

        if (debug) {
            String label = q + ":" + r;
            g.pose().pushPose();
            g.pose().translate(cx, cy - 3, 0f);
            g.pose().scale(0.55f, 0.55f, 1f);
            g.drawString(this.font, label, -this.font.width(label) / 2, 0, 0xFFFFFFFF, false);
            g.pose().popPose();
        }
    }

    private void drawEssenceIcon(GuiGraphics g, EssenceType essence, int x, int y, int size) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        blitScaled(g, ESSENCE_TEXTURES.get(essence), x, y, size);
        RenderSystem.disableBlend();
    }

    /** Returns combined essence count from adjacent storage (snapshotted at open) + belt, or -1 for creative/infinite. */
    private int getEssenceCount(EssenceType type) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return 0;
        }
        if (player.isCreative()) {
            return -1;
        }
        return menu.getStorageCount(type) + EssenceBeltHelper.getEssenceCount(player, type);
    }

    /** Returns how many cells of this type are currently placed on the grid. */
    private int getGridUsed(EssenceType type) {
        int count = 0;
        for (EssenceType t : localGrid.values()) {
            if (t == type) {
                count++;
            }
        }
        return count;
    }

    /** The count left after the cells already drawn, never below 0; -1 in creative. */
    private int getEffectiveCount(EssenceType type) {
        int total = getEssenceCount(type);
        if (total < 0) {
            return -1;
        }
        return Math.max(0, total - getGridUsed(type));
    }

    /** Creative counts as unlimited (-1), hence != 0 rather than > 0. */
    private boolean hasEssenceLeft(EssenceType type) {
        return getEffectiveCount(type) != 0;
    }

    private static void blitScaled(GuiGraphics g, ResourceLocation loc, int x, int y, int size) {
        float s = size / 256f;
        g.pose().pushPose();
        g.pose().translate(x, y, 0f);
        g.pose().scale(s, s, 1f);
        g.blit(loc, 0, 0, 0f, 0f, 256, 256, 256, 256);
        g.pose().popPose();
    }

    private void placeEssence(HexPos pos, EssenceType essence) {
        localGrid.put(pos, essence);
        BoundaryHandler.sendPlaceEssence(new PlaceEssencePacket(tablePos, pos, essence));
    }

    private void removeEssence(HexPos pos) {
        localGrid.remove(pos);
        BoundaryHandler.sendRemoveEssence(new RemoveEssencePacket(tablePos, pos));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        boolean viewOnly = isViewOnly();

        if (button == 0) {
            // Picking an essence from the sidebar: whether it becomes a drag or draw mode is decided on release.
            EssenceType picked = essenceAtMouse(mx, my);
            if (picked != null) {
                if (!viewOnly && hasEssenceLeft(picked)) {
                    heldEssence = picked;
                }
                return true;
            }

            if (!viewOnly) {
                HexPos hexUnder = hexAtMouse(mx, my);
                if (hexUnder != null) {
                    if (selectedEssence != null) {
                        // Draw mode: repainting the same essence is free, anything else needs some left.
                        if (localGrid.get(hexUnder) == selectedEssence || hasEssenceLeft(selectedEssence)) {
                            placeEssence(hexUnder, selectedEssence);
                        }
                        return true;
                    }
                    if (heldEssence == null && localGrid.containsKey(hexUnder)) {
                        // Picking a placed essence back up also leaves draw mode.
                        selectedEssence = null;
                        heldEssence = localGrid.get(hexUnder);
                        removeEssence(hexUnder);
                        return true;
                    }
                }
            }
        }

        if (button == 1 && !viewOnly) {
            HexPos hex = hexAtMouse(mx, my);
            if (hex != null && localGrid.containsKey(hex)) {
                removeEssence(hex);
                return true;
            }
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (!isViewOnly()) {
            if (button == 0 && selectedEssence != null && heldEssence == null) {
                HexPos hex = hexAtMouse(mx, my);
                if (hex != null && localGrid.get(hex) != selectedEssence && hasEssenceLeft(selectedEssence)) {
                    placeEssence(hex, selectedEssence);
                }
                return true;
            }
            if (button == 1) {
                HexPos hex = hexAtMouse(mx, my);
                if (hex != null && localGrid.containsKey(hex)) {
                    removeEssence(hex);
                }
                return true;
            }
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0 && heldEssence != null) {
            HexPos hex = hexAtMouse(mx, my);
            if (hex != null) {
                // Dropped on a cell: place it and leave draw mode.
                selectedEssence = null;
                placeEssence(hex, heldEssence);
            } else {
                // Released off the grid: back on its sidebar icon it toggles draw mode, anywhere else it cancels.
                EssenceType released = essenceAtMouse(mx, my);
                if (released != null) {
                    selectedEssence = (released == selectedEssence) ? null : released;
                }
            }
            heldEssence = null;
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }
}
