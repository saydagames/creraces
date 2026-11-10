package mc.sayda.creraces.engine;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import mc.sayda.creraces.registry.ModAttributes;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A number read from JSON. It is a plain number, a single stat name, or an object:
 * <pre>{ "base": 2, "scales_with": "creraces:ability_power", "factor": 0.05, "entity": "target",
 *   "scales": [{ "stat": "...", "factor": ..., "entity": "..." }], "math": "floor", "min": 0, "max": 10 }</pre>
 * Every term adds stat * factor to the base. A factor can itself be a scaling value, which is how
 * two stats are multiplied. Stat names are parsed into evaluators once, when the JSON loads.
 * <p>
 * A stat is an attribute id, "random", resource:/state:/cooldown:/ability_level:/custom:/config:/race:
 * followed by a name, flag: followed by a flag (1 when set, else 0), an event: value, or one of the
 * functions effect(), enchantment() and position().
 * Every number the engine stores can be read this way, so the compare condition checks any of them.
 */
@SuppressWarnings("null")
public class ScalingValue {
    private static final Set<String> KEYS =
            Set.of("base", "scales_with", "factor", "entity", "scales", "math", "min", "max");

    private final double base;
    private final List<Term> terms;
    private final MathOp math;
    @Nullable
    private final Double min;
    @Nullable
    private final Double max;

    public enum MathOp {
        NONE, ROUND, FLOOR, CEIL, SQRT, ABS
    }

    /** Reads one stat from {@code subject}, the caster or the target depending on the term's "entity". */
    @FunctionalInterface
    public interface Evaluator {
        double evaluate(LivingEntity subject, Player player, @Nullable LivingEntity target,
                @Nullable AbilitySlot slot, @Nullable BlockPos interactPos);
    }

    private record Term(Evaluator stat, ScalingValue factor, boolean onTarget) {
    }

    private ScalingValue(double base, List<Term> terms, MathOp math, @Nullable Double min, @Nullable Double max) {
        this.base = base;
        this.terms = terms;
        this.math = math;
        this.min = min;
        this.max = max;
    }

    public static ScalingValue fixed(double value) {
        return new ScalingValue(value, List.of(), MathOp.NONE, null, null);
    }

    public boolean isConstant() {
        return terms.isEmpty();
    }

    public double evaluate(Player player) {
        return evaluate(player, null, null, null);
    }

    public double evaluate(Player player, @Nullable LivingEntity target) {
        return evaluate(player, target, null, null);
    }

