package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Action that stops a specific sound for the player.
 */
public class StopSoundAction implements ActionRegistry.RaceAction {
    private final ResourceLocation soundId;
    private final SoundSource source;

    public StopSoundAction(ResourceLocation soundId, SoundSource source) {
        this.soundId = soundId;
        this.source = source;
    }

    @Override
    public boolean execute(Player player, @Nullable net.minecraft.world.entity.LivingEntity target,
            @Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @Nullable net.minecraft.core.BlockPos interactionPos) {
        if (!player.level().isClientSide()) {
            mc.sayda.creraces.network.BoundaryHandler.broadcastStopSound(player, soundId, source);
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "stop_sound"), json -> {
            String soundStr = GsonHelper.getAsString(json, "sound", "");
            ResourceLocation id = new ResourceLocation(soundStr);
            String sourceStr = GsonHelper.getAsString(json, "source", "PLAYERS");
            SoundSource source = SoundSource.valueOf(sourceStr.toUpperCase());
            return new StopSoundAction(id, source);
        });
    }
}
