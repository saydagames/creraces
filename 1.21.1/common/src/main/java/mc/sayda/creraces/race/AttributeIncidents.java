package mc.sayda.creraces.race;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.registry.ModAttributes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import mc.sayda.creraces.engine.ManagedModifier;

/**
 * Handles the application of racial attributes.
 */
@SuppressWarnings("null")
public class AttributeIncidents {
    // Modifier ids for the racial modifiers applied from IPlayerVariables
    private static final ResourceLocation RACE_AD_MODIFIER = ResourceLocation.fromNamespaceAndPath("creraces", "race_ad");
    private static final ResourceLocation MANA_AP_MODIFIER = ResourceLocation.fromNamespaceAndPath("creraces", "mana_ap_scaling");
    private static final ResourceLocation EQUIP_DOUBLE_JUMP_MODIFIER = ResourceLocation.fromNamespaceAndPath("creraces", "equip_double_jump");

    public static void eikiJudgment(ServerPlayer player) {
        DataUtils.getVariables(player).ifPresent(vars -> {
            // Fetch Race to apply other modifiers
            mc.sayda.creraces.race.Race race = mc.sayda.creraces.race.RaceRegistry.get(vars.getRace());
            if (race == null) {
                purgeRacialAttributes(player);
                return;
            }

            // 1. Attack Damage (AD) -> Vanilla Attack Damage, scaled by RACIAL_AD_MULTIPLIER
            AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attackDamage != null) {
                double racialAD = vars.getAd();
                double amount = racialAD * mc.sayda.creraces.config.CreRacesConfig.RACIAL_AD_MULTIPLIER.get();
                AttributeModifier.Operation op = AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;

                AttributeModifier existing = attackDamage.getModifier(RACE_AD_MODIFIER);
                if (existing == null || Math.abs(existing.amount() - amount) > 1e-6
                        || existing.operation() != op) {
                    if (existing != null)
                        attackDamage.removeModifier(RACE_AD_MODIFIER);
                    if (amount != 0) {
                        attackDamage.addPermanentModifier(
                                new AttributeModifier(RACE_AD_MODIFIER, amount, op));
                    }
                }
            }

            // 2. Trait Processing
            java.util.Set<ResourceLocation> activeTraits = new java.util.HashSet<>();

            var traits$ = race.traits();
            if (traits$ == null) traits$ = java.util.Collections.emptyList();
            for (mc.sayda.creraces.engine.TraitRegistry.RaceTrait trait : traits$) {
                if (trait instanceof mc.sayda.creraces.engine.traits.AttributeModifierTrait amt) {
                    net.minecraft.core.Holder<Attribute> resolvedAttr = amt.getAttribute();
                    if (resolvedAttr == null) continue;

                    String traitId = trait.getTraitId();
                    ResourceLocation modifierId = ManagedModifier.idOf(traitId);
                    activeTraits.add(modifierId);

                    // Method == REMOVE: unregister and skip.
                    if (amt.getMethod() == mc.sayda.creraces.engine.AttributeMethod.REMOVE) {
                        AttributeInstance instance = player.getAttribute(resolvedAttr);
                        if (instance != null && instance.getModifier(modifierId) != null) {
                            instance.removeModifier(modifierId);
                            vars.removeManagedModifier(modifierId);
                            mc.sayda.creraces.CreRaces.LOGGER.debug("AttributeIncidents: Trait REMOVED {} from {}", traitId, player.getScoreboardName());
                        }
                        continue;
                    }

                    // A. Managed Traits (Live sync/scaling)
                    if (amt.isManaged()) {
                        // Check if we actually need to update the managed list entry (to avoid resetting the timer)
                        vars.getManagedModifier(modifierId).ifPresentOrElse(mod -> {
                            if (!mod.valueJson().equals(amt.getValueJson()) ||
                                (amt.getRawCondition() != null && !mod.conditionJson().equals(amt.getRawCondition()))) {
                                // Update existing entry; this intentionally resets the timer.
                                vars.addManagedModifier(new mc.sayda.creraces.engine.ManagedModifier(
                                    modifierId, amt.getAttributeId(), amt.getValueJson(), amt.getOperation(),
                                    "creraces:" + traitId,
                                    amt.getRawCondition() != null ? amt.getRawCondition() : new com.google.gson.JsonObject(),
                                    amt.getRawCondition() != null, amt.getInterval(), player.tickCount + amt.getInterval()
                                ));
                                mc.sayda.creraces.CreRaces.LOGGER.debug("AttributeIncidents: Updated data for Managed modifier {}", traitId);
                            }
                        }, () -> {
                            // First time application
                            vars.addManagedModifier(new mc.sayda.creraces.engine.ManagedModifier(
                                modifierId, amt.getAttributeId(), amt.getValueJson(), amt.getOperation(),
                                "creraces:" + traitId,
                                amt.getRawCondition() != null ? amt.getRawCondition() : new com.google.gson.JsonObject(),
                                amt.getRawCondition() != null, amt.getInterval(), player.tickCount + amt.getInterval()
                            ));
                            mc.sayda.creraces.CreRaces.LOGGER.debug("AttributeIncidents: Registered new Managed modifier {}", traitId);
                        });
                        continue;
                    }

                    // B. Static Traits (Innate stats)
                    // If not managed, the scanner handles application every 20 ticks.
                    boolean conditionMet = amt.getCondition() == null
                            || amt.getCondition().evaluate(player, null, null, null);

                    if (conditionMet) {
                        AttributeInstance instance = player.getAttribute(resolvedAttr);
                        if (instance != null) {
                            double newValue = amt.getValue().evaluate(player);
                            if (ModAttributes.isPercentAttribute(resolvedAttr))
                                newValue /= 100.0;

                            AttributeModifier.Operation newOp = amt.getOperation();
                            AttributeModifier existing = instance.getModifier(modifierId);

                            // Only update if the value or operation has actually changed (avoid redundant entity updates)
                            if (existing == null || Math.abs(existing.amount() - newValue) > 1e-6
                                    || existing.operation() != newOp) {
                                if (existing != null)
                                    instance.removeModifier(modifierId);

                                AttributeModifier newMod = new AttributeModifier(modifierId, newValue, newOp);
                                instance.addPermanentModifier(newMod);
                                mc.sayda.creraces.CreRaces.LOGGER.debug("EikiJudgment: Applied static trait {} to {}", traitId, player.getScoreboardName());
                            }
                        }
                    } else {
                        // Condition failed for static trait -> remove
                        AttributeInstance instance = player.getAttribute(resolvedAttr);
                        if (instance != null && instance.getModifier(modifierId) != null) {
                            instance.removeModifier(modifierId);
                            mc.sayda.creraces.CreRaces.LOGGER.debug("EikiJudgment: Removed static trait {} from {} (Condition failed)", traitId, player.getScoreboardName());
                        }
                    }
                }
            }

            // 3. Managed Modifier Sync (Smart Sync)
            java.util.List<ResourceLocation> toRemoveManaged = new java.util.ArrayList<>();
            for (mc.sayda.creraces.engine.ManagedModifier mod : vars.getManagedModifiers()) {
                if (mod.shouldCheck(player.tickCount)) {
                    vars.addManagedModifier(mod.withNextCheck(player.tickCount));

                    net.minecraft.core.Holder<Attribute> resolvedAttr = ModAttributes.getAttribute(mod.attributeId());
                    if (resolvedAttr == null) continue;

                    AttributeInstance instance = player.getAttribute(resolvedAttr);
                    if (instance == null) continue;

                    // A. Lifecycle Purge
                    if (mod.hasLifecycle() && mod.getCondition() != null && !mod.getCondition().evaluate(player, null, null, null)) {
                        toRemoveManaged.add(mod.id());
                        instance.removeModifier(mod.id());
                        mc.sayda.creraces.CreRaces.LOGGER.debug("ManagedModifier: Purged lifecycle modifier {} from {}", mod.name(), player.getScoreboardName());
                    } else {
                        // B. Value Sync (Refresh ScalingValues)
                        double newValue = mod.getScalingValue().evaluate(player);
                        if (ModAttributes.isPercentAttribute(resolvedAttr)) newValue /= 100.0;

                        AttributeModifier existing = instance.getModifier(mod.id());
                        if (existing == null || Math.abs(existing.amount() - newValue) > 1e-6 || existing.operation() != mod.operation()) {
                            if (existing != null) instance.removeModifier(mod.id());
                            instance.addPermanentModifier(new AttributeModifier(mod.id(), newValue, mod.operation()));
                            mc.sayda.creraces.CreRaces.LOGGER.debug("ManagedModifier: Synced value for {} on {} (val: {})", mod.name(), player.getScoreboardName(), newValue);
                        }
                    }
                }
            }
            toRemoveManaged.forEach(vars::removeManagedModifier);

            // 4. Double Jump
            AttributeInstance doubleJumpAttr = player
                    .getAttribute(mc.sayda.creraces.registry.ModAttributes.DOUBLE_JUMP);
            if (doubleJumpAttr != null) {
                boolean isEquipped = false;
                for (mc.sayda.creraces.ability.AbilitySlot slot : mc.sayda.creraces.ability.AbilitySlot.values()) {
                    if (ResourceLocation.fromNamespaceAndPath("creraces", "double_jump").equals(vars.getAbilityInSlot(slot))) {
                        isEquipped = true;
                        break;
                    }
                }

                AttributeModifier existing = doubleJumpAttr.getModifier(EQUIP_DOUBLE_JUMP_MODIFIER);
                if (isEquipped) {
                    if (existing == null) {
                        doubleJumpAttr.addPermanentModifier(new AttributeModifier(EQUIP_DOUBLE_JUMP_MODIFIER,
                                1.0, AttributeModifier.Operation.ADD_VALUE));
                        mc.sayda.creraces.CreRaces.LOGGER.debug("EikiJudgment: Applied Double Jump modifier to {}",
                                player.getScoreboardName());
                    }
                } else if (existing != null) {
                    doubleJumpAttr.removeModifier(EQUIP_DOUBLE_JUMP_MODIFIER);
                    mc.sayda.creraces.CreRaces.LOGGER.debug("EikiJudgment: Removed Double Jump modifier from {}",
                            player.getScoreboardName());
                }
            }

            // 5. Global Mana Scaling (30% AP)
            AttributeInstance maxMana = player.getAttribute(ModAttributes.resolve(ModAttributes.MAX_MANA));
            if (maxMana != null) {
                double amount = vars.getAp() * 0.3;
                AttributeModifier existing = maxMana.getModifier(MANA_AP_MODIFIER);
                if (existing == null || Math.abs(existing.amount() - amount) > 1e-6) {
                    if (existing != null)
                        maxMana.removeModifier(MANA_AP_MODIFIER);
                    if (amount != 0) {
                        maxMana.addPermanentModifier(new AttributeModifier(MANA_AP_MODIFIER,
                                amount, AttributeModifier.Operation.ADD_VALUE));
                    }
                }
            }
        });
    }

    /**
     * Performs a one-time "Hard Purge" of all racial attributes.
     * Should be called during race resets or transformations.
     */
    public static void purgeRacialAttributes(ServerPlayer player) {
        // 1. Clear specific deterministic modifiers
        clearModifier(player, ModAttributes.resolve(ModAttributes.DOUBLE_JUMP), EQUIP_DOUBLE_JUMP_MODIFIER);
        clearModifier(player, Attributes.ATTACK_DAMAGE, RACE_AD_MODIFIER);
        clearModifier(player, ModAttributes.resolve(ModAttributes.MAX_MANA), MANA_AP_MODIFIER);

        // 2. Clear all Managed Modifiers
        DataUtils.getVariables(player).ifPresent(vars -> {
            for (mc.sayda.creraces.engine.ManagedModifier mod : vars.getManagedModifiers()) {
                net.minecraft.core.Holder<Attribute> attr = ModAttributes.getAttribute(mod.attributeId());
                if (attr != null) {
                    AttributeInstance instance = player.getAttribute(attr);
                    if (instance != null) {
                        instance.removeModifier(mod.id());
                    }
                }
            }
            vars.clearManagedModifiers();
        });

        // 3. Global Scan & Sweep
        // This ensures any static traits (unmanaged) are wiped clean before the new race applies its own.
        // Modifiers are keyed by ResourceLocation in 1.21+, so the mod's own namespace identifies ours.
        player.getAttributes().getSyncableAttributes().forEach(instance -> {
            java.util.List<ResourceLocation> toRemove = new java.util.ArrayList<>();
            instance.getModifiers().forEach(mod -> {
                if (mod.id().getNamespace().equals(mc.sayda.creraces.CreRaces.MODID)) {
                    toRemove.add(mod.id());
                }
            });
            toRemove.forEach(instance::removeModifier);
        });

        mc.sayda.creraces.CreRaces.LOGGER.debug("AttributeIncidents: Performed global attribute purge for {}", player.getScoreboardName());
    }

    private static void clearModifier(ServerPlayer player, net.minecraft.core.Holder<Attribute> attribute,
            ResourceLocation id) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
        }
    }
}
