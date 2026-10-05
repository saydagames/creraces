package mc.sayda.creraces.client.screen;

import mc.sayda.creraces.block.entity.QuestBoardBlockEntity;
import mc.sayda.creraces.network.AbandonQuestPacket;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.network.TakeQuestPacket;
import mc.sayda.creraces.quest.Quest;
import mc.sayda.creraces.quest.QuestRegistry;
import mc.sayda.creraces.world.inventory.QuestBoardMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * Shows one tier's quest cards at a time, with T1-T5 buttons below to switch tier; all tiers at
 * once never fit, even at small GUI scales. Card buttons flip between Take and Abandon from the
 * menu's per-player taken state every frame. A quest still on this player's cooldown gets a dark
 * overlay and a disabled button rather than a Take the server would silently reject.
 */
public class QuestBoardScreen extends AbstractContainerScreen<QuestBoardMenu> {
    private static final int CARD_W = 210;
    private static final int CARD_H = 86;
    private static final int CARD_GAP_X = 10;
    private static final int CARD_GAP_Y = 6;
    private static final int CARD_PAD = 4;
    private static final int HEADER_H = 20;
    private static final int COLUMNS = QuestBoardBlockEntity.PER_TIER;
    private static final int TIERS = QuestBoardBlockEntity.TIERS;
    private static final int NAV_BTN_W = 36;
    private static final int NAV_BTN_H = 16;
    private static final int NAV_BTN_GAP = 4;

    // Same colours as ability slots A1-A5 in SkillWheelScreen.
    private static final int[] TIER_COLORS = {
            0xFF55FF55, 0xFFFF5555, 0xFFFFFF55, 0xFF5555FF, 0xFFFFAA00 // T1, T2, T3, T4, T5
    };

    private final Button[] cardButtons = new Button[COLUMNS];
    private final Button[] tierButtons = new Button[TIERS];
    private int currentTier = 1;

    public QuestBoardScreen(QuestBoardMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = COLUMNS * CARD_W + (COLUMNS + 1) * CARD_GAP_X;
        this.imageHeight = HEADER_H + CARD_GAP_Y + CARD_H + CARD_GAP_Y + NAV_BTN_H + 8;
        this.inventoryLabelY = Integer.MIN_VALUE;
    }

    @Override
    protected void init() {
        super.init();

        for (int col = 0; col < COLUMNS; col++) {
            int column = col;
            cardButtons[col] = addRenderableWidget(Button.builder(Component.empty(), b -> onCardClicked(column))
                    .bounds(cardX(col) + CARD_PAD, cardY() + CARD_H - 16, CARD_W - CARD_PAD * 2, 14)
                    .build());
        }

        int navRowW = TIERS * NAV_BTN_W + (TIERS - 1) * NAV_BTN_GAP;
        int navX = leftPos + (imageWidth - navRowW) / 2;
        int navY = cardY() + CARD_H + CARD_GAP_Y;
        for (int t = 1; t <= TIERS; t++) {
            int tier = t;
            Button b = Button.builder(Component.translatable("gui.creraces.quest_board.tier", t),
                            btn -> selectTier(tier))
                    .bounds(navX + (t - 1) * (NAV_BTN_W + NAV_BTN_GAP), navY, NAV_BTN_W, NAV_BTN_H)
                    .build();
            b.active = t != currentTier;
            tierButtons[t - 1] = addRenderableWidget(b);
        }
    }

    private void selectTier(int tier) {
        this.currentTier = tier;
        // The card buttons and renderBg read currentTier live, so only the tier buttons need updating.
        for (int t = 1; t <= TIERS; t++) {
            tierButtons[t - 1].active = t != tier;
        }
    }

    private int slotFor(int col) {
        return (currentTier - 1) * COLUMNS + col;
    }

    private int cardX(int col) {
        return leftPos + CARD_GAP_X + col * (CARD_W + CARD_GAP_X);
    }

    private int cardY() {
        return topPos + HEADER_H + CARD_GAP_Y;
    }

    private void onCardClicked(int col) {
        int slot = slotFor(col);
        if (slot >= menu.getSlotCount() || menu.isLocked(slot)) {
            return;
        }
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
            if (slotFor(col) < menu.getSlotCount()) {
                renderCard(g, col);
            }
        }
    }

    private void renderCard(GuiGraphics g, int col) {
        int slot = slotFor(col);
        int x = cardX(col);
        int y = cardY();
        boolean taken = menu.isTaken(slot);
        boolean locked = menu.isLocked(slot);

        g.fill(x, y, x + CARD_W, y + CARD_H, taken ? 0x80303030 : 0x80404060);
        g.fill(x, y, x + CARD_W, y + 1, 0xFF000000 | tierColor(currentTier));

        int textX = x + CARD_PAD;
        int textY = y + CARD_PAD;
        int maxWidth = CARD_W - CARD_PAD * 2;

        Quest quest = QuestRegistry.get(menu.getQuestId(slot));
        if (quest == null) {
            g.drawString(font, "?", textX, textY, 0xFFFF5555, false);
            return;
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

    private static int tierColor(int tier) {
        int idx = Math.max(1, Math.min(TIER_COLORS.length, tier)) - 1;
        return TIER_COLORS[idx];
    }

    /** Opaque version of the colour, at half brightness when dimmed. */
    private static int dim(int rgb, boolean dimmed) {
        if (!dimmed) {
            return 0xFF000000 | rgb;
        }
        int red = ((rgb >> 16) & 0xFF) / 2;
        int green = ((rgb >> 8) & 0xFF) / 2;
        int blue = (rgb & 0xFF) / 2;
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    /** Draws up to {@code maxLines} of word-wrapped text, returning the y position after it. */
    private int drawWrapped(GuiGraphics g, FormattedText text, int x, int y, int maxWidth, int maxLines, int color) {
        List<FormattedCharSequence> lines = font.split(text, maxWidth);
        int drawn = Math.min(lines.size(), maxLines);
        for (int i = 0; i < drawn; i++) {
            g.drawString(font, lines.get(i), x, y, color, false);
            y += font.lineHeight;
        }
        return y;
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, (imageWidth - font.width(title)) / 2, 4, 0xFFFFFFFF, false);
    }

    // Screen.render() calls this on 1.21, and the container version would dim the whole view first.
    // This screen never dimmed on 1.20.1 and paints its own dark panel, so only renderBg is drawn.
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBg(graphics, partialTick, mouseX, mouseY);
    }
}
