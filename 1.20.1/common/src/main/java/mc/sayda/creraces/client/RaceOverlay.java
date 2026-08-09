package mc.sayda.creraces.client;

import com.mojang.blaze3d.systems.RenderSystem;
import mc.sayda.creraces.ability.Ability;
import mc.sayda.creraces.ability.AbilityRegistry;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.config.CreRacesConfig;
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
import java.util.Map;

public class RaceOverlay {
    private static final ResourceLocation UI_BG = new ResourceLocation("creraces", "textures/screens/ui_bg.png");
    private static final ResourceLocation UI_BG2 = new ResourceLocation("creraces", "textures/screens/ui_bg2.png");
    private static final ResourceLocation UI_FRAME = new ResourceLocation("creraces", "textures/screens/ui.png");
    private static final ResourceLocation UI_LVL = new ResourceLocation("creraces", "textures/screens/ui_lvl.png");
    private static final ResourceLocation UI_RDY = new ResourceLocation("creraces", "textures/screens/ui_rdy.png");
    private static final ResourceLocation UI_FG_FRAME = new ResourceLocation("creraces", "textures/screens/ui_fg.png");
    private static final ResourceLocation UI_FG_BG = new ResourceLocation("creraces", "textures/screens/ui_fg_bg.png");
    private static final ResourceLocation UI_M = new ResourceLocation("creraces", "textures/screens/ui_m.png");
    private static final ResourceLocation UI_R = new ResourceLocation("creraces", "textures/screens/ui_r.png");
    private static final ResourceLocation UI_E = new ResourceLocation("creraces", "textures/screens/ui_e.png");
    private static final ResourceLocation UI_G = new ResourceLocation("creraces", "textures/screens/ui_g.png");


