package mc.sayda.creraces.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import mc.sayda.creraces.ability.EssenceType;
import mc.sayda.creraces.ability.HexPos;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.network.CraftScrollPacket;
import mc.sayda.creraces.network.PlaceEssencePacket;
import mc.sayda.creraces.network.RemoveEssencePacket;
import mc.sayda.creraces.world.inventory.ResearchTableMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class ResearchTableScreen extends AbstractContainerScreen<ResearchTableMenu> {

    private static final ResourceLocation BG =
            ResourceLocation.fromNamespaceAndPath("creraces", "textures/screens/gui_research.png");
    private static final ResourceLocation SCROLL_OVERLAY =
            ResourceLocation.fromNamespaceAndPath("creraces", "textures/screens/gui_research_scroll.png");
    private static final ResourceLocation SLOT_BORDER_TEX =
            ResourceLocation.fromNamespaceAndPath("creraces", "textures/essence/slot_border.png");
    private static final Map<EssenceType, ResourceLocation> ESSENCE_TEXTURES;
    static {
        ESSENCE_TEXTURES = new EnumMap<>(EssenceType.class);
        for (EssenceType e : EssenceType.values()) {
            ESSENCE_TEXTURES.put(e, ResourceLocation.fromNamespaceAndPath("creraces",
                    "textures/essence/" + e.getSerializedName() + ".png"));
        }
    }

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

    // State
    private final Map<HexPos, EssenceType> localGrid = new HashMap<>();
    @Nullable private EssenceType heldEssence = null;
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
        this.titleLabelX = Integer.MIN_VALUE;
        this.inventoryLabelX = Integer.MIN_VALUE;
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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isViewOnly() {
        ItemStack scroll = menu.getSlot(1).getItem();
        CompoundTag tag = mc.sayda.creraces.util.ItemNbt.get(scroll);
        if (tag != null && tag.contains("Ability")) return true;
        return menu.getSlot(0).getItem().isEmpty();
    }

    private Map<HexPos, EssenceType> getScrollPattern() {
        ItemStack scroll = menu.getSlot(1).getItem();
        CompoundTag tag = mc.sayda.creraces.util.ItemNbt.get(scroll);
        if (tag == null || !tag.contains("Ability")) return Map.of();
        net.minecraft.resources.ResourceLocation abilityId =
                net.minecraft.resources.ResourceLocation.tryParse(tag.getString("Ability"));
        return mc.sayda.creraces.ability.HexRecipeManager.findByAbility(abilityId)
                .map(mc.sayda.creraces.ability.HexRecipe::pattern)
                .map(this::centerPattern)
                .orElse(Map.of());
    }

    private Map<HexPos, EssenceType> centerPattern(Map<HexPos, EssenceType> pattern) {
        if (pattern.isEmpty()) return pattern;
        int minQ = Integer.MAX_VALUE, maxQ = Integer.MIN_VALUE;
        int minR = Integer.MAX_VALUE, maxR = Integer.MIN_VALUE;
        for (HexPos pos : pattern.keySet()) {
            minQ = Math.min(minQ, pos.q()); maxQ = Math.max(maxQ, pos.q());
            minR = Math.min(minR, pos.r()); maxR = Math.max(maxR, pos.r());
        }
        int baseOffQ = Math.round((minQ + maxQ) / 2.0f);
        int baseOffR = Math.round((minR + maxR) / 2.0f);

        // Try the computed center, then nearby offsets, until all cells fit inHexBounds
        int[] deltas = {0, -1, 1, -2, 2};
        for (int dq : deltas) {
            for (int dr : deltas) {
                int offQ = baseOffQ + dq;
                int offR = baseOffR + dr;
                Map<HexPos, EssenceType> candidate = new HashMap<>();
                boolean allInBounds = true;
                for (Map.Entry<HexPos, EssenceType> entry : pattern.entrySet()) {
                    HexPos shifted = new HexPos(entry.getKey().q() - offQ, entry.getKey().r() - offR);
                    if (!inHexBounds(shifted.q(), shifted.r())) { allInBounds = false; break; }
                    candidate.put(shifted, entry.getValue());
                }
                if (allInBounds) return candidate;
            }
        }
        // Pattern is larger than the grid; center as best we can
        Map<HexPos, EssenceType> best = new HashMap<>();
        for (Map.Entry<HexPos, EssenceType> entry : pattern.entrySet()) {
            best.put(new HexPos(entry.getKey().q() - baseOffQ, entry.getKey().r() - baseOffR), entry.getValue());
        }
        return best;
    }

    // ── Coordinate helpers ────────────────────────────────────────────────────

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
                if (!inHexBounds(q, r)) continue;
                double cx = hexScreenX(q, r);
                double cy = hexScreenY(q, r);
                double dist = Math.hypot(mx - cx, my - cy);
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

    // ── Rendering ─────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean hasScroll = menu.hasScroll();
        boolean viewOnly = isViewOnly();
        hoveredHex = (hasScroll && !viewOnly) ? hexAtMouse(mouseX, mouseY) : null;
        if (craftButton != null) {
            craftButton.visible = true;
            craftButton.active = hasScroll && !viewOnly;
        }
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);

        // Draw held essence following mouse
        if (heldEssence != null) {
            drawEssenceIcon(g, heldEssence, mouseX - ESSENCE_ICON_SIZE / 2, mouseY - ESSENCE_ICON_SIZE / 2, ESSENCE_ICON_SIZE);
        }

        // Essence sidebar tooltips
        if (heldEssence == null) {
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

        // Draw essence sidebar
        EssenceType[] types = EssenceType.values();
        for (int i = 0; i < types.length; i++) {
            EssenceType essenceType = types[i];
            int ex = sidebarEssenceX(i);
            int ey = sidebarEssenceY(i);
            int totalCount = getEssenceCount(essenceType);
            int effectiveCount = getEffectiveCount(essenceType);
            boolean depleted = effectiveCount == 0 && totalCount >= 0;

            // Draw icon, dimmed when depleted
            RenderSystem.enableBlend();
            float dim = depleted ? 0.35f : 1.0f;
            RenderSystem.setShaderColor(dim, dim, dim, depleted ? 0.6f : 1.0f);
            blitScaled(g, ESSENCE_TEXTURES.get(essenceType), ex, ey, ESSENCE_ICON_SIZE);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.disableBlend();

            // Gold border on selected essence in draw mode
            if (essenceType == selectedEssence) {
                RenderSystem.enableBlend();
                RenderSystem.setShaderColor(1.0f, 0.82f, 0.15f, 1f);
                blitScaled(g, SLOT_BORDER_TEX, ex, ey, ESSENCE_ICON_SIZE);
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                RenderSystem.disableBlend();
            }

            // Count badge: shows remaining after grid usage (∞ for creative)
            // White when nothing placed, bright red while decreasing, dark red when empty
            String countStr = (effectiveCount < 0) ? "∞" : String.valueOf(effectiveCount);
            int textColor;
            if (depleted) {
                textColor = 0xAA4444; // dark red: fully used up
            } else if (totalCount >= 0 && effectiveCount < totalCount) {
                textColor = 0xFF6060; // bright red: count going down due to grid placements
            } else {
                textColor = 0xFFFFFF; // white: nothing spent yet
            }
            g.pose().pushPose();
            g.pose().translate(ex + ESSENCE_ICON_SIZE - 1, ey + ESSENCE_ICON_SIZE - 5, 0);
            g.pose().scale(0.6f, 0.6f, 1f);
            int tw = this.font.width(countStr);
            g.drawString(this.font, countStr, -tw, 0, textColor, true);
            g.pose().popPose();
        }

        // Hex grid only visible with a scroll inserted
        if (menu.hasScroll()) {
            boolean viewOnly = isViewOnly();
            boolean debug = !viewOnly && Screen.hasControlDown();
            Map<HexPos, EssenceType> displayGrid = viewOnly ? getScrollPattern() : localGrid;
            for (int q = -GRID_RADIUS; q <= GRID_RADIUS; q++) {
                for (int r = -GRID_RADIUS; r <= GRID_RADIUS; r++) {
                    if (!inHexBounds(q, r)) continue;
                    HexPos pos = new HexPos(q, r);
                    int cx = (int) hexScreenX(q, r);
                    int cy = (int) hexScreenY(q, r);
                    boolean isHovered = !viewOnly && pos.equals(hoveredHex);
                    EssenceType placed = displayGrid.get(pos);
                    drawHex(g, q, r, cx, cy, placed, isHovered, debug, viewOnly);
                }
            }
        }

    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // no labels
    }

    private void drawHex(GuiGraphics g, int q, int r, int cx, int cy, @Nullable EssenceType placed, boolean hovered, boolean debug, boolean viewOnly) {
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
            int tw = this.font.width(label);
            g.drawString(this.font, label, -tw / 2, 0, 0xFFFFFFFF, false);
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
        net.minecraft.client.player.LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) return 0;
        if (player.isCreative()) return -1;
        return menu.getStorageCount(type) + mc.sayda.creraces.util.EssenceBeltHelper.getEssenceCount(player, type);
    }

    /** Returns how many cells of this type are currently placed on the grid. */
    private int getGridUsed(EssenceType type) {
        int count = 0;
        for (EssenceType t : localGrid.values()) {
            if (t == type) count++;
        }
        return count;
    }

    /**
     * Returns the essence count adjusted for grid usage (belt total minus cells already drawn).
     * Creative returns -1. Result is floor-clamped to 0.
     */
    private int getEffectiveCount(EssenceType type) {
        int total = getEssenceCount(type);
        if (total < 0) return -1;
        return Math.max(0, total - getGridUsed(type));
    }

    private static void blitScaled(GuiGraphics g, ResourceLocation loc, int x, int y, int size) {
        float s = size / 256f;
        g.pose().pushPose();
        g.pose().translate(x, y, 0f);
        g.pose().scale(s, s, 1f);
        g.blit(loc, 0, 0, 0f, 0f, 256, 256, 256, 256);
        g.pose().popPose();
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        boolean viewOnly = isViewOnly();

        if (button == 0) {
            // Sidebar click: pick up essence (drag or draw decided on release)
            EssenceType picked = essenceAtMouse(mx, my);
            if (picked != null) {
                if (!viewOnly && getEffectiveCount(picked) != 0) {
                    heldEssence = picked;
                }
                return true;
            }

            if (!viewOnly) {
                HexPos hexUnder = hexAtMouse(mx, my);
                if (hexUnder != null) {
                    if (selectedEssence != null) {
                        // Draw mode: stamp selected essence only if the cell differs and we have remaining.
                        // getEffectiveCount returns -1 for creative (unlimited), so compare with != 0, not > 0.
                        boolean cellAlreadyThis = localGrid.get(hexUnder) == selectedEssence;
                        if (cellAlreadyThis || getEffectiveCount(selectedEssence) != 0) {
                            localGrid.put(hexUnder, selectedEssence);
                            BoundaryHandler.sendPlaceEssence(new PlaceEssencePacket(tablePos, hexUnder, selectedEssence));
                        }
                        return true;
                    }
                    if (heldEssence == null && localGrid.containsKey(hexUnder)) {
                        // Drag from hex: pick up, exit draw mode
                        selectedEssence = null;
                        heldEssence = localGrid.remove(hexUnder);
                        BoundaryHandler.sendRemoveEssence(new RemoveEssencePacket(tablePos, hexUnder));
                        return true;
                    }
                }
            }
        }

        if (button == 1 && !viewOnly) {
            HexPos hex = hexAtMouse(mx, my);
            if (hex != null && localGrid.containsKey(hex)) {
                localGrid.remove(hex);
                BoundaryHandler.sendRemoveEssence(new RemoveEssencePacket(tablePos, hex));
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
                if (hex != null && localGrid.get(hex) != selectedEssence) {
                    // getEffectiveCount returns -1 for creative (unlimited), so compare with != 0, not > 0.
                    if (getEffectiveCount(selectedEssence) != 0) {
                        localGrid.put(hex, selectedEssence);
                        BoundaryHandler.sendPlaceEssence(new PlaceEssencePacket(tablePos, hex, selectedEssence));
                    }
                }
                return true;
            }
            if (button == 1) {
                HexPos hex = hexAtMouse(mx, my);
                if (hex != null && localGrid.containsKey(hex)) {
                    localGrid.remove(hex);
                    BoundaryHandler.sendRemoveEssence(new RemoveEssencePacket(tablePos, hex));
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
                // Dragged to a hex: place it, exit draw mode
                selectedEssence = null;
                localGrid.put(hex, heldEssence);
                BoundaryHandler.sendPlaceEssence(new PlaceEssencePacket(tablePos, hex, heldEssence));
            } else {
                // Released without reaching a hex: treat as a sidebar click for draw mode
                EssenceType released = essenceAtMouse(mx, my);
                if (released != null) {
                    selectedEssence = (released == selectedEssence) ? null : released;
                }
                // else: cancelled drag (dropped on nothing)
            }
            heldEssence = null;
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }
}
