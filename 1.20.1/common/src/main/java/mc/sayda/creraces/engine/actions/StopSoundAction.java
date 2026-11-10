package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/** Stops a sound for the caster and everyone tracking them. */
public class StopSoundAction implements ActionRegistry.RaceAction {
    private final ResourceLocation soundId;
    private final SoundSource source;

    public StopSoundAction(ResourceLocation soundId, SoundSource source) {
        this.soundId = soundId;
        this.source = source;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (!player.level().isClientSide()) {
            BoundaryHandler.broadcastStopSound(player, soundId, source);
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(CreRaces.id("stop_sound"), json -> {
            ResourceLocation sound = ResourceLocation.tryParse(GsonHelper.getAsString(json, "sound"));
            if (sound == null) {
                throw new IllegalArgumentException("malformed sound id");
            }
            return new StopSoundAction(sound, PlaySoundAction.parseSource(json));
        });
    }
}
