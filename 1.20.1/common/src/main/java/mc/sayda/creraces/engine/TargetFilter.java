package mc.sayda.creraces.engine;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.creraces.team.RaceTeamManager;
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
 * Prefix any of these with "!" to explicitly block that category.
 */
public class TargetFilter {
    private final Set<String> allow = new HashSet<>();
    private final Set<String> deny = new HashSet<>();

    public TargetFilter(Set<String> allow, Set<String> deny) {
        this.allow.addAll(allow);
        this.deny.addAll(deny);
        // Standardize: if no positive includes are specified but there are denies,
        // assume "all" is the baseline.
        if (this.allow.isEmpty() && !this.deny.isEmpty()) {
            this.allow.add("all");
        }
        // Default: If entirely empty, target enemies.
        if (this.allow.isEmpty() && this.deny.isEmpty()) {
            this.allow.add("enemies");
        }
    }

    public static TargetFilter fromJson(JsonObject json, String key) {
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
        }
        return new TargetFilter(allow, deny);
    }

    /**
     * Evaluates if the victim passes this filter relative to the caster.
     */
    public boolean isValid(LivingEntity victim, Player caster) {
        // 1. Process explicit DENY rules first (they override allow rules)
        if (deny.contains("all"))
            return false;
        if (deny.contains("self") && victim == caster)
            return false;
        if (deny.contains("players") && victim instanceof Player)
            return false;
        if (deny.contains("mobs") && !(victim instanceof Player))
            return false;

        boolean isAlly = victim == caster || !RaceTeamManager.canHurt(victim, caster);
        if (deny.contains("allies") && isAlly)
            return false;
        if (deny.contains("enemies") && !isAlly)
            return false;

        // 2. Process ALLOW rules
        // If it's an ally, it is ONLY valid if "allies" or "self" (if victim is caster)
        // is explicitly allowed.
        // Otherwise, it must pass the category filters (players/mobs/all/enemies).
        if (isAlly) {
            if (victim == caster && allow.contains("self"))
                return true;
            return allow.contains("allies");
        }

        // Not an ally - check category allows
        if (allow.contains("all") || allow.contains("enemies"))
            return true;

        if (victim instanceof Player) {
            return allow.contains("players");
        } else {
            return allow.contains("mobs");
        }
    }
}
