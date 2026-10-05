package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.TargetFilter;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.race.AttributeIncidents;
import mc.sayda.creraces.util.GsonHelper;
import mc.sayda.creraces.util.IFoodDataAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * Sets, adds to or multiplies a numeric value. The "resource" field picks which one:
 * <ul>
 * <li>"self": the casting ability's persistent state</li>
 * <li>"state:&lt;key&gt;": a named persistent state, e.g. "state:creraces:stored_material"</li>
 * <li>"mana", "energy", "grit", "rage", "soul", "karma", "coins", "ap", "ad", "ah", "cr", "passive_cd": race resources</li>
 * <li>"health", "food", "saturation", "air": vanilla entity resources</li>
 * <li>"custom:&lt;key&gt;": a customization variable, parsed as a number</li>
 * </ul>
 * For states, "mode" can capture a coordinate instead of "value": POS_*, BLOCK_*, TARGET_* or TARGET_BLOCK_*.
 */
public class ModifyValueAction implements ActionRegistry.RaceAction {
    private static final String STATE_PREFIX = "state:";
    private static final String CUSTOM_PREFIX = "custom:";

    private final String resource;
    private final ScalingValue value;
    private final String operation;
    private final String mode;
    private final ScalingValue offsetX;
    private final ScalingValue offsetY;
    private final ScalingValue offsetZ;
    private final boolean persistent;
    private final boolean failIfInsufficient;
    private final boolean useTarget;
    private final TargetFilter targets;

    public ModifyValueAction(String resource, ScalingValue value, String operation, String mode,
            ScalingValue offsetX, ScalingValue offsetY, ScalingValue offsetZ, boolean persistent,
            boolean failIfInsufficient, boolean useTarget, TargetFilter targets) {
        this.resource = resource;
        this.value = value;
        this.operation = operation;
        this.mode = mode;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.persistent = persistent;
        this.failIfInsufficient = failIfInsufficient;
        this.useTarget = useTarget;
        this.targets = targets;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        String res = resource.toLowerCase();
        if (res.equals("self") || res.startsWith(STATE_PREFIX)) {
            return DataUtils.getVariables(player)
                    .map(vars -> modifyState(res, vars, player, target, slot, interactPos))
                    .orElse(true);
        }

        LivingEntity entity = useTarget ? target : player;
        if (entity == null || !targets.isValid(entity, player)) {
            return true;
        }
        double evaluated = value.evaluate(player, target, slot);

        if (res.equals("air") || res.equals("health") || res.equals("food") || res.equals("saturation")) {
            return modifyVanillaResource(res, entity, evaluated);
        }
        if (!(entity instanceof Player subject)) {
            return true;
        }
        return DataUtils.getVariables(subject)
                .map(vars -> res.startsWith(CUSTOM_PREFIX)
                        ? modifyCustomization(vars, subject, evaluated)
                        : modifyRaceResource(res, vars, subject, evaluated))
                .orElse(true);
    }

    private boolean modifyState(String res, IPlayerVariables vars, Player player, @Nullable LivingEntity target,
            @Nullable AbilitySlot slot, @Nullable BlockPos interactPos) {
        ResourceLocation stateId;
        if (res.equals("self")) {
            stateId = slot != null ? vars.getAbilityInSlot(slot) : null;
        } else {
            String subKey = res.substring(STATE_PREFIX.length());
            if (!subKey.contains(":")) {
                subKey = CreRaces.MODID + ":" + subKey;
            }
            stateId = ResourceLocation.tryParse(subKey);
        }
        if (stateId == null) {
            return true;
        }

        double ox = offsetX.evaluate(player, target, slot);
        double oy = offsetY.evaluate(player, target, slot);
        double oz = offsetZ.evaluate(player, target, slot);
        double contextual = value.evaluate(player, target, slot);
        switch (mode.toUpperCase()) {
            case "POS_X" -> contextual = player.getX() + ox;
            case "POS_Y" -> contextual = player.getY() + oy;
            case "POS_Z" -> contextual = player.getZ() + oz;
            case "BLOCK_X" -> contextual = player.blockPosition().getX() + (int) ox;
            case "BLOCK_Y" -> contextual = player.blockPosition().getY() + (int) oy;
            case "BLOCK_Z" -> contextual = player.blockPosition().getZ() + (int) oz;
            case "TARGET_X" -> { if (target != null) contextual = target.getX() + ox; }
            case "TARGET_Y" -> { if (target != null) contextual = target.getY() + oy; }
            case "TARGET_Z" -> { if (target != null) contextual = target.getZ() + oz; }
            case "TARGET_BLOCK_X" -> contextual = targetBlockCoordinate(player, interactPos, Direction.Axis.X, ox);
            case "TARGET_BLOCK_Y" -> contextual = targetBlockCoordinate(player, interactPos, Direction.Axis.Y, oy);
            case "TARGET_BLOCK_Z" -> contextual = targetBlockCoordinate(player, interactPos, Direction.Axis.Z, oz);
            default -> {
            }
        }

        double next = applyOp(vars.getPersistentState(stateId), contextual);
        if (failIfInsufficient && next < 0) {
            return false;
        }
        vars.setPersistentState(stateId, next);
        if (persistent) {
            vars.setStatePersistent(stateId, true);
        }
        BoundaryHandler.resyncVariables(player, player);
        return true;
    }

