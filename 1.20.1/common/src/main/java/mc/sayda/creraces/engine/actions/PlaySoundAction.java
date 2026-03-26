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
    private final mc.sayda.creraces.engine.ScalingValue volume;
    private final mc.sayda.creraces.engine.ScalingValue pitch;
    private final boolean useTarget;

    public PlaySoundAction(ResourceLocation soundId, mc.sayda.creraces.engine.ScalingValue volume,
            mc.sayda.creraces.engine.ScalingValue pitch, boolean useTarget) {
        this.soundId = soundId;
        this.volume = volume;
        this.pitch = pitch;
        this.useTarget = useTarget;
    }

    @Override
    public boolean execute(Player player, @Nullable net.minecraft.world.entity.LivingEntity target,
            @Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @Nullable net.minecraft.core.BlockPos interactionPos) {
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(soundId);
        if (sound == null) {
            CreRaces.LOGGER.error("PlaySoundAction: unknown sound event '{}'", soundId);
            return true;
        }

        if (player.level() != null) {
            // Smart Targeting: Prefer target if present, otherwise respect useTarget flag
            net.minecraft.world.entity.LivingEntity subject = (target != null) ? target : (useTarget ? target : player);
            if (subject != null) {
                player.level().playSound(null, subject.getX(), subject.getY(), subject.getZ(), sound,
                        SoundSource.PLAYERS, (float) volume.evaluate(player, target, slot),
                        (float) pitch.evaluate(player, target, slot));
            }
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "play_sound"), json -> {
            String soundStr = GsonHelper.getAsString(json, "sound", "minecraft:entity.experience_orb.pickup");
            ResourceLocation id = new ResourceLocation(soundStr);
            mc.sayda.creraces.engine.ScalingValue vol = mc.sayda.creraces.engine.ScalingValue.fromJson(json, "volume",
                    1.0);
            mc.sayda.creraces.engine.ScalingValue pit = mc.sayda.creraces.engine.ScalingValue.fromJson(json, "pitch",
                    1.0);
            boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
            return new PlaySoundAction(id, vol, pit, useTarget);
        });
    }
}
