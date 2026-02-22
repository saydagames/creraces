package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class DelayAction implements ActionRegistry.RaceAction {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "delay");

    private final int ticks;
    private final List<ActionRegistry.RaceAction> actions;

    public DelayAction(int ticks, List<ActionRegistry.RaceAction> actions) {
        this.ticks = ticks;
        this.actions = actions;
    }

    @Override
    public void execute(Player player, LivingEntity target, mc.sayda.creraces.ability.AbilitySlot slot) {
        if (player.level().isClientSide())
            return;

        mc.sayda.creraces.util.Scheduler.delay(ticks, () -> {
            for (ActionRegistry.RaceAction action : actions) {
                action.execute(player, target, slot);
            }
        });
    }

    public static void register() {
        ActionRegistry.register(ID, json -> {
            int ticks = GsonHelper.getAsInt(json, "ticks", 20);
            List<ActionRegistry.RaceAction> actions = new ArrayList<>();
            if (json.has("actions")) {
                JsonArray array = json.getAsJsonArray("actions");
                for (int i = 0; i < array.size(); i++) {
                    actions.add(ActionRegistry.fromJson(array.get(i).getAsJsonObject()));
                }
            }
            return new DelayAction(ticks, actions);
        });
    }
}
