package mc.sayda.creraces.client;

import com.mojang.blaze3d.systems.RenderSystem;
import mc.sayda.creraces.ability.Ability;
import mc.sayda.creraces.ability.AbilityRegistry;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import mc.sayda.creraces.race.ResourceType;
import mc.sayda.creraces.registry.ModAttributes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

public class RaceOverlay {
    private static final ResourceLocation UI_BG = new ResourceLocation("creraces", "textures/screens/ui_bg.png");
    private static final ResourceLocation UI_FG_BG = new ResourceLocation("creraces", "textures/screens/ui_fg_bg.png");
    private static final ResourceLocation UI_FRAME = new ResourceLocation("creraces", "textures/screens/ui.png");
    private static final ResourceLocation UI_FG_FRAME = new ResourceLocation("creraces", "textures/screens/ui_fg.png");
    private static final ResourceLocation UI_H = new ResourceLocation("creraces", "textures/screens/ui_h.png");
    private static final ResourceLocation UI_M = new ResourceLocation("creraces", "textures/screens/ui_m.png");
    private static final ResourceLocation UI_R = new ResourceLocation("creraces", "textures/screens/ui_r.png");
    private static final ResourceLocation UI_DR = new ResourceLocation("creraces", "textures/screens/ui_dr.png");
    private static final ResourceLocation UI_E = new ResourceLocation("creraces", "textures/screens/ui_e.png");
    private static final ResourceLocation UI_G = new ResourceLocation("creraces", "textures/screens/ui_g.png");
    private static final ResourceLocation UI_P = new ResourceLocation("creraces", "textures/screens/ui_p.png");

    public static void render(@Nonnull GuiGraphics graphics, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui)
            return;

        graphics.pose().pushPose();
        DataUtils.getVariables(player).ifPresent(vars -> {
            ResourceLocation raceId = vars.getRace();
            if (raceId.equals(RaceRegistry.NONE))
                return;

            Race race = RaceRegistry.get(raceId);
            if (race == null)
                return;

            // Modern 1.20.1 HUD rendering uses GuiGraphics state management

            // Legacy Portrait Base (Based on UI Overlay legacy code)
            int basePosX = 14;
            int basePosY = 13;

            // UI BG (Portrait mask area) - legacy pos: 14, 13
            graphics.blit(UI_BG, basePosX, basePosY, 0, 0, 33, 32, 33, 32);

            // UI FG BG (Bars background) - legacy pos: 14, 45
            graphics.blit(UI_FG_BG, basePosX, basePosY + 32, 0, 0, 32, 15, 32, 15);

            // Race Icon - legacy pos: 15, 16
            graphics.blit(race.icon(), basePosX + 1, basePosY + 3, 0, 0, 29, 29, 29, 29);

            // UI Frame (Portrait ornate border) - legacy pos: 11, 13
            graphics.blit(UI_FRAME, basePosX - 3, basePosY, 0, 0, 36, 33, 36, 33);

            // UI FG Frame (Bars ornate border) - legacy pos: 12, 44
            graphics.blit(UI_FG_FRAME, basePosX - 2, basePosY + 31, 0, 0, 35, 18, 35, 18);

            // Stepped Bars (using legacy 31-step logic)
            // Health Bar - legacy pos: 15, 46
            double maxHealth = player.getMaxHealth();
            double health = player.getHealth();
            renderSteppedBar(graphics, UI_H, basePosX + 1, basePosY + 33, health, maxHealth, 3);

            // Resource Bar - legacy pos: 15, 49
            ResourceLocation resourceTex = UI_M;
            double currentRes = vars.getMana();
            var maxManaAttr = ModAttributes.MAX_MANA.get();
            double maxRes = maxManaAttr != null ? player.getAttributeValue(maxManaAttr) : 0.0;

            switch (race.resourceType()) {
                case RAGE:
                    resourceTex = UI_R;
                    currentRes = vars.getRage();
                    var maxRageAttr = ModAttributes.MAX_RAGE.get();
                    maxRes = maxRageAttr != null ? player.getAttributeValue(maxRageAttr) : 0.0;
                    break;
                case ENERGY:
                    resourceTex = UI_E;
                    currentRes = vars.getEnergy();
                    var maxEnergyAttr = ModAttributes.MAX_ENERGY.get();
                    maxRes = maxEnergyAttr != null ? player.getAttributeValue(maxEnergyAttr) : 0.0;
                    break;
                case GRIT:
                    resourceTex = UI_G;
                    currentRes = vars.getGrit();
                    var maxGritAttr = ModAttributes.MAX_GRIT.get();
                    maxRes = maxGritAttr != null ? player.getAttributeValue(maxGritAttr) : 0.0;
                    break;
                case MANA:
                default:
                    // Already set to Mana
                    break;
            }

            if (race.resourceType() != ResourceType.NONE) {
                renderSteppedBar(graphics, resourceTex, basePosX + 1, basePosY + 36, currentRes, maxRes, 3);
            }

            // Modern Ability Slots (Dynamically rendered based on keybinds)
            int slotX = basePosX + 40;
            int slotY = basePosY + 4;

            for (AbilitySlot slot : AbilitySlot.values()) {
                net.minecraft.client.KeyMapping keyMapping = getKeyMapping(slot);
                if (keyMapping != null && !keyMapping.isUnbound()) {
                    renderAbilitySlot(graphics, vars, slot, slotX, slotY);
                    slotX += 25;
                }
            }

            // graphics.blit handled the state
        });
        
