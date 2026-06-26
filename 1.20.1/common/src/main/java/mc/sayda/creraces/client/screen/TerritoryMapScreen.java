package mc.sayda.creraces.client.screen;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("null")
public class TerritoryMapScreen extends Screen {

    // ── Static caches (shared across screen instances) ─────────────────────────
    private static List<TerritoryDataPacket.ChunkInfo> cachedChunks = new ArrayList<>();
    private static TerrainSamplePacket lastTerrain = null;
    private static DynamicTexture terrainTexture = null;
    private static boolean terrainDirty = true;
    private static final ResourceLocation TERRAIN_TEX_ID =
            new ResourceLocation("creraces", "dynamic/territory_terrain");

    // ── Instance state ─────────────────────────────────────────────────────────
    private Map<Long, TerritoryDataPacket.ChunkInfo> chunkLookup = new HashMap<>();

    /** Center chunk when the map was opened (player's position). */
    private int playerCX;
    private int playerCZ;

    /** Scroll offset in chunk units. */
    private int offsetX = 0;
    private int offsetZ = 0;

    /** Middle-drag tracking. */
    private double dragStartX;
    private double dragStartZ;
    private boolean dragging = false;

    /** Feedback from last claim/unclaim action. */
    private TerritoryManager.ClaimResultType lastResult = null;
    private int resultTimer = 0;

    // Cell size in pixels
    private static final int CELL = 10;

    // Map panel area (set in init)
    private int mapLeft;
    private int mapTop;
    private int mapW;
    private int mapH;
    private int cellsW;
    private int cellsH;

    // Map panel background — solid brown (parchment-like) for unloaded chunks
    private static final int COLOR_MAP_BG    = 0xFF8B7355;

    // Colors — semi-transparent so terrain shows through claimed territory
    private static final int COLOR_OWN     = 0xAA4CAF50;
    private static final int COLOR_ALLIED  = 0xAA2196F3;
    private static final int COLOR_ENEMY   = 0xAAF44336;
    private static final int COLOR_DORMANT    = 0x88000000;
    private static final int COLOR_PLAYER     = 0xFFFFFFFF;
    private static final int COLOR_CLAIMABLE  = 0x55FFFFFF;
    private static final int COLOR_EMPTY_LEGEND = 0xFF8B7355;

    // Border colors — darker, fully opaque to mark territory boundaries
    private static final int BORDER_OWN    = 0xFF2E7D32;
    private static final int BORDER_ALLIED = 0xFF0D47A1;
    private static final int BORDER_ENEMY  = 0xFF7F0000;

