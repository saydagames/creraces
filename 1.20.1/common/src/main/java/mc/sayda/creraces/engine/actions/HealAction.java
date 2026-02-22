package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class HealAction implements ActionRegistry.RaceAction {

    private final ScalingValue amount;

    public HealAction(ScalingValue amount) {
        this.amount = amount;
    }

    @Override
    public void execute(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
        float h = (float) amount.evaluate(player);
        if (h > 0) {
            player.heal(h);
        }
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "heal"), json -> {
            ScalingValue amount = ScalingValue.fromJson(json, "amount", 1.0);
            return new HealAction(amount);
        });
    }
}
