package mc.sayda.creraces.item;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.RecordItem;

/**
 * A RecordItem whose sound is looked up on demand, so the disc still works if it is registered
 * before the mod's own SoundEvents exist.
 */
public class LazyRecordItem extends RecordItem {
    private final RegistrySupplier<SoundEvent> sound;

    public LazyRecordItem(int analogOutput, RegistrySupplier<SoundEvent> sound, Item.Properties properties,
            int lengthInTicks) {
        // RecordItem keys the "Now Playing" lookup on this sound, so it must be the disc's own. The fallback
        // has to be a registered sound (Forge resolves it here) that no vanilla disc uses.
        super(analogOutput, sound.isPresent() ? sound.get() : SoundEvents.EMPTY, properties, lengthInTicks);
        this.sound = sound;
    }

    @Override
    public SoundEvent getSound() {
        return this.sound.get();
    }
}
