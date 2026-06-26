package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.TargetFilter;
import mc.sayda.creraces.util.GsonHelper;
import mc.sayda.creraces.util.IFoodDataAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Unified value-modification action merging the old set_state and modify_resource.
 *
 * resource field:
 *   "self"           – current ability slot's persistent state
 *   "state:<key>"    – named persistent state (e.g. "state:creraces:stored_material")
 *   "mana" / "energy" / "grit" / "rage" / "soul" / "karma" / "coins" /
 *   "ap" / "ad" / "ah" / "cr" / "passive_cd"  – named race resources
 *   "health" / "food" / "saturation" / "air"  – vanilla entity resources
 *   "custom:<key>"   – customization string variable (parsed as double)
 */
public class ModifyValueAction implements ActionRegistry.RaceAction {

    private final String resource;
    private final ScalingValue value;
    private final String operation;   // "set", "add", "multiply"
    private final String mode;        // coordinate capture: STATIC | POS_X/Y/Z | BLOCK_X/Y/Z | TARGET_X/Y/Z | TARGET_BLOCK_X/Y/Z
    private final ScalingValue offsetX, offsetY, offsetZ;
    private final boolean persistent;
    private final boolean failIfInsufficient;
    private final boolean useTarget;
    private final TargetFilter targets;

    public ModifyValueAction(String resource, ScalingValue value, String operation, String mode,
            ScalingValue offsetX, ScalingValue offsetY, ScalingValue offsetZ,
            boolean persistent, boolean failIfInsufficient,
            boolean useTarget, TargetFilter targets) {
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
    public boolean execute(Player player, @Nullable LivingEntity target,
            @Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @Nullable net.minecraft.core.BlockPos interact_pos) {

        String res = resource.toLowerCase();

        // ── State / ability persistent values ──────────────────────────────────
        if (res.equals("self") || res.startsWith("state:")) {
            return DataUtils.getVariables(player).map(vars -> {
                ResourceLocation stateId;
                if (res.equals("self")) {
                    stateId = slot != null ? vars.getAbilityInSlot(slot) : null;
                } else {
                    String subKey = resource.substring(6); // strip "state:"
                    if (!subKey.contains(":")) subKey = "creraces:" + subKey;
                    stateId = ResourceLocation.tryParse(subKey);
                }
                if (stateId == null) return true;

                double current = vars.getPersistentState(stateId);
                double ox = offsetX.evaluate(player, target, slot);
                double oy = offsetY.evaluate(player, target, slot);
                double oz = offsetZ.evaluate(player, target, slot);
                double contextual = value.evaluate(player, target, slot);

                switch (mode.toUpperCase()) {
                    case "POS_X"          -> contextual = player.getX() + ox;
                    case "POS_Y"          -> contextual = player.getY() + oy;
                    case "POS_Z"          -> contextual = player.getZ() + oz;
                    case "BLOCK_X"        -> contextual = player.blockPosition().getX() + (int) ox;
                    case "BLOCK_Y"        -> contextual = player.blockPosition().getY() + (int) oy;
                    case "BLOCK_Z"        -> contextual = player.blockPosition().getZ() + (int) oz;
                    case "TARGET_X"       -> { if (target != null) contextual = target.getX() + ox; }
                    case "TARGET_Y"       -> { if (target != null) contextual = target.getY() + oy; }
                    case "TARGET_Z"       -> { if (target != null) contextual = target.getZ() + oz; }
                    case "TARGET_BLOCK_X" -> contextual = resolveBlockX(player, interact_pos, ox);
                    case "TARGET_BLOCK_Y" -> contextual = resolveBlockY(player, interact_pos, oy);
                    case "TARGET_BLOCK_Z" -> contextual = resolveBlockZ(player, interact_pos, oz);
                }

                double next = applyOp(current, contextual);
                if (failIfInsufficient && next < 0) return false;

                vars.setPersistentState(stateId, next);
                if (persistent) vars.setStatePersistent(stateId, true);
                mc.sayda.creraces.network.BoundaryHandler.resyncVariables(player, player);
                return true;
            }).orElse(true);
        }

        // ── Targeting resolution for non-state resources ────────────────────────
        LivingEntity entity = useTarget ? target : player;
        if (entity == null || !targets.isValid(entity, player)) return true;

        double evaluated = value.evaluate(player, target, slot);

        // ── Vanilla entity resources ────────────────────────────────────────────
        if (res.equals("air") || res.equals("health") || res.equals("food") || res.equals("saturation")) {
            double current = switch (res) {
                case "air"        -> entity.getAirSupply();
                case "health"     -> entity.getHealth();
                case "food"       -> entity instanceof Player p ? ((IFoodDataAccessor) p.getFoodData()).creraces$getFoodLevel() : 0;
                case "saturation" -> entity instanceof Player p ? ((IFoodDataAccessor) p.getFoodData()).creraces$getSaturation() : 0;
                default -> 0;
            };
            double next = applyOp(current, evaluated);
            if (failIfInsufficient && next < 0) return false;
            switch (res) {
                case "air"        -> entity.setAirSupply((int) Math.max(0, Math.min(next, entity.getMaxAirSupply())));
                case "health"     -> entity.setHealth((float) Math.max(0, Math.min(next, entity.getMaxHealth())));
                case "food"       -> { if (entity instanceof Player p) ((IFoodDataAccessor) p.getFoodData()).creraces$setFoodLevel((int) Math.max(0, Math.min(next, CreRacesConfig.PASSIVE_DEFAULT_MAX_FOOD.get()))); }
                case "saturation" -> { if (entity instanceof Player p) ((IFoodDataAccessor) p.getFoodData()).creraces$setSaturation((float) Math.max(0, next)); }
            }
            return true;
        }

        // ── Player-only resources ───────────────────────────────────────────────
        if (!(entity instanceof Player p)) return true;

        return DataUtils.getVariables(p).map(vars -> {
            // Custom string variable
            if (res.startsWith("custom:")) {
                String key = resource.substring(7);
                String valStr = vars.getCustomization(key);
                double current = 0;
                try { if (valStr != null && !valStr.isEmpty()) current = Double.parseDouble(valStr); } catch (NumberFormatException ignored) {}
                double next = applyOp(current, evaluated);
                if (failIfInsufficient && next < 0) return false;
                vars.setCustomization(key, String.valueOf(next));
                mc.sayda.creraces.network.BoundaryHandler.resyncVariables(p, p);
                return true;
            }

            // Named race resources
            double current = switch (res) {
                case "mana"       -> vars.getMana();
                case "energy"     -> vars.getEnergy();
                case "grit"       -> vars.getGrit();
                case "rage"       -> vars.getRage();
                case "soul"       -> vars.getSoul();
                case "karma"      -> vars.getKarma();
                case "coins"      -> vars.getCoins();
                case "ap"         -> vars.getAp();
                case "ad"         -> vars.getAd();
                case "ah"         -> vars.getAh();
                case "cr"         -> vars.getCr();
                case "passive_cd" -> vars.getPassiveCooldown();
                default -> 0;
            };

            double next = applyOp(current, evaluated);
            if (failIfInsufficient && next < 0) return false;

            switch (res) {
                case "mana"       -> vars.setMana(next);
                case "energy"     -> vars.setEnergy(next);
                case "grit"       -> vars.setGrit(next);
                case "rage"       -> vars.setRage(next);
                case "soul"       -> {
                    vars.setSoul(Math.max(0, Math.min(next, CreRacesConfig.MAX_SOUL.get())));
                    if (p instanceof net.minecraft.server.level.ServerPlayer sp)
                        mc.sayda.creraces.race.AttributeIncidents.eikiJudgment(sp);
                }
                case "karma"      -> vars.setKarma(next);
                case "coins"      -> vars.setCoins(next);
                case "ap"         -> vars.setAp(next);
                case "ad"         -> vars.setAd(next);
                case "ah"         -> vars.setAh(next);
                case "cr"         -> vars.setCr(next);
                case "passive_cd" -> vars.setPassiveCooldown(next);
            }
            mc.sayda.creraces.network.BoundaryHandler.resyncVariables(p, p);
            return true;
        }).orElse(true);
    }

    private double applyOp(double current, double incoming) {
        return switch (operation.toLowerCase()) {
            case "add"      -> current + incoming;
            case "multiply" -> current * incoming;
            default         -> incoming; // "set"
        };
    }

    private double resolveBlockX(Player player, @Nullable net.minecraft.core.BlockPos pos, double ox) {
        if (pos != null) return pos.getX() + (int) ox;
        var hit = player.pick(5.0, 0f, false);
        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK)
            return ((net.minecraft.world.phys.BlockHitResult) hit).getBlockPos().getX() + (int) ox;
        return player.getX() + ox;
    }

