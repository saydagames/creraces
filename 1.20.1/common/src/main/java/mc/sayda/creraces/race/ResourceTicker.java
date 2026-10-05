package mc.sayda.creraces.race;

import mc.sayda.creraces.ability.Ability;
import mc.sayda.creraces.ability.AbilityRegistry;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.ability.AbilityType;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.engine.ActionRegistry.RaceAction;
import mc.sayda.creraces.engine.TraitDispatch;
import mc.sayda.creraces.engine.actions.BeamAction;
import mc.sayda.creraces.engine.actions.TetherAction;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.registry.ModAttributes;
import mc.sayda.creraces.registry.ModEnchantments;
import mc.sayda.creraces.registry.ModMobEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Per-tick race upkeep. Runs on both sides so the client can predict resources and cooldowns
 * between syncs; auto-run abilities, sunlight, hunger passives and syncing are server-only.
 */
public class ResourceTicker {

    // Only the first two slots auto-run their innate/passive abilities.
    private static final AbilitySlot[] AUTO_RUN_SLOTS = { AbilitySlot.A1, AbilitySlot.A2 };

    public static void tick(Player player) {
        if (!player.isAlive())
            return;

        Optional<IPlayerVariables> varsOpt = DataUtils.getVariables(player);
        if (varsOpt.isEmpty())
            return;

        IPlayerVariables vars = varsOpt.get();

        ResourceLocation raceId = vars.getRace();
        if (raceId == null || raceId.equals(RaceRegistry.NONE))
            return;

        Race race = RaceRegistry.get(raceId);
        if (race == null)
            return;

        vars.sakuyaTimeLeap();

        if (!tickResourcePools(player, vars))
            return;

        tickChannelledAbility(player, vars, race);
        TetherAction.tickTethers(player);

        if (player instanceof ServerPlayer serverPlayer && player.tickCount % 20 == 0) {
            // Re-evaluated every second so conditional and scaling modifiers stay current.
            AttributeIncidents.eikiJudgment(serverPlayer);

            // Delta sync only: the client predicts resources itself, and full syncs are sent
            // on join, respawn, cast and combat resource events.
            BoundaryHandler.resyncVariables(player, player, false);
        }

        TraitDispatch.runVoid("ResourceTicker", player, trait -> trait.tick(player));

        if (!player.level().isClientSide()
                && player.tickCount % Math.max(1, CreRacesConfig.PASSIVE_EXECUTION_INTERVAL.get()) == 0) {
            runAutoAbilities(player, vars);
        }

        if (player.level().isClientSide())
            return;

        Race.Passives passives = race.passives();
        if (passives == null)
            return;

        tickSunlightBurn(player, passives);
        applyHungerAndSprintPassives(player, passives);
    }

    /**
     * Applies per-tick regeneration and, once the post-activity grace period has passed, decay.
     * Returns false if a resource attribute is missing, which ends the whole tick early.
     */
    private static boolean tickResourcePools(Player player, IPlayerVariables vars) {
        var maxManaAttr = ModAttributes.resolve(ModAttributes.MAX_MANA);
        var maxEnergyAttr = ModAttributes.resolve(ModAttributes.MAX_ENERGY);
        var manaRegenAttr = ModAttributes.resolve(ModAttributes.MANA_REGEN);
        var energyRegenAttr = ModAttributes.resolve(ModAttributes.ENERGY_REGEN);
        if (maxManaAttr == null || maxEnergyAttr == null || manaRegenAttr == null || energyRegenAttr == null)
            return false;
        double maxMana = player.getAttributeValue(maxManaAttr);
        double maxEnergy = player.getAttributeValue(maxEnergyAttr);
        double manaRegen = player.getAttributeValue(manaRegenAttr);
        double energyRegen = player.getAttributeValue(energyRegenAttr);

        if (vars.getMana() < maxMana) {
            vars.setMana(Math.min(maxMana, vars.getMana() + manaRegen));
        }
        if (!player.getAbilities().flying && vars.getEnergy() < maxEnergy) {
            vars.setEnergy(Math.min(maxEnergy, vars.getEnergy() + energyRegen));
        }

        long graceThreshold = CreRacesConfig.RESOURCE_DECAY_GRACE_PERIOD.get();
        boolean inGracePeriod = (player.level().getGameTime() - vars.getResourceTimer()) < graceThreshold;

        var gritDecayAttr = ModAttributes.resolve(ModAttributes.GRIT_DECAY);
        var rageDecayAttr = ModAttributes.resolve(ModAttributes.RAGE_DECAY);
        if (gritDecayAttr == null || rageDecayAttr == null)
            return false;
        double gritDecay = player.getAttributeValue(gritDecayAttr);
        double rageDecay = player.getAttributeValue(rageDecayAttr);

        if (!inGracePeriod) {
            if (vars.getGrit() > 0) {
                vars.setGrit(Math.max(0, vars.getGrit() - gritDecay));
            }
            if (vars.getRage() > 0) {
                vars.setRage(Math.max(0, vars.getRage() - rageDecay));
            }
        }
        return true;
    }

    private static void tickChannelledAbility(Player player, IPlayerVariables vars, Race race) {
        if (!vars.isAbilityActive())
            return;
        ResourceLocation abilityId = vars.getActiveAbility();
        if (abilityId == null)
            return;

        // Running dry already ended the channel this tick, so its duration is left alone.
        if (!drainChannelCost(player, vars, race, abilityId)) {
            tickChannelDuration(player, vars, abilityId);
        }

        BeamAction.tickExecution(player, abilityId);
    }

