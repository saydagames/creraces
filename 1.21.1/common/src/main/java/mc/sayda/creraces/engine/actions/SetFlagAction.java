package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.util.GsonHelper;
import mc.sayda.creraces.util.SpiritRealmUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Sets or toggles a boolean identity flag on the player.
 * JSON: { "type": "creraces:set_flag", "flag": "is_spirit", "value": true }
 *       { "type": "creraces:set_flag", "flag": "is_in_spirit_realm", "operation": "toggle", "radius": 3 }
 */
public class SetFlagAction implements ActionRegistry.RaceAction {

    private enum Flag {
        IN_SPIRIT_REALM(IPlayerVariables::isInSpiritRealm, IPlayerVariables::setInSpiritRealm, true,
                "isinspiritrealm", "is_in_spirit_realm", "inspiritrealm"),
        SMALL_BUILD(IPlayerVariables::isSmallBuild, IPlayerVariables::setSmallBuild, false,
                "issmallbuild", "is_small_build", "minibuild"),
        SPIRIT(IPlayerVariables::isSpirit, IPlayerVariables::setSpirit, true,
                "isspirit", "is_spirit", "spirit"),
        TINY(IPlayerVariables::isTiny, IPlayerVariables::setTiny, false,
                "istiny", "is_tiny", "tiny");

        private final Predicate<IPlayerVariables> getter;
        private final BiConsumer<IPlayerVariables, Boolean> setter;
        private final boolean resyncTrackers;
        private final String[] aliases;

        Flag(Predicate<IPlayerVariables> getter, BiConsumer<IPlayerVariables, Boolean> setter,
                boolean resyncTrackers, String... aliases) {
            this.getter = getter;
            this.setter = setter;
            this.resyncTrackers = resyncTrackers;
            this.aliases = aliases;
        }

        @Nullable
        static Flag byName(String name) {
            String key = name.toLowerCase();
            for (Flag flag : values()) {
                for (String alias : flag.aliases) {
                    if (alias.equals(key)) {
                        return flag;
                    }
                }
            }
            return null;
        }

        static String knownNames() {
            return Arrays.stream(values()).map(f -> f.aliases[1]).collect(Collectors.joining(", "));
        }
    }

    private final Flag flag;
    private final boolean value;
    private final boolean toggle;
    private final ScalingValue radius;

    private SetFlagAction(Flag flag, boolean value, boolean toggle, ScalingValue radius) {
        this.flag = flag;
        this.value = value;
        this.toggle = toggle;
        this.radius = radius;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target,
            @Nullable AbilitySlot slot, @Nullable BlockPos interactPos) {
        Boolean next = DataUtils.getVariables(player).map(vars -> toggle ? !flag.getter.test(vars) : value)
                .orElse(null);
        if (next == null) {
            return true;
        }

        apply(player, next);

        // A radius carries every other player in range to the same state, resolved from the caster
        // so a toggle does not flip each of them independently.
        double r = AreaTargets.clampRadius(radius.evaluate(player, target, slot));
        if (r > 0) {
            if (flag == Flag.IN_SPIRIT_REALM) {
                SpiritRealmUtils.applyToNearby(player, r, next);
            } else {
                AABB area = player.getBoundingBox().inflate(r);
                for (Player nearby : player.level().getEntitiesOfClass(Player.class, area)) {
                    if (nearby != player) {
                        apply(nearby, next);
                    }
                }
            }
        }
        return true;
    }

    private void apply(Player player, boolean next) {
        if (flag == Flag.IN_SPIRIT_REALM) {
            SpiritRealmUtils.setInSpiritRealm(player, next);
            return;
        }
        DataUtils.getVariables(player).ifPresent(vars -> {
            if (flag.getter.test(vars) == next) {
                return;
            }
            flag.setter.accept(vars, next);
            if (flag.resyncTrackers) {
                BoundaryHandler.resyncForAllTrackers(player);
            }
            BoundaryHandler.resyncVariables(player, player);
        });
    }

    public static void register() {
        ActionRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "set_flag"), json -> {
            String name = GsonHelper.getAsString(json, "flag", "");
            Flag flag = Flag.byName(name);
            if (flag == null) {
                CreRaces.LOGGER.error("SetFlagAction: unknown flag '{}' - expected one of: {}", name,
                        Flag.knownNames());
                return null;
            }
            String operation = GsonHelper.getAsString(json, "operation", "set");
            boolean toggle = operation.equalsIgnoreCase("toggle");
            if (!toggle && !operation.equalsIgnoreCase("set")) {
                CreRaces.LOGGER.error("SetFlagAction: unknown operation '{}' for flag '{}' - expected set or toggle",
                        operation, name);
                return null;
            }
            return new SetFlagAction(flag, GsonHelper.getAsBoolean(json, "value", false), toggle,
                    ScalingValue.fromJson(json, "radius", 0.0));
        });
    }
}
