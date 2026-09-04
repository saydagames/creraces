package mc.sayda.creraces.client.screen;

import mc.sayda.creraces.network.AbandonQuestPacket;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.network.TakeQuestPacket;
import mc.sayda.creraces.quest.Quest;
import mc.sayda.creraces.quest.QuestRegistry;
import mc.sayda.creraces.world.inventory.QuestBoardMenu;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * Shows one tier's 2 quest cards at a time (all 5 rows never fit on screen at once, even at
 * higher GUI scales), with T1-T5 tier-navigation buttons below to page between them. Each
 * card shows tier, name, full description, a plain-language objective line, and the time
 * limit. Each card's button flips between "Take Quest" and "Abandon Quest" based on the
 * menu's per-viewing-player taken state, re-evaluated every frame (matching
 * ResearchTableScreen's dynamic-button-relabeling pattern). A quest still on this player's
 * abandon/expiry cooldown gets a dark overlay across the whole card and a disabled button,
 * instead of a clickable "Take Quest" that the server would silently reject.
 */
public class QuestBoardScreen extends AbstractContainerScreen<QuestBoardMenu> {

    private static final int CARD_W = 210;
    private static final int CARD_H = 86;
    private static final int CARD_GAP_X = 10;
    private static final int CARD_GAP_Y = 6;
    private static final int CARD_PAD = 4;
    private static final int COLUMNS = mc.sayda.creraces.block.entity.QuestBoardBlockEntity.PER_TIER;
    private static final int TIERS = mc.sayda.creraces.block.entity.QuestBoardBlockEntity.TIERS;
    private static final int NAV_BTN_W = 36;
    private static final int NAV_BTN_H = 16;

    // Matches AbilitySlot A1-A5's colors in SkillWheelScreen.
    private static final int[] TIER_COLORS = {
            0xFF55FF55, 0xFFFF5555, 0xFFFFFF55, 0xFF5555FF, 0xFFFFAA00 // T1, T2, T3, T4, T5
    };

    private final Button[] cardButtons = new Button[COLUMNS];
    private final Button[] tierButtons = new Button[TIERS];
    private int currentTier = 1;

    public QuestBoardScreen(QuestBoardMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = COLUMNS * CARD_W + (COLUMNS + 1) * CARD_GAP_X;
        this.imageHeight = 20 + CARD_GAP_Y + CARD_H + CARD_GAP_Y + NAV_BTN_H + 8;
        this.inventoryLabelY = Integer.MIN_VALUE;
    }

    @Override
    protected void init() {
        super.init();

        for (int col = 0; col < COLUMNS; col++) {
            int[] xy = cardOrigin(col);
            final int c = col;
            cardButtons[col] = addRenderableWidget(Button.builder(Component.empty(), b -> onCardClicked(c))
                    .bounds(xy[0] + CARD_PAD, xy[1] + CARD_H - 16, CARD_W - CARD_PAD * 2, 14)
                    .build());
        }

        int navRowW = TIERS * NAV_BTN_W + (TIERS - 1) * 4;
        int navX = leftPos + (imageWidth - navRowW) / 2;
        int navY = topPos + 20 + CARD_GAP_Y + CARD_H + CARD_GAP_Y;
        for (int t = 1; t <= TIERS; t++) {
            final int tier = t;
            Button b = Button.builder(Component.translatable("gui.creraces.quest_board.tier", t),
                            btn -> selectTier(tier))
                    .bounds(navX + (t - 1) * (NAV_BTN_W + 4), navY, NAV_BTN_W, NAV_BTN_H)
                    .build();
            b.active = (t != currentTier);
            tierButtons[t - 1] = addRenderableWidget(b);
        }
    }

    private void selectTier(int tier) {
        this.currentTier = tier;
        // No widget rebuild needed: cardButtons' click handlers and renderBg both read
        // currentTier live, so only the tier-nav buttons' "current page" highlight needs updating.
        for (int t = 1; t <= TIERS; t++) {
            tierButtons[t - 1].active = (t != tier);
        }
    }

    private int slotFor(int col) {
        return (currentTier - 1) * COLUMNS + col;
    }

    private int[] cardOrigin(int col) {
        int x = leftPos + CARD_GAP_X + col * (CARD_W + CARD_GAP_X);
        int y = topPos + 20 + CARD_GAP_Y;
        return new int[]{x, y};
    }

