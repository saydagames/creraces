package mc.sayda.creraces.race;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.registry.ModAttributes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * Handles the application of RP attributes.
 */
@SuppressWarnings("null")
public class AttributeIncidents {
    // Unique UUID for racial AD modifier (applied from IPlayerVariables.getAd())
    private static final UUID RACE_AD_MODIFIER = UUID.fromString("c0d3b4be-0001-4000-8000-000000000001");
    private static final UUID MANA_AP_MODIFIER = UUID.fromString("c0d3b4be-0001-4000-8000-000000000010");
    private static final UUID EQUIP_DOUBLE_JUMP_MODIFIER = UUID.fromString("c0d3b4be-0001-4000-8000-000000000005");

    public static void eikiJudgment(ServerPlayer player) {
        DataUtils.getVariables(player).ifPresent(vars -> {
            // Fetch Race to apply other modifiers
            mc.sayda.creraces.race.Race race = mc.sayda.creraces.race.RaceRegistry.get(vars.getRace());
            if (race == null) {
                purgeRacialAttributes(player);
                return;
            }

            // 1. Attack Damage (AD) -> Vanilla Attack Damage (1% per point)
            AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attackDamage != null) {
                double racialAD = vars.getAd();
                double amount = racialAD * mc.sayda.creraces.config.CreRacesConfig.RACIAL_AD_MULTIPLIER.get();
                AttributeModifier.Operation op = AttributeModifier.Operation.MULTIPLY_TOTAL;

                AttributeModifier existing = attackDamage.getModifier(RACE_AD_MODIFIER);
                if (existing == null || Math.abs(existing.getAmount() - amount) > 1e-6
                        || existing.getOperation() != op) {
                    if (existing != null)
                        attackDamage.removeModifier(RACE_AD_MODIFIER);
                    if (amount != 0) {
                        attackDamage.addPermanentModifier(
                                new AttributeModifier(RACE_AD_MODIFIER, "CreRaces AD Modifier", amount, op));
                    }
                }
            }

            // 2. Trait Processing
            java.util.Set<UUID> activeTraits = new java.util.HashSet<>();

            for (mc.sayda.creraces.engine.TraitRegistry.RaceTrait trait : race.traits()) {
                if (trait instanceof mc.sayda.creraces.engine.traits.AttributeModifierTrait amt) {
                    Attribute attr = amt.getAttribute();
                    if (attr == null) continue;

                    Attribute resolvedAttr = ModAttributes.resolve(attr);
                    String traitId = trait.getTraitId();
                    UUID uuid = UUID.nameUUIDFromBytes(("creraces:" + traitId).getBytes());
                    activeTraits.add(uuid);

                    // Method: REMOVE logic
                    if (amt.getMethod() == mc.sayda.creraces.engine.AttributeMethod.REMOVE) {
                        AttributeInstance instance = player.getAttribute(resolvedAttr);
                        if (instance != null && instance.getModifier(uuid) != null) {
                            instance.removeModifier(uuid);
                            vars.removeManagedModifier(uuid);
                            mc.sayda.creraces.CreRaces.LOGGER.debug("AttributeIncidents: Trait REMOVED {} from {}", traitId, player.getScoreboardName());
                        }
                        continue;
                    }

                    // If trait is Managed (explicit flag or has condition)
                    if (amt.isManaged() || amt.getRawCondition() != null) {
                        vars.addManagedModifier(new mc.sayda.creraces.engine.ManagedModifier(
                            uuid, 
                            amt.getAttributeId(), 
                            amt.getValueJson(),
                            amt.getOperation(),
                            "creraces:" + traitId,
                            amt.getRawCondition() != null ? amt.getRawCondition() : new com.google.gson.JsonObject(), 
                            amt.getRawCondition() != null,
                            amt.getInterval(), 
                            player.tickCount + amt.getInterval()
                        ));
                        // Managed modifiers are handled by the background loop below
                        continue;
                    }

                    // Static Trait Application (Legacy path for unmanaged traits)
                    boolean conditionMet = amt.getCondition() == null
                            || amt.getCondition().evaluate(player, null, null, null);

                    if (conditionMet) {
                        AttributeInstance instance = player.getAttribute(resolvedAttr);
                        if (instance != null) {
                            double newValue = amt.getValue().evaluate(player);
                            if (ModAttributes.isPercentAttribute(resolvedAttr))
                                newValue /= 100.0;

                            AttributeModifier.Operation newOp = amt.getOperation();
                            AttributeModifier existing = instance.getModifier(uuid);

                            if (existing == null || Math.abs(existing.getAmount() - newValue) > 1e-6
                                    || existing.getOperation() != newOp) {
                                if (existing != null)
                                    instance.removeModifier(uuid);

                                AttributeModifier newMod = new AttributeModifier(uuid, "creraces:" + traitId,
                                        newValue, newOp);
                                instance.addPermanentModifier(newMod);
                                mc.sayda.creraces.CreRaces.LOGGER.debug("EikiJudgment: Applied static trait {} to {}", traitId, player.getScoreboardName());
                            }
                        }
                    } else {
                        // Condition failed for static trait -> remove
                        AttributeInstance instance = player.getAttribute(resolvedAttr);
                        if (instance != null && instance.getModifier(uuid) != null) {
                            instance.removeModifier(uuid);
                        }
                    }
                }
            }

            // 3. Managed Modifier Sync (Smart Sync)
            java.util.List<java.util.UUID> toRemoveManaged = new java.util.ArrayList<>();
            for (mc.sayda.creraces.engine.ManagedModifier mod : vars.getManagedModifiers()) {
                if (mod.shouldCheck(player.tickCount)) {
                    vars.addManagedModifier(mod.withNextCheck(player.tickCount));

                    net.minecraft.world.entity.ai.attributes.Attribute attr = net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.get(mod.attributeId());
                    if (attr == null) continue;
                    
                    Attribute resolvedAttr = ModAttributes.resolve(attr);
                    AttributeInstance instance = player.getAttribute(resolvedAttr);
                    if (instance == null) continue;

                    // A. Lifecycle Purge
                    if (mod.hasLifecycle() && !mod.getCondition().evaluate(player, null, null, null)) {
                        toRemoveManaged.add(mod.uuid());
                        instance.removeModifier(mod.uuid());
                        mc.sayda.creraces.CreRaces.LOGGER.debug("ManagedModifier: Purged lifecycle modifier {} from {}", mod.name(), player.getScoreboardName());
                    } else {
                        // B. Value Sync (Refresh ScalingValues)
                        double newValue = mod.getScalingValue().evaluate(player);
                        if (ModAttributes.isPercentAttribute(resolvedAttr)) newValue /= 100.0;

                        AttributeModifier existing = instance.getModifier(mod.uuid());
                        if (existing == null || Math.abs(existing.getAmount() - newValue) > 1e-6 || existing.getOperation() != mod.operation()) {
                            if (existing != null) instance.removeModifier(mod.uuid());
                            instance.addPermanentModifier(new AttributeModifier(mod.uuid(), mod.name(), newValue, mod.operation()));
                            mc.sayda.creraces.CreRaces.LOGGER.debug("ManagedModifier: Synced value for {} on {} (val: {})", mod.name(), player.getScoreboardName(), newValue);
                        }
                    }
                }
            }
            toRemoveManaged.forEach(vars::removeManagedModifier);

            // 5. Double Jump
            AttributeInstance doubleJumpAttr = player
                    .getAttribute(mc.sayda.creraces.registry.ModAttributes.DOUBLE_JUMP.get());
            if (doubleJumpAttr != null) {
                boolean isEquipped = false;
                for (mc.sayda.creraces.ability.AbilitySlot slot : mc.sayda.creraces.ability.AbilitySlot.values()) {
                    if (new ResourceLocation("creraces", "double_jump").equals(vars.getAbilityInSlot(slot))) {
                        isEquipped = true;
                        break;
                    }
                }

                AttributeModifier existing = doubleJumpAttr.getModifier(EQUIP_DOUBLE_JUMP_MODIFIER);
                if (isEquipped) {
                    if (existing == null) {
                        doubleJumpAttr.addPermanentModifier(new AttributeModifier(EQUIP_DOUBLE_JUMP_MODIFIER,
                                "Double Jump Ability", 1.0, AttributeModifier.Operation.ADDITION));
                        mc.sayda.creraces.CreRaces.LOGGER.debug("EikiJudgment: Applied Double Jump modifier to {}",
                                player.getScoreboardName());
                    }
                } else if (existing != null) {
                    doubleJumpAttr.removeModifier(EQUIP_DOUBLE_JUMP_MODIFIER);
                    mc.sayda.creraces.CreRaces.LOGGER.debug("EikiJudgment: Removed Double Jump modifier from {}",
                            player.getScoreboardName());
                }
            }

            // 6. Global Mana Scaling (30% AP)
            AttributeInstance maxMana = player.getAttribute(ModAttributes.resolve(ModAttributes.MAX_MANA));
            if (maxMana != null) {
                double amount = vars.getAp() * 0.3;
                AttributeModifier existing = maxMana.getModifier(MANA_AP_MODIFIER);
                if (existing == null || Math.abs(existing.getAmount() - amount) > 1e-6) {
                    if (existing != null)
                        maxMana.removeModifier(MANA_AP_MODIFIER);
                    if (amount != 0) {
                        maxMana.addPermanentModifier(new AttributeModifier(MANA_AP_MODIFIER, "Global Mana Scaling",
                                amount, AttributeModifier.Operation.ADDITION));
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
                Attribute attr = net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.get(mod.attributeId());
                if (attr != null) {
                    AttributeInstance instance = player.getAttribute(ModAttributes.resolve(attr));
                    if (instance != null) {
                        instance.removeModifier(mod.uuid());
                    }
                }
            }
            vars.clearManagedModifiers();
        });

        // 3. Global Scan & Sweep
        // This ensures any static traits (unmanaged) are wiped clean before the new race applies its own.
        player.getAttributes().getSyncableAttributes().forEach(instance -> {
            java.util.List<UUID> toRemove = new java.util.ArrayList<>();
            instance.getModifiers().forEach(mod -> {
                if (mod.getName().startsWith("creraces:")) {
                    toRemove.add(mod.getId());
                }
            });
            toRemove.forEach(instance::removeModifier);
        });
        
        mc.sayda.creraces.CreRaces.LOGGER.debug("AttributeIncidents: Performed global attribute purge for {}", player.getScoreboardName());
    }

    private static void clearModifier(ServerPlayer player, net.minecraft.world.entity.ai.attributes.Attribute attribute,
            UUID id) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
        }
    }
}
