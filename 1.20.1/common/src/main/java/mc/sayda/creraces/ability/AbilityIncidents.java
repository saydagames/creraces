package mc.sayda.creraces.ability;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import mc.sayda.creraces.network.BoundaryHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

/**
 * Handles the casting flow for abilities on the server.
 */
public class AbilityIncidents {

    public static void tryCast(ServerPlayer player, AbilitySlot slot) {
        DataUtils.getVariables(player).ifPresent(vars -> {
            ResourceLocation abilityId = vars.getAbilityInSlot(slot);
            if (abilityId == null)
                return;

            Ability ability = AbilityRegistry.get(abilityId);

            if (ability == null)
                return;

            // 1. Check Cooldown
            ResourceLocation cooldownId = ability.id();
            if (vars.getCooldown(cooldownId) > 0) {
                player.displayClientMessage(
                        (Component) Component.translatable("msg.creraces.cooldown", (Object) ability.name()), true);
                return;
            }

            // 2. Check Resource Cost
            Race race = RaceRegistry.get(vars.getRace());
            if (race == null)
                return;

            if (!canAfford(vars, race, ability.cost())) {
                player.displayClientMessage((Component) Component.translatable("msg.creraces.no_resource"), true);
                return;
            }

            // 3. Find Executor
            AbilityExecutor executor = AbilityExecutionRegistry.get(abilityId);
            if (executor == null) {
                // Ability might not have logic yet, but we should still notify or log
                player.displayClientMessage(
                        (Component) Component.translatable("msg.creraces.no_executor", (Object) ability.name()), true);
                return;
            }

            // 4. Execute Logic
            try {
                if (executor.execute(player, ability, slot)) {
                    // 5. Post-Cast (Consume cost and set cooldown)
                    consumeResource(vars, race, ability.cost());

                    double haste = player
                            .getAttributeValue(mc.sayda.creraces.registry.ModAttributes.ABILITY_HASTE.get());
                    double cap = 40.0; // ABILITY_HASTE_CAP default
                    double effectiveHaste = Math.min(haste, cap);
                    double multiplier = 1.0 - (effectiveHaste / 100.0);
                    int cooledTicks = (int) (ability.cooldown() * multiplier);
                    vars.setCooldown(abilityId, Math.max(0, cooledTicks));

                    // Sync to client
                    BoundaryHandler.resyncVariables(player, player);

                    // 6. Trigger traits
                    if (race.traits() != null) {
                        for (mc.sayda.creraces.engine.TraitRegistry.RaceTrait trait : race.traits()) {
                            trait.onAbilityUse(player, ability);
                        }
                    }
                }
            } catch (Exception e) {
                mc.sayda.creraces.CreRaces.LOGGER.error("Failed to execute ability: " + abilityId, e);
            }
        });
    }

    private static boolean canAfford(mc.sayda.creraces.capability.IPlayerVariables vars, Race race, int cost) {
        if (cost <= 0)
            return true;
        return switch (race.resourceType()) {
            case MANA -> vars.getMana() >= cost;
            case RAGE -> vars.getRage() >= cost;
            case ENERGY -> vars.getEnergy() >= cost;
            case GRIT -> vars.getGrit() >= cost;
            case SOULS -> vars.getSouls() >= cost;
            default -> true;
        };
    }

    private static void consumeResource(mc.sayda.creraces.capability.IPlayerVariables vars, Race race, int cost) {
        if (cost <= 0)
            return;
        switch (race.resourceType()) {
            case MANA -> vars.setMana(Math.max(0, vars.getMana() - cost));
            case RAGE -> vars.setRage(Math.max(0, vars.getRage() - cost));
            case ENERGY -> vars.setEnergy(Math.max(0, vars.getEnergy() - cost));
            case GRIT -> vars.setGrit(Math.max(0, vars.getGrit() - cost));
            case SOULS -> vars.setSouls(Math.max(0, vars.getSouls() - cost));
            case NONE -> {
            }
            default -> {
            }
        }
    }
}
