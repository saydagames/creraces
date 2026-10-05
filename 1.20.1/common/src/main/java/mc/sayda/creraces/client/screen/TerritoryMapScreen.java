package mc.sayda.creraces.client.screen;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.network.ClaimChunkPacket;
import mc.sayda.creraces.network.TerrainSamplePacket;
import mc.sayda.creraces.network.TerritoryDataPacket;
import mc.sayda.creraces.territory.TerritoryManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Chunk map of territory claims around the player, drawn over a terrain texture baked from the
 * server's colour samples. Left click claims, right click unclaims, middle drag pans.
 */
@SuppressWarnings("null")
public class TerritoryMapScreen extends Screen {
    private static final ResourceLocation TERRAIN_TEX_ID =
            new ResourceLocation("creraces", "dynamic/territory_terrain");

    /** Pixels per chunk. */
    private static final int CELL = 10;
    /** Client ticks a claim result stays on screen (4 seconds). */
    private static final int RESULT_DISPLAY_TICKS = 80;
    private static final int[][] NEIGHBOUR_OFFSETS = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };

    // Parchment brown behind anything the terrain sample does not cover.
    private static final int COLOR_MAP_BG = 0xFF8B7355;

    // Claim fills are translucent so the terrain shows through.
    private static final int COLOR_OWN = 0xAA4CAF50;
    private static final int COLOR_ALLIED = 0xAA2196F3;
    private static final int COLOR_ENEMY = 0xAAF44336;
    private static final int COLOR_DORMANT = 0x88000000;
    private static final int COLOR_PLAYER = 0xFFFFFFFF;
    private static final int COLOR_CLAIMABLE = 0x55FFFFFF;

    // Borders are darker and opaque to mark where a territory ends.
    private static final int BORDER_OWN = 0xFF2E7D32;
    private static final int BORDER_ALLIED = 0xFF0D47A1;
    private static final int BORDER_ENEMY = 0xFF7F0000;

    // Kept across screen instances, so reopening the map shows the last data straight away.
    private static List<TerritoryDataPacket.ChunkInfo> cachedChunks = new ArrayList<>();
    private static Set<Long> cachedBiomeClaimable = new HashSet<>();
    private static TerrainSamplePacket lastTerrain = null;
    private static DynamicTexture terrainTexture = null;
    private static boolean terrainDirty = true;

    private Map<Long, TerritoryDataPacket.ChunkInfo> chunkLookup = new HashMap<>();
    private Set<Long> claimableChunks = new HashSet<>();

    /** The player's chunk when the map was opened; the view is centred on it plus the pan offset. */
    private int playerCX;
    private int playerCZ;

    /** Pan offset in chunks. */
    private int offsetX = 0;
    private int offsetZ = 0;

    // Middle-drag panning. The base offset is saved at the start so separate drags add up.
    private boolean dragging = false;
    private double dragStartX;
    private double dragStartZ;
    private int dragBaseX = 0;
    private int dragBaseZ = 0;

    private TerritoryManager.ClaimResultType lastResult = null;
    private int resultTimer = 0;

    // Map panel, laid out by init() to a whole number of cells.
    private int mapLeft;
    private int mapTop;
    private int mapW;
    private int mapH;
    private int cellsW;
    private int cellsH;

    public TerritoryMapScreen() {
        super(Component.translatable("screen.creraces.territory_map"));
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            this.playerCX = mc.player.chunkPosition().x;
            this.playerCZ = mc.player.chunkPosition().z;
        }
        rebuildLookup();
    }

    public static void open() {
        BoundaryHandler.sendRequestTerritoryData();
        Minecraft.getInstance().setScreen(new TerritoryMapScreen());
    }

    public static void updateChunks(TerritoryDataPacket pkt) {
        cachedChunks = new ArrayList<>(pkt.chunks);
        cachedBiomeClaimable = new HashSet<>(pkt.biomeClaimableChunks);
        if (Minecraft.getInstance().screen instanceof TerritoryMapScreen screen) {
            screen.rebuildLookup();
        }
    }

    public static void updateTerrain(TerrainSamplePacket pkt) {
        lastTerrain = pkt;
        terrainDirty = true;
    }

    public static void clearCache() {
        cachedChunks = new ArrayList<>();
        cachedBiomeClaimable = new HashSet<>();
        lastTerrain = null;
        terrainDirty = true;
        if (terrainTexture != null) {
            Minecraft.getInstance().getTextureManager().release(TERRAIN_TEX_ID);
            terrainTexture = null;
        }
    }

    public static void onClaimResponse(TerritoryManager.ClaimResultType result) {
        if (Minecraft.getInstance().screen instanceof TerritoryMapScreen screen) {
            screen.showResult(result);
            if (result == TerritoryManager.ClaimResultType.SUCCESS
                    || result == TerritoryManager.ClaimResultType.UNCLAIM_SUCCESS) {
                BoundaryHandler.sendRequestTerritoryData();
            }
        }
    }

    @Override
    protected void init() {
        // An eighth of the width as margin on each side keeps the map from swamping the screen.
        int hMargin = Math.max(20, width / 8);
        mapLeft = hMargin;
        mapTop = 24;
        mapW = ((width - hMargin * 2) / CELL) * CELL;
        mapH = ((height - mapTop - 40) / CELL) * CELL;
        cellsW = mapW / CELL;
        cellsH = mapH / CELL;

        addRenderableWidget(Button.builder(Component.translatable("screen.creraces.refresh"),
                b -> BoundaryHandler.sendRequestTerritoryData()).bounds(width - 60, 2, 55, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(width / 2 - 50, height - 20, 100, 16).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float dt) {
        if (terrainDirty) {
            rebuildTerrainTexture();
        }

        g.fill(0, 0, width, height, 0xAA000000);
        g.drawCenteredString(font, Component.translatable("screen.creraces.territory_map"), width / 2, 6, 0xFFFFFF);
        drawWoodFrame(g, mapLeft, mapTop, mapW, mapH);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.enableScissor(mapLeft, mapTop, mapLeft + mapW, mapTop + mapH);
        g.fill(mapLeft, mapTop, mapLeft + mapW, mapTop + mapH, COLOR_MAP_BG);
        renderTerrain(g);
        renderChunkOverlays(g);
        g.disableScissor();
        RenderSystem.disableBlend();

        if (isOverMap(mx, my)) {
            renderChunkTooltip(g, mx, my);
        }

        int legendY = mapTop + mapH + 8;
        renderLegend(g, legendY);
        renderCoins(g, legendY);
        renderClaimResult(g);

        super.render(g, mx, my, dt);
    }

    /** Draws the visible part of the baked terrain in one blit, if the sample covers the whole view. */
    private void renderTerrain(GuiGraphics g) {
        if (terrainTexture == null || lastTerrain == null) {
            return;
        }
        int texW = lastTerrain.width * CELL;
        int texH = lastTerrain.height * CELL;
        int uOff = (playerCX + offsetX - cellsW / 2 - lastTerrain.originCX) * CELL;
        int vOff = (playerCZ + offsetZ - cellsH / 2 - lastTerrain.originCZ) * CELL;
        if (uOff >= 0 && vOff >= 0 && uOff + mapW <= texW && vOff + mapH <= texH) {
            g.blit(TERRAIN_TEX_ID, mapLeft, mapTop, uOff, vOff, mapW, mapH, texW, texH);
        }
    }

    /** Claim fills, faction borders, claimable hints and the player marker for every visible chunk. */
    private void renderChunkOverlays(GuiGraphics g) {
        int halfW = cellsW / 2;
        int halfH = cellsH / 2;
        for (int rz = -halfH; rz <= halfH; rz++) {
            for (int rx = -halfW; rx <= halfW; rx++) {
                int cx = playerCX + offsetX + rx;
                int cz = playerCZ + offsetZ + rz;
                int px = mapLeft + (rx + halfW) * CELL;
                int pz = mapTop + (rz + halfH) * CELL;
                if (px < mapLeft || pz < mapTop || px + CELL > mapLeft + mapW || pz + CELL > mapTop + mapH) {
                    continue;
                }

                long key = ChunkPos.asLong(cx, cz);
                TerritoryDataPacket.ChunkInfo info = chunkLookup.get(key);
                if (info != null) {
                    g.fill(px, pz, px + CELL, pz + CELL, colorFor(info.relation));
                    if (info.dormant) {
                        drawDormantStripes(g, px, pz, CELL);
                    }
                    int bc = borderColorFor(info.relation);
                    if (isDifferentOwner(ChunkPos.asLong(cx, cz - 1), info)) g.fill(px, pz, px + CELL, pz + 1, bc);
                    if (isDifferentOwner(ChunkPos.asLong(cx, cz + 1), info)) g.fill(px, pz + CELL - 1, px + CELL, pz + CELL, bc);
                    if (isDifferentOwner(ChunkPos.asLong(cx - 1, cz), info)) g.fill(px, pz, px + 1, pz + CELL, bc);
                    if (isDifferentOwner(ChunkPos.asLong(cx + 1, cz), info)) g.fill(px + CELL - 1, pz, px + CELL, pz + CELL, bc);
                } else if (claimableChunks.contains(key)) {
                    g.fill(px, pz, px + CELL, pz + CELL, COLOR_CLAIMABLE);
                }

                if (cx == playerCX && cz == playerCZ) {
                    g.renderOutline(px, pz, CELL, CELL, COLOR_PLAYER);
                }
            }
        }
    }

    private void renderChunkTooltip(GuiGraphics g, int mx, int my) {
        int chunkX = chunkXAt(mx);
        int chunkZ = chunkZAt(my);
        TerritoryDataPacket.ChunkInfo info = chunkLookup.get(ChunkPos.asLong(chunkX, chunkZ));

        MutableComponent tip = Component.literal("[" + chunkX + ", " + chunkZ + "]");
        if (info != null && !info.factionName.isEmpty()) {
            tip.append(" " + info.factionName);
            if (info.dormant) {
                tip.append(" ").append(Component.translatable("screen.creraces.territory_map.anchor_tag"));
            }
        }
        if (hasShiftDown() && info != null && !info.ownerName.isEmpty()) {
            tip.append(" | " + info.ownerName);
        }
        g.renderTooltip(font, tip, mx, my);
    }

    private void renderLegend(GuiGraphics g, int y) {
        int x = mapLeft;
        x = drawLegendEntry(g, x, y, COLOR_OWN, "own", 35);
        x = drawLegendEntry(g, x, y, COLOR_ALLIED, "allied", 50);
        x = drawLegendEntry(g, x, y, COLOR_MAP_BG, "neutral", 55);
        x = drawLegendEntry(g, x, y, COLOR_ENEMY, "enemy", 48);

        // Anchor chunks: the claimable swatch with the dormant stripes over it.
        g.fill(x, y, x + 8, y + 8, COLOR_CLAIMABLE);
        drawDormantStripes(g, x, y, 8);
        g.drawString(font, legendLabel("anchor"), x + 10, y, 0xCCCCCC, false);
        x += 50;

        drawLegendEntry(g, x, y, COLOR_CLAIMABLE, "claimable", 0);
    }

    /** Draws one swatch and label, returning where the next entry starts. */
    private int drawLegendEntry(GuiGraphics g, int x, int y, int color, String labelKey, int width) {
        g.fill(x, y, x + 8, y + 8, color);
        g.drawString(font, legendLabel(labelKey), x + 10, y, 0xCCCCCC, false);
        return x + width;
    }

    private static Component legendLabel(String key) {
        return Component.translatable("screen.creraces.territory_map.legend." + key);
    }

    private void renderCoins(GuiGraphics g, int y) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        double coins = DataUtils.getVariables(minecraft.player).map(IPlayerVariables::getCoins).orElse(0.0);
        Component coinText = Component.translatable("screen.creraces.territory_map.coins", (int) coins);
        g.drawString(font, coinText, mapLeft + mapW - font.width(coinText), y, 0xFFD700, false);
    }

    private void renderClaimResult(GuiGraphics g) {
        if (resultTimer <= 0 || lastResult == null) {
            return;
        }
        Component msg = switch (lastResult) {
            case SUCCESS -> Component.translatable("msg.creraces.territory.claimed");
            case UNCLAIM_SUCCESS -> Component.translatable("msg.creraces.territory.unclaimed");
            case INVALID_BIOME -> Component.translatable("msg.creraces.territory.wrong_biome");
            case ENEMY_TERRITORY -> Component.translatable("msg.creraces.territory.enemy");
            case INSIDE_OWN_TERRITORY -> Component.translatable("msg.creraces.territory.own");
            case ANCHOR_CHUNK -> Component.translatable("msg.creraces.territory.anchor");
            case INSUFFICIENT_COINS -> Component.translatable("msg.creraces.territory.insufficient_coins",
                    CreRacesConfig.TERRITORY_CLAIM_COST_PER_CHUNK.get());
            case OUT_OF_RANGE -> Component.translatable("msg.creraces.territory.out_of_range");
            case NOT_LEADER -> Component.translatable("msg.creraces.faction.not_leader");
            default -> null;
        };
        if (msg != null) {
            g.drawCenteredString(font, msg, width / 2, height - 38, 0xFFFFFF);
        }
    }

    private void showResult(TerritoryManager.ClaimResultType result) {
        lastResult = result;
        resultTimer = RESULT_DISPLAY_TICKS;
    }

    @Override
    public void tick() {
        if (resultTimer > 0) {
            resultTimer--;
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (!isOverMap(mx, my)) {
            return super.mouseClicked(mx, my, btn);
        }

        if (btn == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            dragging = true;
            dragStartX = mx;
            dragStartZ = my;
            dragBaseX = offsetX;
            dragBaseZ = offsetZ;
            return true;
        }

        int chunkX = chunkXAt(mx);
        int chunkZ = chunkZAt(my);
        TerritoryDataPacket.ChunkInfo info = chunkLookup.get(ChunkPos.asLong(chunkX, chunkZ));

        if (btn == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (info != null) {
                // Already claimed, so the answer is known without a server round trip.
                showResult(info.relation == TerritoryDataPacket.Relation.OWN
                        ? TerritoryManager.ClaimResultType.INSIDE_OWN_TERRITORY
                        : TerritoryManager.ClaimResultType.ENEMY_TERRITORY);
            } else {
                // The server checks biome, range and coins, then answers through onClaimResponse.
                BoundaryHandler.sendClaimChunk(new ClaimChunkPacket(chunkX, chunkZ, ClaimChunkPacket.ClaimAction.CLAIM));
            }
            return true;
        }

        // Anchor chunks are protected here; they can only be removed in the world.
        if (btn == GLFW.GLFW_MOUSE_BUTTON_RIGHT && info != null && info.relation == TerritoryDataPacket.Relation.OWN) {
            if (info.dormant) {
                showResult(TerritoryManager.ClaimResultType.ANCHOR_CHUNK);
            } else {
                BoundaryHandler.sendClaimChunk(new ClaimChunkPacket(chunkX, chunkZ, ClaimChunkPacket.ClaimAction.UNCLAIM));
            }
            return true;
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (dragging && btn == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            offsetX = dragBaseX - (int) ((mx - dragStartX) / CELL);
            offsetZ = dragBaseZ - (int) ((my - dragStartZ) / CELL);
            return true;
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (btn == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            dragging = false;
        }
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (hasShiftDown()) {
            offsetX -= (int) delta;
        } else {
            offsetZ -= (int) delta;
        }
        return true;
    }

    private boolean isOverMap(double mx, double my) {
        return mx >= mapLeft && mx < mapLeft + mapW && my >= mapTop && my < mapTop + mapH;
    }

    private int chunkXAt(double mx) {
        return playerCX + offsetX + (int) (mx - mapLeft) / CELL - cellsW / 2;
    }

    private int chunkZAt(double my) {
        return playerCZ + offsetZ + (int) (my - mapTop) / CELL - cellsH / 2;
    }

    /** Bakes lastTerrain into a texture, CELL pixels per chunk, so the map is a single blit per frame. */
    private static void rebuildTerrainTexture() {
        terrainDirty = false;
        if (lastTerrain == null) {
            return;
        }

        int subCell = CELL / TerrainSamplePacket.SUB;
        int texW = lastTerrain.width * CELL;
        int texH = lastTerrain.height * CELL;

        NativeImage img = new NativeImage(NativeImage.Format.RGBA, texW, texH, false);
        // Unloaded sub-samples keep this parchment fill.
        img.fillRect(0, 0, texW, texH, argbToAbgr(COLOR_MAP_BG));

        for (int rz = 0; rz < lastTerrain.height; rz++) {
            for (int rx = 0; rx < lastTerrain.width; rx++) {
                int baseIdx = (rz * lastTerrain.width + rx) * TerrainSamplePacket.SUB2;
                for (int sy = 0; sy < TerrainSamplePacket.SUB; sy++) {
                    for (int sx = 0; sx < TerrainSamplePacket.SUB; sx++) {
                        byte packed = lastTerrain.colors[baseIdx + sy * TerrainSamplePacket.SUB + sx];
                        if (packed == 0) {
                            continue;
                        }
                        // MapColor's packed colours come back ABGR, which is what setPixelRGBA expects.
                        int abgr = TerrainSamplePacket.packedToArgb(packed);
                        int ipx = rx * CELL + sx * subCell;
                        int ipy = rz * CELL + sy * subCell;
                        for (int dy = 0; dy < subCell; dy++) {
                            for (int dx = 0; dx < subCell; dx++) {
                                img.setPixelRGBA(ipx + dx, ipy + dy, abgr);
                            }
                        }
                    }
                }
            }
        }

        Minecraft mc = Minecraft.getInstance();
        if (terrainTexture != null) {
            mc.getTextureManager().release(TERRAIN_TEX_ID);
        }
        terrainTexture = new DynamicTexture(img);
        mc.getTextureManager().register(TERRAIN_TEX_ID, terrainTexture);
    }

    /** NativeImage stores pixels as ABGR, so a GUI (ARGB) colour needs red and blue swapped. */
    private static int argbToAbgr(int argb) {
        return (argb & 0xFF00FF00) | ((argb >> 16) & 0xFF) | ((argb & 0xFF) << 16);
    }

    private void rebuildLookup() {
        chunkLookup = new HashMap<>(cachedChunks.size() * 2);
        for (TerritoryDataPacket.ChunkInfo info : cachedChunks) {
            chunkLookup.put(ChunkPos.asLong(info.chunkX, info.chunkZ), info);
        }
        rebuildClaimable();
    }

    private void rebuildClaimable() {
        if (!cachedBiomeClaimable.isEmpty()) {
            claimableChunks = cachedBiomeClaimable;
            return;
        }
        // Without a list from the server, any unclaimed chunk next to own territory counts.
        Set<Long> result = new HashSet<>();
        for (TerritoryDataPacket.ChunkInfo info : chunkLookup.values()) {
            if (info.relation != TerritoryDataPacket.Relation.OWN) {
                continue;
            }
            for (int[] offset : NEIGHBOUR_OFFSETS) {
                long neighbour = ChunkPos.asLong(info.chunkX + offset[0], info.chunkZ + offset[1]);
                if (!chunkLookup.containsKey(neighbour)) {
                    result.add(neighbour);
                }
            }
        }
        claimableChunks = result;
    }

    private static int colorFor(TerritoryDataPacket.Relation rel) {
        return switch (rel) {
            case OWN -> COLOR_OWN;
            case ALLIED -> COLOR_ALLIED;
            case ENEMY -> COLOR_ENEMY;
            default -> 0x00000000;
        };
    }

    private static int borderColorFor(TerritoryDataPacket.Relation rel) {
        return switch (rel) {
            case OWN -> BORDER_OWN;
            case ALLIED -> BORDER_ALLIED;
            case ENEMY -> BORDER_ENEMY;
            default -> 0x00000000;
        };
    }

    private boolean isDifferentOwner(long neighborKey, TerritoryDataPacket.ChunkInfo info) {
        TerritoryDataPacket.ChunkInfo neighbor = chunkLookup.get(neighborKey);
        if (neighbor == null) {
            return true;
        }
        if (neighbor.relation != info.relation) {
            return true;
        }
        return !neighbor.factionName.equals(info.factionName);
    }

    private static void drawDormantStripes(GuiGraphics g, int x, int y, int size) {
        for (int s = 0; s < size; s += 2) {
            g.fill(x + s, y, x + s + 1, y + size, COLOR_DORMANT);
        }
    }

    /** Two-tone wood border drawn just outside the given panel bounds. */
    static void drawWoodFrame(GuiGraphics g, int left, int top, int width, int height) {
        for (int inset = 4; inset >= 1; inset--) {
            int color = inset % 2 == 0 ? 0xFF3D2008 : 0xFF7A4A1E;
            g.fill(left - inset, top - inset, left + width + inset, top + height + inset, color);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
