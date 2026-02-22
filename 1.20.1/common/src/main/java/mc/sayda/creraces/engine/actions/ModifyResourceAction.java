package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
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
    private final double value;
    private final boolean useTarget;

    public ModifyResourceAction(String resource, String operation, double value, boolean useTarget) {
        this.resource = resource;
        this.operation = operation;
        this.value = value;
        this.useTarget = useTarget;
    }

    @Override
    public void execute(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
        LivingEntity entity = (useTarget && target != null) ? target : player;
        String res = resource.toLowerCase();

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
                newValue += value;
            else if (operation.equalsIgnoreCase("set"))
                newValue = value;

            if (res.equals("air"))
                entity.setAirSupply((int) newValue);
            else if (res.equals("health"))
                entity.setHealth((float) newValue);
            else if (res.equals("food") && entity instanceof Player p)
                p.getFoodData().setFoodLevel((int) newValue);

            return;
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
                    newValue += value;
                else if (operation.equalsIgnoreCase("set"))
                    newValue = value;

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
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "modify_resource"), json -> {
            String resource = GsonHelper.getAsString(json, "resource", "energy");
            String op = GsonHelper.getAsString(json, "operation", "add");
            double val = GsonHelper.getAsDouble(json, "value", 0.0);
            boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
            return new ModifyResourceAction(resource, op, val, useTarget);
        });
    }
}
