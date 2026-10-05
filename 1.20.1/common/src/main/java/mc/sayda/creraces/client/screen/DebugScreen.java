package mc.sayda.creraces.client.screen;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.engine.WorldState;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import mc.sayda.creraces.registry.ModAttributes;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Live dump of the local player's race data, refreshed every game tick. Operators (permission
 * level 2) can click an editable line to send a new value to the server.
 */
@SuppressWarnings("null")
public class DebugScreen extends Screen {
    private static final int LINE_HEIGHT = 12;
    private static final int LIST_TOP = 25;
    private static final int MAX_LIST_WIDTH = 400;
    private static final int SCROLL_SPEED = 20;
    // Value editor box, anchored to the bottom centre of the screen.
    private static final int EDITOR_HALF_WIDTH = 110;
    private static final int EDITOR_TOP_INSET = 85;
    private static final int EDITOR_BOTTOM_INSET = 38;

    private final List<Component> debugLines = new ArrayList<>();
    private final List<LineMetadata> lineMetadata = new ArrayList<>();
    private double scrollAmount;
    private LineMetadata selectedLine = null;
    private long lastRefreshTick = -1;
    private EditBox editBox;
    private Button applyButton;
    private Button cancelButton;

    public DebugScreen() {
        super(Component.translatable("gui.creraces.debug.title"));
    }

    @Override
    protected void init() {
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onClose())
                .bounds(this.width / 2 - 100, this.height - 30, 98, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Copy"), button -> copyToClipboard())
                .bounds(this.width / 2 + 2, this.height - 30, 98, 20).build());

        // The edit box and its buttons sit centred as a group, shown only while a line is selected.
        editBox = new EditBox(this.font, this.width / 2 - 100, this.height - 60, 100, 16, Component.empty());
        applyButton = Button.builder(Component.literal("Apply"), b -> applyEdit())
                .bounds(this.width / 2 + 5, this.height - 60, 45, 16).build();
        cancelButton = Button.builder(Component.literal("Cancel"), b -> cancelEdit())
                .bounds(this.width / 2 + 55, this.height - 60, 45, 16).build();
        this.addRenderableWidget(editBox);
        this.addRenderableWidget(applyButton);
        this.addRenderableWidget(cancelButton);
        setEditorVisible(false);

