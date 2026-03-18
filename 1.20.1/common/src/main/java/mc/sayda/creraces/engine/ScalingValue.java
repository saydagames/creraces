package mc.sayda.creraces.engine;

import com.google.gson.JsonObject;
import mc.sayda.creraces.util.GsonHelper;
import mc.sayda.creraces.registry.ModAttributes;
import mc.sayda.creraces.capability.DataUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

public class ScalingValue {
    private final double base;
    private final Evaluator evaluator;
    private final double factor;
    private final List<ScalingComponent> additionalScales;

    public ScalingValue(double base, @javax.annotation.Nullable Evaluator evaluator, double factor,
            List<ScalingComponent> additionalScales) {
        this.base = base;
        this.evaluator = evaluator;
        this.factor = factor;
        this.additionalScales = additionalScales != null ? additionalScales : new java.util.ArrayList<>();
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

        public ScalingComponent(Evaluator evaluator, double factor) {
            this.evaluator = evaluator;
            this.factor = factor;
        }

        public Evaluator evaluator() {
            return evaluator;
        }

        public double factor() {
            return factor;
        }
    }

    public static final ScalingValue ZERO = new ScalingValue(0, null, 0, new ArrayList<>());

    @FunctionalInterface
    public interface Evaluator {
        double evaluate(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target);
    }

    public double evaluate(Player player) {
        return evaluate(player, null);
    }

    public double evaluate(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target) {
        double result = base;
        if (evaluator != null) {
            result += evaluator.evaluate(player, target) * factor;
        }

        for (int i = 0; i < additionalScales.size(); i++) {
            ScalingComponent comp = additionalScales.get(i);
            result += comp.evaluator().evaluate(player, target) * comp.factor();
        }

        return result;
    }

    private static Evaluator parseEvaluator(String statKey) {
        if (statKey == null || statKey.isEmpty())
            return (p, t) -> 0.0;

        final String finalKey = statKey.toLowerCase();

        if (finalKey.startsWith("target_")) {
            final String subKey = finalKey.substring(7);
            final Evaluator subEval = parseEvaluator(subKey);
            return (p, t) -> t != null ? subEval.evaluate(p, t) : 0.0;
        }

        // Hardcoded Attributes
        if (finalKey.equals("ap") || finalKey.equals("creraces:ability_power")) {
            return (p, t) -> {
                var attr = ModAttributes.ABILITY_POWER.get();
                return attr != null ? p.getAttributeValue(attr) : 0.0;
            };
        }
        if (finalKey.equals("ad") || finalKey.equals("creraces:attack_damage")) {
            return (p, t) -> {
                var attr = ModAttributes.ATTACK_DAMAGE.get();
                return attr != null ? p.getAttributeValue(attr) : 0.0;
            };
        }
        if (finalKey.equals("ah") || finalKey.equals("haste") || finalKey.equals("creraces:ability_haste")) {
            return (p, t) -> {
                var attr = ModAttributes.ABILITY_HASTE.get();
                if (attr == null)
                    return 0.0;
                double val = p.getAttributeValue(attr);
                double cap = 40.0;
                return Math.min(val, cap);
            };
        }
        if (finalKey.equals("crit") || finalKey.equals("cr") || finalKey.equals("creraces:crit_rate")) {
            return (p, t) -> {
                var attr = ModAttributes.CRIT_RATE.get();
                return attr != null ? p.getAttributeValue(attr) : 0.0;
            };
        }
        if (finalKey.equals("pen") || finalKey.equals("creraces:armor_penetration")) {
            return (p, t) -> {
                var attr = ModAttributes.ARMOR_PENETRATION.get();
                return attr != null ? p.getAttributeValue(attr) : 0.0;
            };
        }
        if (finalKey.equals("hp") || finalKey.equals("health")) {
            return (p, t) -> p.getHealth();
        }
        if (finalKey.equals("max_hp") || finalKey.equals("max_health")
                || finalKey.equals("minecraft:generic.max_health")) {
            return (p, t) -> p.getMaxHealth();
        }
        if (finalKey.equals("armor") || finalKey.equals("minecraft:generic.armor")) {
            return (p, t) -> p.getAttributeValue(
                    Objects.requireNonNull(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR));
        }
        if (finalKey.equals("mr") || finalKey.equals("movement") || finalKey.equals("speed")
                || finalKey.equals("minecraft:generic.movement_speed")) {
            return (p, t) -> p.getAttributeValue(
                    Objects.requireNonNull(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED));
        }

        // Custom Variables
        if (finalKey.startsWith("custom:")) {
            final String key = finalKey.substring(7);
            return (p, t) -> {
                mc.sayda.creraces.capability.IPlayerVariables vars = DataUtils.getVariables(p).orElse(null);
                if (vars != null) {
                    String val = vars.getCustomization(key);
                    try {
                        return val != null ? Double.parseDouble(val) : 0.0;
                    } catch (NumberFormatException ignored) {
                    }
                }
                return 0.0;
            };
        }

        if (finalKey.startsWith("var:")) {
            final String key = finalKey.substring(4);
            return (p, t) -> {
                mc.sayda.creraces.capability.IPlayerVariables vars = DataUtils.getVariables(p).orElse(null);
                if (vars != null) {
                    return switch (key) {
                        case "mana" -> vars.getMana();
                        case "energy" -> vars.getEnergy();
                        case "grit" -> vars.getGrit();
                        case "rage" -> vars.getRage();
                        case "karma" -> vars.getKarma();
                        case "souls" -> vars.getSouls();
                        case "stacks" -> vars.getStacks();
                        case "coins" -> vars.getCoins();
                        default -> 0.0;
                    };
                }
                return 0.0;
            };
        }

        if (finalKey.startsWith("ability:") || finalKey.startsWith("state:")) {
            final String subKey = finalKey.startsWith("ability:") ? finalKey.substring(8) : finalKey.substring(6);
            final ResourceLocation abilityId = ResourceLocation.tryParse(subKey);
            if (abilityId == null) {
                mc.sayda.creraces.CreRaces.LOGGER.error("[CreRaces] Invalid state ID in ScalingValue: {}", subKey);
                return (p, t) -> 0.0;
            }
            return (p, t) -> {
                mc.sayda.creraces.capability.IPlayerVariables vars = DataUtils.getVariables(p).orElse(null);
                return vars != null ? vars.getPersistentState(abilityId) : 0.0;
            };
        }

        // Generic fallback
        final String resLocStr = finalKey.contains(":") ? finalKey : "minecraft:" + finalKey;
        final ResourceLocation attrId = ResourceLocation.tryParse(resLocStr);
        if (attrId == null) {
            mc.sayda.creraces.CreRaces.LOGGER.error("[CreRaces] Invalid attribute/stat ID in ScalingValue: {}", resLocStr);
            return (p, t) -> 0.0;
        }

        return (p, t) -> {
            Attribute attr = BuiltInRegistries.ATTRIBUTE.get(attrId);
            if (attr != null && p.getAttributes().hasAttribute(attr)) {
                return p.getAttributeValue(attr);
            }
            return 0.0;
        };
    }