    private double resolveBlockY(Player player, @Nullable net.minecraft.core.BlockPos pos, double oy) {
        if (pos != null) return pos.getY() + (int) oy;
        var hit = player.pick(5.0, 0f, false);
        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK)
            return ((net.minecraft.world.phys.BlockHitResult) hit).getBlockPos().getY() + (int) oy;
        return player.getY() + oy;
    }

    private double resolveBlockZ(Player player, @Nullable net.minecraft.core.BlockPos pos, double oz) {
        if (pos != null) return pos.getZ() + (int) oz;
        var hit = player.pick(5.0, 0f, false);
        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK)
            return ((net.minecraft.world.phys.BlockHitResult) hit).getBlockPos().getZ() + (int) oz;
        return player.getZ() + oz;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "modify_value"), json -> parse(json));
    }

    private static ModifyValueAction parse(com.google.gson.JsonObject json) {
        String resource = GsonHelper.getAsString(json, "resource", "mana");
        ScalingValue value = ScalingValue.fromJson(json, "value", 0.0);
        String operation = GsonHelper.getAsString(json, "operation", "set");
        String mode = GsonHelper.getAsString(json, "mode", "STATIC");
        ScalingValue ox = ScalingValue.fromJson(json, "offset_x", 0.0);
        ScalingValue oy = ScalingValue.fromJson(json, "offset_y", 0.0);
        ScalingValue oz = ScalingValue.fromJson(json, "offset_z", 0.0);
        boolean persistent = GsonHelper.getAsBoolean(json, "persistent", false);
        boolean failIfInsufficient = GsonHelper.getAsBoolean(json, "fail_if_insufficient", false);
        boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
        TargetFilter targets = TargetFilter.fromJson(json, "targets", java.util.Set.of("enemies", "self"));
        return new ModifyValueAction(resource, value, operation, mode, ox, oy, oz, persistent, failIfInsufficient, useTarget, targets);
    }
}
