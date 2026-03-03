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

public record ScalingValue(double base, @javax.annotation.Nullable String scalingStat, double factor,
        List<Component> additionalScales) {

    public record Component(String stat, double factor) {
    }

    public static final ScalingValue ZERO = new ScalingValue(0, null, 0, new ArrayList<>());

    public ScalingValue(double base, @javax.annotation.Nullable String scalingStat, double factor) {
        this(base, precomputeStat(scalingStat), factor, new ArrayList<>());
    }

    private static @javax.annotation.Nullable String precomputeStat(@javax.annotation.Nullable String stat) {
        return stat == null ? null : stat.toLowerCase();
    }

    public double evaluate(Player player) {
        return evaluate(player, null);
    }

    public double evaluate(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target) {
        double result = base;
        if (scalingStat != null && !scalingStat.isEmpty()) {
            result += evaluateStat(player, target, scalingStat) * factor;
        }

        for (Component comp : additionalScales) {
            result += evaluateStat(player, target, comp.stat().toLowerCase()) * comp.factor();
        }

        return result;
    }

    private double evaluateStat(Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            String statKey) {
        net.minecraft.world.entity.LivingEntity evalEntity = player;

        if (statKey.startsWith("target_")) {
            if (target == null)
                return 0.0;
            evalEntity = target;
            statKey = statKey.substring(7);
        }

        if (statKey.equals("ap") || statKey.equals("creraces:ability_power")) {
            @SuppressWarnings("null")
            var attr = ModAttributes.ABILITY_POWER.get();
            return attr != null ? evalEntity.getAttributeValue(attr) : 0.0;
        }
        if (statKey.equals("ad") || statKey.equals("creraces:attack_damage")) {
            @SuppressWarnings("null")
            var attr = ModAttributes.ATTACK_DAMAGE.get();
            return attr != null ? evalEntity.getAttributeValue(attr) : 0.0;
        }
        if (statKey.equals("ah") || statKey.equals("haste") || statKey.equals("creraces:ability_haste")) {
            @SuppressWarnings("null")
            var attr = ModAttributes.ABILITY_HASTE.get();
            if (attr == null)
                return 0.0;
            double val = evalEntity.getAttributeValue(attr);
            double cap = mc.sayda.creraces.config.CreRacesConfig.ABILITY_HASTE_CAP.get();
            return Math.min(val, cap);
        }
        if (statKey.equals("crit") || statKey.equals("cr") || statKey.equals("creraces:crit_rate")) {
            @SuppressWarnings("null")
            var attr = ModAttributes.CRIT_RATE.get();
            return attr != null ? evalEntity.getAttributeValue(attr) : 0.0;
        }
        if (statKey.equals("pen") || statKey.equals("creraces:armor_penetration")) {
            @SuppressWarnings("null")
            var attr = ModAttributes.ARMOR_PENETRATION.get();
            return attr != null ? evalEntity.getAttributeValue(attr) : 0.0;
        }
        if (statKey.equals("hp") || statKey.equals("health")) {
            return evalEntity.getHealth();
        }
        if (statKey.equals("max_hp") || statKey.equals("max_health")
                || statKey.equals("minecraft:generic.max_health")) {
            return evalEntity.getMaxHealth();
        }
        if (statKey.equals("armor") || statKey.equals("minecraft:generic.armor")) {
            return evalEntity.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
        }
        if (statKey.equals("mr") || statKey.equals("movement") || statKey.equals("speed")
                || statKey.equals("minecraft:generic.movement_speed")) {
            return evalEntity.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        }

        if (evalEntity instanceof Player p) {
            mc.sayda.creraces.capability.IPlayerVariables vars = DataUtils.getVariables(p).orElse(null);
            if (vars != null) {
                if (statKey.startsWith("custom:")) {
                    String key = statKey.substring(7);
                    String val = vars.getCustomization(key);
                    try {
                        return val != null ? Double.parseDouble(val) : 0.0;
                    } catch (NumberFormatException ignored) {
                    }
                } else if (statKey.startsWith("var:")) {
                    String key = statKey.substring(4);
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
                } else if (statKey.startsWith("ability:")) {
                    ResourceLocation abilityId = new ResourceLocation(statKey.substring(8));
                    return vars.getAbilityState(abilityId);
                }
            }
        }

        // Generic attribute fallback
        try {
            String lookupKey = statKey.contains(":") ? statKey : "minecraft:" + statKey;
            Attribute attr = BuiltInRegistries.ATTRIBUTE.get(new ResourceLocation(lookupKey));
            if (attr != null && evalEntity.getAttributes().hasAttribute(attr)) {
                return evalEntity.getAttributeValue(attr);
            }
        } catch (Exception ignored) {
        }

        return 0.0;
    }

    public static ScalingValue fromJson(JsonObject json, String key, double defaultBase) {
        if (!json.has(key)) {
            return new ScalingValue(defaultBase, null, 0);
        }

        if (json.get(key).isJsonObject()) {
            JsonObject obj = json.getAsJsonObject(key);
            double base = GsonHelper.getAsDouble(obj, "base", defaultBase);
            String stat = GsonHelper.getAsString(obj, "scales_with", null);
            double factor = GsonHelper.getAsDouble(obj, "factor", 0.0);

            List<Component> additional = new ArrayList<>();
            if (obj.has("scales") && obj.get("scales").isJsonArray()) {
                com.google.gson.JsonArray array = obj.getAsJsonArray("scales");
                for (int i = 0; i < array.size(); i++) {
                    JsonObject scaleObj = array.get(i).getAsJsonObject();
                    String s = GsonHelper.getAsString(scaleObj, "stat", null);
                    double f = GsonHelper.getAsDouble(scaleObj, "factor", 0.0);
                    if (s != null) {
                        additional.add(new Component(s, f));
                    }
                }
            }

            return new ScalingValue(base, stat, factor, additional);
        } else if (json.get(key).isJsonPrimitive() && json.get(key).getAsJsonPrimitive().isString()) {
            // String shorthand: treat as a stat key with factor 1.0 and base 0.0
            return new ScalingValue(0.0, json.get(key).getAsString(), 1.0, new ArrayList<>());
        } else {
            // Simple number
            return new ScalingValue(json.get(key).getAsDouble(), null, 0, new ArrayList<>());
        }
    }
}
