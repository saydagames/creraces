package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class StealItemAction implements ActionRegistry.RaceAction {

    private final double chance;
    private final String targetSlot; // e.g., "mainhand", "offhand", "random"

    public StealItemAction(double chance, String targetSlot) {
        this.chance = chance;
        this.targetSlot = targetSlot;
    }

    @Override
    public void execute(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
        if (!(target instanceof Player targetPlayer))
            return;
        if (player.level().random.nextDouble() > chance)
            return;

        ItemStack stolenStack = ItemStack.EMPTY;
        if (targetSlot.equals("mainhand")) {
            stolenStack = targetPlayer.getMainHandItem().copy();
            targetPlayer.getMainHandItem().setCount(0);
        } else if (targetSlot.equals("offhand")) {
            stolenStack = targetPlayer.getOffhandItem().copy();
            targetPlayer.getOffhandItem().setCount(0);
        } else {
            // Random slot or coin logic could go here
        }

        if (!stolenStack.isEmpty()) {
            if (!player.getInventory().add(stolenStack)) {
                player.drop(stolenStack, false);
            }
        }
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "steal_item"), json -> {
            double chance = json.has("chance") ? json.get("chance").getAsDouble() : 0.1;
            String slot = json.has("slot") ? json.get("slot").getAsString() : "mainhand";
            return new StealItemAction(chance, slot);
        });
    }
}
