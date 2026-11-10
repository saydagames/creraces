package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.Position;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * Plays "sound" at "position" (the target, or the caster when there is no target) for everyone
 * nearby. "source" is the volume slider it follows (players, hostile, blocks, ambient, ...).
 */
@SuppressWarnings("null")
public class PlaySoundAction implements ActionRegistry.RaceAction {
    private final SoundEvent sound;
    private final ScalingValue volume;
    private final ScalingValue pitch;
    private final SoundSource source;
    private final Position position;

    public PlaySoundAction(SoundEvent sound, ScalingValue volume, ScalingValue pitch, SoundSource source,
            Position position) {
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
        this.source = source;
        this.position = position;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        Vec3 at = position.resolve(player, target, slot, interactPos);
        if (at != null) {
            player.level().playSound(null, at.x, at.y, at.z, sound, source,
                    (float) volume.evaluate(player, target, slot, interactPos),
                    (float) pitch.evaluate(player, target, slot, interactPos));
        }
        return true;
    }

    /** Reads "source" as a lowercase sound category (players, blocks, records, ...); players by default. */
    static SoundSource parseSource(JsonObject json) {
        String name = GsonHelper.getAsString(json, "source", "players");
        for (SoundSource source : SoundSource.values()) {
            if (source.name().toLowerCase(Locale.ROOT).equals(name)) {
                return source;
            }
        }
        throw new IllegalArgumentException("unknown sound source '" + name + "'");
    }

    static SoundEvent parseSound(String text) {
        ResourceLocation id = ResourceLocation.tryParse(text);
        SoundEvent sound = id != null ? BuiltInRegistries.SOUND_EVENT.get(id) : null;
        if (sound == null) {
            throw new IllegalArgumentException("unknown sound '" + text + "'");
        }
        return sound;
    }

    public static void register() {
        ActionRegistry.register(CreRaces.id("play_sound"), json -> new PlaySoundAction(
                parseSound(GsonHelper.getAsString(json, "sound")),
                ScalingValue.fromJson(json, "volume", 1.0),
                ScalingValue.fromJson(json, "pitch", 1.0),
                parseSource(json),
                Position.fromJson(json, "position", Position.of(Position.Source.AUTO))));
    }
}
