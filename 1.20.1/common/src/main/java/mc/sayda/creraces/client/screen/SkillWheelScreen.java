package mc.sayda.creraces.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import mc.sayda.creraces.ability.Ability;
import mc.sayda.creraces.ability.AbilityRegistry;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.client.AbilityIconRenderer;
import mc.sayda.creraces.client.ModKeyMappings;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.network.EquipAbilityPacket;
import mc.sayda.creraces.util.RemoteDocConfig;
import mc.sayda.creraces.util.RemoteDocFetcher;
import mc.sayda.creraces.util.WikiUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Every unlocked ability in a ring. Pressing an ability key while hovering one equips it in that
 * slot; clicking one with a wiki page opens the page.
 */
public class SkillWheelScreen extends Screen {
    private static final int WHEEL_RADIUS = 80;
    private static final int ITEM_SIZE = 24;

    private ResourceLocation hoveredAbility = null;

    public SkillWheelScreen() {
        super(Component.translatable("screen.creraces.skill_wheel"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        this.hoveredAbility = null;

        DataUtils.getVariables(Minecraft.getInstance().player).ifPresent(vars -> {
            Set<ResourceLocation> unlocked = vars.getUnlockedAbilities();
            if (unlocked.isEmpty()) {
                graphics.drawCenteredString(this.font, Component.translatable("screen.creraces.no_abilities"), centerX,
                        centerY, 0xFFFFFF);
                return;
            }

            List<ResourceLocation> abilities = new ArrayList<>(unlocked);
            double angleStep = 2 * Math.PI / abilities.size();
            for (int i = 0; i < abilities.size(); i++) {
                ResourceLocation id = abilities.get(i);
                Ability ability = AbilityRegistry.get(id);
                if (ability == null) {
                    continue;
                }

                double angle = i * angleStep - Math.PI / 2; // start from the top
                int x = centerX + (int) (WHEEL_RADIUS * Math.cos(angle)) - ITEM_SIZE / 2;
                int y = centerY + (int) (WHEEL_RADIUS * Math.sin(angle)) - ITEM_SIZE / 2;
                renderAbility(graphics, vars, id, ability, x, y, mouseX, mouseY);
            }
        });

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderAbility(GuiGraphics graphics, IPlayerVariables vars, ResourceLocation id, Ability ability,
            int x, int y, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + ITEM_SIZE && mouseY >= y && mouseY < y + ITEM_SIZE;
        if (hovered) {
            this.hoveredAbility = id;
            graphics.fill(x - 2, y - 2, x + ITEM_SIZE + 2, y + ITEM_SIZE + 2, 0x80FFFFFF);
            graphics.renderComponentTooltip(this.font, abilityTooltip(id, ability), mouseX, mouseY);
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // One key label per slot the ability is equipped in, stacked beside the icon.
        int labelY = y;
        for (AbilitySlot slot : AbilitySlot.values()) {
            if (id.equals(vars.getAbilityInSlot(slot))) {
                Component keyName = keyFor(slot).getTranslatedKeyMessage();
                if (keyName != null) {
                    graphics.drawCenteredString(this.font, keyName.getString(), x + ITEM_SIZE + 10, labelY,
                            slotColor(slot));
                }
                labelY += 10;
            }
        }

        AbilityIconRenderer.render(graphics, ability.icon(), x, y, ITEM_SIZE);

        int cooldown = vars.getCooldown(id);
        if (cooldown > 0) {
            float cooldownPercent = ability.cooldown() > 0
                    ? Math.min(1f, (float) cooldown / (float) ability.cooldown())
                    : 1f;
            // The shade shrinks from the top as the ability recharges.
            graphics.fill(x, y + (int) (ITEM_SIZE * (1 - cooldownPercent)), x + ITEM_SIZE, y + ITEM_SIZE,
                    0x80000000);
            String seconds = Objects.requireNonNull(String.valueOf(Math.max(1, cooldown / 20)));
            graphics.drawCenteredString(this.font, seconds, x + ITEM_SIZE / 2, y + (ITEM_SIZE / 2) - 4, 0xFFFFFF);
        }
    }

    private static List<Component> abilityTooltip(ResourceLocation id, Ability ability) {
        RemoteDocConfig config = AbilityRegistry.getRemoteDoc(id);
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(ability.name());
        tooltip.add(RemoteDocFetcher.getRemoteDescription(id, config, ability.description()));
        if (config != null) {
            tooltip.add(Component.literal(""));
            tooltip.add(Component.translatable("screen.creraces.click_for_wiki")
                    .withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
        }
        return tooltip;
    }

    private static KeyMapping keyFor(AbilitySlot slot) {
        return switch (slot) {
            case A1 -> ModKeyMappings.ABILITY_A1;
            case A2 -> ModKeyMappings.ABILITY_A2;
            case A3 -> ModKeyMappings.ABILITY_A3;
            case A4 -> ModKeyMappings.ABILITY_A4;
            case A5 -> ModKeyMappings.ABILITY_A5;
        };
    }

    private static int slotColor(AbilitySlot slot) {
        return switch (slot) {
            case A1 -> 0x55FF55; // green
            case A2 -> 0xFF5555; // red
            case A3 -> 0xFFFF55; // yellow
            case A4 -> 0x5555FF; // blue
            case A5 -> 0xFFAA00; // orange
        };
    }

    @Override
    @SuppressWarnings("null")
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.hoveredAbility != null
                && AbilityRegistry.getRemoteDoc(this.hoveredAbility) != null) {
            Ability ability = AbilityRegistry.get(this.hoveredAbility);
            if (ability != null) {
                ConfirmLinkScreen.confirmLinkNow(WikiUtils.getAbilityUrl(ability.name()), this, true);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.hoveredAbility != null) {
            for (AbilitySlot slot : AbilitySlot.values()) {
                if (keyFor(slot).matches(keyCode, scanCode)) {
                    BoundaryHandler.sendEquipAbility(new EquipAbilityPacket(slot, this.hoveredAbility));
                    return true;
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
