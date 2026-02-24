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
    // Unique UUIDs for each race attribute modifier type
    private static final UUID RACE_AD_MODIFIER_ID = UUID.fromString("c0d3b4be-0001-4000-8000-000000000001");
    private static final UUID RACE_MAX_HEALTH_MODIFIER_ID = UUID.fromString("c0d3b4be-0001-4000-8000-000000000002");
    private static final UUID RACE_MOVEMENT_SPEED_MODIFIER_ID = UUID.fromString("c0d3b4be-0001-4000-8000-000000000003");
    private static final UUID RACE_LUCK_MODIFIER_ID = UUID.fromString("c0d3b4be-0001-4000-8000-000000000010");
    private static final UUID RACE_ARMOR_MODIFIER_ID = UUID.fromString("c0d3b4be-0001-4000-8000-000000000011");
    private static final UUID RACE_ARMOR_TOUGHNESS_MODIFIER_ID = UUID
            .fromString("c0d3b4be-0001-4000-8000-000000000012");
    private static final UUID RACE_ATTACK_SPEED_MODIFIER_ID = UUID.fromString("c0d3b4be-0001-4000-8000-000000000013");
    private static final UUID RACE_ATTACK_KNOCKBACK_MODIFIER_ID = UUID
            .fromString("c0d3b4be-0001-4000-8000-000000000014");

    public static void eikiJudgment(ServerPlayer player) {
        // 1. Clear all racial modifiers first to ensure reset works
        clearAllRacialModifiers(player);

        DataUtils.getVariables(player).ifPresent(vars -> {
            // Attack Damage (AD) -> Vanilla Attack Damage (Always apply from vars)
            AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attackDamage != null) {
                double racialAD = vars.getAd();
                if (racialAD > 0) {
                    attackDamage.addPermanentModifier(new AttributeModifier(
                            RACE_AD_MODIFIER_ID, "Race Base AD", racialAD,
                            AttributeModifier.Operation.ADDITION));
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
        clearModifier(player, Attributes.ATTACK_DAMAGE, RACE_AD_MODIFIER_ID);
        clearModifier(player, Attributes.MAX_HEALTH, RACE_MAX_HEALTH_MODIFIER_ID);
        clearModifier(player, Attributes.MOVEMENT_SPEED, RACE_MOVEMENT_SPEED_MODIFIER_ID);
        clearModifier(player, Attributes.LUCK, RACE_LUCK_MODIFIER_ID);
        clearModifier(player, Attributes.ARMOR, RACE_ARMOR_MODIFIER_ID);
        clearModifier(player, Attributes.ARMOR_TOUGHNESS, RACE_ARMOR_TOUGHNESS_MODIFIER_ID);
        clearModifier(player, Attributes.ATTACK_SPEED, RACE_ATTACK_SPEED_MODIFIER_ID);
        clearModifier(player, Attributes.ATTACK_KNOCKBACK, RACE_ATTACK_KNOCKBACK_MODIFIER_ID);

        // Clear generic dynamic traits (Iterate all likely attributes or just standard
        // ones?
        // We can't easily iterate all attributes on the player efficiently to find ours
        // without a list.
        // But we can iterate the *Standard* attributes we support.)
        // Better: Iterate all attributes on the player and remove modifiers that start
        // with "Race Trait"?
        // No, modifier names aren't reliable for logic, UUIDs are.

        // Strategy: We will use deterministic UUIDs per Attribute for the "Active Race
        // Trait".
        // Use the same UUID generation as in application, but we need to cover all
        // possible attributes.
        // Since we don't know *which* attributes the previous race touched, this is
        // tricky.

        // Fallback: If we use the "Same UUID per Attribute" strategy:
        // We still need to know WHICH attributes to clear.
        // We can assume standard set + maybe iterate the Player's AttributeMap?

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

    private static void applyVanillaModifier(ServerPlayer player,
            net.minecraft.world.entity.ai.attributes.Attribute attribute,
            UUID id, double amount, String name) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
            if (amount != 0) {
                instance.addPermanentModifier(new AttributeModifier(
                        id, name, amount, AttributeModifier.Operation.ADDITION));
            }
        }
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
                    if (race.id().toString().contains("lycan")) {
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