        refreshDebugInfo();
    }

    private void copyToClipboard() {
        StringBuilder sb = new StringBuilder();
        for (Component line : debugLines) {
            sb.append(line.getString()).append("\n");
        }
        Minecraft.getInstance().keyboardHandler.setClipboard(sb.toString());
    }

    private void setEditorVisible(boolean visible) {
        editBox.visible = visible;
        applyButton.visible = visible;
        cancelButton.visible = visible;
    }

    private void startEdit(LineMetadata line) {
        selectedLine = line;
        setEditorVisible(true);
        editBox.setValue(Objects.requireNonNull(line.currentValue()));
        editBox.setFocused(true);
        this.setFocused(editBox);
        editBox.setCursorPosition(editBox.getValue().length());
    }

    private void applyEdit() {
        if (selectedLine != null && !editBox.getValue().isEmpty()) {
            BoundaryHandler.sendDebugAction(selectedLine.action(), selectedLine.key(), editBox.getValue());
            cancelEdit();
            refreshDebugInfo();
        }
    }

    private void cancelEdit() {
        selectedLine = null;
        setEditorVisible(false);
        this.setFocused(null);
    }

    private void refreshDebugInfo() {
        debugLines.clear();
        lineMetadata.clear();
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        DataUtils.getVariables(player).ifPresent(vars -> {
            Race race = RaceRegistry.get(vars.getRace());
            debugLines.add(Component.literal(""));
            addIdentitySection(player, vars, race);
            addStatisticsSection(player, vars);
            addResourcesSection(player, vars);
            addGlobalSection(player);
            addFlagsSection(vars, race);
            addActiveAbilitySection(vars);
            addTraitTimersSection(vars);
            addCooldownsSection(vars);
            addDimensionReturnSection(vars);
            addPocketSection(player, vars);

            addHeader("INTERNAL");
            addLine("  Passive CD: ", String.format("%.0f", vars.getPassiveCooldown()), ChatFormatting.WHITE);

            addCustomizationsSection(vars);
            addAbilityStatesSection(vars);
            addAbilitiesSection(vars);
            debugLines.add(Component.literal(""));
        });
    }

    private void addIdentitySection(Player player, IPlayerVariables vars, Race race) {
        addHeader("IDENTITY");
        addLine("  Player: ", player.getName().getString(), ChatFormatting.WHITE);
        addLine("  UUID: ", player.getUUID().toString(), ChatFormatting.DARK_GRAY);
        addEditableLine("  Race ID: ", vars.getRace().toString(), ChatFormatting.AQUA,
                "race", "race", vars.getRace().toString());
        addLine("  Race Name: ", race != null ? race.name().getString() : "None", ChatFormatting.AQUA);

        List<ResourceLocation> parents = race != null ? race.parentRaces() : Collections.emptyList();
        String parentStr = parents.isEmpty() ? "None"
                : parents.stream().map(ResourceLocation::toString).collect(Collectors.joining(", "));
        addLine("  Parent: ", parentStr, ChatFormatting.DARK_AQUA);
        addFlagLine("  Chosen: ", vars.hasChosenRace());

        UUID teamId = vars.getTeamId();
        String teamName = teamId != null ? vars.getTeamName() : "None";
        String teamIdStr = teamId != null ? teamId.toString().substring(0, 8) + "..." : "None";
        debugLines.add(labelled("  Team: ", teamName, ChatFormatting.WHITE)
                .append(Component.literal(" (").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(teamIdStr).withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(")").withStyle(ChatFormatting.GRAY)));
        markEditable("variable", "teamname", vars.getTeamName());

        if (race != null) {
            debugLines.add(labelled("  Base Ratios: ", "AP: " + race.baseAp(), ChatFormatting.GOLD)
                    .append(Component.literal(" AD: " + race.baseAd()).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(" AH: " + race.baseAh()).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(" CR: " + race.baseCr()).withStyle(ChatFormatting.GOLD)));
        } else {
            addLine("  Base Ratios: ", "None", ChatFormatting.DARK_GRAY);
        }
    }

    private void addStatisticsSection(Player player, IPlayerVariables vars) {
        addHeader("STATISTICS");
        double adBase = player.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue();
        addCurrentAndBase("  AD: ", String.format("%.1f", vars.getAd()), String.format("%.1f", adBase));
        markEditable("variable", "ad", String.valueOf(vars.getAd()));

        double apBase = player.getAttribute(ModAttributes.resolve(ModAttributes.ABILITY_POWER)).getBaseValue();
        addCurrentAndBase("  AP: ", String.format("%.1f", vars.getAp()), String.format("%.1f", apBase));
        markEditable("variable", "ap", String.valueOf(vars.getAp()));

        addEditableLine("  Haste: ", String.format("%.1f%%", vars.getAh()), ChatFormatting.GREEN,
                "variable", "ah", String.valueOf(vars.getAh()));
        addEditableLine("  Crit: ", String.format("%.1f%%", vars.getCr()), ChatFormatting.GREEN,
                "variable", "cr", String.valueOf(vars.getCr()));

        double armor = player.getArmorValue();
        double armorBase = player.getAttribute(Attributes.ARMOR).getBaseValue();
        addCurrentAndBase("  Armor: ", String.format("%.1f", armor), String.format("%.1f", armorBase));
        markEditable("attribute", "minecraft:generic.armor", String.valueOf(armorBase));

        double armorPierce = player.getAttributeValue(ModAttributes.resolve(ModAttributes.ARMOR_PIERCE));
        double armorShred = player.getAttributeValue(ModAttributes.resolve(ModAttributes.ARMOR_SHRED));
        addCurrentAndBase("  ArmorPen: ", String.format("%.1f", armorPierce),
                String.format("%.1f%%", armorShred * 100));
        markEditable("attribute", "creraces:armor_pierce", String.valueOf(armorPierce));

        double magicResist = player.getAttributeValue(ModAttributes.resolve(ModAttributes.MAGIC_RESIST));
        double magicResistBase = player.getAttribute(ModAttributes.resolve(ModAttributes.MAGIC_RESIST))
                .getBaseValue();
        addCurrentAndBase("  MR: ", String.format("%.1f", magicResist), String.format("%.1f", magicResistBase));
        markEditable("attribute", "creraces:magic_resist", String.valueOf(magicResistBase));

        double magicPierce = player.getAttributeValue(ModAttributes.resolve(ModAttributes.MAGIC_PIERCE));
        double magicShred = player.getAttributeValue(ModAttributes.resolve(ModAttributes.MAGIC_SHRED));
        addCurrentAndBase("  MagicPen: ", String.format("%.1f", magicPierce),
                String.format("%.1f%%", magicShred * 100));
        markEditable("attribute", "creraces:magic_pierce", String.valueOf(magicPierce));

        double healing = player.getAttributeValue(ModAttributes.resolve(ModAttributes.HEALING_RECEIVED));
        double healingBase = player.getAttribute(ModAttributes.resolve(ModAttributes.HEALING_RECEIVED))
                .getBaseValue();
        addCurrentAndBase("  Heal: ", String.format("%.1f%%", healing * 100),
                String.format("%.1f%%", healingBase * 100));
        markEditable("attribute", "creraces:healing_received", String.valueOf(healingBase));
    }

    private void addResourcesSection(Player player, IPlayerVariables vars) {
        addHeader("RESOURCES");
        long threshold = CreRacesConfig.RESOURCE_DECAY_GRACE_PERIOD.get();
        long timerAge = Math.min(player.level().getGameTime() - vars.getResourceTimer(), threshold);
        addLine("  Resource Timer: ", timerAge + " / " + threshold,
                timerAge < threshold ? ChatFormatting.GREEN : ChatFormatting.RED);

        addEditableLine("  Mana: ", String.format("%.1f", vars.getMana()), ChatFormatting.BLUE,
                "variable", "mana", String.valueOf(vars.getMana()));
        addEditableLine("  Rage: ", String.format("%.1f", vars.getRage()), ChatFormatting.RED,
                "variable", "rage", String.valueOf(vars.getRage()));
        addEditableLine("  Energy: ", String.format("%.1f", vars.getEnergy()), ChatFormatting.YELLOW,
                "variable", "energy", String.valueOf(vars.getEnergy()));
        addEditableLine("  Grit: ", String.format("%.1f", vars.getGrit()), ChatFormatting.WHITE,
                "variable", "grit", String.valueOf(vars.getGrit()));
        addEditableLine("  Soul: ", String.format("%.0f/%.0f", vars.getSoul(), CreRacesConfig.MAX_SOUL.get()),
                ChatFormatting.DARK_PURPLE, "variable", "soul", String.valueOf(vars.getSoul()));
        addEditableLine("  Karma: ", String.format("%.2f", vars.getKarma()), ChatFormatting.GREEN,
                "variable", "karma", String.valueOf(vars.getKarma()));
        addEditableLine("  Coins: ", String.format("%.0f", vars.getCoins()), ChatFormatting.GOLD,
                "variable", "coins", String.valueOf(vars.getCoins()));
    }

    private void addGlobalSection(Player player) {
        addHeader("GLOBAL");
        long dayTime = player.level().getDayTime();
        addLine("  Time: ", String.valueOf(dayTime), ChatFormatting.WHITE);
        addLine("  Day: ", String.valueOf(dayTime / 24000L), ChatFormatting.WHITE);
        addFlagLine("  Spirit Moon: ", WorldState.isSpiritMoon(player.level()));
        addLine("  Dimension: ", player.level().dimension().location().toString(), ChatFormatting.DARK_AQUA);
    }

    /** Race flags show the effective value (player or race), but editing sets the player's own flag. */
    private void addFlagsSection(IPlayerVariables vars, Race race) {
        addHeader("FLAGS");
        if (race != null) {
            addFlagLine("    isUndead: ", vars.isUndead() || race.isUndead());
            markEditable("flag", "isUndead", String.valueOf(vars.isUndead()));
            addFlagLine("    isAquatic: ", vars.isAquatic() || race.isAquatic());
            markEditable("flag", "isAquatic", String.valueOf(vars.isAquatic()));
            addFlagLine("    isSpirit: ", vars.isSpirit() || race.isSpirit());
            markEditable("flag", "isSpirit", String.valueOf(vars.isSpirit()));
            addFlagLine("    isTiny: ", vars.isTiny() || race.isTiny());
            markEditable("flag", "isTiny", String.valueOf(vars.isTiny()));
        }

        addFlagLine("    inSpirit: ", vars.isInSpiritRealm());
        markEditable("flag", "inSpirit", String.valueOf(vars.isInSpiritRealm()));
        addFlagLine("    Morphed: ", vars.isMorphed());
        markEditable("flag", "morphed", String.valueOf(vars.isMorphed()));
        addFlagLine("    smallBuild: ", vars.isSmallBuild());
        markEditable("flag", "smallBuild", String.valueOf(vars.isSmallBuild()));
        addEditableLine("    gState: ", String.valueOf(vars.getGState()), ChatFormatting.WHITE,
                "flag", "gstate", String.valueOf(vars.getGState()));
    }

    private void addActiveAbilitySection(IPlayerVariables vars) {
        addHeader("ACTIVE ABILITY");
        if (!vars.isAbilityActive() || vars.getActiveAbility() == null) {
            addNone("    ");
            return;
        }
        addLine("    ID: ", vars.getActiveAbility().toString(), ChatFormatting.YELLOW);
        debugLines.add(labelled("    Duration: ", String.valueOf(vars.getActiveAbilityDuration()), ChatFormatting.WHITE)
                .append(Component.literal(" | Drain: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.format("%.2f", vars.getActiveAbilityDrain()))
                        .withStyle(ChatFormatting.WHITE)));
    }

    private void addTraitTimersSection(IPlayerVariables vars) {
        addHeader("TRAIT TIMERS");
        Map<ResourceLocation, Integer> timers = vars.getTraitTimers();
        List<ResourceLocation> running = runningTimers(timers);
        if (running.isEmpty()) {
            addNone("    ");
        }
        for (ResourceLocation id : running) {
            addLine("    " + id.getPath() + ": ", String.valueOf(timers.get(id)), ChatFormatting.WHITE);
        }
    }

    private void addCooldownsSection(IPlayerVariables vars) {
        addHeader("COOLDOWNS");
        Map<ResourceLocation, Integer> cooldowns = vars.getCooldowns();
        List<ResourceLocation> running = runningTimers(cooldowns);
        if (running.isEmpty()) {
            addNone("    ");
        }
        for (ResourceLocation id : running) {
            String time = String.valueOf(cooldowns.get(id));
            addEditableLine("    " + id + ": ", time, ChatFormatting.WHITE, "cooldown", id.toString(), time);
        }
    }

    /** Ids whose timer is still counting, sorted so the list does not reshuffle between refreshes. */
    private static List<ResourceLocation> runningTimers(Map<ResourceLocation, Integer> timers) {
        List<ResourceLocation> running = new ArrayList<>();
        for (Map.Entry<ResourceLocation, Integer> entry : timers.entrySet()) {
            if (entry.getValue() > 0) {
                running.add(entry.getKey());
            }
        }
        running.sort(Comparator.comparing(ResourceLocation::toString));
        return running;
    }

    private void addDimensionReturnSection(IPlayerVariables vars) {
        addHeader("DIMENSION RETURN");
        addEditableLine("  Return Dim: ", vars.getReturnDim(), ChatFormatting.WHITE,
                "variable", "returndim", vars.getReturnDim());
        addEditableLine("  Return X: ", String.format("%.1f", vars.getReturnX()), ChatFormatting.WHITE,
                "variable", "returnX", String.valueOf(vars.getReturnX()));
        addEditableLine("  Return Y: ", String.format("%.1f", vars.getReturnY()), ChatFormatting.WHITE,
                "variable", "returnY", String.valueOf(vars.getReturnY()));
        addEditableLine("  Return Z: ", String.format("%.1f", vars.getReturnZ()), ChatFormatting.WHITE,
                "variable", "returnZ", String.valueOf(vars.getReturnZ()));
    }

    private void addPocketSection(Player player, IPlayerVariables vars) {
        addHeader("WORLD & POCKET");
        addFlagLine("  Has Pocket: ", vars.hasPocket());
        addFlagLine("  In Pocket: ", player.level().dimension().location().getPath().contains("pocket"));
        if (!vars.hasPocket()) {
            return;
        }

        addEditableLine("    Pocket X: ", String.format("%.1f", vars.getPocketX()), ChatFormatting.WHITE,
                "variable", "pocketx", String.valueOf(vars.getPocketX()));
        addEditableLine("    Pocket Y: ", String.format("%.1f", vars.getPocketY()), ChatFormatting.WHITE,
                "variable", "pockety", String.valueOf(vars.getPocketY()));
        addEditableLine("    Pocket Z: ", String.format("%.1f", vars.getPocketZ()), ChatFormatting.WHITE,
                "variable", "pocketz", String.valueOf(vars.getPocketZ()));

        double maxSize = CreRacesConfig.POCKET_EXPANSION_LIMIT.get();
        addEditableLine("    Size: ", String.format("%.1f / %.1f", vars.getPocketSize(), maxSize),
                ChatFormatting.WHITE, "variable", "pocketsize", String.valueOf(vars.getPocketSize()));
        addLine("    Host Spawn: ", String.format("%.1f, %.1f, %.1f",
                vars.getPocketSpawnX(), vars.getPocketSpawnY(), vars.getPocketSpawnZ()), ChatFormatting.YELLOW);

        Set<UUID> invites = vars.getPocketInvitations();
        if (!invites.isEmpty()) {
            addLine("    Invites: ", String.valueOf(invites.size()), ChatFormatting.WHITE);
        }
    }

    private void addCustomizationsSection(IPlayerVariables vars) {
        addHeader("CUSTOMIZATIONS");
        Map<String, String> customizations = vars.getCustomizations();
        if (customizations.isEmpty()) {
            addNone("  ");
            return;
        }
        List<String> sortedKeys = new ArrayList<>(customizations.keySet());
        Collections.sort(sortedKeys);
        for (String key : sortedKeys) {
            String value = customizations.get(key);
            addEditableLine("  - " + key + ": ", value, ChatFormatting.LIGHT_PURPLE, "customization", key, value);
        }
    }

    /** Ability states only exist in the serialized form, so they are read back out of it. */
    private void addAbilityStatesSection(IPlayerVariables vars) {
        addHeader("ABILITY STATES");
        CompoundTag states = vars.serialize().getCompound("abilityStates");
        if (states.isEmpty()) {
            addNone("  ");
            return;
        }
        List<String> sortedKeys = new ArrayList<>(states.getAllKeys());
        Collections.sort(sortedKeys);
        for (String key : sortedKeys) {
            double stateValue = states.getDouble(key);
            addEditableLine("  " + key + ": ", String.format("%.2f", stateValue), ChatFormatting.AQUA,
                    "ability_state", key, String.valueOf(stateValue));
        }
    }

    private void addAbilitiesSection(IPlayerVariables vars) {
        addHeader("ABILITIES");
        debugLines.add(Component.literal("    Unlocked:").withStyle(ChatFormatting.GRAY));
        Set<ResourceLocation> unlocked = vars.getUnlockedAbilities();
        if (unlocked.isEmpty()) {
            addNone("    ");
            return;
        }
        List<ResourceLocation> sortedUnlocked = new ArrayList<>(unlocked);
        sortedUnlocked.sort(Comparator.comparing(ResourceLocation::toString));
        for (ResourceLocation ability : sortedUnlocked) {
            debugLines.add(Component.literal("    - " + ability).withStyle(ChatFormatting.YELLOW));
        }
    }

    private void addHeader(String title) {
        if (!debugLines.isEmpty() && !debugLines.get(debugLines.size() - 1).getString().trim().isEmpty()) {
            debugLines.add(Component.literal(" "));
        }
        debugLines.add(Component.literal("[ " + title + " ]").withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD));
    }

    private static MutableComponent labelled(String label, String value, ChatFormatting valueColor) {
        return Component.literal(label).withStyle(ChatFormatting.GRAY)
                .append(Component.literal(value).withStyle(valueColor));
    }

    private void addLine(String label, String value, ChatFormatting valueColor) {
        debugLines.add(labelled(label, value, valueColor));
    }

    private void addEditableLine(String label, String value, ChatFormatting valueColor,
            String action, String key, String currentValue) {
        addLine(label, value, valueColor);
        markEditable(action, key, currentValue);
    }

    /** Lets operators edit the line just added; action and key tell the server what to change. */
    private void markEditable(String action, String key, String currentValue) {
        lineMetadata.add(new LineMetadata(debugLines.size() - 1, action, key, currentValue));
    }

    private void addFlagLine(String label, boolean value) {
        addLine(label, String.valueOf(value), value ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    /** "label current / base" with both numbers in green. */
    private void addCurrentAndBase(String label, String current, String base) {
        debugLines.add(labelled(label, current, ChatFormatting.GREEN)
                .append(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(base).withStyle(ChatFormatting.GREEN)));
    }

    private void addNone(String indent) {
        debugLines.add(Component.literal(indent + "- None").withStyle(ChatFormatting.DARK_GRAY));
    }

    private int listWidth() {
        return Math.min(this.width - 40, MAX_LIST_WIDTH);
    }

    private int listLeft() {
        return (this.width - listWidth()) / 2;
    }

    private int listBottom() {
        return this.height - 45;
    }

    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        long currentTick = minecraft != null && minecraft.level != null ? minecraft.level.getGameTime() : 0;
        if (currentTick != lastRefreshTick) {
            lastRefreshTick = currentTick;
            refreshDebugInfo();
        }
        this.renderBackground(graphics);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFD700);
        renderLineList(graphics, mouseX, mouseY);

        if (selectedLine != null) {
            // The editor box is lifted to z=100 over the list, so the widgets are drawn inside the
            // same translation to stay on top of it.
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 100);
            renderEditorBox(graphics);
            super.render(graphics, mouseX, mouseY, partialTick);
            graphics.pose().popPose();
        } else {
            super.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private void renderLineList(GuiGraphics graphics, int mouseX, int mouseY) {
        int listWidth = listWidth();
        int listLeft = listLeft();
        int listBottom = listBottom();
        int listHeight = listBottom - LIST_TOP;

        graphics.fill(listLeft, LIST_TOP, listLeft + listWidth, listBottom, 0x88000000);
        graphics.renderOutline(listLeft, LIST_TOP, listWidth, listHeight, 0xFFAAAAAA);

        int totalHeight = debugLines.size() * LINE_HEIGHT;
        int maxScroll = Math.max(0, totalHeight - listHeight);
        scrollAmount = Mth.clamp(scrollAmount, 0, maxScroll);

        graphics.enableScissor(listLeft, LIST_TOP, listLeft + listWidth, listBottom);
        int firstLine = (int) (scrollAmount / LINE_HEIGHT);
        int lastLine = Math.min(debugLines.size() - 1, firstLine + listHeight / LINE_HEIGHT + 1);
        for (int i = firstLine; i <= lastLine; i++) {
            int lineY = LIST_TOP + (i * LINE_HEIGHT) - (int) scrollAmount;

            boolean hovered = mouseX >= listLeft && mouseX <= listLeft + listWidth && mouseY >= lineY
                    && mouseY < lineY + LINE_HEIGHT && mouseY >= LIST_TOP && mouseY <= listBottom;
            if (hovered) {
                graphics.fill(listLeft, lineY, listLeft + listWidth, lineY + LINE_HEIGHT, 0x44FFFFFF);
            }
            if (selectedLine != null && selectedLine.index() == i) {
                graphics.fill(listLeft, lineY, listLeft + listWidth, lineY + LINE_HEIGHT, 0x66FFFFFF);
            }

            graphics.drawString(this.font, debugLines.get(i), listLeft + 5, lineY + (LINE_HEIGHT - 9) / 2, 0xFFFFFF);
        }
        graphics.disableScissor();

        if (maxScroll > 0) {
            int scrollbarX = listLeft + listWidth + 2;
            int barHeight = Math.max(10, listHeight * listHeight / totalHeight);
            int barTop = LIST_TOP + (int) ((listHeight - barHeight) * (this.scrollAmount / maxScroll));
            graphics.fill(scrollbarX, barTop, scrollbarX + 3, barTop + barHeight, 0xAAFFFFFF);
        }
    }

    private void renderEditorBox(GuiGraphics graphics) {
        int left = this.width / 2 - EDITOR_HALF_WIDTH;
        int top = this.height - EDITOR_TOP_INSET;
        int bottom = this.height - EDITOR_BOTTOM_INSET;
        graphics.fill(left, top, this.width / 2 + EDITOR_HALF_WIDTH, bottom, 0xFF000000);
        graphics.renderOutline(left, top, EDITOR_HALF_WIDTH * 2, bottom - top, 0xFFAAAAAA);
        graphics.drawString(this.font, "Edit " + selectedLine.key() + ":", this.width / 2 - 100, this.height - 78,
                0xFFD700);
    }

    private boolean isOverEditor(double mouseX, double mouseY) {
        return mouseX >= this.width / 2 - EDITOR_HALF_WIDTH && mouseX <= this.width / 2 + EDITOR_HALF_WIDTH
                && mouseY >= this.height - EDITOR_TOP_INSET && mouseY <= this.height - EDITOR_BOTTOM_INSET;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (selectedLine != null && isOverEditor(mouseX, mouseY)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int listLeft = listLeft();
        if (mouseX >= listLeft && mouseX <= listLeft + listWidth() && mouseY >= LIST_TOP && mouseY <= listBottom()) {
            if (minecraft != null && minecraft.player != null && minecraft.player.hasPermissions(2)) {
                int entryIndex = (int) ((mouseY - LIST_TOP + scrollAmount) / LINE_HEIGHT);
                for (LineMetadata meta : lineMetadata) {
                    if (meta.index() == entryIndex) {
                        startEdit(meta);
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
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                applyEdit();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
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

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        this.scrollAmount -= delta * SCROLL_SPEED;
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Where an editable line sits in debugLines, and what the server should change when it is edited. */
    private record LineMetadata(int index, String action, String key, String currentValue) {
    }
}
