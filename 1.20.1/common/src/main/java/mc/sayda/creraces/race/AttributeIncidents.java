package mc.sayda.creraces.race;

import mc.sayda.creraces.capability.DataUtils;
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
    private static final UUID RESOURCE_MODIFIER = UUID.fromString("c0d3b4be-0001-4000-8000-000000000004");
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
                if (existing == null || Math.abs(existing.getAmount() - amount) > 1e-6 || existing.getOperation() != op) {
                    if (existing != null) attackDamage.removeModifier(RACE_AD_MODIFIER);
                    if (amount != 0) {
                        attackDamage.addPermanentModifier(new AttributeModifier(RACE_AD_MODIFIER, "CreRaces Base AD", amount, op));
                    }
                }
            }

            // 2. Generic Trait Application
            java.util.Set<UUID> activeTraits = new java.util.HashSet<>();
            
            for (mc.sayda.creraces.engine.TraitRegistry.RaceTrait trait : race.traits()) {
                if (trait instanceof mc.sayda.creraces.engine.traits.AttributeModifierTrait amt) {
                    boolean conditionMet = amt.getCondition() == null || amt.getCondition().evaluate(player, null, null, null);
                    
                    if (conditionMet) {
                        Attribute attr = amt.getAttribute();
                        ResourceLocation attrKey = net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.getKey(attr);
                        if (attrKey != null) {
                            String traitId = trait.getTraitId();
                            // Deterministic UUID based on trait ID (managed by user in JSON)
                            UUID uuid = UUID.nameUUIDFromBytes(("creraces:" + traitId).getBytes());
                            activeTraits.add(uuid);
                            
                            AttributeInstance instance = player.getAttribute(attr);
                            if (instance != null) {
                                double newValue = amt.getValue().evaluate(player);
                                AttributeModifier.Operation newOp = amt.getOperation();
                                AttributeModifier existing = instance.getModifier(uuid);

                                if (existing == null || Math.abs(existing.getAmount() - newValue) > 1e-6 || existing.getOperation() != newOp) {
                                    mc.sayda.creraces.CreRaces.LOGGER.debug("EikiJudgment: {} trait {} for {}. Value: {}, Op: {}", 
                                            existing == null ? "Applying" : "Updating", traitId, player.getScoreboardName(), newValue, newOp);
                                    if (existing != null) instance.removeModifier(uuid);
                                    
                                    String modifierName = "CreRaces Trait";
                                    if (traitId != null && !traitId.isEmpty() && !traitId.contains(":")) {
                                        modifierName = "CreRaces: " + traitId;
                                    }
                                    
                                    instance.addPermanentModifier(new AttributeModifier(uuid, modifierName, newValue, newOp));
                                    
                                    // Verify application
                                    if (instance.getModifier(uuid) == null) {
                                        mc.sayda.creraces.CreRaces.LOGGER.error("EikiJudgment: FAILED to set modifier {} ({}) on {}", traitId, attrKey, player.getScoreboardName());
                                    }
                                }
                            } else if (player.tickCount % 200 == 0) {
                                mc.sayda.creraces.CreRaces.LOGGER.warn("EikiJudgment: Player {} lacks attribute: {}", player.getScoreboardName(), attrKey);
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
                    mc.sayda.creraces.CreRaces.LOGGER.debug("EikiJudgment: Clearing {} orphaned race traits from {}", toRemove.size(), player.getScoreboardName());
                    toRemove.forEach(mod -> instance.removeModifier(mod.getId()));
                }
            });

            // 4. Resources
            applyResourceModifier(player, race);

            // 5. Double Jump
            var doubleJumpAttr = player.getAttribute(mc.sayda.creraces.registry.ModAttributes.DOUBLE_JUMP.get());
            if (doubleJumpAttr != null) {
                boolean isEquipped = false;
                for (var slot : mc.sayda.creraces.ability.AbilitySlot.values()) {
                    if (new ResourceLocation("creraces", "double_jump").equals(vars.getAbilityInSlot(slot))) {
                        isEquipped = true;
                        break;
                    }
                }

                AttributeModifier existing = doubleJumpAttr.getModifier(EQUIP_DOUBLE_JUMP_MODIFIER);
                if (isEquipped) {
                    if (existing == null) {
                        doubleJumpAttr.addPermanentModifier(new AttributeModifier(EQUIP_DOUBLE_JUMP_MODIFIER, "Double Jump Ability", 1.0, AttributeModifier.Operation.ADDITION));
                        mc.sayda.creraces.CreRaces.LOGGER.debug("EikiJudgment: Applied Double Jump modifier to {}", player.getScoreboardName());
                    }
                } else if (existing != null) {
                    doubleJumpAttr.removeModifier(EQUIP_DOUBLE_JUMP_MODIFIER);
                    mc.sayda.creraces.CreRaces.LOGGER.debug("EikiJudgment: Removed Double Jump modifier from {}", player.getScoreboardName());
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

        clearModifier(player, mc.sayda.creraces.registry.ModAttributes.MAX_MANA.get(), RESOURCE_MODIFIER);
        clearModifier(player, mc.sayda.creraces.registry.ModAttributes.MAX_RAGE.get(), RESOURCE_MODIFIER);
        clearModifier(player, mc.sayda.creraces.registry.ModAttributes.MAX_ENERGY.get(), RESOURCE_MODIFIER);
        clearModifier(player, mc.sayda.creraces.registry.ModAttributes.MAX_GRIT.get(), RESOURCE_MODIFIER);
        clearModifier(player, mc.sayda.creraces.registry.ModAttributes.DOUBLE_JUMP.get(),
                EQUIP_DOUBLE_JUMP_MODIFIER);
    }

    private static void applyResourceModifier(ServerPlayer player, mc.sayda.creraces.race.Race race) {
        DataUtils.getVariables(player).ifPresent(vars -> {
            net.minecraft.world.entity.ai.attributes.Attribute targetAttr;
            String name;
            double amount = race.maxResource();

            switch (race.resourceType()) {
                case MANA -> { targetAttr = mc.sayda.creraces.registry.ModAttributes.MAX_MANA.get(); name = "Race Mana"; }
                case RAGE -> { targetAttr = mc.sayda.creraces.registry.ModAttributes.MAX_RAGE.get(); name = "Race Rage"; }
                case ENERGY -> { 
                    targetAttr = mc.sayda.creraces.registry.ModAttributes.MAX_ENERGY.get(); name = "Race Energy";
                    if (race.stacksAffectResource()) amount -= vars.getStacks();
                    amount = Math.max(mc.sayda.creraces.config.CreRacesConfig.RESOURCE_MIN_CAPACITY.get(), amount);
                }
                case GRIT -> { targetAttr = mc.sayda.creraces.registry.ModAttributes.MAX_GRIT.get(); name = "Race Grit"; }
                default -> { targetAttr = null; name = ""; }
            }

            // Clear from all resource attributes first
            for (var attr : new net.minecraft.world.entity.ai.attributes.Attribute[]{
                mc.sayda.creraces.registry.ModAttributes.MAX_MANA.get(),
                mc.sayda.creraces.registry.ModAttributes.MAX_RAGE.get(),
                mc.sayda.creraces.registry.ModAttributes.MAX_ENERGY.get(),
                mc.sayda.creraces.registry.ModAttributes.MAX_GRIT.get()
            }) {
                if (attr != targetAttr) clearModifier(player, attr, RESOURCE_MODIFIER);
            }

            if (targetAttr != null) {
                AttributeInstance instance = player.getAttribute(targetAttr);
                if (instance != null) {
                    AttributeModifier existing = instance.getModifier(RESOURCE_MODIFIER);
                    if (existing == null || Math.abs(existing.getAmount() - amount) > 1e-6) {
                        if (existing != null) instance.removeModifier(RESOURCE_MODIFIER);
                        instance.addPermanentModifier(new AttributeModifier(RESOURCE_MODIFIER, name, amount, AttributeModifier.Operation.ADDITION));
                    }
                }
            }
        });
    }

    private static void clearModifier(ServerPlayer player, net.minecraft.world.entity.ai.attributes.Attribute attribute,
            UUID id) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
        }
    }
}
