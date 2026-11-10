package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.EntitySelector;
import mc.sayda.creraces.engine.ScalingValue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/** Heals an entity by "amount" health points. */
public class HealAction implements ActionRegistry.RaceAction {
    private final ScalingValue amount;
    private final EntitySelector entity;

    public HealAction(ScalingValue amount, EntitySelector entity) {
        this.amount = amount;
        this.entity = entity;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        LivingEntity subject = entity.resolve(player, target);
        float heal = (float) amount.evaluate(player, target, slot, interactPos);
        if (subject != null && heal > 0) {
            subject.heal(heal);
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(CreRaces.id("heal"), json -> new HealAction(
                ScalingValue.fromJson(json, "amount", 1.0),
                EntitySelector.fromJson(json, EntitySelector.AUTO)));
    }
}
