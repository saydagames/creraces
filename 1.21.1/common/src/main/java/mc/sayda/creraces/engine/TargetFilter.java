package mc.sayda.creraces.engine;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.creraces.team.RaceTeamManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;

/**
 * Handles complex targeting logic defined by an array of rules.
 * Examples of JSON:
 * "targets": ["enemies"] // Default if omitted
 * "targets": ["allies"] // Only allies
 * "targets": ["all", "!self"] // Everyone except the caster
 * "targets": ["players", "!allies"] // Enemy players only
 *
 * Supported filters:
 * "all", "self", "allies", "enemies", "players", "mobs"
 * "is_spirit" - entities currently in the spirit realm
 * "has_effect:id" - entities with the given mob effect active
 * "race:id" - player entities whose active race matches the ID
 * Prefix any of these with "!" to explicitly block that category.
 */
public class TargetFilter {
    private final Set<String> allow = new HashSet<>();
    private final Set<String> deny = new HashSet<>();

    public TargetFilter(Set<String> allow, Set<String> deny) {
        this(allow, deny, Set.of("enemies"));
    }

    public TargetFilter(Set<String> allow, Set<String> deny, Set<String> defaultAllow) {
        this.allow.addAll(allow);
        this.deny.addAll(deny);
        // Standardize: if no positive includes are specified but there are denies,
        // assume "all" is the baseline.
        if (this.allow.isEmpty() && !this.deny.isEmpty()) {
            this.allow.add("all");
        }
        // Default: If entirely empty, use the provided defaultAllow.
        if (this.allow.isEmpty() && this.deny.isEmpty()) {
            this.allow.addAll(defaultAllow);
        }
    }

    public static TargetFilter fromJson(JsonObject json, String key) {
        return fromJson(json, key, Set.of("enemies"));
    }

    public static TargetFilter fromJson(JsonObject json, String key, Set<String> defaultAllow) {
        Set<String> allow = new HashSet<>();
        Set<String> deny = new HashSet<>();

        if (json.has(key)) {
            JsonArray arr = json.getAsJsonArray(key);
            for (JsonElement e : arr) {
                String rule = e.getAsString().toLowerCase();
                if (rule.startsWith("!")) {
                    deny.add(rule.substring(1));
                } else {
                    allow.add(rule);
                }
            }
        } else {
            return new TargetFilter(defaultAllow, Set.of());
        }
        return new TargetFilter(allow, deny, defaultAllow);
    }

    /**
     * Evaluates if the victim passes this filter relative to the caster.
     */
    public boolean isValid(LivingEntity victim, Player caster) {
        // 1. Process explicit DENY rules first (they override allow rules)
        if (deny.contains("all"))
            return false;
        if (deny.contains("self") && victim.equals(caster))
            return false;
        if (deny.contains("players") && victim instanceof Player)
            return false;
        if (deny.contains("mobs") && !(victim instanceof Player))
            return false;

        boolean isAlly = victim.equals(caster) || !RaceTeamManager.canHurt(victim, caster);
        if (deny.contains("allies") && isAlly)
            return false;
        if (deny.contains("enemies") && !isAlly)
            return false;

        // Parameterized deny rules
        if (matchesParameterized(deny, victim, caster))
            return false;

        // 2. Process ALLOW rules
        boolean basicAllow = false;
        if (isAlly) {
            if (allow.contains("all")) basicAllow = true;
            else if (victim.equals(caster) && allow.contains("self")) basicAllow = true;
            else if (allow.contains("allies")) basicAllow = true;
        } else {
            if (allow.contains("all") || allow.contains("enemies")) basicAllow = true;
            else if (victim instanceof Player && allow.contains("players")) basicAllow = true;
            else if (!(victim instanceof Player) && allow.contains("mobs")) basicAllow = true;
        }

        if (basicAllow) return true;

        // Parameterized allow rules
        return matchesParameterized(allow, victim, caster);
    }

    /**
     * Smart-targeting resolution shared by several actions and conditions: prefer the
     * target if present, otherwise fall back to the caster unless useTarget requires
     * an explicit target (in which case null is returned).
     */
    public static @javax.annotation.Nullable LivingEntity resolveSmartTarget(Player player,
            @javax.annotation.Nullable LivingEntity target, boolean useTarget) {
        return (target != null) ? target : (useTarget ? null : player);
    }

    /**
     * Resolves the single-target subject for an action: prefer the target if present,
     * otherwise fall back to the caster. Invokes the callback only if the resolved
     * subject passes this filter.
     */
    public void applyToSingleTarget(Player player, @javax.annotation.Nullable LivingEntity target,
            java.util.function.BiConsumer<Player, LivingEntity> action) {
        if (target != null) {
            if (isValid(target, player)) {
                action.accept(player, target);
            }
        } else {
            if (isValid(player, player)) {
                action.accept(player, player);
            }
        }
    }

    private static boolean matchesParameterized(Set<String> rules, LivingEntity victim, Player caster) {
        for (String rule : rules) {
            if (rule.equals("is_spirit")) {
                if (isSpiritRealm(victim)) return true;
            } else if (rule.startsWith("has_effect:")) {
                String effectId = rule.substring("has_effect:".length());
                net.minecraft.core.Holder<MobEffect> eff = resolveEffect(effectId);
                if (eff != null && victim.hasEffect(eff)) return true;
            } else if (rule.startsWith("race:")) {
                String raceId = rule.substring("race:".length());
                if (victim instanceof Player p && matchesRace(p, raceId)) return true;
            }
        }
        return false;
    }

    private static boolean isSpiritRealm(LivingEntity entity) {
        if (!(entity instanceof Player player)) return false;
        return mc.sayda.creraces.capability.DataUtils.getVariables(player)
                .map(v -> v.isInSpiritRealm())
                .orElse(false);
    }

    private static net.minecraft.core.Holder<MobEffect> resolveEffect(String id) {
        if (!id.contains(":")) id = "minecraft:" + id;
        ResourceLocation loc = ResourceLocation.tryParse(id);
        if (loc == null) return null;
        return BuiltInRegistries.MOB_EFFECT.getHolder(loc)
                .map(h -> (net.minecraft.core.Holder<MobEffect>) h).orElse(null);
    }

    private static boolean matchesRace(Player player, String raceId) {
        return mc.sayda.creraces.capability.DataUtils.getVariables(player)
                .map(v -> {
                    ResourceLocation race = v.getRace();
                    if (race == null) return false;
                    if (!raceId.contains(":")) return race.getPath().equalsIgnoreCase(raceId);
                    return race.toString().equalsIgnoreCase(raceId);
                })
                .orElse(false);
    }
}
