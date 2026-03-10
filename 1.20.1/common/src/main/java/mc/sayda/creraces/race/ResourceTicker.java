package mc.sayda.creraces.race;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.network.BoundaryHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.List;

@SuppressWarnings("null")
public class ResourceTicker {

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

        // 1. Tick Cooldowns and Timers (Always run first)
        vars.sakuyaTimeLeap();

        // 2. Resource Regeneration Logic (Every tick)
        double maxMana = player.getAttributeValue(mc.sayda.creraces.registry.ModAttributes.MAX_MANA.get());
        double maxEnergy = player.getAttributeValue(mc.sayda.creraces.registry.ModAttributes.MAX_ENERGY.get());

        // Regeneration (absolute per tick)
        double manaRegen = player.getAttributeValue(mc.sayda.creraces.registry.ModAttributes.MANA_REGEN.get());
        double energyRegen = player.getAttributeValue(mc.sayda.creraces.registry.ModAttributes.ENERGY_REGEN.get());

        if (vars.getMana() < maxMana) {
            vars.setMana(Math.min(maxMana, vars.getMana() + manaRegen));
        }
        if (!player.getAbilities().flying && vars.getEnergy() < maxEnergy) {
            vars.setEnergy(Math.min(maxEnergy, vars.getEnergy() + energyRegen));
        }

        // Decay (absolute per tick)
        double gritDecay = player.getAttributeValue(mc.sayda.creraces.registry.ModAttributes.GRIT_DECAY.get());
        double rageDecay = player.getAttributeValue(mc.sayda.creraces.registry.ModAttributes.RAGE_DECAY.get());

        if (vars.getGrit() > 0) {
            vars.setGrit(Math.max(0, vars.getGrit() - gritDecay));
        }
        if (vars.getRage() > 0) {
            vars.setRage(Math.max(0, vars.getRage() - rageDecay));
        }

        // Channeled Ability Logic
        if (vars.isAbilityActive()) {
            ResourceLocation abilityId = vars.getActiveAbility();
            if (abilityId != null) {
                // 1. Drain Resource (use race's actual resource type, not always mana)
                double drain = vars.getActiveAbilityDrain();
                if (drain > 0) {
                    mc.sayda.creraces.ability.Ability ability = mc.sayda.creraces.ability.AbilityRegistry
                            .get(abilityId);
                    if (ability != null) {
                        boolean outOfResource = switch (race.resourceType()) {
                            case MANA -> {
                                vars.setMana(Math.max(0, vars.getMana() - drain));
                                yield vars.getMana() <= 0;
                            }
                            case RAGE -> {
                                vars.setRage(Math.max(0, vars.getRage() - drain));
                                yield vars.getRage() <= 0;
                            }
                            case ENERGY -> {
                                vars.setEnergy(Math.max(0, vars.getEnergy() - drain));
                                yield vars.getEnergy() <= 0;
                            }
                            case GRIT -> {
                                vars.setGrit(Math.max(0, vars.getGrit() - drain));
                                yield vars.getGrit() <= 0;
                            }
                            case SOULS -> {
                                vars.setSouls(Math.max(0, vars.getSouls() - drain));
                                yield vars.getSouls() <= 0;
                            }
                            default -> false;
                        };
                        if (outOfResource) {
                            deactivateAbility(player, vars, abilityId, ability);
                        }
                    }
                }

                // 2. Tick Duration
                int remaining = vars.getActiveAbilityDuration();
                if (remaining > 0) {
                    vars.setActiveAbilityDuration(remaining - 1);
                    if (remaining - 1 <= 0) {
                        mc.sayda.creraces.ability.Ability ability = mc.sayda.creraces.ability.AbilityRegistry
                                .get(abilityId);
                        deactivateAbility(player, vars, abilityId, ability);
                    }
                }

                // 3. Periodic Execution (Beam)
                mc.sayda.creraces.engine.actions.BeamAction.tickExecution(player, abilityId);
            }
        }

        // 3.5. Tick Active Tethers
        mc.sayda.creraces.engine.actions.TetherAction.tickTethers(player);

        if (!player.level().isClientSide()
                && player.tickCount
                        % mc.sayda.creraces.config.CreRacesConfig.TICK_AUTHORITATIVE_SYNC_INTERVAL.get() == 0
                && player instanceof net.minecraft.server.level.ServerPlayer sp) {

            if (mc.sayda.creraces.config.CreRacesConfig.ATTRIBUTE_RECHECK_ENABLED.get()) {
                AttributeIncidents.eikiJudgment(sp);
            }

            // Delta sync only — resources excluded. Client predicts resources.
            // Full sync only happens on join, respawn, cast, and combat resource events.
            BoundaryHandler.resyncVariables(player, player, false);
        }

