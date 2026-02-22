package mc.sayda.creraces.race;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.ability.AbilitySlot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public class ResourceTicker {

    public static void tick(Player player) {
        if (!player.isAlive())
            return;

        Optional<IPlayerVariables> varsOpt = DataUtils.getVariables(player);
        if (varsOpt.isEmpty())
            return;

        IPlayerVariables vars = varsOpt.get();
        ResourceLocation raceId = vars.getRace();
        if (raceId.equals(RaceRegistry.NONE))
            return;

        Race race = RaceRegistry.get(raceId);
        if (race == null)
            return;

        // 1. Tick Cooldowns and Timers (Always run first)
        vars.sakuyaTimeLeap();

        // 2. Resource Regeneration Logic (Every tick)
        double maxMana = player.getAttributeValue(mc.sayda.creraces.registry.ModAttributes.MAX_MANA.get());
        double maxEnergy = player.getAttributeValue(mc.sayda.creraces.registry.ModAttributes.MAX_ENERGY.get());

        // Regeneration (scaled: original/20 per tick)
        double manaRegen = player.getAttributeValue(mc.sayda.creraces.registry.ModAttributes.MANA_REGEN.get());
        double energyRegen = player.getAttributeValue(mc.sayda.creraces.registry.ModAttributes.ENERGY_REGEN.get());

        if (vars.getMana() < maxMana) {
            vars.setMana(Math.min(maxMana, vars.getMana() + manaRegen));
        }
        if (!player.getAbilities().flying && vars.getEnergy() < maxEnergy) {
            vars.setEnergy(Math.min(maxEnergy, vars.getEnergy() + energyRegen));
        }

        // Decay (scaled: original/20 per tick)
        if (vars.getResourceTimer() <= 0) {
            double gritDecay = player.getAttributeValue(mc.sayda.creraces.registry.ModAttributes.GRIT_DECAY.get());
            double rageDecay = player.getAttributeValue(mc.sayda.creraces.registry.ModAttributes.RAGE_DECAY.get());

            if (vars.getGrit() > 0) {
                vars.setGrit(Math.max(0, vars.getGrit() - gritDecay));
            }
            if (vars.getRage() > 0) {
                vars.setRage(Math.max(0, vars.getRage() - rageDecay));
            }
        }

        if (!player.level().isClientSide() && player.tickCount % 40 == 0) {
            BoundaryHandler.resyncVariables(player, player);
        }

        // 4. Tick Race Traits (Moved up for client-side prediction)
        if (race.traits() != null) {
            race.traits().forEach(trait -> trait.tick(player));
        }

        if (player.level().isClientSide())
            return;

        // 5. Sunlight Burning Logic
        if (race.passives() != null && race.passives().burnsInSunlight() && player.level().isDay()
                && !player.level().isThundering()
                && player.level().canSeeSky(player.blockPosition())) {

            boolean immune = race.passives().immuneToDamageTypes().contains("fire") || player.isInvulnerable();

            if (!immune) {
                net.minecraft.world.item.ItemStack headItem = player
                        .getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);

                boolean hasProtection = !headItem.isEmpty() &&
                        net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(
                                mc.sayda.creraces.registry.ModEnchantments.SUN_PROTECTION.get(), player) > 0;

                if (!headItem.isEmpty()) {
                    if (headItem.isDamageableItem() && !hasProtection) {
                        if (player.getRandom().nextFloat() < 0.05F) {
                            headItem.setDamageValue(headItem.getDamageValue() + 1);
                            if (headItem.getDamageValue() >= headItem.getMaxDamage()) {
                                player.broadcastBreakEvent(net.minecraft.world.entity.EquipmentSlot.HEAD);
                                player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,
                                        net.minecraft.world.item.ItemStack.EMPTY);
                            }
                        }
                    }
                } else {
                    player.setSecondsOnFire(8);
                }
            }
        }

        // 6. Passive Effects
        if (race.passives() != null) {
            if (race.passives().nightVision()) {
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.NIGHT_VISION, 220, 0, false, false));
            }
            if (race.passives().waterVision()) {
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.CONDUIT_POWER, 220, 0, false, false));
            }
            if (race.passives().lavaVision()) {
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 220, 0, false, false));
            }

            if (race.passives().noHunger()) {
                player.getFoodData().setFoodLevel(20);
                player.getFoodData().setSaturation(20);
            } else if (race.passives().fixedHunger() > 0) {
                player.getFoodData().setFoodLevel((int) race.passives().fixedHunger());
            }

            if (race.passives().cannotSprint() && player.isSprinting()) {
                player.setSprinting(false);
            }

            if (race.passives().liquidSpeedMultiplier() > 1.0 && (player.isInWater() || player.isInLava())) {
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.DOLPHINS_GRACE, 20, 0, false, false));
            }
        }

    }
}
