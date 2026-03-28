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
                clearAllRacialModifiers(player);
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

            // 2. Generic Trait Application
            java.util.Set<UUID> activeTraits = new java.util.HashSet<>();

            for (mc.sayda.creraces.engine.TraitRegistry.RaceTrait trait : race.traits()) {
                if (trait instanceof mc.sayda.creraces.engine.traits.AttributeModifierTrait amt) {
                    boolean conditionMet = amt.getCondition() == null
                            || amt.getCondition().evaluate(player, null, null, null);

                    if (conditionMet) {
                        Attribute attr = amt.getAttribute();
                        if (attr == null) {
                            if (player.tickCount % 100 == 0)
                                mc.sayda.creraces.CreRaces.LOGGER.warn(
                                    "EikiJudgment: trait '{}' references attribute '{}' which is not in the registry. Is the mod loaded?",
                                    trait.getTraitId(), amt.getAttributeId());
                            continue;
                        }

                        Attribute resolvedAttr = ModAttributes.resolve(attr);
                        ResourceLocation attrKey = net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE
                                .getKey(resolvedAttr);
                        if (attrKey != null) {
                            String traitId = trait.getTraitId();
                            // Deterministic UUID based on trait ID (managed by user in JSON)
                            UUID uuid = UUID.nameUUIDFromBytes(("creraces:" + traitId).getBytes());
                            activeTraits.add(uuid);

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

                                    String modifierName = "CreRaces Trait";
                                    if (traitId != null && !traitId.isEmpty() && !traitId.contains(":")) {
                                        modifierName = "CreRaces: " + traitId;
                                    }

                                    AttributeModifier newMod = new AttributeModifier(uuid, modifierName, newValue, newOp);
                                    instance.addPermanentModifier(newMod);

                                    // Verify application and log final value
                                    double finalValue = instance.getValue();
                                    mc.sayda.creraces.CreRaces.LOGGER.info(
                                            "EikiJudgment: SET trait {} for {}. Mod: {} ({}), Final Value: {}",
                                            traitId, player.getScoreboardName(), newValue, newOp, finalValue);

                                    if (instance.getModifier(uuid) == null) {
                                        mc.sayda.creraces.CreRaces.LOGGER.error(
                                                "EikiJudgment: FAILED to set modifier {} ({}) on {}", traitId, attrKey,
                                                player.getScoreboardName());
                                    }
                                }
                            } else if (player.tickCount % 20 == 0) {
                                mc.sayda.creraces.CreRaces.LOGGER.warn("EikiJudgment: Player {} lacks attribute: {}",
                                        player.getScoreboardName(), attrKey);
                            }
                        }
                    }
                }
            }

            // 3. Clear orphaned "CreRaces Trait" modifiers
            player.getAttributes().getSyncableAttributes().forEach(instance -> {
                java.util.List<AttributeModifier> toRemove = new java.util.ArrayList<>();
                for (AttributeModifier mod : instance.getModifiers()) {
                    String name = mod.getName();
                    if ((name.startsWith("CreRaces:") || "CreRaces Trait".equals(name) || "Race Trait".equals(name))
                            && !activeTraits.contains(mod.getId())) {
                        toRemove.add(mod);
                    }
                }
                if (!toRemove.isEmpty()) {
                    mc.sayda.creraces.CreRaces.LOGGER.debug("EikiJudgment: Clearing {} orphaned race traits from {}",
                            toRemove.size(), player.getScoreboardName());
                    toRemove.forEach(mod -> instance.removeModifier(mod.getId()));
                }
            });

            // 5. Double Jump
            AttributeInstance doubleJumpAttr = player.getAttribute(mc.sayda.creraces.registry.ModAttributes.DOUBLE_JUMP.get());
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

    private static void clearAllRacialModifiers(ServerPlayer player) {
        // Clear the AD modifier (its own UUID, not the creraces:trait: pattern)
        clearModifier(player, Attributes.ATTACK_DAMAGE, RACE_AD_MODIFIER);

        // All generic trait attribute modifiers are applied via AttributeModifierTrait
        // using deterministic per-attribute-and-index UUIDs.
        // We clear everything named "Race Trait" to ensure complete cleanup.
        player.getAttributes().getSyncableAttributes().forEach(instance -> {
            java.util.List<AttributeModifier> toRemove = new java.util.ArrayList<>();
            for (AttributeModifier mod : instance.getModifiers()) {
                String name = mod.getName();
                if (name.startsWith("CreRaces:") || "CreRaces Trait".equals(name) || "Race Trait".equals(name)) {
                    toRemove.add(mod);
                }
            }
            for (AttributeModifier mod : toRemove) {
                instance.removeModifier(mod.getId());
            }
        });

        clearModifier(player, ModAttributes.resolve(ModAttributes.DOUBLE_JUMP),
                EQUIP_DOUBLE_JUMP_MODIFIER);
    }

    private static void clearModifier(ServerPlayer player, net.minecraft.world.entity.ai.attributes.Attribute attribute,
            UUID id) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
        }
    }
}
