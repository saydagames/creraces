package mc.sayda.creraces.race;

import mc.sayda.creraces.capability.DataUtils;
import net.minecraft.server.level.ServerPlayer;
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

            // 1. Clear all racial modifiers first to ensure reset works
            clearAllRacialModifiers(player);
            // Attack Damage (AD) -> Vanilla Attack Damage (1% per point)
            AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attackDamage != null) {
                double racialAD = vars.getAd();
                if (racialAD != 0) {
                    attackDamage.addPermanentModifier(new AttributeModifier(
                            RACE_AD_MODIFIER, "Race Base AD",
                            racialAD * mc.sayda.creraces.config.CreRacesConfig.RACIAL_AD_MULTIPLIER.get(),
                            AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
            }

            // Fetch Race to apply other modifiers
            mc.sayda.creraces.race.Race race = mc.sayda.creraces.race.RaceRegistry.get(vars.getRace());
            if (race != null) {
                // Attribute modifiers are now handled purely via
                // mc.sayda.creraces.engine.traits.AttributeModifierTrait
                // which is applied in the traits loop below.

                // Generic Trait Application
                for (mc.sayda.creraces.engine.TraitRegistry.RaceTrait trait : race.traits()) {
                    if (trait instanceof mc.sayda.creraces.engine.traits.AttributeModifierTrait amt) {
                        // Check condition if present
                        if (amt.getCondition() != null && !amt.getCondition().evaluate(player, null, null, null)) {
                            continue;
                        }

                        // Use uniform UUID per attribute (Only one race active at a time)
                        String descId = amt.getAttribute().getDescriptionId();
                        if (descId != null) { // Type check, though getDescriptionId likely returns non-null
                            UUID uuid = UUID.nameUUIDFromBytes(("creraces:trait:" + descId).getBytes());
                            AttributeInstance instance = player.getAttribute(amt.getAttribute());
                            if (instance != null) {
                                if (instance.getModifier(uuid) == null) {
                                    instance.addPermanentModifier(new AttributeModifier(uuid, "Race Trait",
                                            amt.getValue().evaluate(player), amt.getOperation()));
                                }
                            }
                        }
                    }
                }

                // Resources (Dynamic)
                applyResourceModifier(player, race);

                // Double Jump Passive Ability Support
                var doubleJumpAttr = player.getAttribute(mc.sayda.creraces.registry.ModAttributes.DOUBLE_JUMP.get());
                if (doubleJumpAttr != null) {
                    boolean isEquipped = false;
                    for (var slot : mc.sayda.creraces.ability.AbilitySlot.values()) {
                        if (new net.minecraft.resources.ResourceLocation("creraces", "double_jump")
                                .equals(vars.getAbilityInSlot(slot))) {
                            isEquipped = true;
                            break;
                        }
                    }

                    if (isEquipped) {
                        if (doubleJumpAttr.getModifier(EQUIP_DOUBLE_JUMP_MODIFIER) == null) {
                            doubleJumpAttr.addPermanentModifier(new AttributeModifier(EQUIP_DOUBLE_JUMP_MODIFIER,
                                    "Double Jump Ability", 1.0, AttributeModifier.Operation.ADDITION));
                        }
                    }
                }
            }
        });
    }

    private static void clearAllRacialModifiers(ServerPlayer player) {
        // Clear the AD modifier (its own UUID, not the creraces:trait: pattern)
        clearModifier(player, Attributes.ATTACK_DAMAGE, RACE_AD_MODIFIER);

        // All generic trait attribute modifiers are applied via AttributeModifierTrait
        // using deterministic per-attribute UUIDs — iterate only if needed?
        // Actually, the loop is necessary to ensure cleanup of all racial traits
        // when switching races.
        player.getAttributes().getSyncableAttributes().forEach(instance -> {
            String descId = instance.getAttribute().getDescriptionId();
            if (descId != null) {
                UUID uuid = UUID.nameUUIDFromBytes(("creraces:trait:" + descId).getBytes());
                if (instance.getModifier(uuid) != null) {
                    instance.removeModifier(uuid);
                }
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
        clearModifier(player, mc.sayda.creraces.registry.ModAttributes.MAX_MANA.get(), RESOURCE_MODIFIER);
        clearModifier(player, mc.sayda.creraces.registry.ModAttributes.MAX_RAGE.get(), RESOURCE_MODIFIER);
        clearModifier(player, mc.sayda.creraces.registry.ModAttributes.MAX_ENERGY.get(), RESOURCE_MODIFIER);
        clearModifier(player, mc.sayda.creraces.registry.ModAttributes.MAX_GRIT.get(), RESOURCE_MODIFIER);

        DataUtils.getVariables(player).ifPresent(vars -> {
            switch (race.resourceType()) {
                case MANA -> applyModifier(player, mc.sayda.creraces.registry.ModAttributes.MAX_MANA.get(),
                        RESOURCE_MODIFIER,
                        race.maxResource(), "Race Mana");
                case RAGE -> applyModifier(player, mc.sayda.creraces.registry.ModAttributes.MAX_RAGE.get(),
                        RESOURCE_MODIFIER,
                        race.maxResource(), "Race Rage");
                case ENERGY -> {
                    double max = race.maxResource();
                    if (race.stacksAffectResource()) {
                        max -= vars.getStacks();
                    }
                    applyModifier(player, mc.sayda.creraces.registry.ModAttributes.MAX_ENERGY.get(),
                            RESOURCE_MODIFIER,
                            Math.max(mc.sayda.creraces.config.CreRacesConfig.RESOURCE_MIN_CAPACITY.get(), max),
                            "Race Energy");
                }
                case GRIT -> applyModifier(player, mc.sayda.creraces.registry.ModAttributes.MAX_GRIT.get(),
                        RESOURCE_MODIFIER,
                        race.maxResource(), "Race Grit");
                default -> {
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

    private static void applyModifier(ServerPlayer player, net.minecraft.world.entity.ai.attributes.Attribute attribute,
            UUID id, double amount, String name) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.addPermanentModifier(
                    new AttributeModifier(id, name, amount, AttributeModifier.Operation.ADDITION));
        }
    }
}
