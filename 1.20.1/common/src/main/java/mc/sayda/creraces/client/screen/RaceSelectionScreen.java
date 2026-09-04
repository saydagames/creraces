package mc.sayda.creraces.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Race selection screen matching legacy Race3GUIScreen precisely.
 */
public class RaceSelectionScreen extends Screen {
        private static final ResourceLocation SELECTION_BG = new ResourceLocation("creraces",
                        "textures/screens/selection_bg.png");
        private static final ResourceLocation SELECTION_BORDER = new ResourceLocation("creraces",
                        "textures/screens/selection_border.png");
        private static final ResourceLocation ARROW_LEFT = new ResourceLocation("creraces",
                        "textures/screens/atlas/arrow_left.png");
        private static final ResourceLocation ARROW_RIGHT = new ResourceLocation("creraces",
                        "textures/screens/atlas/arrow_right.png");
        private static final ResourceLocation RACE_SLOT = new ResourceLocation("creraces", "textures/screens/race.png");
        private static final ResourceLocation PORTRAIT_WARNING = new ResourceLocation("creraces", "textures/screens/portrait_warning.png");
        private static final ResourceLocation PORTRAIT_ERROR = new ResourceLocation("creraces", "textures/screens/portrait_error.png");
        private static final ResourceLocation PORTRAIT_INFO = new ResourceLocation("creraces", "textures/screens/portrait_info.png");

        private static final ResourceLocation DECO_CHRISTMAS = new ResourceLocation("creraces",
                        "textures/screens/christmas_decoration.png");
        private static final ResourceLocation DECO_HALLOWEEN = new ResourceLocation("creraces",
                        "textures/screens/halloween_decoration.png");
        private static final ResourceLocation DECO_MIDSUMMER = new ResourceLocation("creraces",
                        "textures/screens/midsummer_decoration.png");

        // Legacy positioning
        private static final int[] PORTRAIT_COLS = { 8, 67, 124 };
        private static final int[] PORTRAIT_ROWS = { -20, 49, 119 };
        private static final int[] BTN_COLS = { 5, 63, 120 };
        private static final int[] BTN_ROWS = { -19, 50, 120 };

        private static final int PORTRAIT_SIZE = 43;
        private static final int BTN_W = 50;

        private int leftPos, topPos;
        private int page = 0;
        private List<RaceEntry> raceEntries = new ArrayList<>();

        public RaceSelectionScreen() {
                super(Component.translatable("screen.creraces.race_selection_grid"));
        }

        @Override
        protected void init() {
                this.leftPos = (this.width - 176) / 2;
                this.topPos = (this.height - 166) / 2;

                if (this.minecraft != null) {
                        raceEntries = RaceRegistry.getRaces().stream()
                                .filter(RaceRegistry::isSelectableRoot)
                                .sorted(java.util.Comparator.comparing(Race::index)
                                                .thenComparing(r -> r.name().getString()))
                                .map(r -> new RaceEntry(r.id(), r.name(), r.portrait(), RaceRegistry.isParent(r.id()), r.state()))
                                .toList();
                }

                rebuildButtons();
        }

        private void rebuildButtons() {
                this.clearWidgets();

                int startIndex = page * 9;
                for (int i = 0; i < 9; i++) {
                        int currentIdx = startIndex + i;
                        int rowIdx = i / 3;
                        int colIdx = i % 3;

                        int slotX = this.leftPos + BTN_COLS[colIdx];
                        int slotY = this.topPos + BTN_ROWS[rowIdx];

                        final int capturedIdx = currentIdx;
                        if (currentIdx < raceEntries.size()) {
                                this.addRenderableWidget(new GenericRaceButton(
                                                slotX, slotY + 44, BTN_W, 20,
                                                Component.translatable("gui.creraces.button.select"),
                                                btn -> {
                                                        onRaceSelected(raceEntries.get(capturedIdx));
                                                }));
                        }
                }

                // Arrows (Page Navigation)
                this.addRenderableWidget(new ImageButton(
                                this.leftPos + -24, this.topPos + 184, 86, 48,
                                0, 0, 48, ARROW_LEFT, 86, 144,
                                btn -> {
                                        if (page > 0) {
                                                page--;
                                                rebuildButtons();
                                        } else {
                                                // Page 0 -> Return to Welcome Screen
                                                net.minecraft.client.Minecraft.getInstance().setScreen(new MenuGUIScreen());
                                        }
                                }));

                this.addRenderableWidget(new ImageButton(
                                this.leftPos + 113, this.topPos + 184, 86, 48,
                                0, 0, 48, ARROW_RIGHT, 86, 144,
                                btn -> {
                                        if ((page + 1) * 9 < raceEntries.size()) {
                                                page++;
                                                rebuildButtons();
                                        }
                                }));
        }

