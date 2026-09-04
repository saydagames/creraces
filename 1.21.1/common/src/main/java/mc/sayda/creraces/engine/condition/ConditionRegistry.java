package mc.sayda.creraces.engine.condition;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.TargetFilter;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for condition types. All built-in conditions are registered under the
 * {@code creraces:} namespace. Addons register their own conditions under their
 * own namespace via {@link #register}.
 *
 * <p>Call {@link #init()} once during mod startup (alongside ActionRegistry.init()).
 */
public class ConditionRegistry {

    public interface ConditionFactory {
        Condition create(JsonObject data);
    }

    private static final Map<ResourceLocation, ConditionFactory> REGISTRY = new HashMap<>();

    public static void register(ResourceLocation id, ConditionFactory factory) {
        REGISTRY.put(id, factory);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("creraces", path);
    }

    public static Condition fromJson(JsonObject json) {
        if (json == null || json.size() == 0 || !json.has("type")) {
            return (player, target, slot, interact_pos) -> true;
        }
        String typeStr = json.get("type").getAsString();
        @SuppressWarnings("null")
        ResourceLocation typeLoc = ResourceLocation.tryParse(typeStr);
        if (typeLoc == null) {
            CreRaces.LOGGER.error("Malformed condition type '{}' - skipping.", typeStr);
            return (player, target, slot, interact_pos) -> false;
        }
        ConditionFactory factory = REGISTRY.get(typeLoc);
        if (factory == null) {
            CreRaces.LOGGER.error("Unknown condition type '{}' - condition will always return false. "
                    + "Did you forget the namespace prefix (e.g. 'creraces:{}') or register it?",
                    typeStr, typeLoc.getPath());
            return (player, target, slot, interact_pos) -> false;
        }
        try {
            return factory.create(json);
        } catch (Exception e) {
            CreRaces.LOGGER.error(
                    "Failed to parse condition '{}': {} - condition will always return false. JSON: {}",
                    typeStr, e.getMessage(), json);
            return (player, target, slot, interact_pos) -> false;
        }
    }

    @SuppressWarnings("null")
    public static void init() {
        register(id("state"), json -> {
            String stateKey = GsonHelper.getAsString(json, "state");
            ScalingValue value = ScalingValue.fromJson(json, "value", 1.0);
            String operator = GsonHelper.getAsString(json, "operator", "==");
            return new StateCondition(stateKey, value, operator);
        });
        register(id("state_equals"), json -> {
            String stateKey = GsonHelper.getAsString(json, "state");
            ScalingValue value = ScalingValue.fromJson(json, "value", 1.0);
            String operator = GsonHelper.getAsString(json, "operator", "==");
            return new StateCondition(stateKey, value, operator);
        });
        register(id("morphed"), json -> {
            boolean expected = GsonHelper.getAsBoolean(json, "value", true);
            return new MorphedCondition(expected);
        });
        register(id("flying"), json -> {
            boolean expected = GsonHelper.getAsBoolean(json, "value", true);
            boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
            return new FlyingCondition(expected, useTarget);
        });
        register(id("sneaking"), json -> {
            boolean expected = GsonHelper.getAsBoolean(json, "value", true);
            boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
            return new SneakingCondition(expected, useTarget);
        });
        register(id("on_ground"), json -> {
            boolean expected = GsonHelper.getAsBoolean(json, "value", true);
            boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
            return new OnGroundCondition(expected, useTarget);
        });
        register(id("wearing_armor"), json -> {
            String item = GsonHelper.getNullableString(json, "item", null);
            String tag = GsonHelper.getNullableString(json, "tag", null);
            String slot = GsonHelper.getAsString(json, "slot", "0");
            if (tag != null && !tag.startsWith("#")) tag = "#" + tag;
            return new WearingArmorCondition(item != null ? item : tag, slot);
        });
        register(id("holding_item"), json -> {
            String item = GsonHelper.getNullableString(json, "item", null);
            String tag = GsonHelper.getNullableString(json, "tag", null);
            boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
            if (tag != null && !tag.startsWith("#")) tag = "#" + tag;
            return new HoldingItemCondition(item != null ? item : tag, useTarget);
        });
        register(id("item_interaction"), json -> {
            String item = GsonHelper.getAsString(json, "item", "minecraft:air");
            return new ItemInteractionCondition(item);
        });
        register(id("and"), json -> {
            com.google.gson.JsonArray array = json.getAsJsonArray("conditions");
            Condition[] conditions = new Condition[array.size()];
            for (int i = 0; i < array.size(); i++)
                conditions[i] = fromJson(array.get(i).getAsJsonObject());
            return new AndCondition(conditions);
        });
        register(id("or"), json -> {
            com.google.gson.JsonArray array = json.getAsJsonArray("conditions");
            Condition[] conditions = new Condition[array.size()];
            for (int i = 0; i < array.size(); i++)
                conditions[i] = fromJson(array.get(i).getAsJsonObject());
            return new OrCondition(conditions);
        });
        register(id("not"), json -> {
            Condition condition = fromJson(json.getAsJsonObject("condition"));
            return new NotCondition(condition);
        });
        register(id("biome"), json -> {
            String biomeId = GsonHelper.getNullableString(json, "biome", null);
            String tag = GsonHelper.getNullableString(json, "tag", null);
            return new BiomeCondition(biomeId, tag);
        });
        register(id("weather"), json -> {
            String weatherType = GsonHelper.getAsString(json, "weather");
            return new WeatherCondition(weatherType);
        });
        register(id("time"), json -> {
            ScalingValue min = ScalingValue.fromJson(json, "min", 0.0);
            ScalingValue max = ScalingValue.fromJson(json, "max", 24000.0);
            return new TimeCondition(min, max);
        });
        register(id("in_water"), json -> {
            boolean expected = GsonHelper.getAsBoolean(json, "value", true);
            boolean includeRain = GsonHelper.getAsBoolean(json, "include_rain", false);
            return new InWaterCondition(expected, includeRain);
        });
        register(id("in_sunlight"), json -> {
            boolean expected = GsonHelper.getAsBoolean(json, "value", true);
            return new InSunlightCondition(expected);
        });
        register(id("altitude"), json -> {
            ScalingValue min = ScalingValue.fromJson(json, "min", Double.NEGATIVE_INFINITY);
            ScalingValue max = ScalingValue.fromJson(json, "max", Double.POSITIVE_INFINITY);
            return new AltitudeCondition(min, max);
        });
        register(id("biome_temperature"), json -> {
            ScalingValue min = ScalingValue.fromJson(json, "min", (double) -Float.MAX_VALUE);
            ScalingValue max = ScalingValue.fromJson(json, "max", (double) Float.MAX_VALUE);
            return (player, target, slot, interact_pos) -> {
                float temp = player.level().getBiome(player.blockPosition()).value().getBaseTemperature();
                return temp >= (float) min.evaluate(player, target)
                        && temp < (float) max.evaluate(player, target);
            };
        });
        register(id("is_burning"), json -> {
            boolean expected = GsonHelper.getAsBoolean(json, "value", true);
            boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
            return new IsBurningCondition(expected, useTarget);
        });
        register(id("is_moving"), json -> {
            boolean expected = GsonHelper.getAsBoolean(json, "value", true);
            ScalingValue threshold = ScalingValue.fromJson(json, "threshold", 0.1);
            boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
            return new IsMovingCondition(expected, threshold, useTarget);
        });
        register(id("resource_level"), json -> {
            String resource = GsonHelper.getAsString(json, "resource");
            String operator = GsonHelper.getAsString(json, "operator", ">=");
            ScalingValue value = ScalingValue.fromJson(json, "value", 0.0);
            return new ResourceLevelCondition(resource, operator, value);
        });
        register(id("has_effect"), json -> {
            String effectIdStr = GsonHelper.getAsString(json, "effect");
            ResourceLocation effectId = ResourceLocation.tryParse(effectIdStr);
            if (effectId == null) {
                CreRaces.LOGGER.error("has_effect condition has malformed effect ID: '{}'", effectIdStr);
                return (player, target, slot, interact_pos) -> false;
            }
            ScalingValue amp = ScalingValue.fromJson(json, "amplifier", 0);
            boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
            return new HasEffectCondition(effectId, amp, useTarget);
        });
        register(id("modulo"), json -> {
            String stateKey = json.has("state") ? GsonHelper.getAsString(json, "state")
                    : GsonHelper.getAsString(json, "id");
            if (!stateKey.contains(":")) stateKey = "creraces:" + stateKey;
            ResourceLocation stateId = ResourceLocation.tryParse(stateKey);
            ScalingValue divisor = ScalingValue.fromJson(json, "divisor", 2.0);
            ScalingValue remainder = ScalingValue.fromJson(json, "remainder", 0.0);
            return new ModuloCondition(stateId, divisor, remainder);
        });
        register(id("scaling_compare"), json -> {
            ScalingValue first = ScalingValue.fromJson(json, "first", 0.0);
            ScalingValue second = ScalingValue.fromJson(json, "second", 0.0);
            String operator = GsonHelper.getAsString(json, "operator", "==");
            return new ScalingCompareCondition(first, second, operator);
        });
        register(id("can_place_block"), json -> {
            ScalingValue ox = ScalingValue.fromJson(json, "offset_x", 0.0);
            ScalingValue oy = ScalingValue.fromJson(json, "offset_y", 0.0);
            ScalingValue oz = ScalingValue.fromJson(json, "offset_z", 0.0);
            boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
            boolean useTargetBlock = GsonHelper.getAsBoolean(json, "use_target_block", false);
            boolean absolute = GsonHelper.getAsBoolean(json, "absolute", false);
            ScalingValue.MathOp math = ScalingValue.MathOp.ROUND;
            if (json.has("math")) {
                try {
                    math = ScalingValue.MathOp.valueOf(json.get("math").getAsString().toUpperCase());
                } catch (Exception e) {
                    CreRaces.LOGGER.warn("Invalid math mode in CanPlaceBlockCondition: {}",
                            json.get("math").getAsString());
                }
            }
            return new CanPlaceBlockCondition(ox, oy, oz, useTarget, useTargetBlock, absolute, math);
        });
        register(id("has_enchantment"), json -> {
            String enchId = GsonHelper.getAsString(json, "enchantment");
            ScalingValue level = ScalingValue.fromJson(json, "level", 1.0);
            String slot = GsonHelper.getAsString(json, "slot", "any");
            String operator = GsonHelper.getAsString(json, "operator", ">=");
            boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
            return new HasEnchantmentCondition(enchId, level, slot, operator, useTarget);
        });
        register(id("is_smeltable"), json -> new IsSmeltableCondition());
        register(id("exposed_to_rain"), json -> {
            boolean expected = GsonHelper.getAsBoolean(json, "value", true);
            return new ExposedToRainCondition(expected);
        });
        register(id("spirit"), json -> {
            boolean expected = GsonHelper.getAsBoolean(json, "value", true);
            return new SpiritCondition(expected);
        });
        register(id("is_spirit_moon"), json -> {
            boolean expected = GsonHelper.getAsBoolean(json, "value", true);
            return new IsSpiritMoonCondition(expected);
        });
        register(id("block_data"), json -> {
            ScalingValue ox = ScalingValue.fromJson(json, "offset_x", 0.0);
            ScalingValue oy = ScalingValue.fromJson(json, "offset_y", 0.0);
            ScalingValue oz = ScalingValue.fromJson(json, "offset_z", 0.0);
            String key = GsonHelper.getAsString(json, "key");
            ScalingValue value = ScalingValue.fromJson(json, "value", 0.0);
            String operator = GsonHelper.getAsString(json, "operator", "==");
            boolean useInteractPos = GsonHelper.getAsBoolean(json, "use_interact_pos", true);
            return new BlockDataCondition(ox, oy, oz, key, value, operator, useInteractPos);
        });
        register(id("entity_data"), json -> EntityDataCondition.fromJson(json));
        register(id("entity"), json -> EntityCondition.fromJson(json));
        register(id("distance"), json -> {
            ScalingValue max = json.has("range")
                    ? ScalingValue.fromJson(json, "range", 5.0)
                    : ScalingValue.fromJson(json, "max", 5.0);
            ScalingValue x = ScalingValue.fromJson(json, "x", 0.0);
            ScalingValue y = ScalingValue.fromJson(json, "y", 0.0);
            ScalingValue z = ScalingValue.fromJson(json, "z", 0.0);
            return new DistanceCondition(x, y, z, max);
        });
        register(id("attack_charged"), json -> {
            ScalingValue threshold = ScalingValue.fromJson(json, "threshold", 0.9);
            return new AttackChargedCondition(threshold);
        });
        register(id("is_position"), json -> {
            ScalingValue x = ScalingValue.fromJson(json, "x", 0.0);
            ScalingValue y = ScalingValue.fromJson(json, "y", 0.0);
            ScalingValue z = ScalingValue.fromJson(json, "z", 0.0);
            boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
            boolean useTargetBlock = GsonHelper.getAsBoolean(json, "use_target_block", false);
            boolean absolute = GsonHelper.getAsBoolean(json, "absolute", true);
            ScalingValue.MathOp math = ScalingValue.MathOp.FLOOR;
            if (json.has("math")) {
                try {
                    math = ScalingValue.MathOp.valueOf(json.get("math").getAsString().toUpperCase());
                } catch (Exception e) {
                    CreRaces.LOGGER.warn("Invalid math mode in IsPositionCondition: {}",
                            json.get("math").getAsString());
                }
            }
            return new IsPositionCondition(x, y, z, useTarget, useTargetBlock, absolute, math);
        });
        register(id("dimension"), json -> {
            String dim = GsonHelper.getAsString(json, "value", "minecraft:overworld");
            return new DimensionCondition(dim);
        });
        register(id("customization_equals"), json -> {
            String condId = GsonHelper.getAsString(json, "id");
            java.util.List<String> validVals = new java.util.ArrayList<>();
            if (json.has("values") && json.get("values").isJsonArray()) {
                for (com.google.gson.JsonElement e : json.getAsJsonArray("values"))
                    validVals.add(e.getAsString());
            } else {
                CreRaces.LOGGER.error("customization_equals condition missing 'values' array");
            }
            return new CustomizationEqualsCondition(condId, validVals.toArray(new String[0]));
        });
        register(id("race_equals"), json -> {
            java.util.List<String> validRaces = new java.util.ArrayList<>();
            if (json.has("race")) validRaces.add(GsonHelper.getAsString(json, "race"));
            if (json.has("races")) {
                for (com.google.gson.JsonElement e : json.getAsJsonArray("races"))
                    validRaces.add(e.getAsString());
            }
            if (validRaces.isEmpty()) {
                CreRaces.LOGGER.error("race_equals condition has no 'race' or 'races' field, will always return false. JSON: {}", json);
            }
            return new RaceEqualsCondition(validRaces);
        });
        register(id("has_customization"), json -> {
            String condId = GsonHelper.getAsString(json, "id");
            return new HasCustomizationCondition(condId);
        });
        register(id("has_entities"), json -> {
            ScalingValue radius = ScalingValue.fromJson(json, "radius", 5.0);
            TargetFilter targets = TargetFilter.fromJson(json, "targets", java.util.Set.of("enemies"));
            return new HasEntitiesCondition(radius, targets);
        });
        register(id("is_block"), json -> {
            String blockStr = GsonHelper.getAsString(json, "block", "minecraft:air");
            ScalingValue ox = ScalingValue.fromJson(json, "offset_x", 0.0);
            ScalingValue oy = ScalingValue.fromJson(json, "offset_y", 0.0);
            ScalingValue oz = ScalingValue.fromJson(json, "offset_z", 0.0);
            boolean useInteractPos = GsonHelper.getAsBoolean(json, "use_interact_pos", true);
            boolean absolute = GsonHelper.getAsBoolean(json, "absolute", false);
            boolean useRaycast = GsonHelper.getAsBoolean(json, "use_raycast", false);
            ScalingValue rayRange = ScalingValue.fromJson(json, "ray_range", 10.0);
            ScalingValue.MathOp math = ScalingValue.MathOp.FLOOR;
            if (json.has("math")) {
                try {
                    math = ScalingValue.MathOp.valueOf(json.get("math").getAsString().toUpperCase());
                } catch (Exception e) {
                    CreRaces.LOGGER.warn("Invalid math mode in IsBlockCondition: {}",
                            json.get("math").getAsString());
                }
            }
            return new IsBlockCondition(blockStr, ox, oy, oz, useInteractPos, absolute, math, useRaycast, rayRange);
        });
        register(id("cooldown"), json -> {
            String condId = json.has("state") ? GsonHelper.getAsString(json, "state")
                    : GsonHelper.getAsString(json, "id");
            if (!condId.contains(":")) condId = "creraces:" + condId;
            String operator = GsonHelper.getAsString(json, "operator", "<=");
            ScalingValue value = ScalingValue.fromJson(json, "value", 0.0);
            return new CooldownCondition(condId, operator, value);
        });
        register(id("ability_level"), json -> {
            String condId = json.has("id") ? GsonHelper.getAsString(json, "id") : "self";
            String operator = GsonHelper.getAsString(json, "operator", ">=");
            ScalingValue value = ScalingValue.fromJson(json, "value", 1.0);
            return new AbilityLevelCondition(condId, operator, value);
        });
        register(id("in_habitable_biome"), json -> new InHabitableBiomeCondition());
        register(id("has_faction"), json -> new HasFactionCondition());
        register(id("is_faction_leader"), json -> new IsFactionLeaderCondition());
        register(id("has_faction_group"), json -> new HasFactionGroupCondition());
        register(id("in_claimed_territory"), json -> {
            String scope = GsonHelper.getAsString(json, "scope", "own");
            return new InClaimedTerritoryCondition(scope);
        });
        register(id("is_spirit"), json -> {
            boolean expected = GsonHelper.getAsBoolean(json, "value", true);
            return new IsSpiritCondition(expected);
        });
    }
}
