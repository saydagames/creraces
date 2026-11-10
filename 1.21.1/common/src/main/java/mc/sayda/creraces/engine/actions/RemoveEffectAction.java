package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonElement;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.EntitySelector;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Removes one "effect" or a list of "effects" (effect ids) from an entity. */
@SuppressWarnings("null")
public class RemoveEffectAction implements ActionRegistry.RaceAction {
    private final List<Holder<MobEffect>> effects;
    private final EntitySelector entity;

    public RemoveEffectAction(List<Holder<MobEffect>> effects, EntitySelector entity) {
        this.effects = effects;
        this.entity = entity;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        LivingEntity subject = entity.resolve(player, target);
        if (subject != null) {
            effects.forEach(subject::removeEffect);
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(CreRaces.id("remove_effect"), json -> {
            List<String> ids = new ArrayList<>();
            if (json.has("effects")) {
                for (JsonElement element : json.getAsJsonArray("effects")) {
                    ids.add(element.getAsString());
                }
            } else {
                ids.add(GsonHelper.getAsString(json, "effect"));
            }
            List<Holder<MobEffect>> effects = new ArrayList<>();
            for (String id : ids) {
                ResourceLocation effectId = ResourceLocation.tryParse(id);
                Holder<MobEffect> effect = effectId != null ? BuiltInRegistries.MOB_EFFECT.getHolder(effectId).orElse(null) : null;
                if (effect == null) {
                    throw new IllegalArgumentException("unknown effect '" + id + "'");
                }
                effects.add(effect);
            }
            return new RemoveEffectAction(effects, EntitySelector.fromJson(json, EntitySelector.AUTO));
        });
    }
}