    public static ScalingValue fromJson(JsonObject json, String key, double defaultBase) {
        if (!json.has(key)) {
            return new ScalingValue(defaultBase, null, 0, new ArrayList<>());
        }

        if (json.get(key).isJsonObject()) {
            JsonObject obj = json.getAsJsonObject(key);
            double base = GsonHelper.getAsDouble(obj, "base", defaultBase);
            String stat = GsonHelper.getNullableString(obj, "scales_with", null);
            double factor = GsonHelper.getAsDouble(obj, "factor", 1.0);

            List<ScalingComponent> additional = new ArrayList<>();

            if (obj.has("scales") && obj.get("scales").isJsonArray()) {
                com.google.gson.JsonArray array = obj.getAsJsonArray("scales");
                for (int i = 0; i < array.size(); i++) {
                    JsonObject scaleObj = array.get(i).getAsJsonObject();
                    String s = GsonHelper.getNullableString(scaleObj, "stat", null);
                    double f = GsonHelper.getAsDouble(scaleObj, "factor", 1.0);
                    if (s != null) {
                        additional.add(new ScalingComponent(parseEvaluator(s), f));
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
                        || sKey.equals("scales") || sKey.equals("scaling")) {
                    continue;
                }
                if (obj.get(sKey).isJsonPrimitive() && obj.get(sKey).getAsJsonPrimitive().isNumber()) {
                    additional.add(new ScalingComponent(parseEvaluator(sKey), obj.get(sKey).getAsDouble()));
                }
            }

            return new ScalingValue(base, parseEvaluator(stat), factor, additional);
        } else if (json.get(key).isJsonPrimitive() && json.get(key).getAsJsonPrimitive().isString()) {
            return new ScalingValue(0.0, parseEvaluator(json.get(key).getAsString()), 1.0, new ArrayList<>());
        } else {
            return new ScalingValue(json.get(key).getAsDouble(), null, 0, new ArrayList<>());
        }
    }
}