        private void onRaceSelected(RaceEntry entry) {
                if (entry.isParentGroup) {
                        if (this.minecraft != null) {
                                this.minecraft.setScreen(new SubRaceScreen(this, entry.name,
                                                RaceRegistry.getSubRaces(entry.id)));
                        }
                } else {
                        // Open Focus Details Screen
                        Race race = RaceRegistry.get(entry.id);
                        if (race != null && this.minecraft != null) {
                                this.minecraft.setScreen(new RaceDetailsScreen(this, race));
                        }
                }
        }

        @Override
        @SuppressWarnings("null")
        public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                this.renderBackground(graphics);
                RenderSystem.setShaderColor(1, 1, 1, 1);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();

                // 1. BG and Border
                graphics.blit(SELECTION_BG, this.leftPos + -4, this.topPos + -26, 0, 0, 181, 220, 181, 220);
                graphics.blit(SELECTION_BORDER, this.leftPos + -25, this.topPos + -47, 0, 0, 225, 264, 225, 264);

                // 2. Portraits (or empty slots)
                int startIndex = page * 9;
                Component hoverTooltip = null;
                for (int i = 0; i < 9; i++) {
                        int currentIdx = startIndex + i;
                        int rowIdx = i / 3;
                        int colIdx = i % 3;
                        int portraitX = this.leftPos + PORTRAIT_COLS[colIdx];
                        int portraitY = this.topPos + PORTRAIT_ROWS[rowIdx];

                        if (currentIdx < raceEntries.size()) {
                                graphics.blit(raceEntries.get(currentIdx).portrait, portraitX, portraitY, 0, 0,
                                                PORTRAIT_SIZE,
                                                PORTRAIT_SIZE, PORTRAIT_SIZE, PORTRAIT_SIZE);
                                
                                // Render State Overlays
                                Race.RaceState state = raceEntries.get(currentIdx).state;
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
                                graphics.blit(RACE_SLOT, portraitX, portraitY, 0, 0, PORTRAIT_SIZE, PORTRAIT_SIZE,
                                                PORTRAIT_SIZE,
                                                PORTRAIT_SIZE);
                        }
                }

                // 3. Decorations based on date
                Calendar now = Calendar.getInstance();
                int month = now.get(Calendar.MONTH);
                int day = now.get(Calendar.DAY_OF_MONTH);
                if (month == Calendar.DECEMBER) {
                        graphics.blit(DECO_CHRISTMAS, this.leftPos + 11, this.topPos - 56, 0, 0, 151, 42, 151, 42);
                } else if (month == Calendar.OCTOBER) {
                        graphics.blit(DECO_HALLOWEEN, this.leftPos + 11, this.topPos - 56, 0, 0, 151, 42, 151, 42);
                } else if (month == Calendar.JUNE && day >= 19 && day <= 26) {
                        // Midsummer week: traditional Nordic celebration around the summer solstice, not the whole summer.
                        graphics.blit(DECO_MIDSUMMER, this.leftPos + 11, this.topPos - 56, 0, 0, 151, 42, 151, 42);
                }

                super.render(graphics, mouseX, mouseY, partialTick);

                if (hoverTooltip != null) {
                        graphics.renderTooltip(this.font, hoverTooltip, mouseX, mouseY);
                }

                RenderSystem.disableBlend();

                Component pageCounter = Component.translatable("gui.creraces.selection.page", (page + 1),
                                ((raceEntries.size() + 8) / 9));
                graphics.drawString(this.font, pageCounter, this.leftPos + 73, this.topPos + 205, -1, false);
        }

        @Override
        public boolean isPauseScreen() {
                return false;
        }

        private static class RaceEntry {
                public final ResourceLocation id;
                public final Component name;
                public final ResourceLocation portrait;
                public final boolean isParentGroup;
                public final Race.RaceState state;

                public RaceEntry(ResourceLocation id, Component name, ResourceLocation portrait, boolean isParentGroup, Race.RaceState state) {
                        this.id = id;
                        this.name = name;
                        this.portrait = portrait;
                        this.isParentGroup = isParentGroup;
                        this.state = state;
                }
        }
}