    public static void render(@Nonnull GuiGraphics graphics, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui)
            return;

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.pose().pushPose();
        DataUtils.getVariables(player).ifPresent(vars -> {
            ResourceLocation raceId = vars.getRace();
            if (raceId.equals(RaceRegistry.NONE))
                return;

            Race race = RaceRegistry.get(raceId);
            if (race == null)
                return;

            // Modern 1.20.1 HUD rendering uses GuiGraphics state management

            // Group positions driven by HUD editor config
            int globalX = CreRacesConfig.HUD_ANCHOR_X.get();
            int globalY = CreRacesConfig.HUD_ANCHOR_Y.get();
            int basePosX = globalX + CreRacesConfig.HUD_PORTRAIT_X.get();
            int basePosY = globalY + CreRacesConfig.HUD_PORTRAIT_Y.get();

            // Scale around the global anchor
            float scale = CreRacesConfig.HUD_SCALE.get().floatValue();
            if (scale != 1.0f) {
                graphics.pose().translate(globalX, globalY, 0);
                graphics.pose().scale(scale, scale, 1.0f);
                graphics.pose().translate(-globalX, -globalY, 0);
            }

            // All portrait assets are 80x80, displayed at half-scale = 40x40
            graphics.blit(UI_BG,  basePosX, basePosY, 0, 0, 40, 40, 40, 40);
            graphics.blit(UI_BG2, basePosX, basePosY, 0, 0, 40, 40, 40, 40);

            // Race Icon (29x29) within the 40x40 canvas
            graphics.blit(race.icon(), basePosX + 5, basePosY + 2, 0, 0, 29, 29, 29, 29);

            // Resource Bar - legacy pos: 15, 49 (rendered before frame so frame draws on top)
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
                    break;
            }
            if (race.resourceType() != ResourceType.NONE) {
                renderSteppedBar(graphics, resourceTex, basePosX + 4, basePosY + 33, currentRes, maxRes, 3);
            }

            // Frame overlays: same 80x80 canvas, displayed at 40x40, same origin as BG
            graphics.blit(UI_FRAME, basePosX, basePosY, 0, 0, 40, 40, 40, 40);
            graphics.blit(UI_LVL,   basePosX, basePosY, 0, 0, 40, 40, 40, 40);
            graphics.blit(UI_RDY,   basePosX, basePosY, 0, 0, 40, 40, 40, 40);

            // Overlay Bars - config-driven position
            int barStartX = globalX + CreRacesConfig.HUD_BARS_X.get();
            int barStartY = globalY + CreRacesConfig.HUD_BARS_Y.get();
            renderOverlayBars(graphics, vars, race, barStartX, barStartY);

            // Ability Slots - config-driven position
            int slotX = globalX + CreRacesConfig.HUD_ABILITIES_X.get();
            int slotY = globalY + CreRacesConfig.HUD_ABILITIES_Y.get();

            boolean abilitiesVertical = CreRacesConfig.HUD_ABILITIES_VERTICAL.get();
            String labelOrientation = CreRacesConfig.HUD_SLOT_LABEL_SIDE.get();
            int verticalStep = switch (labelOrientation) {
                case "side", "left" -> 25;
                case "none"         -> 22;
                default             -> 30;
            };
            for (AbilitySlot slot : AbilitySlot.values()) {
                net.minecraft.client.KeyMapping keyMapping = getKeyMapping(slot);
                if (keyMapping != null && !keyMapping.isUnbound()) {
                    renderAbilitySlot(graphics, vars, slot, slotX, slotY, labelOrientation);
                    if (abilitiesVertical) slotY += verticalStep;
                    else                   slotX += 25;
                }
            }

            // graphics.blit handled the state
        });
        
        graphics.pose().popPose();
    }

    private static void renderOverlayBars(@Nonnull GuiGraphics graphics,
            @Nonnull mc.sayda.creraces.capability.IPlayerVariables vars,
            @Nonnull Race race, int x, int startY) {
        var font = Minecraft.getInstance().font;
        boolean growUp = CreRacesConfig.HUD_BARS_GROW_UP.get();
        int barY = startY;

        for (mc.sayda.creraces.ability.OverlayBar bar : race.overlayBars()) {
            barY = renderBar(graphics, font, vars, bar, x, barY, growUp);
        }

        for (Ability ability : AbilityRegistry.getAll()) {
            if (!ability.allowedRaces().isEmpty() && !ability.allowedRaces().contains(race.id())) continue;
            for (mc.sayda.creraces.ability.OverlayBar bar : ability.overlayBars()) {
                barY = renderBar(graphics, font, vars, bar, x, barY, growUp);
            }
        }
    }

    private static int renderBar(@Nonnull GuiGraphics graphics, net.minecraft.client.gui.Font font,
            @Nonnull mc.sayda.creraces.capability.IPlayerVariables vars,
            @Nonnull mc.sayda.creraces.ability.OverlayBar bar, int x, int barY, boolean growUp) {
        double value = bar.getValue(vars);
        if (value <= 0) return barY;

        // growUp=false: anchor is top of stack, bars go down.
        // growUp=true:  anchor is bottom of stack; drawY+4 places BG bottom exactly at barY.
        int drawY = growUp ? barY + 4 : barY;

        graphics.blit(UI_FG_BG, x - 1, drawY - 11, 0, 0, 35, 7, 35, 7);

        float r = ((bar.color() >> 16) & 0xFF) / 255f;
        float g = ((bar.color() >> 8) & 0xFF) / 255f;
        float b = (bar.color() & 0xFF) / 255f;
        float a = ((bar.color() >> 24) & 0xFF) / 255f;
        // Re-enable blend each bar: previous bar's frame blit teardown disables it
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(r, g, b, a);
        renderSteppedBar(graphics, UI_G, x + 2, drawY - 9, value, bar.max(), 3);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        graphics.blit(UI_FG_FRAME, x - 1, drawY - 11, 0, 0, 35, 7, 35, 7);

        if (font != null) {
            String valueStr = bar.sourceType().equals("cooldown") && CreRacesConfig.BAR_SHOW_SECONDS.get()
                    ? Math.max(1, (int) value / 20) + "s"
                    : String.valueOf((int) value);
            String labelText = switch (CreRacesConfig.BAR_LABEL_MODE.get()) {
                case "name"   -> bar.label();
                case "value"  -> valueStr;
                case "hidden" -> null;
                default       -> bar.label() + ": " + valueStr;
            };
            if (labelText != null) {
                graphics.drawString(font, labelText, x + 37, drawY - 11, 0xAAAAAA, true);
            }
        }

        return growUp ? barY - 9 : barY + 9;
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
            @Nonnull AbilitySlot slot, int x, int y, String labelOrientation) {
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
        if (font != null && !labelOrientation.equals("none")) {
            var label = Component.translatable("gui.creraces.hud.slot." + slot.name().toLowerCase());
            switch (labelOrientation) {
                case "side" -> graphics.drawString(font, label, x + 21, y + 6, 0xAAAAAA, false);
                case "top"  -> graphics.drawCenteredString(font, label, x + 9, y - 10, 0xAAAAAA);
                case "left" -> graphics.drawString(font, label, x - font.width(label) - 3, y + 6, 0xAAAAAA, false);
                default     -> graphics.drawCenteredString(font, label, x + 9, y + 20, 0xAAAAAA);
            }
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
