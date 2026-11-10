package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.EntityFlag;
import mc.sayda.creraces.engine.EntitySelector;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Sets a "flag" (in_spirit_realm, mini_build, spirit, tiny) on the caster, or the target with
 * "entity": "target", to "value", or flips it with "operation": "toggle". Mobs carry flags as tags.
 */
public class SetFlagAction implements ActionRegistry.RaceAction {
    private final EntityFlag flag;
    private final boolean value;
    private final boolean toggle;
    private final EntitySelector entity;

    private SetFlagAction(EntityFlag flag, boolean value, boolean toggle, EntitySelector entity) {
        this.flag = flag;
        this.value = value;
        this.toggle = toggle;
        this.entity = entity;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        LivingEntity subject = entity.resolve(player, target);
        if (subject != null) {
            flag.set(subject, toggle ? !flag.get(subject) : value);
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(CreRaces.id("set_flag"), json -> {
            String name = GsonHelper.getAsString(json, "flag");
            EntityFlag flag = EntityFlag.byId(name);
            if (flag == null || !flag.isWritable()) {
                throw new IllegalArgumentException("flag '" + name + "' can't be set; settable: in_spirit_realm, "
                        + "mini_build, spirit, tiny");
            }
            String operation = GsonHelper.getAsString(json, "operation", "set");
            if (!operation.equals("set") && !operation.equals("toggle")) {
                throw new IllegalArgumentException("unknown operation '" + operation + "' (set, toggle)");
            }
            return new SetFlagAction(flag, GsonHelper.getAsBoolean(json, "value", true), operation.equals("toggle"),
                    EntitySelector.fromJson(json, EntitySelector.SELF));
        });
    }
}
