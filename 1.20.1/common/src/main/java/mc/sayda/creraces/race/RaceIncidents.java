package mc.sayda.creraces.race;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.network.BoundaryHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;

public class RaceIncidents {
    public static void transformPlayer(ServerPlayer player, ResourceLocation raceId) {
        if (raceId.equals(RaceRegistry.NONE)) {
            DataUtils.getVariables(player).ifPresent(vars -> {
                vars.fantasySealReset();
                // Clear cosmetic addons when resetting race
                CosmeticIncidents.clearAllRacialAddons(player);
                AttributeIncidents.eikiJudgment(player);

                // Reset Scale
                try {
                    virtuoel.pehkui.api.ScaleTypes.BASE.getScaleData(player).setScale(1.0f);
                    virtuoel.pehkui.api.ScaleTypes.BASE.getScaleData(player).setTargetScale(1.0f);
                } catch (Throwable ignored) {
                } // Pehkui might not be present

                BoundaryHandler.resyncVariables(player, player);
            });
            return;
        }

        Race race = RaceRegistry.get(raceId);
        if (race == null)
            return;

        DataUtils.getVariables(player).ifPresent(vars -> {
            // Reset state first (Fantasy Seal)
            vars.fantasySealReset();

            // Set new race
            vars.setRace(raceId);
            vars.setHasChosenRace(true);

            // Apply Scale
            try {
                virtuoel.pehkui.api.ScaleTypes.BASE.getScaleData(player).setScale(race.scale());
                virtuoel.pehkui.api.ScaleTypes.BASE.getScaleData(player).setTargetScale(race.scale());
            } catch (Throwable ignored) {
            } // Pehkui might not be present

            // Apply Base Stats
            vars.setAp(race.baseAp());
            vars.setAd(race.baseAd());
            vars.setAh(race.baseAh());
            vars.setCr(race.baseCr());

            // Initialize resources to max
            vars.setMana(race.maxResource());
            vars.setGrit(race.maxResource());
            vars.setEnergy(race.maxResource());
            vars.setRage(0);

            // Apply Eiki's Judgment (Sync attributes to Vanilla)
            AttributeIncidents.eikiJudgment(player);

            // Apply Default Customizations
            if (race.customization() != null) {
                for (mc.sayda.creraces.race.RaceCustomization cust : race.customization()) {
                    vars.setCustomization(cust.id(), cust.defaultValue());
                }
                CosmeticIncidents.applyCustomizations(player, vars.getCustomizations(), race);
            }

            // Grant Starting Abilities
            if (race.startingAbilities() != null) {
                int equippedCount = 0;
                for (ResourceLocation abilityId : race.startingAbilities()) {
                    // Unlock
                    vars.unlockAbility(abilityId);

                    // Auto-equip first two to A1 and A2
                    if (equippedCount == 0) {
                        vars.equipAbility(mc.sayda.creraces.ability.AbilitySlot.A1, abilityId);
                    } else if (equippedCount == 1) {
                        vars.equipAbility(mc.sayda.creraces.ability.AbilitySlot.A2, abilityId);
                    }
                    equippedCount++;
                }
            }

            // Sync to client
            BoundaryHandler.resyncVariables(player, player);
            // Sync to tracking players
            if (player.level() != null) {
                player.level().players().forEach(p -> {
                    if (p instanceof ServerPlayer sp) {
                        BoundaryHandler.resyncVariables(player, sp);
                    }
                });
            }

            // If the race has customizations, open the Mirror Screen
            if (race.customization() != null && !race.customization().isEmpty()) {
                BoundaryHandler.sendOpenMirror(player);
            }
        });
    }
}
