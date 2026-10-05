package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.TargetFilter;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/** Plays a sound at the target's position if there is one, otherwise at the caster's (unless use_target). */
public class PlaySoundAction implements ActionRegistry.RaceAction {
    private final ResourceLocation soundId;
    private final ScalingValue volume;
    private final ScalingValue pitch;
    private final boolean useTarget;

    public PlaySoundAction(ResourceLocation soundId, ScalingValue volume, ScalingValue pitch, boolean useTarget) {
        this.soundId = soundId;
        this.volume = volume;
        this.pitch = pitch;
        this.useTarget = useTarget;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(soundId);
        if (sound == null) {
            CreRaces.LOGGER.error("PlaySoundAction: unknown sound event '{}'", soundId);
            return true;
        }
        LivingEntity subject = TargetFilter.resolveSmartTarget(player, target, useTarget);
        if (subject != null) {
            player.level().playSound(null, subject.getX(), subject.getY(), subject.getZ(), sound, SoundSource.PLAYERS,
                    (float) volume.evaluate(player, target, slot), (float) pitch.evaluate(player, target, slot));
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "play_sound"), json -> new PlaySoundAction(
                ResourceLocation.parse(GsonHelper.getAsString(json, "sound", "minecraft:entity.experience_orb.pickup")),
                ScalingValue.fromJson(json, "volume", 1.0),
                ScalingValue.fromJson(json, "pitch", 1.0),
                GsonHelper.getAsBoolean(json, "use_target", false)));
    }
}
