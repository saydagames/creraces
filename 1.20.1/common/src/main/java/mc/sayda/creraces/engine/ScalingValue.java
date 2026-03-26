package mc.sayda.creraces.engine;

import com.google.gson.JsonObject;
import mc.sayda.creraces.util.GsonHelper;
import mc.sayda.creraces.registry.ModAttributes;
import mc.sayda.creraces.capability.DataUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import mc.sayda.creraces.ability.AbilitySlot;

import java.util.List;
import java.util.ArrayList;

public class ScalingValue {
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

    public ScalingValue(double base, @javax.annotation.Nullable Evaluator evaluator, double factor,
            List<ScalingComponent> additionalScales) {
        this(base, evaluator, factor, false, additionalScales, MathOp.NONE, null, null);
    }

    public ScalingValue(double base, @javax.annotation.Nullable Evaluator evaluator, double factor, boolean useTarget,
            List<ScalingComponent> additionalScales, MathOp math, Double min, Double max) {
        this.base = base;
        this.evaluator = evaluator;
        this.factor = factor;
        this.useTarget = useTarget;
        this.additionalScales = additionalScales != null ? additionalScales : new java.util.ArrayList<>();
        this.math = math != null ? math : MathOp.NONE;
        this.min = min;
        this.max = max;
    }

    public double base() {
        return base;
    }

    public @javax.annotation.Nullable Evaluator evaluator() {
        return evaluator;
    }

    public double factor() {
        return factor;
    }

    public List<ScalingComponent> additionalScales() {
        return additionalScales;
    }

    public boolean isZero() {
        return base == 0 && evaluator == null && additionalScales.isEmpty();
    }

