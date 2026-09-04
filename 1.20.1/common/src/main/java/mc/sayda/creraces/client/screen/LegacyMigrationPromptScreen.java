package mc.sayda.creraces.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.util.function.IntConsumer;

/**
 * Shown once, on the local (singleplayer) client only, when a CreRaces Classic world is detected.
 * Constructed and dismissed entirely by LegacyWorldLoadGate, BEFORE the world/integrated server
 * starts loading at all, never while anything is blocked waiting on it. An earlier version of
 * this feature tried to block the integrated server's startup thread on this screen's answer;
 * that raced vanilla's own level-loading screen for control of the display and tripped Forge's
 * server watchdog, crashing the game. Answering here now simply decides whether
 * WorldOpenFlows.loadLevel proceeds at all, nothing is ever blocked.
 */
public class LegacyMigrationPromptScreen extends Screen {
    private static final int CONTENT_WIDTH = 340;
    private static final int BUTTON_WIDTH = 340;
    private static final int BUTTON_HEIGHT = 20;

    private final IntConsumer onChoice;

    public LegacyMigrationPromptScreen(IntConsumer onChoice) {
        super(Component.translatable("screen.creraces.legacy_migration.title"));
        this.onChoice = onChoice;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = layoutStartY();

        y += titleHeight() + 8;
        y += bodyHeight() + 14;

        y = addOption(centerX, y, 1, "screen.creraces.legacy_migration.option_1", "screen.creraces.legacy_migration.option_1.desc");
        y = addOption(centerX, y, 2, "screen.creraces.legacy_migration.option_2", "screen.creraces.legacy_migration.option_2.desc");
        addOption(centerX, y, 3, "screen.creraces.legacy_migration.option_3", "screen.creraces.legacy_migration.option_3.desc");
    }

    private int addOption(int centerX, int y, int choice, String labelKey, String descKey) {
        this.addRenderableWidget(Button.builder(Component.translatable(labelKey), btn -> onChoice.accept(choice))
                .bounds(centerX - BUTTON_WIDTH / 2, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        y += BUTTON_HEIGHT + 4;
        y += descLineCount(descKey) * this.font.lineHeight;
        return y + 14;
    }

    private int layoutStartY() {
        int totalHeight = titleHeight() + 8 + bodyHeight() + 14
                + optionBlockHeight("screen.creraces.legacy_migration.option_1.desc")
                + optionBlockHeight("screen.creraces.legacy_migration.option_2.desc")
                + optionBlockHeight("screen.creraces.legacy_migration.option_3.desc");
        return Math.max(20, this.height / 2 - totalHeight / 2);
    }

    private int optionBlockHeight(String descKey) {
        return BUTTON_HEIGHT + 4 + descLineCount(descKey) * this.font.lineHeight + 14;
    }

    private int titleHeight() {
        return this.font.lineHeight;
    }

    private int bodyHeight() {
        return this.font.split(Component.translatable("screen.creraces.legacy_migration.body"), CONTENT_WIDTH).size()
                * this.font.lineHeight;
    }

    private int descLineCount(String descKey) {
        return Math.max(1, this.font.split(Component.translatable(descKey), CONTENT_WIDTH).size());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    @SuppressWarnings("null")
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int y = layoutStartY();

        graphics.drawCenteredString(this.font, this.title, centerX, y, 0xFFFFFF);
        y += titleHeight() + 8;

        graphics.drawWordWrap(this.font, Component.translatable("screen.creraces.legacy_migration.body"),
                centerX - CONTENT_WIDTH / 2, y, CONTENT_WIDTH, 0xCCCCCC);
        y += bodyHeight() + 14;

        y = drawOptionDesc(graphics, centerX, y, "screen.creraces.legacy_migration.option_1.desc");
        y = drawOptionDesc(graphics, centerX, y, "screen.creraces.legacy_migration.option_2.desc");
        drawOptionDesc(graphics, centerX, y, "screen.creraces.legacy_migration.option_3.desc");
    }

    private int drawOptionDesc(GuiGraphics graphics, int centerX, int y, String descKey) {
        y += BUTTON_HEIGHT + 4;
        graphics.drawWordWrap(this.font, Component.translatable(descKey),
                centerX - CONTENT_WIDTH / 2, y, CONTENT_WIDTH, 0x999999);
        y += descLineCount(descKey) * this.font.lineHeight;
        return y + 14;
    }
}