    /** Drains the channel's cost from the race's own resource. Returns true if that ended the channel. */
    private static boolean drainChannelCost(Player player, IPlayerVariables vars, Race race, ResourceLocation abilityId) {
        double drain = vars.getActiveAbilityDrain();
        if (drain <= 0)
            return false;
        Ability ability = AbilityRegistry.get(abilityId);
        if (ability == null)
            return false;
        if (!drainResource(vars, race.resourceType(), drain))
            return false;

        deactivateAbility(player, vars, abilityId, ability, vars.getSlotForAbility(abilityId));
        return true;
    }

    /** Returns true if the pool is empty after draining. */
    private static boolean drainResource(IPlayerVariables vars, ResourceType type, double amount) {
        return switch (type) {
            case MANA -> {
                vars.setMana(Math.max(0, vars.getMana() - amount));
                yield vars.getMana() <= 0;
            }
            case RAGE -> {
                vars.setRage(Math.max(0, vars.getRage() - amount));
                yield vars.getRage() <= 0;
            }
            case ENERGY -> {
                vars.setEnergy(Math.max(0, vars.getEnergy() - amount));
                yield vars.getEnergy() <= 0;
            }
            case GRIT -> {
                vars.setGrit(Math.max(0, vars.getGrit() - amount));
                yield vars.getGrit() <= 0;
            }
            case SOUL -> {
                vars.setSoul(Math.max(0, vars.getSoul() - amount));
                yield vars.getSoul() <= 0;
            }
            default -> false;
        };
    }

    private static void tickChannelDuration(Player player, IPlayerVariables vars, ResourceLocation abilityId) {
        int remaining = vars.getActiveAbilityDuration();
        if (remaining <= 0)
            return;
        vars.setActiveAbilityDuration(remaining - 1);
        if (remaining == 1) {
            deactivateAbility(player, vars, abilityId, AbilityRegistry.get(abilityId), vars.getSlotForAbility(abilityId));
        }
    }

    /** Innate and passive abilities re-run their on_activate actions on a fixed interval. */
    private static void runAutoAbilities(Player player, IPlayerVariables vars) {
        for (AbilitySlot slot : AUTO_RUN_SLOTS) {
            ResourceLocation abilityId = vars.getAbilityInSlot(slot);
            if (abilityId == null)
                continue;
            Ability ability = AbilityRegistry.get(abilityId);
            if (ability == null || ability.onActivate() == null)
                continue;
            if (ability.type() != AbilityType.INNATE && ability.type() != AbilityType.PASSIVE)
                continue;
            for (RaceAction action : ability.onActivate()) {
                action.execute(player, null, slot, null);
            }
        }
    }

    private static void tickSunlightBurn(Player player, Race.Passives passives) {
        int interval = passives.sunlightBurnInterval();
        if (interval < 0 || !player.level().isDay() || player.level().isRaining()
                || !player.level().canSeeSky(player.blockPosition()))
            return;
        if (isImmuneToSunlight(player, passives))
            return;

        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.isEmpty()) {
            if (player.tickCount % 20 == 0) {
                player.setSecondsOnFire(CreRacesConfig.SUNLIGHT_BURN_SECONDS.get());
            }
            return;
        }

        // A helmet takes the sunlight instead of the wearer and wears down unless it is protected.
        if (interval > 0 && player.tickCount % interval == 0 && helmet.isDamageableItem() && !hasSunProtection(player)) {
            helmet.setDamageValue(helmet.getDamageValue() + 1);
            if (helmet.getDamageValue() >= helmet.getMaxDamage()) {
                player.broadcastBreakEvent(EquipmentSlot.HEAD);
                player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            }
        }
    }

    private static boolean isImmuneToSunlight(Player player, Race.Passives passives) {
        MobEffect sunResistance = ModMobEffects.SUN_RESISTANCE.get();
        return passives.immuneToDamageTypes().contains("minecraft:fire")
                || player.isInvulnerable()
                || (sunResistance != null && player.hasEffect(sunResistance));
    }

    private static boolean hasSunProtection(Player player) {
        Enchantment sunProtection = ModEnchantments.SUN_PROTECTION.get();
        return sunProtection != null && EnchantmentHelper.getEnchantmentLevel(sunProtection, player) > 0;
    }

    private static void applyHungerAndSprintPassives(Player player, Race.Passives passives) {
        if (passives.noHunger()) {
            player.getFoodData().setFoodLevel(CreRacesConfig.PASSIVE_DEFAULT_MAX_FOOD.get());
            player.getFoodData().setSaturation(CreRacesConfig.PASSIVE_DEFAULT_MAX_SATURATION.get().floatValue());
        } else if (passives.fixedHunger() != null) {
            double fixedHunger = passives.fixedHunger().evaluate(player);
            if (fixedHunger > 0) {
                player.getFoodData().setFoodLevel((int) fixedHunger);
            }
        }

        if (passives.cannotSprint() && player.isSprinting()) {
            player.setSprinting(false);
        }
    }

    private static void deactivateAbility(Player player, IPlayerVariables vars, ResourceLocation abilityId,
            @Nullable Ability ability, @Nullable AbilitySlot slot) {
        vars.setAbilityActive(false);
        if (ability != null) {
            vars.setCooldown(abilityId, ability.cooldown());
            if (ability.onDeactivate() != null) {
                for (RaceAction action : ability.onDeactivate()) {
                    action.execute(player, null, slot, null);
                }
            }
        }
    }
}