    /** The interacted or looked-at block's coordinate plus the offset, or the caster's own when there is none. */
    private static double targetBlockCoordinate(Player player, @Nullable BlockPos interactPos, Direction.Axis axis,
            double offset) {
        BlockPos block = BlockTargeting.interactedOrLookedAt(player, interactPos);
        return block != null ? block.get(axis) + (int) offset : player.position().get(axis) + offset;
    }

    private boolean modifyVanillaResource(String res, LivingEntity entity, double evaluated) {
        double current = switch (res) {
            case "air" -> entity.getAirSupply();
            case "health" -> entity.getHealth();
            case "food" -> entity instanceof Player p ? foodData(p).creraces$getFoodLevel() : 0;
            case "saturation" -> entity instanceof Player p ? foodData(p).creraces$getSaturation() : 0;
            default -> 0;
        };
        double next = applyOp(current, evaluated);
        if (failIfInsufficient && next < 0) {
            return false;
        }
        switch (res) {
            case "air" -> entity.setAirSupply((int) Math.max(0, Math.min(next, entity.getMaxAirSupply())));
            case "health" -> entity.setHealth((float) Math.max(0, Math.min(next, entity.getMaxHealth())));
            case "food" -> {
                if (entity instanceof Player p) {
                    foodData(p).creraces$setFoodLevel(
                            (int) Math.max(0, Math.min(next, CreRacesConfig.PASSIVE_DEFAULT_MAX_FOOD.get())));
                }
            }
            case "saturation" -> {
                if (entity instanceof Player p) {
                    foodData(p).creraces$setSaturation((float) Math.max(0, next));
                }
            }
            default -> {
            }
        }
        return true;
    }

    private static IFoodDataAccessor foodData(Player player) {
        return (IFoodDataAccessor) player.getFoodData();
    }

    private boolean modifyCustomization(IPlayerVariables vars, Player player, double evaluated) {
        String key = resource.substring(CUSTOM_PREFIX.length());
        String stored = vars.getCustomization(key);
        double current = 0;
        if (stored != null && !stored.isEmpty()) {
            try {
                current = Double.parseDouble(stored);
            } catch (NumberFormatException e) {
                // A non-numeric customization counts as 0.
            }
        }
        double next = applyOp(current, evaluated);
        if (failIfInsufficient && next < 0) {
            return false;
        }
        vars.setCustomization(key, String.valueOf(next));
        BoundaryHandler.resyncVariables(player, player);
        return true;
    }

    private boolean modifyRaceResource(String res, IPlayerVariables vars, Player player, double evaluated) {
        double current = switch (res) {
            case "mana" -> vars.getMana();
            case "energy" -> vars.getEnergy();
            case "grit" -> vars.getGrit();
            case "rage" -> vars.getRage();
            case "soul" -> vars.getSoul();
            case "karma" -> vars.getKarma();
            case "coins" -> vars.getCoins();
            case "ap" -> vars.getAp();
            case "ad" -> vars.getAd();
            case "ah" -> vars.getAh();
            case "cr" -> vars.getCr();
            case "passive_cd" -> vars.getPassiveCooldown();
            default -> 0;
        };
        double next = applyOp(current, evaluated);
        if (failIfInsufficient && next < 0) {
            return false;
        }
        switch (res) {
            case "mana" -> vars.setMana(next);
            case "energy" -> vars.setEnergy(next);
            case "grit" -> vars.setGrit(next);
            case "rage" -> vars.setRage(next);
            case "soul" -> {
                vars.setSoul(Math.max(0, Math.min(next, CreRacesConfig.MAX_SOUL.get())));
                // Attribute modifiers can scale with soul, so they are re-applied straight away.
                if (player instanceof ServerPlayer serverPlayer) {
                    AttributeIncidents.eikiJudgment(serverPlayer);
                }
            }
            case "karma" -> vars.setKarma(next);
            case "coins" -> vars.setCoins(next);
            case "ap" -> vars.setAp(next);
            case "ad" -> vars.setAd(next);
            case "ah" -> vars.setAh(next);
            case "cr" -> vars.setCr(next);
            case "passive_cd" -> vars.setPassiveCooldown(next);
            default -> {
            }
        }
        BoundaryHandler.resyncVariables(player, player);
        return true;
    }

    private double applyOp(double current, double incoming) {
        return switch (operation.toLowerCase()) {
            case "add" -> current + incoming;
            case "multiply" -> current * incoming;
            default -> incoming;
        };
    }

    public static void register() {
        ActionRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "modify_value"), ModifyValueAction::parse);
    }

    private static ModifyValueAction parse(JsonObject json) {
        return new ModifyValueAction(
                GsonHelper.getAsString(json, "resource", "mana"),
                ScalingValue.fromJson(json, "value", 0.0),
                GsonHelper.getAsString(json, "operation", "set"),
                GsonHelper.getAsString(json, "mode", "STATIC"),
                ScalingValue.fromJson(json, "offset_x", 0.0),
                ScalingValue.fromJson(json, "offset_y", 0.0),
                ScalingValue.fromJson(json, "offset_z", 0.0),
                GsonHelper.getAsBoolean(json, "persistent", false),
                GsonHelper.getAsBoolean(json, "fail_if_insufficient", false),
                GsonHelper.getAsBoolean(json, "use_target", false),
                TargetFilter.fromJson(json, "targets", Set.of("enemies", "self")));
    }
}
