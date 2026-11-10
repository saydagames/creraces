package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.Position;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** Drops "amount" of "item" on the ground at "position" (the caster by default). */
@SuppressWarnings("null")
public class DropItemAction implements ActionRegistry.RaceAction {
    private final Item item;
    private final ScalingValue amount;
    private final Position position;

    public DropItemAction(Item item, ScalingValue amount, Position position) {
        this.item = item;
        this.amount = amount;
        this.position = position;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (player.level().isClientSide()) {
            return true;
        }
        Vec3 at = position.resolve(player, target, slot, interactPos);
        if (at == null) {
            return false;
        }
        int count = (int) amount.evaluate(player, target, slot, interactPos);
        if (count > 0) {
            player.level().addFreshEntity(new ItemEntity(player.level(), at.x, at.y, at.z, new ItemStack(item, count)));
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(CreRaces.id("drop_item"), json -> new DropItemAction(
                GiveItemAction.parseItem(GsonHelper.getAsString(json, "item")),
                ScalingValue.fromJson(json, "amount", 1.0),
                Position.fromJson(json, "position", Position.of(Position.Source.SELF))));
    }
}