        // 4. Tick Race Traits (Moved up for client-side prediction)
        List<mc.sayda.creraces.engine.TraitRegistry.RaceTrait> traits = race.traits();
        if (traits != null) {
            for (int i = 0; i < traits.size(); i++) {
                traits.get(i).tick(player);
            }
        }

        // 5. Auto-apply Innate/Passive Abilities (Server only)
        if (!player.level().isClientSide()
                && player.tickCount % mc.sayda.creraces.config.CreRacesConfig.PASSIVE_EXECUTION_INTERVAL.get() == 0) {
            // Only tick A1 and A2
            for (mc.sayda.creraces.ability.AbilitySlot slot : new mc.sayda.creraces.ability.AbilitySlot[] {
                    mc.sayda.creraces.ability.AbilitySlot.A1, mc.sayda.creraces.ability.AbilitySlot.A2 }) {
                ResourceLocation abilityId = vars.getAbilityInSlot(slot);
                if (abilityId != null) {
                    mc.sayda.creraces.ability.Ability ability = mc.sayda.creraces.ability.AbilityRegistry
                            .get(abilityId);
                    if (ability != null && (ability.type() == mc.sayda.creraces.ability.AbilityType.INNATE
                            || ability.type() == mc.sayda.creraces.ability.AbilityType.PASSIVE)) {
                        // Innates/Passives auto-execute on_activate actions periodically
                        if (ability.onActivate() != null) {
                            for (mc.sayda.creraces.engine.ActionRegistry.RaceAction action : ability.onActivate()) {
                                action.execute(player, null, null, null);
                            }
                        }
                    }
                }
            }
        }

        if (player.level().isClientSide())
            return;

        // 6. Sunlight Burning Logic
        Race.Passives passives = race.passives();
        if (passives != null && passives.burnsInSunlight() && player.level().isDay()
                && !player.level().isRaining() // isRaining covers both rain and thunder; isThundering() missed light
                                               // rain
                && player.level().canSeeSky(player.blockPosition())) {

            boolean immune = passives.immuneToDamageTypes().contains("fire") || player.isInvulnerable();

            if (!immune) {
                net.minecraft.world.item.ItemStack headItem = player
                        .getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);

                boolean hasProtection = !headItem.isEmpty() &&
                        net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(
                                mc.sayda.creraces.registry.ModEnchantments.SUN_PROTECTION.get(), player) > 0;

                if (!headItem.isEmpty()) {
                    if (headItem.isDamageableItem() && !hasProtection) {
                        if (player.getRandom()
                                .nextFloat() < mc.sayda.creraces.config.CreRacesConfig.SUNLIGHT_EQUIPMENT_BREAK_CHANCE
                                        .get()) {
                            headItem.setDamageValue(headItem.getDamageValue() + 1);
                            if (headItem.getDamageValue() >= headItem.getMaxDamage()) {
                                player.broadcastBreakEvent(net.minecraft.world.entity.EquipmentSlot.HEAD);
                                player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,
                                        net.minecraft.world.item.ItemStack.EMPTY);
                            }
                        }
                    }
                } else {
                    player.setSecondsOnFire(mc.sayda.creraces.config.CreRacesConfig.SUNLIGHT_BURN_SECONDS.get());
                }
            }
        }

        // 6. Passive Effects
        if (passives != null) {
            if (passives.nightVision()) {
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.NIGHT_VISION,
                        mc.sayda.creraces.config.CreRacesConfig.PASSIVE_EFFECT_BUFFER_TICKS.get(), 0, false, false));
            }

            if (passives.noHunger()) {
                player.getFoodData()
                        .setFoodLevel(20);
                player.getFoodData()
                        .setSaturation(20.0f);
            } else if (passives.fixedHunger() != null) {
                double fixedH = passives.fixedHunger().evaluate(player);
                if (fixedH > 0) {
                    player.getFoodData().setFoodLevel((int) fixedH);
                }
            }

            if (passives.cannotSprint() && player.isSprinting()) {
                player.setSprinting(false);
            }

        }

    }

    private static void deactivateAbility(Player player, IPlayerVariables vars, ResourceLocation abilityId,
            mc.sayda.creraces.ability.Ability ability) {
        vars.setAbilityActive(false);
        if (ability != null) {
            vars.setCooldown(abilityId, ability.cooldown());
            if (ability.onDeactivate() != null) {
                for (mc.sayda.creraces.engine.ActionRegistry.RaceAction action : ability.onDeactivate()) {
                    action.execute(player, null, null, null);
                }
            }
        }
    }

}
