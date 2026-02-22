package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Action that plays a sound at the player's location.
 */
public class PlaySoundAction implements ActionRegistry.RaceAction {
    private final ResourceLocation soundId;
    private final float volume;
    private final float pitch;

    public PlaySoundAction(ResourceLocation soundId, float volume, float pitch) {
        this.soundId = soundId;
        this.volume = volume;
        this.pitch = pitch;
    }

    @Override
    public void execute(Player player, @Nullable net.minecraft.world.entity.LivingEntity target,
            @Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(soundId);
        if (sound != null) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS,
                    volume, pitch);
        }
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "play_sound"), json -> {
            String soundStr = GsonHelper.getAsString(json, "sound", "minecraft:entity.experience_orb.pickup");
            ResourceLocation id = new ResourceLocation(soundStr);
            float vol = GsonHelper.getAsFloat(json, "volume", 1.0f);
            float pit = GsonHelper.getAsFloat(json, "pitch", 1.0f);
            return new PlaySoundAction(id, vol, pit);
        });
    }
}
