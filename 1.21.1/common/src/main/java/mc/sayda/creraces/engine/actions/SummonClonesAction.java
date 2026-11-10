package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.CloneManager;
import mc.sayda.creraces.engine.Position;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.entity.CloneEntity;
import mc.sayda.creraces.registry.ModEntities;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Summons copies of the caster that repeat their movement around a "center": each entry of
 * "clones" turns it by "rotation" degrees, flips it with "mirror" (left_right, front_back), shifts
 * it by "offset" (right, up, forward, from the direction faced when cast) and can lag "delay" ticks
 * behind. Clones last "duration" ticks (-1 until broken), break after "health" damage and can copy
 * the caster's abilities and attacks, deal "damage_multiplier" times the damage, pass on
 * "damage_to_owner" of the hits they take, and pull mobs off the caster ("distract"). "on_break" and
 * "on_expire" run with the clone as the source.
 */
@SuppressWarnings("null")
public class SummonClonesAction implements ActionRegistry.RaceAction {
    /** A cap on clones per cast, so a typo can't fill the world with them. */
    private static final int MAX_CLONES = 16;

    private record CloneSpec(ScalingValue rotation, CloneEntity.Mirror mirror, ScalingValue right, ScalingValue up,
            ScalingValue forward, ScalingValue delay) {
    }

    private final Position center;
    private final List<CloneSpec> clones;
    private final ScalingValue duration;
    private final ScalingValue health;
    private final ScalingValue damageMultiplier;
    private final ScalingValue damageToOwner;
    private final boolean copyAbilities;
    private final boolean copyAttacks;
    private final boolean distract;
    private final boolean replace;
    private final List<ActionRegistry.RaceAction> onBreak;
    private final List<ActionRegistry.RaceAction> onExpire;

    private SummonClonesAction(Position center, List<CloneSpec> clones, ScalingValue duration, ScalingValue health,
            ScalingValue damageMultiplier, ScalingValue damageToOwner, boolean copyAbilities, boolean copyAttacks,
            boolean distract,
            boolean replace, List<ActionRegistry.RaceAction> onBreak, List<ActionRegistry.RaceAction> onExpire) {
        this.center = center;
        this.clones = clones;
        this.duration = duration;
        this.health = health;
        this.damageMultiplier = damageMultiplier;
        this.damageToOwner = damageToOwner;
        this.copyAbilities = copyAbilities;
        this.copyAttacks = copyAttacks;
        this.distract = distract;
        this.replace = replace;
        this.onBreak = onBreak;
        this.onExpire = onExpire;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (player.level().isClientSide()) {
            return true;
        }
        Vec3 point = center.resolve(player, target, slot, interactPos);
        if (point == null) {
            return false;
        }
        if (replace) {
            CloneManager.removeClones(player);
        }

        CloneEntity.Behaviour behaviour = new CloneEntity.Behaviour(
                (int) duration.evaluate(player, target, slot, interactPos),
                damageMultiplier.evaluate(player, target, slot, interactPos),
                damageToOwner.evaluate(player, target, slot, interactPos),
                copyAbilities, copyAttacks, distract, onBreak, onExpire);
        float hp = (float) health.evaluate(player, target, slot, interactPos);
        for (CloneSpec spec : clones) {
            CloneEntity clone = ModEntities.CLONE.get().create(player.level());
            if (clone == null) {
                return false;
            }
            CloneEntity.Placement placement = new CloneEntity.Placement(
                    (float) spec.rotation().evaluate(player, target, slot, interactPos),
                    spec.mirror(),
                    new Vec3(spec.right().evaluate(player, target, slot, interactPos),
                            spec.up().evaluate(player, target, slot, interactPos),
                            spec.forward().evaluate(player, target, slot, interactPos)),
                    (int) spec.delay().evaluate(player, target, slot, interactPos));
            clone.setup(player, point, player.getYRot(), placement, behaviour, hp);
            if (player.level().addFreshEntity(clone)) {
                CloneManager.track(clone, player);
            }
        }
        return true;
    }

    private static CloneSpec parseClone(JsonObject json) {
        String mirrorName = GsonHelper.getAsString(json, "mirror", "none");
        CloneEntity.Mirror mirror;
        try {
            mirror = CloneEntity.Mirror.valueOf(mirrorName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown mirror '" + mirrorName + "' (none, left_right, front_back)");
        }
        JsonObject offset = json.has("offset") ? json.getAsJsonObject("offset") : new JsonObject();
        return new CloneSpec(
                ScalingValue.fromJson(json, "rotation", 0.0),
                mirror,
                ScalingValue.fromJson(offset, "right", 0.0),
                ScalingValue.fromJson(offset, "up", 0.0),
                ScalingValue.fromJson(offset, "forward", 0.0),
                ScalingValue.fromJson(json, "delay", 0.0));
    }

    public static void register() {
        ActionRegistry.register(CreRaces.id("summon_clones"), json -> {
            if (!json.has("clones") || !json.get("clones").isJsonArray()) {
                throw new IllegalArgumentException("needs a \"clones\" list");
            }
            List<CloneSpec> clones = new ArrayList<>();
            for (JsonElement element : json.getAsJsonArray("clones")) {
                if (clones.size() == MAX_CLONES) {
                    throw new IllegalArgumentException("at most " + MAX_CLONES + " clones per cast");
                }
                clones.add(parseClone(element.getAsJsonObject()));
            }
            return new SummonClonesAction(
                    Position.fromJson(json, "center", Position.of(Position.Source.SELF)),
                    clones,
                    ScalingValue.fromJson(json, "duration", 200.0),
                    ScalingValue.fromJson(json, "health", 1.0),
                    ScalingValue.fromJson(json, "damage_multiplier", 1.0),
                    ScalingValue.fromJson(json, "damage_to_owner", 0.0),
                    GsonHelper.getAsBoolean(json, "copy_abilities", false),
                    GsonHelper.getAsBoolean(json, "copy_attacks", false),
                    GsonHelper.getAsBoolean(json, "distract", false),
                    GsonHelper.getAsBoolean(json, "replace", true),
                    ActionRegistry.listFromJson(json, "on_break"),
                    ActionRegistry.listFromJson(json, "on_expire"));
        });
    }
}
