package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class DisableShieldAction implements ActionRegistry.RaceAction {

    @Override
    public void execute(Player player, LivingEntity target, mc.sayda.creraces.ability.AbilitySlot slot) {
        if (target instanceof Player targetPlayer) {
            targetPlayer.disableShield(true);
        }
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "disable_shield"),
                json -> new DisableShieldAction());
    }
}
