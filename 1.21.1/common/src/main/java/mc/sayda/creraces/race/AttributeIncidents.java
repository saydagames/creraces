package mc.sayda.creraces.race;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.engine.AttributeMethod;
import mc.sayda.creraces.engine.ManagedModifier;
import mc.sayda.creraces.engine.TraitDispatch;
import mc.sayda.creraces.engine.TraitRegistry.RaceTrait;
import mc.sayda.creraces.engine.traits.AttributeModifierTrait;
import mc.sayda.creraces.registry.ModAttributes;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Applies racial attribute modifiers: racial AD, trait modifiers, the double jump grant and
 * AP-based mana scaling.
 */
@SuppressWarnings("null")
public class AttributeIncidents {
    // Fixed ids so each modifier is found and replaced, never stacked, across checks and saves.
    private static final ResourceLocation RACE_AD_MODIFIER = ResourceLocation.fromNamespaceAndPath("creraces", "race_ad");
    private static final ResourceLocation MANA_AP_MODIFIER = ResourceLocation.fromNamespaceAndPath("creraces", "mana_ap_scaling");
    private static final ResourceLocation EQUIP_DOUBLE_JUMP_MODIFIER = ResourceLocation.fromNamespaceAndPath("creraces", "equip_double_jump");

    private static final ResourceLocation DOUBLE_JUMP_ABILITY = ResourceLocation.fromNamespaceAndPath("creraces", "double_jump");
    private static final double MAX_MANA_PER_AP = 0.3;
    private static final double AMOUNT_EPSILON = 1e-6;

    public static void eikiJudgment(ServerPlayer player) {
        DataUtils.getVariables(player).ifPresent(vars -> {
            Race race = RaceRegistry.get(vars.getRace());
            if (race == null) {
                purgeRacialAttributes(player);
                return;
            }

            applyRacialAttackDamage(player, vars);
            applyTraitModifiers(player, vars);
            syncManagedModifiers(player, vars);
            applyDoubleJump(player, vars);
            applyManaScaling(player, vars);
        });
    }

    /** Racial AD multiplies vanilla attack damage, scaled by RACIAL_AD_MULTIPLIER. */
    private static void applyRacialAttackDamage(ServerPlayer player, IPlayerVariables vars) {
        AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage == null)
            return;

