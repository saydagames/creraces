package mc.sayda.creraces.engine.condition;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.engine.Comparison;
import mc.sayda.creraces.engine.EntitySelector;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.WorldState;
import mc.sayda.creraces.territory.ClaimData;
import mc.sayda.creraces.territory.DiplomacyStatus;
import mc.sayda.creraces.territory.TerritoryManager;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/**
 * Conditions about the world: weather, time and the spirit moon, the values compare and modulo, and
 * where an entity is (biome, temperature, dimension, claimed land). compare checks any number a
 * scaling value can read, from states and cooldowns to an entity's height. The location ones look
 * at the target when there is one and at the caster otherwise.
 */
@SuppressWarnings("null")
final class WorldConditions {
    private WorldConditions() {
    }

    static void register() {
        ConditionRegistry.register(ConditionRegistry.id("weather"), json -> {
            String weather = GsonHelper.getAsString(json, "weather");
            return switch (weather) {
                case "rain" -> (player, target, slot, pos) -> player.level().isRaining();
                case "thunder" -> (player, target, slot, pos) -> player.level().isThundering();
                case "clear" -> (player, target, slot, pos) -> !player.level().isRaining();
                default -> throw new IllegalArgumentException("unknown weather '" + weather + "' (rain, thunder, clear)");
            };
        });

        ConditionRegistry.register(ConditionRegistry.id("time"), json -> {
            ScalingValue min = ScalingValue.fromJson(json, "min", 0.0);
            ScalingValue max = ScalingValue.fromJson(json, "max", 24000.0);
            return (player, target, slot, pos) -> {
                long time = player.level().getDayTime() % Level.TICKS_PER_DAY;
                return time >= min.evaluate(player, target, slot, pos) && time <= max.evaluate(player, target, slot, pos);
            };
        });

        ConditionRegistry.register(ConditionRegistry.id("spirit_moon"), json -> {
            boolean expected = GsonHelper.getAsBoolean(json, "value", true);
            return (player, target, slot, pos) -> WorldState.isSpiritMoon(player.level()) == expected;
        });

        ConditionRegistry.register(ConditionRegistry.id("compare"), json -> {
            ScalingValue value = ScalingValue.fromJson(json, "value", 0.0);
            ScalingValue to = ScalingValue.fromJson(json, "to", 0.0);
            Comparison comparison = ConditionRegistry.operator(json);
            return (player, target, slot, pos) -> comparison.test(value.evaluate(player, target, slot, pos),
                    to.evaluate(player, target, slot, pos));
        });

        // Whole-number check, e.g. even/odd: value mod divisor == remainder.
        ConditionRegistry.register(ConditionRegistry.id("modulo"), json -> {
            ScalingValue value = ScalingValue.fromJson(json, "value", 0.0);
            ScalingValue divisor = ScalingValue.fromJson(json, "divisor", 2.0);
            ScalingValue remainder = ScalingValue.fromJson(json, "remainder", 0.0);
            return (player, target, slot, pos) -> {
                int div = (int) divisor.evaluate(player, target, slot, pos);
                return div != 0 && Math.floorMod((int) value.evaluate(player, target, slot, pos), div)
                        == (int) remainder.evaluate(player, target, slot, pos);
            };
        });

        EntityConditions.register("biome", EntitySelector.AUTO, json -> {
            String biome = GsonHelper.getAsString(json, "biome");
            if (biome.startsWith("#")) {
                ResourceLocation tagId = ResourceLocation.tryParse(biome.substring(1));
                if (tagId == null) {
                    throw new IllegalArgumentException("malformed biome tag '" + biome + "'");
                }
                TagKey<Biome> tag = TagKey.create(Registries.BIOME, tagId);
                return (entity, caster, target, slot, pos) -> entity.level().getBiome(entity.blockPosition()).is(tag);
            }
            ResourceLocation biomeId = ResourceLocation.tryParse(biome);
            if (biomeId == null) {
                throw new IllegalArgumentException("malformed biome id '" + biome + "'");
            }
            return (entity, caster, target, slot, pos) -> {
                Holder<Biome> holder = entity.level().getBiome(entity.blockPosition());
                return holder.unwrapKey().map(key -> key.location().equals(biomeId)).orElse(false);
            };
        });

        EntityConditions.register("biome_temperature", EntitySelector.AUTO, json -> {
            ScalingValue min = ScalingValue.fromJson(json, "min", -Double.MAX_VALUE);
            ScalingValue max = ScalingValue.fromJson(json, "max", Double.MAX_VALUE);
            return (entity, caster, target, slot, pos) -> {
                float temperature = entity.level().getBiome(entity.blockPosition()).value().getBaseTemperature();
                return temperature >= min.evaluate(caster, target, slot, pos)
                        && temperature <= max.evaluate(caster, target, slot, pos);
            };
        });

        EntityConditions.register("dimension", EntitySelector.AUTO, json -> {
            ResourceLocation dimension = ResourceLocation.tryParse(GsonHelper.getAsString(json, "dimension"));
            if (dimension == null) {
                throw new IllegalArgumentException("malformed dimension id");
            }
            return (entity, caster, target, slot, pos) -> entity.level().dimension().location().equals(dimension);
        });

        // "scope": own (the caster's race), allied (own or an ally's), other (anyone else's) or any.
        EntityConditions.register("in_claimed_territory", EntitySelector.AUTO, json -> {
            String scope = GsonHelper.getAsString(json, "scope", "own");
            if (!scope.equals("own") && !scope.equals("allied") && !scope.equals("other") && !scope.equals("any")) {
                throw new IllegalArgumentException("unknown scope '" + scope + "' (own, allied, other, any)");
            }
            return (entity, caster, target, slot, pos) -> {
                TerritoryManager territories = TerritoryManager.get();
                ClaimData claim = territories.getClaimAt(new ChunkPos(entity.blockPosition()));
                if (claim == null) {
                    return false;
                }
                if (scope.equals("any")) {
                    return true;
                }
                ResourceLocation casterRace = DataUtils.getVariables(caster).map(IPlayerVariables::getRace).orElse(null);
                if (casterRace == null) {
                    return false;
                }
                boolean own = claim.getRaceId().equals(casterRace);
                return switch (scope) {
                    case "own" -> own;
                    case "other" -> !own;
                    default -> own || territories.getDiplomacy(casterRace, claim.getRaceId()) == DiplomacyStatus.ALLY;
                };
            };
        });
    }
}