    public TerritoryMapScreen() {
        super(Component.translatable("creraces.screen.territory_map"));
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            this.playerCX = mc.player.chunkPosition().x;
            this.playerCZ = mc.player.chunkPosition().z;
        }
        rebuildLookup();
    }

    // ── Static entry points ────────────────────────────────────────────────────

    public static void open() {
        BoundaryHandler.sendRequestTerritoryData();
        Minecraft.getInstance().setScreen(new TerritoryMapScreen());
    }

    private static Set<Long> cachedBiomeClaimable = new HashSet<>();

    public static void updateChunks(TerritoryDataPacket pkt) {
        cachedChunks = new ArrayList<>(pkt.chunks);
        cachedBiomeClaimable = new HashSet<>(pkt.biomeClaimableChunks);
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof TerritoryMapScreen s) {
            s.rebuildLookup();
        } else {
            // Auto-open: setScreen calls init() synchronously, which reads cachedChunks
            mc.setScreen(new TerritoryMapScreen());
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
            Minecraft mc = Minecraft.getInstance();
            mc.getTextureManager().release(TERRAIN_TEX_ID);
            terrainTexture = null;
        }
    }

    public static void onClaimResponse(TerritoryManager.ClaimResultType result) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof TerritoryMapScreen s) {
            s.lastResult = result;
            s.resultTimer = 80;
            if (result == TerritoryManager.ClaimResultType.SUCCESS) {
                // Re-request fresh data
                BoundaryHandler.sendRequestTerritoryData();
            }
        }
    }

    // ── Init ───────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        // Map area: slightly inset (≈12.5% margin each side) for a less overwhelming UI
        int hMargin = Math.max(20, width / 8);
        mapLeft = hMargin;
        mapTop  = 24;
        mapW    = ((width - hMargin * 2) / CELL) * CELL;
        mapH    = (((height - mapTop - 40) / CELL)) * CELL;
        cellsW  = mapW / CELL;
        cellsH  = mapH / CELL;

        // Refresh button
        addRenderableWidget(Button.builder(
                Component.translatable("creraces.screen.refresh"), b -> BoundaryHandler.sendRequestTerritoryData()
        ).bounds(width - 60, 2, 55, 16).build());

        // Close button
        addRenderableWidget(Button.builder(
                Component.translatable("gui.done"), b -> onClose()
        ).bounds(width / 2 - 50, height - 20, 100, 16).build());
    }

    // ── Render ─────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mx, int my, float dt) {
        // Bake terrain into GPU texture if data changed (happens at most once per refresh)
        if (terrainDirty) rebuildTerrainTexture();

        // Dim entire screen behind the map panel
        g.fill(0, 0, width, height, 0xAA000000);

        // Title
        g.drawCenteredString(font, Component.translatable("creraces.screen.territory_map"),
                width / 2, 6, 0xFFFFFF);

        // ── Wood-frame border drawn behind the map area ───────────────────────
        g.fill(mapLeft - 4, mapTop - 4, mapLeft + mapW + 4, mapTop + mapH + 4, 0xFF3D2008);
        g.fill(mapLeft - 3, mapTop - 3, mapLeft + mapW + 3, mapTop + mapH + 3, 0xFF7A4A1E);
        g.fill(mapLeft - 2, mapTop - 2, mapLeft + mapW + 2, mapTop + mapH + 2, 0xFF3D2008);
        g.fill(mapLeft - 1, mapTop - 1, mapLeft + mapW + 1, mapTop + mapH + 1, 0xFF7A4A1E);

        // ── Map content ───────────────────────────────────────────────────────
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        g.enableScissor(mapLeft, mapTop, mapLeft + mapW, mapTop + mapH);

        // Parchment fallback for areas outside terrain sample bounds
        g.fill(mapLeft, mapTop, mapLeft + mapW, mapTop + mapH, COLOR_MAP_BG);

        int halfW = cellsW / 2;
        int halfH = cellsH / 2;

        // ── Claimable zone set: biome-wide (server) or adjacent fallback ──────
        Set<Long> claimableChunks;
        if (!cachedBiomeClaimable.isEmpty()) {
            claimableChunks = cachedBiomeClaimable;
        } else {
            claimableChunks = new HashSet<>();
            int[] DX = {-1, 1, 0, 0};
            int[] DZ = { 0, 0,-1, 1};
            for (Map.Entry<Long, TerritoryDataPacket.ChunkInfo> e : chunkLookup.entrySet()) {
                if (e.getValue().relation != TerritoryDataPacket.Relation.OWN) continue;
                ChunkPos own = new ChunkPos(e.getValue().chunkX, e.getValue().chunkZ);
                for (int i = 0; i < 4; i++) {
                    long nkey = ChunkPos.asLong(own.x + DX[i], own.z + DZ[i]);
                    if (!chunkLookup.containsKey(nkey)) claimableChunks.add(nkey);
                }
            }
        }

        // ── Base layer: single blit from pre-baked terrain texture ────────────
        if (terrainTexture != null && lastTerrain != null) {
            int texW = lastTerrain.width  * CELL;
            int texH = lastTerrain.height * CELL;
            int uOff = (playerCX + offsetX - halfW - lastTerrain.originCX) * CELL;
            int vOff = (playerCZ + offsetZ - halfH - lastTerrain.originCZ) * CELL;
            if (uOff >= 0 && vOff >= 0 && uOff + mapW <= texW && vOff + mapH <= texH) {
                g.blit(TERRAIN_TEX_ID, mapLeft, mapTop, uOff, vOff, mapW, mapH, texW, texH);
            }
        }

        // ── Territory overlays and player marker (only claimed chunks) ─────────
        for (int rz = -halfH; rz <= halfH; rz++) {
            for (int rx = -halfW; rx <= halfW; rx++) {
                int cx = playerCX + offsetX + rx;
                int cz = playerCZ + offsetZ + rz;
                int px = mapLeft + (rx + halfW) * CELL;
                int pz = mapTop  + (rz + halfH) * CELL;

                if (px < mapLeft || pz < mapTop || px + CELL > mapLeft + mapW || pz + CELL > mapTop + mapH)
                    continue;

                long key = ChunkPos.asLong(cx, cz);
                TerritoryDataPacket.ChunkInfo info = chunkLookup.get(key);
                if (info != null) {
                    g.fill(px, pz, px + CELL, pz + CELL, colorFor(info.relation));
                    if (info.dormant) {
                        for (int s = 0; s < CELL; s += 2) {
                            g.fill(px + s, pz, px + s + 1, pz + CELL, COLOR_DORMANT);
                        }
                    }
                    // Draw borders on edges adjacent to different-faction chunks
                    int bc = borderColorFor(info.relation);
                    if (isDifferentOwner(ChunkPos.asLong(cx, cz - 1), info)) g.fill(px, pz, px + CELL, pz + 1, bc);
                    if (isDifferentOwner(ChunkPos.asLong(cx, cz + 1), info)) g.fill(px, pz + CELL - 1, px + CELL, pz + CELL, bc);
                    if (isDifferentOwner(ChunkPos.asLong(cx - 1, cz), info)) g.fill(px, pz, px + 1, pz + CELL, bc);
                    if (isDifferentOwner(ChunkPos.asLong(cx + 1, cz), info)) g.fill(px + CELL - 1, pz, px + CELL, pz + CELL, bc);
                } else if (claimableChunks.contains(key)) {
                    g.fill(px, pz, px + CELL, pz + CELL, COLOR_CLAIMABLE);
                }

                // Player position marker — white outline
                if (cx == playerCX && cz == playerCZ) {
                    g.renderOutline(px, pz, CELL, CELL, COLOR_PLAYER);
                }
            }
        }

        g.disableScissor();
        RenderSystem.disableBlend();

        // ── Hover tooltip (outside scissor) ───────────────────────────────────
        if (mx >= mapLeft && mx < mapLeft + mapW && my >= mapTop && my < mapTop + mapH) {
            int relX = (mx - mapLeft) / CELL - halfW;
            int relZ = (my - mapTop)  / CELL - halfH;
            int hcx  = playerCX + offsetX + relX;
            int hcz  = playerCZ + offsetZ + relZ;
            long key  = ChunkPos.asLong(hcx, hcz);
            TerritoryDataPacket.ChunkInfo info = chunkLookup.get(key);

            String tip = "[" + hcx + ", " + hcz + "]";
            if (info != null && !info.factionName.isEmpty())
                tip += " " + info.factionName + (info.dormant ? " (dormant)" : "");
            if (hasShiftDown() && info != null && !info.ownerName.isEmpty())
                tip += " | " + info.ownerName;
            g.renderTooltip(font, Component.literal(tip), mx, my);
        }

        // ── Legend ────────────────────────────────────────────────────────────
        int lx = mapLeft;
        int ly = mapTop + mapH + 8;
        g.fill(lx, ly, lx + 8, ly + 8, COLOR_OWN);
        g.drawString(font, "Own", lx + 10, ly, 0xCCCCCC, false);
        lx += 35;
        g.fill(lx, ly, lx + 8, ly + 8, COLOR_ALLIED);
        g.drawString(font, "Allied", lx + 10, ly, 0xCCCCCC, false);
        lx += 50;
        g.fill(lx, ly, lx + 8, ly + 8, COLOR_ENEMY);
        g.drawString(font, "Enemy", lx + 10, ly, 0xCCCCCC, false);
        lx += 48;
        g.fill(lx, ly, lx + 8, ly + 8, COLOR_CLAIMABLE);
        g.drawString(font, "Claimable", lx + 10, ly, 0xCCCCCC, false);

        // ── Result feedback ───────────────────────────────────────────────────
        if (resultTimer > 0 && lastResult != null) {
            resultTimer--;
            String msg = switch (lastResult) {
                case SUCCESS -> "§aClaimed!";
                case PARTIAL -> "§ePartial claim.";
                case INVALID_RANK -> "§cInsufficient rank.";
                case INVALID_BIOME -> "§cInvalid biome.";
                case ENEMY_TERRITORY -> "§cEnemy territory.";
                case INSIDE_OWN_TERRITORY -> "§eAlready claimed.";
                default -> "";
            };
            if (!msg.isEmpty())
                g.drawCenteredString(font, Component.literal(msg), width / 2, height - 38, 0xFFFFFF);
        }

        super.render(g, mx, my, dt);
    }

    // ── Mouse ──────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (mx >= mapLeft && mx < mapLeft + mapW && my >= mapTop && my < mapTop + mapH) {
            if (btn == 2) { // middle — start drag
                dragging = true;
                dragStartX = mx;
                dragStartZ = my;
                return true;
            }

            int halfW = cellsW / 2;
            int halfH = cellsH / 2;
            int relX  = (int)(mx - mapLeft) / CELL - halfW;
            int relZ  = (int)(my - mapTop)  / CELL - halfH;
            int cx    = playerCX + offsetX + relX;
            int cz    = playerCZ + offsetZ + relZ;

            long key = ChunkPos.asLong(cx, cz);
            TerritoryDataPacket.ChunkInfo info = chunkLookup.get(key);

            if (btn == 0) { // left click → CLAIM unclaimed, or REQUEST_TRANSFER on allied/enemy
                ClaimChunkPacket.ClaimAction action;
                if (info == null) {
                    action = ClaimChunkPacket.ClaimAction.CLAIM;
                } else if (info.relation == TerritoryDataPacket.Relation.ALLIED
                        || info.relation == TerritoryDataPacket.Relation.ENEMY) {
                    action = ClaimChunkPacket.ClaimAction.REQUEST_TRANSFER;
                } else return true; // already own
                BoundaryHandler.sendClaimChunk(new ClaimChunkPacket(cx, cz, action));
                return true;
            }
            if (btn == 1) { // right click → UNCLAIM own
                if (info != null && info.relation == TerritoryDataPacket.Relation.OWN) {
                    BoundaryHandler.sendClaimChunk(new ClaimChunkPacket(cx, cz,
                            ClaimChunkPacket.ClaimAction.UNCLAIM));
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (dragging && btn == 2) {
            double totalDx = mx - dragStartX;
            double totalDz = my - dragStartZ;
            offsetX = -(int)(totalDx / CELL);
            offsetZ = -(int)(totalDz / CELL);
            return true;
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (btn == 2) dragging = false;
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (hasShiftDown()) offsetX -= (int) delta;
        else                offsetZ -= (int) delta;
        return true;
    }

    // ── Terrain texture baking ─────────────────────────────────────────────────

    /** Builds a NativeImage from lastTerrain and uploads it as a DynamicTexture. */
    private static void rebuildTerrainTexture() {
        terrainDirty = false;
        if (lastTerrain == null) return;

        final int subCell = CELL / TerrainSamplePacket.SUB; // 10/5 = 2 px per sub-sample
        int texW = lastTerrain.width  * CELL;
        int texH = lastTerrain.height * CELL;

        NativeImage img = new NativeImage(NativeImage.Format.RGBA, texW, texH, false);
        // Fill parchment fallback for unloaded (zero) sub-samples
        img.fillRect(0, 0, texW, texH, COLOR_MAP_BG);

        for (int rz = 0; rz < lastTerrain.height; rz++) {
            for (int rx = 0; rx < lastTerrain.width; rx++) {
                int baseIdx = (rz * lastTerrain.width + rx) * TerrainSamplePacket.SUB2;
                for (int sy = 0; sy < TerrainSamplePacket.SUB; sy++) {
                    for (int sx = 0; sx < TerrainSamplePacket.SUB; sx++) {
                        byte packed = lastTerrain.colors[baseIdx + sy * TerrainSamplePacket.SUB + sx];
                        if (packed == 0) continue; // unloaded — keep parchment
                        // NativeImage.setPixelRGBA takes the same ARGB format MapColor returns
                        int argb = TerrainSamplePacket.packedToArgb(packed);
                        int ipx = rx * CELL + sx * subCell;
                        int ipy = rz * CELL + sy * subCell;
                        for (int dy = 0; dy < subCell; dy++) {
                            for (int dx = 0; dx < subCell; dx++) {
                                img.setPixelRGBA(ipx + dx, ipy + dy, argb);
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

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void rebuildLookup() {
        chunkLookup = new HashMap<>(cachedChunks.size() * 2);
        for (TerritoryDataPacket.ChunkInfo info : cachedChunks) {
            chunkLookup.put(ChunkPos.asLong(info.chunkX, info.chunkZ), info);
        }
    }

    private static int colorFor(TerritoryDataPacket.Relation rel) {
        return switch (rel) {
            case OWN    -> COLOR_OWN;
            case ALLIED -> COLOR_ALLIED;
            case ENEMY  -> COLOR_ENEMY;
            default     -> 0x00000000; // fully transparent — bare terrain
        };
    }

    private static int borderColorFor(TerritoryDataPacket.Relation rel) {
        return switch (rel) {
            case OWN    -> BORDER_OWN;
            case ALLIED -> BORDER_ALLIED;
            case ENEMY  -> BORDER_ENEMY;
            default     -> 0x00000000;
        };
    }

    private boolean isDifferentOwner(long neighborKey, TerritoryDataPacket.ChunkInfo info) {
        TerritoryDataPacket.ChunkInfo neighbor = chunkLookup.get(neighborKey);
        if (neighbor == null) return true;
        if (neighbor.relation != info.relation) return true;
        return !neighbor.factionName.equals(info.factionName);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
