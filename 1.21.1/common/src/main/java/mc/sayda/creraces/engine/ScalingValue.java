package mc.sayda.creraces.engine;

import com.google.gson.JsonArray;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A number read from JSON: a constant, a single stat, or {@code base + stat * factor} plus any
 * number of extra stat terms, optionally rounded and clamped. Stat keys are parsed into evaluators
 * once, when the JSON is loaded.
 */
public class ScalingValue {
    // Members of a scaling object that are not "<stat>": factor shorthand entries.
    private static final Set<String> RESERVED_KEYS =
            Set.of("base", "scales_with", "factor", "scales", "scaling", "math", "min", "max");

    private final double base;
    private final Evaluator evaluator;
    private final double factor;
    private final boolean useTarget;
    private final List<ScalingComponent> additionalScales;
    private final MathOp math;
    private final Double min;
    private final Double max;

    public enum MathOp {
        NONE, ROUND, FLOOR, CEIL, SQRT, ABS
    }

    public ScalingValue(double base, @Nullable Evaluator evaluator, double factor,
            List<ScalingComponent> additionalScales) {
        this(base, evaluator, factor, false, additionalScales, MathOp.NONE, null, null);
    }

    public ScalingValue(double base, @Nullable Evaluator evaluator, double factor, boolean useTarget,
            List<ScalingComponent> additionalScales, MathOp math, Double min, Double max) {
        this.base = base;
        this.evaluator = evaluator;
        this.factor = factor;
        this.useTarget = useTarget;
        this.additionalScales = additionalScales != null ? additionalScales : new ArrayList<>();
        this.math = math != null ? math : MathOp.NONE;
        this.min = min;
        this.max = max;
    }

    public double base() {
        return base;
    }

    public boolean isZero() {
        return base == 0 && evaluator == null && additionalScales.isEmpty();
    }

    public static class ScalingComponent {
        private final Evaluator evaluator;
        private final double factor;
        private final boolean useTarget;

        public ScalingComponent(Evaluator evaluator, double factor) {
            this(evaluator, factor, false);
        }

        public ScalingComponent(Evaluator evaluator, double factor, boolean useTarget) {
            this.evaluator = evaluator;
            this.factor = factor;
            this.useTarget = useTarget;
        }

        public Evaluator evaluator() {
            return evaluator;
        }

        public double factor() {
            return factor;
        }

        public boolean useTarget() {
            return useTarget;
        }
    }

    public static ScalingValue fixed(double value) {
        return new ScalingValue(value, null, 0, false, new ArrayList<>(), MathOp.NONE, null, null);
    }

    /** Reads one stat from {@code subject}: the player, or the target when the term has use_target. */
    @FunctionalInterface
    public interface Evaluator {
        double evaluate(LivingEntity subject, Player player, @Nullable LivingEntity target,
                @Nullable AbilitySlot slot, @Nullable BlockPos interactPos);
    }

    public double evaluate(Player player) {
        return evaluate(player, null);
    }

    public double evaluate(Player player, @Nullable LivingEntity target) {
        return evaluate(player, target, null);
    }