    public boolean isConstant() {
        return evaluator == null && additionalScales.isEmpty();
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

    public static final ScalingValue ZERO = new ScalingValue(0, null, 0, new ArrayList<>());

    @FunctionalInterface
    public interface Evaluator {
        double evaluate(net.minecraft.world.entity.LivingEntity subject, Player player,
                @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
                @javax.annotation.Nullable AbilitySlot slot);
    }

    public double evaluate(Player player) {
        return evaluate(player, null);
    }

    public double evaluate(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target) {
        return evaluate(player, target, null);
    }

    public double evaluate(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable AbilitySlot slot) {
        double result = base;
        if (evaluator != null) {
            net.minecraft.world.entity.LivingEntity subject = (useTarget && target != null) ? target : player;
            result += evaluator.evaluate(subject, player, target, slot) * factor;
        }

        for (int i = 0; i < additionalScales.size(); i++) {
            ScalingComponent comp = additionalScales.get(i);
            net.minecraft.world.entity.LivingEntity subject = (comp.useTarget() && target != null) ? target : player;
            result += comp.evaluator().evaluate(subject, player, target, slot) * comp.factor();
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
            return (s, p, t, sl) -> 0.0;

        final String finalKey = statKey.toLowerCase();

        // Hardcoded Attributes
        if (finalKey.equals("creraces:ap") || finalKey.equals("creraces:ability_power")) {
            return (s, p, t, sl) -> {
                Attribute attr = ModAttributes.resolve(ModAttributes.ABILITY_POWER);
                return attr != null ? s.getAttributeValue(attr) : 0.0;
            };
        }
        if (finalKey.equals("creraces:ad") || finalKey.equals("creraces:attack_damage")) {
            return (s, p, t, sl) -> {
                Attribute attr = ModAttributes.resolve(ModAttributes.ATTACK_DAMAGE);
                return attr != null ? s.getAttributeValue(attr) : 0.0;
            };
        }
        if (finalKey.equals("creraces:crit") || finalKey.equals("creraces:crit_rate")) {
            return (s, p, t, sl) -> {
                Attribute attr = ModAttributes.resolve(ModAttributes.CRIT_RATE);
                if (attr == null)
                    return 0.0;
                double val = s.getAttributeValue(attr);
                if (ModAttributes.isPercentAttribute(attr))
                    val *= 100.0;
                return val;
            };
        }
        if (finalKey.equals("creraces:health")) {
            return (s, p, t, sl) -> s.getHealth();
        }
        if (finalKey.equals("creraces:max_health")) {
            return (s, p, t, sl) -> s.getMaxHealth();
        }
        if (finalKey.equals("creraces:ability_haste")) {
            return (s, p, t, sl) -> {
                Attribute attr = ModAttributes.resolve(ModAttributes.ABILITY_HASTE);
                if (attr == null)
                    return 0.0;
                double val = s.getAttributeValue(attr);
                if (ModAttributes.isPercentAttribute(attr))
                    val *= 100.0;
                double cap = 40.0;
                return Math.min(val, cap);
            };
        }
        if (finalKey.equals("creraces:armor_shred")) {
            return (s, p, t, sl) -> {
                Attribute attr = ModAttributes.resolve(ModAttributes.ARMOR_SHRED);
                if (attr == null)
                    return 0.0;
                double val = s.getAttributeValue(attr);
                if (ModAttributes.isPercentAttribute(attr))
                    val *= 100.0;
                return val;
            };
        }

        // Custom Variables
        if (finalKey.startsWith("custom:")) {
            final String key = finalKey.substring(7);
            return (s, p, t, sl) -> {
                if (s instanceof Player sp) {
                    mc.sayda.creraces.capability.IPlayerVariables vars = DataUtils.getVariables(sp).orElse(null);
                    if (vars != null) {
                        String val = vars.getCustomization(key);
                        try {
                            return val != null ? Double.parseDouble(val) : 0.0;
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
                return 0.0;
            };
        }

        if (finalKey.startsWith("var:")) {
            final String key = finalKey.substring(4);
            return (s, p, t, sl) -> {
                if (s instanceof Player sp) {
                    mc.sayda.creraces.capability.IPlayerVariables vars = DataUtils.getVariables(sp).orElse(null);
                    if (vars != null) {
                        return switch (key) {
                            case "mana" -> vars.getMana();
                            case "energy" -> vars.getEnergy();
                            case "grit" -> vars.getGrit();
                            case "rage" -> vars.getRage();
                            case "karma" -> vars.getKarma();
                            case "soul" -> vars.getSoul();
                            case "coins" -> vars.getCoins();
                            case "gstate" -> (double) vars.getGState();
                            default -> 0.0;
                        };
                    }
                }
                return 0.0;
            };
        }

        if (finalKey.startsWith("state:")) {
            String subKey = finalKey.substring(6);
            if (subKey.equals("self")) {
                return (s, p, t, sl) -> {
                    if (sl != null) {
                        mc.sayda.creraces.capability.IPlayerVariables vars = DataUtils.getVariables(p).orElse(null);
                        if (vars != null) {
                            ResourceLocation abilityId = vars.getAbilityInSlot(sl);
                            return abilityId != null ? vars.getPersistentState(abilityId) : 0.0;
                        }
                    }
                    return 0.0;
                };
            }
            if (!subKey.contains(":")) {
                subKey = "creraces:" + subKey;
            }
            final ResourceLocation abilityId = ResourceLocation.tryParse(subKey);
            if (abilityId == null) {
                mc.sayda.creraces.CreRaces.LOGGER.error("Invalid state ID in ScalingValue: {}", subKey);
                return (s, p, t, sl) -> 0.0;
            }
            return (s, p, t, sl) -> {
                mc.sayda.creraces.capability.IPlayerVariables vars = DataUtils.getVariables(p).orElse(null);
                return vars != null ? vars.getPersistentState(abilityId) : 0.0;
            };
        }

        // Generic fallback
        final String resLocStr = finalKey.contains(":") ? finalKey : "minecraft:" + finalKey;
        final ResourceLocation attrId = ResourceLocation.tryParse(resLocStr);
        if (attrId == null) {
            mc.sayda.creraces.CreRaces.LOGGER.error("Invalid attribute/stat ID in ScalingValue: {}",
                    resLocStr);
            return (s, p, t, sl) -> 0.0;
        }

        return (s, p, t, sl) -> {
            Attribute attr = BuiltInRegistries.ATTRIBUTE.get(attrId);
            if (attr != null && s.getAttributes().hasAttribute(attr)) {
                double val = s.getAttributeValue(attr);
                if (ModAttributes.isPercentAttribute(attr))
                    val *= 100.0;
                return val;
            }
            return 0.0;
        };
    }

    public static ScalingValue fromJson(JsonObject json, String key, double defaultBase) {
        if (!json.has(key)) {
            return new ScalingValue(defaultBase, null, 0, false, new ArrayList<>(), MathOp.NONE, null, null);
        }

        if (json.get(key).isJsonObject()) {
            JsonObject obj = json.getAsJsonObject(key);
            double base = GsonHelper.getAsDouble(obj, "base", defaultBase);
            String stat = GsonHelper.getNullableString(obj, "scales_with", null);
            double factor = GsonHelper.getAsDouble(obj, "factor", 1.0);
            boolean useTargetRoot = GsonHelper.getAsBoolean(obj, "use_target", false);

            List<ScalingComponent> additional = new ArrayList<>();

            if (obj.has("scales") && obj.get("scales").isJsonArray()) {
                com.google.gson.JsonArray array = obj.getAsJsonArray("scales");
                for (int i = 0; i < array.size(); i++) {
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
                JsonObject scalingObj = obj.getAsJsonObject("scaling");
                for (String sKey : scalingObj.keySet()) {
                    additional.add(new ScalingComponent(parseEvaluator(sKey), scalingObj.get(sKey).getAsDouble()));
                }
            }

            for (String sKey : obj.keySet()) {
                if (sKey.equals("base") || sKey.equals("scales_with") || sKey.equals("factor")
                        || sKey.equals("scales") || sKey.equals("scaling") || sKey.equals("math")
                        || sKey.equals("min") || sKey.equals("max")) {
                    continue;
                }
                if (obj.get(sKey).isJsonPrimitive() && obj.get(sKey).getAsJsonPrimitive().isNumber()) {
                    additional.add(new ScalingComponent(parseEvaluator(sKey), obj.get(sKey).getAsDouble()));
                }
            }

            MathOp math = MathOp.NONE;
            if (obj.has("math")) {
                try {
                    math = MathOp.valueOf(obj.get("math").getAsString().toUpperCase());
                } catch (Exception e) {
                    mc.sayda.creraces.CreRaces.LOGGER.warn("Unknown math operation: {}", obj.get("math").getAsString());
                }
            }
            Double min = obj.has("min") ? obj.get("min").getAsDouble() : null;
            Double max = obj.has("max") ? obj.get("max").getAsDouble() : null;

            return new ScalingValue(base, parseEvaluator(stat), factor, useTargetRoot, additional, math, min, max);
        } else if (json.get(key).isJsonPrimitive() && json.get(key).getAsJsonPrimitive().isString()) {
            return new ScalingValue(0.0, parseEvaluator(json.get(key).getAsString()), 1.0, false, new ArrayList<>(),
                    MathOp.NONE, null, null);
        } else {
            return new ScalingValue(json.get(key).getAsDouble(), null, 0, false, new ArrayList<>(), MathOp.NONE, null,
                    null);
        }
    }
}
