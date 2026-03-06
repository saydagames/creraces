package mc.sayda.creraces.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.ChatFormatting;

public class DebugScreen extends Screen {
        private final List<Component> debugLines = new ArrayList<>();
        private double scrollAmount;

        public DebugScreen() {
                super(Component.translatable("gui.creraces.debug.title"));
        }

        @Override
        protected void init() {
                super.init();

                this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), (button) -> {
                        this.onClose();
                }).bounds(this.width / 2 - 100, this.height - 30, 200, 20).build());

                refreshDebugInfo();
        }

        private void refreshDebugInfo() {
                debugLines.clear();
                Player player = Minecraft.getInstance().player;
                if (player == null)
                        return;

                DataUtils.getVariables(player).ifPresent(vars -> {
                        // --- IDENTITY ---
                        addHeader("IDENTITY");
                        debugLines.add(Component.literal("  Player: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(player.getName().getString())
                                                        .withStyle(ChatFormatting.WHITE)));
                        debugLines.add(Component.literal("  Race ID: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(vars.getRace().toString())
                                                        .withStyle(ChatFormatting.AQUA)));

                        Race race = RaceRegistry.get(vars.getRace());
                        String raceName = race != null ? race.name().getString() : "None";
                        debugLines.add(Component.literal("  Race Name: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(raceName).withStyle(ChatFormatting.AQUA)));

                        if (race != null) {
                                if (race.parentRace() != null) {
                                        debugLines.add(Component.literal("  Parent: ").withStyle(ChatFormatting.GRAY)
                                                        .append(Component.literal(race.parentRace().toString())
                                                                        .withStyle(ChatFormatting.DARK_AQUA)));
                                }
                                debugLines.add(Component.literal("  Flags: ").withStyle(ChatFormatting.GRAY)
                                                .append(Component.literal("Spirit: ").withStyle(ChatFormatting.GRAY))
                                                .append(Component.literal(String.valueOf(race.isSpirit()))
                                                                .withStyle(race.isSpirit() ? ChatFormatting.GREEN
                                                                                : ChatFormatting.RED))
                                                .append(Component.literal(" | Tiny: ").withStyle(ChatFormatting.GRAY))
                                                .append(Component.literal(String.valueOf(race.isTiny()))
                                                                .withStyle(race.isTiny() ? ChatFormatting.GREEN
                                                                                : ChatFormatting.RED))
                                                .append(Component.literal(" | StkRes: ").withStyle(ChatFormatting.GRAY))
                                                .append(Component.literal(String.valueOf(race.stacksAffectResource()))
                                                                .withStyle(race.stacksAffectResource()
                                                                                ? ChatFormatting.GREEN
                                                                                : ChatFormatting.RED)));

                                debugLines.add(Component.literal("  gState: ").withStyle(ChatFormatting.GRAY)
                                                .append(Component.literal(String.valueOf(vars.getGState()))
                                                                .withStyle(ChatFormatting.WHITE)));

                                debugLines.add(Component.literal("  Base Ratios: ").withStyle(ChatFormatting.GRAY)
                                                .append(Component.literal("AP: " + race.baseAp())
                                                                .withStyle(ChatFormatting.GOLD))
                                                .append(Component.literal(" AD: " + race.baseAd())
                                                                .withStyle(ChatFormatting.GOLD))
                                                .append(Component.literal(" AH: " + race.baseAh())
                                                                .withStyle(ChatFormatting.GOLD))
                                                .append(Component.literal(" CR: " + race.baseCr())
                                                                .withStyle(ChatFormatting.GOLD)));
                        }

                        // --- STATISTICS ---
                        addHeader("STATISTICS");
                        debugLines.add(Component.literal("  AP: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(String.format("%.1f", vars.getAp()))
                                                        .withStyle(ChatFormatting.GREEN))
                                        .append(Component.literal(" | AD: ").withStyle(ChatFormatting.GRAY))
                                        .append(Component.literal(String.format("%.1f", vars.getAd()))
                                                        .withStyle(ChatFormatting.GREEN)));
                        debugLines.add(Component.literal("  AH: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(String.format("%.1f", vars.getAh()))
                                                        .withStyle(ChatFormatting.GREEN))
                                        .append(Component.literal(" | CR: ").withStyle(ChatFormatting.GRAY))
                                        .append(Component.literal(String.format("%.1f", vars.getCr()))
                                                        .withStyle(ChatFormatting.GREEN)));

                        // --- RESOURCES ---
                        addHeader("RESOURCES");
                        debugLines.add(Component.literal("  Mana: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(String.format("%.1f", vars.getMana()))
                                                        .withStyle(ChatFormatting.BLUE))
                                        .append(Component.literal(" | Rage: ").withStyle(ChatFormatting.GRAY))
                                        .append(Component.literal(String.format("%.1f", vars.getRage()))
                                                        .withStyle(ChatFormatting.RED)));

                        debugLines.add(Component.literal("  Energy: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(String.format("%.1f", vars.getEnergy()))
                                                        .withStyle(ChatFormatting.YELLOW))
                                        .append(Component.literal(" | Grit: ").withStyle(ChatFormatting.GRAY))
                                        .append(Component.literal(String.format("%.1f", vars.getGrit()))
                                                        .withStyle(ChatFormatting.WHITE)));

                        debugLines.add(Component.literal("  Karma: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(String.format("%.2f", vars.getKarma()))
                                                        .withStyle(ChatFormatting.GOLD))
                                        .append(Component.literal(" | Coins: ").withStyle(ChatFormatting.GRAY))
                                        .append(Component.literal(String.format("%.0f", vars.getCoins()))
                                                        .withStyle(ChatFormatting.GOLD)));

                        debugLines.add(Component.literal("  Stacks: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(String.format("%.1f", vars.getStacks()))
                                                        .withStyle(ChatFormatting.LIGHT_PURPLE)));

                        if (vars.getSouls() > 0) {
                                debugLines.add(Component.literal("  Souls: ").withStyle(ChatFormatting.GRAY)
                                                .append(Component.literal(String.format("%.0f", vars.getSouls()))
                                                                .withStyle(ChatFormatting.DARK_PURPLE)));
                        }

                        debugLines.add(Component.literal("  States: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal("Morphed: ").withStyle(ChatFormatting.GRAY))
                                        .append(Component.literal(String.valueOf(vars.isMorphed()))
                                                        .withStyle(vars.isMorphed() ? ChatFormatting.GREEN
                                                                        : ChatFormatting.RED)));

                        // --- ACTIVE ABILITY ---
                        addHeader("ACTIVE ABILITY");
                        if (vars.isAbilityActive() && vars.getActiveAbility() != null) {
                                debugLines.add(Component.literal("  ID: ").withStyle(ChatFormatting.GRAY)
                                                .append(Component.literal(vars.getActiveAbility().toString())
                                                                .withStyle(ChatFormatting.YELLOW)));
                                debugLines.add(Component.literal("  Duration: ").withStyle(ChatFormatting.GRAY)
                                                .append(Component
                                                                .literal(String.valueOf(
                                                                                vars.getActiveAbilityDuration()))
                                                                .withStyle(ChatFormatting.WHITE))
                                                .append(Component.literal(" | Drain: ").withStyle(ChatFormatting.GRAY))
                                                .append(Component
                                                                .literal(String.format("%.2f",
                                                                                vars.getActiveAbilityDrain()))
                                                                .withStyle(ChatFormatting.WHITE)));
                        } else {
                                debugLines.add(Component.literal("  - None").withStyle(ChatFormatting.DARK_GRAY));
                        }

                        // --- WORLD & POCKET ---
                        addHeader("WORLD & POCKET");
                        debugLines.add(Component.literal("  Pocket: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(String.valueOf(vars.hasPocket()))
                                                        .withStyle(vars.hasPocket() ? ChatFormatting.GREEN
                                                                        : ChatFormatting.RED)));
                        if (vars.hasPocket()) {
                                debugLines.add(Component.literal("    Pos: ").withStyle(ChatFormatting.GRAY)
                                                .append(Component
                                                                .literal(String.format("%.1f, %.1f, %.1f",
                                                                                vars.getPocketX(),
                                                                                vars.getPocketY(), vars.getPocketZ()))
                                                                .withStyle(ChatFormatting.WHITE)));
                        }
                        debugLines.add(Component.literal("  Return Dim: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(vars.getReturnDim())
                                                        .withStyle(ChatFormatting.WHITE)));
                        debugLines.add(Component.literal("    Pos: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(
                                                        String.format("%.1f, %.1f, %.1f", vars.getReturnX(),
                                                                        vars.getReturnY(), vars.getReturnZ()))
                                                        .withStyle(ChatFormatting.WHITE)));

                        // --- INTERNAL ---
                        addHeader("INTERNAL");
                        debugLines.add(Component.literal("  Passive CD: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(String.format("%.0f", vars.getPassiveCooldown()))
                                                        .withStyle(ChatFormatting.WHITE)));

                        // --- CUSTOMIZATIONS ---
                        addHeader("CUSTOMIZATIONS");
                        Map<String, String> cust = vars.getCustomizations();
                        if (cust.isEmpty()) {
                                debugLines.add(Component.literal("  - None").withStyle(ChatFormatting.DARK_GRAY));
                        } else {
                                cust.forEach((k, v) -> {
                                        debugLines.add(Component.literal("  - " + k + ": ")
                                                        .withStyle(ChatFormatting.GRAY)
                                                        .append(Component.literal(v)
                                                                        .withStyle(ChatFormatting.LIGHT_PURPLE)));
                                });
                        }

                        // --- ABILITIES & SLOTS ---
                        addHeader("ABILITIES & SLOTS");
                        debugLines.add(Component.literal("  Slots: ").withStyle(ChatFormatting.GRAY)
                                        .append(formatSlot(vars, mc.sayda.creraces.ability.AbilitySlot.A1))
                                        .append(" ").append(formatSlot(vars, mc.sayda.creraces.ability.AbilitySlot.A2))
                                        .append(" ").append(formatSlot(vars, mc.sayda.creraces.ability.AbilitySlot.A3))
                                        .append(" ").append(formatSlot(vars, mc.sayda.creraces.ability.AbilitySlot.A4))
                                        .append(" ")
                                        .append(formatSlot(vars, mc.sayda.creraces.ability.AbilitySlot.A5)));

                        debugLines.add(Component.literal("  Unlocked:").withStyle(ChatFormatting.GRAY));
                        Set<ResourceLocation> unlocked = vars.getUnlockedAbilities();
                        if (unlocked.isEmpty()) {
                                debugLines.add(Component.literal("    - None").withStyle(ChatFormatting.DARK_GRAY));
                        } else {
                                for (ResourceLocation ability : unlocked) {
                                        debugLines.add(Component.literal("    - " + ability.toString())
                                                        .withStyle(ChatFormatting.YELLOW));
                                }
                        }
                });
        }

        private void addHeader(String title) {
                if (!debugLines.isEmpty()) {
                        debugLines.add(Component.literal(" ")); // Spacer
                }
                debugLines.add(Component.literal("[ " + title + " ]").withStyle(ChatFormatting.BOLD,
                                ChatFormatting.GOLD));
        }

        private Component formatSlot(IPlayerVariables vars, mc.sayda.creraces.ability.AbilitySlot slot) {
                ResourceLocation abilityId = vars.getAbilityInSlot(slot);
                double state = abilityId != null ? vars.getAbilityState(abilityId) : 0.0;
                return Component.literal(slot.name() + "(" + String.format("%.0f", state) + ")")
                                .withStyle(state > 0 ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY);
        }

        @Override
        public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                refreshDebugInfo();
                this.renderBackground(graphics);

                graphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFD700);

                int listWidth = Math.min(this.width - 40, 400);
                int listLeft = (this.width - listWidth) / 2;
                int listTop = 25;
                int listBottom = this.height - 45;

                // Draw Panel Background
                graphics.fill(listLeft - 5, listTop - 5, listLeft + listWidth + 5, listBottom + 5, 0xAA000000);
                graphics.renderOutline(listLeft - 5, listTop - 5, listWidth + 10, (listBottom - listTop) + 10,
                                0xAA888888);

                int entryHeight = 10;
                int totalHeight = debugLines.size() * entryHeight;

                // Clamp scroll
                int maxScroll = Math.max(0, totalHeight - (listBottom - listTop));
                this.scrollAmount = Mth.clamp(this.scrollAmount, 0, maxScroll);

                graphics.enableScissor(listLeft, listTop, listLeft + listWidth, listBottom);
                PoseStack pose = graphics.pose();
                pose.pushPose();
                pose.translate(0, -this.scrollAmount, 0);

                int y = listTop;
                for (Component line : debugLines) {
                        graphics.drawString(this.font, line, listLeft + 5, y, 0xFFFFFF);
                        y += entryHeight;
                }

                pose.popPose();
                graphics.disableScissor();

                // Draw Scrollbar (modern)
                if (maxScroll > 0) {
                        int scrollbarX = listLeft + listWidth + 2;
                        int barHeight = Math.max(10, (listBottom - listTop) * (listBottom - listTop) / totalHeight);
                        int barTop = listTop
                                        + (int) ((listBottom - listTop - barHeight) * (this.scrollAmount / maxScroll));
                        graphics.fill(scrollbarX, barTop, scrollbarX + 3, barTop + barHeight, 0xAAFFFFFF);
                }

                super.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
                this.scrollAmount -= delta * 10;
                return true;
        }

        @Override
        public boolean isPauseScreen() {
                return false;
        }
}
