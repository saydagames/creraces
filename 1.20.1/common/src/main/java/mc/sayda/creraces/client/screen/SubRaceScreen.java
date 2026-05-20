package mc.sayda.creraces.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import mc.sayda.creraces.race.Race;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Sub-race selection screen matching RaceSelectionScreen layout for legacy 1:1
 * restoration.
 */
public class SubRaceScreen extends Screen {
    private static final ResourceLocation SELECTION_BG = new ResourceLocation("creraces",
            "textures/screens/selection_bg.png");
    private static final ResourceLocation SELECTION_BORDER = new ResourceLocation("creraces",
            "textures/screens/selection_border.png");
    private static final ResourceLocation ARROW_LEFT = new ResourceLocation("creraces",
            "textures/screens/atlas/arrow_left.png");
    private static final ResourceLocation RACE_SLOT = new ResourceLocation("creraces", "textures/screens/race.png");
    private static final ResourceLocation PORTRAIT_WARNING = new ResourceLocation("creraces", "textures/screens/portrait_warning.png");
    private static final ResourceLocation PORTRAIT_ERROR = new ResourceLocation("creraces", "textures/screens/portrait_error.png");
    private static final ResourceLocation PORTRAIT_INFO = new ResourceLocation("creraces", "textures/screens/portrait_info.png");

    // Legacy positioning
    private static final int[] PORTRAIT_COLS = { 8, 67, 124 };
    private static final int[] PORTRAIT_ROWS = { -20, 49, 119 };
    private static final int[] BTN_COLS = { 5, 63, 120 };
    private static final int[] BTN_ROWS = { -19, 50, 120 };

    private static final int PORTRAIT_SIZE = 43;
    private static final int BTN_W = 50;

    private final Screen parent;

    private final List<Race> subRaces;
    private int leftPos, topPos;

    public SubRaceScreen(Screen parent, Component groupName, List<Race> subRaces) {
        super(groupName);
        this.parent = parent;
        this.subRaces = subRaces.stream()
            .sorted(java.util.Comparator.comparing(Race::index)
                .thenComparing(r -> r.name().getString()))
            .toList();
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - 176) / 2;
        this.topPos = (this.height - 166) / 2;

        // Back button (left arrow)
        this.addRenderableWidget(new ImageButton(
                this.leftPos + -24, this.topPos + 184, 86, 48,
                0, 0, 48, ARROW_LEFT, 86, 96,
                btn -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(parent);
                    }
                }));

        // Sub-race slot buttons
        for (int i = 0; i < Math.min(subRaces.size(), 9); i++) {
            int rowIdx = i / 3;
            int colIdx = i % 3;
            int slotX = this.leftPos + BTN_COLS[colIdx];
            int slotY = this.topPos + BTN_ROWS[rowIdx];

            final Race race = subRaces.get(i);
            this.addRenderableWidget(new GenericRaceButton(
                    slotX, slotY + 44, BTN_W, 20,
                    Component.translatable("gui.creraces.button.select"),
                    btn -> {
                        if (this.minecraft != null) {
                            this.minecraft.setScreen(new RaceDetailsScreen(this, race));
                        }
                    }));
        }
    }

    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // 1. BG and Border
        graphics.blit(SELECTION_BG, this.leftPos + -4, this.topPos + -26, 0, 0, 181, 220, 181, 220);
        graphics.blit(SELECTION_BORDER, this.leftPos + -25, this.topPos + -47, 0, 0, 225, 264, 225, 264);

        // 2. Portraits
        Component hoverTooltip = null;
        for (int i = 0; i < 9; i++) {
            int rowIdx = i / 3;
            int colIdx = i % 3;
            int portraitX = this.leftPos + PORTRAIT_COLS[colIdx];
            int portraitY = this.topPos + PORTRAIT_ROWS[rowIdx];

            if (i < subRaces.size()) {
                Race race = subRaces.get(i);
                if (race.portrait() != null) {
                    graphics.blit(race.portrait(), portraitX, portraitY, 0, 0, PORTRAIT_SIZE, PORTRAIT_SIZE,
                            PORTRAIT_SIZE, PORTRAIT_SIZE);
                    
                    // Render State Overlays
                    Race.RaceState state = race.state();
                    if (state != Race.RaceState.FINISHED) {
                        ResourceLocation overlay = switch (state) {
                            case NEW -> PORTRAIT_INFO;
                            case EXPERIMENTAL -> PORTRAIT_ERROR;
                            case UNFINISHED -> PORTRAIT_WARNING;
                            default -> null;
                        };
                        if (overlay != null) {
                            graphics.blit(overlay, portraitX, portraitY, 0, 0, PORTRAIT_SIZE, PORTRAIT_SIZE, PORTRAIT_SIZE, PORTRAIT_SIZE);
                            
                            // Check for hover
                            if (mouseX >= portraitX && mouseX < portraitX + PORTRAIT_SIZE && mouseY >= portraitY && mouseY < portraitY + PORTRAIT_SIZE) {
                                hoverTooltip = Component.translatable("gui.creraces.status." + state.name().toLowerCase());
                            }
                        }
                    }
                } else {
                    graphics.blit(RACE_SLOT, portraitX, portraitY, 0, 0, PORTRAIT_SIZE, PORTRAIT_SIZE, PORTRAIT_SIZE,
                            PORTRAIT_SIZE);
                }
            } else {
                graphics.blit(RACE_SLOT, portraitX, portraitY, 0, 0, PORTRAIT_SIZE, PORTRAIT_SIZE, PORTRAIT_SIZE,
                        PORTRAIT_SIZE);
            }
        }

        // 3. Group title - Removed for clean grid view
        // graphics.drawCenteredString(this.font, groupName, this.leftPos + 88,
        // this.topPos + -38, 0xFFCC00);

        super.render(graphics, mouseX, mouseY, partialTick);

        if (hoverTooltip != null) {
            graphics.renderTooltip(this.font, hoverTooltip, mouseX, mouseY);
        }

        RenderSystem.disableBlend();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
