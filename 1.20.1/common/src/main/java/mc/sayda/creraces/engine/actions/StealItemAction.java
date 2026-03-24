package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class StealItemAction implements ActionRegistry.RaceAction {

    private final ScalingValue chance;
    private final String targetSlot; // e.g., "mainhand", "offhand", "random"

    public StealItemAction(ScalingValue chance, String targetSlot) {
        this.chance = chance;
        this.targetSlot = targetSlot;
    }

    @Override
    public boolean execute(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        if (!(target instanceof Player targetPlayer))
            return true;

        double c = chance.evaluate(player, target, slot);
        if (player.level().random.nextDouble() > c)
            return true;

        ItemStack stolenStack = ItemStack.EMPTY;
        if (targetSlot.equals("mainhand")) {
            stolenStack = targetPlayer.getMainHandItem().copy();
            targetPlayer.getMainHandItem().setCount(0);
        } else if (targetSlot.equals("offhand")) {
            stolenStack = targetPlayer.getOffhandItem().copy();
            targetPlayer.getOffhandItem().setCount(0);
        } else if (targetSlot.equals("random")) {
            // Pick a random non-empty hotbar slot (0-8)
            java.util.List<Integer> nonEmpty = new java.util.ArrayList<>();
            for (int i = 0; i < 9; i++) {
                if (!targetPlayer.getInventory().getItem(i).isEmpty()) {
                    nonEmpty.add(i);
                }
            }
            if (!nonEmpty.isEmpty()) {
                int pickedSlot = nonEmpty.get(player.level().random.nextInt(nonEmpty.size()));
                stolenStack = targetPlayer.getInventory().getItem(pickedSlot).copy();
                targetPlayer.getInventory().setItem(pickedSlot, net.minecraft.world.item.ItemStack.EMPTY);
            }
        }

        if (!stolenStack.isEmpty()) {
            if (!player.getInventory().add(stolenStack)) {
                player.drop(stolenStack, false);
            }
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "steal_item"), json -> {
            ScalingValue chance = ScalingValue.fromJson(json, "chance", 0.5);
            String slot = GsonHelper.getAsString(json, "slot", "random");
            return new StealItemAction(chance, slot);
        });
    }
}
