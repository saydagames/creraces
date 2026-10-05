package mc.sayda.creraces.global;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.EntityMatcher;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import mc.sayda.creraces.util.CombatUtils;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * A parsed {@code data/creraces/globals/*.json} handler: an event trigger, optional
 * whitelist/blacklist against the event's subject entity, an actor-resolution order, and
 * either an ordinary condition/actions block or reused {@link TraitRegistry.RaceTrait} blocks
 * (for player-scoped triggers, applied to every player rather than run once per event).
 */
public final class GlobalHandler {
    private static final Set<String> RESERVED_KEYS = Set.of(
            "trigger", "triggers", "enabled", "whitelist", "blacklist", "actor", "actor_radius",
            "condition", "actions");

    public enum ActorSource {
        KILLER, VICTIM, VICTIM_OWNER, NEAREST_PLAYER;

        @Nullable
        static ActorSource fromString(String s) {
            for (ActorSource a : values()) {
                if (a.name().equalsIgnoreCase(s)) {
                    return a;
                }
            }
            return null;
        }
    }

    private final ResourceLocation id;
    private final Set<GlobalTrigger> triggers;
    private final boolean enabled;
    private final EntityMatcher whitelist;
    private final EntityMatcher blacklist;
    private final List<ActorSource> actorSources;
    private final double actorRadius;
    @Nullable
    private final Condition condition;
    private final List<ActionRegistry.RaceAction> actions;
    private final List<TraitRegistry.RaceTrait> traits;

    private GlobalHandler(ResourceLocation id, Set<GlobalTrigger> triggers, boolean enabled, EntityMatcher whitelist,
            EntityMatcher blacklist, List<ActorSource> actorSources, double actorRadius,
            @Nullable Condition condition, List<ActionRegistry.RaceAction> actions,
            List<TraitRegistry.RaceTrait> traits) {
        this.id = id;
        this.triggers = triggers;
        this.enabled = enabled;
        this.whitelist = whitelist;
        this.blacklist = blacklist;
        this.actorSources = actorSources;
        this.actorRadius = actorRadius;
        this.condition = condition;
        this.actions = actions;
        this.traits = traits;
    }

    public ResourceLocation id() {
        return id;
    }

    public Set<GlobalTrigger> triggers() {
        return triggers;
    }

    public List<TraitRegistry.RaceTrait> traits() {
        return traits;
    }

    public void run(GlobalTrigger trigger, LivingEntity subject, @Nullable Player killer) {
        if (!enabled) {
            return;
        }
        if (!whitelist.isEmpty() && !whitelist.matches(subject)) {
            return;
        }
        if (blacklist.matches(subject)) {
            return;
        }

        Player actor = resolveActor(subject, killer);
        if (actor == null) {
            CreRaces.LOGGER.debug("[globals/{}] {}: no resolvable actor, skipping", id, trigger.id());
            return;
        }

        ActionRegistry.RaceAction current = null;
        try {
            if (condition != null && !condition.evaluate(actor, subject, null, null)) {
                return;
            }
            for (ActionRegistry.RaceAction action : actions) {
                current = action;
                if (!action.execute(actor, subject, null, null)) {
                    break;
                }
            }
        } catch (Exception e) {
            String actionName = current != null ? current.getClass().getSimpleName() : "condition";
            GlobalDispatcher.reportFailure(id, trigger, actionName, e);
        }
    }

    @Nullable
    private Player resolveActor(LivingEntity subject, @Nullable Player killer) {
        for (ActorSource source : actorSources) {
            Player candidate = switch (source) {
                case KILLER -> killer;
                case VICTIM -> subject instanceof Player p ? p : null;
                case VICTIM_OWNER -> CombatUtils.getRootOwner(subject);
                case NEAREST_PLAYER -> nearestPlayer(subject);
            };
            if (candidate != null && !candidate.isRemoved() && candidate.level() == subject.level()) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Iterates level.players() manually rather than Level.getNearestPlayer, which applies
     * NO_CREATIVE_OR_SPECTATOR and would skip exactly the creative-mode player most likely to
     * be testing this.
     */
    @Nullable
    private Player nearestPlayer(LivingEntity subject) {
        if (!(subject.level() instanceof ServerLevel level)) {
            return null;
        }
        Player nearest = null;
        double nearestDistSq = actorRadius * actorRadius;
        for (Player p : level.players()) {
            double distSq = p.distanceToSqr(subject);
            if (distSq <= nearestDistSq) {
                nearestDistSq = distSq;
                nearest = p;
            }
        }
        return nearest;
    }

    public static GlobalHandler fromJson(ResourceLocation id, JsonObject json) {
        Set<GlobalTrigger> triggers = parseTriggers(json, id);
        boolean enabled = GsonHelper.getAsBoolean(json, "enabled", true);
        EntityMatcher whitelist = EntityMatcher.fromJson(json, "whitelist");
        EntityMatcher blacklist = EntityMatcher.fromJson(json, "blacklist");

        List<ActorSource> actorSources = parseActorSources(json, triggers);
        double actorRadius = GsonHelper.getAsDouble(json, "actor_radius", 128.0);

        Condition condition = json.has("condition") ? Condition.fromJson(json.getAsJsonObject("condition")) : null;

        List<ActionRegistry.RaceAction> actions = new ArrayList<>();
        if (json.has("actions")) {
            for (JsonElement e : json.getAsJsonArray("actions")) {
                actions.add(ActionRegistry.fromJson(e.getAsJsonObject()));
            }
        }

        List<TraitRegistry.RaceTrait> traits = parseTraits(json, id);

        return new GlobalHandler(id, triggers, enabled, whitelist, blacklist, actorSources, actorRadius, condition,
                actions, traits);
    }

    private static Set<GlobalTrigger> parseTriggers(JsonObject json, ResourceLocation id) {
        Set<GlobalTrigger> triggers = EnumSet.noneOf(GlobalTrigger.class);
        List<String> raw = new ArrayList<>();
        if (json.has("trigger")) {
            raw.add(GsonHelper.getAsString(json, "trigger"));
        }
        if (json.has("triggers") && json.get("triggers").isJsonArray()) {
            for (JsonElement e : json.getAsJsonArray("triggers")) {
                raw.add(e.getAsString());
            }
        }
        for (String s : raw) {
            GlobalTrigger t = GlobalTrigger.fromString(s);
            if (t != null) {
                triggers.add(t);
            } else {
                CreRaces.LOGGER.warn("globals/{}: unknown trigger '{}'", id, s);
            }
        }
        if (triggers.isEmpty()) {
            CreRaces.LOGGER.warn("globals/{}: no valid trigger/triggers - this handler will never run", id);
        }
        return triggers;
    }

    private static List<ActorSource> parseActorSources(JsonObject json, Set<GlobalTrigger> triggers) {
        List<ActorSource> sources = new ArrayList<>();
        if (json.has("actor") && json.get("actor").isJsonArray()) {
            for (JsonElement e : json.getAsJsonArray("actor")) {
                ActorSource source = ActorSource.fromString(e.getAsString());
                if (source != null) {
                    sources.add(source);
                }
            }
            if (!sources.isEmpty()) {
                return sources;
            }
        }
        if (triggers.contains(GlobalTrigger.ON_ENTITY_SPAWN)) {
            sources.add(ActorSource.NEAREST_PLAYER);
        } else if (triggers.contains(GlobalTrigger.ON_WORLD_TICK)) {
            sources.add(ActorSource.VICTIM);
        } else {
            sources.add(ActorSource.KILLER);
            sources.add(ActorSource.VICTIM);
            sources.add(ActorSource.VICTIM_OWNER);
        }
        return sources;
    }

    /**
     * Same discovery convention as RaceManager: any non-reserved top-level key whose value is
     * an array of trait objects becomes a list of player-scoped traits.
     */
    private static List<TraitRegistry.RaceTrait> parseTraits(JsonObject json, ResourceLocation id) {
        List<TraitRegistry.RaceTrait> traits = new ArrayList<>();
        for (var entry : json.entrySet()) {
            String key = entry.getKey();
            // Unprefixed, non-reserved keys are trait categories - the same convention
            // RaceManager uses, so a namespaced key is never mistaken for one.
            if (key.contains(":") || RESERVED_KEYS.contains(key)) {
                continue;
            }
            JsonElement value = entry.getValue();
            if (value.isJsonArray()) {
                JsonArray array = value.getAsJsonArray();
                for (int i = 0; i < array.size(); i++) {
                    JsonElement e = array.get(i);
                    if (e.isJsonObject()) {
                        traits.add(TraitRegistry.fromJson(e.getAsJsonObject(), id + ":" + key + ":" + i));
                    }
                }
            } else if (value.isJsonObject()) {
                JsonObject obj = value.getAsJsonObject();
                if (obj.has("type")) {
                    traits.add(TraitRegistry.fromJson(obj, id + ":" + key + ":0"));
                }
            }
        }
        return traits;
    }
}