    private void onCardClicked(int col) {
        int slot = slotFor(col);
        if (slot >= menu.getSlotCount()) return;
        if (menu.isLocked(slot)) return;
        var questId = menu.getQuestId(slot);
        if (menu.isTaken(slot)) {
            BoundaryHandler.sendAbandonQuest(new AbandonQuestPacket(questId));
        } else {
            BoundaryHandler.sendTakeQuest(new TakeQuestPacket(menu.getBoardPos(), questId));
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xC0101010);

        for (int col = 0; col < COLUMNS; col++) {
            int slot = slotFor(col);
            if (slot >= menu.getSlotCount()) continue;

            int[] xy = cardOrigin(col);
            int x = xy[0];
            int y = xy[1];
            boolean taken = menu.isTaken(slot);
            boolean locked = menu.isLocked(slot);

            g.fill(x, y, x + CARD_W, y + CARD_H, taken ? 0x80303030 : 0x80404060);
            g.fill(x, y, x + CARD_W, y + 1, 0xFF000000 | tierColor(currentTier));

            Quest quest = QuestRegistry.get(menu.getQuestId(slot));
            int textX = x + CARD_PAD;
            int textY = y + CARD_PAD;
            int maxWidth = CARD_W - CARD_PAD * 2;

            if (quest == null) {
                g.drawString(font, "?", textX, textY, 0xFFFF5555, false);
                continue;
            }

            Component tierLabel = Component.translatable("gui.creraces.quest_board.tier", currentTier);
            g.drawString(font, tierLabel.getString(), textX, textY, dim(tierColor(currentTier), taken), false);
            g.drawString(font, quest.name().getString(), textX + font.width(tierLabel) + 4, textY,
                    dim(0xFFFFFF, taken), false);
            textY += font.lineHeight + 2;

            textY = drawWrapped(g, quest.description(), textX, textY, maxWidth, 2, dim(0xC0C0C0, taken));

            Component objective = Component.translatable("gui.creraces.quest_board.objective",
                    quest.objective().verb(), quest.objective().count(), quest.objective().targetName());
            textY = drawWrapped(g, objective, textX, textY, maxWidth, 1, dim(0xFFFFAA, taken));

            String expires = Component.translatable("gui.creraces.quest_board.expires", quest.durationDays())
                    .getString();
            g.drawString(font, expires, textX, textY, dim(0xAAAAAA, taken), false);

            if (locked) {
                g.fill(x, y, x + CARD_W, y + CARD_H, 0xA0000000);
            }

            cardButtons[col].active = !locked;
            cardButtons[col].setMessage(Component.translatable(
                    taken ? "gui.creraces.quest_board.abandon" : "gui.creraces.quest_board.take"));
        }
    }

    private static int tierColor(int tier) {
        int idx = Math.max(1, Math.min(TIER_COLORS.length, tier)) - 1;
        return TIER_COLORS[idx];
    }

    private static int dim(int rgb, boolean dimmed) {
        if (!dimmed) return 0xFF000000 | rgb;
        int r = ((rgb >> 16) & 0xFF) / 2;
        int gg = ((rgb >> 8) & 0xFF) / 2;
        int b = (rgb & 0xFF) / 2;
        return 0xFF000000 | (r << 16) | (gg << 8) | b;
    }

    /** Draws up to {@code maxLines} of word-wrapped text, returning the y position after it. */
    private int drawWrapped(GuiGraphics g, FormattedText text, int x, int y, int maxWidth, int maxLines, int color) {
        Font f = this.font;
        List<net.minecraft.util.FormattedCharSequence> lines = f.split(text, maxWidth);
        int drawn = Math.min(lines.size(), maxLines);
        for (int i = 0; i < drawn; i++) {
            g.drawString(f, lines.get(i), x, y, color, false);
            y += f.lineHeight;
        }
        return y;
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, (imageWidth - font.width(title)) / 2, 4, 0xFFFFFFFF, false);
    }

    /**
     * 1.21's AbstractContainerScreen.renderBackground draws renderTransparentBackground before
     * renderBg, and Screen.render() now calls it automatically. This screen never dimmed the view
     * on 1.20.1 and paints its own opaque panel, so only the panel is drawn here.
     */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBg(graphics, partialTick, mouseX, mouseY);
    }
}
