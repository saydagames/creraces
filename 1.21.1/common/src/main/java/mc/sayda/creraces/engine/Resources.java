package mc.sayda.creraces.engine;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.race.AttributeIncidents;
import mc.sayda.creraces.registry.ModAttributes;
import mc.sayda.creraces.util.IFoodDataAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.ToDoubleFunction;

/**
 * The one list of named resources the engine reads and writes: modify_value, "resource:" scaling
 * values, flight drain and overlay bars all look names up here. Race resources live on players
 * only; health and air work on any living entity. Mana, energy, grit and rage stay between 0 and
 * their max attribute. Attributes (ability power and the like) are not
 * resources: they are read as stats and changed with attribute modifiers.
 */
@SuppressWarnings("null")
public final class Resources {
    private static final Map<String, Resource> BY_NAME = new LinkedHashMap<>();

    static {
        pool("mana", "max_mana", IPlayerVariables::getMana, IPlayerVariables::setMana);
        pool("energy", "max_energy", IPlayerVariables::getEnergy, IPlayerVariables::setEnergy);
        pool("grit", "max_grit", IPlayerVariables::getGrit, IPlayerVariables::setGrit);
        pool("rage", "max_rage", IPlayerVariables::getRage, IPlayerVariables::setRage);
        race("karma", IPlayerVariables::getKarma, IPlayerVariables::setKarma);
        race("coins", IPlayerVariables::getCoins, IPlayerVariables::setCoins);
        race("passive_cd", IPlayerVariables::getPassiveCooldown, IPlayerVariables::setPassiveCooldown);
        race("gstate", vars -> vars.getGState(), null);
        BY_NAME.put("soul", new Resource("soul", true,
                entity -> vars(entity) != null ? vars(entity).getSoul() : 0,
                (entity, value) -> {
                    IPlayerVariables vars = vars(entity);
                    if (vars != null) {
                        vars.setSoul(Math.max(0, Math.min(value, CreRacesConfig.MAX_SOUL.get())));
                        // Attribute modifiers can scale with soul, so they are re-applied straight away.
                        if (entity instanceof ServerPlayer serverPlayer) {
                            AttributeIncidents.eikiJudgment(serverPlayer);
                        }
                    }
                }));
        BY_NAME.put("health", new Resource("health", false, LivingEntity::getHealth,
                (entity, value) -> entity.setHealth((float) Math.max(0, Math.min(value, entity.getMaxHealth())))));
        BY_NAME.put("air", new Resource("air", false, LivingEntity::getAirSupply,
                (entity, value) -> entity.setAirSupply((int) Math.max(0, Math.min(value, entity.getMaxAirSupply())))));
        BY_NAME.put("food", new Resource("food", true,
                entity -> entity instanceof Player p ? food(p).creraces$getFoodLevel() : 0,
                (entity, value) -> {
                    if (entity instanceof Player p) {
                        food(p).creraces$setFoodLevel(
                                (int) Math.max(0, Math.min(value, CreRacesConfig.PASSIVE_DEFAULT_MAX_FOOD.get())));
                    }
                }));
        BY_NAME.put("saturation", new Resource("saturation", true,
                entity -> entity instanceof Player p ? food(p).creraces$getSaturation() : 0,
                (entity, value) -> {
                    if (entity instanceof Player p) {
                        food(p).creraces$setSaturation((float) Math.max(0, value));
                    }
                }));
    }

    private Resources() {
    }

    /** One named resource. A null writer makes it read-only. */
    public record Resource(String name, boolean playerOnly, ToDoubleFunction<LivingEntity> reader,
            @Nullable BiConsumer<LivingEntity, Double> writer) {

        public double read(LivingEntity entity) {
            return playerOnly && !(entity instanceof Player) ? 0 : reader.applyAsDouble(entity);
        }

        /**
         * Changes the value without syncing it, for writes every tick that the regular delta sync
         * picks up. Returns false when the entity can't hold this resource or it is read-only.
         */
        public boolean set(LivingEntity entity, double value) {
            if (writer == null || playerOnly && !(entity instanceof Player)) {
                return false;
            }
            writer.accept(entity, value);
            return true;
        }

        /** Changes the value and syncs it to the player straight away. */
        public boolean write(LivingEntity entity, double value) {
            if (!set(entity, value)) {
                return false;
            }
            if (entity instanceof Player player) {
                BoundaryHandler.resyncVariables(player, player);
            }
            return true;
        }
    }

    @Nullable
    public static Resource get(String name) {
        return BY_NAME.get(name);
    }

    public static String knownNames() {
        return String.join(", ", BY_NAME.keySet());
    }

    /** A race resource that fills up to the player's {@code maxAttribute}; writes are clamped to 0 and that. */
    private static void pool(String name, String maxAttribute, ToDoubleFunction<IPlayerVariables> getter,
            BiConsumer<IPlayerVariables, Double> setter) {
        BY_NAME.put(name, new Resource(name, true,
                entity -> {
                    IPlayerVariables vars = vars(entity);
                    return vars != null ? getter.applyAsDouble(vars) : 0;
                },
                (entity, value) -> {
                    IPlayerVariables vars = vars(entity);
                    if (vars != null) {
                        setter.accept(vars, Math.max(0, Math.min(value, attributeValue(entity, maxAttribute))));
                    }
                }));
    }

    /** One of our attributes on the entity; no limit if the entity doesn't have it. */
    private static double attributeValue(LivingEntity entity, String path) {
        var attribute = ModAttributes.getAttribute(CreRaces.id(path));
        return attribute != null && entity.getAttributes().hasAttribute(attribute)
                ? entity.getAttributeValue(attribute)
                : Double.MAX_VALUE;
    }

    private static void race(String name, ToDoubleFunction<IPlayerVariables> getter,
            @Nullable BiConsumer<IPlayerVariables, Double> setter) {
        BY_NAME.put(name, new Resource(name, true,
                entity -> {
                    IPlayerVariables vars = vars(entity);
                    return vars != null ? getter.applyAsDouble(vars) : 0;
                },
                setter == null ? null : (entity, value) -> {
                    IPlayerVariables vars = vars(entity);
                    if (vars != null) {
                        setter.accept(vars, value);
                    }
                }));
    }

    @Nullable
    private static IPlayerVariables vars(LivingEntity entity) {
        return entity instanceof Player player ? DataUtils.getVariables(player).orElse(null) : null;
    }

    private static IFoodDataAccessor food(Player player) {
        return (IFoodDataAccessor) player.getFoodData();
    }
}
