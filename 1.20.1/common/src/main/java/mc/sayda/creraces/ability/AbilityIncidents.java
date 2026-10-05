package mc.sayda.creraces.ability;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.engine.ActionRegistry.RaceAction;
import mc.sayda.creraces.engine.TraitDispatch;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import mc.sayda.creraces.network.BoundaryHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Objects;

/**
 * Handles the casting flow for abilities on the server.
 */
public class AbilityIncidents {
    private static final double DEFAULT_PICK_RANGE = 5.0;

    public static void tryCast(ServerPlayer player, AbilitySlot slot) {
        Objects.requireNonNull(player, "Player cannot be null for ability casting.");
        Objects.requireNonNull(slot, "AbilitySlot cannot be null for ability casting.");

        DataUtils.getVariables(player).ifPresent(vars -> {
            ResourceLocation abilityId = vars.getAbilityInSlot(slot);
            if (abilityId == null)
                return;

            Ability ability = AbilityRegistry.get(abilityId);
            if (ability == null)
                return;

            if (vars.getCooldown(ability.id()) > 0) {
                var name = ability.name();
                if (name != null) {
                    player.displayClientMessage(Objects.requireNonNull(Component.translatable("msg.creraces.cooldown", (Object) name)), true);
                }
                return;
            }

            Race race = RaceRegistry.get(vars.getRace());
            if (race == null)
                return;

            if (!canAfford(vars, race, ability.cost())) {
                player.displayClientMessage(Objects.requireNonNull(Component.translatable("msg.creraces.no_resource")), true);
                return;
            }

            if (ability.condition() != null && !ability.condition().evaluate(player, null, slot, null)) {
                String failKey = ability.conditionFailMessage() != null
                        ? ability.conditionFailMessage()
                        : "msg.creraces.condition_failed";
                player.displayClientMessage(Component.translatable(failKey), true);
                return;
            }

            try {
                if (runJsonActions(player, ability, slot)) {
                    consumeResource(vars, race, ability.cost());

                    // getAh() reads Apothic's cooldown_reduction instead when that mod is installed.
                    double effectiveHaste = Math.min(vars.getAh(), CreRacesConfig.ABILITY_HASTE_CAP.get());
                    int cooledTicks = (int) (ability.cooldown() * (1.0 - effectiveHaste / 100.0));

                    // An action may have set its own cooldown during execution; keep that one.
                    if (vars.getCooldown(abilityId) <= 0) {
                        vars.setCooldown(abilityId, Math.max(0, cooledTicks));
                    }

                    BoundaryHandler.resyncVariables(player, player);

                    TraitDispatch.runVoid("AbilityIncidents", player, trait -> trait.onAbilityUse(player, ability));
                }
            } catch (Exception e) {
                CreRaces.LOGGER.error("Failed to execute ability: {}", abilityId, e);
            }
        });
    }

    /** Runs the on_activate actions in order, stopping at the first one that fails. */
    private static boolean runJsonActions(ServerPlayer player, Ability ability, AbilitySlot slot) {
        if (ability.onActivate() == null || ability.onActivate().isEmpty())
            return false;

        // Actions using use_target_block target the space in front of the block face being looked at.
        BlockPos lookTarget = null;
        HitResult hit = player.pick(DEFAULT_PICK_RANGE, 0f, false);
        if (hit instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK) {
            lookTarget = bhr.getBlockPos().relative(bhr.getDirection());
        }

        CreRaces.LOGGER.debug("AbilityIncidents: Executing {} actions for ability {}",
                ability.onActivate().size(), ability.id());
        for (RaceAction action : ability.onActivate()) {
            if (!action.execute(player, null, slot, lookTarget)) {
                CreRaces.LOGGER.debug("AbilityIncidents: Action failed, stopping execution for ability {}", ability.id());
                return false;
            }
        }
        return true;
    }

    private static boolean canAfford(IPlayerVariables vars, Race race, int cost) {
        if (cost <= 0)
            return true;
        return switch (race.resourceType()) {
            case MANA -> vars.getMana() >= cost;
            case RAGE -> vars.getRage() >= cost;
            case ENERGY -> vars.getEnergy() >= cost;
            case GRIT -> vars.getGrit() >= cost;
            case SOUL -> vars.getSoul() >= cost;
            default -> true;
        };
    }

    private static void consumeResource(IPlayerVariables vars, Race race, int cost) {
        if (cost <= 0)
            return;
        switch (race.resourceType()) {
            case MANA -> vars.setMana(Math.max(0, vars.getMana() - cost));
            case RAGE -> vars.setRage(Math.max(0, vars.getRage() - cost));
            case ENERGY -> vars.setEnergy(Math.max(0, vars.getEnergy() - cost));
            case GRIT -> vars.setGrit(Math.max(0, vars.getGrit() - cost));
            case SOUL -> vars.setSoul(Math.max(0, vars.getSoul() - cost));
            default -> {
            }
        }
    }
}
