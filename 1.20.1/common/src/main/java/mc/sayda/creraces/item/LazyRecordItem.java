package mc.sayda.creraces.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.RecordItem;
import net.minecraft.sounds.SoundEvent;

import java.util.function.Supplier;

/**
 * A RecordItem wrapper that defers resolving the SoundEvent until after
 * registries have initialized.
 * This prevents null pointer exceptions or mapping issues when RecordItems are
 * registered before SoundEvents are frozen.
 */
public class LazyRecordItem extends RecordItem {
    private final Supplier<SoundEvent> soundSupplier;

    public LazyRecordItem(int analogOutput, Supplier<SoundEvent> soundSupplier, Item.Properties properties,
            int lengthInTicks) {
        super(analogOutput, net.minecraft.sounds.SoundEvents.MUSIC_DISC_11, properties, lengthInTicks); // Pass valid
                                                                                                        // vanilla sound
                                                                                                        // to prevent
                                                                                                        // Forge
                                                                                                        // registration
                                                                                                        // crash
        this.soundSupplier = soundSupplier;
    }

    @Override
    public SoundEvent getSound() {
        return this.soundSupplier.get();
    }
}