        double amount = vars.getAd() * CreRacesConfig.RACIAL_AD_MULTIPLIER.get();
        AttributeModifier.Operation op = AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
        AttributeModifier existing = attackDamage.getModifier(RACE_AD_MODIFIER);
        if (needsUpdate(existing, amount, op)) {
            if (existing != null)
                attackDamage.removeModifier(RACE_AD_MODIFIER);
            if (amount != 0) {
                attackDamage.addPermanentModifier(new AttributeModifier(RACE_AD_MODIFIER, amount, op));
            }
        }
    }

    private static void applyTraitModifiers(ServerPlayer player, IPlayerVariables vars) {
        for (RaceTrait trait : TraitDispatch.forPlayer(player)) {
            if (!(trait instanceof AttributeModifierTrait amt))
                continue;
            // Already resolved to the Apothic equivalent where one exists.
            Holder<Attribute> attribute = amt.getAttribute();
            if (attribute == null)
                continue;

            String traitId = trait.getTraitId();
            ResourceLocation modifierId = ManagedModifier.idOf(traitId);

            if (amt.getMethod() == AttributeMethod.REMOVE) {
                AttributeInstance instance = player.getAttribute(attribute);
                if (instance != null && instance.getModifier(modifierId) != null) {
                    instance.removeModifier(modifierId);
                    vars.removeManagedModifier(modifierId);
                    CreRaces.LOGGER.debug("AttributeIncidents: Trait REMOVED {} from {}", traitId, player.getScoreboardName());
                }
            } else if (amt.isManaged()) {
                registerManagedModifier(player, vars, amt, modifierId, traitId);
            } else {
                applyStaticTraitModifier(player, amt, attribute, modifierId, traitId);
            }
        }
    }

    /**
     * Managed modifiers are applied by syncManagedModifiers on their own interval. Re-registering
     * one restarts that interval, so it only happens when the trait's value or condition changed.
     */
    private static void registerManagedModifier(ServerPlayer player, IPlayerVariables vars,
            AttributeModifierTrait amt, ResourceLocation modifierId, String traitId) {
        Optional<ManagedModifier> existing = vars.getManagedModifier(modifierId);
        if (existing.isEmpty()) {
            vars.addManagedModifier(createManagedModifier(player, amt, modifierId, traitId));
            CreRaces.LOGGER.debug("AttributeIncidents: Registered new Managed modifier {}", traitId);
            return;
        }

        ManagedModifier mod = existing.get();
        JsonObject rawCondition = amt.getRawCondition();
        boolean changed = !mod.valueJson().equals(amt.getValueJson())
                || (rawCondition != null && !mod.conditionJson().equals(rawCondition));
        if (changed) {
            vars.addManagedModifier(createManagedModifier(player, amt, modifierId, traitId));
            CreRaces.LOGGER.debug("AttributeIncidents: Updated data for Managed modifier {}", traitId);
        }
    }

    private static ManagedModifier createManagedModifier(ServerPlayer player, AttributeModifierTrait amt,
            ResourceLocation modifierId, String traitId) {
        JsonObject rawCondition = amt.getRawCondition();
        return new ManagedModifier(
                modifierId, amt.getAttributeId(), amt.getValueJson(), amt.getOperation(),
                "creraces:" + traitId,
                rawCondition != null ? rawCondition : new JsonObject(),
                rawCondition != null, amt.getInterval(), player.tickCount + amt.getInterval());
    }

    /** Unmanaged trait modifiers follow their condition on every check. */
    private static void applyStaticTraitModifier(ServerPlayer player, AttributeModifierTrait amt,
            Holder<Attribute> attribute, ResourceLocation modifierId, String traitId) {
        boolean conditionMet = amt.getCondition() == null
                || amt.getCondition().evaluate(player, null, null, null);
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null)
            return;

        if (!conditionMet) {
            if (instance.getModifier(modifierId) != null) {
                instance.removeModifier(modifierId);
                CreRaces.LOGGER.debug("EikiJudgment: Removed static trait {} from {} (Condition failed)", traitId, player.getScoreboardName());
            }
            return;
        }

        double amount = amt.getValue().evaluate(player);
        if (ModAttributes.isPercentAttribute(attribute))
            amount /= 100.0;
        AttributeModifier.Operation op = amt.getOperation();
        AttributeModifier existing = instance.getModifier(modifierId);
        if (needsUpdate(existing, amount, op)) {
            if (existing != null)
                instance.removeModifier(modifierId);
            instance.addPermanentModifier(new AttributeModifier(modifierId, amount, op));
            CreRaces.LOGGER.debug("EikiJudgment: Applied static trait {} to {}", traitId, player.getScoreboardName());
        }
    }

    /**
     * Re-evaluates managed modifiers whose check interval has elapsed. Lifecycle modifiers whose
     * condition no longer holds are removed for good.
     */
    private static void syncManagedModifiers(ServerPlayer player, IPlayerVariables vars) {
        List<ResourceLocation> expired = new ArrayList<>();
        for (ManagedModifier mod : vars.getManagedModifiers()) {
            if (!mod.shouldCheck(player.tickCount))
                continue;
            vars.addManagedModifier(mod.withNextCheck(player.tickCount));

            Holder<Attribute> attribute = ModAttributes.getAttribute(mod.attributeId());
            if (attribute == null)
                continue;
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null)
                continue;

            if (mod.hasLifecycle() && mod.getCondition() != null && !mod.getCondition().evaluate(player, null, null, null)) {
                expired.add(mod.id());
                instance.removeModifier(mod.id());
                CreRaces.LOGGER.debug("ManagedModifier: Purged lifecycle modifier {} from {}", mod.name(), player.getScoreboardName());
                continue;
            }

            double amount = mod.getScalingValue().evaluate(player);
            if (ModAttributes.isPercentAttribute(attribute))
                amount /= 100.0;
            AttributeModifier existing = instance.getModifier(mod.id());
            if (needsUpdate(existing, amount, mod.operation())) {
                if (existing != null)
                    instance.removeModifier(mod.id());
                instance.addPermanentModifier(new AttributeModifier(mod.id(), amount, mod.operation()));
                CreRaces.LOGGER.debug("ManagedModifier: Synced value for {} on {} (val: {})", mod.name(), player.getScoreboardName(), amount);
            }
        }
        expired.forEach(vars::removeManagedModifier);
    }

    /** Equipping the double_jump ability grants the attribute that enables the jump. */
    private static void applyDoubleJump(ServerPlayer player, IPlayerVariables vars) {
        AttributeInstance doubleJump = player.getAttribute(ModAttributes.DOUBLE_JUMP);
        if (doubleJump == null)
            return;

        boolean equipped = false;
        for (AbilitySlot slot : AbilitySlot.values()) {
            if (DOUBLE_JUMP_ABILITY.equals(vars.getAbilityInSlot(slot))) {
                equipped = true;
                break;
            }
        }

        AttributeModifier existing = doubleJump.getModifier(EQUIP_DOUBLE_JUMP_MODIFIER);
        if (equipped && existing == null) {
            doubleJump.addPermanentModifier(new AttributeModifier(EQUIP_DOUBLE_JUMP_MODIFIER,
                    1.0, AttributeModifier.Operation.ADD_VALUE));
            CreRaces.LOGGER.debug("EikiJudgment: Applied Double Jump modifier to {}", player.getScoreboardName());
        } else if (!equipped && existing != null) {
            doubleJump.removeModifier(EQUIP_DOUBLE_JUMP_MODIFIER);
            CreRaces.LOGGER.debug("EikiJudgment: Removed Double Jump modifier from {}", player.getScoreboardName());
        }
    }

    private static void applyManaScaling(ServerPlayer player, IPlayerVariables vars) {
        AttributeInstance maxMana = player.getAttribute(ModAttributes.resolve(ModAttributes.MAX_MANA));
        if (maxMana == null)
            return;

        double amount = vars.getAp() * MAX_MANA_PER_AP;
        AttributeModifier.Operation op = AttributeModifier.Operation.ADD_VALUE;
        AttributeModifier existing = maxMana.getModifier(MANA_AP_MODIFIER);
        if (needsUpdate(existing, amount, op)) {
            if (existing != null)
                maxMana.removeModifier(MANA_AP_MODIFIER);
            if (amount != 0) {
                maxMana.addPermanentModifier(new AttributeModifier(MANA_AP_MODIFIER, amount, op));
            }
        }
    }

    // Skipping unchanged modifiers avoids a remove/add, and the attribute sync that comes with it, every check.
    private static boolean needsUpdate(@Nullable AttributeModifier existing, double amount, AttributeModifier.Operation op) {
        return existing == null || Math.abs(existing.amount() - amount) > AMOUNT_EPSILON || existing.operation() != op;
    }

    /**
     * Strips every racial modifier from the player. Called on race resets and transformations,
     * before the new race applies its own.
     */
    public static void purgeRacialAttributes(ServerPlayer player) {
        clearModifier(player, ModAttributes.resolve(ModAttributes.DOUBLE_JUMP), EQUIP_DOUBLE_JUMP_MODIFIER);
        clearModifier(player, Attributes.ATTACK_DAMAGE, RACE_AD_MODIFIER);
        clearModifier(player, ModAttributes.resolve(ModAttributes.MAX_MANA), MANA_AP_MODIFIER);

        DataUtils.getVariables(player).ifPresent(vars -> {
            for (ManagedModifier mod : vars.getManagedModifiers()) {
                Holder<Attribute> attribute = ModAttributes.getAttribute(mod.attributeId());
                if (attribute != null) {
                    clearModifier(player, attribute, mod.id());
                }
            }
            vars.clearManagedModifiers();
        });

        // Static trait modifiers are not tracked anywhere. Modifiers are keyed by
        // ResourceLocation since 1.21, so our namespace identifies them.
        player.getAttributes().getSyncableAttributes().forEach(instance -> {
            List<ResourceLocation> toRemove = new ArrayList<>();
            instance.getModifiers().forEach(mod -> {
                if (mod.id().getNamespace().equals(CreRaces.MODID)) {
                    toRemove.add(mod.id());
                }
            });
            toRemove.forEach(instance::removeModifier);
        });

        CreRaces.LOGGER.debug("AttributeIncidents: Performed global attribute purge for {}", player.getScoreboardName());
    }

    private static void clearModifier(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
        }
    }
}
