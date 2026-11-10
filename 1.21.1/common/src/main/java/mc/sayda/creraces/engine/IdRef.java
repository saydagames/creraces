package mc.sayda.creraces.engine;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * An id written in JSON for a state, cooldown or ability, where "self" stands for the casting
 * ability: the one in the triggering slot, or the active ability when there is no slot. Ids
 * without a namespace are creraces ones.
 */
public final class IdRef {
    private IdRef() {
    }

    @Nullable
    public static ResourceLocation resolve(String text, Player caster, @Nullable AbilitySlot slot) {
        if (!text.equals("self")) {
            return parse(text);
        }
        IPlayerVariables vars = DataUtils.getVariables(caster).orElse(null);
        if (vars == null) {
            return null;
        }
        if (slot != null) {
            return vars.getAbilityInSlot(slot);
        }
        return vars.isAbilityActive() ? vars.getActiveAbility() : null;
    }

    /** The id itself, creraces: by default; null when malformed. "self" is not resolved here. */
    @Nullable
    public static ResourceLocation parse(String text) {
        return ResourceLocation.tryParse(text.contains(":") ? text : CreRaces.MODID + ":" + text);
    }
}
