package mc.sayda.creraces.engine;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.team.RaceTeamManager;
import mc.sayda.creraces.util.RaceUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Decides which entities an action or condition may affect, from a JSON array of rules.
 * Examples:
 * "targets": ["enemies"] // Default if omitted
 * "targets": ["allies"] // Only allies
 * "targets": ["all", "!self"] // Everyone except the caster
 * "targets": ["players", "!allies"] // Enemy players only
 *
 * Supported filters:
 * "all", "self", "allies", "enemies"
 * "players", "mobs" - by entity kind, ally or not ("players" includes the caster; narrow with "!self" or "!allies")
 * "is_spirit" - spirits (spirit races, and mobs tagged creraces:spirit), wherever they are
 * "in_spirit_realm" - entities currently in the spirit realm (players, and mobs tagged creraces:in_spirit_realm)
 * "has_effect:id" - entities with the given mob effect active
 * "race:id" - player entities whose active race matches the ID
 * Prefix any of these with "!" to block that category; deny rules beat allow rules.
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
        // A deny-only list such as ["!self"] means "everyone except ...".
        if (this.allow.isEmpty() && !this.deny.isEmpty()) {
            this.allow.add("all");
        }
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

    public boolean isValid(LivingEntity victim, Player caster) {
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

        if (matchesParameterized(deny, victim, caster))
            return false;

        boolean basicAllow = false;
        if (allow.contains("all")) basicAllow = true;
        else if (victim instanceof Player && allow.contains("players")) basicAllow = true;
        else if (!(victim instanceof Player) && allow.contains("mobs")) basicAllow = true;
        else if (isAlly) {
            if (victim.equals(caster) && allow.contains("self")) basicAllow = true;
            else if (allow.contains("allies")) basicAllow = true;
        } else if (allow.contains("enemies")) basicAllow = true;

        if (basicAllow) return true;

        return matchesParameterized(allow, victim, caster);
    }

    /**
     * Smart-targeting resolution shared by several actions and conditions: prefer the
     * target if present, otherwise fall back to the caster unless useTarget requires
     * an explicit target (in which case null is returned).
     */
    public static @Nullable LivingEntity resolveSmartTarget(Player player, @Nullable LivingEntity target,
            boolean useTarget) {
        return (target != null) ? target : (useTarget ? null : player);
    }

    /**
     * Resolves the single-target subject for an action: prefer the target if present,
     * otherwise fall back to the caster. Invokes the callback only if the resolved
     * subject passes this filter.
     */
    public void applyToSingleTarget(Player player, @Nullable LivingEntity target,
            BiConsumer<Player, LivingEntity> action) {
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
                if (isSpirit(victim)) return true;
            } else if (rule.equals("in_spirit_realm")) {
                if (isInSpiritRealm(victim)) return true;
            } else if (rule.startsWith("has_effect:")) {
                String effectId = rule.substring("has_effect:".length());
                MobEffect eff = resolveEffect(effectId);
                if (eff != null && victim.hasEffect(eff)) return true;
            } else if (rule.startsWith("race:")) {
                String raceId = rule.substring("race:".length());
                if (victim instanceof Player p && matchesRace(p, raceId)) return true;
            }
        }
        return false;
    }

    // Being a spirit is identity and holds anywhere; being in the realm only decides whether
    // spirit-plane rules apply right now. A human in the realm is still a human.
    private static boolean isSpirit(LivingEntity entity) {
        if (entity instanceof Player player) {
            return RaceUtils.isSpirit(player);
        }
        return entity.getTags().contains("creraces:spirit");
    }

    private static boolean isInSpiritRealm(LivingEntity entity) {
        if (entity instanceof Player player) {
            return DataUtils.getVariables(player).map(IPlayerVariables::isInSpiritRealm).orElse(false);
        }
        return entity.getTags().contains("creraces:in_spirit_realm");
    }

    private static MobEffect resolveEffect(String id) {
        if (!id.contains(":")) id = "minecraft:" + id;
        ResourceLocation loc = ResourceLocation.tryParse(id);
        return loc != null ? BuiltInRegistries.MOB_EFFECT.get(loc) : null;
    }

    private static boolean matchesRace(Player player, String raceId) {
        return DataUtils.getVariables(player)
                .map(v -> {
                    ResourceLocation race = v.getRace();
                    if (race == null) return false;
                    if (!raceId.contains(":")) return race.getPath().equalsIgnoreCase(raceId);
                    return race.toString().equalsIgnoreCase(raceId);
                })
                .orElse(false);
    }
}