    public double evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot) {
        return evaluate(player, target, slot, null);
    }

    public double evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        double result = base;
        for (Term term : terms) {
            // A term that reads the target adds nothing when there is no target.
            LivingEntity subject = term.onTarget() ? target : player;
            if (subject != null) {
                result += term.stat().evaluate(subject, player, target, slot, interactPos)
                        * term.factor().evaluate(player, target, slot, interactPos);
            }
        }

        switch (math) {
            case ROUND -> result = Math.round(result);
            case FLOOR -> result = Math.floor(result);
            case CEIL -> result = Math.ceil(result);
            case SQRT -> result = Math.sqrt(Math.max(0, result));
            case ABS -> result = Math.abs(result);
            default -> {
            }
        }
        if (min != null) {
            result = Math.max(min, result);
        }
        if (max != null) {
            result = Math.min(max, result);
        }
        return result;
    }

    public static ScalingValue fromJson(JsonObject json, String key, double defaultBase) {
        if (!json.has(key)) {
            return fixed(defaultBase);
        }
        return fromElement(json.get(key), defaultBase);
    }

    private static ScalingValue fromElement(JsonElement element, double defaultBase) {
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isNumber()) {
                return fixed(primitive.getAsDouble());
            }
            if (primitive.isString()) {
                return new ScalingValue(0, List.of(new Term(parseStat(primitive.getAsString()), fixed(1), false)),
                        MathOp.NONE, null, null);
            }
        }
        if (!element.isJsonObject()) {
            CreRaces.LOGGER.error("Expected a number, stat or scaling object, got {}", element);
            return fixed(defaultBase);
        }

        JsonObject obj = element.getAsJsonObject();
        for (String member : obj.keySet()) {
            if (!KEYS.contains(member)) {
                CreRaces.LOGGER.warn("Unknown key '{}' in scaling value {}", member, obj);
            }
        }

        List<Term> terms = new ArrayList<>();
        if (obj.has("scales_with")) {
            terms.add(new Term(parseStat(GsonHelper.getAsString(obj, "scales_with")), fromJson(obj, "factor", 1.0),
                    readsTarget(obj)));
        }
        if (obj.has("scales") && obj.get("scales").isJsonArray()) {
            for (JsonElement entry : obj.getAsJsonArray("scales")) {
                if (entry.isJsonObject() && entry.getAsJsonObject().has("stat")) {
                    JsonObject scale = entry.getAsJsonObject();
                    terms.add(new Term(parseStat(GsonHelper.getAsString(scale, "stat")), fromJson(scale, "factor", 1.0),
                            readsTarget(scale)));
                }
            }
        }
        Double min = obj.has("min") ? obj.get("min").getAsDouble() : null;
        Double max = obj.has("max") ? obj.get("max").getAsDouble() : null;
        return new ScalingValue(GsonHelper.getAsDouble(obj, "base", defaultBase), terms, parseMath(obj, MathOp.NONE),
                min, max);
    }

    private static boolean readsTarget(JsonObject term) {
        return EntitySelector.fromJson(term, EntitySelector.SELF) == EntitySelector.TARGET;
    }

    /** Reads "math"; a missing or unknown mode gives {@code fallback}. */
    public static MathOp parseMath(JsonObject json, MathOp fallback) {
        if (!json.has("math")) {
            return fallback;
        }
        String name = json.get("math").getAsString();
        try {
            return MathOp.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            CreRaces.LOGGER.error("Unknown math mode '{}' in {}", name, json);
            return fallback;
        }
    }

    private static Evaluator parseStat(String text) {
        String stat = text.trim().toLowerCase();
        Evaluator function = parseFunction(stat);
        if (function != null) {
            return function;
        }

        switch (stat) {
            case "random" -> {
                return (s, p, t, sl, ip) -> p.getRandom().nextDouble();
            }
            case "creraces:ability_haste" -> {
                return (s, p, t, sl, ip) -> Math.min(
                        attributeValue(s, ModAttributes.resolve(ModAttributes.ABILITY_HASTE)),
                        CreRacesConfig.ABILITY_HASTE_CAP.get());
            }
            case "event:damage" -> {
                return (s, p, t, sl, ip) -> EventContext.current().damage();
            }
            case "event:fall_distance" -> {
                return (s, p, t, sl, ip) -> EventContext.current().fallDistance();
            }
            case "event:item_count" -> {
                return (s, p, t, sl, ip) -> EventContext.current().item().getCount();
            }
            default -> {
            }
        }

        int colon = stat.indexOf(':');
        String prefix = colon > 0 ? stat.substring(0, colon) : "";
        String rest = colon > 0 ? stat.substring(colon + 1) : stat;
        switch (prefix) {
            case "resource" -> {
                Resources.Resource resource = Resources.get(rest);
                if (resource == null) {
                    CreRaces.LOGGER.error("Unknown resource '{}' in scaling value; known: {}", rest, Resources.knownNames());
                    return (s, p, t, sl, ip) -> 0.0;
                }
                return (s, p, t, sl, ip) -> resource.read(s);
            }
            case "state" -> {
                return playerNumber(rest, IPlayerVariables::getPersistentState);
            }
            case "flag" -> {
                EntityFlag flag = EntityFlag.byId(rest);
                if (flag == null) {
                    CreRaces.LOGGER.error("Unknown flag '{}' in scaling value; known: {}", rest, EntityFlag.knownIds());
                    return (s, p, t, sl, ip) -> 0.0;
                }
                return (s, p, t, sl, ip) -> flag.get(s) ? 1.0 : 0.0;
            }
            case "cooldown" -> {
                return playerNumber(rest, IPlayerVariables::getCooldown);
            }
            case "ability_level" -> {
                return playerNumber(rest, IPlayerVariables::getAbilityLevel);
            }
            case "custom" -> {
                return customizationEvaluator(rest);
            }
            case "config" -> {
                return configEvaluator(rest.toUpperCase());
            }
            case "race" -> {
                return raceEvaluator(rest);
            }
            case "event" -> {
                CreRaces.LOGGER.error("Unknown stat '{}' in scaling value", stat);
                return (s, p, t, sl, ip) -> 0.0;
            }
            default -> {
            }
        }

        // Anything else is an attribute id, defaulting to the minecraft namespace. Our own attributes
        // read Apothic Attributes' equivalent when that mod is loaded.
        ResourceLocation attributeId = ResourceLocation.tryParse(stat.contains(":") ? stat : "minecraft:" + stat);
        if (attributeId == null || !BuiltInRegistries.ATTRIBUTE.containsKey(attributeId)) {
            // Logged rather than rejected: the attribute may belong to an optional mod that isn't loaded.
            CreRaces.LOGGER.warn("Stat '{}' is not a known attribute; it reads as 0 unless that attribute exists", stat);
        }
        return (s, p, t, sl, ip) -> {
            Holder<Attribute> attribute = attributeId != null ? ModAttributes.getAttribute(attributeId) : null;
            return attribute != null && s.getAttributes().hasAttribute(attribute) ? attributeValue(s, attribute) : 0.0;
        };
    }

    /** effect(id, property), enchantment(id, slot) and position(from, axis); null when the text isn't a function. */
    @Nullable
    private static Evaluator parseFunction(String stat) {
        int open = stat.indexOf('(');
        if (open <= 0 || !stat.endsWith(")")) {
            return null;
        }
        String name = stat.substring(0, open);
        String[] args = stat.substring(open + 1, stat.length() - 1).split(",");
        if (args.length != 2) {
            CreRaces.LOGGER.error("Stat '{}' needs exactly two arguments", stat);
            return (s, p, t, sl, ip) -> 0.0;
        }
        String first = args[0].trim();
        String second = args[1].trim();
        return switch (name) {
            case "effect" -> effectEvaluator(first, second);
            case "enchantment" -> enchantmentEvaluator(first, second);
            case "position" -> positionEvaluator(first, second);
            default -> {
                CreRaces.LOGGER.error("Unknown stat function '{}' in '{}'", name, stat);
                yield (s, p, t, sl, ip) -> 0.0;
            }
        };
    }

    /** Percent-style attributes (crit chance, cooldown reduction, ...) are reported on a 0-100 scale. */
    private static double attributeValue(LivingEntity subject, @Nullable Holder<Attribute> attribute) {
        if (attribute == null) {
            return 0.0;
        }
        double value = subject.getAttributeValue(attribute);
        return ModAttributes.isPercentAttribute(attribute) ? value * 100.0 : value;
    }

    /** Reads one of a player's per-id numbers (a state, cooldown or ability level). */
    @FunctionalInterface
    private interface PlayerNumber {
        double read(IPlayerVariables vars, ResourceLocation id);
    }

    /** A per-id number of the subject player, 0 for other entities. "self" is the caster's casting ability. */
    private static Evaluator playerNumber(String key, PlayerNumber number) {
        boolean self = key.equals("self");
        ResourceLocation fixedId = self ? null : IdRef.parse(key);
        if (!self && fixedId == null) {
            CreRaces.LOGGER.error("Invalid id '{}' in scaling value", key);
            return (s, p, t, sl, ip) -> 0.0;
        }
        return (s, p, t, sl, ip) -> {
            ResourceLocation id = self ? IdRef.resolve(key, p, sl) : fixedId;
            IPlayerVariables vars = s instanceof Player sp ? DataUtils.getVariables(sp).orElse(null) : null;
            return id != null && vars != null ? number.read(vars, id) : 0.0;
        };
    }

    private static Evaluator customizationEvaluator(String key) {
        return (s, p, t, sl, ip) -> {
            String value = s instanceof Player sp
                    ? DataUtils.getVariables(sp).map(vars -> vars.getCustomization(key)).orElse(null)
                    : null;
            if (value == null) {
                return 0.0;
            }
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                // Non-numeric customizations (colors, variant names) count as 0.
                return 0.0;
            }
        };
    }

    private static Evaluator raceEvaluator(String key) {
        int axis = switch (key) {
            case "respawn_x" -> 0;
            case "respawn_y" -> 1;
            case "respawn_z" -> 2;
            default -> -1;
        };
        if (axis < 0) {
            CreRaces.LOGGER.error("Unknown race stat 'race:{}'; known: respawn_x, respawn_y, respawn_z", key);
            return (s, p, t, sl, ip) -> 0.0;
        }
        return (s, p, t, sl, ip) -> {
            if (!(s instanceof Player sp)) {
                return 0.0;
            }
            Race race = DataUtils.getVariables(sp).map(vars -> RaceRegistry.get(vars.getRace())).orElse(null);
            return race != null && race.respawnPos() != null ? race.respawnPos()[axis] : 0.0;
        };
    }

    private static Evaluator effectEvaluator(String idText, String property) {
        ResourceLocation effectId = ResourceLocation.tryParse(idText.contains(":") ? idText : "minecraft:" + idText);
        if (effectId == null) {
            CreRaces.LOGGER.error("Invalid effect id '{}' in scaling value", idText);
            return (s, p, t, sl, ip) -> 0.0;
        }
        return (s, p, t, sl, ip) -> {
            Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.getHolder(effectId).orElse(null);
            MobEffectInstance instance = effect != null ? s.getEffect(effect) : null;
            if (instance == null) {
                return 0.0;
            }
            return switch (property) {
                case "duration" -> instance.getDuration();
                case "amplifier" -> instance.getAmplifier();
                case "visible" -> instance.isVisible() ? 1.0 : 0.0;
                case "ambient" -> instance.isAmbient() ? 1.0 : 0.0;
                default -> 0.0;
            };
        };
    }

    private static Evaluator enchantmentEvaluator(String idText, String slot) {
        ResourceLocation enchantmentId = ResourceLocation.tryParse(idText.contains(":") ? idText : "minecraft:" + idText);
        if (enchantmentId == null) {
            CreRaces.LOGGER.error("Invalid enchantment id '{}' in scaling value", idText);
            return (s, p, t, sl, ip) -> 0.0;
        }
        return (s, p, t, sl, ip) -> {
            Holder<Enchantment> enchantment = s.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                    .getHolder(enchantmentId).orElse(null);
            if (enchantment == null) {
                return 0.0;
            }
            int best = 0;
            for (var stack : ItemSlots.stacks(s, slot)) {
                best = Math.max(best, EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack));
            }
            return best;
        };
    }

    private static Evaluator positionEvaluator(String from, String axis) {
        Position.Source source = Position.parseSource(from);
        int index = switch (axis) {
            case "x" -> 0;
            case "y" -> 1;
            case "z" -> 2;
            default -> -1;
        };
        if (source == null || index < 0) {
            CreRaces.LOGGER.error("position({}, {}) needs a source (self, target, block, look, origin) and x, y or z",
                    from, axis);
            return (s, p, t, sl, ip) -> 0.0;
        }
        return (s, p, t, sl, ip) -> {
            Vec3 point = Position.base(source, p, t, ip);
            if (point == null) {
                return 0.0;
            }
            return index == 0 ? point.x : index == 1 ? point.y : point.z;
        };
    }

    private static Evaluator configEvaluator(String fieldName) {
        Field field;
        try {
            field = CreRacesConfig.class.getField(fieldName);
        } catch (NoSuchFieldException e) {
            CreRaces.LOGGER.error("Unknown config value '{}' in scaling value", fieldName);
            return (s, p, t, sl, ip) -> 0.0;
        }
        return (s, p, t, sl, ip) -> {
            try {
                // Read the field on every call: the loader swaps in new suppliers when the config loads.
                if (field.get(null) instanceof Supplier<?> supplier) {
                    Object value = supplier.get();
                    if (value instanceof Number number) {
                        return number.doubleValue();
                    } else if (value instanceof Boolean bool) {
                        return bool ? 1.0 : 0.0;
                    }
                }
            } catch (Exception e) {
                CreRaces.LOGGER.error("Failed to read config value '{}' in scaling value", fieldName);
            }
            return 0.0;
        };
    }
}
