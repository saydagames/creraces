package mc.sayda.creraces.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.util.function.IntConsumer;

/**
 * Asks what to do with a CreRaces Classic world before singleplayer loads it. LegacyWorldLoadGate
 * opens it before WorldOpenFlows.loadLevel runs, so the answer only decides whether loading goes
 * ahead; nothing waits on this screen, which keeps it clear of the integrated server's startup.
 */
@SuppressWarnings("null")
public class LegacyMigrationPromptScreen extends Screen {
    private static final int CONTENT_WIDTH = 340;
    private static final int BUTTON_HEIGHT = 20;
    private static final int OPTION_COUNT = 3;
    private static final int TITLE_GAP = 8;
    private static final int SECTION_GAP = 14;
    private static final int BUTTON_TO_DESCRIPTION_GAP = 4;

    private final IntConsumer onChoice;

    /** @param onChoice receives the chosen option, 1 to 3 */
    public LegacyMigrationPromptScreen(IntConsumer onChoice) {
        super(Component.translatable("screen.creraces.legacy_migration.title"));
        this.onChoice = onChoice;
    }

    @Override
    protected void init() {
        int y = optionsTop();
        for (int option = 1; option <= OPTION_COUNT; option++) {
            int choice = option;
            this.addRenderableWidget(Button.builder(Component.translatable(optionKey(option)),
                            btn -> onChoice.accept(choice))
                    .bounds(this.width / 2 - CONTENT_WIDTH / 2, y, CONTENT_WIDTH, BUTTON_HEIGHT).build());
            y += optionBlockHeight(option);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        int left = this.width / 2 - CONTENT_WIDTH / 2;
        int y = layoutStartY();
        graphics.drawCenteredString(this.font, this.title, this.width / 2, y, 0xFFFFFF);
        y += titleHeight() + TITLE_GAP;

        graphics.drawWordWrap(this.font, bodyText(), left, y, CONTENT_WIDTH, 0xCCCCCC);

        y = optionsTop();
        for (int option = 1; option <= OPTION_COUNT; option++) {
            graphics.drawWordWrap(this.font, Component.translatable(descriptionKey(option)), left,
                    y + BUTTON_HEIGHT + BUTTON_TO_DESCRIPTION_GAP, CONTENT_WIDTH, 0x999999);
            y += optionBlockHeight(option);
        }
    }

    /** Top of the whole block (title, body, options), centred vertically but never above y = 20. */
    private int layoutStartY() {
        int totalHeight = titleHeight() + TITLE_GAP + bodyHeight() + SECTION_GAP;
        for (int option = 1; option <= OPTION_COUNT; option++) {
            totalHeight += optionBlockHeight(option);
        }
        return Math.max(20, this.height / 2 - totalHeight / 2);
    }

    private int optionsTop() {
        return layoutStartY() + titleHeight() + TITLE_GAP + bodyHeight() + SECTION_GAP;
    }

    /** One option's button, its wrapped description and the gap before the next option. */
    private int optionBlockHeight(int option) {
        int descriptionLines = Math.max(1, this.font.split(Component.translatable(descriptionKey(option)),
                CONTENT_WIDTH).size());
        return BUTTON_HEIGHT + BUTTON_TO_DESCRIPTION_GAP + descriptionLines * this.font.lineHeight + SECTION_GAP;
    }

    private int titleHeight() {
        return this.font.lineHeight;
    }

    private int bodyHeight() {
        return this.font.split(bodyText(), CONTENT_WIDTH).size() * this.font.lineHeight;
    }

    private static Component bodyText() {
        return Component.translatable("screen.creraces.legacy_migration.body");
    }

    private static String optionKey(int option) {
        return "screen.creraces.legacy_migration.option_" + option;
    }

    private static String descriptionKey(int option) {
        return optionKey(option) + ".desc";
    }

    // Drawn at the top of render() instead, so Screen.render() does not draw it a second time.
    @Override
    public void renderBackground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }
}
