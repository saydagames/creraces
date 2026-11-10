package mc.sayda.creraces.engine;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.team.RaceTeamManager;
import mc.sayda.creraces.util.CombatUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * Decides which entities an area action (aoe, raycast, beam, tether, has_entities, on_hit, ...)
 * may pick, from a "targets" list of rules about how they relate to the caster. An entity is picked
 * when any allow rule matches and no deny ("!") rule does; a list with only deny rules allows
 * everything else.
 * <ul>
 * <li>all, self, allies (the caster included), enemies, players (the caster included), mobs</li>
 * <li>owned: pets, servants and other mobs whose owner is the caster</li>
 * </ul>
 * Example: ["enemies", "!players"] picks hostile mobs only. What an entity is or has (its type, race,
 * effects, flags) is checked with a "condition" next to the targets instead.
 */
@SuppressWarnings("null")
public class TargetFilter {
    private final List<BiPredicate<LivingEntity, Player>> allow;
    private final List<BiPredicate<LivingEntity, Player>> deny;

    private TargetFilter(List<BiPredicate<LivingEntity, Player>> allow, List<BiPredicate<LivingEntity, Player>> deny) {
        this.allow = allow;
        this.deny = deny;
    }

    public static TargetFilter fromJson(JsonObject json, String key, Set<String> defaultRules) {
        List<String> rules = new ArrayList<>();
        if (json.has(key) && json.get(key).isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray(key)) {
                rules.add(element.getAsString());
            }
        } else {
            rules.addAll(defaultRules);
        }
        return of(rules);
    }

    public static TargetFilter of(Iterable<String> rules) {
        List<BiPredicate<LivingEntity, Player>> allow = new ArrayList<>();
        List<BiPredicate<LivingEntity, Player>> deny = new ArrayList<>();
        boolean hasAllowRule = false;
        for (String rule : rules) {
            boolean negated = rule.startsWith("!");
            hasAllowRule |= !negated;
            BiPredicate<LivingEntity, Player> test = parseRule(negated ? rule.substring(1) : rule);
            if (test != null) {
                (negated ? deny : allow).add(test);
            }
        }
        // Only deny rules means "everyone else". Allow rules that all failed to parse allow nobody,
        // so a typo never widens what an action hits.
        if (!hasAllowRule) {
            allow.add((victim, caster) -> true);
        }
        return new TargetFilter(allow, deny);
    }

    public boolean isValid(LivingEntity victim, Player caster) {
        for (BiPredicate<LivingEntity, Player> test : deny) {
            if (test.test(victim, caster)) {
                return false;
            }
        }
        for (BiPredicate<LivingEntity, Player> test : allow) {
            if (test.test(victim, caster)) {
                return true;
            }
        }
        return false;
    }

    /** The caster counts as their own ally. */
    public static boolean isAlly(LivingEntity victim, Player caster) {
        return victim == caster || !RaceTeamManager.canHurt(victim, caster);
    }

    @Nullable
    private static BiPredicate<LivingEntity, Player> parseRule(String rule) {
        return switch (rule) {
            case "all" -> (victim, caster) -> true;
            case "self" -> (victim, caster) -> victim == caster;
            case "allies" -> TargetFilter::isAlly;
            case "enemies" -> (victim, caster) -> !isAlly(victim, caster);
            case "players" -> (victim, caster) -> victim instanceof Player;
            case "mobs" -> (victim, caster) -> !(victim instanceof Player);
            case "owned" -> (victim, caster) -> victim != caster && CombatUtils.getRootOwner(victim) == caster;
            default -> {
                CreRaces.LOGGER.error("Unknown target rule '{}' (all, self, allies, enemies, players, mobs, owned)", rule);
                yield null;
            }
        };
    }
}
