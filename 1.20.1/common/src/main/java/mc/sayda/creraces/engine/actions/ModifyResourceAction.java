package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.util.GsonHelper;
import mc.sayda.creraces.util.IFoodDataAccessor;
import mc.sayda.creraces.config.CreRacesConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class ModifyResourceAction implements ActionRegistry.RaceAction {

    private final String resource;
    private final String operation; // set, add
    private final mc.sayda.creraces.engine.ScalingValue value;
    private final boolean useTarget;
    private final mc.sayda.creraces.engine.TargetFilter targets;
    private final boolean failIfInsufficient;

    public ModifyResourceAction(String resource, String operation, mc.sayda.creraces.engine.ScalingValue value,
            boolean useTarget, mc.sayda.creraces.engine.TargetFilter targets, boolean failIfInsufficient) {
        this.resource = resource;
        this.operation = operation;
        this.value = value;
        this.useTarget = useTarget;
        this.targets = targets;
        this.failIfInsufficient = failIfInsufficient;
    }

    @Override
    public boolean execute(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        // Targeting: use the explicit target if provided; otherwise fall back to player
        // unless use_target forces null
        LivingEntity entity = useTarget ? target : player;
        if (entity == null || !targets.isValid(entity, player))
            return true;

        String res = resource.toLowerCase();
        double evaluatedValue = value.evaluate(player, target, slot);

        // Handle Vanilla Resources first (available for all/most LivingEntities)
        if (res.equals("air") || res.equals("health") || res.equals("food") || res.equals("saturation")) {
            double current = 0;
            if (res.equals("air"))
                current = entity.getAirSupply();
            else if (res.equals("health"))
                current = entity.getHealth();
            else if (res.equals("food") && entity instanceof Player p)
                current = ((IFoodDataAccessor) p.getFoodData()).creraces$getFoodLevel();
            else if (res.equals("saturation") && entity instanceof Player p)
                current = ((IFoodDataAccessor) p.getFoodData()).creraces$getSaturation();

            double newValue = current;
            if (operation.equalsIgnoreCase("add"))
                newValue += evaluatedValue;
            else if (operation.equalsIgnoreCase("set"))
                newValue = evaluatedValue;

            if (failIfInsufficient && newValue < 0) {
                return false;
            }

            if (res.equals("air"))
                entity.setAirSupply((int) Math.max(0, Math.min(newValue, entity.getMaxAirSupply())));
            else if (res.equals("health"))
                entity.setHealth((float) Math.max(0, Math.min(newValue, entity.getMaxHealth())));
            else if (res.equals("food") && entity instanceof Player p) {
                int maxFood = CreRacesConfig.PASSIVE_DEFAULT_MAX_FOOD.get();
                ((IFoodDataAccessor) p.getFoodData()).creraces$setFoodLevel((int) Math.max(0, Math.min(newValue, maxFood)));
            } else if (res.equals("saturation") && entity instanceof Player p) {
                ((IFoodDataAccessor) p.getFoodData())
                        .creraces$setSaturation((float) Math.max(0, Math.min(newValue, p.getFoodData().getFoodLevel())));
            }

            return true;
        }

        // Handle Custom Variables (only for Players)
        if (entity instanceof Player p) {
            var varsOpt = DataUtils.getVariables(p);
            if (varsOpt.isPresent()) {
                var vars = varsOpt.get();
                double current = 0;
                if (res.startsWith("custom:")) {
                    String key = resource.substring(7); // Use original 'resource' to maintain case
                    String valStr = vars.getCustomization(key);
                    try {
                        current = (valStr != null && !valStr.isEmpty()) ? Double.parseDouble(valStr) : 0.0;
                    } catch (NumberFormatException e) {
                        current = 0.0;
                    }

                    double newValue = current;
                    if (operation.equalsIgnoreCase("add"))
                        newValue += evaluatedValue;
                    else if (operation.equalsIgnoreCase("set"))
                        newValue = evaluatedValue;

                    if (failIfInsufficient && newValue < 0) {
                        return false;
                    }

                    vars.setCustomization(key, String.valueOf(newValue));
                    mc.sayda.creraces.network.BoundaryHandler.resyncVariables(p, p);
                    return true;
                }

                if (res.startsWith("state:")) {
                    String subKey = resource.substring(6);
                    if (!subKey.contains(":")) {
                        subKey = "creraces:" + subKey;
                    }
                    ResourceLocation id = ResourceLocation.tryParse(subKey);
                    if (id != null) {
                        current = vars.getPersistentState(id);
                        double newValue = current;
                        if (operation.equalsIgnoreCase("add"))
                            newValue += evaluatedValue;
                        else if (operation.equalsIgnoreCase("set"))
                            newValue = evaluatedValue;

                        if (failIfInsufficient && newValue < 0) {
                            return false;
                        }

                        vars.setPersistentState(id, newValue);
                        mc.sayda.creraces.network.BoundaryHandler.resyncVariables(p, p);
                        return true;
                    }
                }

                if (res.equals("energy"))
                    current = vars.getEnergy();
                else if (res.equals("mana"))
                    current = vars.getMana();
                else if (res.equals("rage"))
                    current = vars.getRage();
                else if (res.equals("grit"))
                    current = vars.getGrit();
                else if (res.equals("soul"))
                    current = vars.getSoul();
                else if (res.equals("karma"))
                    current = vars.getKarma();
                else if (res.equals("coins"))
                    current = vars.getCoins();
                else if (res.equals("ap"))
                    current = vars.getAp();
                else if (res.equals("ad"))
                    current = vars.getAd();
                else if (res.equals("ah"))
                    current = vars.getAh();
                else if (res.equals("cr"))
                    current = vars.getCr();
                else if (res.equals("passive_cd"))
                    current = vars.getPassiveCooldown();

                double newValue = current;
                if (operation.equalsIgnoreCase("add"))
                    newValue += evaluatedValue;
                else if (operation.equalsIgnoreCase("set"))
                    newValue = evaluatedValue;

                if (failIfInsufficient && newValue < 0) {
                    return false;
                }

                if (res.equals("energy"))
                    vars.setEnergy(newValue);
                else if (res.equals("mana"))
                    vars.setMana(newValue);
                else if (res.equals("rage"))
                    vars.setRage(newValue);
                else if (res.equals("grit"))
                    vars.setGrit(newValue);
                else if (res.equals("soul")) {
                    vars.setSoul(Math.max(0, Math.min(newValue, CreRacesConfig.MAX_SOUL.get())));
                    if (p instanceof net.minecraft.server.level.ServerPlayer sp) {
                        mc.sayda.creraces.race.AttributeIncidents.eikiJudgment(sp);
                    }
                } else if (res.equals("karma"))
                    vars.setKarma(newValue);
                else if (res.equals("coins"))
                    vars.setCoins(newValue);
                else if (res.equals("ap"))
                    vars.setAp(newValue);
                else if (res.equals("ad"))
                    vars.setAd(newValue);
                else if (res.equals("ah"))
                    vars.setAh(newValue);
                else if (res.equals("cr"))
                    vars.setCr(newValue);
                else if (res.equals("passive_cd"))
                    vars.setPassiveCooldown(newValue);

                mc.sayda.creraces.network.BoundaryHandler.resyncVariables(p, p);
            }
        }
        return true;

    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "modify_resource"), json -> {
            String resource = GsonHelper.getAsString(json, "resource", "mana");
            String op = GsonHelper.getAsString(json, "operation", "add");
            mc.sayda.creraces.engine.ScalingValue val = mc.sayda.creraces.engine.ScalingValue.fromJson(json, "value",
                    0.0);
            boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
            mc.sayda.creraces.engine.TargetFilter targets = mc.sayda.creraces.engine.TargetFilter.fromJson(json,
                    "targets", java.util.Set.of("enemies", "self"));
            boolean failIfInsufficient = GsonHelper.getAsBoolean(json, "fail_if_insufficient", false);
            return new ModifyResourceAction(resource, op, val, useTarget, targets, failIfInsufficient);
        });
    }
}