        graphics.pose().popPose();
    }

    private static void renderSteppedBar(@Nonnull GuiGraphics graphics, ResourceLocation texture, int x, int y,
            double current, double max, int frameHeight) {
        if (max <= 0)
            return;
        int step = (int) (Math.max(0, max - current) * 30 / max);
        step = Math.min(30, step);
        graphics.blit(texture, x, y, 0, step * frameHeight, 30, frameHeight, 30, 31 * frameHeight);
    }

    private static void renderAbilitySlot(@Nonnull GuiGraphics graphics,
            @Nonnull mc.sayda.creraces.capability.IPlayerVariables vars,
            @Nonnull AbilitySlot slot, int x, int y) {
        ResourceLocation abilityId = vars.getAbilityInSlot(slot);

        // Draw Slot Background
        graphics.fill(x - 1, y - 1, x + 19, y + 19, 0x44000000);

        if (abilityId != null && !abilityId.toString().equals("creraces:none")) {
            Ability ability = AbilityRegistry.get(abilityId);
            if (ability != null) {
                // Unified icon rendering: handles both item IDs and texture paths
                mc.sayda.creraces.client.AbilityIconRenderer.render(graphics, ability.icon(), x + 1, y + 1, 16);

                // Push Z so overlays render above 3D block icons
                graphics.pose().pushPose();
                graphics.pose().translate(0, 0, 200);

                // Draw Cooldown
                int cooldown = vars.getCooldown(abilityId);
                if (cooldown > 0) {
                    // Clamp percent between 0 and 1 regardless of cooldown definition
                    float cooldownPercent = ability.cooldown() > 0
                            ? Math.min(1f, (float) cooldown / (float) ability.cooldown())
                            : 1f;
                    graphics.fill(x + 1, y + 1 + (int) (16 * (1 - cooldownPercent)), x + 17, y + 17, 0x80000000);
                    String k = java.util.Objects.requireNonNull(String.valueOf(Math.max(1, cooldown / 20)));
                    var font = Minecraft.getInstance().font;
                    if (font != null) {
                        graphics.drawCenteredString(font, k, x + 9, y + 5, 0xFFFFFF);
                    }
                }

                // Draw "Unusable" or "Off" indication (Borders)
                boolean usable = isUsable(Minecraft.getInstance().player, vars, ability);
                boolean active = vars.getPersistentState(abilityId) > 0;

                if (!usable) {
                    drawBorder(graphics, x - 1, y - 1, 20, 20, 0x88FF0000); // Translucent Red
                } else if (active) {
                    drawBorder(graphics, x - 1, y - 1, 20, 20, 0x8800FF00); // Translucent Green
                } else if (ability.type() == mc.sayda.creraces.ability.AbilityType.INNATE
                        || ability.type() == mc.sayda.creraces.ability.AbilityType.PASSIVE) {
                    // Only show gray border if it's a toggleable ability that is currently OFF
                    if (ability.onDeactivate() != null && !ability.onDeactivate().isEmpty()) {
                        drawBorder(graphics, x - 1, y - 1, 20, 20, 0x88AAAAAA); // Translucent Gray
                    }
                }

                graphics.pose().popPose();

                // Draw Level Overlay
                int level = vars.getAbilityLevel(abilityId);
                if (level > 0) {
                    mc.sayda.creraces.client.AbilityIconRenderer.renderLevel(graphics, level, x + 1, y + 1, 16);
                }
            }
        }

        // Draw Keybind Label
        var font = Minecraft.getInstance().font;
        if (font != null) {
            graphics.drawCenteredString(font,
                    Component.translatable("gui.creraces.hud.slot." + slot.name().toLowerCase()), x + 9, y + 20, 0xAAAAAA);
        }
    }

    private static void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        // Top
        graphics.fill(x, y, x + width, y + 1, color);
        // Bottom
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        // Left
        graphics.fill(x, y + 1, x + 1, y + height - 1, color);
        // Right
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    private static boolean isUsable(net.minecraft.client.player.LocalPlayer player,
            mc.sayda.creraces.capability.IPlayerVariables vars, Ability ability) {
        if (ability.cost() <= 0)
            return true;

        ResourceLocation raceId = vars.getRace();
        Race race = RaceRegistry.get(raceId);
        if (race == null)
            return true;

        double currentRes = 0;
        switch (race.resourceType()) {
            case MANA:
                currentRes = vars.getMana();
                break;
            case RAGE:
                currentRes = vars.getRage();
                break;
            case ENERGY:
                currentRes = vars.getEnergy();
                break;
            case GRIT:
                currentRes = vars.getGrit();
                break;
            case SOUL:
                currentRes = vars.getSoul();
                break;
            case NONE:
            default:
                return true;
        }

        return currentRes >= ability.cost();
    }

    private static net.minecraft.client.KeyMapping getKeyMapping(AbilitySlot slot) {
        switch (slot) {
            case A1:
                return ModKeyMappings.ABILITY_A1;
            case A2:
                return ModKeyMappings.ABILITY_A2;
            case A3:
                return ModKeyMappings.ABILITY_A3;
            case A4:
                return ModKeyMappings.ABILITY_A4;
            case A5:
                return ModKeyMappings.ABILITY_A5;
            default:
                return null;
        }
    }
}
