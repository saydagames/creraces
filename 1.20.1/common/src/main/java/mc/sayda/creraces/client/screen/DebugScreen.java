package mc.sayda.creraces.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import mc.sayda.creraces.config.CreRacesConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import javax.annotation.Nonnull;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.EditBox;

public class DebugScreen extends Screen {
        private static final int LINE_HEIGHT = 12;
        private final List<Component> debugLines = new ArrayList<>();
        private double scrollAmount;
        private int maxScroll;
        private final List<LineMetadata> lineMetadata = new ArrayList<>();
        private LineMetadata selectedLine = null;
        private long lastRefreshTick = -1;
        private EditBox editBox;
        private Button applyButton;
        private Button cancelButton;

        public DebugScreen() {
                super(Component.translatable("gui.creraces.debug.title"));
        }

        @Override
        @SuppressWarnings("null")
        protected void init() {
                this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), (button) -> {
                        this.onClose();
                }).bounds(this.width / 2 - 100, this.height - 30, 98, 20).build());

                this.addRenderableWidget(Button.builder(Component.literal("Copy"), (button) -> {
                        StringBuilder sb = new StringBuilder();
                        for (Component line : debugLines) {
                                sb.append(line.getString()).append("\n");
                        }
                        Minecraft.getInstance().keyboardHandler.setClipboard(sb.toString());
                }).bounds(this.width / 2 + 2, this.height - 30, 98, 20).build());

                // Editor widgets (hidden by default)
                // Two-row layout: Label on top, Input/Buttons below
                // Slimmer EditBox (100) and Buttons (45) centered as a group
                editBox = new EditBox(this.font, this.width / 2 - 100, this.height - 60, 100, 16, Component.empty());
                applyButton = Button.builder(Component.literal("Apply"), b -> applyEdit())
                                .bounds(this.width / 2 + 5, this.height - 60, 45, 16).build();
                cancelButton = Button.builder(Component.literal("Cancel"), b -> cancelEdit())
                                .bounds(this.width / 2 + 55, this.height - 60, 45, 16).build();

                this.addRenderableWidget(editBox);
                this.addRenderableWidget(applyButton);
                this.addRenderableWidget(cancelButton);

                editBox.visible = false;
                applyButton.visible = false;
                cancelButton.visible = false;

                refreshDebugInfo();
        }

        private void applyEdit() {
                if (selectedLine != null && !editBox.getValue().isEmpty()) {
                        mc.sayda.creraces.network.BoundaryHandler.sendDebugAction(selectedLine.action, selectedLine.key,
                                        editBox.getValue());
                        cancelEdit();
                        refreshDebugInfo();
                }
        }

        @SuppressWarnings("null")
        private void cancelEdit() {
                selectedLine = null;
                editBox.visible = false;
                applyButton.visible = false;
                cancelButton.visible = false;
                this.setFocused(null);
        }

        @SuppressWarnings("null")
        private void refreshDebugInfo() {
                debugLines.clear();
                lineMetadata.clear();
                Player player = Minecraft.getInstance().player;
                if (player == null)
                        return;

                DataUtils.getVariables(player).ifPresent(vars -> {

                        // --- IDENTITY ---
                        debugLines.add(Component.literal(""));
                        addHeader("IDENTITY");
                        Race race = RaceRegistry.get(vars.getRace());
                        debugLines.add(Component.literal("  Player: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(player.getName().getString())
                                                        .withStyle(ChatFormatting.WHITE)));
                        debugLines.add(Component.literal("  UUID: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(player.getUUID().toString())
                                                        .withStyle(ChatFormatting.DARK_GRAY)));
                        debugLines.add(Component.literal("  Race ID: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(vars.getRace().toString())
                                                        .withStyle(ChatFormatting.AQUA)));
                        lineMetadata.add(new LineMetadata(debugLines.size() - 1, "race", "race",
                                        vars.getRace().toString()));
                        String raceName = race != null ? race.name().getString() : "None";
                        debugLines.add(Component.literal("  Race Name: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(raceName).withStyle(ChatFormatting.AQUA)));
                        List<ResourceLocation> parents = race != null ? race.parentRaces()
                                        : java.util.Collections.emptyList();
                        String parentStr = parents.isEmpty() ? "None"
                                        : parents.stream()
                                                        .map(ResourceLocation::toString)
                                                        .collect(java.util.stream.Collectors.joining(", "));
                        debugLines.add(Component.literal("  Parent: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(parentStr).withStyle(ChatFormatting.DARK_AQUA)));
                        debugLines.add(Component.literal("  Chosen: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(String.valueOf(vars.hasChosenRace()))
                                                        .withStyle(vars.hasChosenRace() ? ChatFormatting.GREEN
                                                                        : ChatFormatting.RED)));

                        String teamName = vars.getTeamId() != null ? vars.getTeamName() : "None";
                        String teamIdStr = vars.getTeamId() != null
                                        ? vars.getTeamId().toString().substring(0, 8) + "..."
                                        : "None";
                        debugLines.add(Component.literal("  Team: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(teamName).withStyle(ChatFormatting.WHITE))
                                        .append(Component.literal(" (").withStyle(ChatFormatting.GRAY))
                                        .append(Component.literal(teamIdStr).withStyle(ChatFormatting.DARK_GRAY))
                                        .append(Component.literal(")").withStyle(ChatFormatting.GRAY)));
                        lineMetadata.add(new LineMetadata(debugLines.size() - 1, "variable", "teamname",
                                        vars.getTeamName()));

                        if (race != null) {
                                debugLines.add(Component.literal("  Base Ratios: ").withStyle(ChatFormatting.GRAY)
                                                .append(Component.literal("AP: " + race.baseAp())
                                                                .withStyle(ChatFormatting.GOLD))
                                                .append(Component.literal(" AD: " + race.baseAd())
                                                                .withStyle(ChatFormatting.GOLD))
                                                .append(Component.literal(" AH: " + race.baseAh())
                                                                .withStyle(ChatFormatting.GOLD))
                                                .append(Component.literal(" CR: " + race.baseCr())
                                                                .withStyle(ChatFormatting.GOLD)));
                        } else {
                                debugLines.add(Component.literal("  Base Ratios: ").withStyle(ChatFormatting.GRAY)
                                                .append(Component.literal("None").withStyle(ChatFormatting.DARK_GRAY)));
                        }

                        // --- STATISTICS ---
                        addHeader("STATISTICS");
                        double adBase = player
                                        .getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
                                        .getBaseValue();
                        debugLines.add(Component.literal("  AD: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(String.format("%.1f", vars.getAd()))
                                                        .withStyle(ChatFormatting.GREEN))
                                        .append(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
                                        .append(Component.literal(String.format("%.1f", adBase))
                                                        .withStyle(ChatFormatting.GREEN)));
                        lineMetadata.add(new LineMetadata(debugLines.size() - 1, "variable", "ad",
                                        String.valueOf(vars.getAd())));

                        double apBase = player.getAttribute(mc.sayda.creraces.registry.ModAttributes
                                        .resolve(mc.sayda.creraces.registry.ModAttributes.ABILITY_POWER))
                                        .getBaseValue();
                        debugLines.add(Component.literal("  AP: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(String.format("%.1f", vars.getAp()))
                                                        .withStyle(ChatFormatting.GREEN))
                                        .append(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
                                        .append(Component.literal(String.format("%.1f", apBase))
                                                        .withStyle(ChatFormatting.GREEN)));
                        lineMetadata.add(new LineMetadata(debugLines.size() - 1, "variable", "ap",
                                        String.valueOf(vars.getAp())));

                        debugLines.add(Component.literal("  Haste: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(String.format("%.1f%%", vars.getAh()))
                                                        .withStyle(ChatFormatting.GREEN)));
                        lineMetadata.add(new LineMetadata(debugLines.size() - 1, "variable", "ah",
                                        String.valueOf(vars.getAh())));

                        debugLines.add(Component.literal("  Crit: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(String.format("%.1f%%", vars.getCr()))
                                                        .withStyle(ChatFormatting.GREEN)));
                        lineMetadata.add(new LineMetadata(debugLines.size() - 1, "variable", "cr",
                                        String.valueOf(vars.getCr())));

                        // Resistances (Consolidated into STATISTICS)
                        double armor = player.getArmorValue();
                        double armorBase = player
                                        .getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)
                                        .getBaseValue();
                        debugLines.add(Component.literal("  Armor: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(String.format("%.1f", armor))
                                                        .withStyle(ChatFormatting.GREEN))
                                        .append(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
                                        .append(Component.literal(String.format("%.1f", armorBase))
                                                        .withStyle(ChatFormatting.GREEN)));
                        lineMetadata.add(new LineMetadata(debugLines.size() - 1, "attribute",
                                        "minecraft:generic.armor", String.valueOf(armorBase)));

                        double armPierce = player.getAttributeValue(
                                        mc.sayda.creraces.registry.ModAttributes.resolve(
                                                        mc.sayda.creraces.registry.ModAttributes.ARMOR_PIERCE));
                        double armShred = player.getAttributeValue(
                                        mc.sayda.creraces.registry.ModAttributes
                                                        .resolve(mc.sayda.creraces.registry.ModAttributes.ARMOR_SHRED));
                        debugLines.add(Component.literal("  ArmorPen: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(String.format("%.1f", armPierce))
                                                        .withStyle(ChatFormatting.GREEN))
                                        .append(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
                                        .append(Component.literal(String.format("%.1f%%", armShred * 100))
                                                        .withStyle(ChatFormatting.GREEN)));
                        lineMetadata.add(new LineMetadata(debugLines.size() - 1, "attribute",
                                        "creraces:armor_pierce", String.valueOf(armPierce)));

                        double magResist = player.getAttributeValue(
                                        mc.sayda.creraces.registry.ModAttributes.resolve(
                                                        mc.sayda.creraces.registry.ModAttributes.MAGIC_RESIST));
                        double magResistBase = player.getAttribute(
                                        mc.sayda.creraces.registry.ModAttributes.resolve(
                                                        mc.sayda.creraces.registry.ModAttributes.MAGIC_RESIST))
                                        .getBaseValue();
                        debugLines.add(Component.literal("  MR: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(String.format("%.1f", magResist))
                                                        .withStyle(ChatFormatting.GREEN))
                                        .append(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
                                        .append(Component.literal(String.format("%.1f", magResistBase))
                                                        .withStyle(ChatFormatting.GREEN)));
                        lineMetadata.add(new LineMetadata(debugLines.size() - 1, "attribute",
                                        "creraces:magic_resist", String.valueOf(magResistBase)));

                        double magPierce = player.getAttributeValue(
                                        mc.sayda.creraces.registry.ModAttributes.resolve(
                                                        mc.sayda.creraces.registry.ModAttributes.MAGIC_PIERCE));
                        double magShred = player.getAttributeValue(
                                        mc.sayda.creraces.registry.ModAttributes
                                                        .resolve(mc.sayda.creraces.registry.ModAttributes.MAGIC_SHRED));
                        debugLines.add(Component.literal("  MagicPen: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(String.format("%.1f", magPierce))
                                                        .withStyle(ChatFormatting.GREEN))
                                        .append(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
                                        .append(Component.literal(String.format("%.1f%%", magShred * 100))
                                                        .withStyle(ChatFormatting.GREEN)));
                        lineMetadata.add(new LineMetadata(debugLines.size() - 1, "attribute",
                                        "creraces:magic_pierce", String.valueOf(magPierce)));

                        double healing = player.getAttributeValue(
                                        mc.sayda.creraces.registry.ModAttributes.resolve(
                                                        mc.sayda.creraces.registry.ModAttributes.HEALING_RECEIVED));
                        double healingBase = player.getAttribute(
                                        mc.sayda.creraces.registry.ModAttributes.resolve(
                                                        mc.sayda.creraces.registry.ModAttributes.HEALING_RECEIVED))
                                        .getBaseValue();
                        debugLines.add(Component.literal("  Heal: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(String.format("%.1f%%", healing * 100))
                                                        .withStyle(ChatFormatting.GREEN))
                                        .append(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
                                        .append(Component.literal(String.format("%.1f%%", healingBase * 100))
                                                        .withStyle(ChatFormatting.GREEN)));
                        lineMetadata.add(new LineMetadata(debugLines.size() - 1, "attribute",
                                        "creraces:healing_received", String.valueOf(healingBase)));

                        // --- RESOURCES ---
                        addHeader("RESOURCES");
                        long threshold = mc.sayda.creraces.config.CreRacesConfig.RESOURCE_DECAY_GRACE_PERIOD.get();
                        long timerAge = Math.min(player.level().getGameTime() - vars.getResourceTimer(), threshold);
                        ChatFormatting timerColor = timerAge < threshold ? ChatFormatting.GREEN : ChatFormatting.RED;
                        debugLines.add(Component.literal("  Resource Timer: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(timerAge + " / " + threshold)
                                                        .withStyle(timerColor)));

                        debugLines.add(Component.literal("  Mana: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(String.format("%.1f", vars.getMana()))
                                                        .withStyle(ChatFormatting.BLUE)));
                        lineMetadata.add(new LineMetadata(debugLines.size() - 1, "variable", "mana",
                                        String.valueOf(vars.getMana())));

                        debugLines.add(Component.literal("  Rage: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(String.format("%.1f", vars.getRage()))
                                                        .withStyle(ChatFormatting.RED)));
                        lineMetadata.add(new LineMetadata(debugLines.size() - 1, "variable", "rage",
                                        String.valueOf(vars.getRage())));

                        debugLines.add(Component.literal("  Energy: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(String.format("%.1f", vars.getEnergy()))
                                                        .withStyle(ChatFormatting.YELLOW)));
                        lineMetadata.add(new LineMetadata(debugLines.size() - 1, "variable", "energy",
                                        String.valueOf(vars.getEnergy())));

                        debugLines.add(Component.literal("  Grit: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(String.format("%.1f", vars.getGrit()))
                                                        .withStyle(ChatFormatting.WHITE)));
                        lineMetadata.add(new LineMetadata(debugLines.size() - 1, "variable", "grit",
                                        String.valueOf(vars.getGrit())));

                        debugLines.add(Component.literal("  Soul: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component
                                                        .literal(String.format("%.0f/%.0f", vars.getSoul(),
                                                                        mc.sayda.creraces.config.CreRacesConfig.MAX_SOUL
                                                                                        .get()))
                                                        .withStyle(ChatFormatting.DARK_PURPLE)));
                        lineMetadata.add(new LineMetadata(debugLines.size() - 1, "variable", "soul",
                                        String.valueOf(vars.getSoul())));

                        debugLines.add(Component.literal("  Karma: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(String.format("%.2f", vars.getKarma()))
                                                        .withStyle(ChatFormatting.GREEN)));
                        lineMetadata.add(new LineMetadata(debugLines.size() - 1, "variable", "karma",
                                        String.valueOf(vars.getKarma())));

                        debugLines.add(Component.literal("  Coins: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component
                                                        .literal(java.util.Objects.requireNonNull(
                                                                        String.format("%.0f", vars.getCoins())))
                                                        .withStyle(ChatFormatting.GREEN)));
                        lineMetadata.add(new LineMetadata(debugLines.size() - 1, "variable", "coins",
                                        String.valueOf(vars.getCoins())));

                        // --- GLOBAL ---
                        addHeader("GLOBAL");
                        long dayTime = player.level().getDayTime();
                        long dayCount = dayTime / 24000L;
                        boolean sm = mc.sayda.creraces.engine.WorldState.isSpiritMoon(player.level());
                        debugLines.add(Component.literal("  Time: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(String.valueOf(dayTime))
                                                        .withStyle(ChatFormatting.WHITE)));
                        debugLines.add(Component.literal("  Day: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(String.valueOf(dayCount))
                                                        .withStyle(ChatFormatting.WHITE)));
                        debugLines.add(Component.literal("  Spirit Moon: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(String.valueOf(sm))
                                                        .withStyle(sm ? ChatFormatting.GREEN : ChatFormatting.RED)));
                        debugLines.add(Component.literal("  Dimension: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(player.level().dimension().location().toString())
                                                        .withStyle(ChatFormatting.DARK_AQUA)));

                        // --- FLAGS ---
                        addHeader("FLAGS");
                        if (race != null) {
                                boolean effectiveUndead = vars.isUndead() || race.isUndead();
                                debugLines.add(Component.literal("    isUndead: ").withStyle(ChatFormatting.GRAY)
                                                .append(Component.literal(String.valueOf(effectiveUndead))
                                                                .withStyle(effectiveUndead ? ChatFormatting.GREEN
                                                                                : ChatFormatting.RED)));
                                lineMetadata.add(new LineMetadata(debugLines.size() - 1, "flag", "isUndead",
                                                String.valueOf(vars.isUndead())));

                                boolean effectiveAquatic = vars.isAquatic() || race.isAquatic();
                                debugLines.add(Component.literal("    isAquatic: ").withStyle(ChatFormatting.GRAY)
                                                .append(Component.literal(String.valueOf(effectiveAquatic))
                                                                .withStyle(effectiveAquatic ? ChatFormatting.GREEN
                                                                                : ChatFormatting.RED)));
                                lineMetadata.add(new LineMetadata(debugLines.size() - 1, "flag", "isAquatic",
                                                String.valueOf(vars.isAquatic())));

                                boolean effectiveSpirit = vars.isSpirit() || race.isSpirit();
                                debugLines.add(Component.literal("    isSpirit: ").withStyle(ChatFormatting.GRAY)
                                                .append(Component.literal(String.valueOf(effectiveSpirit))
                                                                .withStyle(effectiveSpirit ? ChatFormatting.GREEN
                                                                                : ChatFormatting.RED)));
                                lineMetadata.add(new LineMetadata(debugLines.size() - 1, "flag", "isSpirit",
                                                String.valueOf(vars.isSpirit())));

                                boolean effectiveTiny = vars.isTiny() || race.isTiny();
                                debugLines.add(Component.literal("    isTiny: ").withStyle(ChatFormatting.GRAY)
                                                .append(Component.literal(String.valueOf(effectiveTiny))
                                                                .withStyle(effectiveTiny ? ChatFormatting.GREEN
                                                                                : ChatFormatting.RED)));
                                lineMetadata.add(new LineMetadata(debugLines.size() - 1, "flag", "isTiny",
                                                String.valueOf(vars.isTiny())));
                        }

                        debugLines.add(Component.literal("    inSpirit: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(String.valueOf(vars.isInSpiritRealm()))
                                                        .withStyle(vars.isInSpiritRealm() ? ChatFormatting.GREEN
                                                                        : ChatFormatting.RED)));
                        lineMetadata.add(new LineMetadata(debugLines.size() - 1, "flag", "inSpirit",
                                        String.valueOf(vars.isInSpiritRealm())));

                        debugLines.add(Component.literal("    Morphed: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(String.valueOf(vars.isMorphed()))
                                                        .withStyle(vars.isMorphed() ? ChatFormatting.GREEN
                                                                        : ChatFormatting.RED)));
                        lineMetadata.add(new LineMetadata(debugLines.size() - 1, "flag", "morphed",
                                        String.valueOf(vars.isMorphed())));

                        debugLines.add(Component.literal("    smallBuild: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(String.valueOf(vars.isSmallBuild()))
                                                        .withStyle(vars.isSmallBuild() ? ChatFormatting.GREEN
                                                                        : ChatFormatting.RED)));
                        lineMetadata.add(new LineMetadata(debugLines.size() - 1, "flag", "smallBuild",
                                        String.valueOf(vars.isSmallBuild())));

                        debugLines.add(Component.literal("    gState: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(String.valueOf(vars.getGState()))
                                                        .withStyle(ChatFormatting.WHITE)));
                        lineMetadata.add(new LineMetadata(debugLines.size() - 1, "flag", "gstate",
                                        String.valueOf(vars.getGState())));

                        // --- ACTIVE ABILITY ---
                        addHeader("ACTIVE ABILITY");
                        if (vars.isAbilityActive() && vars.getActiveAbility() != null) {
                                debugLines.add(Component.literal("    ID: ").withStyle(ChatFormatting.GRAY)
                                                .append(Component.literal(vars.getActiveAbility().toString())
                                                                .withStyle(ChatFormatting.YELLOW)));
                                debugLines.add(Component.literal("    Duration: ").withStyle(ChatFormatting.GRAY)
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
                                debugLines.add(Component.literal("    - None").withStyle(ChatFormatting.DARK_GRAY));
                        }

                        // --- TRAIT TIMERS ---
                        addHeader("TRAIT TIMERS");
                        Map<ResourceLocation, Integer> timers = vars.getTraitTimers();
                        boolean hasActiveTimers = timers.values().stream().anyMatch(t -> t > 0);
                        if (hasActiveTimers) {
                                // Sort by ID for consistency
                                List<ResourceLocation> sortedIds = new ArrayList<>(timers.keySet());
                                sortedIds.sort(Comparator.comparing(ResourceLocation::toString));
                                for (ResourceLocation id : sortedIds) {
                                        int time = timers.get(id);
                                        if (time > 0) {
                                                debugLines.add(Component.literal("    " + id.getPath() + ": ")
                                                                .withStyle(ChatFormatting.GRAY)
                                                                .append(Component.literal(String.valueOf(time))
                                                                                .withStyle(ChatFormatting.WHITE)));
                                        }
                                }
                        } else {
                                debugLines.add(Component.literal("    - None").withStyle(ChatFormatting.DARK_GRAY));
                        }

                        // --- COOLDOWNS ---
                        addHeader("COOLDOWNS");
                        Map<ResourceLocation, Integer> cooldowns = vars.getCooldowns();
                        boolean hasActiveCooldowns = cooldowns.values().stream().anyMatch(t -> t > 0);
                        if (hasActiveCooldowns) {
                                List<ResourceLocation> sortedIds = new ArrayList<>(cooldowns.keySet());
                                sortedIds.sort(Comparator.comparing(ResourceLocation::toString));
                                for (ResourceLocation id : sortedIds) {
                                        int time = cooldowns.get(id);
                                        if (time > 0) {
                                                debugLines.add(Component.literal("    " + id.toString() + ": ")
                                                                .withStyle(ChatFormatting.GRAY)
                                                                .append(Component.literal(String.valueOf(time))
                                                                                .withStyle(ChatFormatting.WHITE)));
                                                lineMetadata.add(new LineMetadata(debugLines.size() - 1, "cooldown",
                                                                id.toString(), String.valueOf(time)));
                                        }
                                }
                        } else {
                                debugLines.add(Component.literal("    - None").withStyle(ChatFormatting.DARK_GRAY));
                        }

                        addHeader("DIMENSION RETURN");
                        debugLines.add(Component.literal("  Return Dim: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(vars.getReturnDim())
                                                        .withStyle(ChatFormatting.WHITE)));
                        lineMetadata.add(new LineMetadata(debugLines.size() - 1, "variable", "returndim",
                                        vars.getReturnDim()));

                        debugLines.add(Component.literal("  Return X: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(String.format("%.1f", vars.getReturnX()))
                                                        .withStyle(ChatFormatting.WHITE)));
                        lineMetadata.add(new LineMetadata(debugLines.size() - 1, "variable", "returnX",
                                        String.valueOf(vars.getReturnX())));

                        debugLines.add(Component.literal("  Return Y: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(String.format("%.1f", vars.getReturnY()))
                                                        .withStyle(ChatFormatting.WHITE)));
                        lineMetadata.add(new LineMetadata(debugLines.size() - 1, "variable", "returnY",
                                        String.valueOf(vars.getReturnY())));

                        debugLines.add(Component.literal("  Return Z: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(String.format("%.1f", vars.getReturnZ()))
                                                        .withStyle(ChatFormatting.WHITE)));
                        lineMetadata.add(new LineMetadata(debugLines.size() - 1, "variable", "returnZ",
                                        String.valueOf(vars.getReturnZ())));

                        addHeader("WORLD & POCKET");
                        debugLines.add(Component.literal("  Has Pocket: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(String.valueOf(vars.hasPocket()))
                                                        .withStyle(vars.hasPocket() ? ChatFormatting.GREEN
                                                                        : ChatFormatting.RED)));

                        debugLines.add(Component.literal("  In Pocket: ").withStyle(ChatFormatting.GRAY)
                                        .append(Component
                                                        .literal(String.valueOf(player.level().dimension().location()
                                                                        .getPath().contains("pocket")))
                                                        .withStyle(player.level().dimension().location().getPath()
                                                                        .contains("pocket") ? ChatFormatting.GREEN
                                                                                        : ChatFormatting.RED)));

                        if (vars.hasPocket()) {
                                debugLines.add(Component.literal("    Pocket X: ").withStyle(ChatFormatting.GRAY)
                                                .append(Component.literal(String.format("%.1f", vars.getPocketX()))
                                                                .withStyle(ChatFormatting.WHITE)));
                                lineMetadata.add(new LineMetadata(debugLines.size() - 1, "variable", "pocketx",
                                                String.valueOf(vars.getPocketX())));

                                debugLines.add(Component.literal("    Pocket Y: ").withStyle(ChatFormatting.GRAY)
                                                .append(Component.literal(String.format("%.1f", vars.getPocketY()))
                                                                .withStyle(ChatFormatting.WHITE)));
                                lineMetadata.add(new LineMetadata(debugLines.size() - 1, "variable", "pockety",
                                                String.valueOf(vars.getPocketY())));

                                debugLines.add(Component.literal("    Pocket Z: ").withStyle(ChatFormatting.GRAY)
                                                .append(Component.literal(String.format("%.1f", vars.getPocketZ()))
                                                                .withStyle(ChatFormatting.WHITE)));
                                lineMetadata.add(new LineMetadata(debugLines.size() - 1, "variable", "pocketz",
                                                String.valueOf(vars.getPocketZ())));

                                double maxSize = CreRacesConfig.POCKET_EXPANSION_LIMIT.get();
                                debugLines.add(Component.literal("    Size: ").withStyle(ChatFormatting.GRAY)
                                                .append(Component
                                                                .literal(String.format("%.1f / %.1f",
                                                                                vars.getPocketSize(), maxSize))
                                                                .withStyle(ChatFormatting.WHITE)));
                                lineMetadata.add(new LineMetadata(debugLines.size() - 1, "variable", "pocketsize",
                                                String.valueOf(vars.getPocketSize())));

                                debugLines.add(Component.literal("    Host Spawn: ").withStyle(ChatFormatting.GRAY)
                                                .append(Component.literal(String.format("%.1f, %.1f, %.1f",
                                                                vars.getPocketSpawnX(),
                                                                vars.getPocketSpawnY(), vars.getPocketSpawnZ()))
                                                                .withStyle(ChatFormatting.YELLOW)));

                                Set<java.util.UUID> invites = vars.getPocketInvitations();
                                if (!invites.isEmpty()) {
                                        debugLines.add(Component.literal("    Invites: ").withStyle(ChatFormatting.GRAY)
                                                        .append(Component.literal(String.valueOf(invites.size()))
                                                                        .withStyle(ChatFormatting.WHITE)));
                                }
                        }

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
                                List<String> sortedKeys = new ArrayList<>(cust.keySet());
                                Collections.sort(sortedKeys);
                                for (String k : sortedKeys) {
                                        String v = cust.get(k);
                                        debugLines.add(Component.literal("  - " + k + ": ")
                                                        .withStyle(ChatFormatting.GRAY)
                                                        .append(Component.literal(v)
                                                                        .withStyle(ChatFormatting.LIGHT_PURPLE)));
                                        lineMetadata.add(
                                                        new LineMetadata(debugLines.size() - 1, "customization", k, v));
                                }
                        }

                        // --- ABILITY STATES (Dynamic) ---
                        addHeader("ABILITY STATES");
                        CompoundTag nbt = vars.serialize();
                        CompoundTag states = nbt.getCompound("abilityStates");
                        if (states.isEmpty()) {
                                debugLines.add(Component.literal("  - None").withStyle(ChatFormatting.DARK_GRAY));
                        } else {
                                List<String> sortedKeys = new ArrayList<>(states.getAllKeys());
                                Collections.sort(sortedKeys);
                                for (String key : sortedKeys) {
                                        double stateValue = states.getDouble(key);
                                        debugLines.add(Component.literal("  " + key + ": ")
                                                        .withStyle(ChatFormatting.GRAY)
                                                        .append(Component.literal(String.format("%.2f", stateValue))
                                                                        .withStyle(ChatFormatting.AQUA)));
                                        lineMetadata.add(new LineMetadata(debugLines.size() - 1, "ability_state", key,
                                                        String.valueOf(stateValue)));
                                }
                        }

                        // --- ABILITIES ---
                        addHeader("ABILITIES");
                        debugLines.add(Component.literal("    Unlocked:").withStyle(ChatFormatting.GRAY));
                        Set<ResourceLocation> unlocked = vars.getUnlockedAbilities();
                        if (unlocked.isEmpty()) {
                                debugLines.add(Component.literal("    - None").withStyle(ChatFormatting.DARK_GRAY));
                        } else {
                                List<ResourceLocation> sortedUnlocked = new ArrayList<>(unlocked);
                                sortedUnlocked.sort(Comparator.comparing(ResourceLocation::toString));
                                for (ResourceLocation ability : sortedUnlocked) {
                                        debugLines.add(Component.literal("    - " + ability.toString())
                                                        .withStyle(ChatFormatting.YELLOW));
                                }
                        }
                        debugLines.add(Component.literal(""));
                });
        }

        private void addHeader(String title) {
                if (!debugLines.isEmpty() && !debugLines.get(debugLines.size() - 1).getString().trim().isEmpty()) {
                        debugLines.add(Component.literal(" ")); // Spacer
                }
                debugLines.add(Component.literal("[ " + title + " ]").withStyle(ChatFormatting.BOLD,
                                ChatFormatting.GOLD));
        }

        @Override
        @SuppressWarnings("null")
        public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                long currentTick = minecraft != null && minecraft.level != null ? minecraft.level.getGameTime() : 0;
                if (currentTick != lastRefreshTick) {
                        lastRefreshTick = currentTick;
                        refreshDebugInfo();
                }
                this.renderBackground(graphics);

                graphics.drawCenteredString(Objects.requireNonNull(this.font), this.title, this.width / 2, 8, 0xFFD700);

                int listWidth = Math.min(this.width - 40, 400);
                int listLeft = (this.width - listWidth) / 2;
                int listTop = 25;
                int listBottom = this.height - 45;

                // Background panel for the list
                graphics.fill(listLeft, listTop, listLeft + listWidth, listBottom, 0x88000000);
                graphics.renderOutline(listLeft, listTop, listWidth, listBottom - listTop, 0xFFAAAAAA);

                // Calculations for clipping and scrolling
                int totalHeight = debugLines.size() * LINE_HEIGHT;
                maxScroll = Math.max(0, totalHeight - (listBottom - listTop));
                scrollAmount = Mth.clamp(scrollAmount, 0, maxScroll);

                graphics.enableScissor(listLeft, listTop, listLeft + listWidth, listBottom);
                PoseStack pose = graphics.pose();
                pose.pushPose();

                int startLine = (int) (scrollAmount / LINE_HEIGHT);
                int endLine = Math.min(debugLines.size() - 1, startLine + (listBottom - listTop) / LINE_HEIGHT + 1);

                for (int i = startLine; i <= endLine; i++) {
                        Component line = debugLines.get(i);
                        int currentY = listTop + (i * LINE_HEIGHT) - (int) scrollAmount;

                        // Hover highlight
                        if (mouseX >= listLeft && mouseX <= listLeft + listWidth && mouseY >= currentY
                                        && mouseY < currentY + LINE_HEIGHT && mouseY >= listTop
                                        && mouseY <= listBottom) {
                                graphics.fill(listLeft, currentY, listLeft + listWidth, currentY + LINE_HEIGHT,
                                                0x44FFFFFF);
                        }

                        // Selection highlight
                        for (LineMetadata meta : lineMetadata) {
                                if (meta.index == i && selectedLine == meta) {
                                        graphics.fill(listLeft, currentY, listLeft + listWidth, currentY + LINE_HEIGHT,
                                                        0x66FFFFFF);
                                        break;
                                }
                        }

                        graphics.drawString(this.font, line, listLeft + 5, currentY + (LINE_HEIGHT - 9) / 2, 0xFFFFFF);
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

                if (selectedLine != null) {
                        graphics.pose().pushPose();
                        graphics.pose().translate(0, 0, 100);

                        // Editor background (Tighter fit)
                        graphics.fill(this.width / 2 - 110, this.height - 85, this.width / 2 + 110, this.height - 38,
                                        0xFF000000);
                        graphics.renderOutline(this.width / 2 - 110, this.height - 85, 220, 47, 0xFFAAAAAA);

                        String labelStr = "Edit " + selectedLine.key + ":";
                        graphics.drawString(this.font, labelStr, this.width / 2 - 100, this.height - 78, 0xFFD700);

                        // Render widgets inside the Z-translation block so they are on top of the black
                        // box
                        super.render(graphics, mouseX, mouseY, partialTick);

                        graphics.pose().popPose();
                } else {
                        super.render(graphics, mouseX, mouseY, partialTick);
                }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
                // If editor is visible, check if click is inside editor bounds
                if (selectedLine != null) {
                        if (mouseX >= this.width / 2 - 110 && mouseX <= this.width / 2 + 110
                                        && mouseY >= this.height - 85 && mouseY <= this.height - 38) {
                                return super.mouseClicked(mouseX, mouseY, button);
                        }
                }

                int listWidth = Math.min(this.width - 40, 400);
                int listLeft = (this.width - listWidth) / 2;
                int listTop = 25;
                int listBottom = this.height - 45;

                if (mouseX >= listLeft && mouseX <= listLeft + listWidth && mouseY >= listTop && mouseY <= listBottom) {
                        if (minecraft != null && minecraft.player != null && minecraft.player.hasPermissions(2)) {
                                double clickedY = mouseY - listTop + scrollAmount;
                                int entryIndex = (int) (clickedY / LINE_HEIGHT);

                                // Find metadata for this index
                                for (LineMetadata meta : lineMetadata) {
                                        if (meta.index == entryIndex) {
                                                selectedLine = meta;
                                                editBox.visible = true;
                                                applyButton.visible = true;
                                                cancelButton.visible = true;
                                                editBox.setValue(java.util.Objects.requireNonNull(meta.currentValue));
                                                editBox.setFocused(true);
                                                this.setFocused(editBox);
                                                editBox.setCursorPosition(editBox.getValue().length());
                                                return true;
                                        }
                                }
                        }

                        cancelEdit();
                }

                return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (selectedLine != null) {
                        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
                                applyEdit();
                                return true;
                        }
                        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                                cancelEdit();
                                return true;
                        }
                }
                return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean charTyped(char codePoint, int modifiers) {
                if (selectedLine != null && editBox.charTyped(codePoint, modifiers)) {
                        return true;
                }
                return super.charTyped(codePoint, modifiers);
        }

        private static class LineMetadata {
                final int index;
                final String action;
                final String key;
                final String currentValue;

                LineMetadata(int index, String action, String key, String currentValue) {
                        this.index = index;
                        this.action = action;
                        this.key = key;
                        this.currentValue = currentValue;
                }
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
                this.scrollAmount -= delta * 20; // Increased sensitivity slightly
                return true;
        }

        @Override
        public boolean isPauseScreen() {
                return false;
        }
}
