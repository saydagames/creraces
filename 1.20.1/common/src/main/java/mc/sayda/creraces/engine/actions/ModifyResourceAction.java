package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class ModifyResourceAction implements ActionRegistry.RaceAction {

    private final String resource;
    private final String operation; // set, add
    private final mc.sayda.creraces.engine.ScalingValue value;
    private final boolean useTarget;
    private final mc.sayda.creraces.engine.TargetFilter targets;

    public ModifyResourceAction(String resource, String operation, mc.sayda.creraces.engine.ScalingValue value,
            boolean useTarget, mc.sayda.creraces.engine.TargetFilter targets) {
        this.resource = resource;
        this.operation = operation;
        this.value = value;
        this.useTarget = useTarget;
        this.targets = targets;
    }

    @Override
    public boolean execute(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        // Targeting: use the explicit target if provided; otherwise fall back to player
        // unless use_target forces null
        LivingEntity entity = (target != null) ? target : (useTarget ? null : player);
        if (entity == null || !targets.isValid(entity, player))
            return true;

        String res = resource.toLowerCase();
        double evaluatedValue = value.evaluate(player, target);

        // Handle Vanilla Resources first (available for all/most LivingEntities)
        if (res.equals("air") || res.equals("health") || res.equals("food")) {
            double current = 0;
            if (res.equals("air"))
                current = entity.getAirSupply();
            else if (res.equals("health"))
                current = entity.getHealth();
            else if (res.equals("food") && entity instanceof Player p)
                current = p.getFoodData().getFoodLevel();

            double newValue = current;
            if (operation.equalsIgnoreCase("add"))
                newValue += evaluatedValue;
            else if (operation.equalsIgnoreCase("set"))
                newValue = evaluatedValue;

            if (res.equals("air"))
                entity.setAirSupply((int) Math.max(0, Math.min(newValue, entity.getMaxAirSupply())));
            else if (res.equals("health"))
                entity.setHealth((float) Math.max(0, Math.min(newValue, entity.getMaxHealth())));
            else if (res.equals("food") && entity instanceof Player p)
                p.getFoodData().setFoodLevel((int) Math.max(0, Math.min(newValue, 20)));

            return true;
        }

        // Handle Custom Variables (only for Players)
        if (entity instanceof Player p) {
            DataUtils.getVariables(p).ifPresent(vars -> {
                double current = 0;
                if (res.equals("energy"))
                    current = vars.getEnergy();
                else if (res.equals("mana"))
                    current = vars.getMana();
                else if (res.equals("rage"))
                    current = vars.getRage();
                else if (res.equals("grit"))
                    current = vars.getGrit();
                else if (res.equals("souls"))
                    current = vars.getSouls();
                else if (res.equals("stacks"))
                    current = vars.getStacks();

                double newValue = current;
                if (operation.equalsIgnoreCase("add"))
                    newValue += evaluatedValue;
                else if (operation.equalsIgnoreCase("set"))
                    newValue = evaluatedValue;

                if (res.equals("energy"))
                    vars.setEnergy(newValue);
                else if (res.equals("mana"))
                    vars.setMana(newValue);
                else if (res.equals("rage"))
                    vars.setRage(newValue);
                else if (res.equals("grit"))
                    vars.setGrit(newValue);
                else if (res.equals("souls"))
                    vars.setSouls(newValue);
                else if (res.equals("stacks")) {
                    vars.setStacks(newValue);
                    if (p instanceof net.minecraft.server.level.ServerPlayer sp) {
                        mc.sayda.creraces.race.AttributeIncidents.eikiJudgment(sp);
                    }
                }

                mc.sayda.creraces.network.BoundaryHandler.resyncVariables(p, p);
            });
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
                    "targets");
            return new ModifyResourceAction(resource, op, val, useTarget, targets);
        });
    }
}
