package mc.sayda.creraces.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import mc.sayda.creraces.ability.Ability;
import mc.sayda.creraces.ability.AbilityRegistry;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.network.EquipAbilityPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SkillWheelScreen extends Screen {
    private static final int WHEEL_RADIUS = 80;
    private static final int ITEM_SIZE = 24;

    public SkillWheelScreen() {
        super(Component.translatable("creraces.screen.skill_wheel"));
    }

    private ResourceLocation hoveredAbility = null;

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.hoveredAbility = null;

        DataUtils.getVariables(Minecraft.getInstance().player).ifPresent(vars -> {
            Set<ResourceLocation> unlocked = vars.getUnlockedAbilities();
            if (unlocked.isEmpty()) {
                graphics.drawCenteredString(this.font, Component.translatable("creraces.screen.no_abilities"), centerX,
                        centerY, 0xFFFFFF);
                return;
            }

            List<ResourceLocation> abilities = new ArrayList<>(unlocked);
            int count = abilities.size();
            double angleStep = 2 * Math.PI / count;

            for (int i = 0; i < count; i++) {
                ResourceLocation id = abilities.get(i);
                Ability ability = AbilityRegistry.get(id);
                if (ability == null)
                    continue;

                double angle = i * angleStep - Math.PI / 2; // Start from top
                int x = centerX + (int) (WHEEL_RADIUS * Math.cos(angle)) - ITEM_SIZE / 2;
                int y = centerY + (int) (WHEEL_RADIUS * Math.sin(angle)) - ITEM_SIZE / 2;

                // Hover check
                boolean isHovered = mouseX >= x && mouseX < x + ITEM_SIZE && mouseY >= y && mouseY < y + ITEM_SIZE;

                // Selection Highlight
                if (isHovered) {
                    this.hoveredAbility = id;
                    graphics.fill(x - 2, y - 2, x + ITEM_SIZE + 2, y + ITEM_SIZE + 2, 0x80FFFFFF);

                    // Tooltip
                    mc.sayda.creraces.util.RemoteDocConfig config = AbilityRegistry.getRemoteDoc(id);
                    Component description = mc.sayda.creraces.util.RemoteDocFetcher.getRemoteDescription(id, config,
                            ability.description());

                    List<Component> tooltip = new ArrayList<>();
                    tooltip.add(ability.name());
                    tooltip.add(description);
                    tooltip.add(Component.literal(""));
                    tooltip.add(Component.translatable("creraces.screen.click_for_wiki")
                            .withStyle(net.minecraft.ChatFormatting.BLUE, net.minecraft.ChatFormatting.ITALIC));

                    graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
                }

                // Icon
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

                // Dynamically get the keybind for each slot
                int slotY = y;
                for (AbilitySlot slot : AbilitySlot.values()) {
                    if (id.equals(vars.getAbilityInSlot(slot))) {
                        net.minecraft.client.KeyMapping key = switch (slot) {
                            case A1 -> mc.sayda.creraces.client.ModKeyMappings.ABILITY_A1;
                            case A2 -> mc.sayda.creraces.client.ModKeyMappings.ABILITY_A2;
                            case A3 -> mc.sayda.creraces.client.ModKeyMappings.ABILITY_A3;
                            case A4 -> mc.sayda.creraces.client.ModKeyMappings.ABILITY_A4;
                            case A5 -> mc.sayda.creraces.client.ModKeyMappings.ABILITY_A5;
                        };
                        int color = switch (slot) {
                            case A1 -> 0x55FF55; // green
                            case A2 -> 0xFF5555; // red
                            case A3 -> 0xFFFF55; // yellow
                            case A4 -> 0x5555FF; // blue
                            case A5 -> 0xFFAA00; // orange
                        };
                        graphics.drawCenteredString(this.font, key.getTranslatedKeyMessage(), x + ITEM_SIZE + 10, slotY,
                                color);
                        slotY += 10;
                    }
                }

                graphics.blit(ability.icon(), x, y, 0, 0, ITEM_SIZE, ITEM_SIZE, ITEM_SIZE, ITEM_SIZE);
            }
        });

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    @SuppressWarnings("null")
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.hoveredAbility != null) {
            Ability ability = AbilityRegistry.get(this.hoveredAbility);
            if (ability != null) {
                String url = mc.sayda.creraces.util.WikiUtils.getAbilityUrl(ability.name());
                Minecraft.getInstance().setScreen(new net.minecraft.client.gui.screens.ConfirmLinkScreen(confirmed -> {
                    if (confirmed) {
                        net.minecraft.Util.getPlatform().openUri(url);
                    }
                    Minecraft.getInstance().setScreen(this);
                }, url, true));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.hoveredAbility != null) {
            for (AbilitySlot slot : AbilitySlot.values()) {
                net.minecraft.client.KeyMapping key = switch (slot) {
                    case A1 -> mc.sayda.creraces.client.ModKeyMappings.ABILITY_A1;
                    case A2 -> mc.sayda.creraces.client.ModKeyMappings.ABILITY_A2;
                    case A3 -> mc.sayda.creraces.client.ModKeyMappings.ABILITY_A3;
                    case A4 -> mc.sayda.creraces.client.ModKeyMappings.ABILITY_A4;
                    case A5 -> mc.sayda.creraces.client.ModKeyMappings.ABILITY_A5;
                };

                if (key.matches(keyCode, scanCode)) {
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