    public double evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot) {
        return evaluate(player, target, slot, null);
    }

    public double evaluate(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        double result = base;
        if (evaluator != null) {
            LivingEntity subject = (useTarget && target != null) ? target : player;
            result += evaluator.evaluate(subject, player, target, slot, interactPos) * factor;
        }

        for (ScalingComponent comp : additionalScales) {
            LivingEntity subject = (comp.useTarget() && target != null) ? target : player;
            result += comp.evaluator().evaluate(subject, player, target, slot, interactPos) * comp.factor();
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

        if (min != null)
            result = Math.max(min, result);
        if (max != null)
            result = Math.min(max, result);

        return result;
    }

    private static Evaluator parseEvaluator(String statKey) {
        if (statKey == null || statKey.isEmpty())
            return (s, p, t, sl, ip) -> 0.0;

        final String stat = statKey.toLowerCase();

        // Aliases for the CreRaces combat attributes. They go through ModAttributes.resolve so an
        // Apothic Attributes replacement is used when that mod is loaded.
        if (stat.equals("creraces:ap") || stat.equals("creraces:ability_power")) {
            return (s, p, t, sl, ip) -> attributeValue(s, ModAttributes.resolve(ModAttributes.ABILITY_POWER));
        }
        if (stat.equals("creraces:ad") || stat.equals("creraces:attack_damage")) {
            return (s, p, t, sl, ip) -> attributeValue(s, ModAttributes.resolve(ModAttributes.ATTACK_DAMAGE));
        }
        if (stat.equals("creraces:crit") || stat.equals("creraces:crit_rate")) {
            return (s, p, t, sl, ip) -> attributeValue(s, ModAttributes.resolve(ModAttributes.CRIT_RATE));
        }
        if (stat.equals("creraces:health")) {
            return (s, p, t, sl, ip) -> s.getHealth();
        }
        if (stat.equals("creraces:max_health")) {
            return (s, p, t, sl, ip) -> s.getMaxHealth();
        }
        if (stat.equals("creraces:ability_haste")) {
            return (s, p, t, sl, ip) -> {
                Holder<Attribute> attr = ModAttributes.resolve(ModAttributes.ABILITY_HASTE);
                return attr != null ? Math.min(attributeValue(s, attr), CreRacesConfig.ABILITY_HASTE_CAP.get()) : 0.0;
            };
        }
        if (stat.equals("creraces:armor_shred")) {
            return (s, p, t, sl, ip) -> attributeValue(s, ModAttributes.resolve(ModAttributes.ARMOR_SHRED));
        }

        if (stat.startsWith("race:")) {
            return raceEvaluator(stat.substring(5));
        }
        if (stat.startsWith("custom:")) {
            return customizationEvaluator(stat.substring(7));
        }
        if (stat.startsWith("var:")) {
            return variableEvaluator(stat.substring(4));
        }
        if (stat.startsWith("state:")) {
            return stateEvaluator(stat.substring(6));
        }
        if (stat.startsWith("effect(") && stat.endsWith(")")) {
            Evaluator effect = effectEvaluator(stat.substring(7, stat.length() - 1));
            if (effect != null) {
                return effect;
            }
        }
        if (stat.startsWith("config:")) {
            return configEvaluator(stat.substring(7).toUpperCase());
        }

        // Anything else is an attribute id, defaulting to the minecraft namespace.
        final String resLocStr = stat.contains(":") ? stat : "minecraft:" + stat;
        final ResourceLocation attrId = ResourceLocation.tryParse(resLocStr);
        if (attrId == null) {
            CreRaces.LOGGER.error("Invalid attribute/stat ID in ScalingValue: {}", resLocStr);
            return (s, p, t, sl, ip) -> 0.0;
        }

        return (s, p, t, sl, ip) -> {
            Holder<Attribute> attr = BuiltInRegistries.ATTRIBUTE.getHolder(attrId).orElse(null);
            return attr != null && s.getAttributes().hasAttribute(attr) ? attributeValue(s, attr) : 0.0;
        };
    }

    /** Percent-style attributes (crit chance, cooldown reduction, ...) are reported on a 0-100 scale. */
    private static double attributeValue(LivingEntity subject, @Nullable Holder<Attribute> attr) {
        if (attr == null)
            return 0.0;
        double val = subject.getAttributeValue(attr);
        return ModAttributes.isPercentAttribute(attr) ? val * 100.0 : val;
    }

    private static Evaluator raceEvaluator(String key) {
        return (s, p, t, sl, ip) -> {
            if (!(s instanceof Player sp)) {
                return 0.0;
            }
            IPlayerVariables vars = DataUtils.getVariables(sp).orElse(null);
            if (vars == null) {
                return 0.0;
            }
            Race race = RaceRegistry.get(vars.getRace());
            if (race == null || race.respawnPos() == null) {
                return 0.0;
            }
            double[] pos = race.respawnPos();
            return switch (key) {
                case "respawn_x" -> pos[0];
                case "respawn_y" -> pos[1];
                case "respawn_z" -> pos[2];
                default -> 0.0;
            };
        };
    }

    private static Evaluator customizationEvaluator(String key) {
        return (s, p, t, sl, ip) -> {
            if (s instanceof Player sp) {
                IPlayerVariables vars = DataUtils.getVariables(sp).orElse(null);
                if (vars != null) {
                    String val = vars.getCustomization(key);
                    try {
                        return val != null ? Double.parseDouble(val) : 0.0;
                    } catch (NumberFormatException ignored) {
                        // Non-numeric customizations (colours, variant names) count as 0.
                    }
                }
            }
            return 0.0;
        };
    }

    private static Evaluator variableEvaluator(String key) {
        return (s, p, t, sl, ip) -> {
            if (!(s instanceof Player sp)) {
                return 0.0;
            }
            IPlayerVariables vars = DataUtils.getVariables(sp).orElse(null);
            if (vars == null) {
                return 0.0;
            }
            return switch (key) {
                case "mana" -> vars.getMana();
                case "energy" -> vars.getEnergy();
                case "grit" -> vars.getGrit();
                case "rage" -> vars.getRage();
                case "karma" -> vars.getKarma();
                case "soul" -> vars.getSoul();
                case "coins" -> vars.getCoins();
                case "passive_cd" -> vars.getPassiveCooldown();
                case "gstate" -> (double) vars.getGState();
                case "pos_x" -> ip != null ? (double) ip.getX() : sp.getX();
                case "pos_y" -> ip != null ? (double) ip.getY() : sp.getY();
                case "pos_z" -> ip != null ? (double) ip.getZ() : sp.getZ();
                case "player_x" -> sp.getX();
                case "player_y" -> sp.getY();
                case "player_z" -> sp.getZ();
                case "target_x" -> t != null ? t.getX() : sp.getX();
                case "target_y" -> t != null ? t.getY() : sp.getY();
                case "target_z" -> t != null ? t.getZ() : sp.getZ();
                case "level" -> abilityLevel(vars, sl);
                default -> 0.0;
            };
        };
    }

    /** Level of the ability in the given slot, else of the active ability, else 1. */
    private static double abilityLevel(IPlayerVariables vars, @Nullable AbilitySlot slot) {
        ResourceLocation id;
        if (slot != null) {
            id = vars.getAbilityInSlot(slot);
        } else if (vars.isAbilityActive()) {
            id = vars.getActiveAbility();
        } else {
            return 1.0;
        }
        return id != null ? (double) vars.getAbilityLevel(id) : 1.0;
    }

    private static Evaluator stateEvaluator(String key) {
        if (key.equals("self")) {
            return (s, p, t, sl, ip) -> {
                if (sl != null) {
                    IPlayerVariables vars = DataUtils.getVariables(p).orElse(null);
                    if (vars != null) {
                        ResourceLocation abilityId = vars.getAbilityInSlot(sl);
                        return abilityId != null ? vars.getPersistentState(abilityId) : 0.0;
                    }
                }
                return 0.0;
            };
        }
        String stateKey = key.contains(":") ? key : "creraces:" + key;
        final ResourceLocation stateId = ResourceLocation.tryParse(stateKey);
        if (stateId == null) {
            CreRaces.LOGGER.error("Invalid state ID in ScalingValue: {}", stateKey);
            return (s, p, t, sl, ip) -> 0.0;
        }
        return (s, p, t, sl, ip) -> {
            IPlayerVariables vars = DataUtils.getVariables(p).orElse(null);
            return vars != null ? vars.getPersistentState(stateId) : 0.0;
        };
    }

    /**
     * Parses the inside of {@code effect(id, property)}. Returns null when it is malformed, which
     * leaves the key to the attribute fallback and its error log.
     */
    @Nullable
    private static Evaluator effectEvaluator(String content) {
        String[] parts = content.split(",", 2);
        if (parts.length != 2) {
            return null;
        }
        String idStr = parts[0].trim();
        final String property = parts[1].trim().toLowerCase();
        if (!idStr.contains(":")) {
            idStr = "minecraft:" + idStr;
        }
        final ResourceLocation effectId = ResourceLocation.tryParse(idStr);
        if (effectId == null) {
            return null;
        }
        return (s, p, t, sl, ip) -> {
            Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.getHolder(effectId).orElse(null);
            MobEffectInstance inst = effect != null ? s.getEffect(effect) : null;
            if (inst == null) {
                return 0.0;
            }
            return switch (property) {
                case "duration" -> (double) inst.getDuration();
                case "amplifier" -> (double) inst.getAmplifier();
                case "visible" -> inst.isVisible() ? 1.0 : 0.0;
                case "ambient" -> inst.isAmbient() ? 1.0 : 0.0;
                default -> 0.0;
            };
        };
    }

    private static Evaluator configEvaluator(String fieldName) {
        Field field;
        try {
            field = CreRacesConfig.class.getField(fieldName);
        } catch (NoSuchFieldException e) {
            CreRaces.LOGGER.error("Unknown config value '{}' in ScalingValue", fieldName);
            return (s, p, t, sl, ip) -> 0.0;
        }
        return (s, p, t, sl, ip) -> {
            try {
                // Read the field on every call: the loader swaps in new suppliers when the config loads.
                if (field.get(null) instanceof Supplier<?> supplier) {
                    Object value = supplier.get();
                    if (value instanceof Number num) {
                        return num.doubleValue();
                    } else if (value instanceof Boolean bool) {
                        return bool ? 1.0 : 0.0;
                    }
                }
            } catch (Exception e) {
                CreRaces.LOGGER.error("Failed to read config value '{}' in ScalingValue", fieldName);
            }
            return 0.0;
        };
    }

    public static ScalingValue fromJson(JsonObject json, String key, double defaultBase) {
        if (!json.has(key)) {
            return fixed(defaultBase);
        }

        if (json.get(key).isJsonObject()) {
            JsonObject obj = json.get(key).getAsJsonObject();
            double base = GsonHelper.getAsDouble(obj, "base", defaultBase);
            String stat = GsonHelper.getNullableString(obj, "scales_with", null);
            double factor = GsonHelper.getAsDouble(obj, "factor", 1.0);
            boolean useTargetRoot = GsonHelper.getAsBoolean(obj, "use_target", false);

            List<ScalingComponent> additional = new ArrayList<>();

            if (obj.has("scales") && obj.get("scales").isJsonArray()) {
                JsonArray array = obj.getAsJsonArray("scales");
                for (int i = 0; i < array.size(); i++) {
                    if (!array.get(i).isJsonObject())
                        continue;
                    JsonObject scaleObj = array.get(i).getAsJsonObject();
                    String s = GsonHelper.getNullableString(scaleObj, "stat", null);
                    double f = GsonHelper.getAsDouble(scaleObj, "factor", 1.0);
                    boolean ut = GsonHelper.getAsBoolean(scaleObj, "use_target", false);
                    if (s != null) {
                        additional.add(new ScalingComponent(parseEvaluator(s), f, ut));
                    }
                }
            }

            if (obj.has("scaling") && obj.get("scaling").isJsonObject()) {
                addStatFactors(obj.get("scaling").getAsJsonObject(), additional, false);
            }
            addStatFactors(obj, additional, true);

            MathOp math = MathOp.NONE;
            if (obj.has("math")) {
                try {
                    math = MathOp.valueOf(obj.get("math").getAsString().toUpperCase());
                } catch (IllegalArgumentException e) {
                    CreRaces.LOGGER.warn("Unknown math operation: {}", obj.get("math").getAsString());
                }
            }
            Double min = obj.has("min") ? obj.get("min").getAsDouble() : null;
            Double max = obj.has("max") ? obj.get("max").getAsDouble() : null;

            return new ScalingValue(base, parseEvaluator(stat), factor, useTargetRoot, additional, math, min, max);
        } else if (json.get(key).isJsonPrimitive()) {
            JsonPrimitive primitive = json.get(key).getAsJsonPrimitive();
            if (primitive.isString()) {
                return new ScalingValue(0.0, parseEvaluator(primitive.getAsString()), 1.0, false, new ArrayList<>(),
                        MathOp.NONE, null, null);
            } else if (primitive.isNumber()) {
                return fixed(primitive.getAsDouble());
            }
        }
        return fixed(defaultBase);
    }

    /** Adds every numeric {@code "<stat>": factor} member of {@code obj} as an extra scaling term. */
    private static void addStatFactors(JsonObject obj, List<ScalingComponent> additional, boolean skipReserved) {
        for (String statKey : obj.keySet()) {
            if (skipReserved && RESERVED_KEYS.contains(statKey)) {
                continue;
            }
            if (obj.get(statKey).isJsonPrimitive() && obj.get(statKey).getAsJsonPrimitive().isNumber()) {
                additional.add(new ScalingComponent(parseEvaluator(statKey), obj.get(statKey).getAsDouble()));
            }
        }
    }
}
