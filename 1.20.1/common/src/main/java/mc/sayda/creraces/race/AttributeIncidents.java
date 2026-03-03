package mc.sayda.creraces.race;

import mc.sayda.creraces.capability.DataUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * Handles the application of RP attributes (Eiki's field of judgment).
 */
public class AttributeIncidents {
    // Unique UUID for racial AD modifier (applied from IPlayerVariables.getAd())
    private static final UUID RACE_AD_MODIFIER = UUID.fromString("c0d3b4be-0001-4000-8000-000000000001");

    public static void eikiJudgment(ServerPlayer player) {
        // 1. Clear all racial modifiers first to ensure reset works
        clearAllRacialModifiers(player);

        DataUtils.getVariables(player).ifPresent(vars -> {
            // Attack Damage (AD) -> Vanilla Attack Damage (1% per point)
            AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attackDamage != null) {
                double racialAD = vars.getAd();
                if (racialAD != 0) {
                    attackDamage.addPermanentModifier(new AttributeModifier(
                            RACE_AD_MODIFIER, "Race Base AD", racialAD * 0.01,
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

                // Armor Locking Attributes (Twilight Lib)
                mc.sayda.creraces.race.Race.Passives p = race.passives();
                if (p != null) {
                    applyLockAttribute(player, "twilight_lib:lock_helmet", p.cannotWearHelmet());
                    applyLockAttribute(player, "twilight_lib:lock_chestplate", p.cannotWearChestplate());
                    applyLockAttribute(player, "twilight_lib:lock_leggings", p.cannotWearLeggings());
                    applyLockAttribute(player, "twilight_lib:lock_boots", p.cannotWearBoots());
                }
            }
        });
    }

    private static void applyLockAttribute(ServerPlayer player, String attrId, boolean active) {
        AttributeInstance instance = player.getAttribute(net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE
                .get(new net.minecraft.resources.ResourceLocation(attrId)));
        if (instance != null) {
            UUID id = UUID.nameUUIDFromBytes(("creraces:lock:" + attrId).getBytes());
            instance.removeModifier(id);
            if (active) {
                instance.addPermanentModifier(new AttributeModifier(id, "Race Armor Lock", 1.0,
                        AttributeModifier.Operation.ADDITION));
            }
        }
    }

    private static void clearAllRacialModifiers(ServerPlayer player) {
        // Clear the AD modifier (its own UUID, not the creraces:trait: pattern)
        clearModifier(player, Attributes.ATTACK_DAMAGE, RACE_AD_MODIFIER);

        // All generic trait attribute modifiers are applied via AttributeModifierTrait
        // using deterministic per-attribute UUIDs — iterate syncable attributes to
        // clear them
        player.getAttributes().getSyncableAttributes().forEach(instance -> {
            UUID uuid = UUID
                    .nameUUIDFromBytes(("creraces:trait:" + instance.getAttribute().getDescriptionId()).getBytes());
            if (instance.getModifier(uuid) != null) {
                instance.removeModifier(uuid);
            }
        });

        UUID RESOURCE_MODIFIER_ID = UUID.fromString("c0d3b4be-0001-4000-8000-000000000004");
        clearModifier(player, mc.sayda.creraces.registry.ModAttributes.MAX_MANA.get(), RESOURCE_MODIFIER_ID);
        clearModifier(player, mc.sayda.creraces.registry.ModAttributes.MAX_RAGE.get(), RESOURCE_MODIFIER_ID);
        clearModifier(player, mc.sayda.creraces.registry.ModAttributes.MAX_ENERGY.get(), RESOURCE_MODIFIER_ID);
        clearModifier(player, mc.sayda.creraces.registry.ModAttributes.MAX_GRIT.get(), RESOURCE_MODIFIER_ID);
    }

    private static void applyResourceModifier(ServerPlayer player, mc.sayda.creraces.race.Race race) {
        UUID RESOURCE_MODIFIER_ID = UUID.fromString("c0d3b4be-0001-4000-8000-000000000004");

        clearModifier(player, mc.sayda.creraces.registry.ModAttributes.MAX_MANA.get(), RESOURCE_MODIFIER_ID);
        clearModifier(player, mc.sayda.creraces.registry.ModAttributes.MAX_RAGE.get(), RESOURCE_MODIFIER_ID);
        clearModifier(player, mc.sayda.creraces.registry.ModAttributes.MAX_ENERGY.get(), RESOURCE_MODIFIER_ID);
        clearModifier(player, mc.sayda.creraces.registry.ModAttributes.MAX_GRIT.get(), RESOURCE_MODIFIER_ID);

        DataUtils.getVariables(player).ifPresent(vars -> {
            switch (race.resourceType()) {
                case MANA:
                    applyModifier(player, mc.sayda.creraces.registry.ModAttributes.MAX_MANA.get(),
                            RESOURCE_MODIFIER_ID,
                            race.maxResource(), "Race Mana");
                    break;
                case RAGE:
                    applyModifier(player, mc.sayda.creraces.registry.ModAttributes.MAX_RAGE.get(),
                            RESOURCE_MODIFIER_ID,
                            race.maxResource(), "Race Rage");
                    break;
                case ENERGY:
                    double max = race.maxResource();
                    if (race.stacksAffectResource()) {
                        max -= vars.getStacks();
                    }
                    applyModifier(player, mc.sayda.creraces.registry.ModAttributes.MAX_ENERGY.get(),
                            RESOURCE_MODIFIER_ID,
                            Math.max(1, max), "Race Energy");
                    break;
                case GRIT:
                    applyModifier(player, mc.sayda.creraces.registry.ModAttributes.MAX_GRIT.get(),
                            RESOURCE_MODIFIER_ID,
                            race.maxResource(), "Race Grit");
                    break;
                default:
                    break;
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
