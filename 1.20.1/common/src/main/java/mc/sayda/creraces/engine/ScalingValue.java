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

    public static ScalingValue fixed(double value) {
        return new ScalingValue(value, null, 0, false, new ArrayList<>(), MathOp.NONE, null, null);
    }

    @FunctionalInterface
    public interface Evaluator {
        double evaluate(net.minecraft.world.entity.LivingEntity subject, Player player,
                @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
                @javax.annotation.Nullable AbilitySlot slot,
                @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos);
    }

    public double evaluate(Player player) {
        return evaluate(player, null);
    }

    public double evaluate(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target) {
        return evaluate(player, target, null);
    }

    public double evaluate(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable AbilitySlot slot) {
        return evaluate(player, target, slot, null);
    }

    public double evaluate(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable AbilitySlot slot, @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        double result = base;
        if (evaluator != null) {
            net.minecraft.world.entity.LivingEntity subject = (useTarget && target != null) ? target : player;
            result += evaluator.evaluate(subject, player, target, slot, interact_pos) * factor;
        }

        for (int i = 0; i < additionalScales.size(); i++) {
            ScalingComponent comp = additionalScales.get(i);
            net.minecraft.world.entity.LivingEntity subject = (comp.useTarget() && target != null) ? target : player;
            result += comp.evaluator().evaluate(subject, player, target, slot, interact_pos) * comp.factor();
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

        final String finalKey = statKey.toLowerCase();

        // Hardcoded Attributes
        if (finalKey.equals("creraces:ap") || finalKey.equals("creraces:ability_power")) {
            return (s, p, t, sl, ip) -> {
                Attribute attr = ModAttributes.resolve(ModAttributes.ABILITY_POWER);
                return attr != null ? s.getAttributeValue(attr) : 0.0;
            };
        }
        if (finalKey.equals("creraces:ad") || finalKey.equals("creraces:attack_damage")) {
            return (s, p, t, sl, ip) -> {
                Attribute attr = ModAttributes.resolve(ModAttributes.ATTACK_DAMAGE);
                return attr != null ? s.getAttributeValue(attr) : 0.0;
            };
        }
        if (finalKey.equals("creraces:crit") || finalKey.equals("creraces:crit_rate")) {
            return (s, p, t, sl, ip) -> {
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
            return (s, p, t, sl, ip) -> s.getHealth();
        }
        if (finalKey.equals("creraces:max_health")) {
            return (s, p, t, sl, ip) -> s.getMaxHealth();
        }
        if (finalKey.equals("creraces:ability_haste")) {
            return (s, p, t, sl, ip) -> {
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
            return (s, p, t, sl, ip) -> {
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
        if (finalKey.startsWith("race:")) {
            final String key = finalKey.substring(5);
            return (s, p, t, sl, ip) -> {
                if (!(s instanceof Player sp)) {
                    return 0.0;
                }
                mc.sayda.creraces.capability.IPlayerVariables vars = DataUtils.getVariables(sp).orElse(null);
                if (vars == null) {
                    return 0.0;
                }
                mc.sayda.creraces.race.Race race = mc.sayda.creraces.race.RaceRegistry.get(vars.getRace());
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

        if (finalKey.startsWith("custom:")) {
            final String key = finalKey.substring(7);
            return (s, p, t, sl, ip) -> {
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
            return (s, p, t, sl, ip) -> {
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
                            case "level" -> {
                                if (sl != null) {
                                    ResourceLocation id = vars.getAbilityInSlot(sl);
                                    yield id != null ? (double) vars.getAbilityLevel(id) : 1.0;
                                }
                                if (vars.isAbilityActive()) {
                                    ResourceLocation id = vars.getActiveAbility();
                                    yield id != null ? (double) vars.getAbilityLevel(id) : 1.0;
                                }
                                yield 1.0;
                            }
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
                return (s, p, t, sl, ip) -> {
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
                return (s, p, t, sl, ip) -> 0.0;
            }
            return (s, p, t, sl, ip) -> {
                mc.sayda.creraces.capability.IPlayerVariables vars = DataUtils.getVariables(p).orElse(null);
                return vars != null ? vars.getPersistentState(abilityId) : 0.0;
            };
        }

        if (finalKey.startsWith("effect(") && finalKey.endsWith(")")) {
            String content = finalKey.substring(7, finalKey.length() - 1);
            String[] parts = content.split(",", 2);
            if (parts.length == 2) {
                String idStr = parts[0].trim();
                final String property = parts[1].trim().toLowerCase();
                if (!idStr.contains(":")) {
                    idStr = "minecraft:" + idStr;
                }
                final ResourceLocation effectId = ResourceLocation.tryParse(idStr);
                if (effectId != null) {
                    return (s, p, t, sl, ip) -> {
                        net.minecraft.world.effect.MobEffect effect = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.get(effectId);
                        if (effect != null && s.hasEffect(effect)) {
                            net.minecraft.world.effect.MobEffectInstance inst = s.getEffect(effect);
                            if (inst != null) {
                                return switch (property) {
                                    case "duration" -> (double) inst.getDuration();
                                    case "amplifier" -> (double) inst.getAmplifier();
                                    case "visible" -> inst.isVisible() ? 1.0 : 0.0;
                                    case "ambient" -> inst.isAmbient() ? 1.0 : 0.0;
                                    default -> 0.0;
                                };
                            }
                        }
                        return 0.0;
                    };
                }
            }
        }

        if (finalKey.startsWith("config:")) {
            final String configKey = finalKey.substring(7).toUpperCase();
            return (s, p, t, sl, ip) -> {
                try {
                    java.lang.reflect.Field field = mc.sayda.creraces.config.CreRacesConfig.class.getField(configKey);
                    Object val = field.get(null);
                    if (val instanceof java.util.function.Supplier<?> supplier) {
                        Object result = supplier.get();
                        if (result instanceof Number num) {
                            return num.doubleValue();
                        } else if (result instanceof Boolean bool) {
                            return bool ? 1.0 : 0.0;
                        }
                    }
                } catch (Exception e) {
                    mc.sayda.creraces.CreRaces.LOGGER.error("Failed to read config value '{}' in ScalingValue", configKey);
                }
                return 0.0;
            };
        }

        // Generic fallback
        final String resLocStr = finalKey.contains(":") ? finalKey : "minecraft:" + finalKey;
        final ResourceLocation attrId = ResourceLocation.tryParse(resLocStr);
        if (attrId == null) {
            mc.sayda.creraces.CreRaces.LOGGER.error("Invalid attribute/stat ID in ScalingValue: {}",
                    resLocStr);
            return (s, p, t, sl, ip) -> 0.0;
        }

        return (s, p, t, sl, ip) -> {
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
            JsonObject obj = json.get(key).getAsJsonObject();
            double base = GsonHelper.getAsDouble(obj, "base", defaultBase);
            String stat = GsonHelper.getNullableString(obj, "scales_with", null);
            double factor = GsonHelper.getAsDouble(obj, "factor", 1.0);
            boolean useTargetRoot = GsonHelper.getAsBoolean(obj, "use_target", false);

            List<ScalingComponent> additional = new ArrayList<>();

            if (obj.has("scales") && obj.get("scales").isJsonArray()) {
                com.google.gson.JsonArray array = obj.getAsJsonArray("scales");
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
                JsonObject scalingObj = obj.get("scaling").getAsJsonObject();
                for (String sKey : scalingObj.keySet()) {
                    if (scalingObj.get(sKey).isJsonPrimitive()
                            && scalingObj.get(sKey).getAsJsonPrimitive().isNumber()) {
                        additional.add(new ScalingComponent(parseEvaluator(sKey), scalingObj.get(sKey).getAsDouble()));
                    }
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
        } else if (json.get(key).isJsonPrimitive()) {
            com.google.gson.JsonPrimitive primitive = json.get(key).getAsJsonPrimitive();
            if (primitive.isString()) {
                return new ScalingValue(0.0, parseEvaluator(primitive.getAsString()), 1.0, false, new ArrayList<>(),
                        MathOp.NONE, null, null);
            } else if (primitive.isNumber()) {
                return new ScalingValue(primitive.getAsDouble(), null, 0, false, new ArrayList<>(), MathOp.NONE, null,
                        null);
            }
        }
        return new ScalingValue(defaultBase, null, 0, false, new ArrayList<>(), MathOp.NONE, null, null);
    }
}
